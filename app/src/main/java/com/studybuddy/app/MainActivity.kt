package com.studybuddy.app

import android.os.Bundle
import android.Manifest
import android.app.Notification
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.studybuddy.app.ui.theme.StudyBuddyTheme

private val Purple = Color(0xFF6754A7)
private val Dark = Color(0xFF392D57)
private val Lavender = Color(0xFFE9DFFF)
private val Pink = Color(0xFFFFDCEB)
private val Paper = Color(0xFFFFFBFF)
private val Soft = Color(0xFFF6F0FF)

private data class Notice(val title: String, val message: String, val kind: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.studybuddy.app.school.SchoolSchedule.reconcile(this)
        val pushMessageId = intent.getStringExtra("google.message_id")
        if (pushMessageId != null) {
            val title = intent.getStringExtra("title") ?: intent.getStringExtra("announcement_title")
            val body = intent.getStringExtra("body") ?: intent.getStringExtra("announcement_body")
            if (title != null && body != null) com.studybuddy.app.notifications.PushInbox.receive(this,
                com.studybuddy.app.notifications.PushAnnouncement(intent.getStringExtra("eventId") ?: pushMessageId, title, body, intent.getStringExtra("courseName").orEmpty(), System.currentTimeMillis()), notify = false)
            startActivity(Intent(this, com.studybuddy.app.notifications.PushInboxActivity::class.java))
        }
        setContent { StudyBuddyTheme { StudyBuddyApp() } }
    }
}

@Composable
private fun StudyBuddyApp() {
    var screen by remember { mutableStateOf("Login") }
    var studentName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val tasks = remember { mutableStateListOf<StudyTask>().apply { addAll(TaskStore.load(context)) } }
    DisposableEffect(context) {
        val prefs = TaskStore.preferences(context)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "tasks") {
                tasks.clear()
                tasks.addAll(TaskStore.load(context))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val notices = remember { mutableStateListOf(
        Notice("Assignment reminder", "Your Kotlin homework is coming up.", "Assignments"),
        Notice("Exam announcement", "Review your upcoming exam schedule.", "Exams"),
        Notice("Welcome to StudyBuddy", "Your study space is ready!", "General")
    ) }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFF0F7), Color(0xFFF3EDFF), Color(0xFFEAF8FF)))
        )
    ) {
        if (screen == "Login") {
            LoginPage(studentName, { studentName = it }) { screen = "Home" }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    when (screen) {
                        "Home" -> HomePage(studentName, tasks, notices, onAdd = { screen = "Tasks" }, onNotifications = { screen = "Notifications" })
                        "Tasks" -> TasksPage(tasks, onAdd = { name, course ->
                            val id = TaskStore.preferences(context).getInt("next_id", 4).coerceAtLeast((tasks.maxOfOrNull { it.id } ?: 0) + 1)
                            TaskStore.preferences(context).edit().putInt("next_id", id + 1).commit()
                            TaskStore.save(context, tasks + StudyTask(id, name, course))
                        }, onToggle = { id, checked ->
                            TaskStore.save(context, tasks.map { if (it.id == id) it.copy(done = checked) else it })
                            if (checked) com.studybuddy.app.notifications.TaskNotifications.cancel(context, id)
                        }, onDelete = { id ->
                            TaskStore.save(context, tasks.filterNot { it.id == id })
                            com.studybuddy.app.notifications.TaskNotifications.cancel(context, id)
                        })
                        "Notifications" -> NotificationsPage(notices, tasks, onAddTask = { screen = "Tasks" })
                        "Admin" -> AdminPage(onSend = { title, message, kind ->
                            notices.add(0, Notice(title, message, kind))
                            screen = "Notifications"
                        })
                    }
                }
                NavigationBar(containerColor = Paper, tonalElevation = 0.dp) {
                    listOf("Home" to "⌂", "Tasks" to "✓", "Notifications" to "♧", "Admin" to "⚙").forEach { (label, symbol) ->
                        NavigationBarItem(
                            selected = screen == label,
                            onClick = { screen = label },
                            icon = { Text(symbol, fontSize = 22.sp) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Lavender, selectedIconColor = Purple, selectedTextColor = Purple)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginPage(name: String, onNameChange: (String) -> Unit, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✦", fontSize = 76.sp, color = Purple)
        Text("StudyBuddy", fontSize = 35.sp, fontWeight = FontWeight.Bold, color = Dark)
        Spacer(Modifier.height(10.dp))
        Text("Your little space for big goals", color = Purple)
        Spacer(Modifier.height(30.dp))
        PrettyCard {
            Text("Welcome!", fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = Dark)
            Spacer(Modifier.height(5.dp))
            Text("Enter your name to explore the app prototype.", color = Color.Gray)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp)
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) { Text("Get Started  →") }
        }
        Spacer(Modifier.height(18.dp))
        Text("Design prototype · No real authentication yet", fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 20.dp)) {
        Text(title, fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Dark)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, color = Purple, fontSize = 14.sp)
    }
}

