package com.jnetai.medtracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.jnetai.medtracker.data.MedDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "med_reminders"
        const val EXTRA_MED_ID = "med_id"
        const val EXTRA_MED_NAME = "med_name"
        const val EXTRA_MED_DOSAGE = "med_dosage"
        const val EXTRA_REMINDER_TIME = "reminder_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: return
        val medDosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: ""
        val medId = intent.getLongExtra(EXTRA_MED_ID, -1)
        val reminderTime = intent.getStringExtra(EXTRA_REMINDER_TIME) ?: ""

        createNotificationChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, medId.toInt(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark as MISSED if not taken within 1 hour — schedule via AlarmManager
        // For now, create the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💊 Medication Reminder")
            .setContentText("$medName — $medDosage")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Time to take $medName ($medDosage). Don't forget!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Mark Taken",
                getActionIntent(context, medId, medName, reminderTime, "taken")
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Skip",
                getActionIntent(context, medId, medName, reminderTime, "skip")
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(medId.toInt() * 100 + reminderTime.hashCode().mod(100), notification)

        // Record missed dose entry if not already tracked
        val db = MedDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            val existing = db.medDao().getDose(medId, today, reminderTime)
            if (existing == null) {
                db.medDao().insertDoseHistory(
                    com.jnetai.medtracker.data.DoseHistory(
                        medicationId = medId,
                        medicationName = medName,
                        scheduledTime = reminderTime,
                        scheduledDate = today,
                        takenAt = null,
                        status = com.jnetai.medtracker.data.DoseStatus.MISSED
                    )
                )
            }
        }
    }

    private fun getActionIntent(
        context: Context,
        medId: Long,
        medName: String,
        time: String,
        action: String
    ): PendingIntent {
        val intent = Intent(context, DoseActionReceiver::class.java).apply {
            putExtra(EXTRA_MED_ID, medId)
            putExtra(EXTRA_MED_NAME, medName)
            putExtra(EXTRA_REMINDER_TIME, time)
            putExtra("dose_action", action)
        }
        return PendingIntent.getBroadcast(
            context, (medId * 1000 + action.hashCode()).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for medication dose reminders"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}