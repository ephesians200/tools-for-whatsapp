package com.prince.whatstoolbox.ui.screens
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prince.whatstoolbox.StickerProvider
import com.prince.whatstoolbox.domain.usecase.Engine
import com.prince.whatstoolbox.presentation.MediaViewModel
import com.prince.whatstoolbox.ui.components.*
import com.prince.whatstoolbox.util.Wa

@Composable fun Splitter(vm: MediaViewModel = hiltViewModel()) { NotificationPermissionEffect(); val s by vm.state.collectAsStateWithLifecycle()
    val secs = listOf(30, 60, 90); var clip by remember { mutableIntStateOf(0) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u: Uri? -> u?.let { vm.splitVideo(it, secs[clip]) } }
    Text("Splits a video into consecutive clips, fully on-device. Pick the length WhatsApp currently allows for Status — it has changed over time.")
    Chips(secs.map { "$it s" }, clip) { clip = it }
    Button({ pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }, enabled = !s.busy) { Text("Choose video") }; MediaStatus(s) { vm.cancel() }
    if (s.files.isNotEmpty()) { Text("${s.files.size} clips ready"); ShareBar("video/mp4") { s.files } } }

@Composable fun DocPrep(vm: MediaViewModel = hiltViewModel()) { NotificationPermissionEffect(); val s by vm.state.collectAsStateWithLifecycle()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { if (it.isNotEmpty()) vm.prepareDocs(it) }
    Text("Original bytes stay untouched; files are shared as documents so WhatsApp doesn't recompress them.")
    Button({ pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !s.busy) { Text("Choose photos") }; MediaStatus(s) { vm.cancel() }
    if (s.files.isNotEmpty()) ShareBar("application/octet-stream") { s.files } }

@Composable fun Stickers(vm: MediaViewModel = hiltViewModel()) { NotificationPermissionEffect(); val c = LocalContext.current; val s by vm.state.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("My Pack") }; var emoji by remember { mutableStateOf("😀") }; var result by remember { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { if (it.isNotEmpty()) vm.createStickers(it, name, emoji) }
    // WhatsApp answers with RESULT_OK or a validation error, which we now show instead of ignoring.
    val enable = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        result = if (r.resultCode == Activity.RESULT_OK) "Pack added to WhatsApp ✓" else r.data?.getStringExtra("validation_error")?.let { "WhatsApp rejected the pack: $it" } ?: "Pack was not added." }
    LaunchedEffect(s.packId) { s.packId?.let { id -> val pkg = Wa.installed(c).firstOrNull()
        if (pkg == null) vm.setStatus("WhatsApp isn't installed.") else runCatching {
            enable.launch(Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK").setPackage(pkg).putExtra("sticker_pack_id", id)
                .putExtra("sticker_pack_authority", StickerProvider.authority(c)).putExtra("sticker_pack_name", s.packName)) }.onFailure { vm.setStatus("Couldn't open WhatsApp: ${it.message}") }
        vm.consumePack() } }
    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Pack name") })
    OutlinedTextField(emoji, { emoji = it.take(4) }, label = { Text("Emoji for the stickers") }, singleLine = true)
    Button({ pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !s.busy) { Text("Choose 3–30 images & add to WhatsApp") }; MediaStatus(s) { vm.cancel() }
    if (result.isNotBlank()) Text(result) }

@Composable fun Compressor(vm: MediaViewModel = hiltViewModel()) { NotificationPermissionEffect(); val s by vm.state.collectAsStateWithLifecycle()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(vm::compressVideo) }
    Text("Shrinks a video to 720p at about 1.5 Mbps."); Button({ pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }, enabled = !s.busy) { Text("Choose video") }; MediaStatus(s) { vm.cancel() }
    if (s.files.isNotEmpty()) { Text("${s.files[0].length() / 1024} KB"); ShareBar("video/mp4") { s.files } } }

