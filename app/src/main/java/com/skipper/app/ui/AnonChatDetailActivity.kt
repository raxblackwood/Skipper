package com.skipper.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.skipper.app.SkipperApp
import com.skipper.app.databinding.ActivityAnonChatDetailBinding

class AnonChatDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnonChatDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnonChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as SkipperApp
        val prefs = app.preferencesManager

        // Populate current values
        binding.switchAnonChatEnabled.isChecked = prefs.isAnonChatEnabled
        binding.switchAutoSkipMale.isChecked = prefs.isAnonChatAutoSkipMale

        if (prefs.isAnonChatOnlyInstantSkip) {
            binding.rbInstantSkipOnly.isChecked = true
        } else {
            binding.rbAllSkips.isChecked = true
        }

        binding.etActionDelay.setText(prefs.anonChatClickDelayMs.toString())
        binding.etCooldownDelay.setText(prefs.anonChatCooldownMs.toString())

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            val isEnabled = binding.switchAnonChatEnabled.isChecked
            val autoSkipMale = binding.switchAutoSkipMale.isChecked
            val isOnlyInstant = binding.rbInstantSkipOnly.isChecked
            val actionDelay = binding.etActionDelay.text.toString().toLongOrNull() ?: 350L
            val cooldown = binding.etCooldownDelay.text.toString().toLongOrNull() ?: 2000L

            prefs.isAnonChatEnabled = isEnabled
            prefs.isAnonChatAutoSkipMale = autoSkipMale
            prefs.isAnonChatOnlyInstantSkip = isOnlyInstant
            prefs.anonChatClickDelayMs = actionDelay
            prefs.anonChatCooldownMs = cooldown

            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
