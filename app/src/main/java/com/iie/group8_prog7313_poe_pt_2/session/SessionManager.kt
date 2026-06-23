package com.iie.group8_prog7313_poe_pt_2.session

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun isLoggedIn(): Boolean = getUserId() != null

    fun clearSession() {
        prefs.edit().remove(KEY_USER_ID).apply()

    }

    companion object {
        private const val PREF_FILE = "moneymingle_session"
        private const val KEY_USER_ID = "user_id"
    }
}
