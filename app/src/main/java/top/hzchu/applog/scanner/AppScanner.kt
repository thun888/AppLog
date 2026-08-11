package top.hzchu.applog.scanner

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import top.hzchu.applog.model.AppInfo
import java.security.MessageDigest

/**
 * 应用扫描器 - 通过 PackageManager 采集应用元数据
 */
class AppScanner(private val context: Context) {

    /**
     * 扫描所有已安装应用，返回按 packageName 字典序排序的列表
     */
    fun scanAllApps(): List<AppInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

        return packages
            .mapNotNull { pkg -> scanPackage(pm, pkg) }
            .sortedBy { it.packageName }
    }

    /**
     * 扫描单个包
     */
    private fun scanPackage(pm: PackageManager, pkg: PackageInfo): AppInfo? {
        return try {
            val appInfo = pkg.applicationInfo ?: return null
            val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            AppInfo(
                packageName = pkg.packageName,
                appName = pm.getApplicationLabel(appInfo).toString(),
                versionName = pkg.versionName ?: "unknown",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkg.versionCode.toLong()
                },
                firstInstallTime = pkg.firstInstallTime,
                lastUpdateTime = pkg.lastUpdateTime,
                installerPackageName = pm.getInstallerPackageName(pkg.packageName) ?: "",
                appType = if (isSystemApp) AppInfo.AppType.SYSTEM else AppInfo.AppType.THIRD_PARTY,
                signatureSha256 = getSignatureSha256(pm, pkg.packageName)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取应用签名 SHA-256 Hash
     */
    private fun getSignatureSha256(pm: PackageManager, packageName: String): String {
        return try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }

            val pkgInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(packageName, flags)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, flags)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures
            } ?: return ""

            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(signatures.first().toByteArray())
            Base64.encodeToString(hash, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }
}
