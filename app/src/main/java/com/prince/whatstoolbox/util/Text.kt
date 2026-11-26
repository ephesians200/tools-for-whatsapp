package com.prince.whatstoolbox.util

object FancyFonts {
    private const val BASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private fun table(e: String): (String) -> String { val m = e.split(","); return { s -> s.map { c -> BASE.indexOf(c).let { i -> if (i >= 0) m[i] else c.toString() } }.joinToString("") } }
    private fun combine(mark: String): (String) -> String = { s -> s.map { if (it.isWhitespace()) it.toString() else "$it$mark" }.joinToString("") }
    private fun wrap(l: String, r: String): (String) -> String = { s -> "$l$s$r" }
    private val leet = mapOf('a' to '4', 'e' to '3', 'i' to '1', 'o' to '0', 's' to '5', 't' to '7', 'b' to '8', 'g' to '9')

    /** 50+ styles: Unicode tables, combining marks, decorators and transforms. */
    val styles: Map<String, (String) -> String> = LinkedHashMap<String, (String) -> String>().apply {
        FontTables.data.forEach { (n, e) -> put(n, table(e)) }
        listOf("Strikethrough" to "\u0336", "Underline" to "\u0332", "Double Underline" to "\u0333", "Slash" to "\u0338", "Overline" to "\u0305",
            "Dotted Above" to "\u0307", "Dot Below" to "\u0323", "Tilde Through" to "\u0334", "Tilde Above" to "\u0303", "Ring Above" to "\u030A",
            "Cross Below" to "\u0353", "Bridge Below" to "\u032A", "Arrow Below" to "\u034D").forEach { (n, m) -> put(n, combine(m)) }
        listOf("Sparkles" to ("✨" to "✨"), "Flourish" to ("꧁" to "꧂"), "Corner Brackets" to ("『" to "』"), "Lenticular" to ("【" to "】"), "Stars" to ("★" to "★"),
            "Dotted Ends" to ("•°•" to "•°•"), "Blocks" to ("▄▀▄ " to " ▄▀▄"), "Hearts" to ("♥ " to " ♥"), "Fire" to ("🔥" to "🔥"), "Quotes" to ("「" to "」"),
            "Arrows" to ("➤ " to " ◄"), "Crown" to ("👑 " to " 👑")).forEach { (n, p) -> put(n, wrap(p.first, p.second)) }
        put("Spaced") { s -> s.toList().joinToString(" ") }
        put("Dotted") { s -> s.toList().joinToString("·") }
        put("Alternating Case") { s -> s.mapIndexed { i, c -> if (i % 2 == 0) c.uppercaseChar() else c.lowercaseChar() }.joinToString("") }
        put("Leet") { s -> s.map { leet[it.lowercaseChar()] ?: it }.joinToString("") }
        put("Upside Down") { s -> Gen.flip(s) }
        put("Reversed") { s -> s.reversed() }
    }
}
object MarkdownStyler {
    fun bold(s: String) = "*$s*"; fun italic(s: String) = "_${s}_"; fun strike(s: String) = "~$s~"
    fun mono(s: String) = "```$s```"; fun quote(s: String) = s.lines().joinToString("\n") { "> $it" }
    fun bullets(s: String) = s.lines().filter { it.isNotBlank() }.joinToString("\n") { "• $it" }
}
object TextStats {
    fun words(s: String) = s.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    fun readSeconds(s: String) = words(s) * 60 / 200
}
