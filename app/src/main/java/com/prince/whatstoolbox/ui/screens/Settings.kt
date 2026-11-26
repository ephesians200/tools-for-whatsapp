package com.prince.whatstoolbox.ui.screens
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.prince.whatstoolbox.R
import com.prince.whatstoolbox.presentation.SettingsViewModel
import com.prince.whatstoolbox.ui.components.LocalPremium
import com.prince.whatstoolbox.ui.components.Chips
import com.prince.whatstoolbox.ui.components.NumberKeyboard

@Composable fun SettingsScreen(onUpgrade: () -> Unit = {}, vm: SettingsViewModel = hiltViewModel(), pvm: com.prince.whatstoolbox.presentation.PremiumViewModel = hiltViewModel()) {
    val premium = LocalPremium.current; val price by pvm.price.collectAsStateWithLifecycle(); val s by vm.state.collectAsStateWithLifecycle(); val c = LocalContext.current
    val version = remember { runCatching { c.packageManager.getPackageInfo(c.packageName, 0).versionName }.getOrNull().orEmpty() }
    var cc by remember(s.countryCode) { mutableStateOf(s.countryCode) }
    val dark = s.themeMode == 2 || (s.themeMode == 0 && androidx.compose.foundation.isSystemInDarkTheme())
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (premium) { Text("✓ Pro unlocked", style = MaterialTheme.typography.titleMedium); Text("No ads and every tool unlocked. Thank you for supporting the app!", style = MaterialTheme.typography.bodyMedium) }
            else { Text("Remove ads & unlock all tools", style = MaterialTheme.typography.titleMedium); Text("One payment of $price. No subscription.", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onUpgrade) { Text("Go Pro") }; OutlinedButton({ pvm.restore() }) { Text("Restore purchase") } } } } }
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Chips(listOf("System", "Light", "Dark"), s.themeMode) { vm.setThemeMode(it) }
        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Dynamic color"); Text(if (Build.VERSION.SDK_INT >= 31) "Use your wallpaper colours (Android 12+)" else "Requires Android 12+", style = MaterialTheme.typography.bodySmall) }
            Switch(s.dynamicColor && Build.VERSION.SDK_INT >= 31, vm::setDynamic, enabled = Build.VERSION.SDK_INT >= 31) }
        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("True AMOLED black"); Text(if (dark) "Pure black background" else "Applies in dark theme", style = MaterialTheme.typography.bodySmall) }
            Switch(s.amoled, vm::setAmoled) }
        Text("Phone numbers", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(cc, { cc = it.filter(Char::isDigit).take(4); if (cc.isNotBlank()) vm.setCountryCode(cc) }, Modifier.fillMaxWidth(), label = { Text("Default country code") }, prefix = { Text("+") },
            singleLine = true, keyboardOptions = NumberKeyboard, supportingText = { Text("Used when a number has no country code, in every tool.") })
        Text("About", style = MaterialTheme.typography.titleMedium)
        Text("Version $version", style = MaterialTheme.typography.bodySmall)
        Text("Your chats, photos and files are processed on your phone — they are never uploaded. Vault entries are encrypted with a key that never leaves this device (they are not included in Android backups).", style = MaterialTheme.typography.bodySmall)
        Text("Ads are shown by Google AdMob in the free version and use your device's advertising ID; Pro removes them.", style = MaterialTheme.typography.bodySmall)
        val info = remember { com.google.android.ump.UserMessagingPlatform.getConsentInformation(c) }
        if (!premium && info.privacyOptionsRequirementStatus == com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED)
            TextButton({ c.activity()?.let { a -> com.google.android.ump.UserMessagingPlatform.showPrivacyOptionsForm(a) { } } }) { Text("Ad privacy choices") }
        Text("Disclaimer: ${stringResource(R.string.app_name)} is an independent utility app and is not affiliated, associated, authorized, endorsed by, or in any way officially connected with WhatsApp LLC or Meta Platforms, Inc.", style = MaterialTheme.typography.bodySmall) } }
