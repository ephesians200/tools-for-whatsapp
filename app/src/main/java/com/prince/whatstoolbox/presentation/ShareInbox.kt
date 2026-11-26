package com.prince.whatstoolbox.presentation
import com.prince.whatstoolbox.util.ChatParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything that arrives from outside the app (share sheet, text-selection menu, shortcuts, widget) lands here as one [Item].
 * Each item has a nonce, so sharing the same text twice still navigates; tools [consume] an item once they've used it.
 */
@Singleton class ShareInbox @Inject constructor() {
    enum class Kind { CHAT, PHONE, URL, TEXT, OPEN }
    data class Item(val nonce: Long, val kind: Kind, val text: String)

    private val _item = MutableStateFlow<Item?>(null); val item: StateFlow<Item?> = _item.asStateFlow(); private var n = 0L
    /** The last shared chat export. Stays available to every chat tool until the user taps "use another chat" (or the app process ends). */
    private val _chat = MutableStateFlow<String?>(null); val chat: StateFlow<String?> = _chat.asStateFlow()
    fun clearChat() { _chat.value = null }
    fun post(raw: String, force: Kind? = null) { val t = raw.trim(); if (t.isEmpty()) return; val k = force ?: classify(t); if (k == Kind.CHAT) _chat.value = t; _item.value = Item(++n, k, t) }
    fun openTool(id: String) { _item.value = Item(++n, Kind.OPEN, id) }
    fun consume(nonce: Long) { _item.update { if (it?.nonce == nonce) null else it } }

    companion object {
        private val urlRx = Regex("^(https?://|www\\.)\\S+$", RegexOption.IGNORE_CASE); private val phoneRx = Regex("^[+\\d][\\d\\s().-]{5,24}$")
        fun classify(t: String): Kind {
            if (ChatParser.parse(t).count { it.sender != null } >= 2) return Kind.CHAT
            if (!t.contains('\n') && urlRx.matches(t)) return Kind.URL
            if (phoneRx.matches(t) && t.count(Char::isDigit) in 7..15) return Kind.PHONE
            return if (t.contains('\n') && t.length > 200) Kind.CHAT else Kind.TEXT
        }
    }
}
