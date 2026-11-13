package com.studybuddy.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studybuddy.app.notifications.*
import com.studybuddy.app.ui.theme.StudyBuddyTheme
import kotlinx.coroutines.delay

class LessonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LessonEvents.clearOldPlan(this)
        setContent { StudyBuddyTheme { StopwatchPage() } }
    }
    @Composable
    private fun StopwatchPage() {
        val context = this
        val prefs = LessonEvents.prefs(context)
        var revision by remember { mutableIntStateOf(0) }
        var tick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        var course by remember { mutableStateOf(prefs.getString("course", "Mobil Programlama") ?: "Mobil Programlama") }
        var courses by remember { mutableStateOf(CourseStore.all(context)) }
        var courseMenu by remember { mutableStateOf(false) }
        var addCourse by remember { mutableStateOf(false) }
        var newCourse by remember { mutableStateOf("") }
        var goalMinutes by remember { mutableStateOf(prefs.getInt("goal_minutes", 25).toString()) }
        var notice by remember { mutableStateOf("") }
        var pending by remember { mutableStateOf<String?>(null) }
        DisposableEffect(prefs) {
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        val state = remember(revision) { LessonEvents.state(context) }
        LaunchedEffect(state.phase) {
            tick = SystemClock.elapsedRealtime()
            while (state.phase == "running") { tick = SystemClock.elapsedRealtime(); LessonEvents.checkGoal(context); delay(200) }
        }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (pending == "start") prefs.edit().putString("course", course).putInt("goal_minutes", goalMinutes.toIntOrNull()?.coerceIn(1, 240) ?: 25).commit()
            pending?.let { LessonEvents.act(context, it) }
            pending = null
            if (!granted) notice = "Kronometre çalışıyor. Bildirim için ayarlardan izin verin."
        }
        fun action(value: String) {
            if (value == "start" && goalMinutes.toIntOrNull() !in 1..240) { notice = "1–240 dakika arasında bir hedef gir."; return }
            if (value == "start" || value == "resume") {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    pending = value; launcher.launch(Manifest.permission.POST_NOTIFICATIONS); return
                }
                notice = if (DemoNotifications.enabled(context, "demo_lessons")) "" else "Bildirimler kapalı. Kronometreyi burada kullanabilirsiniz."
            }
            if (value == "start") prefs.edit().putString("course", course).putInt("goal_minutes", goalMinutes.toIntOrNull()?.coerceIn(1, 240) ?: 25).commit()
            LessonEvents.act(context, value)
        }
        val purple = Color(0xFF6754A7)
        Surface(color = Color(0xFFF6F0FF)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                TextButton(onClick = { finish() }, modifier = Modifier.align(Alignment.Start)) { Text("‹ Geri", color = purple) }
                Text("Çalışma süresi", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF392D57))
                Box {
                    OutlinedButton(onClick = { courseMenu = true }, enabled = state.phase == "idle" || state.phase == "finished", shape = RoundedCornerShape(12.dp)) { Text("$course  ⌄", color = purple) }
                    DropdownMenu(expanded = courseMenu, onDismissRequest = { courseMenu = false }) {
                        courses.forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = { course = name; courseMenu = false }) }
                        DropdownMenuItem(text = { Text("+ Ders ekle") }, onClick = { courseMenu = false; addCourse = true })
                    }
                }
                if (addCourse) AlertDialog(onDismissRequest = { addCourse = false }, title = { Text("Yeni ders") }, text = {
                    OutlinedTextField(newCourse, { newCourse = it }, label = { Text("Ders adı") }, singleLine = true)
                }, confirmButton = { TextButton(enabled = newCourse.isNotBlank(), onClick = {
                    CourseStore.add(context, newCourse); courses = CourseStore.all(context); course = newCourse.trim(); newCourse = ""; addCourse = false
                }) { Text("Ekle") } }, dismissButton = { TextButton(onClick = { addCourse = false }) { Text("Vazgeç") } })
                if (state.phase == "idle" || state.phase == "finished") {
                    OutlinedTextField(goalMinutes, { goalMinutes = it.filter(Char::isDigit).take(3) }, label = { Text("Çalışma hedefi (dakika)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(15, 25, 45).forEach { minutes ->
                            FilterChip(selected = goalMinutes == minutes.toString(), onClick = { goalMinutes = minutes.toString() }, label = { Text("$minutes dk") })
                        }
                    }
                }
                Surface(shape = CircleShape, color = Color(0xFFE9DFFF), modifier = Modifier.sizeIn(maxWidth = 300.dp).fillMaxWidth().aspectRatio(1f)) {
                    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(when (state.phase) { "running" -> "Çalışılıyor"; "paused" -> "Duraklatıldı"; "finished" -> "Toplam çalışma süresi"; else -> "Hazır olduğunda başlat" }, color = purple, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(LessonEvents.format(state.elapsed(tick)), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF392D57))
                        Text("saat : dakika : saniye", color = purple, fontSize = 12.sp)
                    }
                }
                if (state.phase != "idle") {
                    val target = LessonEvents.goalMillis(context)
                    val elapsed = state.elapsed(tick)
                    Text(if (prefs.getBoolean("goal_reached", false)) "Hedef tamamlandı · ${prefs.getInt("goal_minutes", 25)} dakika" else "Hedef: ${prefs.getInt("goal_minutes", 25)} dk · Kalan: ${LessonEvents.format((target - elapsed).coerceAtLeast(0))}", color = purple)
                }
                if (state.phase == "idle" || state.phase == "finished") Text("Hedefine ulaştığında haber verelim. İstersen çalışmaya devam edebilirsin.", color = purple, fontSize = 13.sp)
                if (Build.VERSION.SDK_INT >= 31 && !getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()) {
                    Text("Arka planda hedef bildirimi gecikebilir.", color = purple, fontSize = 12.sp)
                    TextButton(onClick = { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:$packageName"))) }) { Text("Zamanında hatırlatma için izin ver") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { action(when (state.phase) { "running" -> "pause"; "paused" -> "resume"; else -> "start" }) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = purple)) {
                        Text(when (state.phase) { "running" -> "Duraklat"; "paused" -> "Devam et"; "finished" -> "Yeni oturum"; else -> "Başlat" })
                    }
                    if (state.phase == "running" || state.phase == "paused") OutlinedButton(onClick = { action("finish") }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) { Text("Bitir", color = purple) }
                }
                val serviceError = remember(revision) { prefs.getString("service_error", null) }
                if (serviceError != null) Text(serviceError, color = purple)
                if (notice.isNotBlank()) {
                    Text(notice, color = purple)
                    TextButton(onClick = { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)) }) { Text("Bildirim ayarları") }
                }
            }
        }
    }
}
