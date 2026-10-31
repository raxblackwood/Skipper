package com.skipper.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.skipper.app.SkipperApp
import com.skipper.app.databinding.ActivityChatousDetailBinding

class ChatousDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatousDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatousDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as SkipperApp
        val prefs = app.preferencesManager

        binding.switchChatousEnabled.isChecked = prefs.isChatousEnabled
        binding.switchSkipMale.isChecked = prefs.isChatousSkipMale

        // Target Gender (Skip Male vs Skip Female)
        if (prefs.chatousGenderTarget.equals("FEMALE", ignoreCase = true)) {
            binding.rbSkipFemaleChatous.isChecked = true
        } else {
            binding.rbSkipMaleChatous.isChecked = true
        }

        binding.etActionDelay.setText(prefs.chatousClickDelayMs.toString())
        binding.etCooldownDelay.setText(prefs.chatousCooldownMs.toString())

        binding.btnBack.setOnClickListener {
            finish()
        }

        val saveAction = {
            val isEnabled = binding.switchChatousEnabled.isChecked
            val skipMale = binding.switchSkipMale.isChecked
            val targetGender = if (binding.rbSkipFemaleChatous.isChecked) "FEMALE" else "MALE"
            val actionDelay = binding.etActionDelay.text.toString().toLongOrNull() ?: 250L
            val cooldown = binding.etCooldownDelay.text.toString().toLongOrNull() ?: 1500L

            prefs.isChatousEnabled = isEnabled
            prefs.isChatousSkipMale = skipMale
            prefs.chatousGenderTarget = targetGender
            prefs.chatousClickDelayMs = actionDelay
            prefs.chatousCooldownMs = cooldown

            Toast.makeText(this, "Chatous Settings Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnTopSave.setOnClickListener { saveAction() }
        binding.btnSave.setOnClickListener { saveAction() }
    }
}
