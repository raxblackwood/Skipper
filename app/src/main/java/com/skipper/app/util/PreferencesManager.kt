package com.skipper.app.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("skipper_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVICE_MASTER_SWITCH = "master_switch_enabled"
        
        // AnonChat Keys
        private const val KEY_ANONCHAT_ENABLED = "anonchat_enabled"
        private const val KEY_ANONCHAT_ONLY_INSTANT = "anonchat_only_instant"
        private const val KEY_ANONCHAT_CLICK_DELAY = "anonchat_click_delay"
        private const val KEY_ANONCHAT_COOLDOWN = "anonchat_cooldown"
        private const val KEY_ANONCHAT_CUSTOM_PKG = "anonchat_custom_pkg"

        // Chatous Keys
        private const val KEY_CHATOUS_ENABLED = "chatous_enabled"
        private const val KEY_CHATOUS_SKIP_MALE = "chatous_skip_male"
        private const val KEY_CHATOUS_CLICK_DELAY = "chatous_click_delay"
        private const val KEY_CHATOUS_COOLDOWN = "chatous_cooldown"
    }

    var isMasterSwitchEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_MASTER_SWITCH, true)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_MASTER_SWITCH, value).apply()

    var isAnonChatEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANONCHAT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ANONCHAT_ENABLED, value).apply()

    var isAnonChatOnlyInstantSkip: Boolean
        get() = prefs.getBoolean(KEY_ANONCHAT_ONLY_INSTANT, true)
        set(value) = prefs.edit().putBoolean(KEY_ANONCHAT_ONLY_INSTANT, value).apply()

    var anonChatClickDelayMs: Long
        get() = prefs.getLong(KEY_ANONCHAT_CLICK_DELAY, 350L)
        set(value) = prefs.edit().putLong(KEY_ANONCHAT_CLICK_DELAY, value).apply()

    var anonChatCooldownMs: Long
        get() = prefs.getLong(KEY_ANONCHAT_COOLDOWN, 2000L)
        set(value) = prefs.edit().putLong(KEY_ANONCHAT_COOLDOWN, value).apply()

    var anonChatCustomPackage: String
        get() = prefs.getString(KEY_ANONCHAT_CUSTOM_PKG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANONCHAT_CUSTOM_PKG, value).apply()

    // Chatous Settings
    var isChatousEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHATOUS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CHATOUS_ENABLED, value).apply()

    var isChatousSkipMale: Boolean
        get() = prefs.getBoolean(KEY_CHATOUS_SKIP_MALE, true)
        set(value) = prefs.edit().putBoolean(KEY_CHATOUS_SKIP_MALE, value).apply()

    var chatousClickDelayMs: Long
        get() = prefs.getLong(KEY_CHATOUS_CLICK_DELAY, 250L)
        set(value) = prefs.edit().putLong(KEY_CHATOUS_CLICK_DELAY, value).apply()

    var chatousCooldownMs: Long
        get() = prefs.getLong(KEY_CHATOUS_COOLDOWN, 1500L)
        set(value) = prefs.edit().putLong(KEY_CHATOUS_COOLDOWN, value).apply()
}
