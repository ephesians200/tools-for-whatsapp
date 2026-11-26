package com.prince.whatstoolbox.ui.screens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.prince.whatstoolbox.domain.usecase.ChatReport
import com.prince.whatstoolbox.presentation.*
import com.prince.whatstoolbox.ui.components.*
import com.prince.whatstoolbox.util.*

/**
 * Source of an exported chat: the chat shared to the app (kept until "Use another chat") or text pasted by hand.
 * A shared chat is no longer stuck forever — there's an explicit way back to the paste box.
 */
@Composable fun ChatInput(vm: ChatViewModel = hiltViewModel(), content: @Composable (ChatReport) -> Unit) {
    val inbox = LocalInbox.current; val shared by inbox.chat.collectAsStateWithLifecycle()
    var pasted by remember { mutableStateOf("") }   // not rememberSaveable: big chats would overflow the saved-state Bundle
    val raw = shared ?: pasted
    LaunchedEffect(raw) { vm.load(raw) }
    if (shared != null) AssistChip({ inbox.clearChat() }, { Text("Using the chat you shared · tap to use another") })
    else OutlinedTextField(pasted, { pasted = it }, Modifier.fillMaxWidth(), label = { Text("Paste exported chat text (or share it to this app)") }, minLines = 4)
    val r by vm.report.collectAsStateWithLifecycle()
    if (r.msgs.isNotEmpty()) content(r) else if (shared != null) Text("Could not read any messages from the shared file.") }

@Composable fun ListTool(items: List<Pair<String, String>>) { var q by remember { mutableStateOf("") }
    OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), label = { Text("Search") }, singleLine = true)
    items.filter { q.isBlank() || it.first.contains(q, true) || it.second.contains(q, true) }.forEach { (cat, txt) ->
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(14.dp)) {
            Text(cat, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text(txt); Actions(txt) } } } }

// ---------- chat extractors ----------
@Composable fun OrderExtractor() { val c = LocalContext.current
    ChatInput { r -> if (r.ordersCsv.lines().count { it.isNotBlank() } <= 1) Text("No items with prices found in this chat.") else Output(r.ordersCsv)
        ShareBar("text/csv") { listOf(Out.writeText(c, "orders.csv", r.ordersCsv)) } } }
@Composable fun LinkExtractor() { ChatInput { r -> var q by remember { mutableStateOf("") }
    OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), label = { Text("Filter") })
    Output((r.links.map { "🔗 $it" } + r.emails.map { "✉️ $it" } + r.phones.map { "📞 $it" }).filter { it.contains(q, true) }.joinToString("\n").ifBlank { "Nothing found" }) } }
@Composable fun Participation() { ChatInput { r -> r.stats?.let { s -> val t = s.total.coerceAtLeast(1)
    Output("👥 Participation\n" + s.bySender.joinToString("\n") { "${it.first}: ${it.second} (${it.second * 100 / t}%)" } + "\n\n🔇 Silent (<3 msgs): " + r.silent.joinToString().ifBlank { "none" }) } } }

// ---------- username / chat tools ----------
@Composable fun ConnectCard() { val c = LocalContext.current; var u by remember { mutableStateOf("") }; var k by remember { mutableStateOf("") }; var n by remember { mutableStateOf("") }
    OutlinedTextField(n, { n = it }, Modifier.fillMaxWidth(), label = { Text("Display name / tagline") })
    OutlinedTextField(u, { u = it }, Modifier.fillMaxWidth(), label = { Text("@username") }, isError = u.isNotEmpty() && !Username.isValid(u))
    OutlinedTextField(k, { k = it.filter(Char::isDigit).take(4) }, label = { Text("4-digit key (optional)") }, keyboardOptions = NumberKeyboard, singleLine = true)
    if (Username.isValid(u)) { val b by rememberBitmapAsync(u, k, n) { Img.connectCard(Username.normalize(u), k, n) }; Preview(b)
        ShareBar("image/png", text = "@${Username.normalize(u)}") { listOfNotNull(b?.let { Out.saveBitmap(c, it, "connect_card.png") }) } } }
