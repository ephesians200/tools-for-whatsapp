package com.prince.whatstoolbox.ui.screens
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.prince.whatstoolbox.domain.usecase.FieldType
import com.prince.whatstoolbox.presentation.FormIntent
import com.prince.whatstoolbox.presentation.FormViewModel
import com.prince.whatstoolbox.ui.components.*
import com.prince.whatstoolbox.util.*
import java.util.concurrent.TimeUnit

/** Renders any declarative form tool: free text, number pad, switch or chips depending on the field's [FieldType]. */
@Composable fun FormTool(id: String, vm: FormViewModel = hiltViewModel()) {
    val cc = LocalCc.current
    LaunchedEffect(id) { vm.load(id) }
    val s by vm.state.collectAsStateWithLifecycle(); val spec = s.spec
    if (spec == null) { Text("This tool isn't available yet."); return }
    spec.fields.forEach { f -> val v = s.values[f.key].orEmpty()
        when (val t = f.type) {
            FieldType.Toggle -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(f.label, Modifier.weight(1f)); Switch(v == "true", { vm.onIntent(FormIntent.Edit(f.key, it.toString())) }) }
            is FieldType.Choice -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(f.label, style = MaterialTheme.typography.labelLarge); Chips(t.options, t.options.indexOf(v)) { vm.onIntent(FormIntent.Edit(f.key, t.options[it])) } }
            FieldType.Number -> OutlinedTextField(v, { vm.onIntent(FormIntent.Edit(f.key, it.filter { ch -> ch.isDigit() || ch == '.' })) }, Modifier.fillMaxWidth(), label = { Text(f.label) }, singleLine = true, keyboardOptions = DecimalKeyboard)
            FieldType.Text -> OutlinedTextField(v, { vm.onIntent(FormIntent.Edit(f.key, it)) }, Modifier.fillMaxWidth(), label = { Text(f.label) }, minLines = f.lines)
        } }
    s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    AnimatedVisibility(s.output.isNotBlank()) { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Output(s.output, spec.preview?.invoke(s.output), spec.numberKey?.let { s.values[it] }?.takeIf { it.isNotBlank() }?.let { NumberCleaner.clean(it, cc) }.orEmpty()) } } }

/** Notification permission is asked when the user taps Schedule (not when the screen opens), and the result is reported honestly. */
@Composable fun Reminder() { val c = LocalContext.current; val cc = LocalCc.current
    var n by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }; var mins by remember { mutableStateOf("60") }; var done by remember { mutableStateOf("") }
    fun schedule(notificationsOk: Boolean) {
        WorkManager.getInstance(c).enqueue(OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(mins.toLongOrNull() ?: 60, TimeUnit.MINUTES)
            .setInputData(workDataOf("n" to NumberCleaner.clean(n, cc), "t" to note)).build())
        done = if (notificationsOk) "Scheduled ✓ — tap the notification to open the chat" else "Scheduled, but notifications are blocked — turn them on in Android Settings or you won't see the reminder." }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> schedule(ok) }
    OutlinedTextField(n, { n = it }, Modifier.fillMaxWidth(), label = { Text("Customer number") }, keyboardOptions = PhoneKeyboard, singleLine = true)
    OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Reminder note") })
    OutlinedTextField(mins, { mins = it.filter(Char::isDigit) }, label = { Text("Remind me in (minutes)") }, keyboardOptions = NumberKeyboard, singleLine = true)
    Text("Timing is approximate — Android may deliver it a few minutes late when the phone is idle.", style = MaterialTheme.typography.bodySmall)
    Button({ if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) perm.launch(Manifest.permission.POST_NOTIFICATIONS) else schedule(true) },
        enabled = n.isNotBlank()) { Text("Schedule") }
    if (done.isNotBlank()) Text(done) }

@Composable fun CalendarTool() { val c = LocalContext.current; var t by remember { mutableStateOf("") }
    OutlinedTextField(t, { t = it }, Modifier.fillMaxWidth(), label = { Text("Paste message, e.g. “Meeting tomorrow 3pm at the office”") }, minLines = 3)
    val d = DateParse.parse(t)
    if (t.isNotBlank()) { Text(d?.let { java.text.SimpleDateFormat("EEE d MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(it.time) } ?: "No date/time found")
        if (d != null) Button({ runCatching { c.startActivity(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.Events.TITLE, t.take(60))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, d.timeInMillis).putExtra(CalendarContract.EXTRA_EVENT_END_TIME, d.timeInMillis + 3_600_000)) } }) { Text("Add to Calendar") } } }

@Composable fun Invoice() { val c = LocalContext.current; var s by remember { mutableStateOf("") }; var b by remember { mutableStateOf("") }; var cur by remember { mutableStateOf("₵") }; var items by remember { mutableStateOf("") }
    OutlinedTextField(s, { s = it }, Modifier.fillMaxWidth(), label = { Text("Your business") }); OutlinedTextField(b, { b = it }, Modifier.fillMaxWidth(), label = { Text("Buyer") })
    OutlinedTextField(cur, { cur = it }, label = { Text("Currency") }); OutlinedTextField(items, { items = it }, Modifier.fillMaxWidth(), label = { Text("Items: name, qty, price (one per line)") }, minLines = 4)
    val rows = items.lines().mapNotNull { l -> l.split(",").map { p -> p.trim() }.takeIf { p -> p.size == 3 && p[1].toIntOrNull() != null && p[2].toDoubleOrNull() != null }?.let { p -> Triple(p[0], p[1].toInt(), p[2].toDouble()) } }
    Text("Total: $cur%.2f".format(rows.sumOf { r -> r.second * r.third }))
    if (rows.isNotEmpty()) ShareBar("application/pdf", text = "Invoice from $s") { listOf(Pdfs.invoice(c, s, b, rows, cur)) } }
