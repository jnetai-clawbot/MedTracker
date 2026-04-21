package com.jnetai.medtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    val frequencyPerDay: Int,
    val reminderTimes: String, // JSON array of "HH:mm" strings
    val remainingPills: Int,
    val refillThreshold: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "dose_history")
data class DoseHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val scheduledTime: String, // "HH:mm"
    val scheduledDate: String, // "yyyy-MM-dd"
    val takenAt: Long?, // timestamp when taken, null if skipped/not taken
    val status: DoseStatus, // TAKEN, SKIPPED, MISSED
    val updatedAt: Long = System.currentTimeMillis()
)

enum class DoseStatus {
    TAKEN, SKIPPED, MISSED
}

@Entity(tableName = "refill_history")
data class RefillHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val quantityAdded: Int,
    val refillDate: Long = System.currentTimeMillis()
)