@Composable fun VCard() { val c = LocalContext.current; var n by remember { mutableStateOf("") }; var u by remember { mutableStateOf("") }
    OutlinedTextField(n, { n = it }, Modifier.fillMaxWidth(), label = { Text("Display name") }); OutlinedTextField(u, { u = it }, Modifier.fillMaxWidth(), label = { Text("@username") })
    val vcf = "BEGIN:VCARD\nVERSION:3.0\nFN:$n\nNOTE:WhatsApp username @${Username.normalize(u)}\nEND:VCARD"
    if (n.isNotBlank() && Username.isValid(u)) { Output(vcf); ShareBar("text/x-vcard", showChat = false) { listOf(Out.writeText(c, "${n.replace(' ', '_')}.vcf", vcf)) } } }
@Composable fun Batch() { val c = LocalContext.current; val cc = LocalCc.current; var nums by remember { mutableStateOf("") }; var msg by remember { mutableStateOf("") }; var i by remember { mutableIntStateOf(0) }
    OutlinedTextField(nums, { nums = it; i = 0 }, Modifier.fillMaxWidth(), label = { Text("Numbers (one per line)") }, minLines = 4, keyboardOptions = PhoneKeyboard)
    OutlinedTextField(msg, { msg = it }, Modifier.fillMaxWidth(), label = { Text("Message (optional)") })
    val list = nums.split(Regex("[\n,;]")).filter { it.isNotBlank() }.map { NumberCleaner.clean(it, cc) }.filter { NumberCleaner.isValid(it) }
    val progress by animateProgress(if (list.isEmpty()) 0f else i.toFloat() / list.size)
    Text("Queue: ${list.size} valid · ${if (i < list.size) "next #${i + 1}: +${list[i]}" else "all done"}")
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button({ Wa.openChat(c, list[i], msg); i++ }, enabled = i < list.size) { Text(if (i == 0) "Start" else "Open next") }; TextButton({ i = 0 }) { Text("Restart") } } }
@Composable private fun animateProgress(target: Float) = androidx.compose.animation.core.animateFloatAsState(target, label = "queue")
@Composable fun LinkQr() { val c = LocalContext.current; val cc = LocalCc.current; var n by remember { mutableStateOf("") }; var m by remember { mutableStateOf("") }
    OutlinedTextField(n, { n = it }, Modifier.fillMaxWidth(), label = { Text("Phone number") }, keyboardOptions = PhoneKeyboard); OutlinedTextField(m, { m = it }, Modifier.fillMaxWidth(), label = { Text("Pre-filled message") })
    val num = NumberCleaner.clean(n, cc)
    if (n.isNotBlank() && NumberCleaner.isValid(num)) { val link = Wa.link(num, m).toString(); Output(link, number = num)
        val b = remember(link) { Img.qr(link) }; Preview(b); ShareBar("image/png", showChat = false) { listOf(Out.saveBitmap(c, b, "wa_qr.png")) } } }
@Composable fun QrScan() { val c = LocalContext.current; val cc = LocalCc.current; var res by remember { mutableStateOf("") }
    Button({ GmsBarcodeScanning.getClient(c).startScan().addOnSuccessListener { res = it.rawValue.orEmpty() }.addOnFailureListener { res = "Scan failed: ${it.message}" } }) { Text("Scan QR (no camera permission needed)") }
    if (res.isNotBlank()) { Text(res)
        val n = Regex("""wa\.me/(\d+)""").find(res)?.groupValues?.get(1) ?: Regex("""phone=(\d+)""").find(res)?.groupValues?.get(1) ?: Regex("""TEL[^:]*:([+\d\s\-]+)""").find(res)?.groupValues?.get(1)?.let { NumberCleaner.clean(it, cc) }
        if (n != null) Button({ Wa.openChat(c, n) }) { Text("Open chat with +$n") } } }
@Composable fun SelfNote(vm: SelfNoteViewModel = hiltViewModel()) { val c = LocalContext.current; val cc = LocalCc.current; var n by remember { mutableStateOf(vm.saved) }; var m by remember { mutableStateOf("") }
    OutlinedTextField(n, { n = it }, Modifier.fillMaxWidth(), label = { Text("Your own number") }, keyboardOptions = PhoneKeyboard); OutlinedTextField(m, { m = it }, Modifier.fillMaxWidth(), label = { Text("Note (optional)") }, minLines = 2)
    Button({ vm.save(n); Wa.openChat(c, NumberCleaner.clean(n, cc), m) }, enabled = n.isNotBlank()) { Text("Open my self-chat") }
    if (m.isNotBlank()) Actions(m) }
