package cloud.meis.data.local.preference

import android.content.Context

class SessionManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

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

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "pingmon_session"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_USER_NAME = "user_name"
    }
}
