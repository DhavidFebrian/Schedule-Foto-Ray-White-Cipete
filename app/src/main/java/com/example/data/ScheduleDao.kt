package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY tanggal ASC, jam ASC")
    fun getAllSchedules(): Flow<List<Schedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<Schedule>)

    @Query("DELETE FROM schedules")
    suspend fun clearSchedules()

    @Transaction
    suspend fun updateSchedulesTransaction(newSchedules: List<Schedule>, unsyncedSchedules: List<Schedule>) {
        clearSchedules()
        if (newSchedules.isNotEmpty()) {
            insertSchedules(newSchedules)
        }
        if (unsyncedSchedules.isNotEmpty()) {
            insertSchedules(unsyncedSchedules)
        }
    }

    @Delete
    suspend fun deleteSchedule(schedule: Schedule)

    @Query("DELETE FROM schedules WHERE idListing IN (:ids)")
    suspend fun deleteSchedulesByIds(ids: List<String>)

    @Update
    suspend fun updateSchedule(schedule: Schedule)
}
