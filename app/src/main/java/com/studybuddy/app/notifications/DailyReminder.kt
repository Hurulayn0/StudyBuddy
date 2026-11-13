package com.studybuddy.app.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.studybuddy.app.MainActivity
import java.util.Calendar
import java.util.Locale

object DailyReminder {
    fun prefs(context: Context) = context.getSharedPreferences("daily_reminder", Context.MODE_PRIVATE)
    private fun pending(context: Context) = PendingIntent.getBroadcast(context, 9001, Intent(context, DailyReminderReceiver::class.java).setAction("daily"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun schedule(context: Context) {
        val p = prefs(context)
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(pending(context))
        if (!p.getBoolean("enabled", false)) return
        val time = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, p.getInt("hour", 20)); set(Calendar.MINUTE, p.getInt("minute", 0)); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        try {
            if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending(context))
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending(context))
        } catch (_: SecurityException) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending(context)) }
    }
    fun show(context: Context) {
        if (!DemoNotifications.enabled(context, "daily_reminder")) return
        val open = PendingIntent.getActivity(context, 9002, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(NotificationManager::class.java).notify(9002, Notification.Builder(context, "daily_reminder")
            .setSmallIcon(com.studybuddy.app.R.drawable.ic_notification_book).setContentTitle("Daily Reminder")
            .setContentText("Bugünkü çalışma hedefin için biraz zaman ayır.")
            .setContentIntent(open).setAutoCancel(true).build())
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!DailyReminder.prefs(context).getBoolean("enabled", false)) return
        if (intent.action == "daily") DailyReminder.show(context)
        DailyReminder.schedule(context)
    }
}

@Composable
fun DailyReminderCard() {
    val context = LocalContext.current
    val p = DailyReminder.prefs(context)
    var enabled by remember { mutableStateOf(p.getBoolean("enabled", false)) }
    var hour by remember { mutableIntStateOf(p.getInt("hour", 20)) }
    var minute by remember { mutableIntStateOf(p.getInt("minute", 0)) }
    var message by remember { mutableStateOf("") }
    fun enable() {
        if (!DemoNotifications.enabled(context, "daily_reminder")) { message = "Günlük hatırlatmalar kapalı. Bildirim ayarlarından izin ver."; return }
        p.edit().putBoolean("enabled", true).commit(); enabled = true
        DailyReminder.schedule(context); message = "Her gün seçtiğin saatte hatırlatılacak."
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) enable() else message = "Bildirim izni verilmedi." }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFF))) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Daily Reminder", style = MaterialTheme.typography.titleLarge, color = Color(0xFF392D57))
                Switch(checked = enabled, onCheckedChange = { checked ->
                    if (!checked) { enabled = false; p.edit().putBoolean("enabled", false).commit(); DailyReminder.schedule(context); context.getSystemService(NotificationManager::class.java).cancel(9002); message = "" }
                    else if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else enable()
                })
            }
            Text("Her gün çalışmak için kendine zaman ayır.")
            OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), onClick = {
                TimePickerDialog(context, { _, h, m -> hour = h; minute = m; p.edit().putInt("hour", h).putInt("minute", m).commit(); if (enabled) DailyReminder.schedule(context) }, hour, minute, true).show()
            }) { Text("Her gün · ${String.format(Locale.ROOT, "%02d:%02d", hour, minute)}") }
            if (enabled && Build.VERSION.SDK_INT >= 31 && !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) Text("Güç tasarrufunda hatırlatma gecikebilir.", style = MaterialTheme.typography.bodySmall)
            if (message.isNotBlank()) {
                Text(message, style = MaterialTheme.typography.bodySmall)
                if (!enabled) TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Bildirim ayarları") }
            }
        }
    }
}
