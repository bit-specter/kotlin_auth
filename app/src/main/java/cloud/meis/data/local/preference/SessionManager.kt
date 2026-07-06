package cloud.meis.data.local.preference

import android.content.Context

class SessionManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    fun saveLogin(userId: Int, userName: String) {
        preferences.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .apply()
    }

    fun saveLogin(userName: String) {
        preferences.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USER_NAME, userName)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(KEY_LOGGED_IN, false)
    }

    fun getUserName(): String {
        return preferences.getString(KEY_USER_NAME, null) ?: "Guest"
    }

    fun getUserId(): Int {
        return preferences.getInt(KEY_USER_ID, 0)
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "pingmon_session"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
    }
}
