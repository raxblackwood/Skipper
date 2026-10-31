package com.skipper.app.ui

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.skipper.app.R
import com.skipper.app.SkipperApp
import com.skipper.app.databinding.ActivityMainBinding
import com.skipper.app.service.SkipperAccessibilityService
import com.skipper.app.ui.adapter.AppSectionAdapter
import com.skipper.app.ui.adapter.LogAdapter
import com.skipper.app.util.Logger

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appSectionAdapter: AppSectionAdapter
    private lateinit var logAdapter: LogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as SkipperApp
        val prefs = app.preferencesManager
        val engine = app.automationEngine

        // Three-panel menu button toggles the side drawer for logs
        binding.btnOpenMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.drawerLogsLayout.btnCloseDrawer.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.drawerLogsLayout.btnClearLogsDrawer.setOnClickListener {
            Logger.clear()
        }

        // Master Switch setup
        binding.switchMaster.isChecked = prefs.isMasterSwitchEnabled
        binding.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            prefs.isMasterSwitchEnabled = isChecked
            Logger.log("Skipper", "Master Switch", if (isChecked) "Automation enabled" else "Automation paused")
        }

        // Tapping the status indicator / text opens Accessibility Settings
        val openSettingsAction = {
            if (!prefs.isAccessibilityDisclosureAccepted) {
                ProminentDisclosureDialog(this, prefs) {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }.show()
            } else {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        binding.indicatorStatus.setOnClickListener { openSettingsAction() }
        binding.tvServiceStatus.setOnClickListener { openSettingsAction() }

        fun updateServiceStatusUI(isConnected: Boolean) {
            val active = isConnected || isAccessibilityServiceEnabled()
            val dot = binding.indicatorStatus.background as? GradientDrawable
            if (active) {
                dot?.setColor(Color.parseColor("#4CAF50"))
                binding.tvServiceStatus.text = getString(R.string.service_active)
                binding.tvServiceStatus.setTextColor(Color.parseColor("#81C784"))
            } else {
                dot?.setColor(Color.parseColor("#F44336"))
                binding.tvServiceStatus.text = "Tap to enable accessibility in Settings"
                binding.tvServiceStatus.setTextColor(Color.parseColor("#E57373"))
            }
        }

        SkipperAccessibilityService.isServiceConnected.observe(this) { isConnected ->
            updateServiceStatusUI(isConnected)
        }

        // Subscription & Free Trial UI Setup
        fun updateSubscriptionUI() {
            if (prefs.isProActivated) {
                val tierLabel = when (prefs.subscriptionTier) {
                    com.skipper.app.billing.BillingManager.SUB_DAILY -> "DAILY VIP"
                    com.skipper.app.billing.BillingManager.SUB_WEEKLY -> "WEEKLY PRO"
                    com.skipper.app.billing.BillingManager.SUB_MONTHLY -> "MONTHLY PRO"
                    "PRO_TEST" -> "TEST PRO"
                    "PRO_PROMO" -> "PROMO UNLOCKED"
                    else -> "PRO ACTIVE"
                }
                binding.tvSubscriptionBadge.text = tierLabel
                binding.tvSubscriptionBadge.setBackgroundColor(Color.parseColor("#2E7D32"))
                binding.tvSubscriptionSubtitle.text = "Unlimited automated skipping active"
                binding.btnOpenPaywall.text = "Manage"
            } else {
                val remaining = prefs.remainingFreeSkips
                if (remaining > 0) {
                    binding.tvSubscriptionBadge.text = "TRIAL ACTIVE"
                    binding.tvSubscriptionBadge.setBackgroundColor(Color.parseColor("#6200EE"))
                    binding.tvSubscriptionSubtitle.text = "$remaining of 10 free skips remaining"
                } else {
                    binding.tvSubscriptionBadge.text = "TRIAL EXPIRED"
                    binding.tvSubscriptionBadge.setBackgroundColor(Color.parseColor("#C62828"))
                    binding.tvSubscriptionSubtitle.text = "10 free skips used. Upgrade to continue."
                }
                binding.btnOpenPaywall.text = "Upgrade"
            }
        }
        updateSubscriptionUI()

        app.billingManager.isProSubscribed.observe(this) {
            updateSubscriptionUI()
        }

        binding.cardSubscriptionStatus.setOnClickListener {
            SubscriptionPaywallDialog(this) { updateSubscriptionUI() }.show()
        }

        binding.btnOpenPaywall.setOnClickListener {
            SubscriptionPaywallDialog(this) { updateSubscriptionUI() }.show()
        }

        // App Sections RecyclerView (Only App Name + Switch; Clicking opens app details)
        appSectionAdapter = AppSectionAdapter(
            items = engine.getAllAppSections(),
            onToggle = { section, isEnabled ->
                when (section.id) {
                    "anonchat" -> prefs.isAnonChatEnabled = isEnabled
                    "chatous" -> prefs.isChatousEnabled = isEnabled
                }
                Logger.log("Skipper", "${section.name} Toggle", if (isEnabled) "Activated" else "Disabled")
            },
            onConfigure = { section ->
                when (section.id) {
                    "anonchat" -> startActivity(Intent(this, AnonChatDetailActivity::class.java))
                    "chatous" -> startActivity(Intent(this, ChatousDetailActivity::class.java))
                }
            }
        )
        binding.rvAppSections.layoutManager = LinearLayoutManager(this)
        binding.rvAppSections.adapter = appSectionAdapter

        // Live Logs inside Side Navigation Drawer
        logAdapter = LogAdapter()
        binding.drawerLogsLayout.rvDrawerLogs.layoutManager = LinearLayoutManager(this)
        binding.drawerLogsLayout.rvDrawerLogs.adapter = logAdapter

        Logger.logsLiveData.observe(this) { logs ->
            logAdapter.updateLogs(logs)
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as SkipperApp
        appSectionAdapter.updateItems(app.automationEngine.getAllAppSections())
        app.billingManager.queryActivePurchases()

        val active = SkipperAccessibilityService.instance != null || isAccessibilityServiceEnabled()
        val dot = binding.indicatorStatus.background as? GradientDrawable
        if (active) {
            dot?.setColor(Color.parseColor("#4CAF50"))
            binding.tvServiceStatus.text = getString(R.string.service_active)
            binding.tvServiceStatus.setTextColor(Color.parseColor("#81C784"))
        } else {
            dot?.setColor(Color.parseColor("#F44336"))
            binding.tvServiceStatus.text = "Tap to enable accessibility in Settings"
            binding.tvServiceStatus.setTextColor(Color.parseColor("#E57373"))
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, SkipperAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }
}