@Composable
private fun PrettyCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        content = { Column(Modifier.padding(18.dp), content = content) }
    )
}

@Composable
private fun HomePage(name: String, tasks: List<StudyTask>, notices: List<Notice>, onAdd: () -> Unit, onNotifications: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(16.dp))
        PageTitle("Hello, ${name.ifBlank { "student" }}! ✨", "Let's make today productive.")
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Lavender)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Your study overview", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Dark)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("${tasks.count { !it.done }}", fontSize = 30.sp, color = Purple, fontWeight = FontWeight.Bold)
                        Text("To do", fontSize = 13.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("${tasks.count { it.done }}", fontSize = 30.sp, color = Purple, fontWeight = FontWeight.Bold)
                        Text("Completed", fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Upcoming tasks", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Dark)
            TextButton(onClick = onAdd) { Text("+ Add task") }
        }
        PrettyCard {
            if (tasks.none { !it.done }) Text("All done — amazing work!", color = Purple)
            tasks.filter { !it.done }.take(3).forEachIndexed { index, task ->
                Text("•  ${task.name}", color = Dark, fontSize = 15.sp)
                Text("    ${task.course}", fontSize = 12.sp, color = Color.Gray)
                if (index < 2) Spacer(Modifier.height(14.dp))
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Latest updates", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Dark)
            TextButton(onClick = onNotifications) { Text("View all") }
        }
        PrettyCard {
            notices.take(2).forEach { notice ->
                Text("✦  ${notice.title}", fontWeight = FontWeight.SemiBold, color = Dark)
                Text(notice.message, fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun TasksPage(tasks: List<StudyTask>, onAdd: (String, String) -> Unit, onToggle: (Int, Boolean) -> Unit, onDelete: (Int) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    var taskName by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Spacer(Modifier.height(16.dp))
        PageTitle("My Tasks 📚", "Manage your homework and deadlines.")
        Button(onClick = { dialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Purple), shape = RoundedCornerShape(14.dp)) {
            Text("+  Add New Task")
        }
        Spacer(Modifier.height(17.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tasks, key = { it.id }) { task ->
                PrettyCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = task.done, onCheckedChange = { onToggle(task.id, it) })
                        Column(Modifier.weight(1f)) {
                            Text(task.name, fontWeight = FontWeight.SemiBold, color = Dark, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(task.course.ifBlank { "General" }, fontSize = 12.sp, color = Purple)
                        }
                        TextButton(onClick = { onDelete(task.id) }) { Text("Delete") }
                    }
                }
            }
        }
    }
    if (dialog) {
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("New Task") },
            text = {
                Column {
                    OutlinedTextField(taskName, { taskName = it }, label = { Text("Task title") }, singleLine = true)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(course, { course = it }, label = { Text("Course") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(enabled = taskName.isNotBlank(), onClick = {
                onAdd(taskName.trim(), course.trim())
                taskName = ""; course = ""; dialog = false
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { dialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsPage(notices: List<Notice>, tasks: List<StudyTask>, onAddTask: () -> Unit) {
    var showReminder by remember { mutableStateOf(false) }
    var reminderTaskId by remember { mutableStateOf<Int?>(null) }
    var showAnnouncements by remember { mutableStateOf(false) }
    val schoolContext = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(16.dp))
        PageTitle("Bildirimler", "Hatırlatmaların tek bir yerde.")
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Lavender)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Derslerim", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Dark)
                Text("Ders saatlerini ve devam eden derslerini takip et.", color = Purple)
                Button(onClick = { schoolContext.startActivity(Intent(schoolContext, com.studybuddy.app.school.SchoolLessonsActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Dersleri görüntüle") }
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = { schoolContext.startActivity(Intent(schoolContext, com.studybuddy.app.notifications.PushInboxActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Öğretmen duyuruları") }
        Spacer(Modifier.height(12.dp))
        com.studybuddy.app.notifications.NotificationDemoCard(
            tasks = tasks, onAddTask = onAddTask,
            onPlanReminder = { id -> reminderTaskId = id; showReminder = true }
        )
        if (showReminder) {
            ModalBottomSheet(onDismissRequest = { showReminder = false }, containerColor = Paper) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                    TaskReminderCard(tasks, reminderTaskId)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { showAnnouncements = !showAnnouncements }, modifier = Modifier.fillMaxWidth()) {
            Text("Duyurular (${notices.size})  ${if (showAnnouncements) "−" else "+"}", color = Purple)
        }
        if (showAnnouncements) notices.forEach { notice ->
                PrettyCard {
                    Surface(color = if (notice.kind == "Exams") Pink else Soft, shape = RoundedCornerShape(10.dp)) {
                        Text(notice.kind, color = Purple, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(notice.title, fontWeight = FontWeight.Bold, color = Dark, fontSize = 17.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(notice.message, color = Color.DarkGray, fontSize = 14.sp)
                }
                Spacer(Modifier.height(13.dp))
        }
    }
}

// The user chooses a real task from the Tasks screen before testing the local notification.
@Composable
private fun TaskReminderCard(tasks: List<StudyTask>, initialTaskId: Int? = null) {
    val context = LocalContext.current
    val activeTasks = tasks.filterNot { it.done }
    var selectedTaskId by remember { mutableStateOf(initialTaskId ?: activeTasks.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }
    val selectedTask = activeTasks.firstOrNull { it.id == selectedTaskId }
    var chosenTime by remember {
        mutableLongStateOf(Calendar.getInstance().apply { add(Calendar.MINUTE, 10) }.timeInMillis)
    }
    val formattedTime = remember(chosenTime) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(chosenTime)
    }
    var pendingSchedule by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }

    fun trySchedule() {
        val task = selectedTask ?: return
        if (!com.studybuddy.app.notifications.DemoNotifications.enabled(context, "studybuddy_reminders")) {
            feedback = "Ödev bildirimleri kapalı. Bildirim ayarlarından izin ver."
            return
        }
        if (chosenTime <= System.currentTimeMillis()) {
            Toast.makeText(context, "Gelecek bir tarih ve saat seç.", Toast.LENGTH_SHORT).show()
            return
        }
        val result = scheduleTaskNotification(context, task, chosenTime)
        feedback = if (result) "${task.name}\n$formattedTime için kaydedildi." else "Alarm iznini açıp Hatırlatmayı kaydet'e tekrar bas."
        Toast.makeText(
            context,
            if (result) "Hatırlatma kaydedildi: $formattedTime"
            else "Alarm iznini açıp yeniden kaydet.",
            Toast.LENGTH_LONG
        ).show()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingSchedule) trySchedule()
        else if (!granted) Toast.makeText(context, "Ayarlardan bildirimlere izin ver.", Toast.LENGTH_LONG).show()
        pendingSchedule = false
    }

    PrettyCard {
        Text("Hatırlatma planla", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Dark)
        Spacer(Modifier.height(8.dp))
        Text("Ödevini ve hatırlatma zamanını seç.", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedTask?.let { "${it.name} · ${it.course}" } ?: "Ödev seç ▼")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                activeTasks.forEach { task ->
                    DropdownMenuItem(
                        text = { Text("${task.name} · ${task.course}") },
                        onClick = { selectedTaskId = task.id; expanded = false }
                    )
                }
            }
        }
        if (activeTasks.isEmpty()) {
            Text("Önce Ödevler bölümünden bir ödev ekle.", color = Purple, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("Hatırlatma: $formattedTime", color = Dark)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val c = Calendar.getInstance().apply { timeInMillis = chosenTime }
                DatePickerDialog(context, { _, year, month, day ->
                    val selected = Calendar.getInstance().apply { timeInMillis = chosenTime }
                    selected.set(Calendar.YEAR, year)
                    selected.set(Calendar.MONTH, month)
                    selected.set(Calendar.DAY_OF_MONTH, day)
                    chosenTime = selected.timeInMillis
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }
        ) { Text("Tarih seç") }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val c = Calendar.getInstance().apply { timeInMillis = chosenTime }
                TimePickerDialog(context, { _, hour, minute ->
                    val selected = Calendar.getInstance().apply { timeInMillis = chosenTime }
                    selected.set(Calendar.HOUR_OF_DAY, hour)
                    selected.set(Calendar.MINUTE, minute)
                    selected.set(Calendar.SECOND, 0)
                    selected.set(Calendar.MILLISECOND, 0)
                    chosenTime = selected.timeInMillis
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
            }
        ) { Text("Saat seç") }
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = selectedTask != null && chosenTime > System.currentTimeMillis(),
            onClick = {
                if (Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    pendingSchedule = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else trySchedule()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
            shape = RoundedCornerShape(15.dp)
        ) { Text("Hatırlatmayı kaydet") }
        if (feedback.isNotBlank()) Text(feedback, color = Purple, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            "Alarm izni istenirse izin verip yeniden kaydet.",
            color = Color.Gray, fontSize = 11.sp
        )
    }
}

@Composable
private fun AdminPage(onSend: (String, String, String) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("General") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(16.dp))
        PageTitle("Öğretmen paneli", "Derslerini ve duyurularını yönet.")
        OutlinedButton(onClick = { context.startActivity(Intent(context, com.studybuddy.app.notifications.PushInboxActivity::class.java).putExtra("setup", true)) }, modifier = Modifier.fillMaxWidth()) { Text("Firebase bağlantısı") }
        Button(onClick = { context.startActivity(Intent(context, com.studybuddy.app.school.SchoolLessonsActivity::class.java).putExtra("teacher_mode", true)) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Purple)) { Text("Ders ekle ve yönet") }
        Spacer(Modifier.height(16.dp))
        PrettyCard {
            Text("Yeni duyuru", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Dark)
            Spacer(Modifier.height(15.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(Modifier.height(14.dp))
            Text("Category", color = Dark, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("General", "Exams", "Assignments").forEach { option ->
                    FilterChip(selected = kind == option, onClick = { kind = option }, label = { Text(option, fontSize = 11.sp) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSend(title.trim(), message.trim(), kind); title = ""; message = "" },
                enabled = title.isNotBlank() && message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)
            ) { Text("Preview Announcement →") }
        }
        Spacer(Modifier.height(12.dp))
        Text("Demo: announcements are in-app only. Teacher-to-student FCM delivery comes later.", fontSize = 12.sp, color = Color.Gray)
    }
}

private fun scheduleTaskNotification(context: Context, task: StudyTask, whenMillis: Long): Boolean {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (whenMillis <= System.currentTimeMillis()) return false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !alarmManager.canScheduleExactAlarms()
    ) {
        context.startActivity(
            Intent(
                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                android.net.Uri.parse("package:${context.packageName}")
            )
        )
        return false
    }

    val requestCode = task.id
    val broadcastIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
        putExtra("task_name", task.name)
        putExtra("task_id", task.id)
        putExtra("task_course", task.course)
        putExtra("notification_id", requestCode)
        putExtra("scheduled_time", whenMillis)
    }
    val broadcastPendingIntent = PendingIntent.getBroadcast(
        context, requestCode, broadcastIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val showPendingIntent = PendingIntent.getActivity(
        context, requestCode,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return try {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, broadcastPendingIntent)
        true
    } catch (_: SecurityException) { false }
}

// This is a LOCAL Android notification demo, not a remote FCM push message.
private fun showTaskReminderNotification(context: Context, task: StudyTask) {
    com.studybuddy.app.notifications.TaskNotifications.show(context, task)
}
