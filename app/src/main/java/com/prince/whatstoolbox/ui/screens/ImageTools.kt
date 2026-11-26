package com.prince.whatstoolbox.ui.screens
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.prince.whatstoolbox.ui.components.*
import com.prince.whatstoolbox.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

// All bitmap building below runs on Dispatchers.Default (rememberBitmapAsync / withContext) — never on the main thread while typing or dragging.

@Composable fun Fitter() { val c = LocalContext.current; val scope = rememberCoroutineScope(); var src by remember { mutableStateOf<Bitmap?>(null) }; var mode by remember { mutableIntStateOf(0) }
    Button(pickImage { u -> scope.launch { src = withContext(Dispatchers.IO) { Img.load(c, u) } } }) { Text("Choose photo") }; Chips(listOf("Blur", "Mirror", "White", "Black", "Green"), mode) { mode = it }
    src?.let { s -> val o by rememberBitmapAsync(s, mode) { Img.fitSquare(s, mode, listOf(android.graphics.Color.WHITE, android.graphics.Color.BLACK, 0xFF25D366.toInt())[(mode - 2).coerceIn(0, 2)]) }
        Preview(o); ShareBar("image/png") { listOfNotNull(o?.let { Out.saveBitmap(c, it, "avatar.png") }) } } }

@Composable fun Redact() { val c = LocalContext.current; val scope = rememberCoroutineScope()
    var orig by remember { mutableStateOf<Bitmap?>(null) }; var bmp by remember { mutableStateOf<Bitmap?>(null) }; var ver by remember { mutableIntStateOf(0) }
    val undo: SnapshotStateList<Bitmap> = remember { mutableStateListOf() }
    var chip by remember { mutableIntStateOf(0) }; val modes = listOf(2, 1, 0)   // chips: Black-out (default), Pixelate, Blur -> Img.redact modes
    var a by remember { mutableStateOf(Offset.Zero) }; var z by remember { mutableStateOf(Offset.Zero) }; var dragging by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize(1, 1)) }; var busy by remember { mutableStateOf(false) }
    Button(pickImage { u -> scope.launch { val o = withContext(Dispatchers.IO) { Img.load(c, u) }; orig = o; bmp = o.copy(Bitmap.Config.ARGB_8888, true); undo.clear(); ver++ } }) { Text("Choose photo") }
    Chips(listOf("Black-out", "Pixelate", "Blur"), chip) { chip = it }
    Text("Drag over the area to hide. Blur and pixelate can sometimes be partly reversed on small text — use Black-out for anything sensitive.", style = MaterialTheme.typography.bodySmall)
    bmp?.let { b ->
        androidx.compose.runtime.key(ver) { Box(Modifier.fillMaxWidth().aspectRatio(b.width.toFloat() / b.height).onSizeChanged { size = it }.pointerInput(b, chip) {
            detectDragGestures(onDragStart = { a = it; z = it; dragging = true }, onDrag = { ch, _ -> z = ch.position }, onDragCancel = { dragging = false }, onDragEnd = {
                dragging = false
                val sx = b.width.toFloat() / size.width; val sy = b.height.toFloat() / size.height
                val r = android.graphics.Rect((min(a.x, z.x) * sx).toInt(), (min(a.y, z.y) * sy).toInt(), (maxOf(a.x, z.x) * sx).toInt(), (maxOf(a.y, z.y) * sy).toInt())
                if (r.width() > 2 && r.height() > 2) { undo.add(b.copy(Bitmap.Config.ARGB_8888, true)); if (undo.size > 3) undo.removeAt(0)
                    scope.launch { busy = true; withContext(Dispatchers.Default) { Img.redact(b, r, modes[chip]) }; busy = false; ver++ } } }) }) {
            Image(b.asImageBitmap(), "Photo to redact", Modifier.fillMaxSize())
            if (dragging) Canvas(Modifier.fillMaxSize()) { val tl = Offset(min(a.x, z.x), min(a.y, z.y)); val sz = Size(abs(a.x - z.x), abs(a.y - z.y))
                drawRect(Color.White.copy(alpha = 0.25f), tl, sz); drawRect(Color.White, tl, sz, style = Stroke(3f)) }
            if (busy) CircularProgressIndicator(Modifier.align(Alignment.Center)) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ if (undo.isNotEmpty()) { bmp = undo.removeAt(undo.lastIndex); ver++ } }, enabled = undo.isNotEmpty() && !busy) { Text("Undo") }
            TextButton({ orig?.let { bmp = it.copy(Bitmap.Config.ARGB_8888, true); undo.clear(); ver++ } }) { Text("Reset") } }
        ShareBar("image/jpeg") { listOf(Out.saveBitmap(c, b, "redacted.jpg")) } } }

