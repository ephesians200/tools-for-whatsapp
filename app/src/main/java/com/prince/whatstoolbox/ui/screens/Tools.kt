package com.prince.whatstoolbox.ui.screens
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prince.whatstoolbox.domain.Tool
import com.prince.whatstoolbox.domain.VaultKind
import com.prince.whatstoolbox.presentation.*
import com.prince.whatstoolbox.ui.components.*
import com.prince.whatstoolbox.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ToolHost(tool: Tool, back: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text(tool.title) }, navigationIcon = { IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }) { p ->
        // imePadding: the keyboard must never cover the field being typed in (edge-to-edge doesn't resize the window).
        Column(Modifier.padding(p).imePadding().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tool.blurb.isNotBlank()) Text(tool.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (tool.id) {
                "DirectChat", "SmartNumberCleaner" -> DirectChat()
                "UsernameFinderBridge" -> UsernameBridge()
                "ChatAnalyticsWrapped" -> ChatAnalytics(pdf = false)
                "ChatToStyledPdf" -> ChatAnalytics(pdf = true)
                "StatusVideoSplitter" -> Splitter()
                "MarkdownStyler", "UnicodeFancyFonts", "WordCharacterCounter" -> TextStudio()
                "TrackingLinkCleaner" -> LinkCleaner()
                "UsernameVault" -> VaultPane(VaultKind.HANDLE)
                "CannedResponseVault" -> VaultPane(VaultKind.SNIPPET)
                "DraftVault" -> VaultPane(VaultKind.DRAFT)
                "MiniCrmNotes" -> VaultPane(VaultKind.CRM)
                else -> MoreTool(tool.id)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun DirectChat(vm: DirectChatViewModel = hiltViewModel()) {
    val c = LocalContext.current; val inbox = LocalInbox.current; val s by vm.state.collectAsStateWithLifecycle(); val item by inbox.item.collectAsStateWithLifecycle()
    // A number shared from another app / selected text arrives here pre-filled.
    LaunchedEffect(item?.nonce) { item?.takeIf { it.kind == ShareInbox.Kind.PHONE }?.let { vm.onIntent(DirectChatIntent.Number(it.text)); inbox.consume(it.nonce) } }
    // The system contact picker hands back one number with a temporary grant — no contacts permission needed.
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.data?.let { u -> runCatching { c.contentResolver.query(u, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use { cur ->
            if (cur.moveToFirst()) vm.onIntent(DirectChatIntent.Number(cur.getString(0).orEmpty())) } } } }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        OutlinedTextField(s.cc, { vm.onIntent(DirectChatIntent.Cc(it)) }, Modifier.width(104.dp), label = { Text("Code") }, prefix = { Text("+") }, singleLine = true, keyboardOptions = NumberKeyboard)
        OutlinedTextField(s.number, { vm.onIntent(DirectChatIntent.Number(it)) }, Modifier.weight(1f), label = { Text("Phone number (any format)") }, singleLine = true, keyboardOptions = PhoneKeyboard,
            trailingIcon = { IconButton({ pick.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) }) { Icon(Icons.Default.Contacts, "Pick from contacts") } }) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Only the clipboard *description* is read here (no toast); the text itself is read when the chip is tapped.
        if (Wa.clipboardHasText(c)) AssistChip({ vm.onIntent(DirectChatIntent.Number(Wa.readClipboard(c))) }, { Text("Paste from clipboard") })
        s.recents.forEach { r -> AssistChip({ vm.onIntent(DirectChatIntent.Number(r)) }, { Text("+$r") }) } }
    OutlinedTextField(s.message, { vm.onIntent(DirectChatIntent.Message(it)) }, Modifier.fillMaxWidth(), label = { Text("Message (optional)") })
    AnimatedVisibility(s.number.isNotBlank()) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(if (s.valid) Icons.Default.CheckCircle else Icons.Default.Error, null, tint = if (s.valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        Text(if (s.valid) "+${s.clean}" else "+${s.clean} — invalid length", color = if (s.valid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error) } }
    Text(s.link, style = MaterialTheme.typography.bodySmall)
    val apps = remember { Wa.installed(c) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({ vm.onIntent(DirectChatIntent.Remember(s.clean)); Wa.openChat(c, s.clean, s.message) }, enabled = s.valid) { Text("Open chat") }
        if ("com.whatsapp.w4b" in apps && "com.whatsapp" in apps) OutlinedButton({ vm.onIntent(DirectChatIntent.Remember(s.clean)); Wa.openChat(c, s.clean, s.message, "com.whatsapp.w4b") }, enabled = s.valid) { Text("WhatsApp Business") } }
    Actions(s.message.ifBlank { s.link }) }

@Composable fun UsernameBridge() { val c = LocalContext.current; val snack = LocalSnack.current; val scope = rememberCoroutineScope(); var h by remember { mutableStateOf("") }
    OutlinedTextField(h, { h = it }, Modifier.fillMaxWidth(), label = { Text("@username") }, singleLine = true,
        isError = h.isNotEmpty() && !Username.isValid(h), supportingText = { Text("3–30 chars: a-z, 0-9, dot, underscore") })
    Button({ val ok = Username.findInWhatsApp(c, h); scope.launch { snack.showSnackbar(if (ok) "Handle copied — tap Search in WhatsApp and paste" else "WhatsApp isn't installed") } }, enabled = Username.isValid(h)) { Text("Copy & open WhatsApp") }
    Text("WhatsApp has no public deep link for username search, so this copies the handle and opens the app. Then tap Search and paste.", style = MaterialTheme.typography.bodySmall) }

@OptIn(ExperimentalLayoutApi::class)
@Composable fun TextStudio(vm: TextStudioViewModel = hiltViewModel()) { val s by vm.state.collectAsStateWithLifecycle()
    val inbox = LocalInbox.current; val item by inbox.item.collectAsStateWithLifecycle()
    LaunchedEffect(item?.nonce) { item?.takeIf { it.kind == ShareInbox.Kind.TEXT }?.let { vm.onIntent(TextStudioIntent.Edit(it.text)); inbox.consume(it.nonce) } }
    OutlinedTextField(s.text, { vm.onIntent(TextStudioIntent.Edit(it)) }, Modifier.fillMaxWidth().heightIn(min = 120.dp), label = { Text("Your text") })
    Text("${s.chars} chars · ${s.words} words · ~${s.seconds}s read")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        com.prince.whatstoolbox.domain.usecase.MdOp.entries.forEach { op -> AssistChip({ vm.onIntent(TextStudioIntent.Apply(op)) }, { Text(op.label) }) } }
    if (s.text.isNotBlank()) { Actions(s.text); Text("Fancy fonts (${s.fonts.size})", style = MaterialTheme.typography.titleSmall)
        s.fonts.forEach { (name, out) -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(12.dp)) { Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text(out); Actions(out) } } } } }

@Composable fun LinkCleaner() { var u by remember { mutableStateOf("") }
    val inbox = LocalInbox.current; val item by inbox.item.collectAsStateWithLifecycle()
    LaunchedEffect(item?.nonce) { item?.takeIf { it.kind == ShareInbox.Kind.URL }?.let { u = it.text; inbox.consume(it.nonce) } }
    OutlinedTextField(u, { u = it }, Modifier.fillMaxWidth(), label = { Text("Paste URL") }, keyboardOptions = UriKeyboard)
    val out = remember(u) { TrackingCleaner.clean(u) }
    AnimatedVisibility(u.isNotBlank()) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (out != u.trim()) Text("Cleaned ✓ — tracking removed", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        else Text("No tracking parameters found", style = MaterialTheme.typography.labelLarge)
        Text(out); Actions(out) } } }
