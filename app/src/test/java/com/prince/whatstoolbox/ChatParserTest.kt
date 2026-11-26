package com.prince.whatstoolbox
import com.prince.whatstoolbox.util.ChatParser
import org.junit.Assert.*
import org.junit.Test

class ChatParserTest {
    private val android24 = "12/03/2024, 14:05 - Ama: Hello 😀\n12/03/2024, 14:06 - Kofi: Hi there\nsecond line\n12/03/2024, 14:07 - Messages are end-to-end encrypted."
    @Test fun parsesAndroid24h() { val m = ChatParser.parse(android24); assertEquals(3, m.size); assertEquals("Ama", m[0].sender); assertEquals("Hi there\nsecond line", m[1].text); assertNull(m[2].sender) }
    @Test fun twelveHourIsConvertedTo24h() {
        val m = ChatParser.parse("3/12/24, 2:05 PM - Ama: afternoon\n3/12/24, 2:10 AM - Kofi: night\n3/12/24, 12:30 AM - Ama: just after midnight")
        assertEquals(listOf("14:05", "02:10", "00:30"), m.map { it.time })
        val s = ChatParser.stats(m); assertEquals(1, s.byHour[14]); assertEquals(1, s.byHour[2]); assertEquals(1, s.byHour[0])   // 2 PM and 2 AM are no longer merged
    }
    @Test fun narrowNoBreakSpaceBeforeAmPm() { val m = ChatParser.parse("3/12/24, 2:05\u202FPM - Ama: hi"); assertEquals(1, m.size); assertEquals("14:05", m[0].time) }
    @Test fun iosBracketFormat() { val m = ChatParser.parse("[12/03/24, 14:05:11] Ama: hello"); assertEquals("Ama", m[0].sender); assertEquals("hello", m[0].text) }
    @Test fun emojiAreCountedAsWholeClusters() {
        val e = ChatParser.emojis("👨\u200D👩\u200D👧 👍🏽 ❤️ plain text 123")
        assertTrue(e.size >= 3); assertFalse(e.any { it.length == 1 && it[0].isLetterOrDigit() })
    }
}
