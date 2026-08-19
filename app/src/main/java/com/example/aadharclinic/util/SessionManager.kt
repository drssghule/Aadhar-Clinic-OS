package com.example.aadharclinic.util

import android.content.Context
import android.content.SharedPreferences
import com.example.aadharclinic.data.model.ClinicUser
import com.example.aadharclinic.data.model.UserRole

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aadhar_clinic_auth_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_HOSPITAL_ID = "key_hospital_id"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_LOGIN_ID = "key_login_id"
        private const val KEY_LOGIN_TIMESTAMP = "key_login_timestamp"
    }

    data class SessionData(
        val isLoggedIn: Boolean,
        val hospitalId: String,
        val userId: Long,
        val userName: String,
        val role: UserRole,
        val loginId: String,
        val timestamp: Long
    )

    fun saveSession(hospitalId: String, user: ClinicUser) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_HOSPITAL_ID, hospitalId.trim().lowercase())
            putLong(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_ROLE, user.role.name)
            putString(KEY_LOGIN_ID, user.loginId)
            putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }

    fun getSession(): SessionData {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val hospitalId = prefs.getString(KEY_HOSPITAL_ID, "") ?: ""
        val userId = prefs.getLong(KEY_USER_ID, 0L)
        val userName = prefs.getString(KEY_USER_NAME, "") ?: ""
        val roleStr = prefs.getString(KEY_USER_ROLE, UserRole.ADMIN.name) ?: UserRole.ADMIN.name
        val role = try {
            UserRole.valueOf(roleStr)
        } catch (e: Exception) {
            UserRole.ADMIN
        }
        val loginId = prefs.getString(KEY_LOGIN_ID, "") ?: ""
        val timestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)

        return SessionData(
            isLoggedIn = isLoggedIn,
            hospitalId = hospitalId,
            userId = userId,
            userName = userName,
            role = role,
            loginId = loginId,
            timestamp = timestamp
        )
    }

    fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) &&
                !prefs.getString(KEY_HOSPITAL_ID, "").isNullOrBlank()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
