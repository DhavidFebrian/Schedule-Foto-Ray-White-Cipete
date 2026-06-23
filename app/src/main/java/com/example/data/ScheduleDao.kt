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
            val newUniqueKeys = newSchedules.map { "${it.idListing.trim()}_${it.namaMe.trim()}_${it.tanggal.trim()}_${it.jam.trim()}" }.toSet()
            val filteredUnsynced = unsyncedSchedules.filter {
                val key = "${it.idListing.trim()}_${it.namaMe.trim()}_${it.tanggal.trim()}_${it.jam.trim()}"
                !newUniqueKeys.contains(key)
            }
            if (filteredUnsynced.isNotEmpty()) {
                insertSchedules(filteredUnsynced)
            }
        }
    }

    @Delete
    suspend fun deleteSchedule(schedule: Schedule)

    @Query("DELETE FROM schedules WHERE idListing IN (:ids)")
    suspend fun deleteSchedulesByIds(ids: List<String>)

    @Update
    suspend fun updateSchedule(schedule: Schedule)
}
