package com.jnetai.medtracker

import android.app.Application
import com.jnetai.medtracker.data.MedDatabase

class MedTrackerApp : Application() {
    val database: MedDatabase by lazy { MedDatabase.getInstance(this) }
}