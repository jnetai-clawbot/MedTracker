package com.jnetai.medtracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jnetai.medtracker.data.Medication
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

object ReminderScheduler {

    private val gson = Gson()

    fun scheduleReminders(context: Context, med: Medication) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val times = parseTimes(med.reminderTimes)

        for (time in times) {
            val (hour, minute) = parseHourMinute(time)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderReceiver.EXTRA_MED_ID, med.id)
                putExtra(ReminderReceiver.EXTRA_MED_NAME, med.name)
                putExtra(ReminderReceiver.EXTRA_MED_DOSAGE, med.dosage)
                putExtra(ReminderReceiver.EXTRA_REMINDER_TIME, time)
            }

            val requestCode = "${med.id}_$time".hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set repeating alarm every day
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            // Also try exact alarm for API 31+
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (_: SecurityException) {
                // Fallback to inexact alarm
            }
        }
    }

    fun cancelReminders(context: Context, med: Medication) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val times = parseTimes(med.reminderTimes)

        for (time in times) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderReceiver.EXTRA_MED_ID, med.id)
                putExtra(ReminderReceiver.EXTRA_MED_NAME, med.name)
                putExtra(ReminderReceiver.EXTRA_MED_DOSAGE, med.dosage)
                putExtra(ReminderReceiver.EXTRA_REMINDER_TIME, time)
            }
            val requestCode = "${med.id}_$time".hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    private fun parseTimes(json: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseHourMinute(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        return Pair(parts.getOrElse(0) { "8" }.toInt(), parts.getOrElse(1) { "0" }.toInt())
    }
}