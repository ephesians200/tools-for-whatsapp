package com.prince.whatstoolbox
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.prince.whatstoolbox.presentation.SettingsViewModel
import com.prince.whatstoolbox.presentation.ShareInbox
import com.prince.whatstoolbox.ui.AppRoot
import com.prince.whatstoolbox.ui.components.VaultLock
import com.prince.whatstoolbox.ui.theme.ToolboxTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream
import javax.inject.Inject

// FragmentActivity (still a ComponentActivity) because BiometricPrompt needs it.
@AndroidEntryPoint class MainActivity : FragmentActivity() {
    @Inject lateinit var inbox: ShareInbox
    @Inject lateinit var billing: com.prince.whatstoolbox.data.BillingManager
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen(); super.onCreate(savedInstanceState)
        if (!billing.premium.value) com.prince.whatstoolbox.ui.components.Ads.consentAndStart(this)   // Pro users never load the ads SDK
        if (savedInstanceState == null) route(intent)   // don't re-deliver the same share after rotation
        setContent {
            val vm: SettingsViewModel = hiltViewModel(); val s by vm.state.collectAsStateWithLifecycle()
            val system = isSystemInDarkTheme(); val dark = when (s.themeMode) { 1 -> false; 2 -> true; else -> system }
            // Status/navigation-bar icon colours follow the *resolved* theme (e.g. forced dark/AMOLED on a light system).
            DisposableEffect(dark) {
                enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark })
                onDispose { }
            }
            ToolboxTheme(dark, s.amoled, s.dynamicColor) { AppRoot(inbox) }
        }
    }
    override fun onResume() { super.onResume(); billing.start() }   // picks up purchases completed elsewhere / refunds
    override fun onNewIntent(i: Intent) { super.onNewIntent(i); setIntent(i); route(i) }
    override fun onStop() { super.onStop(); if (!isChangingConfigurations) VaultLock.lock() }   // vault re-locks when the app leaves the screen

    private fun route(i: Intent?) {
        i ?: return
        when (i.action) {
            Intent.ACTION_SEND -> readShare(i)
            Intent.ACTION_PROCESS_TEXT -> i.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.let { inbox.post(it, ShareInbox.Kind.PHONE) }
            ACTION_OPEN_TOOL -> i.getStringExtra("tool")?.let { inbox.openTool(it) }
        }
    }
    @Suppress("DEPRECATION")
    private fun readShare(i: Intent) {
        val stream = if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (stream == null) { i.getStringExtra(Intent.EXTRA_TEXT)?.let { inbox.post(it) }; return }
        // File reads happen off the main thread (large exports used to risk an ANR).
        lifecycleScope.launch(Dispatchers.IO) {
            val text = runCatching { readStream(stream, i.type) }.getOrNull()
            if (!text.isNullOrBlank()) withContext(Dispatchers.Main) { inbox.post(text) }
        }
    }
    private fun readStream(u: Uri, mime: String?): String? {
        val zip = mime == "application/zip" || contentResolver.getType(u) == "application/zip" || u.lastPathSegment?.endsWith(".zip", true) == true
        return contentResolver.openInputStream(u)?.use { ins ->
            if (zip) ZipInputStream(ins).let { z -> generateSequence { z.nextEntry }.firstOrNull { it.name.endsWith(".txt", true) }?.let { z.bufferedReader().readText() } }
            else ins.bufferedReader().readText() }
    }
    companion object {
        const val ACTION_OPEN_TOOL = "com.prince.whatstoolbox.OPEN_TOOL"
        private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF); private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
