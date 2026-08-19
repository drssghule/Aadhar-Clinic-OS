package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.ClinicProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ClinicProfileDao {
    @Query("SELECT * FROM clinic_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<ClinicProfile?>

    @Query("SELECT * FROM clinic_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileOnce(): ClinicProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: ClinicProfile)
}
