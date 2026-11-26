package com.prince.whatstoolbox
import com.prince.whatstoolbox.util.DateParse
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class DateParseTest {
    @Test fun findsTimeInMessage() { val d = DateParse.parse("Meeting tomorrow 3pm at the office"); assertNotNull(d); assertEquals(15, d!!.get(Calendar.HOUR_OF_DAY)) }
    @Test fun noDate() = assertNull(DateParse.parse("hello there"))
}
