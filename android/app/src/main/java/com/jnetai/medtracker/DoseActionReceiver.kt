package com.jnetai.medtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jnetai.medtracker.data.MedDatabase
import com.jnetai.medtracker.data.DoseStatus
import com.jnetai.medtracker.data.DoseHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DoseActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(ReminderReceiver.EXTRA_MED_ID, -1)
        val medName = intent.getStringExtra(ReminderReceiver.EXTRA_MED_NAME) ?: return
        val time = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_TIME) ?: return
        val action = intent.getStringExtra("dose_action") ?: return

        val db = MedDatabase.getInstance(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        CoroutineScope(Dispatchers.IO).launch {
            val existing = db.medDao().getDose(medId, today, time)
            if (action == "taken") {
                if (existing != null) {
                    db.medDao().updateDoseHistory(existing.copy(
                        status = DoseStatus.TAKEN,
                        takenAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ))
                } else {
                    db.medDao().insertDoseHistory(DoseHistory(
                        medicationId = medId,
                        medicationName = medName,
                        scheduledTime = time,
                        scheduledDate = today,
                        takenAt = System.currentTimeMillis(),
                        status = DoseStatus.TAKEN
                    ))
                }
                // Decrement pill count
                val med = db.medDao().getMedicationById(medId)
                if (med != null && med.remainingPills > 0) {
                    db.medDao().updatePillCount(medId, med.remainingPills - 1)
                }
            } else if (action == "skip") {
                if (existing != null) {
                    db.medDao().updateDoseHistory(existing.copy(
                        status = DoseStatus.SKIPPED,
                        updatedAt = System.currentTimeMillis()
                    ))
                } else {
                    db.medDao().insertDoseHistory(DoseHistory(
                        medicationId = medId,
                        medicationName = medName,
                        scheduledTime = time,
                        scheduledDate = today,
                        takenAt = null,
                        status = DoseStatus.SKIPPED
                    ))
                }
            }
        }

        // Cancel the notification
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.cancel(medId.toInt() * 100 + time.hashCode().mod(100))
    }
}