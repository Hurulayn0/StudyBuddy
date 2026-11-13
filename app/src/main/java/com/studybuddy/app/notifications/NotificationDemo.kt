package com.studybuddy.app.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.studybuddy.app.MainActivity

private const val LESSON = "demo_lessons"





object DemoNotifications {
    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)

    fun channels(context: Context) {
        manager(context).deleteNotificationChannel("demo_homework")
        listOf(7100, 7200, 7201, 7202, 7299, 7300, 7301).forEach { manager(context).cancel(it) }
        manager(context).createNotificationChannels(listOf(
            NotificationChannel(LESSON, "Çalışma süresi", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel("daily_reminder", "Daily Reminder", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel("studybuddy_reminders", "Ödev hatırlatmaları", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel("study_goals", "Çalışma hedefleri", NotificationManager.IMPORTANCE_HIGH)
        ))
    }

    fun enabled(context: Context, channel: String): Boolean {
        channels(context)
        return (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            manager(context).areNotificationsEnabled() && manager(context).getNotificationChannel(channel)?.importance != NotificationManager.IMPORTANCE_NONE
    }

}

@Composable
fun NotificationDemoCard(tasks: List<com.studybuddy.app.StudyTask>, onAddTask: () -> Unit, onPlanReminder: (Int?) -> Unit = {}) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var taskMenu by remember { mutableStateOf(false) }
    val activeTasks = tasks.filterNot { it.done }
    val selectedTask = activeTasks.firstOrNull { it.id == selectedTaskId } ?: activeTasks.firstOrNull()
    val taskTitle = selectedTask?.name
    val pendingCount = activeTasks.size
    var status by remember { mutableStateOf("") }

    var lessonActive by remember { mutableStateOf((LessonEvents.state(context).phase in listOf("running", "paused"))) }
    var settingsExpanded by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context) {
        val prefs = LessonEvents.prefs(context)
        LessonEvents.clearOldPlan(context)
        lessonActive = (LessonEvents.state(context).phase in listOf("running", "paused"))
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            lessonActive = (LessonEvents.state(context).phase in listOf("running", "paused"))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun permissionStatus(): String {
        DemoNotifications.channels(context)
        val alarms = context.getSystemService(AlarmManager::class.java)
        return "Ders bildirimleri: ${if (DemoNotifications.enabled(context, LESSON)) "açık" else "kapalı"}\nÖdev bildirimleri: ${if (DemoNotifications.enabled(context, "studybuddy_reminders")) "açık" else "kapalı"}\nKesin alarm erişimi: ${if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) "açık" else "kapalı"}"
    }

    fun sendReminder(all: Boolean = false) {
        if (!DemoNotifications.enabled(context, "studybuddy_reminders")) {
            status = "Ödev bildirimleri kapalı. Ayarlardan izin ver."
            return
        }
        if (all) {
            val count = activeTasks.count { TaskNotifications.show(context, it) }
            status = if (count > 0) "$count ödev için hatırlatma gönderildi." else "Bildirim gönderilemedi. İzinleri kontrol et."
            return
        }
        val task = selectedTask ?: return
        status = if (TaskNotifications.show(context, task)) "Hatırlatma gönderildi." else "Bildirim gösterilemedi. Bildirim ayarlarını kontrol et."
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pending
        pending = null
        if (granted && action != null) sendReminder(action == "all")
        else status = "Bildirim izni verilmedi. Bildirim ayarlarından açabilirsiniz."
    }
    DisposableEffect(owner, settingsExpanded) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && settingsExpanded) status = permissionStatus()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val purple = Color(0xFF6754A7)
    val ink = Color(0xFF392D57)
    fun launchDemo(action: String) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pending = action
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else sendReminder(action == "all")
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DailyReminderCard()
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFF))) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Ödev hatırlatması", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ink)
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFE7EE)) {
                        Text("$pendingCount bekleyen", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, color = purple)
                    }
                }
                Box {
                    OutlinedButton(onClick = { taskMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text(taskTitle ?: "Bekleyen ödev yok", modifier = Modifier.weight(1f), color = ink)
                        Text("⌄", color = purple)
                    }
                    DropdownMenu(expanded = taskMenu, onDismissRequest = { taskMenu = false }) {
                        activeTasks.forEach { task ->
                            DropdownMenuItem(text = { Column { Text(task.name); Text(task.course, fontSize = 12.sp) } }, onClick = { selectedTaskId = task.id; taskMenu = false })
                        }
                        DropdownMenuItem(text = { Text("+ Ödev ekle") }, onClick = { taskMenu = false; onAddTask() })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { launchDemo("homework") }, enabled = taskTitle != null, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = purple)) { Text("Bildirim gönder") }
                    OutlinedButton(onClick = { onPlanReminder(selectedTask?.id) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) { Text("Zamanla", color = purple) }
                }
            }
        }
        if (pendingCount > 1) TextButton(onClick = { launchDemo("all") }, modifier = Modifier.fillMaxWidth()) {
            Text("Tüm ödevleri hatırlat ($pendingCount)", color = purple)
        }
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE9DFFF))) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Çalışma süresi", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = ink)
                    Text("◷", fontSize = 34.sp, color = purple)
                }
                Text(if (lessonActive) "Çalışmana kaldığın yerden devam et." else "Dersini seç, süreyi başlat.", color = purple, fontSize = 14.sp)
                Button(onClick = { context.startActivity(Intent(context, com.studybuddy.app.LessonActivity::class.java)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = purple)) {
                    Text(if (lessonActive) "Kronometreye dön" else "Kronometreyi aç")
                }
            }
        }
        if (status.isNotBlank()) {
            Surface(color = Color(0xFFEDE5FA), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(status, Modifier.weight(1f), color = ink, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { status = "" }) { Text("×", color = purple, fontSize = 20.sp) }
                }
            }
        }
        OutlinedButton(onClick = {
            settingsExpanded = !settingsExpanded
            if (settingsExpanded) status = permissionStatus()
        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text(if (settingsExpanded) "İzinler ve ayarlar  −" else "İzinler ve ayarlar  +", color = purple)
        }
        if (settingsExpanded) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFF))) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    TextButton(onClick = { status = permissionStatus() }) { Text("İzinleri kontrol et", color = purple) }
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }) { Text("Bildirim ayarları  →", color = purple) }
                    if (Build.VERSION.SDK_INT >= 31) {
                        TextButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                        }) { Text("Alarm erişimi  →", color = purple) }
                    }
                }
            }
        }
    }
}
