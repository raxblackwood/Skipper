package com.skipper.app

import android.app.Application
import com.skipper.app.billing.BillingManager
import com.skipper.app.engine.AppAutomationEngine
import com.skipper.app.util.PreferencesManager

class SkipperApp : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var automationEngine: AppAutomationEngine
        private set

    lateinit var billingManager: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        automationEngine = AppAutomationEngine(preferencesManager)
        billingManager = BillingManager(this, preferencesManager)
        billingManager.startConnection()
    }
}
