package com.skipper.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skipper.app.SkipperApp
import com.skipper.app.model.TriggerStatus
import com.skipper.app.util.Logger

class SkipperAccessibilityService : AccessibilityService() {

    companion object {
        private val _isServiceConnected = MutableLiveData(false)
        val isServiceConnected: LiveData<Boolean> = _isServiceConnected

        var instance: SkipperAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.postValue(true)
        Logger.log(
            appName = "Skipper",
            action = "Service Connected",
            details = "Accessibility Service is now active and monitoring triggers.",
            status = TriggerStatus.SUCCESS
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val app = application as? SkipperApp ?: return
        val rootNode = rootInActiveWindow

        app.automationEngine.dispatchEvent(event, rootNode, this)
    }

    override fun onInterrupt() {
        Logger.log(
            appName = "Skipper",
            action = "Service Interrupted",
            details = "Accessibility service was paused or interrupted by system.",
            status = TriggerStatus.INFO
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceConnected.postValue(false)
        Logger.log(
            appName = "Skipper",
            action = "Service Destroyed",
            details = "Accessibility service stopped.",
            status = TriggerStatus.INFO
        )
    }

    /**
     * Dispatches an emulated tap gesture at coordinates (x, y).
     * Essential fallback when custom views or wrapped components don't respond to ACTION_CLICK.
     */
    fun clickCoordinates(x: Float, y: Float, durationMs: Long = 50L): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            return dispatchGesture(gesture, null, null)
        }
        return false
    }
}
