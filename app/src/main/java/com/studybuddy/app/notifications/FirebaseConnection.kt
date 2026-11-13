package com.studybuddy.app.notifications

import android.app.*
import android.content.*
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.studybuddy.app.R
import org.json.JSONArray
import org.json.JSONObject

data class PushAnnouncement(val id: String, val title: String, val body: String, val course: String, val receivedAt: Long, val read: Boolean = false)

object FirebaseConnection {
    const val CHANNEL = "teacher_announcements"
    const val TOPIC = "school_announcements"
    fun prefs(context: Context) = context.getSharedPreferences("firebase_connection", Context.MODE_PRIVATE)
    fun initialize(context: Context): Boolean {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "Öğretmen duyuruları", NotificationManager.IMPORTANCE_HIGH))
        return runCatching { FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null }.getOrDefault(false)
    }
    fun refresh(context: Context, result: (String) -> Unit) {
        if (!initialize(context)) { result("Firebase yapılandırması eksik. Ekibin google-services.json dosyası gerekli."); return }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) { prefs(context).edit().putString("token", task.result).apply(); result("Bu cihaz FCM kaydını aldı. Gelen mesaj testi henüz yapılmadı.") }
            else result("Cihaz kaydı alınamadı. İnterneti, Play Services ve Firebase ayarlarını kontrol et.")
        }
    }
    fun subscribe(context: Context, subscribe: Boolean, result: (String) -> Unit) {
        if (!initialize(context)) { result("Önce Firebase yapılandırmasını ekleyin."); return }
        val task = if (subscribe) FirebaseMessaging.getInstance().subscribeToTopic(TOPIC) else FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC)
        task.addOnCompleteListener {
            if (it.isSuccessful) {
                prefs(context).edit().putBoolean("subscribed", subscribe).apply()
                result(if (subscribe) "Genel okul duyurularına abone olundu." else "Genel okul duyuruları aboneliği kapatıldı.")
            } else result("Abonelik değiştirilemedi. İnternet bağlantısını kontrol et.")
        }
    }
}

object PushInbox {
    fun prefs(context: Context) = context.getSharedPreferences("push_inbox", Context.MODE_PRIVATE)
    @Synchronized fun load(context: Context): List<PushAnnouncement> = runCatching {
        val a = JSONArray(prefs(context).getString("items", "[]"))
        List(a.length()) { i -> a.getJSONObject(i).let { PushAnnouncement(it.getString("id"), it.getString("title"), it.getString("body"), it.optString("course"), it.getLong("receivedAt"), it.optBoolean("read")) } }
    }.getOrDefault(emptyList())
    private fun save(context: Context, items: List<PushAnnouncement>) {
        val a = JSONArray()
        items.take(200).forEach { a.put(JSONObject().put("id", it.id).put("title", it.title).put("body", it.body).put("course", it.course).put("receivedAt", it.receivedAt).put("read", it.read)) }
        prefs(context).edit().putString("items", a.toString()).commit()
    }
    @Synchronized fun receive(context: Context, item: PushAnnouncement, notify: Boolean = true) {
        val items = load(context)
        if (items.any { it.id == item.id }) return
        save(context, listOf(item) + items)
        if (!notify || !FirebaseConnection.initialize(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled() || (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)) return
        val intent = Intent(context, PushInboxActivity::class.java).setData(android.net.Uri.parse("studybuddy://announcement/${android.net.Uri.encode(item.id)}")).putExtra("announcement_id", item.id)
        val open = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify("push:${item.id}", 9500, Notification.Builder(context, FirebaseConnection.CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_book).setContentTitle(item.title).setContentText(item.body)
            .setStyle(Notification.BigTextStyle().bigText(item.body)).setSubText(item.course.takeIf { it.isNotBlank() })
            .setContentIntent(open).setAutoCancel(true).build())
    }
    @Synchronized fun markRead(context: Context, id: String) { save(context, load(context).map { if (it.id == id) it.copy(read = true) else it }) }
}
