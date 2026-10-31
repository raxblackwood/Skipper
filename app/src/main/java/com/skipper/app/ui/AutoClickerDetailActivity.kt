package com.skipper.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.skipper.app.SkipperApp
import com.skipper.app.databinding.ActivityAutoClickerDetailBinding

class AutoClickerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAutoClickerDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAutoClickerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as SkipperApp
        val prefs = app.preferencesManager

        // Populate current values
        binding.switchAutoClickerEnabled.isChecked = prefs.isAutoClickerEnabled

        if (prefs.autoClickMode == "CONTINUOUS") {
            binding.rbContinuousTapMode.isChecked = true
        } else {
            binding.rbSmartButtonMode.isChecked = true
        }

        binding.etClickInterval.setText(prefs.autoClickIntervalMs.toString())
        binding.etCooldown.setText(prefs.autoClickCooldownMs.toString())

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            val isEnabled = binding.switchAutoClickerEnabled.isChecked
            val mode = if (binding.rbContinuousTapMode.isChecked) "CONTINUOUS" else "SMART_BUTTON"
            val interval = binding.etClickInterval.text.toString().toLongOrNull() ?: 500L
            val cooldown = binding.etCooldown.text.toString().toLongOrNull() ?: 1000L

            prefs.isAutoClickerEnabled = isEnabled
            prefs.autoClickMode = mode
            prefs.autoClickIntervalMs = interval
            prefs.autoClickCooldownMs = cooldown

            Toast.makeText(this, "Auto-Clicker Settings Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
