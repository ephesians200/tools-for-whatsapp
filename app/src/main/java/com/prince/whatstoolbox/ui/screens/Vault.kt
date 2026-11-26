package com.prince.whatstoolbox.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import com.prince.whatstoolbox.domain.ToolRegistry
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prince.whatstoolbox.domain.VaultKind
import com.prince.whatstoolbox.presentation.*
import com.prince.whatstoolbox.ui.components.*
import com.prince.whatstoolbox.util.NumberCleaner
import com.prince.whatstoolbox.util.Wa
import kotlinx.coroutines.launch

/** One encrypted vault section (replies / drafts / CRM notes / handles). Locked behind biometrics / screen lock; used by the Vault tab and the matching tool screens. */
@Composable fun VaultPane(kind: VaultKind, vm: VaultViewModel = hiltViewModel()) = VaultGate { VaultContent(kind, vm) }

@Composable private fun VaultContent(kind: VaultKind, vm: VaultViewModel) {
    val c = LocalContext.current; val cc = LocalCc.current; val snack = LocalSnack.current; val scope = rememberCoroutineScope()
    LaunchedEffect(kind) { vm.onIntent(VaultIntent.Select(kind)) }
    val s by vm.state.collectAsStateWithLifecycle()
    var editId by remember(kind) { mutableLongStateOf(0L) }; var editCreated by remember(kind) { mutableLongStateOf(0L) }
    var t by remember(kind) { mutableStateOf("") }; var b by remember(kind) { mutableStateOf("") }; var e by remember(kind) { mutableStateOf("") }; var q by remember(kind) { mutableStateOf("") }
    fun clear() { editId = 0; editCreated = 0; t = ""; b = ""; e = "" }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(t, { t = it }, Modifier.fillMaxWidth(), label = { Text(kind.titleLabel) })
        OutlinedTextField(b, { b = it }, Modifier.fillMaxWidth(), label = { Text(kind.bodyLabel) }, minLines = 2)
        kind.extraLabel?.let { l -> OutlinedTextField(e, { e = it }, Modifier.fillMaxWidth(), label = { Text(l) }) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // createdAt is kept when editing, so an edited entry doesn't jump to the top of the list.
            Button({ vm.onIntent(VaultIntent.Save(editId, t, b, e, editCreated)); clear() }, enabled = t.isNotBlank() || b.isNotBlank()) { Text(if (editId == 0L) "Save" else "Update") }
            if (editId != 0L) TextButton({ clear() }) { Text("Cancel") } }
        Text("🔒 Stored encrypted on this device", style = MaterialTheme.typography.bodySmall)
        if (s.items.size > 3) OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), label = { Text("Search") }, singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) })
        val shown = s.items.filter { it.kind == kind.key && (q.isBlank() || it.title.contains(q, true) || it.body.contains(q, true)) }   // search runs on already-decrypted rows, in memory
        if (shown.isEmpty()) Text(if (s.items.isEmpty()) "Nothing saved yet." else "No matches.", style = MaterialTheme.typography.bodyMedium)
        shown.forEach { i -> key(i.id) { var reveal by remember(i.id) { mutableStateOf(false) }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(14.dp)) {
                Text(i.title, fontWeight = FontWeight.Bold); Text(i.body)
                if (i.extra.isNotBlank()) { if (kind == VaultKind.HANDLE) { Text(if (reveal) "Key: ${i.extra}" else "Key: ••••")
                    Row { TextButton({ reveal = !reveal }) { Text(if (reveal) "Hide key" else "Show key") }; TextButton({ Wa.copy(c, i.extra, sensitive = true) }) { Text("Copy key") } } } else Text(i.extra) }
                Actions(i.body.ifBlank { i.title }, if (kind == VaultKind.CRM && i.extra.count { it.isDigit() } >= 8) NumberCleaner.clean(i.extra, cc) else "")
                Row { TextButton({ editId = i.id; editCreated = i.createdAt; t = i.title; b = i.body; e = i.extra }) { Text("Edit") }
                    TextButton({ vm.onIntent(VaultIntent.Delete(i.id))
                        scope.launch { if (snack.showSnackbar("Deleted “${i.title.ifBlank { "entry" }}”", "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed)
                            vm.onIntent(VaultIntent.Save(i.id, i.title, i.body, i.extra, i.createdAt)) } }) { Text("Delete") } } } } } }
    }
}
/** Which Pro tool each Vault section belongs to — the Vault tab must not be a back door around the lock. */
private fun VaultKind.toolId() = when (this) { VaultKind.SNIPPET -> "CannedResponseVault"; VaultKind.DRAFT -> "DraftVault"; VaultKind.CRM -> "MiniCrmNotes"; VaultKind.HANDLE -> "UsernameVault" }

@Composable fun VaultScreen(onUnlock: (String) -> Unit = {}) { var tab by remember { mutableIntStateOf(0) }; val kinds = VaultKind.entries; val premium = LocalPremium.current
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Vault", style = MaterialTheme.typography.headlineMedium)
        ScrollableTabRow(tab, containerColor = MaterialTheme.colorScheme.background, edgePadding = 0.dp) { kinds.forEachIndexed { i, k ->
            Tab(tab == i, { tab = i }, text = { Row(verticalAlignment = Alignment.CenterVertically) { Text(k.label)
                if (ToolRegistry.isLocked(k.toolId(), premium)) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Lock, "Pro", Modifier.size(14.dp), tint = com.prince.whatstoolbox.ui.theme.Amber) } } }) } }
        val k = kinds[tab]
        if (ToolRegistry.isLocked(k.toolId(), premium)) LockedFeature(ToolRegistry.byId(k.toolId()).title) { onUnlock(k.toolId()) } else VaultPane(k) } }
