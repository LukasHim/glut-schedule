package com.glut.schedule.service.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.glut.schedule.MainActivity
import com.glut.schedule.R

/**
 * 上课提醒通知的渠道与发送。
 *
 * 通知点击后直接回到课表首页，不带任何额外参数：单 Activity + singleTask 的
 * 既有启动方式不需要为提醒再造一条深链。
 */
object ClassReminderNotifier {
    const val CHANNEL_ID = "class_reminders"
    private const val CHANNEL_NAME = "上课提醒"
    private const val CHANNEL_DESCRIPTION = "在课程开始前提醒你前往教室"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    /** Android 13 起需要运行时通知权限；更低版本只要系统通知开关没关即可。 */
    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** @return 通知是否真的发出；没有权限时返回 false，调用方据此不写去重标记。 */
    fun notify(context: Context, reminder: ClassReminder, minutesBefore: Int): Boolean {
        if (!canPostNotifications(context)) return false
        ensureChannel(context)
        val title = if (minutesBefore <= 1) "即将上课" else "${minutesBefore} 分钟后上课"
        val details = buildString {
            append("《${reminder.courseTitle}》")
            append(" ")
            append(reminder.startTime)
            if (reminder.room.isNotBlank()) {
                append(" · ")
                append(reminder.room)
            }
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.key.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_class)
            .setContentTitle(title)
            .setContentText(details)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(
                reminder.key.hashCode() and 0x0FFFFFFF,
                notification
            )
        }.isSuccess
    }
}
