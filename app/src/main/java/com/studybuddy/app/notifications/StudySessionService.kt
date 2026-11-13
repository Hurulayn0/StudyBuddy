package com.studybuddy.app.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/** Owns the user-started study session and monitors goal completion outside the Activity. */
class StudySessionService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val monitor = object : Runnable {
        override fun run() {
            if (LessonEvents.state(this@StudySessionService).phase == "running") {
                LessonEvents.checkGoal(this@StudySessionService)
                handler.postDelayed(this, 1000)
            }
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val before = LessonEvents.state(this)
        if (intent == null && before.phase != "running") { stopSelf(); return START_NOT_STICKY }
        val notification = LessonEvents.notification(this, before)
        if (Build.VERSION.SDK_INT >= 34) startForeground(8100, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(8100, notification)
        intent?.action?.let { LessonEvents.act(this, it, fromService = true) }
        handler.removeCallbacks(monitor)
        val after = LessonEvents.state(this)
        if (after.phase == "running") {
            getSystemService(android.app.NotificationManager::class.java).notify(8100, LessonEvents.notification(this, after))
            handler.post(monitor)
            return START_STICKY
        }
        // A paused session needs no running service. Keep its resume/finish notification.
        stopForeground(if (after.phase == "paused") STOP_FOREGROUND_DETACH else STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
    }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }
    companion object {
        fun request(context: Context, action: String) {
            try {
                context.startForegroundService(Intent(context, StudySessionService::class.java).setAction(action))
                LessonEvents.prefs(context).edit().remove("service_error").apply()
            } catch (_: IllegalStateException) {
                LessonEvents.prefs(context).edit().putString("service_error", "Çalışmayı başlatmak için uygulamayı açıp tekrar dene.").apply()
            } catch (_: SecurityException) {
                LessonEvents.prefs(context).edit().putString("service_error", "Çalışma servisi başlatılamadı. Uygulamayı güncelleyip tekrar dene.").apply()
            }
        }
    }
}
