package com.studybuddy.app

import android.app.Application
import com.studybuddy.app.notifications.FirebaseConnection

class StudyBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseConnection.initialize(this)
    }
}
