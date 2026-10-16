package com.skipper.app.engine

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.skipper.app.model.AppSection
import com.skipper.app.service.SkipperAccessibilityService

/**
 * Base interface for an App Automation Module in Skipper.
 * Each module handles state detection, safeguards, and actions for a specific application.
 */
interface BaseAppModule {
    /** Unique identifier for the module (e.g. "anonchat") */
    val id: String

    /** Human-readable display name (e.g. "AnonChat") */
    val name: String

    /** Target Android package names this module handles */
    val targetPackages: List<String>

    /** Returns UI section model for the dashboard */
    fun getAppSection(): AppSection

    /** Whether this module is currently enabled by the user */
    fun isEnabled(): Boolean

    /**
     * Checks if the module should handle an incoming accessibility event.
     */
    fun matchesPackage(packageName: String): Boolean {
        if (!isEnabled()) return false
        return targetPackages.any { pkg ->
            packageName.equals(pkg, ignoreCase = true) ||
            packageName.contains(pkg, ignoreCase = true)
        }
    }

    /**
     * Called whenever a relevant accessibility event is received.
     * Implementations inspect the view hierarchy, evaluate triggers and safeguards,
     * and perform automation if all conditions are strictly met.
     */
    fun processHierarchy(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        service: SkipperAccessibilityService
    )
}
