package com.skipper.app.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
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

        // Master Switch setup
        binding.switchMaster.isChecked = prefs.isMasterSwitchEnabled
        binding.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            prefs.isMasterSwitchEnabled = isChecked
            Logger.log("Skipper", "Master Switch", if (isChecked) "Automation enabled" else "Automation paused")
        }

        // Accessibility Service check & button
        binding.btnEnableService.setOnClickListener {
            if (!prefs.isAccessibilityDisclosureAccepted) {
                ProminentDisclosureDialog(this, prefs) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }.show()
            } else {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }

        SkipperAccessibilityService.isServiceConnected.observe(this) { isConnected ->
            if (isConnected) {
                binding.indicatorStatus.setBackgroundColor(Color.parseColor("#4CAF50"))
                binding.tvServiceStatus.text = getString(R.string.service_active)
                binding.btnEnableService.text = "Settings"
            } else {
                binding.indicatorStatus.setBackgroundColor(Color.parseColor("#F44336"))
                binding.tvServiceStatus.text = getString(R.string.service_inactive)
                binding.btnEnableService.text = "Enable"
            }
        }

        // App Sections RecyclerView
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

        // Live Logs RecyclerView
        logAdapter = LogAdapter()
        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = logAdapter

        Logger.logsLiveData.observe(this) { logs ->
            logAdapter.updateLogs(logs)
        }

        binding.btnClearLogs.setOnClickListener {
            Logger.clear()
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as SkipperApp
        appSectionAdapter.updateItems(app.automationEngine.getAllAppSections())
    }
}
