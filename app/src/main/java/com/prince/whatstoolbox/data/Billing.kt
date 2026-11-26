package com.prince.whatstoolbox.data
import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.prince.whatstoolbox.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one-time "Pro" purchase (Google Play Billing, in-app product [BuildConfig.PREMIUM_PRODUCT_ID], priced $5.99 in Play Console).
 * - [premium] is cached on disk so Pro works offline; it is re-checked against Google Play on every launch/resume (restores after reinstall, honours refunds).
 * - [price] is the localised price Google returns; "$5.99" is only shown until that arrives.
 * - Purchases are not verified on a server — fine for a small utility app, add server-side verification if piracy becomes a problem.
 */
@Singleton class BillingManager @Inject constructor(@ApplicationContext private val ctx: Context) : PurchasesUpdatedListener {
    private val prefs = ctx.getSharedPreferences("premium_v1", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _premium = MutableStateFlow(prefs.getBoolean("premium", false)); val premium: StateFlow<Boolean> = _premium.asStateFlow()
    private val _ready = MutableStateFlow(false); /** true once the first purchase check finished (or failed) */ val ready: StateFlow<Boolean> = _ready.asStateFlow()
    private val _price = MutableStateFlow("$5.99"); val price: StateFlow<String> = _price.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4); val messages: SharedFlow<String> = _messages.asSharedFlow()
    private var details: ProductDetails? = null
    private val client: BillingClient = BillingClient.newBuilder(ctx).setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    val paywallSeen: Boolean get() = prefs.getBoolean("paywall_seen", false)
    fun markPaywallSeen() { prefs.edit().putBoolean("paywall_seen", true).apply() }

    /** Connects to Google Play and checks what the user owns. Safe to call repeatedly (launch, resume). */
    fun start() {
        if (client.isReady) { scope.launch { refresh() }; return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingClient.BillingResponseCode.OK) scope.launch { loadDetails(); refresh() } else _ready.value = true }
            override fun onBillingServiceDisconnected() { /* reconnects on the next start() */ }
        })
    }

    private suspend fun loadDetails() {
        val q = QueryProductDetailsParams.newBuilder().setProductList(listOf(QueryProductDetailsParams.Product.newBuilder()
            .setProductId(BuildConfig.PREMIUM_PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build())).build()
        details = client.queryProductDetails(q).productDetailsList?.firstOrNull()
        details?.oneTimePurchaseOfferDetails?.formattedPrice?.let { _price.value = it }
    }

    private suspend fun refresh() {
        val r = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
        if (r.billingResult.responseCode == BillingClient.BillingResponseCode.OK) handle(r.purchasesList, authoritative = true)
        _ready.value = true
    }

    private suspend fun handle(list: List<Purchase>?, authoritative: Boolean = false) {
        val mine = list.orEmpty().filter { BuildConfig.PREMIUM_PRODUCT_ID in it.products }
        val owned = mine.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (owned != null) {
            if (!owned.isAcknowledged) client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(owned.purchaseToken).build())   // unacknowledged purchases are refunded after 3 days
            if (!_premium.value) _messages.tryEmit("Thank you! All ads removed and every tool unlocked.")
            setPremium(true)
        } else {
            if (mine.any { it.purchaseState == Purchase.PurchaseState.PENDING }) _messages.tryEmit("Payment pending — Pro unlocks as soon as it completes.")
            else if (authoritative) setPremium(false)   // refunded / never bought
        }
    }
    private fun setPremium(v: Boolean) { prefs.edit().putBoolean("premium", v).apply(); _premium.value = v }

    fun purchase(activity: Activity) {
        val d = details
        if (d == null) { _messages.tryEmit("Google Play isn't available right now. Check your connection and try again."); start(); return }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build())).build()
        val r = client.launchBillingFlow(activity, params)
        when (r.responseCode) {
            BillingClient.BillingResponseCode.OK, BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> _messages.tryEmit("Purchases aren't available on this device or profile (Google Play may be blocked or restricted).")
            else -> _messages.tryEmit("Couldn't start the purchase (${r.responseCode}).")
        }
    }

    fun restore() { scope.launch { if (!client.isReady) { start(); _messages.tryEmit("Checking Google Play… tap Restore again in a moment if nothing changes."); return@launch }
        refresh(); if (!_premium.value) _messages.tryEmit("No previous purchase found on this Google account.") } }

    override fun onPurchasesUpdated(r: BillingResult, purchases: List<Purchase>?) {
        when (r.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch { handle(purchases) }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { refresh() }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> _messages.tryEmit("Purchases aren't available on this device or profile (Google Play may be blocked or restricted).")
            else -> _messages.tryEmit("Purchase failed (${r.responseCode}). You have not been charged.")
        }
    }
}
