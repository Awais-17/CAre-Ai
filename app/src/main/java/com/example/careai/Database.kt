package com.example.careai

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "health_history")
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val event: String,
    val triage: String,
    val details: String
)

@Entity(tableName = "medicine_reminders")
data class MedicineReminder(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val dosage: String,
    val time: String,
    val isTaken: Boolean = false
)

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_history ORDER BY id DESC")
    fun getAllHealthRecords(): Flow<List<HealthRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthRecord(record: HealthRecord)

    @Query("SELECT * FROM medicine_reminders ORDER BY time ASC")
    fun getAllReminders(): Flow<List<MedicineReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicineReminder)

    @Update
    suspend fun updateReminder(reminder: MedicineReminder)

    @Delete
    suspend fun deleteReminder(reminder: MedicineReminder)
}

@Database(entities = [HealthRecord::class, MedicineReminder::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "careai_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
