package com.studybuddy.app.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.studybuddy.app.*

object TaskNotifications {
    private const val CHANNEL = "studybuddy_reminders"
    private const val GROUP = "studybuddy.homework"
    private const val SUMMARY = 9400
    private fun alarm(context: Context, id: Int) = PendingIntent.getBroadcast(context, id,
        Intent(context, NotificationActionReceiver::class.java).setAction("remind").putExtra("task_id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun cancel(context: Context, id: Int) {
        context.getSystemService(AlarmManager::class.java).cancel(alarm(context, id))
        context.getSystemService(AlarmManager::class.java).cancel(PendingIntent.getBroadcast(context, id,
            Intent(context, TaskAlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.activeNotifications.filter { it.notification.extras.getInt("task_id", -1) == id }
            .forEach { manager.cancel(it.id) }
        manager.cancel(10000 + id)
        refreshSummary(context)
    }

    fun show(context: Context, task: StudyTask, message: String? = null, actions: Boolean = true): Boolean {
        if (task.done) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Ödev hatırlatmaları", NotificationManager.IMPORTANCE_HIGH))
        if (!manager.areNotificationsEnabled() || (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) || manager.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        val open = PendingIntent.getActivity(context, task.id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text = message ?: "${task.name} · ${task.course}"
        val builder = Notification.Builder(context, CHANNEL).setSmallIcon(com.studybuddy.app.R.drawable.ic_notification_book)
            .setContentTitle("StudyBuddy · Ödev hatırlatması").setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true)
            .setGroup(GROUP).setGroupAlertBehavior(Notification.GROUP_ALERT_CHILDREN)
            .setDeleteIntent(PendingIntent.getBroadcast(context, task.id, Intent(context, NotificationActionReceiver::class.java).setAction("dismiss"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .addExtras(Bundle().apply { putInt("task_id", task.id) })
        if (actions) listOf("complete" to "Tamamlandı", "snooze" to "10 dk ertele").forEach { (action, label) ->
            val pending = PendingIntent.getBroadcast(context, task.id,
                Intent(context, NotificationActionReceiver::class.java).setAction(action).putExtra("task_id", task.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(Notification.Action.Builder(null, label, pending).build())
        }
        manager.notify(10000 + task.id, builder.build())
        refreshSummary(context)
        return manager.activeNotifications.any { it.id == 10000 + task.id }
    }

    fun refreshSummary(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val ids = manager.activeNotifications.filter { it.notification.group == GROUP && it.id != SUMMARY }
            .map { it.notification.extras.getInt("task_id", -1) }.toSet()
        val tasks = TaskStore.load(context).filter { !it.done && it.id in ids }
        if (tasks.size < 2) { manager.cancel(SUMMARY); return }
        if (!manager.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(context, SUMMARY, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val style = Notification.InboxStyle().setBigContentTitle("${tasks.size} ödev hatırlatması")
        tasks.take(5).forEach { style.addLine("${it.course} · ${it.name}") }
        manager.notify(SUMMARY, Notification.Builder(context, CHANNEL)
            .setSmallIcon(com.studybuddy.app.R.drawable.ic_notification_book)
            .setContentTitle("${tasks.size} ödev hatırlatması").setContentText("Ödevlerini birlikte görüntüle")
            .setStyle(style).setGroup(GROUP).setGroupSummary(true).setOnlyAlertOnce(true)
            .setGroupAlertBehavior(Notification.GROUP_ALERT_CHILDREN).setContentIntent(open).setAutoCancel(true).build())
    }

    fun snooze(context: Context, task: StudyTask) {
        // Inexact alarm needs no special access; power-saving modes may delay delivery.
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 600_000L, alarm(context, task.id))
        show(context, task, "${task.name}\n10 dakika ertelendi. Güç tasarrufunda hatırlatma gecikebilir.", actions = false)
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "dismiss") { TaskNotifications.refreshSummary(context); return }
        val id = intent.getIntExtra("task_id", -1)
        val tasks = TaskStore.load(context)
        val task = tasks.firstOrNull { it.id == id && !it.done } ?: return
        when (intent.action) {
            "complete" -> {
                if (TaskStore.save(context, tasks.map { if (it.id == id) it.copy(done = true) else it })) TaskNotifications.cancel(context, id)
            }
            "snooze" -> TaskNotifications.snooze(context, task)
            "remind" -> TaskNotifications.show(context, task)
        }
    }
}
