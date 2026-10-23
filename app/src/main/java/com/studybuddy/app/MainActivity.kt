package com.studybuddy.app

import android.os.Bundle
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
import com.studybuddy.app.ui.theme.StudyBuddyTheme

private val Purple = Color(0xFF6754A7)
private val Dark = Color(0xFF392D57)
private val Lavender = Color(0xFFE9DFFF)
private val Pink = Color(0xFFFFDCEB)
private val Paper = Color(0xFFFFFBFF)
private val Soft = Color(0xFFF6F0FF)

private data class StudyTask(val id: Int, val name: String, val course: String, val done: Boolean = false)
private data class Notice(val title: String, val message: String, val kind: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudyBuddyTheme { StudyBuddyApp() } }
    }
}

@Composable
private fun StudyBuddyApp() {
    var screen by remember { mutableStateOf("Login") }
    var studentName by remember { mutableStateOf("") }
    val tasks = remember { mutableStateListOf(
        StudyTask(1, "Finish Kotlin homework", "Mobile Programming"),
        StudyTask(2, "Review database notes", "Database Systems"),
        StudyTask(3, "Prepare project sketches", "Mobile Programming")
    ) }
    val notices = remember { mutableStateListOf(
        Notice("Assignment reminder", "Your Kotlin homework is coming up.", "Assignments"),
        Notice("Exam announcement", "Review your upcoming exam schedule.", "Exams"),
        Notice("Welcome to StudyBuddy", "Your study space is ready!", "General")
    ) }
    var nextId by remember { mutableIntStateOf(4) }

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
                            tasks.add(StudyTask(nextId++, name, course))
                        }, onToggle = { id, checked ->
                            val i = tasks.indexOfFirst { it.id == id }
                            if (i >= 0) tasks[i] = tasks[i].copy(done = checked)
                        }, onDelete = { id -> tasks.removeAll { it.id == id } })
                        "Notifications" -> NotificationsPage(notices)
                        "Admin" -> AdminPage { title, message, kind ->
                            notices.add(0, Notice(title, message, kind))
                            screen = "Notifications"
                        }
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

@Composable
private fun NotificationsPage(notices: List<Notice>) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Spacer(Modifier.height(16.dp))
        PageTitle("Notifications 🔔", "Important reminders in one place.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            items(notices) { notice ->
                PrettyCard {
                    Surface(color = if (notice.kind == "Exams") Pink else Soft, shape = RoundedCornerShape(10.dp)) {
                        Text(notice.kind, color = Purple, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(notice.title, fontWeight = FontWeight.Bold, color = Dark, fontSize = 17.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(notice.message, color = Color.DarkGray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun AdminPage(onSend: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("General") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(16.dp))
        PageTitle("Admin Panel ✉", "Create a sample class announcement.")
        PrettyCard {
            Text("New announcement", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Dark)
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
        Spacer(Modifier.height(15.dp))
        Text("Prototype only: this adds a message inside the app. FCM sending, authentication, and server-side admin permissions will be implemented later.", fontSize = 12.sp, color = Color.Gray)
    }
}
