package com.prince.whatstoolbox.ui.components
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prince.whatstoolbox.presentation.MediaState
import com.prince.whatstoolbox.presentation.ShareInbox
import com.prince.whatstoolbox.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ---------- app-wide values provided by AppRoot ----------
val LocalInbox = staticCompositionLocalOf<ShareInbox> { error("ShareInbox not provided") }
val LocalCc = staticCompositionLocalOf { "233" }
/** true once the one-time purchase is owned (no ads, nothing locked). */
/** Android 13+: without POST_NOTIFICATIONS the "job running" notification is hidden from the shade. Asked once, when a media-job screen opens. */
@Composable fun NotificationPermissionEffect() {
    val c = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { if (android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(c, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) ask.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
}
val LocalPremium = staticCompositionLocalOf { false }
val LocalSnack = staticCompositionLocalOf<SnackbarHostState> { error("SnackbarHostState not provided") }

// ---------- copy / share ----------
@Composable fun CopyButton(enabled: Boolean = true, onCopy: () -> Unit) {
    var copied by remember { mutableStateOf(false) }; val haptic = LocalHapticFeedback.current
    LaunchedEffect(copied) { if (copied) { delay(1500); copied = false } }
    OutlinedButton({ onCopy(); copied = true; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, enabled = enabled) {
        AnimatedContent(copied, transitionSpec = { (fadeIn(tween(150)) + scaleIn(initialScale = 0.8f)) togetherWith fadeOut(tween(100)) }, label = "copy") { done ->
            if (done) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Copied") }
            else Text("Copy") }
    }
}

/** [Copy] [Share to WhatsApp] (+ [Open Chat] when a number is known) for text results. [sensitive] hides the clipboard preview (keys, PINs). */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun Actions(text: String, number: String = "", sensitive: Boolean = false) { val c = LocalContext.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CopyButton { Wa.copy(c, text, sensitive) }
        Button({ Wa.shareText(c, text) }) { Text("Share to WhatsApp") }
        if (number.isNotBlank()) FilledTonalButton({ Wa.openChat(c, number, text) }) { Text("Open Chat") } } }

/**
 * Same actions for files. [make] builds the file(s) on a background thread only when a button is pressed, so big PDFs / images never block the UI.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun ShareBar(mime: String, text: String = "", showChat: Boolean = true, make: () -> List<File>) {
    val c = LocalContext.current; val cc = LocalCc.current; val scope = rememberCoroutineScope(); var num by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    fun run(then: (List<File>) -> Unit) { if (busy) return; busy = true
        scope.launch { val files = try { withContext(Dispatchers.IO) { make() } } catch (e: Exception) { emptyList() } finally { busy = false }; then(files) } }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CopyButton(enabled = !busy) { run { Out.copy(c, it, mime) } }
        Button({ run { Out.shareToWhatsApp(c, it, mime) } }, enabled = !busy) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text("Share to WhatsApp") } }
    if (showChat) { OutlinedTextField(num, { num = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Chat number (optional)") },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone))
        FilledTonalButton({ Wa.openChat(c, NumberCleaner.clean(num, cc), text) }, enabled = num.isNotBlank()) { Text("Open Chat") } } }

@Composable fun Output(text: String, shown: String? = null, number: String = "") {
    Card(Modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) { Text(shown ?: text, Modifier.padding(14.dp)) }
    Actions(text, number) }

// ---------- images ----------
/** Builds a bitmap off the main thread (debounced while keys change quickly, e.g. typing or dragging a slider). The previous bitmap stays visible meanwhile. */
@Composable fun rememberBitmapAsync(vararg keys: Any?, debounceMs: Long = 120, build: () -> Bitmap): State<Bitmap?> =
    produceState<Bitmap?>(initialValue = null, *keys) { delay(debounceMs); value = withContext(Dispatchers.Default) { build() } }

@Composable fun Preview(b: Bitmap?) {
    Crossfade(b, label = "preview") { x ->
        if (x == null) Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) { CircularProgressIndicator() }
        else Image(x.asImageBitmap(), "Preview", Modifier.fillMaxWidth().heightIn(max = 460.dp)) } }
@Composable fun pickImage(onPick: (Uri) -> Unit): () -> Unit {
    val l = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(onPick) }
    return { l.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }

// ---------- selection ----------
@Composable fun Swatches(colors: List<Int>, sel: Int, on: (Int) -> Unit) = Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    colors.forEachIndexed { i, c -> Box(Modifier.size(36.dp).clip(CircleShape).background(Color(c)).border(if (i == sel) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape).clickable { on(i) }) } }
@OptIn(ExperimentalLayoutApi::class)
@Composable fun Chips(names: List<String>, sel: Int, on: (Int) -> Unit) = FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    names.forEachIndexed { i, n -> FilterChip(sel == i, { on(i) }, { Text(n) }) } }

// ---------- job status ----------
@Composable fun MediaStatus(s: MediaState, onCancel: () -> Unit) {
    AnimatedVisibility(s.busy) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val p = s.progress
        if (p != null) { val a by animateFloatAsState(p, label = "progress"); LinearProgressIndicator(progress = { a }, modifier = Modifier.fillMaxWidth()) } else LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedButton(onCancel) { Text("Cancel") } } }
    if (s.status.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(s.done, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) {
            Icon(Icons.Default.CheckCircle, null, Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.primary) }
        Text(s.status) }
    if (s.warning.isNotBlank()) Text(s.warning, color = MaterialTheme.colorScheme.error) }

// ---------- misc ----------
@Composable fun EmptyState(icon: ImageVector, title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium); Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable fun SectionCard(title: String, rows: List<String>) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(14.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall); rows.ifEmpty { listOf("—") }.forEach { Text(it, style = MaterialTheme.typography.bodySmall) } } } }

// ---------- keyboards ----------
val PhoneKeyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
val NumberKeyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
val DecimalKeyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
val UriKeyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri)
