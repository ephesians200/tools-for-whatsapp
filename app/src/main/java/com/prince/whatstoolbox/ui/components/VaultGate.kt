package com.prince.whatstoolbox.ui.components
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** In-memory unlock state. Re-locks whenever the app leaves the foreground (see MainActivity.onStop). */
object VaultLock { var unlocked by mutableStateOf(false); fun lock() { unlocked = false } }

private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
private fun Context.findActivity(): FragmentActivity? { var c: Context? = this; while (c is ContextWrapper) { if (c is FragmentActivity) return c; c = c.baseContext }; return null }
/** Returns a message when authentication can't run (no screen lock); otherwise shows the system prompt. */
private fun authenticate(a: FragmentActivity, onOk: () -> Unit, onMsg: (String) -> Unit) {
    if (BiometricManager.from(a).canAuthenticate(AUTH) != BiometricManager.BIOMETRIC_SUCCESS) { onMsg("NO_LOCK"); return }
    val prompt = BiometricPrompt(a, ContextCompat.getMainExecutor(a), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onOk() }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onMsg(errString.toString()) }
    })
    prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock vault").setSubtitle("Your replies, notes and handles are protected")
        .setAllowedAuthenticators(AUTH).build())
}

/** Shows [content] only after biometric / screen-lock authentication. While unlocked the window is FLAG_SECURE (no screenshots, blank recents thumbnail). */
@Composable fun VaultGate(content: @Composable () -> Unit) {
    val activity = LocalContext.current.findActivity()
    if (VaultLock.unlocked) {
        DisposableEffect(Unit) { activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE); onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) } }
        content()
    } else {
        var msg by remember { mutableStateOf<String?>(null) }
        val start = { if (activity == null) VaultLock.unlocked = true else authenticate(activity, { VaultLock.unlocked = true }, { msg = it }) }
        LaunchedEffect(Unit) { start() }
        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Lock, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Vault locked", style = MaterialTheme.typography.titleLarge)
            if (msg == "NO_LOCK") {
                Text("This phone has no screen lock, so the vault can't be protected. Set a PIN, pattern or fingerprint in Android settings.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton({ VaultLock.unlocked = true }) { Text("Open anyway") }
            } else {
                msg?.let { Text(it, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button({ msg = null; start() }) { Text("Unlock") }
            }
        }
    }
}
