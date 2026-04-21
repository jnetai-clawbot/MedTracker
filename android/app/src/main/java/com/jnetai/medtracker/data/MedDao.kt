package com.jnetai.medtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedDao {

    // Medications
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(med: Medication): Long

    @Update
    suspend fun updateMedication(med: Medication)

    @Delete
    suspend fun deleteMedication(med: Medication)

    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Long): Medication?

    @Query("UPDATE medications SET remainingPills = :count WHERE id = :id")
    suspend fun updatePillCount(id: Long, count: Int)

    // Dose History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseHistory(dose: DoseHistory): Long

    @Update
    suspend fun updateDoseHistory(dose: DoseHistory)

    @Query("SELECT * FROM dose_history WHERE scheduledDate = :date ORDER BY scheduledTime ASC")
    fun getDosesForDate(date: String): Flow<List<DoseHistory>>

    @Query("SELECT * FROM dose_history WHERE medicationId = :medId ORDER BY scheduledDate DESC, scheduledTime DESC")
    fun getDosesForMedication(medId: Long): Flow<List<DoseHistory>>

    @Query("SELECT * FROM dose_history ORDER BY scheduledDate DESC, scheduledTime DESC")
    fun getAllDoseHistory(): Flow<List<DoseHistory>>

    @Query("SELECT * FROM dose_history WHERE medicationId = :medId AND scheduledDate = :date AND scheduledTime = :time LIMIT 1")
    suspend fun getDose(medId: Long, date: String, time: String): DoseHistory?

    @Query("DELETE FROM dose_history WHERE medicationId = :medId")
    suspend fun deleteDoseHistoryForMedication(medId: Long)

    @Query("SELECT * FROM medications ORDER BY name ASC")
    suspend fun getAllMedicationsSuspend(): List<Medication>

    // Refill History
    @Insert
    suspend fun insertRefill(refill: RefillHistory): Long

    @Query("SELECT * FROM refill_history WHERE medicationId = :medId ORDER BY refillDate DESC")
    fun getRefillsForMedication(medId: Long): Flow<List<RefillHistory>>

    @Query("SELECT * FROM refill_history ORDER BY refillDate DESC")
    fun getAllRefills(): Flow<List<RefillHistory>>
}