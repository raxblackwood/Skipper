package com.skipper.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.skipper.app.SkipperApp
import com.skipper.app.databinding.ActivityChatousDetailBinding

class ChatousDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatousDetailBinding
    private val currentKeywords = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatousDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as SkipperApp
        val prefs = app.preferencesManager

        binding.switchChatousEnabled.isChecked = prefs.isChatousEnabled
        binding.switchSkipMale.isChecked = prefs.isChatousSkipMale

        // Target Gender
        if (prefs.chatousGenderTarget.equals("FEMALE", ignoreCase = true)) {
            binding.rbSkipFemaleChatous.isChecked = true
        } else {
            binding.rbSkipMaleChatous.isChecked = true
        }

        // Custom Keywords
        currentKeywords.addAll(prefs.chatousCustomKeywords)
        populateChips()

        binding.btnAddKeywordChatous.setOnClickListener {
            val kw = binding.etNewKeywordChatous.text?.toString()?.trim() ?: ""
            if (kw.isNotEmpty()) {
                if (!currentKeywords.contains(kw)) {
                    currentKeywords.add(kw)
                    addChip(kw)
                    binding.etNewKeywordChatous.setText("")
                } else {
                    Toast.makeText(this, "Keyword already exists", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.etActionDelay.setText(prefs.chatousClickDelayMs.toString())
        binding.etCooldownDelay.setText(prefs.chatousCooldownMs.toString())

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            val isEnabled = binding.switchChatousEnabled.isChecked
            val skipMale = binding.switchSkipMale.isChecked
            val targetGender = if (binding.rbSkipFemaleChatous.isChecked) "FEMALE" else "MALE"
            val actionDelay = binding.etActionDelay.text.toString().toLongOrNull() ?: 250L
            val cooldown = binding.etCooldownDelay.text.toString().toLongOrNull() ?: 1500L

            prefs.isChatousEnabled = isEnabled
            prefs.isChatousSkipMale = skipMale
            prefs.chatousGenderTarget = targetGender
            prefs.chatousCustomKeywords = currentKeywords.toSet()
            prefs.chatousClickDelayMs = actionDelay
            prefs.chatousCooldownMs = cooldown

            Toast.makeText(this, "Chatous Settings Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun populateChips() {
        binding.chipGroupChatousKeywords.removeAllViews()
        for (kw in currentKeywords) {
            addChip(kw)
        }
    }

    private fun addChip(keyword: String) {
        val chip = Chip(this).apply {
            text = keyword
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                binding.chipGroupChatousKeywords.removeView(this)
                currentKeywords.remove(keyword)
            }
        }
        binding.chipGroupChatousKeywords.addView(chip)
    }
}
