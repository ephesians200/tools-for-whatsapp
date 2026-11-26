package com.prince.whatstoolbox.ui.screens
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prince.whatstoolbox.R
import com.prince.whatstoolbox.domain.ToolRegistry
import com.prince.whatstoolbox.presentation.PremiumViewModel
import com.prince.whatstoolbox.ui.components.LocalSnack
import com.prince.whatstoolbox.ui.icon
import com.prince.whatstoolbox.ui.theme.Amber

internal fun Context.activity(): Activity? { var c: Context? = this; while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }; return null }

/** Tools named on the paywall, in order of how much people want them. */
private val HEADLINERS = listOf("StatusVideoSplitter", "VoiceNoteTranscriber", "ChatToStyledPdf", "InstantPdfInvoiceMaker", "OfflineStickerMaker", "BatchChatLauncher")

/**
 * Full-screen paywall. Shown (1) once on a fresh install, (2) when a locked tool / Vault section is opened ([toolId] set), (3) from Settings.
 * Closes itself the moment the purchase goes through.
 */
@Composable fun PaywallScreen(toolId: String? = null, onClose: () -> Unit, vm: PremiumViewModel = hiltViewModel()) {
    val price by vm.price.collectAsStateWithLifecycle(); val premium by vm.premium.collectAsStateWithLifecycle(); val snack = LocalSnack.current
    val activity = LocalContext.current.activity()
    val locked = toolId?.let { ToolRegistry.byIdOrNull(it) }?.takeIf { it.premium }
    val proCount = remember { ToolRegistry.all.count { it.premium } }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }
    LaunchedEffect(premium) { if (premium) onClose() }
    val cs = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().background(cs.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 56.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(72.dp).background(cs.primaryContainer, CircleShape), Alignment.Center) { Icon(Icons.Default.WorkspacePremium, null, Modifier.size(40.dp), tint = Amber) }
            Text(if (locked != null) "${locked.title} is a Pro tool" else "Go Pro — pay once, keep it forever", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(if (locked != null) "${locked.blurb}. Unlock it and $proCount Pro tools in total with a single payment." else "The tools people use most, with no ads, for one small payment.",
                style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(cs.surface)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Benefit(Icons.Default.Block, "No more ads", "The banner and every video ad disappear — for good.")
                Benefit(Icons.Default.LockOpen, "$proCount Pro tools unlocked", "Video splitter & compressor, voice-note transcriber & MP3, chat to PDF, invoices, stickers, bulk messaging and more.")
                Benefit(Icons.Default.AllInclusive, "Every future tool included", "This app keeps growing toward 100+ tools. You pay once and get all of them.")
                Benefit(Icons.Default.Payments, "One payment. No subscription.", "$price once. Never charged again, never expires, works on all your devices with the same Google account.")
                Benefit(Icons.Default.PhoneAndroid, "Still private", "Your chats, photos and vault never leave your phone.") } }

            Text("What you unlock", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(cs.surface)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HEADLINERS.mapNotNull { ToolRegistry.byIdOrNull(it) }.forEach { t -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(t.icon(), null, tint = cs.primary); Column { Text(t.title, style = MaterialTheme.typography.titleSmall); Text(t.blurb, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant) } } }
                Text("+ ${proCount - HEADLINERS.size} more", style = MaterialTheme.typography.labelLarge, color = cs.primary) } }

            Spacer(Modifier.height(4.dp))
            Button({ activity?.let(vm::buy) }, Modifier.fillMaxWidth().height(54.dp), enabled = activity != null) { Text("Unlock everything — $price", style = MaterialTheme.typography.titleMedium) }
            TextButton({ vm.restore() }) { Text("Restore purchase") }
            TextButton(onClose) { Text(if (locked != null) "Not now" else "Maybe later — continue with ads and free tools") }
            Text("One-time purchase through Google Play. The ${ToolRegistry.all.size - proCount} free tools stay free.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        IconButton(onClose, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) { Icon(Icons.Default.Close, "Close") }
    }
}

@Composable private fun Benefit(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.padding(top = 2.dp), tint = MaterialTheme.colorScheme.primary)
        Column { Text(title, style = MaterialTheme.typography.titleSmall); Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

/** Small in-place upsell used where a locked feature would otherwise appear (e.g. a locked Vault tab). */
@Composable fun LockedFeature(title: String, onUnlock: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Lock, null, Modifier.size(36.dp), tint = Amber)
        Text("$title is a Pro feature", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text("Unlock it, remove all ads and get every Pro tool with one payment.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onUnlock) { Text("See Pro") } } } }
