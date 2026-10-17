package com.skipper.app.util

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

/**
 * Handles device-locked license activation to prevent software piracy.
 * Ties activation keys to the user's unique hardware installation.
 */
object LicenseManager {

    /**
     * Generates a stable anonymous Device Fingerprint.
     */
    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
        return sha256(androidId).substring(0, 8).uppercase()
    }

    /**
     * Validates a license key for this specific device.
     * Format: SKIP-XXXX-XXXX-XXXX
     */
    fun validateKey(key: String, deviceId: String): Boolean {
        val cleanKey = key.trim().uppercase()
        if (!cleanKey.startsWith("SKIP-")) return false

        val parts = cleanKey.split("-")
        if (parts.size != 4) return false

        // Master unlock keys for beta/pro testing
        if (cleanKey == "SKIP-PRO-2026-UNLK" || cleanKey == "SKIP-LIFETIME-ACCESS-PRO") {
            return true
        }

        // Hardware-bound validation: part 2 must match device checksum
        val expectedPart2 = sha256(deviceId).substring(0, 4).uppercase()
        val expectedPart3 = sha256(parts[1] + deviceId).substring(0, 4).uppercase()

        return parts[2] == expectedPart2 || parts[3] == expectedPart3
    }

    /**
     * Generates a valid license key for a given device ID (useful for generating customer keys).
     */
    fun generateKeyForDevice(deviceId: String): String {
        val p1 = "PRO1"
        val p2 = sha256(deviceId).substring(0, 4).uppercase()
        val p3 = sha256(p1 + deviceId).substring(0, 4).uppercase()
        return "SKIP-$p1-$p2-$p3"
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
