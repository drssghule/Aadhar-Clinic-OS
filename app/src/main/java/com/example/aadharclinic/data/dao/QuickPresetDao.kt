package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.QuickPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickPresetDao {
    @Query("SELECT * FROM quick_presets ORDER BY presetName ASC")
    fun getAllPresets(): Flow<List<QuickPreset>>

    @Query("SELECT * FROM quick_presets WHERE clinicId = :clinicId ORDER BY presetName ASC")
    fun getPresetsByClinic(clinicId: String): Flow<List<QuickPreset>>

    @Query("SELECT * FROM quick_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: Long): QuickPreset?

    @Query("SELECT COUNT(*) FROM quick_presets")
    suspend fun getPresetCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: QuickPreset): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<QuickPreset>)

    @Update
    suspend fun updatePreset(preset: QuickPreset)

    @Delete
    suspend fun deletePreset(preset: QuickPreset)

    @Query("DELETE FROM quick_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)
}
