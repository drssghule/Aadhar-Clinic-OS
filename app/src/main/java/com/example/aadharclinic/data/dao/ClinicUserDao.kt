package com.example.aadharclinic.data.dao

import androidx.room.*
import com.example.aadharclinic.data.model.ClinicUser
import com.example.aadharclinic.data.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface ClinicUserDao {

    @Query("SELECT * FROM clinic_users ORDER BY role ASC, name ASC")
    fun getAllUsers(): Flow<List<ClinicUser>>

    @Query("SELECT * FROM clinic_users WHERE isActive = 1 ORDER BY role ASC, name ASC")
    fun getActiveUsers(): Flow<List<ClinicUser>>

    @Query("SELECT * FROM clinic_users WHERE clinicId = :hospitalId ORDER BY role ASC, name ASC")
    fun getUsersByHospital(hospitalId: String): Flow<List<ClinicUser>>

    @Query("SELECT * FROM clinic_users WHERE clinicId = :hospitalId AND isActive = 1 ORDER BY role ASC, name ASC")
    fun getActiveUsersByHospital(hospitalId: String): Flow<List<ClinicUser>>

    @Query("SELECT * FROM clinic_users WHERE clinicId = :hospitalId AND role = :role AND isActive = 1")
    suspend fun getUsersByHospitalAndRole(hospitalId: String, role: UserRole): List<ClinicUser>

    @Query("SELECT * FROM clinic_users WHERE clinicId = :hospitalId AND role = :role AND passwordHash = :password AND isActive = 1 LIMIT 1")
    suspend fun authenticateByHospitalAndRole(hospitalId: String, role: UserRole, password: String): ClinicUser?

    @Query("SELECT * FROM clinic_users WHERE clinicId = :hospitalId AND role = 'ADMIN' AND isActive = 1 LIMIT 1")
    suspend fun getAdminByHospital(hospitalId: String): ClinicUser?

    @Query("SELECT * FROM clinic_users WHERE id = :id LIMIT 1")
    fun getUserById(id: Long): Flow<ClinicUser?>

    @Query("SELECT * FROM clinic_users WHERE id = :id LIMIT 1")
    suspend fun getUserByIdOnce(id: Long): ClinicUser?

    @Query("SELECT * FROM clinic_users WHERE loginId = :loginId LIMIT 1")
    suspend fun getUserByLoginId(loginId: String): ClinicUser?

    @Query("SELECT * FROM clinic_users WHERE loginId = :loginId AND passwordHash = :password AND isActive = 1 LIMIT 1")
    suspend fun authenticate(loginId: String, password: String): ClinicUser?

    @Query("SELECT COUNT(*) FROM clinic_users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: ClinicUser): Long

    @Update
    suspend fun updateUser(user: ClinicUser)

    @Query("UPDATE clinic_users SET passwordHash = :newPassword, updatedAt = :timestamp WHERE id = :userId")
    suspend fun resetPassword(userId: Long, newPassword: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE clinic_users SET isActive = :isActive, updatedAt = :timestamp WHERE id = :userId")
    suspend fun setActiveStatus(userId: Long, isActive: Boolean, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteUser(user: ClinicUser)

    // Sync helpers
    @Query("SELECT * FROM clinic_users WHERE syncStatus = :status")
    suspend fun getUsersBySyncStatus(status: String = "PENDING"): List<ClinicUser>

    @Query("SELECT * FROM clinic_users WHERE recordId = :recordId LIMIT 1")
    suspend fun getUserByRecordId(recordId: String): ClinicUser?

    @Query("UPDATE clinic_users SET syncStatus = :status, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateUserSyncStatus(id: Long, status: String, timestamp: Long)

    @Query("SELECT * FROM clinic_users")
    suspend fun getAllUsersOnce(): List<ClinicUser>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<ClinicUser>)
}
