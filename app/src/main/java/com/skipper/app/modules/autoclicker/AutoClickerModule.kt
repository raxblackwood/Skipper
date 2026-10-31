package com.skipper.app.modules.autoclicker

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.skipper.app.engine.BaseAppModule
import com.skipper.app.model.AppSection
import com.skipper.app.service.SkipperAccessibilityService
import com.skipper.app.util.PreferencesManager

class AutoClickerModule(
    private val prefs: PreferencesManager
) : BaseAppModule {

    override val id: String = "autoclicker"
    override val name: String = "Smart Auto-Clicker"
    override val targetPackages: List<String> = listOf()

    override fun isEnabled(): Boolean = prefs.isAutoClickerEnabled

    override fun getAppSection(): AppSection {
        val mode = if (prefs.autoClickMode == "CONTINUOUS") "Continuous Tap" else "Smart Button Detection"
        return AppSection(
            id = id,
            name = name,
            targetPackages = targetPackages,
            description = "Universal click assistant with customizable interval & smart triggers",
            isEnabled = isEnabled(),
            summaryRule = "$mode (${prefs.autoClickIntervalMs}ms)",
            iconResName = "ic_touch_app"
        )
    }

    override fun matchesPackage(packageName: String): Boolean {
        // Universal clicker doesn't hijack specific chat apps handled by dedicated modules
        if (!isEnabled()) return false
        val isChatApp = packageName.contains("anonchat", ignoreCase = true) ||
                packageName.contains("chatous", ignoreCase = true)
        return !isChatApp
    }

    override fun processHierarchy(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        service: SkipperAccessibilityService
    ) {
        // Universal Smart Auto-Clicker logic when active
    }
}
