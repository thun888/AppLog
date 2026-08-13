package top.hzchu.applog.utils

import android.content.Context
import top.hzchu.applog.R

object InstallerUtils {
    private val storeMap = mapOf(
        "com.xiaomi.market" to R.string.store_xiaomi,
        "com.heytap.market" to R.string.store_oppo,
        "com.bbk.appstore" to R.string.store_vivo,
        "com.huawei.appmarket" to R.string.store_huawei,
        "com.meizu.mstore" to R.string.store_meizu,
        "com.android.vending" to R.string.google_play_store,
        "com.sec.android.app.samsungapps" to R.string.store_samsung,
        "com.hihonor.appmarket" to R.string.store_honor,
        "com.lenovo.leos.appstore" to R.string.store_lenovo,
        "com.yulong.android.coolmart" to R.string.store_coolpad,
        "com.coolapk.market" to R.string.store_coolapk,
        "com.wandoujia.phoenix2" to R.string.store_wandoujia,
        "com.tencent.android.qqdownloader" to R.string.store_tencent,
        "com.baidu.appsearch" to R.string.store_baidu,
        "com.qihoo.appstore" to R.string.store_360
    )

    fun getInstallerName(context: Context, installerPackageName: String?): String {
        if (installerPackageName.isNullOrBlank()) return context.getString(R.string.unknown_installer)
        val resId = storeMap[installerPackageName]
        return if (resId != null) {
            context.getString(resId)
        } else {
            // Try to get app name from package manager if it's not a known store
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(installerPackageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                installerPackageName
            }
        }
    }
}
