package com.studybuddy.app.notifications

import android.app.*
import android.content.*
import android.os.SystemClock
import android.os.Build
import android.provider.Settings
import com.studybuddy.app.LessonActivity
import java.util.Locale

object LessonEvents {
    private fun goalAlarm(context: Context) = PendingIntent.getBroadcast(context, 8300,
        Intent(context, LessonEventReceiver::class.java).setAction("goal").putExtra("stopwatch", true),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun goalMillis(context: Context) = prefs(context).getInt("goal_minutes", 25).coerceIn(1, 240) * 60_000L

    fun checkGoal(context: Context) {
        val state = state(context)
        val p = prefs(context)
        if (state.phase !in listOf("running", "paused", "finished") || p.getBoolean("goal_reached", false) ||
            state.elapsed(SystemClock.elapsedRealtime()) < goalMillis(context)) return
        // Claim once before posting, so the screen and alarm cannot send duplicate achievements.
        p.edit().putBoolean("goal_reached", true).commit()
        context.getSystemService(AlarmManager::class.java).cancel(goalAlarm(context))
        if (!DemoNotifications.enabled(context, "study_goals")) return
        val open = PendingIntent.getActivity(context, 8301, Intent(context, LessonActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(NotificationManager::class.java).notify(8301,
            Notification.Builder(context, "study_goals").setSmallIcon(com.studybuddy.app.R.drawable.ic_notification_book)
                .setContentTitle("Çalışma hedefine ulaştın!")
                .setContentText("${p.getString("course", "Ders")} · ${p.getInt("goal_minutes", 25)} dakika tamamlandı.")
                .setContentIntent(open).setAutoCancel(true).build())
    }

    private fun scheduleGoal(context: Context, state: StopwatchState) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.cancel(goalAlarm(context))
        if (state.phase != "running" || prefs(context).getBoolean("goal_reached", false)) return
        val remaining = goalMillis(context) - state.elapsed(SystemClock.elapsedRealtime())
        if (remaining <= 0) { checkGoal(context); return }
        val trigger = SystemClock.elapsedRealtime() + remaining
        try {
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms())
                alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, goalAlarm(context))
            else alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, goalAlarm(context))
        } catch (_: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, goalAlarm(context))
        }
    }
    fun prefs(context: Context) = context.getSharedPreferences("lesson_stopwatch", Context.MODE_PRIVATE)
    private fun boot(context: Context) = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    fun state(context: Context): StopwatchState {
        val p = prefs(context)
        val state = StopwatchState(p.getString("phase", "idle") ?: "idle", p.getLong("elapsed", 0), p.getLong("anchor", 0))
        // A reboot invalidates elapsedRealtime anchors. Preserve the last saved amount as paused.
        return if (state.phase == "running" && p.getInt("boot", -1) != boot(context)) state.copy(phase = "paused", anchor = 0) else state
    }
    fun format(millis: Long): String {
        val seconds = millis / 1000
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    }
    private fun save(context: Context, state: StopwatchState): Boolean = prefs(context).edit()
        .putString("phase", state.phase).putLong("elapsed", state.accumulated).putLong("anchor", state.anchor).putInt("boot", boot(context)).commit()

    fun act(context: Context, action: String, fromService: Boolean = false) {
        if (action == "goal") { checkGoal(context); return }
        if (!fromService) { StudySessionService.request(context, action); return }
        val now = SystemClock.elapsedRealtime()
        val state = state(context)
        val updated = when (action) {
            "start" -> if (state.phase == "idle" || state.phase == "finished") state.start(now) else state
            "pause" -> state.pause(now)
            "resume" -> state.resume(now)
            "finish" -> state.finish(now)
            else -> return
        }
        if (updated == state) return
        if (!save(context, updated)) return
        if (action == "start" && state.phase in listOf("idle", "finished")) {
            prefs(context).edit().putBoolean("goal_reached", false).commit()
            context.getSystemService(NotificationManager::class.java).cancel(8301)
        }
        scheduleGoal(context, updated)
        checkGoal(context)
        if (updated.phase == "finished") context.getSystemService(NotificationManager::class.java).cancel(8100)
        else show(context, updated)
    }
    fun clearOldPlan(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        listOf("start" to 8101, "end" to 8102).forEach { (action, id) ->
            alarms.cancel(PendingIntent.getBroadcast(context, id, Intent(context, LessonEventReceiver::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        context.getSharedPreferences("lesson_session", Context.MODE_PRIVATE).edit().clear().apply()
    }
    private fun show(context: Context, state: StopwatchState) {
        if (DemoNotifications.enabled(context, "demo_lessons")) context.getSystemService(NotificationManager::class.java).notify(8100, notification(context, state))
    }
    fun notification(context: Context, state: StopwatchState): Notification {
        DemoNotifications.channels(context)
        val open = PendingIntent.getActivity(context, 8100, Intent(context, LessonActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = Notification.Builder(context, "demo_lessons")
            .setSmallIcon(com.studybuddy.app.R.drawable.ic_notification_book)
            .setContentTitle(if (state.phase == "running") "${prefs(context).getString("course", "Ders")} · Çalışılıyor" else "${prefs(context).getString("course", "Ders")} · Duraklatıldı")
            .setContentText(if (state.phase == "running") "Çalışma süresi" else "Çalışma süresi: ${format(state.accumulated)}")
            .setContentIntent(open).setOngoing(true).setAutoCancel(false).setOnlyAlertOnce(true)
        if (state.phase == "running") builder.setWhen(System.currentTimeMillis() - state.elapsed(SystemClock.elapsedRealtime())).setUsesChronometer(true)
        else builder.setShowWhen(false)
        val actions = listOf((if (state.phase == "running") "pause" else "resume") to (if (state.phase == "running") "Duraklat" else "Devam et"), "finish" to "Bitir")
        actions.forEach { (action, label) ->
            val intent = PendingIntent.getForegroundService(context, 8200, Intent(context, StudySessionService::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(Notification.Action.Builder(null, label, intent).build())
        }
        return builder.build()
    }
}

class LessonEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra("stopwatch", false)) LessonEvents.act(context, intent.action ?: "")
    }
}
