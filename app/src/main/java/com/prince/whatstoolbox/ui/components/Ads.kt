package com.prince.whatstoolbox.ui.components
import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.*
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.prince.whatstoolbox.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Starts AdMob only after the consent step (UMP form where the law requires it). Nothing here runs for Pro users. */
object Ads {
    private val _ready = MutableStateFlow(false); val ready = _ready.asStateFlow()
    private var started = false
    fun consentAndStart(a: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(a)
        info.requestConsentInfoUpdate(a, ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(a) { _ -> if (info.canRequestAds()) start(a) } },
            { _ -> if (info.canRequestAds()) start(a) })
        if (info.canRequestAds()) start(a)   // consent already given in an earlier session
    }
    private fun start(c: Context) { if (started) return; started = true; Thread { MobileAds.initialize(c.applicationContext) { _ready.value = true } }.start() }
}

/** Persistent adaptive banner shown above the bottom bar. Space is reserved up-front so the layout doesn't jump when the ad arrives. */
@Composable fun BannerAd(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current; val width = LocalConfiguration.current.screenWidthDp
    val size = remember(width) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, width) }
    val ready by Ads.ready.collectAsStateWithLifecycle()
    Box(modifier.fillMaxWidth().height(size.height.dp).background(MaterialTheme.colorScheme.surface)) {
        if (ready) AndroidView(modifier = Modifier.fillMaxSize(), onRelease = { (it as AdView).destroy() },
            factory = { c -> AdView(c).apply { setAdSize(size); adUnitId = BuildConfig.ADMOB_BANNER_ID; loadAd(AdRequest.Builder().build()) } })
    }
}

/** Keeps one native ad per grid slot for the session (re-requested after 50 min; native ads expire after an hour) so scrolling never re-requests. */
object NativeAdCache {
    val ads = mutableStateMapOf<String, NativeAd>()
    private val loadedAt = HashMap<String, Long>(); private val failedAt = HashMap<String, Long>(); private val loading = HashSet<String>()
    fun request(c: Context, slot: String) {
        val now = SystemClock.elapsedRealtime()
        if (slot in loading) return
        if (ads[slot] != null && now - (loadedAt[slot] ?: 0L) < 50 * 60_000L) return
        if (now - (failedAt[slot] ?: -120_000L) < 60_000L) return   // no-fill: wait a minute before asking again
        loading += slot
        AdLoader.Builder(c.applicationContext, BuildConfig.ADMOB_NATIVE_VIDEO_ID)
            .forNativeAd { ad -> loading -= slot; ads.put(slot, ad)?.destroy(); loadedAt[slot] = SystemClock.elapsedRealtime() }
            .withAdListener(object : AdListener() { override fun onAdFailedToLoad(e: LoadAdError) { loading -= slot; failedAt[slot] = SystemClock.elapsedRealtime() } })
            .withNativeAdOptions(NativeAdOptions.Builder().setVideoOptions(VideoOptions.Builder().setStartMuted(true).build()).build())
            .build().loadAd(AdRequest.Builder().build())
    }
    fun clear() { ads.values.forEach { it.destroy() }; ads.clear() }
}

/**
 * Video ad shaped exactly like a ToolCard (full grid cell wide, 150 dp tall): 120 dp video + a 30 dp strip with the "Ad" label and headline.
 * 120 dp is AdMob's minimum size for a native video. Renders nothing until an ad has loaded (e.g. offline / no fill).
 */
@Composable fun NativeAdTile(slot: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current; val ready by Ads.ready.collectAsStateWithLifecycle()
    LaunchedEffect(ready, slot) { if (ready) NativeAdCache.request(ctx, slot) }
    val ad = NativeAdCache.ads[slot] ?: return
    val cs = MaterialTheme.colorScheme
    val text = cs.onSurface.toArgb(); val badgeBg = cs.primaryContainer.toArgb(); val badgeFg = cs.onPrimaryContainer.toArgb()
    Card(modifier.fillMaxWidth().height(150.dp), colors = CardDefaults.cardColors(cs.surface)) {
        AndroidView(
            factory = { c -> buildNativeView(c, text, badgeBg, badgeFg) },
            modifier = Modifier.fillMaxSize(),
            update = { v -> bindNative(v, ad) }
        )
    }
}

private fun Context.px(dp: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
private const val TAG_MEDIA = 1; private const val TAG_HEAD = 2

private fun buildNativeView(c: Context, textColor: Int, badgeBg: Int, badgeFg: Int): NativeAdView {
    val media = MediaView(c).apply { tag = TAG_MEDIA; setImageScaleType(ImageView.ScaleType.CENTER_CROP); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, c.px(120)) }
    val badge = TextView(c).apply { text = "Ad"; textSize = 10f; setTextColor(badgeFg); setBackgroundColor(badgeBg); setPadding(c.px(5), c.px(1), c.px(5), c.px(1))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = c.px(8) } }
    val head = TextView(c).apply { tag = TAG_HEAD; textSize = 12f; setTextColor(textColor); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
    val strip = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(c.px(10), 0, c.px(10), 0)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, c.px(30)); addView(badge); addView(head) }
    val col = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL; layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT); addView(media); addView(strip) }
    return NativeAdView(c).apply { addView(col); mediaView = media; headlineView = head }
}
private fun bindNative(v: NativeAdView, ad: NativeAd) {
    (v.headlineView as TextView).text = ad.headline
    v.setNativeAd(ad)
}
