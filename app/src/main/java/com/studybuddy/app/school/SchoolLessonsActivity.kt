package com.studybuddy.app.school

import android.Manifest
import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studybuddy.app.ui.theme.StudyBuddyTheme
import java.util.Calendar
import kotlinx.coroutines.delay

class SchoolLessonsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SchoolSchedule.reconcile(this)
        setContent { StudyBuddyTheme { LessonsPage() } }
    }
    @Composable
    private fun LessonsPage() {
        val context = this
        val teacherMode = intent.getBooleanExtra("teacher_mode", false)
        val focusId = intent.getIntExtra("lesson_id", -1)
        val prefs = SchoolSchedule.prefs(context)
        var revision by remember { mutableIntStateOf(0) }
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var adding by remember { mutableStateOf(false) }
        var name by remember { mutableStateOf("") }
        var teacher by remember { mutableStateOf("") }
        var room by remember { mutableStateOf("") }
        var start by remember { mutableLongStateOf(System.currentTimeMillis() + 600000) }
        var end by remember { mutableLongStateOf(System.currentTimeMillis() + 4200000) }
        var message by remember { mutableStateOf("") }
        DisposableEffect(prefs) {
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
        val lessons = remember(revision) { SchoolSchedule.load(context) }
        val visible = lessons.sortedWith(compareBy<SchoolLesson> { if (it.id == focusId) 0 else 1 }.thenBy { it.start })
        fun save() {
            if (!SchoolSchedule.exactAllowed(context)) { message = "Ders saatinde bildirim için alarm erişimini açıp tekrar kaydet."; return }
            if (SchoolSchedule.add(context, name, teacher, room, start, end)) {
                adding = false; name = ""; message = "Ders kaydedildi. Başlangıç ve bitiş bildirimleri ayarlandı."
            } else message = "Ders ve öğretmen adını doldur. Başlangıç gelecekte, bitiş başlangıçtan sonra olmalı."
        }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) save() else message = "Bildirim izni verilmedi. İzin verdikten sonra dersi kaydet."
        }
        fun pick(value: Long, update: (Long) -> Unit) {
            val c = Calendar.getInstance().apply { timeInMillis = value }
            DatePickerDialog(context, { _, y, m, d ->
                TimePickerDialog(context, { _, h, minute -> update(Calendar.getInstance().apply { set(y, m, d, h, minute, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis) }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        val purple = Color(0xFF6754A7)
        Surface(color = Color(0xFFF6F0FF)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TextButton(onClick = { finish() }) { Text("‹ Geri", color = purple) }
                Text(if (teacherMode) "Ders yönetimi" else "Derslerim", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF392D57))
                if (teacherMode) Button(onClick = { adding = !adding }, modifier = Modifier.fillMaxWidth()) { Text(if (adding) "Formu kapat" else "+ Ders ekle") }
                if (adding && teacherMode) Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(name, { name = it }, label = { Text("Ders adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(teacher, { teacher = it }, label = { Text("Öğretmen") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(room, { room = it }, label = { Text("Derslik") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedButton(onClick = { pick(start) { start = it } }, modifier = Modifier.fillMaxWidth()) { Text("Başlangıç · ${SchoolSchedule.time(start)}") }
                        OutlinedButton(onClick = { pick(end) { end = it } }, modifier = Modifier.fillMaxWidth()) { Text("Bitiş · ${SchoolSchedule.time(end)}") }
                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else save()
                        }, modifier = Modifier.fillMaxWidth()) { Text("Dersi kaydet") }
                    }
                }
                if (message.isNotBlank()) Text(message, color = purple)
                if (Build.VERSION.SDK_INT >= 31 && !SchoolSchedule.exactAllowed(context)) TextButton(onClick = { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))) }) { Text("Ders bildirimleri için alarm erişimi") }
                TextButton(onClick = { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)) }) { Text("Bildirim ayarları") }
                if (lessons.isEmpty()) Text(if (teacherMode) "Henüz ders eklenmedi." else "Öğretmenin eklediği dersler burada görünecek.")
                visible.forEach { lesson ->
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = if (lesson.status(now) == "Devam ediyor") Color(0xFFE9DFFF) else Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(lesson.status(now), color = purple, style = MaterialTheme.typography.labelLarge)
                            Text(lesson.name, style = MaterialTheme.typography.titleLarge)
                            Text(lesson.teacher + if (lesson.room.isNotBlank()) " · ${lesson.room}" else "")
                            Text("${SchoolSchedule.time(lesson.start)}\n${SchoolSchedule.time(lesson.end)}")
                            if (teacherMode) TextButton(onClick = { SchoolSchedule.delete(context, lesson.id) }) { Text("Dersi kaldır") }
                        }
                    }
                }
            }
        }
    }
}
