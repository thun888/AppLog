package top.hzchu.applog.model

/**
 * 可视化差异结果
 */
data class DiffResult(
    /** 新增的应用 */
    val added: List<AppInfo> = emptyList(),
    /** 卸载的应用 (含卸载前版本信息) */
    val removed: List<AppInfo> = emptyList(),
    /** 更新的应用: Pair(旧版本, 新版本) */
    val updated: List<Pair<AppInfo, AppInfo>> = emptyList(),
    /** 备注变更的应用: Pair(旧版, 新版) */
    val noteChanged: List<Pair<AppInfo, AppInfo>> = emptyList(),
    /** 标签变更的应用: Pair(旧版, 新版) */
    val tagsChanged: List<Pair<AppInfo, AppInfo>> = emptyList()
) {
//    val isEmpty: Boolean
//        get() = added.isEmpty() && removed.isEmpty() && updated.isEmpty() && noteChanged.isEmpty() && tagsChanged.isEmpty()
//
//    val totalChanges: Int
//        get() = added.size + removed.size + updated.size + noteChanged.size + tagsChanged.size
//
//    enum class ChangeType {
//        ADDED,
//        REMOVED,
//        UPDATED,
//        NOTE_CHANGED,
//        TAGS_CHANGED
//    }
}