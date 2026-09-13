package com.madrigalsolu.ecolima.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Local persistent session manager backed by SharedPreferences.
 * Prevents unexpected session loss when the app restarts, works offline,
 * or recovers from background process recreation.
 */
class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveSession(name: String, email: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    val isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    val userName: String
        get() = prefs.getString(KEY_USER_NAME, "Usuario") ?: "Usuario"

    val userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "usuario@ecolim.pe") ?: "usuario@ecolim.pe"

    fun hasPurgedGhostRecords(): Boolean = prefs.getBoolean(KEY_GHOSTS_PURGED, false)

    fun markGhostRecordsPurged() {
        prefs.edit().putBoolean(KEY_GHOSTS_PURGED, true).apply()
    }

    companion object {
        private const val PREF_NAME = "ecolim_session_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_GHOSTS_PURGED = "ghost_records_purged_v3"

        @Volatile
        private var instance: UserSessionManager? = null

        fun getInstance(context: Context): UserSessionManager {
            return instance ?: synchronized(this) {
                instance ?: UserSessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
