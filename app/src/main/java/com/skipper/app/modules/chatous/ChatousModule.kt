package com.skipper.app.modules.chatous

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

class ChatousModule(
    private val prefs: PreferencesManager
) : BaseAppModule {

    override val id: String = "chatous"
    override val name: String = "Chatous"

    override val targetPackages: List<String> = listOf(
        "com.chatous.chatous",
        "chatous"
    )

    private val handler = Handler(Looper.getMainLooper())
    private var lastTriggerTime: Long = 0L
    private var isExecutingAction: Boolean = false

    override fun isEnabled(): Boolean = prefs.isChatousEnabled

    override fun getAppSection(): AppSection {
        val target = if (prefs.chatousGenderTarget == "FEMALE") "Female" else "Male"
        val ruleSummary = if (prefs.isChatousSkipMale) {
            "Trigger: Auto-skip $target profiles (Screen 4)"
        } else {
            "Monitoring only"
        }
        return AppSection(
            id = id,
            name = name,
            targetPackages = targetPackages,
            description = "Auto-skips male profiles instantly upon matching",
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

        val now = System.currentTimeMillis()
        if (isExecutingAction || (now - lastTriggerTime < prefs.chatousCooldownMs)) {
            return
        }

        // Safeguard 1: Do not trigger on Home Screen (Screen 1)
        if (NodeMatcher.hasText(rootNode, "Featured Users") || NodeMatcher.hasText(rootNode, "has ended the chat")) {
            return
        }

        // Safeguard 2: Do not trigger on Search Setup Screen (Screen 2)
        if (NodeMatcher.hasText(rootNode, "START CHAT") || NodeMatcher.hasText(rootNode, "More Options")) {
            return
        }

        // Safeguard 3: Do not trigger on Searching/Waiting Screen (Screen 3)
        if (NodeMatcher.hasText(rootNode, "STOP WAITING") || NodeMatcher.hasText(rootNode, "WAITING FOR PERSON")) {
            return
        }

        // Safeguard 4: Screen 4 (Found Person) Verification
        // Must contain "Let's Chat!" / "Let’s Chat!" AND "SKIP"
        val hasLetsChat = NodeMatcher.hasText(rootNode, "Let's Chat") ||
                NodeMatcher.hasText(rootNode, "Let’s Chat")
        val hasSkipButton = NodeMatcher.hasText(rootNode, "SKIP")

        if (!hasLetsChat || !hasSkipButton) {
            return
        }

        // Screen 4 is confirmed!
        val allTexts = NodeMatcher.collectAllText(rootNode)

        val targetGender = prefs.chatousGenderTarget
        val customKeywords = prefs.chatousCustomKeywords

        val maleRegex = Regex("""\bMale\b""", RegexOption.IGNORE_CASE)
        val femaleRegex = Regex("""\bFemale\b""", RegexOption.IGNORE_CASE)

        val hasMaleWord = allTexts.any { maleRegex.containsMatchIn(it) }
        val hasMaleSymbol = allTexts.any { it.contains("♂") }
        val isMale = hasMaleWord || hasMaleSymbol

        val hasFemaleWord = allTexts.any { femaleRegex.containsMatchIn(it) }
        val hasFemaleSymbol = allTexts.any { it.contains("♀") }
        val isFemale = hasFemaleWord || hasFemaleSymbol

        // Check custom keywords
        val matchedCustom = customKeywords.filter { kw ->
            val trimmed = kw.trim()
            trimmed.isNotEmpty() && allTexts.any { it.contains(trimmed, ignoreCase = true) }
        }

        val shouldSkip = if (matchedCustom.isNotEmpty()) {
            true
        } else if (targetGender == "FEMALE") {
            isFemale
        } else {
            isMale
        }

        if (!shouldSkip) {
            val label = if (targetGender == "FEMALE") "Male/Non-targeted profile" else "Female/Non-targeted profile"
            Logger.log(
                appName = name,
                action = "Partner Matched",
                details = "$label found. Skipping aborted - ready to chat!",
                status = TriggerStatus.INFO
            )
            return
        }

        if (!prefs.isChatousSkipMale) {
            return
        }

        // Locate the "SKIP" button at the bottom of the screen
        val skipButtonNode = locateSkipButton(rootNode, service)
        if (skipButtonNode == null) {
            Logger.log(
                appName = name,
                action = "Button Guard",
                details = "Target profile detected, but 'SKIP' button node could not be safely targeted.",
                status = TriggerStatus.BLOCKED_BY_SAFEGUARD
            )
            return
        }

        isExecutingAction = true
        lastTriggerTime = now
        val delay = prefs.chatousClickDelayMs

        val triggerReason = if (matchedCustom.isNotEmpty()) {
            "Matched custom keywords: $matchedCustom"
        } else if (targetGender == "FEMALE") {
            "Female Profile Detected"
        } else {
            "Male Profile Detected"
        }

        Logger.log(
            appName = name,
            action = triggerReason,
            details = "Trigger on Screen 4. Auto-clicking 'SKIP' in ${delay}ms...",
            status = TriggerStatus.INFO
        )

        handler.postDelayed({
            try {
                val freshRoot = service.rootInActiveWindow ?: rootNode
                val target = locateSkipButton(freshRoot, service)
                if (target != null) {
                    val clicked = performClickSafely(target, service)
                    if (clicked) {
                        Logger.log(
                            appName = name,
                            action = "Clicked 'SKIP'",
                            details = "Successfully skipped male match. Back to search.",
                            status = TriggerStatus.SUCCESS
                        )
                    } else {
                        Logger.log(
                            appName = name,
                            action = "Click Failed",
                            details = "Accessibility click failed; fallback gesture dispatched.",
                            status = TriggerStatus.ERROR
                        )
                    }
                }
            } catch (e: Exception) {
                Logger.log(
                    appName = name,
                    action = "Error",
                    details = "Exception during Chatous skip: ${e.message}",
                    status = TriggerStatus.ERROR
                )
            } finally {
                isExecutingAction = false
            }
        }, delay)
    }

    private fun locateSkipButton(
        rootNode: AccessibilityNodeInfo,
        service: SkipperAccessibilityService
    ): AccessibilityNodeInfo? {
        val displayHeight = service.resources.displayMetrics.heightPixels

        // Look for nodes matching "SKIP"
        val candidates = NodeMatcher.findNodes(rootNode) { node ->
            val text = node.text?.toString() ?: node.contentDescription?.toString()
            text != null && text.trim().equals("SKIP", ignoreCase = true)
        }

        for (node in candidates) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            // SKIP button is located at the lower region of the screen (bottom 35%)
            if (bounds.top > (displayHeight * 0.65)) {
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
