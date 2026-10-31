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
    private val currentNeverSkipKeywords = mutableSetOf<String>()

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

        // Custom Skip Keywords Chips
        currentKeywords.addAll(prefs.anonChatCustomKeywords)
        populateChips()

        binding.btnAddKeyword.setOnClickListener {
            val kw = binding.etNewKeyword.text?.toString()?.trim() ?: ""
            if (kw.isNotEmpty()) {
                if (!currentKeywords.contains(kw)) {
                    currentKeywords.add(kw)
                    addChip(kw, isNeverSkip = false)
                    binding.etNewKeyword.setText("")
                } else {
                    Toast.makeText(this, "Keyword already exists", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Never-Skip (Protected Whitelist) Chips
        currentNeverSkipKeywords.addAll(prefs.anonChatNeverSkipKeywords)
        populateNeverSkipChips()

        binding.btnAddNeverSkipKeyword.setOnClickListener {
            val kw = binding.etNewNeverSkipKeyword.text?.toString()?.trim() ?: ""
            if (kw.isNotEmpty()) {
                if (!currentNeverSkipKeywords.contains(kw)) {
                    currentNeverSkipKeywords.add(kw)
                    addChip(kw, isNeverSkip = true)
                    binding.etNewNeverSkipKeyword.setText("")
                } else {
                    Toast.makeText(this, "Protected word already exists", Toast.LENGTH_SHORT).show()
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

        val saveAction = {
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
            prefs.anonChatNeverSkipKeywords = currentNeverSkipKeywords.toSet()
            prefs.isAnonChatOnlyInstantSkip = isOnlyInstant
            prefs.anonChatClickDelayMs = actionDelay
            prefs.anonChatCooldownMs = cooldown

            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnTopSave.setOnClickListener { saveAction() }
        binding.btnSave.setOnClickListener { saveAction() }
    }

    private fun populateChips() {
        binding.chipGroupKeywords.removeAllViews()
        for (kw in currentKeywords) {
            addChip(kw, isNeverSkip = false)
        }
    }

    private fun populateNeverSkipChips() {
        binding.chipGroupNeverSkipKeywords.removeAllViews()
        for (kw in currentNeverSkipKeywords) {
            addChip(kw, isNeverSkip = true)
        }
    }

    private fun addChip(keyword: String, isNeverSkip: Boolean) {
        val chip = Chip(this).apply {
            text = keyword
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                if (isNeverSkip) {
                    binding.chipGroupNeverSkipKeywords.removeView(this)
                    currentNeverSkipKeywords.remove(keyword)
                } else {
                    binding.chipGroupKeywords.removeView(this)
                    currentKeywords.remove(keyword)
                }
            }
        }
        if (isNeverSkip) {
            binding.chipGroupNeverSkipKeywords.addView(chip)
        } else {
            binding.chipGroupKeywords.addView(chip)
        }
    }
}
