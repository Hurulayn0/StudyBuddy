package com.studybuddy.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class StudyBuddyMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        FirebaseConnection.prefs(this).edit().putString("token", token).apply()
        // Authenticated user/device registration with the backend is the next integration stage.
    }
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: return
        val body = data["body"] ?: message.notification?.body ?: ""
        val id = data["eventId"] ?: message.messageId ?: java.util.UUID.randomUUID().toString()
        PushInbox.receive(this, PushAnnouncement(id, title.take(200), body.take(6000), data["courseName"].orEmpty().take(200), System.currentTimeMillis()))
    }
}
