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
            val target = if (prefs.anonChatGenderTarget == "FEMALE") "Female" else "Male"
            rules.add("AI $target-Bio Auto-Skip")
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
                NodeMatcher.hasText(rootNode, "Chat was ended by you") ||
                NodeMatcher.hasText(rootNode, "Chat was ended") ||
                NodeMatcher.hasText(rootNode, "has left the chat")

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
        // SCENARIO 2: AI Bio & Keyword Detection (When partner matches & does not skip)
        // =========================================================================
        if (prefs.isAnonChatAutoSkipMale && !isChatEnded) {
            val hasProfileCard = NodeMatcher.hasText(rootNode, "About partner") ||
                    NodeMatcher.hasText(rootNode, "Chat starts")

            if (hasProfileCard) {
                val bioText = extractBioAndProfileText(rootNode)
                val targetGender = prefs.anonChatGenderTarget
                val customKeywords = prefs.anonChatCustomKeywords
                val report = GenderClassifier.classify(
                    rawText = bioText,
                    customKeywords = customKeywords,
                    targetGender = targetGender
                )

                val shouldSkip = if (targetGender == "FEMALE") {
                    report.result == GenderResult.FEMALE
                } else {
                    report.result == GenderResult.MALE
                }

                if (shouldSkip) {
                    val label = if (targetGender == "FEMALE") "AI Female/Keyword Detected" else "AI Male/Keyword Detected"
                    Logger.log(
                        appName = name,
                        action = label,
                        details = "Matched tokens: ${report.matchedTokens} in bio (\"$bioText\"). Auto-skipping...",
                        status = TriggerStatus.INFO
                    )
                    executeSkipActivePartner(rootNode, service)
                    return
                } else if (report.result != GenderResult.UNDETERMINED) {
                    val oppositeLabel = if (targetGender == "FEMALE") "Male Profile Kept" else "Female Profile Kept"
                    Logger.log(
                        appName = name,
                        action = oppositeLabel,
                        details = "Protected non-target match: ${report.matchedTokens}. Enjoy chatting!",
                        status = TriggerStatus.SUCCESS
                    )
                    return
                }
            }
        }
    }

    /**
     * Executes skipping an active partner who matches skip criteria but hasn't disconnected.
     * In AnonChat, to end an active chat, we MUST click "End chat" (NOT "New partner").
     */
    private fun executeSkipActivePartner(
        rootNode: AccessibilityNodeInfo,
        service: SkipperAccessibilityService
    ) {
        isExecutingAction = true
        lastTriggerTime = System.currentTimeMillis()
        val delay = prefs.anonChatClickDelayMs

        handler.postDelayed({
            try {
                val freshRoot = service.rootInActiveWindow ?: rootNode

                // Step 1: In an active chat, find and click the "End chat" button
                val endChatNodes = NodeMatcher.findNodes(freshRoot) { node ->
                    val text = node.text?.toString() ?: node.contentDescription?.toString()
                    text != null && (text.trim().equals("End chat", ignoreCase = true) ||
                                     text.contains("End chat", ignoreCase = true) ||
                                     text.contains("Leave chat", ignoreCase = true))
                }
                val endChatTarget = endChatNodes.lastOrNull()?.let { NodeMatcher.findClickableTarget(it) ?: it }

                if (endChatTarget != null) {
                    performClickSafely(endChatTarget, service)
                    Logger.log(name, "Clicked 'End chat'", "Ending chat with non-matching partner.", TriggerStatus.INFO)

                    // Step 1b: If AnonChat displays a confirmation dialog, click "End chat" again
                    handler.postDelayed({
                        try {
                            val dialogRoot = service.rootInActiveWindow
                            if (dialogRoot != null) {
                                val confirmNodes = NodeMatcher.findNodes(dialogRoot) { n ->
                                    val t = n.text?.toString() ?: n.contentDescription?.toString()
                                    t != null && t.trim().equals("End chat", ignoreCase = true)
                                }
                                confirmNodes.firstOrNull()?.let {
                                    val target = NodeMatcher.findClickableTarget(it) ?: it
                                    performClickSafely(target, service)
                                    Logger.log(name, "Confirmed 'End chat'", "Confirmed exit dialog.", TriggerStatus.INFO)
                                }
                            }
                        } catch (e: Exception) {
                            // Dialog did not appear
                        }
                    }, 350L)
                    return@postDelayed
                }

                // Step 2: Fallback via top-right action icon (!) or Android Back action
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
                    Logger.log(name, "Action Icon Triggered", "Opening end chat menu for target partner.", TriggerStatus.INFO)
                } else {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    Logger.log(name, "Back Action Triggered", "Prompting end chat for target partner.", TriggerStatus.INFO)
                }
            } catch (e: Exception) {
                Logger.log(name, "Error", "Exception skipping partner: ${e.message}", TriggerStatus.ERROR)
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
            if (capturing) {
                // Stop capturing when reaching bottom input or actions
                if (text.contains("Write a message", ignoreCase = true) ||
                    text.contains("Send friend request", ignoreCase = true) ||
                    text.contains("End chat", ignoreCase = true)) {
                    break
                }
                sb.append(text).append(" ")
            }
        }

        if (sb.isNotEmpty()) {
            return sb.toString().trim()
        }
        return allTexts.filter {
            !it.contains("New partner", ignoreCase = true) &&
            !it.contains("anon.chat", ignoreCase = true) &&
            !it.contains("Write a message", ignoreCase = true) &&
            !it.contains("Send friend request", ignoreCase = true) &&
            !it.contains("End chat", ignoreCase = true)
        }.joinToString(" ").trim()
    }

    private fun evaluateIsInstantSkipScreen4(rootNode: AccessibilityNodeInfo): Boolean {
        val allTexts = NodeMatcher.collectAllText(rootNode)
        val hasProfileHeader = allTexts.any { it.contains("About partner", ignoreCase = true) }
        val hasChatStarts = allTexts.any { it.contains("Chat starts", ignoreCase = true) }
        val hasInterests = allTexts.any { it.contains("Interests", ignoreCase = true) }

        return hasProfileHeader || hasChatStarts || hasInterests
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

            // Must be in bottom half of screen to avoid top toolbar title "New partner"
            val isAtBottom = bounds.top > (displayHeight * 0.55)
            if (isAtBottom) {
                return NodeMatcher.findClickableTarget(node) ?: node
            }
        }

        return null
    }

    private fun performClickSafely(
        node: AccessibilityNodeInfo,
        service: SkipperAccessibilityService
    ): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val target = NodeMatcher.findClickableTarget(node) ?: node
        val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        // Always also dispatch gesture coordinates if bounds are valid to guarantee the physical touch is registered
        if (!bounds.isEmpty && bounds.width() > 0 && bounds.height() > 0) {
            val cx = bounds.centerX().toFloat()
            val cy = bounds.centerY().toFloat()
            service.clickCoordinates(cx, cy)
            return true
        }
        return clicked
    }
}
