package com.ingredientguard.utils

object Constants {
    // Notification
    const val CHANNEL_ALLERGEN_ALERTS = "allergen_alerts"
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    // SharedPreferences
    const val PREFS_NAME = "allergen_scanner_prefs"
    const val KEY_FIRST_LAUNCH = "first_launch"
    const val KEY_LAST_PROFILE_SYNC = "last_profile_sync"
    const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

    // Database
    const val DATABASE_NAME = "ingredientguard_database"
    const val DATABASE_VERSION = 2

    // User
    const val DEFAULT_USER_ID = "current_user"

    // Allergen severity
    const val SEVERITY_LOW = "LOW"
    const val SEVERITY_MEDIUM = "MEDIUM"
    const val SEVERITY_HIGH = "HIGH"

    // Theme
    const val THEME_LIGHT = "LIGHT"
    const val THEME_DARK = "DARK"
    const val THEME_SYSTEM = "SYSTEM"

    // Language
    const val LANGUAGE_RO = "ro"
    const val LANGUAGE_EN = "en"
}