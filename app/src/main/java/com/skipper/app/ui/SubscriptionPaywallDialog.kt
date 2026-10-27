package com.skipper.app.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.skipper.app.R
import com.skipper.app.SkipperApp
import com.skipper.app.billing.BillingManager
import com.skipper.app.databinding.DialogSubscriptionPaywallBinding
import com.skipper.app.util.LicenseManager

class SubscriptionPaywallDialog(
    private val activity: Activity,
    private val onSubscriptionUpdated: () -> Unit = {}
) : BottomSheetDialog(activity, R.style.Theme_Skipper) {

    private lateinit var binding: DialogSubscriptionPaywallBinding
    private val app = activity.application as SkipperApp
    private val prefs = app.preferencesManager
    private val billing = app.billingManager

    private var selectedProductId = BillingManager.SUB_WEEKLY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSubscriptionPaywallBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        setupTrialStatus()
        setupTierSelection()
        setupLivePricing()
        setupActions()
    }

    private fun setupTrialStatus() {
        val remaining = prefs.remainingFreeSkips
        if (prefs.isProActivated) {
            val tierName = when (prefs.subscriptionTier) {
                BillingManager.SUB_DAILY -> "Daily VIP"
                BillingManager.SUB_WEEKLY -> "Weekly Pass"
                BillingManager.SUB_MONTHLY -> "Monthly Unlimited"
                "PRO_TEST" -> "Test PRO (Unlimited)"
                "PRO_PROMO" -> "Promo Unlocked"
                else -> "PRO Active (Unlimited)"
            }
            binding.tvTrialStatus.text = "⭐ $tierName"
            binding.tvTrialStatus.setTextColor(Color.parseColor("#81C784"))
            binding.btnSubscribe.text = "PRO Active"
            binding.btnResetTrialDebug.text = "Reset to 10"
        } else if (remaining > 0) {
            binding.tvTrialStatus.text = "Free Trial: $remaining of 10 skips remaining"
            binding.tvTrialStatus.setTextColor(Color.parseColor("#E1BEE7"))
            binding.btnResetTrialDebug.text = "Unlock PRO"
        } else {
            binding.tvTrialStatus.text = "⚠️ Free Trial Expired (10/10 skips used)"
            binding.tvTrialStatus.setTextColor(Color.parseColor("#FF8A80"))
            binding.btnResetTrialDebug.text = "Unlock PRO"
        }

        binding.btnResetTrialDebug.setOnClickListener {
            if (prefs.isProActivated) {
                prefs.resetTrial()
                Toast.makeText(context, "Reset to Free Trial (10 skips).", Toast.LENGTH_SHORT).show()
            } else {
                prefs.activateTestPro()
                Toast.makeText(context, "Unlimited Test PRO Activated! Enjoy testing.", Toast.LENGTH_SHORT).show()
            }
            setupTrialStatus()
            onSubscriptionUpdated()
        }
    }

    private fun setupTierSelection() {
        fun updateSelection(productId: String) {
            selectedProductId = productId
            binding.rbTierDaily.isChecked = productId == BillingManager.SUB_DAILY
            binding.rbTierWeekly.isChecked = productId == BillingManager.SUB_WEEKLY
            binding.rbTierMonthly.isChecked = productId == BillingManager.SUB_MONTHLY

            binding.cardTierDaily.strokeWidth = if (productId == BillingManager.SUB_DAILY) 4 else 2
            binding.cardTierDaily.strokeColor = if (productId == BillingManager.SUB_DAILY) Color.parseColor("#BB86FC") else Color.parseColor("#3A3842")

            binding.cardTierWeekly.strokeWidth = if (productId == BillingManager.SUB_WEEKLY) 4 else 2
            binding.cardTierWeekly.strokeColor = if (productId == BillingManager.SUB_WEEKLY) Color.parseColor("#BB86FC") else Color.parseColor("#3A3842")

            binding.cardTierMonthly.strokeWidth = if (productId == BillingManager.SUB_MONTHLY) 4 else 2
            binding.cardTierMonthly.strokeColor = if (productId == BillingManager.SUB_MONTHLY) Color.parseColor("#BB86FC") else Color.parseColor("#3A3842")
        }

        binding.cardTierDaily.setOnClickListener { updateSelection(BillingManager.SUB_DAILY) }
        binding.rbTierDaily.setOnClickListener { updateSelection(BillingManager.SUB_DAILY) }

        binding.cardTierWeekly.setOnClickListener { updateSelection(BillingManager.SUB_WEEKLY) }
        binding.rbTierWeekly.setOnClickListener { updateSelection(BillingManager.SUB_WEEKLY) }

        binding.cardTierMonthly.setOnClickListener { updateSelection(BillingManager.SUB_MONTHLY) }
        binding.rbTierMonthly.setOnClickListener { updateSelection(BillingManager.SUB_MONTHLY) }

        updateSelection(BillingManager.SUB_WEEKLY)
    }

    private fun setupLivePricing() {
        billing.productDetailsMap.observe(activity as androidx.lifecycle.LifecycleOwner) { detailsMap ->
            detailsMap[BillingManager.SUB_DAILY]?.let { details ->
                val price = details.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                if (price != null) binding.tvPriceDaily.text = "$price / day"
            }
            detailsMap[BillingManager.SUB_WEEKLY]?.let { details ->
                val price = details.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                if (price != null) binding.tvPriceWeekly.text = "$price / wk"
            }
            detailsMap[BillingManager.SUB_MONTHLY]?.let { details ->
                val price = details.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                if (price != null) binding.tvPriceMonthly.text = "$price / mo"
            }
        }

        billing.billingError.observe(activity as androidx.lifecycle.LifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                binding.tvBillingMessage.text = errorMsg
                binding.tvBillingMessage.visibility = View.VISIBLE
            }
        }
    }

    private fun setupActions() {
        binding.btnClosePaywall.setOnClickListener {
            dismiss()
        }

        binding.btnSubscribe.setOnClickListener {
            if (prefs.isProActivated) {
                Toast.makeText(context, "You are already a PRO subscriber!", Toast.LENGTH_SHORT).show()
                dismiss()
                return@setOnClickListener
            }

            // Launch Google Play Billing subscription flow
            val hasProduct = billing.productDetailsMap.value?.containsKey(selectedProductId) == true
            if (hasProduct) {
                billing.launchSubscriptionFlow(activity, selectedProductId)
                dismiss()
            } else {
                // If Play Billing isn't available yet (e.g. app not yet published in Play Console),
                // explain clearly and offer a test activation
                AlertDialog.Builder(activity)
                    .setTitle("Google Play Store Setup")
                    .setMessage("Live Play Store subscriptions will be active once published on Google Play.\n\nWould you like to activate a test PRO subscription right now?")
                    .setPositiveButton("Activate Test PRO") { _, _ ->
                        prefs.activateTestPro()
                        setupTrialStatus()
                        onSubscriptionUpdated()
                        Toast.makeText(activity, "Test PRO Activated! Unlimited skips enabled.", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                    .setNegativeButton("Wait for Play Store", null)
                    .show()
            }
        }

        binding.btnRestorePurchases.setOnClickListener {
            billing.queryActivePurchases()
            Toast.makeText(context, "Checking Google Play subscriptions...", Toast.LENGTH_SHORT).show()
            binding.root.postDelayed({
                setupTrialStatus()
                onSubscriptionUpdated()
            }, 1000)
        }

        binding.btnEnterPromoKey.setOnClickListener {
            val deviceId = LicenseManager.getDeviceId(context)
            val input = EditText(context).apply {
                hint = "Enter SKIP-XXXX-XXXX-XXXX"
                setText(prefs.licenseKey)
                setSingleLine()
            }
            AlertDialog.Builder(activity)
                .setTitle("Redeem Promo Key")
                .setMessage("Device ID: $deviceId\nEnter your promotional key:")
                .setView(input)
                .setPositiveButton("Redeem") { _, _ ->
                    val key = input.text.toString().trim()
                    if (LicenseManager.validateKey(key, deviceId)) {
                        prefs.isProActivated = true
                        prefs.subscriptionTier = "PRO_PROMO"
                        prefs.licenseKey = key
                        setupTrialStatus()
                        onSubscriptionUpdated()
                        Toast.makeText(context, "Promo Key Redeemed! PRO Activated.", Toast.LENGTH_LONG).show()
                        dismiss()
                    } else {
                        Toast.makeText(context, "Invalid Promo Key.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNeutralButton("Auto-Generate Test Key") { _, _ ->
                    val testKey = LicenseManager.generateKeyForDevice(deviceId)
                    prefs.isProActivated = true
                    prefs.subscriptionTier = "PRO_TEST"
                    prefs.licenseKey = testKey
                    setupTrialStatus()
                    onSubscriptionUpdated()
                    Toast.makeText(context, "Test Key generated ($testKey) and activated!", Toast.LENGTH_LONG).show()
                    dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
