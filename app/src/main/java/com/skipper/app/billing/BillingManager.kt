package com.skipper.app.billing

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.skipper.app.util.Logger
import com.skipper.app.util.PreferencesManager

class BillingManager(
    private val context: Context,
    private val prefs: PreferencesManager
) : PurchasesUpdatedListener {

    companion object {
        const val SUB_DAILY = "skipper_sub_daily"
        const val SUB_WEEKLY = "skipper_sub_weekly"
        const val SUB_MONTHLY = "skipper_sub_monthly"

        val ALL_SUBSCRIPTION_IDS = listOf(SUB_DAILY, SUB_WEEKLY, SUB_MONTHLY)
    }

    private val _productDetailsMap = MutableLiveData<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: LiveData<Map<String, ProductDetails>> = _productDetailsMap

    private val _isProSubscribed = MutableLiveData<Boolean>(prefs.isProActivated)
    val isProSubscribed: LiveData<Boolean> = _isProSubscribed

    private val _billingError = MutableLiveData<String?>()
    val billingError: LiveData<String?> = _billingError

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Logger.log("Billing", "Connected", "Google Play Billing client connected successfully.")
                    queryAvailableSubscriptions()
                    queryActivePurchases()
                } else {
                    Logger.log("Billing", "Setup Error", "Response: ${billingResult.debugMessage}")
                    _billingError.postValue("Google Play Billing not available: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Logger.log("Billing", "Disconnected", "Google Play service disconnected. Will retry on demand.")
            }
        })
    }

    fun queryAvailableSubscriptions() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }

        val productList = ALL_SUBSCRIPTION_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = productDetailsList.associateBy { it.productId }
                _productDetailsMap.postValue(map)
                Logger.log("Billing", "Products Loaded", "Loaded ${map.size} subscription tier(s) from Play Store.")
            } else {
                Logger.log("Billing", "Query Error", "Failed to load products: ${billingResult.debugMessage}")
            }
        }
    }

    fun queryActivePurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var hasActiveSub = false
                var activeTier = "NONE"

                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        hasActiveSub = true
                        activeTier = purchase.products.firstOrNull() ?: "PRO"

                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }

                if (hasActiveSub) {
                    prefs.isProActivated = true
                    prefs.subscriptionTier = activeTier
                } else {
                    // Only revoke PRO status if the current tier was an official Play Store subscription product.
                    // Never revoke if the user is in test mode, redeemed a promo/hardware key, or activated Test PRO!
                    val isPlayStoreTier = prefs.subscriptionTier == SUB_DAILY ||
                            prefs.subscriptionTier == SUB_WEEKLY ||
                            prefs.subscriptionTier == SUB_MONTHLY
                    if (isPlayStoreTier) {
                        prefs.isProActivated = false
                        prefs.subscriptionTier = "NONE"
                    }
                }
                _isProSubscribed.postValue(prefs.isProActivated || prefs.subscriptionTier.startsWith("PRO"))
            }
        }
    }

    fun launchSubscriptionFlow(activity: Activity, productId: String) {
        val details = _productDetailsMap.value?.get(productId)
        if (details == null) {
            _billingError.value = "Subscription details not loaded yet. Checking Google Play..."
            queryAvailableSubscriptions()
            return
        }

        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            _billingError.value = "No subscription offer available for $productId"
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, flowParams).responseCode
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            _billingError.value = "Failed to launch Google Play: response code $responseCode"
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Logger.log("Billing", "Cancelled", "User cancelled subscription flow.")
        } else {
            Logger.log("Billing", "Purchase Error", "Code: ${billingResult.responseCode}, ${billingResult.debugMessage}")
            _billingError.postValue("Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val tier = purchase.products.firstOrNull() ?: SUB_MONTHLY
            prefs.isProActivated = true
            prefs.subscriptionTier = tier
            _isProSubscribed.postValue(true)

            Logger.log("Billing", "Purchased!", "Successfully subscribed to $tier!")

            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Logger.log("Billing", "Acknowledged", "Purchase token acknowledged with Google Play.")
            }
        }
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
