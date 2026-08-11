## 核心功能逻辑与系统架构

实现一个体验优秀、具备真正 Git 引擎能力的应用列表版本控制工具，：

```
[ Android 系统包管理器 ] 
          │ (监听广播 / 定时扫描)
          ▼
[ 1. 扫描与元数据提取 ] ──> (确定性排序) ──> [ 2. {app1info}\n{app2info} ]
                                                       │
                                                       ▼
[ 5. UI 交互 & 差异还原 ] <── [ 4. 可视化 Diff 引擎 ] <── [ 3. JGit 内置 Git 引擎 ]
```

### 1. 扫描与元数据提取模块

通过 Android `PackageManager` API 扫描系统中的应用，采集关键元数据：

- **核心字段**：包名 (`packageName`)、应用名称 (`appName`)、版本号 (`versionName`)、版本代码 (`versionCode`)。
- **扩展字段**：安装时间 (`firstInstallTime`)、最后更新时间 (`lastUpdateTime`)、安装来源 (`installerPackageName`，如 Google Play、F-Droid)、应用类型（第三方/系统应用）、签名 SHA-256 Hash（用于识别异源覆盖）。
- 备注：使用内置数据库，可以记录用户为某个应用的特殊备注，若用户删除了应用，则以灰色显示（类型kv，包名对应备注）。备注同样附到元数据里

以上元数据均使用`|`分隔，构建appinfo

### 2. 确定性序列化模块

Git 是基于文本/字节流变更的，**序列化时的顺序一致性**决定了 Git Diff 的可读性：

- **必须确定性排序**：扫描到的列表必须按照 `packageName` 进行**严格字典序排序**，否则每次导出的行顺序随机变化会导致无效的提交（Git 噪音）。

- **格式选择**：

  Plaintext

  ```
  com.discord (Discord)|v5.2.0 (5200)|2026-03-01
  com.github.android (GitHub)|v2.1.0 (2100)|2026-04-12
  ```

### 3. 内置 Git 引擎模块

无需依赖系统安装 Git 命令行或 Root 权限：

- **技术选型**：直接集成 **JGit**（Eclipse 开源的纯 Java/Kotlin 实现 Git 引擎），运行在 App 沙盒内部。
- **仓库隔离**：在 App 私有目录（`context.getFilesDir()/git_repo`）或用户指定的本地存储位置 `git init` 初始化仓库。
- **核心操作流**：
  - `Git.init()`：首次启动初始化。
  - `Git.add()`：将生成的列表文本写入文件（如 `apps_snapshot.json`）并暂存。
  - `Git.commit()`：生成 Commit，提交信息自动格式化（例：`[AutoCommit] +2 apps, -1 app, 3 updated`）。
  - `Git.log()` / `Git.diff()`：提取历史版本树和差异节点。
  - `Git.push()`：支持 SSH Key / Personal Access Token 认证，后台同步至 GitHub/Gitea/GitLab。
- 注意：要实现以下功能：
  - 可选拉取远程仓库，可以强行拉取或同步

### 4. 事件监听与防抖动机制

- **广播监听**：注册 `BroadcastReceiver` 监听 Android 系统包变更广播：
  - `Intent.ACTION_PACKAGE_ADDED`
  - `Intent.ACTION_PACKAGE_REMOVED`
  - `Intent.ACTION_PACKAGE_REPLACED`
- **防抖动缓冲（Debounce Buffer）**：
  - *问题*：应用商店批量更新时，1 分钟内可能连续触发 10 次广播，若每次都 commit 会导致提交历史极其碎片化。
  - *逻辑*：监听到广播后不立即 commit，在监测到指定数量的广播后通知提醒，通知里可点击下次提醒来重置计数器

### 5. 可视化差异与恢复指引模块

- **Visual Diff 视图**：
  - 🔴 **Red (-)**：已卸载的应用包名及卸载前版本。
  - 🟢 **Green (+)**：新安装的应用。
  - 🔵 **Blue (↑/↓)**：更新/降级的应用（显示旧版本号 $\to$ 新版本号）。
- **里程碑打标签 (Tagging)**：允许用户对关键 Commit 打标签（如 `Tag: 换机初始备份`、`Tag: 刷机前快照`）。
- **恢复助手 (Restoration Helper)**：
  - 当用户回滚或查看历史快照时，App 提取差异列表中被删除的应用。
  - 提供“一键搜索下载”或生成 ADB 批量安装脚本（`adb install`），若记录了 Play 商店/F-Droid 来源，可直接拉起对应应用商店页面。

## 建议

1. **Android 11+ 包可见性限制 (Package Visibility)**：

   需要在 `AndroidManifest.xml` 中声明 `<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />`，否则 `PackageManager` 只能扫描到 App 自身和少量系统组件。

2. **多设备与多分支策略**：

   如果用户有手机和平板，或者经常刷机，建议 Git 架构设计为**默认分支按设备名区分**（如 `branch: pixel-8`、`branch: pad-pro`），在云端汇总时不发生冲突。