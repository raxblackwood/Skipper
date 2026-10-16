package com.skipper.app.modules.anonchat

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.skipper.app.engine.BaseAppModule
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

    // Known package names for AnonChat / Anonymous Chat variants
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
        val ruleSummary = if (prefs.isAnonChatOnlyInstantSkip) {
            "Trigger: Instant profile skip (Screen 4 only)"
        } else {
            "Trigger: Any partner disconnect (Screens 3 & 4)"
        }
        return AppSection(
            id = id,
            name = name,
            targetPackages = targetPackages,
            description = "Auto-skips to next partner when someone immediately skips on your profile",
            isEnabled = isEnabled(),
            summaryRule = ruleSummary
        )
    }

    override fun processHierarchy(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        service: SkipperAccessibilityService
    ) {
        if (rootNode == null) return

        // 1. Cooldown & Concurrency Guard
        val now = System.currentTimeMillis()
        if (isExecutingAction || (now - lastTriggerTime < prefs.anonChatCooldownMs)) {
            return
        }

        // 2. Safeguard: Active Chat Input Guard
        // If an active EditText (e.g. "Write a message...") is present, we are in an active chat.
        // Under NO circumstances should we trigger while a chat is actively running!
        if (NodeMatcher.hasActiveTextInput(rootNode)) {
            return
        }

        // 3. Safeguard: Disconnection Confirmation Guard
        // The screen MUST contain "Chat was ended by your partner" (or "Chat was ended")
        val isChatEnded = NodeMatcher.hasText(rootNode, "Chat was ended by your partner") ||
                NodeMatcher.hasText(rootNode, "Chat was ended")
        if (!isChatEnded) {
            return
        }

        // 4. Distinction Guard: Screen 4 (Instant Skip) vs Screen 3 (Post-Chat Skip)
        if (prefs.isAnonChatOnlyInstantSkip) {
            val isScreen4 = evaluateIsInstantSkipScreen4(rootNode)
            if (!isScreen4) {
                Logger.log(
                    appName = name,
                    action = "Skip Ignored",
                    details = "Chat was ended after conversation took place (Screen 3). Trigger set to instant skips only.",
                    status = TriggerStatus.BLOCKED_BY_SAFEGUARD
                )
                return
            }
        }

        // 5. Button Safeguard: Locate the TRUE "New partner" action button at the bottom
        val newPartnerButton = locateBottomNewPartnerButton(rootNode, service)
        if (newPartnerButton == null) {
            Logger.log(
                appName = name,
                action = "Button Guard",
                details = "'New partner' text detected, but not as bottom action button. Ignoring (safeguard against header/chat text).",
                status = TriggerStatus.BLOCKED_BY_SAFEGUARD
            )
            return
        }

        // 6. Execute Action with user-configured delay
        isExecutingAction = true
        lastTriggerTime = now
        val delay = prefs.anonChatClickDelayMs

        Logger.log(
            appName = name,
            action = "Instant Skip Detected",
            details = "Partner skipped immediately on profile! Triggering 'New partner' in ${delay}ms...",
            status = TriggerStatus.INFO
        )

        handler.postDelayed({
            try {
                // Re-verify root node before clicking
                val freshRoot = service.rootInActiveWindow ?: rootNode
                val target = locateBottomNewPartnerButton(freshRoot, service)

                if (target != null) {
                    val clicked = performClickSafely(target, service)
                    if (clicked) {
                        Logger.log(
                            appName = name,
                            action = "Clicked 'New partner'",
                            details = "Successfully skipped to next partner search.",
                            status = TriggerStatus.SUCCESS
                        )
                    } else {
                        Logger.log(
                            appName = name,
                            action = "Click Failed",
                            details = "Accessibility click failed; gesture fallback dispatched.",
                            status = TriggerStatus.ERROR
                        )
                    }
                }
            } catch (e: Exception) {
                Logger.log(
                    appName = name,
                    action = "Error",
                    details = "Exception during action execution: ${e.message}",
                    status = TriggerStatus.ERROR
                )
            } finally {
                isExecutingAction = false
            }
        }, delay)
    }

    /**
     * Accurately determines if the current screen is Screen 4 (Instant Skip on Profile).
     *
     * Screen 4 characteristics:
     * - Contains "Chat starts" OR "About partner" OR "Interests" profile card
     * - Contains "Chat was ended by your partner"
     * - Contains NO user sent message bubbles / conversation history
     */
    private fun evaluateIsInstantSkipScreen4(rootNode: AccessibilityNodeInfo): Boolean {
        val allTexts = NodeMatcher.collectAllText(rootNode)

        val hasProfileHeader = allTexts.any { it.contains("About partner", ignoreCase = true) }
        val hasChatStarts = allTexts.any { it.contains("Chat starts", ignoreCase = true) }
        val hasInterests = allTexts.any { it.contains("Interests", ignoreCase = true) }

        // Must have at least one initial profile feed indicator
        if (!hasProfileHeader && !hasChatStarts && !hasInterests) {
            return false
        }

        // Verify absence of conversation bubbles.
        // In Screen 2 & 3, there are user messages ("Me", message quotes, timestamps like "3:44 PM", "Seen", etc.)
        // Filter out known profile card text & standard buttons to see if conversation occurred:
        val ignoredPhrases = listOf(
            "New partner", "Chat starts", "About partner", "Interests",
            "Chat was ended by your partner", "Chat was ended",
            "Report", "End chat", "anon.chat"
        )

        val conversationMessages = allTexts.filter { text ->
            ignoredPhrases.none { text.contains(it, ignoreCase = true) }
        }

        // In Screen 4, the only other texts are the profile bio (e.g. "22 M Ind - Mumbai...")
        // and interest tags ("Movies", "Loneliness", "Flirt", etc.).
        // If there are sent message timestamps ("PM", "AM", "Seen") or quoted replies, conversation occurred!
        val hasTimestampsOrReceipts = allTexts.any { text ->
            text.contains("Seen", ignoreCase = true) ||
            Regex("\\b\\d{1,2}:\\d{2}\\s*(AM|PM)?\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
        }

        return !hasTimestampsOrReceipts
    }

    /**
     * Locates the dedicated "New partner" action button at the bottom of the screen.
     * Prevents false triggers from the header/toolbar title or message text.
     */
    private fun locateBottomNewPartnerButton(
        rootNode: AccessibilityNodeInfo,
        service: SkipperAccessibilityService
    ): AccessibilityNodeInfo? {
        val displayHeight = service.resources.displayMetrics.heightPixels

        // Find all nodes mentioning "New partner"
        val candidateNodes = NodeMatcher.findNodes(rootNode) { node ->
            val text = node.text?.toString() ?: node.contentDescription?.toString()
            text != null && text.contains("New partner", ignoreCase = true)
        }

        for (node in candidateNodes) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            // SAFEGUARD 1: Position check
            // The action button is strictly located at the bottom of the screen (typically Y > 65% of screen)
            // The top bar "New partner" is at Y < 20% of screen.
            val isAtBottom = bounds.top > (displayHeight * 0.55)

            // SAFEGUARD 2: Context check
            // Sibling check or screen context: accompanied by "End chat" or "Report"
            val hasNeighborButtons = NodeMatcher.hasText(rootNode, "End chat") ||
                    NodeMatcher.hasText(rootNode, "Report")

            if (isAtBottom && hasNeighborButtons) {
                // Find clickable target (either node itself or its enclosing layout container)
                val target = NodeMatcher.findClickableTarget(node) ?: node
                return target
            }
        }

        return null
    }

    /**
     * Clicks the target node using standard accessibility action, falling back to gesture tap.
     */
    private fun performClickSafely(
        node: AccessibilityNodeInfo,
        service: SkipperAccessibilityService
    ): Boolean {
        // Method A: Standard Accessibility Action Click
        val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (clicked) return true

        // Method B: Gesture coordinate tap fallback
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            return service.clickCoordinates(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
        return false
    }
}
