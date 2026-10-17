package com.skipper.app.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import com.google.android.material.button.MaterialButton
import com.skipper.app.R
import com.skipper.app.util.PreferencesManager

/**
 * Google Play Store Compliant Prominent Disclosure Dialog for AccessibilityService.
 * Strictly adheres to Google Play's User Data & Accessibility Services policy.
 */
class ProminentDisclosureDialog(
    context: Context,
    private val prefs: PreferencesManager,
    private val onAccepted: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_prominent_disclosure)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)

        val btnAccept = findViewById<MaterialButton>(R.id.btnAcceptDisclosure)
        val btnDecline = findViewById<MaterialButton>(R.id.btnDeclineDisclosure)

        btnAccept.setOnClickListener {
            prefs.isAccessibilityDisclosureAccepted = true
            dismiss()
            onAccepted()
        }

        btnDecline.setOnClickListener {
            dismiss()
        }
    }
}
