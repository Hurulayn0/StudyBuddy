package com.studybuddy.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studybuddy.app.notifications.TaskNotifications

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("task_id", -1)
        val task = TaskStore.load(context).firstOrNull { it.id == id && !it.done } ?: return
        TaskNotifications.show(context, task)
    }
}
