package com.skipper.app.modules.anonchat

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.skipper.app.engine.BaseAppModule
import com.skipper.app.engine.GenderClassifier
import com.skipper.app.engine.GenderResult
import com.skipper.app.engine.NodeMatcher
import com.skipper.app.model.AppSection
import com.skipper.app.model.TriggerStatus
import com.skipper.app.service.SkipperAccessibilityService
import com.skipper.app.util.Logger
import com.skipper.app.util.PreferencesManager

class AnonChatModule(
    private val prefs: PreferencesManager
) : BaseAppModule {

    override val id: String = "anonchat"
    override val name: String = "AnonChat"

    override val targetPackages: List<String> = listOf(
        "io.anonchat",
        "chat.anon",
        "com.anonchat",
        "com.anon.chat",
        "com.antiland",
        "chat.anonymous"
    )

    private val handler = Handler(Looper.getMainLooper())
    private var lastTriggerTime: Long = 0L
    private var isExecutingAction: Boolean = false

    override fun isEnabled(): Boolean = prefs.isAnonChatEnabled

    override fun getAppSection(): AppSection {
        val rules = mutableListOf<String>()
        if (prefs.isAnonChatAutoSkipMale) {
            rules.add("AI Male-Bio Auto-Skip")
        }
        if (prefs.isAnonChatOnlyInstantSkip) {
            rules.add("Instant Disconnect Skip (Screen 4)")
        } else {
            rules.add("Any Disconnect Skip")
        }
        return AppSection(
            id = id,
            name = name,
            targetPackages = targetPackages,
            description = "On-device AI bio gender detection & automatic instant skipping",
            isEnabled = isEnabled(),
            summaryRule = rules.joinToString(" • ")
        )
    }

    override fun processHierarchy(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        service: SkipperAccessibilityService
    ) {
        if (rootNode == null) return

        val now = System.currentTimeMillis()
        if (isExecutingAction || (now - lastTriggerTime < prefs.anonChatCooldownMs)) {
            return
        }

        // =========================================================================
        // SCENARIO 1: Partner already disconnected ("Chat was ended by your partner")
        // =========================================================================
        val isChatEnded = NodeMatcher.hasText(rootNode, "Chat was ended by your partner") ||
                NodeMatcher.hasText(rootNode, "Chat was ended")

        if (isChatEnded) {
            if (prefs.isAnonChatOnlyInstantSkip && !evaluateIsInstantSkipScreen4(rootNode)) {
                return
            }

            val newPartnerButton = locateBottomNewPartnerButton(rootNode, service)
            if (newPartnerButton != null) {
                executeClickNewPartner(newPartnerButton, service, "Partner Disconnected")
                return
            }
        }

        // =========================================================================
        // SCENARIO 2: AI Male-Bio Detection (When partner matches & does not skip)
        // =========================================================================
        if (prefs.isAnonChatAutoSkipMale) {
            val hasProfileCard = NodeMatcher.hasText(rootNode, "About partner") ||
                    NodeMatcher.hasText(rootNode, "Chat starts")

            if (hasProfileCard && !hasConversationHistory(rootNode)) {
                val bioText = extractBioAndProfileText(rootNode)
                val report = GenderClassifier.classify(bioText)

                when (report.result) {
                    GenderResult.MALE -> {
                        Logger.log(
                            appName = name,
                            action = "AI Male Detected",
                            details = "Matched male keywords: ${report.matchedTokens} in bio (\"$bioText\"). Auto-skipping...",
                            status = TriggerStatus.INFO
                        )
                        executeSkipActiveMale(rootNode, service)
                        return
                    }
                    GenderResult.FEMALE -> {
                        Logger.log(
                            appName = name,
                            action = "Female Profile Kept",
                            details = "Protected female match: ${report.matchedTokens}. Enjoy chatting!",
                            status = TriggerStatus.SUCCESS
                        )
                        return
                    }
                    GenderResult.UNDETERMINED -> {
                        // Gender not specified in bio -> Leave untouched
                        return
                    }
                }
            }
        }
    }

    /**
     * Executes skipping an active male partner who hasn't disconnected on their own.
     */
    private fun executeSkipActiveMale(
        rootNode: AccessibilityNodeInfo,
        service: SkipperAccessibilityService
    ) {
        isExecutingAction = true
        lastTriggerTime = System.currentTimeMillis()
        val delay = prefs.anonChatClickDelayMs

        handler.postDelayed({
            try {
                val freshRoot = service.rootInActiveWindow ?: rootNode

                // Step 1: If "New partner" button already appeared, click it
                val newPartnerBtn = locateBottomNewPartnerButton(freshRoot, service)
                if (newPartnerBtn != null) {
                    performClickSafely(newPartnerBtn, service)
                    Logger.log(name, "Clicked 'New partner'", "Skipped male profile.", TriggerStatus.SUCCESS)
                    return@postDelayed
                }

                // Step 2: Check for "End chat" button in dialog or screen
                val endChatNodes = NodeMatcher.findNodes(freshRoot) { node ->
                    val text = node.text?.toString() ?: node.contentDescription?.toString()
                    text != null && (text.contains("End chat", ignoreCase = true) || text.contains("Leave chat", ignoreCase = true))
                }
                val endChatTarget = endChatNodes.firstOrNull()?.let { NodeMatcher.findClickableTarget(it) ?: it }
                if (endChatTarget != null) {
                    performClickSafely(endChatTarget, service)
                    Logger.log(name, "Clicked 'End chat'", "Ending chat with male partner.", TriggerStatus.INFO)
                    return@postDelayed
                }

                // Step 3: Trigger exit menu via top-right action icon (!) or Android Back action
                val displayWidth = service.resources.displayMetrics.widthPixels
                val displayHeight = service.resources.displayMetrics.heightPixels

                val topActionNodes = NodeMatcher.findNodes(freshRoot) { node ->
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    bounds.top < displayHeight * 0.15 && bounds.right > displayWidth * 0.70 &&
                            (node.isClickable || node.className?.contains("Image") == true)
                }
                val topAction = topActionNodes.firstOrNull()?.let { NodeMatcher.findClickableTarget(it) ?: it }
                if (topAction != null) {
                    performClickSafely(topAction, service)
                    Logger.log(name, "Action Icon Triggered", "Opening end chat menu for male partner.", TriggerStatus.INFO)
                } else {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    Logger.log(name, "Back Action Triggered", "Prompting end chat for male partner.", TriggerStatus.INFO)
                }
            } catch (e: Exception) {
                Logger.log(name, "Error", "Exception skipping male: ${e.message}", TriggerStatus.ERROR)
            } finally {
                isExecutingAction = false
            }
        }, delay)
    }

    private fun executeClickNewPartner(
        buttonNode: AccessibilityNodeInfo,
        service: SkipperAccessibilityService,
        reason: String
    ) {
        isExecutingAction = true
        lastTriggerTime = System.currentTimeMillis()
        val delay = prefs.anonChatClickDelayMs

        Logger.log(name, "Trigger: $reason", "Clicking 'New partner' in ${delay}ms...", TriggerStatus.INFO)

        handler.postDelayed({
            try {
                val freshRoot = service.rootInActiveWindow ?: buttonNode
                val target = locateBottomNewPartnerButton(freshRoot, service) ?: buttonNode
                val clicked = performClickSafely(target, service)
                if (clicked) {
                    Logger.log(name, "Clicked 'New partner'", "Successfully transitioned to next search.", TriggerStatus.SUCCESS)
                }
            } catch (e: Exception) {
                Logger.log(name, "Error", "Exception clicking 'New partner': ${e.message}", TriggerStatus.ERROR)
            } finally {
                isExecutingAction = false
            }
        }, delay)
    }

    private fun extractBioAndProfileText(rootNode: AccessibilityNodeInfo): String {
        val allTexts = NodeMatcher.collectAllText(rootNode)
        val sb = StringBuilder()
        var capturing = false

        for (text in allTexts) {
            if (text.contains("About partner", ignoreCase = true)) {
                capturing = true
                continue
            }
            if (text.contains("Write a message", ignoreCase = true) || text.contains("New partner", ignoreCase = true)) {
                break
            }
            if (capturing) {
                sb.append(text).append(" ")
            }
        }

        if (sb.isEmpty()) {
            return allTexts.filter { !it.contains("New partner") && !it.contains("anon.chat") }.joinToString(" ")
        }
        return sb.toString().trim()
    }

    private fun hasConversationHistory(rootNode: AccessibilityNodeInfo): Boolean {
        val allTexts = NodeMatcher.collectAllText(rootNode)
        return allTexts.any { text ->
            text.contains("Seen", ignoreCase = true) ||
            Regex("""\b\d{1,2}:\d{2}\s*(AM|PM)?\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        }
    }

    private fun evaluateIsInstantSkipScreen4(rootNode: AccessibilityNodeInfo): Boolean {
        val allTexts = NodeMatcher.collectAllText(rootNode)
        val hasProfileHeader = allTexts.any { it.contains("About partner", ignoreCase = true) }
        val hasChatStarts = allTexts.any { it.contains("Chat starts", ignoreCase = true) }
        val hasInterests = allTexts.any { it.contains("Interests", ignoreCase = true) }

        if (!hasProfileHeader && !hasChatStarts && !hasInterests) {
            return false
        }

        return !hasConversationHistory(rootNode)
    }

    private fun locateBottomNewPartnerButton(
        rootNode: AccessibilityNodeInfo,
        service: SkipperAccessibilityService
    ): AccessibilityNodeInfo? {
        val displayHeight = service.resources.displayMetrics.heightPixels

        val candidateNodes = NodeMatcher.findNodes(rootNode) { node ->
            val text = node.text?.toString() ?: node.contentDescription?.toString()
            text != null && text.contains("New partner", ignoreCase = true)
        }

        for (node in candidateNodes) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val isAtBottom = bounds.top > (displayHeight * 0.55)
            val hasNeighborButtons = NodeMatcher.hasText(rootNode, "End chat") ||
                    NodeMatcher.hasText(rootNode, "Report")

            if (isAtBottom && hasNeighborButtons) {
                return NodeMatcher.findClickableTarget(node) ?: node
            }
        }

        return null
    }

    private fun performClickSafely(
        node: AccessibilityNodeInfo,
        service: SkipperAccessibilityService
    ): Boolean {
        val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (clicked) return true

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            return service.clickCoordinates(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
        return false
    }
}
