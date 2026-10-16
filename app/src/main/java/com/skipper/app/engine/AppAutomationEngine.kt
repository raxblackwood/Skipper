package com.skipper.app.engine

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.skipper.app.model.AppSection
import com.skipper.app.modules.anonchat.AnonChatModule
import com.skipper.app.modules.chatous.ChatousModule
import com.skipper.app.service.SkipperAccessibilityService
import com.skipper.app.util.PreferencesManager
import java.util.concurrent.CopyOnWriteArrayList

class AppAutomationEngine(
    private val prefs: PreferencesManager
) {
    private val modules = CopyOnWriteArrayList<BaseAppModule>()

    init {
        // Register core modules
        registerModule(AnonChatModule(prefs))
        registerModule(ChatousModule(prefs))
    }

    fun registerModule(module: BaseAppModule) {
        modules.add(module)
    }

    fun getAllAppSections(): List<AppSection> {
        return modules.map { it.getAppSection() }
    }

    fun getModuleById(id: String): BaseAppModule? {
        return modules.firstOrNull { it.id == id }
    }

    fun dispatchEvent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        service: SkipperAccessibilityService
    ) {
        if (!prefs.isMasterSwitchEnabled) return

        val pkgName = event.packageName?.toString() ?: return

        for (module in modules) {
            if (module.matchesPackage(pkgName)) {
                module.processHierarchy(event, rootNode, service)
            }
        }
    }
}
