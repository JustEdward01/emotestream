package com.ingredientguard.utils

import android.content.Context

object PreferencesHelper {
    private fun getPrefs(context: Context) =
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    fun isFirstLaunch(context: Context): Boolean {
        return getPrefs(context).getBoolean(Constants.KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted(context: Context) {
        getPrefs(context).edit()
            .putBoolean(Constants.KEY_FIRST_LAUNCH, false)
            .apply()
    }

    fun updateLastProfileSync(context: Context) {
        getPrefs(context).edit()
            .putLong(Constants.KEY_LAST_PROFILE_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun getLastProfileSync(context: Context): Long {
        return getPrefs(context).getLong(Constants.KEY_LAST_PROFILE_SYNC, 0)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, enabled)
            .apply()
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(Constants.KEY_NOTIFICATIONS_ENABLED, true)
    }
}