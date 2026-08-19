package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.FollowUp
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowUpDao {
    @Query("SELECT * FROM follow_ups ORDER BY scheduledDate ASC")
    fun getAllFollowUps(): Flow<List<FollowUp>>

    @Query("SELECT * FROM follow_ups WHERE scheduledDate >= :startOfDay AND scheduledDate < :endOfDay AND isCompleted = 0 ORDER BY scheduledDate ASC")
    fun getTodayFollowUps(startOfDay: Long, endOfDay: Long): Flow<List<FollowUp>>

    @Query("SELECT COUNT(*) FROM follow_ups WHERE scheduledDate >= :startOfDay AND scheduledDate < :endOfDay AND isCompleted = 0")
    fun getTodayFollowUpCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT * FROM follow_ups WHERE scheduledDate >= :startOfTomorrow AND isCompleted = 0 ORDER BY scheduledDate ASC")
    fun getUpcomingFollowUps(startOfTomorrow: Long): Flow<List<FollowUp>>

    @Query("SELECT * FROM follow_ups WHERE scheduledDate < :startOfDay AND isCompleted = 0 ORDER BY scheduledDate DESC")
    fun getOverdueFollowUps(startOfDay: Long): Flow<List<FollowUp>>

    @Query("SELECT * FROM follow_ups WHERE patientId = :patientId ORDER BY scheduledDate DESC")
    fun getFollowUpsForPatient(patientId: Long): Flow<List<FollowUp>>

    @Query("SELECT * FROM follow_ups WHERE patientId = :patientId")
    suspend fun getFollowUpsForPatientOnce(patientId: Long): List<FollowUp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUp(followUp: FollowUp): Long

    @Update
    suspend fun updateFollowUp(followUp: FollowUp)

    @Delete
    suspend fun deleteFollowUp(followUp: FollowUp)
}
