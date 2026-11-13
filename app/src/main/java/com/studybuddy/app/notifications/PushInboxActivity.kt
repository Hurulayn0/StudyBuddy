package com.studybuddy.app.notifications

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
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

class PushInboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra("announcement_id")?.let { PushInbox.markRead(this, it) }
        setContent { StudyBuddyTheme { InboxPage() } }
    }
    @Composable
    private fun InboxPage() {
        val context = this
        val setup = intent.getBooleanExtra("setup", false)
        var status by remember { mutableStateOf(if (FirebaseConnection.initialize(context)) "Firebase yapılandırması mevcut." else "Firebase yapılandırması henüz eklenmedi.") }
        var revision by remember { mutableIntStateOf(0) }
        var busy by remember { mutableStateOf(false) }
        var subscribed by remember { mutableStateOf(FirebaseConnection.prefs(context).getBoolean("subscribed", false)) }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { status = if (it) "Bildirim izni açık." else "Bildirim izni verilmedi." }
        DisposableEffect(Unit) {
            val p = PushInbox.prefs(context)
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
            p.registerOnSharedPreferenceChangeListener(listener)
            onDispose { p.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        val items = remember(revision) { PushInbox.load(context) }
        Surface(color = Color(0xFFF6F0FF)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TextButton(onClick = { finish() }) { Text("‹ Geri") }
                Text(if (setup) "Firebase bağlantısı" else "Öğretmen duyuruları", style = MaterialTheme.typography.headlineMedium)
                if (setup) {
                    Text(status)
                    Button(enabled = !busy, onClick = { busy = true; FirebaseConnection.refresh(context) { status = it; busy = false } }, modifier = Modifier.fillMaxWidth()) { Text("Cihaz bağlantısını kontrol et") }
                    OutlinedButton(onClick = {
                        val token = FirebaseConnection.prefs(context).getString("token", null)
                        if (token.isNullOrBlank()) status = "Önce cihaz bağlantısını kontrol et."
                        else { getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("FCM device token", token)); status = "Cihaz tokenı kopyalandı. Firebase Console'da test cihazı olarak kullanabilirsin." }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Cihaz tokenını kopyala") }
                    Row {
                        Text("Genel okul duyuruları", modifier = Modifier.weight(1f))
                        Switch(checked = subscribed, enabled = !busy, onCheckedChange = { next ->
                            busy = true
                            FirebaseConnection.subscribe(context, next) { status = it; subscribed = FirebaseConnection.prefs(context).getBoolean("subscribed", false); busy = false }
                        })
                    }
                    Text("Bu abonelik yalnız genel okul duyuruları içindir. Özel ders verileri için kullanıcı ve ders yetkilendirmesi sonraki adımdır.", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                }) { Text("Duyuru bildirim izni") }
                if (!setup && items.isEmpty()) Text("Gelen öğretmen duyuruları burada saklanacak.")
                items.forEach { item ->
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.course.isNotBlank()) Text(item.course, style = MaterialTheme.typography.labelLarge)
                            Text(item.title, style = MaterialTheme.typography.titleLarge)
                            Text(item.body)
                            if (!item.read) TextButton(onClick = { PushInbox.markRead(context, item.id) }) { Text("Okundu işaretle") }
                        }
                    }
                }
            }
        }
    }
}
