package top.hzchu.applog.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import top.hzchu.applog.MainActivity
import top.hzchu.applog.R

/**
 * 包变更广播接收器 - 实现防抖动计数器机制
 *
 * 逻辑:
 * 1. 收到广播 -> 递增计数器
 * 2. 计数器达到阈值 -> 发送通知提醒用户
 * 3. 通知可点击"下次提醒"重置计数器
 */
class PackageChangeReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "app_changes"
        const val NOTIFICATION_ID = 1001
        const val ACTION_RESET_COUNTER = "top.hzchu.applog.RESET_COUNTER"
        const val ACTION_TRIGGER_SCAN = "top.hzchu.applog.TRIGGER_SCAN"

        private const val PREFS_NAME = "applog_debounce"
        private const val KEY_COUNTER = "debounce_counter"
        private const val DEFAULT_THRESHOLD = 5

        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED
        )

        fun resetCounter(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_COUNTER, 0)
                .apply()
        }

        fun getCounter(context: Context): Int {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_COUNTER, 0)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_RESET_COUNTER -> {
                resetCounter(context)
                return
            }
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                handlePackageChange(context, intent)
            }
        }
    }

    private fun handlePackageChange(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        if (replacing) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val threshold = prefs.getInt("debounce_threshold", DEFAULT_THRESHOLD)
        val counter = prefs.getInt(KEY_COUNTER, 0) + 1
        prefs.edit().putInt(KEY_COUNTER, counter).apply()

        if (counter >= threshold) {
            sendNotification(context, counter)
        }
    }

    private fun sendNotification(context: Context, count: Int) {
        createNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = Intent(context, PackageChangeReceiver::class.java).apply {
            action = ACTION_RESET_COUNTER
        }
        val resetPendingIntent = PendingIntent.getBroadcast(
            context, 1, resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(context.getString(R.string.notif_title_list_changed))
            .setContentText(context.getString(R.string.notif_text_changes_detected, count))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_revert, context.getString(R.string.remind_later), resetPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Android 13+ no notification permission
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
