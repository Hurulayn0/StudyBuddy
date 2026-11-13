package com.studybuddy.app.school

import android.app.*
import android.content.*
import android.os.Build
import com.studybuddy.app.R
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

object SchoolSchedule {
    const val CHANNEL = "school_lessons"
    private const val COMPLETE = "school_lesson_end"
    fun prefs(context: Context) = context.getSharedPreferences("school_schedule", Context.MODE_PRIVATE)
    fun load(context: Context): List<SchoolLesson> {
        val a = JSONArray(prefs(context).getString("lessons", "[]"))
        return List(a.length()) { i -> a.getJSONObject(i).let { SchoolLesson(it.getInt("id"), it.getString("name"), it.getString("teacher"), it.getString("room"), it.getLong("start"), it.getLong("end"), it.optBoolean("started"), it.optBoolean("ended")) } }.sortedBy { it.start }
    }
    private fun save(context: Context, lessons: List<SchoolLesson>) {
        val a = JSONArray()
        lessons.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("teacher", it.teacher).put("room", it.room).put("start", it.start).put("end", it.end).put("started", it.started).put("ended", it.ended)) }
        prefs(context).edit().putString("lessons", a.toString()).commit()
    }
    fun exactAllowed(context: Context) = Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun channels(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(
            NotificationChannel(CHANNEL, "Ders başlangıcı ve devamı", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(COMPLETE, "Ders bitişleri", NotificationManager.IMPORTANCE_DEFAULT)))
    }
    fun time(value: Long) = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(value)
    private fun alarm(context: Context, id: Int, action: String) = PendingIntent.getBroadcast(context, id,
        Intent(context, SchoolLessonReceiver::class.java).setAction(action).putExtra("lesson_id", id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun schedule(context: Context, lesson: SchoolLesson) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        listOf("start" to lesson.start, "end" to lesson.end).forEach { (action, time) ->
            alarms.cancel(alarm(context, lesson.id, action))
            if (time > System.currentTimeMillis()) {
                try { if (exactAllowed(context)) {
                    if (action == "start") alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarm(context, lesson.id, action))
                    else alarms.setExact(AlarmManager.RTC_WAKEUP, time, alarm(context, lesson.id, action))
                }
                else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarm(context, lesson.id, action)) }
                catch (_: SecurityException) { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarm(context, lesson.id, action)) }
            }
        }
    }
    fun add(context: Context, name: String, teacher: String, room: String, start: Long, end: Long): Boolean {
        val id = prefs(context).getInt("next_id", 1)
        val lesson = SchoolLesson(id, name.trim(), teacher.trim(), room.trim(), start, end)
        if (!lesson.valid(System.currentTimeMillis())) return false
        prefs(context).edit().putInt("next_id", id + 1).commit()
        save(context, load(context) + lesson); schedule(context, lesson)
        return true
    }
    fun delete(context: Context, id: Int) {
        save(context, load(context).filterNot { it.id == id })
        listOf("start", "end").forEach { context.getSystemService(AlarmManager::class.java).cancel(alarm(context, id, it)) }
        context.getSystemService(NotificationManager::class.java).apply { cancel(200000 + id); cancel(300000 + id) }
    }
    private fun open(context: Context, id: Int) = PendingIntent.getActivity(context, id,
        Intent(context, SchoolLessonsActivity::class.java).putExtra("lesson_id", id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun allowed(context: Context, channel: String): Boolean {
        channels(context)
        val m = context.getSystemService(NotificationManager::class.java)
        return (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) && m.areNotificationsEnabled() && m.getNotificationChannel(channel).importance != NotificationManager.IMPORTANCE_NONE
    }
    private fun ongoing(context: Context, lesson: SchoolLesson) {
        if (!allowed(context, CHANNEL)) return
        context.getSystemService(NotificationManager::class.java).notify(200000 + lesson.id,
            Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification_book)
                .setContentTitle("${lesson.name} dersi devam ediyor")
                .setContentText("${time(lesson.start)} – ${time(lesson.end)} · ${lesson.room}")
                .setSubText("Ders başladı · ${lesson.teacher}").setContentIntent(open(context, lesson.id))
                .setOngoing(true).setAutoCancel(false).setOnlyAlertOnce(true).setTimeoutAfter((lesson.end - System.currentTimeMillis()).coerceAtLeast(1)).build())
    }
    fun event(context: Context, id: Int, action: String) {
        val lessons = load(context)
        val lesson = lessons.firstOrNull { it.id == id } ?: return
        val now = System.currentTimeMillis()
        if (action == "start" && now >= lesson.start && now < lesson.end && !lesson.ended) {
            if (!lesson.started) save(context, lessons.map { if (it.id == id) it.copy(started = true) else it })
            ongoing(context, lesson)
        } else if (now >= lesson.end && !lesson.ended) {
            save(context, lessons.map { if (it.id == id) it.copy(ended = true) else it })
            context.getSystemService(NotificationManager::class.java).cancel(200000 + id)
            if (allowed(context, COMPLETE)) context.getSystemService(NotificationManager::class.java).notify(300000 + id,
                Notification.Builder(context, COMPLETE).setSmallIcon(R.drawable.ic_notification_book)
                    .setContentTitle("${lesson.name} dersi bitti").setContentText("Ders notlarını ve verilen ödevleri kontrol et.")
                    .setContentIntent(open(context, id)).setAutoCancel(true).build())
        }
    }
    fun reconcile(context: Context) {
        val now = System.currentTimeMillis()
        load(context).forEach { lesson ->
            schedule(context, lesson)
            if (now >= lesson.start && now < lesson.end && !lesson.ended) event(context, lesson.id, "start")
            else if (now >= lesson.end) {
                context.getSystemService(NotificationManager::class.java).cancel(200000 + lesson.id)
                // Do not replay old end notifications after reboot or a time change.
                if (!lesson.ended) save(context, load(context).map { if (it.id == lesson.id) it.copy(ended = true) else it })
            }
        }
    }
}
class SchoolLessonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in listOf("start", "end")) SchoolSchedule.event(context, intent.getIntExtra("lesson_id", -1), intent.action ?: "")
        else SchoolSchedule.reconcile(context)
    }
}
