package com.jnetai.medtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jnetai.medtracker.data.MedDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = MedDatabase.getInstance(context)
            CoroutineScope(Dispatchers.IO).launch {
                val meds = db.medDao().getAllMedicationsSuspend()
                for (med in meds) {
                    ReminderScheduler.scheduleReminders(context, med)
                }
            }
        }
    }
}