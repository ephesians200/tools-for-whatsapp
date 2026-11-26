package com.prince.whatstoolbox.util
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.StyleSpan
import java.io.File
import java.text.BreakIterator
import java.util.Locale

/** [time] is always normalised to 24-hour "HH:mm", whatever the export's locale used. */
data class Msg(val date: String, val time: String, val sender: String?, val text: String)
data class ChatStats(val total: Int, val bySender: List<Pair<String, Int>>, val byHour: Map<Int, Int>,
                     val byDay: List<Pair<String, Int>>, val topEmoji: List<Pair<String, Int>>, val links: List<String>)

object ChatParser {
    // Android: "12/03/2024, 14:05 - Name: text"  |  iOS: "[12/03/24, 14:05:11] Name: text"  |  12h: "3/12/24, 2:05 PM - …" (space before AM/PM may be U+202F or U+00A0)
    private const val WS = "[\\s\\u202F\\u00A0]"
    private val line = Regex("""^\u200E?\[?(\d{1,4}[/.\-]\d{1,2}[/.\-]\d{1,4}),?$WS+(\d{1,2}:\d{2})(?::\d{2})?(?:$WS?([APap])\.?[Mm]\.?)?\]?$WS?-?$WS?(?:([^:]{1,60}?):$WS)?(.*)$""")
    private val url = Regex("""https?://[^\s]+""")

    fun to24(time: String, ampm: String): String {
        val (h, m) = time.split(":"); var hh = h.toInt()
        if (ampm.equals("p", true) && hh < 12) hh += 12 else if (ampm.equals("a", true) && hh == 12) hh = 0
        return String.format(Locale.ROOT, "%02d:%s", hh, m)
    }

    fun parse(raw: String): List<Msg> {
        val out = ArrayList<Msg>()
        for (l in raw.lineSequence()) {
            val m = line.find(l)
            if (m != null) out += Msg(m.groupValues[1], to24(m.groupValues[2], m.groupValues[3]), m.groupValues[4].ifEmpty { null }, m.groupValues[5])
            else if (out.isNotEmpty()) out[out.lastIndex] = out.last().let { it.copy(text = it.text + "\n" + l) } // multi-line message
        }
        return out
    }

    /** True for emoji (incl. flags and symbols) but not for plain text, digits, arrows or box-drawing characters. */
    fun isEmoji(g: String): Boolean { if (g.isEmpty()) return false; val cp = g.codePointAt(0)
        return cp in 0x1F000..0x1FAFF || cp in 0x2600..0x27BF || cp == 0x2B50 || cp == 0x2B55 || cp == 0x2B1B || cp == 0x2B1C || cp == 0x231A || cp == 0x231B ||
            cp in 0x23E9..0x23F3 || cp == 0x2934 || cp == 0x2935 || cp == 0x203C || cp == 0x2049 || cp == 0x3030 || cp == 0x303D || cp == 0x3297 || cp == 0x3299 }
    /** Grapheme-cluster aware: "👨‍👩‍👧", "👍🏽" and flags count once. */
    fun emojis(s: String): List<String> {
        val bi = BreakIterator.getCharacterInstance(); bi.setText(s); val out = ArrayList<String>(); var st = bi.first(); var en = bi.next()
        while (en != BreakIterator.DONE) { val g = s.substring(st, en); if (isEmoji(g)) out += g; st = en; en = bi.next() }
        return out
    }

    fun stats(msgs: List<Msg>): ChatStats {
        val real = msgs.filter { it.sender != null }
        val ec = HashMap<String, Int>()
        real.forEach { m -> emojis(m.text).forEach { ec.merge(it, 1, Int::plus) } }
        return ChatStats(real.size,
            real.groupingBy { it.sender!! }.eachCount().toList().sortedByDescending { it.second },
            real.groupingBy { it.time.substringBefore(':').toInt() }.eachCount().toSortedMap(),
            real.groupingBy { it.date }.eachCount().toList().sortedByDescending { it.second }.take(5),
            ec.toList().sortedByDescending { it.second }.take(10),
            real.flatMap { m -> url.findAll(m.text).map { it.value }.toList() }.distinct())
    }
    fun silent(s: ChatStats, threshold: Int = 3) = s.bySender.filter { it.second < threshold }.map { it.first }

    /** WhatsApp-styled printable PDF (A4, chat bubbles, width-aware wrapping, page numbers). Call off the main thread for big chats. */
    fun toPdf(c: Context, msgs: List<Msg>, me: String?): File {
        val doc = PdfDocument(); val w = 595; val h = 842; val pad = 32f; val bw = 380f; val inner = (bw - 20f).toInt()
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.BLACK }
        val bg = Paint(Paint.ANTI_ALIAS_FLAG); val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = Color.DKGRAY }
        val foot = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = Color.GRAY; textAlign = Paint.Align.CENTER }
        var n = 1; var page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, n).create()); var y = pad
        fun finish() { page.canvas.drawText("Page $n", w / 2f, h - 14f, foot); doc.finishPage(page) }
        for (m in msgs) {
            val sender = m.sender ?: continue
            val sp = SpannableStringBuilder(sender).apply { setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); append("\n"); append(m.text) }
            val sl = StaticLayout.Builder.obtain(sp, 0, sp.length, tp, inner).setMaxLines(40).setEllipsize(TextUtils.TruncateAt.END).build()
            val bh = sl.height + 24f
            if (y + bh > h - pad - 10f) { finish(); page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, ++n).create()); y = pad }
            val mine = sender == me; val x = if (mine) w - pad - bw else pad
            bg.color = if (mine) Color.parseColor("#D9FDD3") else Color.parseColor("#F0F0F0")
            page.canvas.drawRoundRect(x, y, x + bw, y + bh, 14f, 14f, bg)
            page.canvas.save(); page.canvas.translate(x + 10f, y + 8f); sl.draw(page.canvas); page.canvas.restore()
            page.canvas.drawText(m.time, x + bw - 34f, y + bh - 5f, small)
            y += bh + 8f
        }
        finish()
        val f = File(File(c.cacheDir, "out").apply { mkdirs() }, "chat.pdf")
        f.outputStream().use { doc.writeTo(it) }; doc.close(); return f
    }
}