@Composable fun ExifWm() { val c = LocalContext.current; val scope = rememberCoroutineScope(); var src by remember { mutableStateOf<Bitmap?>(null) }; var t by remember { mutableStateOf("FOR KYC ONLY") }
    Button(pickImage { u -> scope.launch { src = withContext(Dispatchers.IO) { Img.load(c, u) } } }) { Text("Choose photo") }; OutlinedTextField(t, { t = it }, Modifier.fillMaxWidth(), label = { Text("Watermark text") })
    Text("Output is re-encoded, so GPS/camera EXIF is removed.", style = MaterialTheme.typography.bodySmall)
    src?.let { s -> val o by rememberBitmapAsync(s, t) { Img.watermark(s, t) }; Preview(o); ShareBar("image/jpeg") { listOfNotNull(o?.let { Out.saveBitmap(c, it, "clean.jpg") }) } } }

@Composable fun Poster() { val c = LocalContext.current; var t by remember { mutableStateOf("Your text here") }; var g by remember { mutableIntStateOf(0) }; var st by remember { mutableIntStateOf(0) }; var sz by remember { mutableFloatStateOf(90f) }
    val pairs = listOf(0xFF6D5BE8 to 0xFF3A2A9C, 0xFF25D366 to 0xFF075E54, 0xFFFF7E5F to 0xFFFEB47B, 0xFF000000 to 0xFF1F1F1F, 0xFF00B8D9 to 0xFF0B3C5D)
    OutlinedTextField(t, { t = it }, Modifier.fillMaxWidth(), label = { Text("Text") }, minLines = 2); Swatches(pairs.map { it.first.toInt() }, g) { g = it }
    Chips(listOf("Bold", "Serif", "Mono", "Italic"), st) { st = it }; Slider(sz, { sz = it }, valueRange = 40f..160f)
    val b by rememberBitmapAsync(t, g, st, sz) { Img.poster(t, pairs[g].first.toInt(), pairs[g].second.toInt(), sz, st) }; Preview(b)
    ShareBar("image/png", text = t) { listOfNotNull(b?.let { Out.saveBitmap(c, it, "status_poster.png") }) } }

@Composable fun Wallpaper(amoled: Boolean) { val c = LocalContext.current; var bg by remember { mutableIntStateOf(0) }; var gl by remember { mutableIntStateOf(0) }; var bub by remember { mutableStateOf(!amoled) }
    val bgs = listOf(0xFF0B141A, 0xFF075E54, 0xFFE9E5FB, 0xFF1F2C34, 0xFF3A2A9C).map { it.toInt() }; val gls = listOf(0xFF6D5BE8, 0xFF25D366, 0xFFF5A623, 0xFFFF4D6D, 0xFF00B8D9).map { it.toInt() }
    if (!amoled) { Text("Background"); Swatches(bgs, bg) { bg = it } }; Text(if (amoled) "Glow colour" else "Glow accent"); Swatches(gls, gl) { gl = it }
    Row(verticalAlignment = Alignment.CenterVertically) { Switch(bub, { bub = it }); Text("  Preview with chat bubbles") }
    val base = if (amoled) 0xFF000000.toInt() else bgs[bg]; val b by rememberBitmapAsync(bg, gl, bub) { Img.wallpaper(base, gls[gl], bub) }; Preview(b)
    ShareBar("image/png", showChat = false) { listOf(Out.saveBitmap(c, Img.wallpaper(base, gls[gl], false), "wallpaper.png")) } }
