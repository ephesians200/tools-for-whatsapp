package com.prince.whatstoolbox.ui
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.prince.whatstoolbox.domain.ToolRegistry
import com.prince.whatstoolbox.presentation.SettingsViewModel
import com.prince.whatstoolbox.presentation.ShareInbox
import com.prince.whatstoolbox.presentation.PremiumViewModel
import com.prince.whatstoolbox.ui.components.*
import com.prince.whatstoolbox.ui.components.LocalCc
import com.prince.whatstoolbox.ui.components.LocalInbox
import com.prince.whatstoolbox.ui.components.LocalSnack
import com.prince.whatstoolbox.ui.screens.*
import kotlinx.serialization.Serializable

@Serializable object Home
@Serializable data class ToolRoute(val id: String)
@Serializable object VaultRoute
@Serializable object SettingsRoute
/** [toolId] = the locked tool that triggered the paywall (null when shown on first launch / from Settings). */
@Serializable data class PaywallRoute(val toolId: String? = null)

/** Tab switch that keeps each tab's state and never stacks duplicates. */
private fun NavHostController.goTab(route: Any) = navigate(route) { popUpTo<Home> { saveState = true }; launchSingleTop = true; restoreState = true }

@OptIn(ExperimentalLayoutApi::class)
@Composable fun AppRoot(inbox: ShareInbox) {
    val nav = rememberNavController()
    val settings by hiltViewModel<SettingsViewModel>().state.collectAsStateWithLifecycle()
    val item by inbox.item.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    val dest = nav.currentBackStackEntryAsState().value?.destination
    val pvm = hiltViewModel<PremiumViewModel>(); val premium by pvm.premium.collectAsStateWithLifecycle()
    // Locked tools go to the paywall instead of the tool; Pro users never see it.
    val openTool: (String) -> Unit = { id -> nav.navigate(if (ToolRegistry.isLocked(id, premium)) PaywallRoute(id) else ToolRoute(id)) { launchSingleTop = true } }

    // Fresh install: show the paywall once. Waits (max 3 s) for Google Play's first purchase check so someone who already paid and reinstalled isn't asked again.
    LaunchedEffect(Unit) {
        if (pvm.paywallSeen) return@LaunchedEffect
        kotlinx.coroutines.withTimeoutOrNull(3000) { pvm.ready.first { it } }
        if (!pvm.premium.value) { pvm.markPaywallSeen(); nav.navigate(PaywallRoute()) }
    }
    // Pro: free the ad memory.
    LaunchedEffect(premium) { if (premium) NativeAdCache.clear() }

    // Anything that arrives from outside (share sheet, text selection, shortcut, widget) is routed to the right tool.
    LaunchedEffect(item?.nonce) {
        val it = item ?: return@LaunchedEffect
        val target = when (it.kind) { ShareInbox.Kind.CHAT -> "ChatAnalyticsWrapped"; ShareInbox.Kind.PHONE -> "DirectChat"; ShareInbox.Kind.URL -> "TrackingLinkCleaner"
            ShareInbox.Kind.TEXT -> "MarkdownStyler"; ShareInbox.Kind.OPEN -> it.text }
        if (ToolRegistry.byIdOrNull(target) != null) openTool(target)
        if (it.kind == ShareInbox.Kind.OPEN || it.kind == ShareInbox.Kind.CHAT) inbox.consume(it.nonce)   // other kinds are consumed by the tool that uses them
    }

    CompositionLocalProvider(LocalInbox provides inbox, LocalCc provides settings.countryCode, LocalSnack provides snack, LocalPremium provides premium) {
        val onTool = dest?.hasRoute<ToolRoute>() == true; val onPaywall = dest?.hasRoute<PaywallRoute>() == true
        val keyboard = WindowInsets.isImeVisible
        Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snack) }, bottomBar = {
            Column {
                // Persistent banner directly above the bottom bar (on tool screens it sits at the bottom, and steps aside while the keyboard is open).
                if (!premium && !onPaywall && !(onTool && keyboard)) Box(if (onTool) Modifier.navigationBarsPadding() else Modifier) { BannerAd() }
                if (!onTool && !onPaywall) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(dest?.hasRoute<Home>() == true, { nav.goTab(Home) }, { Icon(Icons.Default.Home, null) }, label = { Text("Tools") })
                    NavigationBarItem(dest?.hasRoute<VaultRoute>() == true, { nav.goTab(VaultRoute) }, { Icon(Icons.Default.Lock, null) }, label = { Text("Vault") })
                    NavigationBarItem(dest?.hasRoute<SettingsRoute>() == true, { nav.goTab(SettingsRoute) }, { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
                } } }) { pad ->
            // consumeWindowInsets: the screens' own Scaffolds must not add the system-bar padding a second time.
            NavHost(nav, Home, Modifier.padding(pad).consumeWindowInsets(pad),
                enterTransition = { if (targetState.destination.hasRoute<ToolRoute>()) slideInHorizontally(tween(300)) { it / 8 } + fadeIn(tween(300)) else fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(300)) },
                popExitTransition = { if (initialState.destination.hasRoute<ToolRoute>()) slideOutHorizontally(tween(250)) { it / 8 } + fadeOut(tween(200)) else fadeOut(tween(150)) }) {
                composable<Home> { Dashboard(open = openTool) }
                composable<ToolRoute> { e -> val t = ToolRegistry.byIdOrNull(e.toRoute<ToolRoute>().id) ?: ToolRegistry.byId("DirectChat")
                    if (ToolRegistry.isLocked(t.id, premium)) PaywallScreen(t.id, onClose = { nav.popBackStack() }) else ToolHost(t) { nav.popBackStack() } }   // belt and braces: a locked tool can never render
                composable<VaultRoute> { VaultScreen(onUnlock = { nav.navigate(PaywallRoute(it)) }) }
                composable<SettingsRoute> { SettingsScreen(onUpgrade = { nav.navigate(PaywallRoute()) }) }
                composable<PaywallRoute> { e -> PaywallScreen(e.toRoute<PaywallRoute>().toolId, onClose = { nav.popBackStack() }) }
            }
        }
    }
}
