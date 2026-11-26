package com.prince.whatstoolbox.util
import java.util.*

object Gen {
    fun handles(name: String): List<String> {
        val b = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), ".").trim('.'); if (b.isEmpty()) return emptyList()
        val j = b.replace(".", ""); val y = Calendar.getInstance().get(Calendar.YEAR)
        return listOf(b, j, "the.$j", "its.$j", "get.$j", "$j.official", "$j.hq", "$j.co", "$j.shop", "$j.store", "hello.$j", "$j.$y", "${j}_", "$j.1").distinct().filter { Username.isValid(it) }
    }
    fun repeat(t: String, n: Int, sep: String, nl: Boolean) = List(n.coerceIn(1, 5000)) { t }.joinToString(sep + if (nl) "\n" else "")
    private const val UP = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz"
    fun flip(t: String) = t.lowercase().map { if (it in 'a'..'z') UP[it - 'a'] else it }.reversed().joinToString("")
    fun invisible(kind: Int, n: Int) = String(CharArray(n.coerceIn(1, 5000)) { when (kind) { 0 -> '\u200B'; 1 -> '\u200D'; else -> '\u2800' } })

    private val FONT = """
010101111101101 110101110101110 011100100100011 110101101101110 111100110100111 111100110100100 011100101101011
101101111101101 111010010010111 001001001101010 101101110101101 100100100100111 101111111101101 110101101101101
010101101101010 110101110100100 010101101111011 110101110101101 011100010001110 111010010010010 101101101101111
101101101101010 101101111111101 101101010101101 101101010010010 111001010100111 111101101101111 010110010010111
110001010100111 110001110001110 101101111001001 111100110001110 011100111101111 111001010010010 111101111101111 111101111001110"""
    private val glyphs = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toList().zip(FONT.trim().split(Regex("\\s+"))).toMap()
    fun emojiArt(t: String, on: String, off: String): String =
        t.uppercase().filter { it in glyphs }.take(24).chunked(4).joinToString("\n$off\n") { chunk ->
            (0 until 5).joinToString("\n") { r -> chunk.toList().joinToString(off) { ch -> glyphs[ch]!!.substring(r * 3, r * 3 + 3).map { if (it == '1') on else off }.joinToString("") } } }

    fun split(total: Double, people: Int, tip: Double, custom: String, cur: String): String {
        val f = { d: Double -> "%.2f".format(d) }; val k = 1 + tip / 100; val n = people.coerceAtLeast(1)
        val sb = StringBuilder("🧾 *Bill split*\nSubtotal: $cur${f(total)}\nTip: ${tip.toInt()}%\nTotal: $cur${f(total * k)}\n\n")
        val shares = custom.split(",", "\n").mapNotNull { s -> s.split(":").takeIf { it.size == 2 && it[1].trim().toDoubleOrNull() != null }?.let { it[0].trim() to it[1].trim().toDouble() } }
        if (shares.isEmpty()) sb.append("Each of $n: $cur${f(total * k / n)}")
        else { shares.forEach { (nm, a) -> sb.append("• $nm: $cur${f(a * k)}\n") }
            val rest = total - shares.sumOf { it.second }; if (rest > 0.005) sb.append("Unassigned: $cur${f(rest * k)}") }
        return sb.toString()
    }
}
object DateParse {
    fun parse(text: String): Calendar? {
        val c = Calendar.getInstance(); val t = text.lowercase(); var found = false
        val iso = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""").find(t); val dmy = Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})\b""").find(t)
        when {
            iso != null -> { c.set(iso.groupValues[1].toInt(), iso.groupValues[2].toInt() - 1, iso.groupValues[3].toInt()); found = true }
            dmy != null -> { val y = dmy.groupValues[3].toInt().let { if (it < 100) it + 2000 else it }; c.set(y, dmy.groupValues[2].toInt() - 1, dmy.groupValues[1].toInt()); found = true }
            "tomorrow" in t -> { c.add(Calendar.DAY_OF_YEAR, 1); found = true }
            "today" in t -> found = true
            else -> listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday").indexOfFirst { it in t }.takeIf { it >= 0 }?.let { i ->
                var add = (i + 1) - c.get(Calendar.DAY_OF_WEEK); if (add <= 0) add += 7; c.add(Calendar.DAY_OF_YEAR, add); found = true }
        }
        val ap = Regex("""\b(\d{1,2})(?::(\d{2}))?\s?(am|pm)\b""").find(t); val h24 = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""").find(t)
        when {
            ap != null -> { var h = ap.groupValues[1].toInt() % 12; if (ap.groupValues[3] == "pm") h += 12; c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, ap.groupValues[2].ifEmpty { "0" }.toInt()); found = true }
            h24 != null -> { c.set(Calendar.HOUR_OF_DAY, h24.groupValues[1].toInt()); c.set(Calendar.MINUTE, h24.groupValues[2].toInt()); found = true }
            else -> { c.set(Calendar.HOUR_OF_DAY, 9); c.set(Calendar.MINUTE, 0) }
        }
        c.set(Calendar.SECOND, 0); return if (found) c else null
    }
}
object Extract {
    private val url = Regex("""https?://\S+|www\.\S+"""); private val email = Regex("""[\w.+-]+@[\w-]+\.[\w.-]+""")
    private val phone = Regex("""\+?\d[\d\s\-()]{7,16}\d""")
    private val order = Regex("""(?i)([a-z][a-z0-9 ]{1,40}?)\s*(?:x\s*\d+\s*)?[-:=@]\s*(?:gh₵|₵|ghs|\$|₦|€|£|ksh)?\s?(\d[\d,]*(?:\.\d{1,2})?)""")
    fun links(m: List<Msg>) = m.flatMap { x -> url.findAll(x.text).map { it.value }.toList() }.distinct()
    fun emails(m: List<Msg>) = m.flatMap { x -> email.findAll(x.text).map { it.value }.toList() }.distinct()
    fun phones(m: List<Msg>) = m.flatMap { x -> phone.findAll(x.text).map { it.value.trim() }.toList() }.distinct()
    private fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
    fun ordersCsv(m: List<Msg>): String = "Date,Customer,Item,Price\n" + m.filter { it.sender != null }.flatMap { x ->
        order.findAll(x.text.replace("\n", " ")).map { r -> listOf(x.date, x.sender!!, r.groupValues[1].trim(), r.groupValues[2].replace(",", "")).joinToString(",") { q(it) } }.toList() }.joinToString("\n")
}
object Content {
    val captions = listOf(
        "Good morning" to "Rise and shine ☀️ Today is a fresh page — write something great.",
        "Good morning" to "New day, new blessings 🌅 Make it count.",
        "Good night" to "Rest well 🌙 Tomorrow brings new chances.",
        "Good night" to "Close the day with gratitude ✨ Sweet dreams.",
        "Motivation" to "Small steps every day beat big plans someday 💪",
        "Motivation" to "Don't wish for it. Work for it. 🚀",
        "Motivation" to "Progress, not perfection. 🌱",
        "Festive" to "Wishing you joy, peace and love this festive season 🎉",
        "Festive" to "Happy New Year! May every day bring you closer to your goals 🎆",
        "Festive" to "Eid Mubarak! May your days be blessed 🌙",
        "Festive" to "Merry Christmas! Warm wishes to you and your loved ones 🎄",
        "Birthday" to "Happy Birthday! Wishing you a year full of wins 🎂",
        "Funny" to "I'm not lazy, I'm on energy-saving mode 🔋",
        "Funny" to "Life is short. Smile while you still have teeth 😁",
        "Business" to "Quality you can trust. DM to order today 🛍️",
        "Business" to "Open for orders! Limited stock — message us now 📦")
    val metaPrompts = listOf(
        "Writing" to "Rewrite this message to sound polite and professional: [paste text]",
        "Writing" to "Write a short, friendly follow-up message to a customer who hasn't replied in 3 days.",
        "Writing" to "Give me 5 catchy WhatsApp Status captions about [topic].",
        "Planning" to "Plan a 3-day budget trip to [city] with a daily itinerary.",
        "Planning" to "Create a weekly meal plan with a grocery list for [number] people.",
        "Learning" to "Explain [topic] like I'm 12, then give me 3 quiz questions.",
        "Learning" to "Teach me 10 useful phrases in [language] with pronunciation.",
        "Business" to "Write a product description for [product] in under 40 words.",
        "Business" to "Suggest 10 name ideas for a small business that sells [product].",
        "Fun" to "Tell me a clean joke about [topic].",
        "Fun" to "Give me 5 icebreaker questions for a group chat.",
        "Productivity" to "Turn these notes into a clear to-do list: [paste notes]")
}
