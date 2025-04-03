package com.example.rfidstockpro.sharedpref

import android.content.Context
import android.content.SharedPreferences

class SessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "UserSession"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROLE = "role"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Save user login data
    fun saveUserData(userName: String, email: String, role: Int) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, userName)
            putString(KEY_EMAIL, email)
            putInt(KEY_ROLE, role)
            putBoolean(KEY_IS_LOGGED_IN, true) // Mark user as logged in
            apply()
        }
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Get stored user name
    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "Guest") ?: "Guest"
    }

    // Get stored email
    fun getEmail(): String {
        return prefs.getString(KEY_EMAIL, "") ?: ""
    }

    // Get stored role
    fun getUserRole(): Int {
        return prefs.getInt(KEY_ROLE, 3) // Default: Staff
    }

    // Logout user (Clear session)
    fun logout() {
        prefs.edit().clear().apply()
    }
}
