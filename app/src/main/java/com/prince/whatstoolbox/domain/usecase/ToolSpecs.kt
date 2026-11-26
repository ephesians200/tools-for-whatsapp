package com.prince.whatstoolbox.domain.usecase
import com.prince.whatstoolbox.util.Gen

/** How a form field is rendered: free text, a number pad, an on/off switch ("true"/"false"), or a one-of-N chip row (value = the chosen option). */
sealed interface FieldType { data object Text : FieldType; data object Number : FieldType; data object Toggle : FieldType; data class Choice(val options: List<String>) : FieldType }
data class F(val key: String, val label: String, val lines: Int = 1, val def: String = "", val type: FieldType = FieldType.Text)
/** Declarative spec for every "fill fields → get text" tool. Pure logic: no Android, no UI. */
data class FormSpec(val fields: List<F>, val numberKey: String? = null, val preview: ((String) -> String)? = null, val run: (Map<String, String>) -> String)

object ToolSpecs {
    private val INVISIBLE = listOf("Zero-width space", "Zero-width joiner", "Blank braille")
    val forms: Map<String, FormSpec> = mapOf(
        "BrandHandleGenerator" to FormSpec(listOf(F("n", "Brand or name"))) { v -> Gen.handles(v["n"]!!).joinToString("\n") { "@$it" } },
        "QuickTextRepeater" to FormSpec(listOf(F("t", "Text", 2), F("n", "Times (max 5,000)", def = "10", type = FieldType.Number), F("s", "Separator", def = " "), F("l", "New line between copies", def = "false", type = FieldType.Toggle))) { v ->
            Gen.repeat(v["t"]!!, v["n"]!!.toIntOrNull() ?: 1, v["s"]!!, v["l"] == "true") },
        "TextToEmojiArt" to FormSpec(listOf(F("t", "Text (A-Z, 0-9, ≤24 chars)"), F("a", "Filled emoji", def = "🟩"), F("b", "Empty emoji", def = "⬜"))) { v -> Gen.emojiArt(v["t"]!!, v["a"]!!, v["b"]!!) },
        "InvisibleSpaceGenerator" to FormSpec(listOf(F("n", "How many characters", def = "50", type = FieldType.Number), F("k", "Type", def = INVISIBLE[2], type = FieldType.Choice(INVISIBLE))),
            preview = { "Generated ${it.length} invisible characters — tap Copy" }) { v -> Gen.invisible(INVISIBLE.indexOf(v["k"]).let { if (it < 0) 2 else it }, v["n"]!!.toIntOrNull() ?: 50) },
        "FlipReverseText" to FormSpec(listOf(F("t", "Text", 2), F("m", "Mode", def = "Flip", type = FieldType.Choice(listOf("Flip", "Reverse"))))) { v -> if (v["m"]!!.startsWith("r", true)) v["t"]!!.reversed() else Gen.flip(v["t"]!!) },
        "GroupMeetingFormatter" to FormSpec(listOf(F("t", "Meeting title"), F("d", "Date"), F("h", "Time"), F("p", "Venue / link"), F("a", "Agenda (one per line)", 4))) { v ->
            "📅 *${v["t"]}*\n🗓️ ${v["d"]}  ⏰ ${v["h"]}\n📍 ${v["p"]}\n\n*Agenda*\n" + v["a"]!!.lines().filter { it.isNotBlank() }.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n") + "\n\n*RSVP* — reply:\n✅ Yes   ❌ No   🤔 Maybe" },
        "TextCatalogBuilder" to FormSpec(listOf(F("h", "Catalog title", def = "Our Catalog"), F("c", "Currency", def = "₵"), F("i", "Items: Name - price (one per line)", 5))) { v ->
            val e = listOf("🛍️", "✨", "🔥", "💎"); "📦 *${v["h"]}*\n\n" + v["i"]!!.lines().filter { it.isNotBlank() }.mapIndexed { i, l ->
                val p = l.split("-", limit = 2); "${e[i % 4]} ${p[0].trim()}" + if (p.size > 1) " — ${v["c"]}${p[1].trim()}" else "" }.joinToString("\n") + "\n\n💬 Message us to order!" },
        "StandardOrderForm" to FormSpec(listOf(F("b", "Business name"), F("p", "Product / service"))) { v ->
            "🛒 *${v["b"]} — Order form*\nProduct: ${v["p"]}\n\nName:\nPhone:\nItem / size / colour:\nQuantity:\nDelivery address:\nPayment method:\n\nPlease fill in and send back. Thank you! 🙏" },
        "BillSplitter" to FormSpec(listOf(F("t", "Total", def = "0", type = FieldType.Number), F("n", "People", def = "2", type = FieldType.Number), F("tip", "Tip %", def = "0", type = FieldType.Number), F("c", "Currency", def = "₵"), F("x", "Custom shares: Ama:30, Kofi:20 (optional)", 2))) { v ->
            Gen.split(v["t"]!!.toDoubleOrNull() ?: 0.0, v["n"]!!.toIntOrNull() ?: 1, v["tip"]!!.toDoubleOrNull() ?: 0.0, v["x"]!!, v["c"]!!) },
        "ChatListOrganizer" to FormSpec(listOf(F("a", "🟡 Lead — chats/notes", 2), F("b", "🧾 Invoiced", 2), F("c", "✅ Paid", 2), F("d", "📦 Shipped", 2))) { v ->
            "📋 *Chat Lists plan*\n\n🟡 Lead: ${v["a"]}\n🧾 Invoiced: ${v["b"]}\n✅ Paid: ${v["c"]}\n📦 Shipped: ${v["d"]}\n\nCreate a custom list for each stage in WhatsApp's Chats tab, then move each customer chat as they progress." },
        "GroupEventFormatter" to FormSpec(listOf(F("t", "Event name"), F("d", "Date & time"), F("p", "Place"), F("x", "Details (one per line)", 3), F("r", "RSVP by"))) { v ->
            val bar = "━━━━━━━━━━━━━━"; "🎉 *${v["t"]}*\n$bar\n🗓️ ${v["d"]}\n📍 ${v["p"]}\n$bar\n" + v["x"]!!.lines().filter { it.isNotBlank() }.joinToString("\n") { "• $it" } + "\n$bar\n✅ RSVP by ${v["r"]}" },
        "AnnouncementDispatcher" to FormSpec(listOf(F("m", "Mention", def = "Team", type = FieldType.Choice(listOf("Team", "Admins", "Everyone"))), F("t", "Headline"), F("b", "Message", 4))) { v ->
            "📢 *ANNOUNCEMENT*\n@${v["m"]}\n\n*${v["t"]}*\n${v["b"]}" },
        "ChannelPollCreator" to FormSpec(listOf(F("q", "Question"), F("o", "Options (one per line, 2–12)", 4))) { v ->
            val o = v["o"]!!.lines().filter { it.isNotBlank() }; val er = buildList {
                if (v["q"]!!.length > 255) add("Question ${v["q"]!!.length}/255 chars"); if (o.size !in 2..12) add("Need 2–12 options (have ${o.size})"); o.filter { it.length > 100 }.forEach { add("Option too long (>100): ${it.take(15)}…") } }
            "📊 *${v["q"]}*\n" + o.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n") + if (er.isEmpty()) "" else "\n\n⚠️ " + er.joinToString("; ") }
    )
}