@Composable fun AudioVideo(vm: MediaViewModel = hiltViewModel()) { NotificationPermissionEffect(); val s by vm.state.collectAsStateWithLifecycle(); var img by remember { mutableStateOf<Uri?>(null) }; var aud by remember { mutableStateOf<Uri?>(null) }
    val pa = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { aud = it }
    Button(pickImage { img = it }) { Text(if (img == null) "1. Choose image" else "Image ✓") }; Button({ pa.launch("audio/*") }) { Text(if (aud == null) "2. Choose audio" else "Audio ✓") }
    Button({ vm.audioToVideo(img!!, aud!!) }, enabled = img != null && aud != null && !s.busy) { Text("Create 30s video") }; MediaStatus(s) { vm.cancel() }
    if (s.files.isNotEmpty()) ShareBar("video/mp4") { s.files } }

@Composable fun Opus(vm: MediaViewModel = hiltViewModel()) { NotificationPermissionEffect(); val s by vm.state.collectAsStateWithLifecycle()
    val pa = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(vm::toMp3) }
    Text("Converts a WhatsApp voice note (.opus) to a real .mp3 (falls back to .m4a only if the encoder can't load).", style = MaterialTheme.typography.bodySmall)
    Button({ pa.launch("audio/*") }, enabled = !s.busy) { Text("Choose voice note") }; MediaStatus(s) { vm.cancel() }
    s.files.firstOrNull()?.let { f -> Text(f.name); ShareBar(if (f.extension == "mp3") "audio/mpeg" else "audio/mp4") { s.files } } }

@Composable fun Transcribe(vm: MediaViewModel = hiltViewModel()) { val s by vm.state.collectAsStateWithLifecycle(); val engine = remember { vm.engine() }
    if (engine == Engine.MIC_ONLY) { TranscribeMic(); return }
    var uri by remember { mutableStateOf<Uri?>(null) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) uri?.let(vm::transcribe) else vm.setStatus("Android's speech service needs the microphone permission, even for files.") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u -> if (u != null) { uri = u; if (engine == Engine.VOSK) vm.transcribe(u) else perm.launch(Manifest.permission.RECORD_AUDIO) } }
    Text(if (engine == Engine.VOSK) "Offline engine (Vosk) — works on any Android version, no microphone permission." else "Using Android's on-device speech service (Android 13+).", style = MaterialTheme.typography.bodySmall)
    Button({ pick.launch("audio/*") }, enabled = !s.busy) { Text("Choose voice note") }; MediaStatus(s) { vm.cancel() }
    OutlinedTextField(s.text, vm::editText, Modifier.fillMaxWidth(), minLines = 5); if (s.text.isNotBlank()) Actions(s.text) }

/** Android 12 and older without the Vosk model: live microphone dictation only. */
@Composable fun TranscribeMic() { val c = LocalContext.current; var text by remember { mutableStateOf("") }; var on by remember { mutableStateOf(false) }
    val rec = remember { if (android.speech.SpeechRecognizer.isRecognitionAvailable(c)) android.speech.SpeechRecognizer.createSpeechRecognizer(c) else null }; DisposableEffect(Unit) { onDispose { rec?.destroy() } }
    val start = { rec?.setRecognitionListener(object : android.speech.RecognitionListener {
        override fun onReadyForSpeech(p: android.os.Bundle?) {}; override fun onBeginningOfSpeech() {}; override fun onRmsChanged(v: Float) {}; override fun onBufferReceived(b: ByteArray?) {}
        override fun onEndOfSpeech() {}; override fun onError(e: Int) { on = false }; override fun onPartialResults(p: android.os.Bundle?) {}; override fun onEvent(t: Int, p: android.os.Bundle?) {}
        override fun onResults(r: android.os.Bundle?) { text += r?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty() + " "; on = false } })
        rec?.startListening(Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE, true)); on = true }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) start() }
    Text("This Android version can't transcribe files. Add the offline Vosk model (see README) or tap Listen and dictate / play the note aloud.", style = MaterialTheme.typography.bodySmall)
    Button({ perm.launch(Manifest.permission.RECORD_AUDIO) }, enabled = rec != null && !on) { Text(if (on) "Listening…" else "Listen") }
    OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), minLines = 4); if (text.isNotBlank()) Actions(text) }
