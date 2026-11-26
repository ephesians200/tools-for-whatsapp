package com.prince.whatstoolbox.domain.usecase
import com.prince.whatstoolbox.domain.*
import com.prince.whatstoolbox.domain.repository.*
import com.prince.whatstoolbox.util.*
import javax.inject.Inject

class FilterToolsUseCase @Inject constructor() {
    /** Every word of [q] must match the title, description, keywords or category (so "pdf" finds the invoice maker). */
    operator fun invoke(q: String, cat: Category?, onlyFav: Boolean, favs: Set<String>): List<Tool> {
        val terms = q.trim().split(' ').filter { it.isNotBlank() }
        return ToolRegistry.all.filter { t -> (cat == null || t.category == cat) && (!onlyFav || t.id in favs) &&
            terms.all { w -> t.title.contains(w, true) || t.keywords.contains(w, true) || t.blurb.contains(w, true) || t.category.label.contains(w, true) } }
    }
}
class ObserveFavoritesUseCase @Inject constructor(private val r: FavoritesRepository) { operator fun invoke() = r.favorites }
class ToggleFavoriteUseCase @Inject constructor(private val r: FavoritesRepository) { suspend operator fun invoke(id: String) = r.toggle(id) }
class ObserveVaultUseCase @Inject constructor(private val r: VaultRepository) { operator fun invoke(kind: VaultKind) = r.observe(kind.key) }
class SaveVaultEntryUseCase @Inject constructor(private val r: VaultRepository) { suspend operator fun invoke(e: VaultEntry) = r.save(e) }
class DeleteVaultEntryUseCase @Inject constructor(private val r: VaultRepository) { suspend operator fun invoke(id: Long) = r.delete(id) }

data class WaLink(val clean: String, val valid: Boolean, val link: String)
class BuildWaLinkUseCase @Inject constructor() {
    operator fun invoke(number: String, cc: String, text: String): WaLink { val c = NumberCleaner.clean(number, cc.ifBlank { "233" }); return WaLink(c, NumberCleaner.isValid(c), Wa.link(c, text).toString()) }
}
data class ChatReport(val msgs: List<Msg>, val stats: ChatStats?, val silent: List<String>, val links: List<String>, val emails: List<String>, val phones: List<String>, val ordersCsv: String) {
    companion object { val EMPTY = ChatReport(emptyList(), null, emptyList(), emptyList(), emptyList(), emptyList(), "") }
}
class AnalyzeChatUseCase @Inject constructor() {
    operator fun invoke(raw: String): ChatReport { val m = ChatParser.parse(raw); if (m.isEmpty()) return ChatReport.EMPTY
        val s = ChatParser.stats(m); return ChatReport(m, s, ChatParser.silent(s), Extract.links(m), Extract.emails(m), Extract.phones(m), Extract.ordersCsv(m)) }
}
enum class MdOp(val label: String) { BOLD("B"), ITALIC("I"), STRIKE("S"), MONO("</>"), QUOTE("❝"), BULLETS("•") }
class ApplyMarkdownUseCase @Inject constructor() {
    operator fun invoke(op: MdOp, t: String) = when (op) { MdOp.BOLD -> MarkdownStyler.bold(t); MdOp.ITALIC -> MarkdownStyler.italic(t); MdOp.STRIKE -> MarkdownStyler.strike(t)
        MdOp.MONO -> MarkdownStyler.mono(t); MdOp.QUOTE -> MarkdownStyler.quote(t); MdOp.BULLETS -> MarkdownStyler.bullets(t) }
}
class FancyTextUseCase @Inject constructor() { operator fun invoke(t: String): List<Pair<String, String>> = if (t.isBlank()) emptyList() else FancyFonts.styles.map { it.key to it.value(t) } }
