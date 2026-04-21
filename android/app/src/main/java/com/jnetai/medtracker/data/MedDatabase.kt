package com.jnetai.medtracker.data

import androidx.room.*
import android.content.Context

class Converters {
    @TypeConverter
    fun fromDoseStatus(status: DoseStatus): String = status.name

    @TypeConverter
    fun toDoseStatus(value: String): DoseStatus = DoseStatus.valueOf(value)
}

@Database(
    entities = [Medication::class, DoseHistory::class, RefillHistory::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MedDatabase : RoomDatabase() {
    abstract fun medDao(): MedDao

    companion object {
        @Volatile
        private var INSTANCE: MedDatabase? = null

        fun getInstance(context: Context): MedDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedDatabase::class.java,
                    "medtracker.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}