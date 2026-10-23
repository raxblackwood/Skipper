package com.skipper.app.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("skipper_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVICE_MASTER_SWITCH = "master_switch_enabled"
        private const val KEY_DISCLOSURE_ACCEPTED = "disclosure_accepted"
        
        // AnonChat Keys
        private const val KEY_ANONCHAT_ENABLED = "anonchat_enabled"
        private const val KEY_ANONCHAT_ONLY_INSTANT = "anonchat_only_instant"
        private const val KEY_ANONCHAT_CLICK_DELAY = "anonchat_click_delay"
        private const val KEY_ANONCHAT_COOLDOWN = "anonchat_cooldown"
        private const val KEY_ANONCHAT_CUSTOM_PKG = "anonchat_custom_pkg"
        private const val KEY_ANONCHAT_AUTO_SKIP_MALE = "anonchat_auto_skip_male"

        // License Keys
        private const val KEY_IS_PRO = "is_pro_activated"
        private const val KEY_LICENSE_KEY = "license_key"

        // Gender Targets ("MALE", "FEMALE", "NONE")
        private const val KEY_ANONCHAT_GENDER_TARGET = "anonchat_gender_target"
        private const val KEY_ANONCHAT_CUSTOM_KEYWORDS = "anonchat_custom_keywords"
        private const val KEY_ANONCHAT_NEVER_SKIP_KEYWORDS = "anonchat_never_skip_keywords"
        private const val KEY_CHATOUS_GENDER_TARGET = "chatous_gender_target"

        // Chatous Keys
        private const val KEY_CHATOUS_ENABLED = "chatous_enabled"
        private const val KEY_CHATOUS_SKIP_MALE = "chatous_skip_male"
        private const val KEY_CHATOUS_CLICK_DELAY = "chatous_click_delay"
        private const val KEY_CHATOUS_COOLDOWN = "chatous_cooldown"
    }

    var isMasterSwitchEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_MASTER_SWITCH, true)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_MASTER_SWITCH, value).apply()

    var isAccessibilityDisclosureAccepted: Boolean
        get() = prefs.getBoolean(KEY_DISCLOSURE_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_DISCLOSURE_ACCEPTED, value).apply()

    // License state
    var isProActivated: Boolean
        get() = prefs.getBoolean(KEY_IS_PRO, true) // Enabled by default for full capability
        set(value) = prefs.edit().putBoolean(KEY_IS_PRO, value).apply()

    var licenseKey: String
        get() = prefs.getString(KEY_LICENSE_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LICENSE_KEY, value).apply()

    // AnonChat settings
    var isAnonChatEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANONCHAT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ANONCHAT_ENABLED, value).apply()

    var isAnonChatOnlyInstantSkip: Boolean
        get() = prefs.getBoolean(KEY_ANONCHAT_ONLY_INSTANT, true)
        set(value) = prefs.edit().putBoolean(KEY_ANONCHAT_ONLY_INSTANT, value).apply()

    var isAnonChatAutoSkipMale: Boolean
        get() = prefs.getBoolean(KEY_ANONCHAT_AUTO_SKIP_MALE, true)
        set(value) = prefs.edit().putBoolean(KEY_ANONCHAT_AUTO_SKIP_MALE, value).apply()

    var anonChatGenderTarget: String
        get() = prefs.getString(KEY_ANONCHAT_GENDER_TARGET, "MALE") ?: "MALE"
        set(value) = prefs.edit().putString(KEY_ANONCHAT_GENDER_TARGET, value).apply()

    var anonChatCustomKeywords: Set<String>
        get() = prefs.getStringSet(KEY_ANONCHAT_CUSTOM_KEYWORDS, setOf("m", "male", "boy", "guy", "dude", "looking for f", "m4f")) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_ANONCHAT_CUSTOM_KEYWORDS, value).apply()

    var anonChatNeverSkipKeywords: Set<String>
        get() = prefs.getStringSet(KEY_ANONCHAT_NEVER_SKIP_KEYWORDS, setOf("female", "f", "girl", "woman", "lady", "she", "her", "queen", "gal")) ?: setOf("female", "f", "girl", "woman", "lady", "she", "her", "queen", "gal")
        set(value) = prefs.edit().putStringSet(KEY_ANONCHAT_NEVER_SKIP_KEYWORDS, value).apply()

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

    var chatousGenderTarget: String
        get() = prefs.getString(KEY_CHATOUS_GENDER_TARGET, "MALE") ?: "MALE"
        set(value) = prefs.edit().putString(KEY_CHATOUS_GENDER_TARGET, value).apply()

    var chatousClickDelayMs: Long
        get() = prefs.getLong(KEY_CHATOUS_CLICK_DELAY, 250L)
        set(value) = prefs.edit().putLong(KEY_CHATOUS_CLICK_DELAY, value).apply()

    var chatousCooldownMs: Long
        get() = prefs.getLong(KEY_CHATOUS_COOLDOWN, 1500L)
        set(value) = prefs.edit().putLong(KEY_CHATOUS_COOLDOWN, value).apply()
}
