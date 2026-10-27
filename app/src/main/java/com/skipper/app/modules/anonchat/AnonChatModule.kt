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
            // Guard: If any text exchange took place, NEVER auto-skip!
            if (hasChatExchanges(rootNode)) {
                Logger.log(
                    appName = name,
                    action = "Conversation Protected",
                    details = "Messages were exchanged before chat ended. Auto-skip prevented so you can view chat history.",
                    status = TriggerStatus.BLOCKED_BY_SAFEGUARD
                )
                return
            }

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
            // Guard: If conversation is already underway, do not trigger bio skip!
            if (hasChatExchanges(rootNode)) {
                return
            }

            val hasProfileCard = NodeMatcher.hasText(rootNode, "About partner") ||
                    NodeMatcher.hasText(rootNode, "Chat starts")

            if (hasProfileCard) {
                val bioText = extractBioAndProfileText(rootNode)
                val targetGender = prefs.anonChatGenderTarget
                val customKeywords = prefs.anonChatCustomKeywords
                val neverSkipKeywords = prefs.anonChatNeverSkipKeywords
                val report = GenderClassifier.classify(
                    rawText = bioText,
                    customKeywords = customKeywords,
                    nonSkipKeywords = neverSkipKeywords,
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
                        details = "Matched tokens: ${report.matchedTokens} in bio (\"$bioText\"). Auto-skipping to new partner...",
                        status = TriggerStatus.INFO
                    )
                    val newPartnerButton = locateBottomNewPartnerButton(rootNode, service)
                        ?: service.rootInActiveWindow?.let { locateBottomNewPartnerButton(it, service) }
                    if (newPartnerButton != null) {
                        executeClickNewPartner(newPartnerButton, service, label)
                    } else {
                        Logger.log(name, "Warning", "Could not locate 'New partner' button to skip.", TriggerStatus.INFO)
                    }
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

    private fun executeClickNewPartner(
        buttonNode: AccessibilityNodeInfo,
        service: SkipperAccessibilityService,
        reason: String
    ) {
        if (!prefs.canSkip()) {
            Logger.log(
                appName = name,
                action = "Free Trial Ended",
                details = "10 free skips used! Subscribe to PRO in Skipper for unlimited automatic skipping.",
                status = TriggerStatus.BLOCKED_BY_SAFEGUARD
            )
            return
        }

        isExecutingAction = true
        lastTriggerTime = System.currentTimeMillis()
        val delay = prefs.anonChatClickDelayMs

        Logger.log(name, "Trigger: $reason", "Clicking 'New partner' in ${delay}ms...", TriggerStatus.INFO)

        handler.postDelayed({
            try {
                val freshRoot = service.rootInActiveWindow ?: buttonNode
                val target = locateBottomNewPartnerButton(freshRoot, service) ?: buttonNode

                // Defensive check: Ensure the target node is strictly in the lower region of the screen
                val targetBounds = Rect()
                target.getBoundsInScreen(targetBounds)
                val displayHeight = service.resources.displayMetrics.heightPixels
                if (targetBounds.top < (displayHeight * 0.50)) {
                    Logger.log(name, "Safety Block", "Target was in top half (${targetBounds.top}px). Aborting click to protect bio header.", TriggerStatus.BLOCKED_BY_SAFEGUARD)
                    return@postDelayed
                }

                val clicked = performClickSafely(target, service)
                if (clicked) {
                    val remaining = prefs.consumeSkip()
                    val skipInfo = if (prefs.isProActivated) "PRO Unlimited" else "$remaining trial skip(s) left"
                    Logger.log(name, "Clicked 'New partner'", "Successfully transitioned to next search ($skipInfo).", TriggerStatus.SUCCESS)
                }
            } catch (e: Exception) {
                Logger.log(name, "Error", "Exception clicking 'New partner': ${e.message}", TriggerStatus.ERROR)
            } finally {
                isExecutingAction = false
            }
        }, delay)
    }

    /**
     * Checks if any chat messages or conversational exchanges have taken place.
     * In AnonChat, any message sent or received creates timestamp nodes (e.g. "3:44 PM", "Seen · 3:45 PM"),
     * outgoing bubble indicators ("Me"), or message read receipts ("Seen", "Delivered").
     */
    private fun hasChatExchanges(rootNode: AccessibilityNodeInfo): Boolean {
        val allTexts = NodeMatcher.collectAllText(rootNode)

        // 1. Any message timestamps: e.g. "3:45 PM", "12:04 AM", "3:45 pm"
        val timeRegex = Regex("""\b\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)\b""")
        if (allTexts.any { timeRegex.containsMatchIn(it) }) {
            return true
        }

        // 2. Message status indicators: "Me", "Seen", or "Seen · 3:45 PM", "Delivered"
        if (allTexts.any { it.equals("Me", ignoreCase = false) || it.startsWith("Seen", ignoreCase = true) || it.equals("Delivered", ignoreCase = true) }) {
            return true
        }

        // 3. If chat was ended, but the initial profile card is no longer visible,
        // then the screen was previously filled with active conversation messages!
        val hasProfileCard = allTexts.any { it.contains("About partner", ignoreCase = true) || it.contains("Interests", ignoreCase = true) }
        val isChatEnded = allTexts.any { it.contains("Chat was ended", ignoreCase = true) || it.contains("has left the chat", ignoreCase = true) }
        if (isChatEnded && !hasProfileCard) {
            return true
        }

        return false
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
        if (hasChatExchanges(rootNode)) {
            return false
        }

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

        // Search candidates from bottom to top so the actual bottom button is prioritized
        for (node in candidateNodes.reversed()) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            // Must be strictly in the lower region of the screen (> 60% of display height)
            // This strictly excludes the top header title/avatar at ~6% of display height
            if (bounds.top > (displayHeight * 0.60)) {
                if (node.isClickable) {
                    return node
                }
                val parentTarget = NodeMatcher.findClickableTarget(node)
                if (parentTarget != null) {
                    val parentBounds = Rect()
                    parentTarget.getBoundsInScreen(parentBounds)
                    if (parentBounds.top > (displayHeight * 0.60)) {
                        return parentTarget
                    }
                }
                return node
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
        val target = if (node.isClickable) node else (NodeMatcher.findClickableTarget(node) ?: node)
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
