package top.hzchu.applog

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class AppLogApplication : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "app_changes"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "应用变更提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "应用安装/卸载/更新提醒"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
