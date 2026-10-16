package com.skipper.app.engine

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Utility functions for safely traversing and querying the Accessibility View Hierarchy.
 * Provides defensive safeguards against ambiguous matches, header titles, and message bubbles.
 */
object NodeMatcher {

    /**
     * Recursively traverses nodes and collects all visible text strings on screen.
     */
    fun collectAllText(root: AccessibilityNodeInfo?): List<String> {
        val texts = mutableListOf<String>()
        if (root == null) return texts

        fun traverse(node: AccessibilityNodeInfo) {
            val text = node.text?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                texts.add(text)
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { traverse(it) }
            }
        }

        traverse(root)
        return texts
    }

    /**
     * Checks if any node on screen contains the specified text (case-insensitive).
     */
    fun hasText(root: AccessibilityNodeInfo?, targetText: String, ignoreCase: Boolean = true): Boolean {
        if (root == null) return false
        val cleanTarget = targetText.trim()

        fun traverse(node: AccessibilityNodeInfo): Boolean {
            val nodeText = node.text?.toString() ?: node.contentDescription?.toString()
            if (nodeText != null && nodeText.contains(cleanTarget, ignoreCase = ignoreCase)) {
                return true
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null && traverse(child)) {
                    return true
                }
            }
            return false
        }

        return traverse(root)
    }

    /**
     * Finds all nodes matching the predicate.
     */
    fun findNodes(
        root: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        if (root == null) return results

        fun traverse(node: AccessibilityNodeInfo) {
            if (predicate(node)) {
                results.add(node)
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { traverse(it) }
            }
        }

        traverse(root)
        return results
    }

    /**
     * Checks whether an active text input (EditText) is currently visible on the screen.
     */
    fun hasActiveTextInput(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val editTexts = findNodes(root) { node ->
            node.className?.toString()?.contains("EditText", ignoreCase = true) == true ||
            node.isEditable
        }
        return editTexts.isNotEmpty()
    }

    /**
     * Finds a clickable target: if the node itself is not clickable, climbs up to its clickable parent.
     */
    fun findClickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current
            }
            current = current.parent
        }
        return null
    }

    /**
     * Gets node screen bounds in pixel coordinates.
     */
    fun getBounds(node: AccessibilityNodeInfo): Rect {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect
    }
}
