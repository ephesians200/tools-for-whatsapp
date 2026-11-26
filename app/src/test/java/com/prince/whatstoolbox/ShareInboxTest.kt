package com.prince.whatstoolbox
import com.prince.whatstoolbox.presentation.ShareInbox
import com.prince.whatstoolbox.presentation.ShareInbox.Kind
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareInboxTest {
    @Test fun phone() = assertEquals(Kind.PHONE, ShareInbox.classify("+233 24 412 3456"))
    @Test fun url() = assertEquals(Kind.URL, ShareInbox.classify("https://example.com/a?utm_source=x"))
    @Test fun chat() = assertEquals(Kind.CHAT, ShareInbox.classify("12/03/2024, 14:05 - Ama: Hello\n12/03/2024, 14:06 - Kofi: Hi"))
    @Test fun plainText() = assertEquals(Kind.TEXT, ShareInbox.classify("buy milk"))
}
