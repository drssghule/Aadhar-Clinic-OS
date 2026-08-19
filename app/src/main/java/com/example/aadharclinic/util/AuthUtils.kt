package com.example.aadharclinic.util

import java.security.MessageDigest

object AuthUtils {

    /**
     * Hashes password using SHA-256 with clinic context salt.
     * Prevents plain-text password storage.
     */
    fun hashPassword(password: String, salt: String = "AADHAR_HOSPITAL_SALT_2026"): String {
        if (password.isBlank()) return ""
        val input = "$salt:$password"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies plain text password against stored hash or fallback
     */
    fun verifyPassword(plainPassword: String, storedHash: String, salt: String = "AADHAR_HOSPITAL_SALT_2026"): Boolean {
        if (plainPassword.isBlank() || storedHash.isBlank()) return false
        val computedHash = hashPassword(plainPassword, salt)
        return computedHash.equals(storedHash, ignoreCase = true) || plainPassword == storedHash
    }

    /**
     * Generates a suggested clean unique Hospital ID from clinic name
     * Example: "Aadhar Multi-Speciality Clinic" -> "aadhar123"
     */
    fun generateHospitalIdSuggestion(clinicName: String): String {
        val sanitized = clinicName.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .take(8)
        val prefix = if (sanitized.isNotBlank()) sanitized else "clinic"
        val randomSuffix = (100..999).random()
        return "$prefix$randomSuffix"
    }
}
