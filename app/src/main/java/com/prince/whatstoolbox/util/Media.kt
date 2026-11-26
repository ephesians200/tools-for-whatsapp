package com.prince.whatstoolbox.util
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Out {
    /** Deletes exports and temp audio older than [maxAgeMs] (default 6 h) so the cache can't grow without bound. */
    fun cleanup(c: Context, maxAgeMs: Long = 6 * 3_600_000L) { val now = System.currentTimeMillis()
        val outFiles = File(c.cacheDir, "out").listFiles()?.toList() ?: emptyList()
        val pcmFiles = c.cacheDir.listFiles { f: File -> f.name.startsWith("tr_") && f.name.endsWith(".pcm") }?.toList() ?: emptyList()
        (outFiles + pcmFiles).filter { it.isFile && now - it.lastModified() > maxAgeMs }.forEach { it.delete() } }
    fun file(c: Context, name: String) = File(File(c.cacheDir, "out").apply { mkdirs() }, name)
    fun uri(c: Context, f: File): Uri = FileProvider.getUriForFile(c, "${c.packageName}.fileprovider", f)
    fun share(c: Context, f: File, mime: String) = c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(mime)
        .putExtra(Intent.EXTRA_STREAM, uri(c, f)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null))
    fun shareBitmap(c: Context, b: Bitmap, name: String) { val png = name.endsWith(".png"); val f = file(c, name)
        f.outputStream().use { b.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95, it) }; share(c, f, if (png) "image/png" else "image/jpeg") }
    fun saveBitmap(c: Context, b: Bitmap, name: String): File { val f = file(c, name)
        f.outputStream().use { b.compress(if (name.endsWith(".png")) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95, it) }; return f }
    fun writeText(c: Context, name: String, content: String): File = file(c, name).also { it.writeText(content) }
    /** Puts the file(s) on the clipboard as content URIs. */
    fun copy(c: Context, files: List<File>, mime: String) { if (files.isEmpty()) return
        val uris = files.map { uri(c, it) }; val clip = android.content.ClipData.newUri(c.contentResolver, mime, uris[0]); uris.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it)) }
        (c.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip) }
    /** Targets WhatsApp directly when installed, otherwise falls back to the system chooser. */
    fun shareToWhatsApp(c: Context, files: List<File>, mime: String) { if (files.isEmpty()) return
        val uris = ArrayList(files.map { uri(c, it) })
        val i = (if (uris.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0]) else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris))
            .setType(mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val pkg = Wa.installed(c).firstOrNull(); runCatching { c.startActivity(if (pkg != null) Intent(i).setPackage(pkg) else Intent.createChooser(i, null)) } }
    fun shareText(c: Context, name: String, mime: String, content: String) { val f = file(c, name); f.writeText(content); share(c, f, mime) }
}
object Img {
    fun load(c: Context, uri: Uri, max: Int = 2048): Bitmap {
        val b = if (Build.VERSION.SDK_INT >= 28) android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(c.contentResolver, uri)) { d, info, _ ->
            d.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            val s = maxOf(info.size.width, info.size.height); if (s > max) d.setTargetSampleSize(s / max) }
        else c.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
        return b.copy(Bitmap.Config.ARGB_8888, true)
    }
    fun fitSquare(src: Bitmap, mode: Int, color: Int, size: Int = 1080): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888); val cv = Canvas(out); val p = Paint(Paint.FILTER_BITMAP_FLAG); val sz = size.toFloat()
        val s = minOf(sz / src.width, sz / src.height); val w = src.width * s; val h = src.height * s; val l = (sz - w) / 2; val t = (sz - h) / 2
        val r = RectF(l, t, l + w, t + h)
        when (mode) {
            0 -> cv.drawBitmap(Bitmap.createScaledBitmap(src, 32, 32, true), null, RectF(0f, 0f, sz, sz), p)
            1 -> listOf(Triple(-1f, 1f, l), Triple(-1f, 1f, l + w), Triple(1f, -1f, t), Triple(1f, -1f, t + h)).forEachIndexed { i, (sx, sy, pv) ->
                cv.save(); if (i < 2) cv.scale(sx, sy, pv, 0f) else cv.scale(sx, sy, 0f, pv); cv.drawBitmap(src, null, r, p); cv.restore() }
            else -> cv.drawColor(color)
        }
        cv.drawBitmap(src, null, r, p); return out
    }
    fun redact(b: Bitmap, r: Rect, mode: Int) {
        r.intersect(0, 0, b.width, b.height); if (r.width() < 2 || r.height() < 2) return
        val cv = Canvas(b)
        if (mode == 2) { cv.drawRect(r, Paint().apply { color = Color.BLACK }); return }
        val sub = Bitmap.createBitmap(b, r.left, r.top, r.width(), r.height()); val w = r.width(); val h = r.height()
        val big = if (mode == 1) Bitmap.createScaledBitmap(Bitmap.createScaledBitmap(sub, maxOf(1, w / 12), maxOf(1, h / 12), false), w, h, false)
        else Bitmap.createScaledBitmap(Bitmap.createScaledBitmap(sub, maxOf(1, w / 20), maxOf(1, h / 20), true), w, h, true)
        cv.drawBitmap(big, r.left.toFloat(), r.top.toFloat(), null)
    }
    /** Re-encoding drops ALL EXIF/GPS metadata; then stamps a tiled watermark. */
    fun watermark(src: Bitmap, text: String): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true); val cv = Canvas(out); val w = out.width.toFloat(); val h = out.height.toFloat()
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 255, 255, 255); textSize = w / 18f; typeface = Typeface.DEFAULT_BOLD; setShadowLayer(3f, 0f, 0f, Color.argb(150, 0, 0, 0)) }
        cv.rotate(-30f, w / 2, h / 2); var y = -h
        while (y < h * 2) { var x = -w; while (x < w * 2) { cv.drawText(text, x, y, p); x += p.measureText(text) + p.textSize * 2 }; y += p.textSize * 3 }
        return out
    }
    fun poster(text: String, c1: Int, c2: Int, sizePx: Float, style: Int): Bitmap {
        val b = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888); val cv = Canvas(b)
        cv.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 0f, 1920f, c1, c2, Shader.TileMode.CLAMP) })
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = sizePx
            typeface = when (style) { 0 -> Typeface.DEFAULT_BOLD; 1 -> Typeface.SERIF; 2 -> Typeface.MONOSPACE; else -> Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC) } }
        val sl = StaticLayout.Builder.obtain(text, 0, text.length, tp, 900).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        cv.translate(90f, (1920 - sl.height) / 2f); sl.draw(cv); return b
    }
    fun wallpaper(bg: Int, glow: Int, bubbles: Boolean): Bitmap {
        val b = Bitmap.createBitmap(1080, 2340, Bitmap.Config.ARGB_8888); val cv = Canvas(b); cv.drawColor(bg)
        if (glow != 0) cv.drawPaint(Paint().apply { shader = RadialGradient(540f, 2100f, 900f, Color.argb(110, Color.red(glow), Color.green(glow), Color.blue(glow)), Color.TRANSPARENT, Shader.TileMode.CLAMP) })
        if (bubbles) { val p = Paint(Paint.ANTI_ALIAS_FLAG); val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 46f }
            listOf(Triple("Hey! Are we still on for today? 👋", false, 700f), Triple("Yes! See you at 5 😊", true, 860f), Triple("Perfect, sending the details now", false, 1020f), Triple("Thanks a lot 🙏", true, 1180f)).forEach { (s, out, y) ->
                val w = t.measureText(s) + 60f; val l = if (out) 1040f - w else 40f; p.color = if (out) 0xFF005C4B.toInt() else 0xFF202C33.toInt()
                cv.drawRoundRect(l, y, l + w, y + 100f, 28f, 28f, p); cv.drawText(s, l + 30f, y + 64f, t) } }
        return b
    }
    fun qr(text: String, size: Int = 720): Bitmap { val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
        return Bitmap.createBitmap(IntArray(size * size) { if (m[it % size, it / size]) Color.BLACK else Color.WHITE }, size, size, Bitmap.Config.ARGB_8888) }
    fun connectCard(user: String, key: String, name: String): Bitmap {
        val b = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888); val cv = Canvas(b)
        cv.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 1080f, 1350f, 0xFF6D5BE8.toInt(), 0xFF3A2A9C.toInt(), Shader.TileMode.CLAMP) })
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }
        p.textSize = 52f; cv.drawText(name.ifBlank { "Connect with me on WhatsApp" }, 540f, 170f, p)
        p.textSize = 96f; p.isFakeBoldText = true; cv.drawText("@$user", 540f, 330f, p); p.isFakeBoldText = false
        if (key.isNotBlank()) { p.textSize = 52f; cv.drawText("Key: $key", 540f, 410f, p) }
        cv.drawRoundRect(RectF(250f, 480f, 830f, 1060f), 40f, 40f, Paint().apply { color = Color.WHITE }); cv.drawBitmap(qr("@$user", 520), 280f, 510f, null)
        p.textSize = 40f; cv.drawText("Search this username in WhatsApp", 540f, 1160f, p); return b
    }
    fun sticker(src: Bitmap): ByteArray {
        val b = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888); val s = minOf(512f / src.width, 512f / src.height)
        val w = src.width * s; val h = src.height * s; Canvas(b).drawBitmap(src, null, RectF((512 - w) / 2, (512 - h) / 2, (512 + w) / 2, (512 + h) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
        var q = 90; var o: ByteArrayOutputStream
        do { o = ByteArrayOutputStream(); @Suppress("DEPRECATION") b.compress(if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP, q, o); q -= 15 } while (o.size() > 100_000 && q > 10)
        return o.toByteArray()
    }
    fun tray(src: Bitmap): ByteArray = ByteArrayOutputStream().also { Bitmap.createScaledBitmap(src, 96, 96, true).compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
}
object Pdfs {
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun invoice(c: Context, seller: String, buyer: String, rows: List<Triple<String, Int, Double>>, cur: String): File {
        val doc = PdfDocument(); val pg = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create()); val cv = pg.canvas
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 24f; isFakeBoldText = true }; cv.drawText("INVOICE", 40f, 60f, p)
        p.textSize = 12f; p.isFakeBoldText = false
        cv.drawText("From: $seller", 40f, 92f, p); cv.drawText("To: $buyer", 40f, 110f, p)
        cv.drawText("Date: " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()), 400f, 92f, p)
        var y = 150f; cv.drawRect(40f, y - 16f, 555f, y + 6f, Paint().apply { color = Color.LTGRAY }); p.isFakeBoldText = true
        cv.drawText("Item", 48f, y, p); cv.drawText("Qty", 330f, y, p); cv.drawText("Price", 395f, y, p); cv.drawText("Total", 480f, y, p); p.isFakeBoldText = false
        var sum = 0.0
        rows.forEach { (n, q, pr) -> y += 24f; sum += q * pr; cv.drawText(n.take(40), 48f, y, p); cv.drawText("$q", 330f, y, p); cv.drawText("$cur%.2f".format(pr), 395f, y, p); cv.drawText("$cur%.2f".format(q * pr), 480f, y, p) }
        y += 40f; p.isFakeBoldText = true; p.textSize = 15f; cv.drawText("TOTAL: $cur%.2f".format(sum), 400f, y, p)
        doc.finishPage(pg); val f = Out.file(c, "invoice.pdf"); f.outputStream().use { doc.writeTo(it) }; doc.close(); return f
    }
}
