package com.skipper.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.skipper.app.R
import com.skipper.app.SkipperApp
import com.skipper.app.databinding.ActivityAnonChatDetailBinding

class AnonChatDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnonChatDetailBinding
    private val currentKeywords = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnonChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as SkipperApp
        val prefs = app.preferencesManager

        // Populate current values
        binding.switchAnonChatEnabled.isChecked = prefs.isAnonChatEnabled
        binding.switchAutoSkipMale.isChecked = prefs.isAnonChatAutoSkipMale

        // Target Gender
        if (prefs.anonChatGenderTarget.equals("FEMALE", ignoreCase = true)) {
            binding.rbSkipFemaleAnonChat.isChecked = true
        } else {
            binding.rbSkipMaleAnonChat.isChecked = true
        }

        // Custom Keywords Chips
        currentKeywords.addAll(prefs.anonChatCustomKeywords)
        populateChips()

        binding.btnAddKeyword.setOnClickListener {
            val kw = binding.etNewKeyword.text?.toString()?.trim() ?: ""
            if (kw.isNotEmpty()) {
                if (!currentKeywords.contains(kw)) {
                    currentKeywords.add(kw)
                    addChip(kw)
                    binding.etNewKeyword.setText("")
                } else {
                    Toast.makeText(this, "Keyword already exists", Toast.LENGTH_SHORT).show()
                }
            }
        }

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
            val targetGender = if (binding.rbSkipFemaleAnonChat.isChecked) "FEMALE" else "MALE"
            val isOnlyInstant = binding.rbInstantSkipOnly.isChecked
            val actionDelay = binding.etActionDelay.text.toString().toLongOrNull() ?: 350L
            val cooldown = binding.etCooldownDelay.text.toString().toLongOrNull() ?: 2000L

            prefs.isAnonChatEnabled = isEnabled
            prefs.isAnonChatAutoSkipMale = autoSkipMale
            prefs.anonChatGenderTarget = targetGender
            prefs.anonChatCustomKeywords = currentKeywords.toSet()
            prefs.isAnonChatOnlyInstantSkip = isOnlyInstant
            prefs.anonChatClickDelayMs = actionDelay
            prefs.anonChatCooldownMs = cooldown

            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun populateChips() {
        binding.chipGroupKeywords.removeAllViews()
        for (kw in currentKeywords) {
            addChip(kw)
        }
    }

    private fun addChip(keyword: String) {
        val chip = Chip(this).apply {
            text = keyword
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                binding.chipGroupKeywords.removeView(this)
                currentKeywords.remove(keyword)
            }
        }
        binding.chipGroupKeywords.addView(chip)
    }
}
