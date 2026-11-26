package com.prince.whatstoolbox
import com.prince.whatstoolbox.util.NumberCleaner
import org.junit.Assert.*
import org.junit.Test

class NumberCleanerTest {
    @Test fun localNumberGetsCountryCode() = assertEquals("233244123456", NumberCleaner.clean("0244 123 456"))
    @Test fun alreadyInternationalWithoutPlusIsNotDoublePrefixed() = assertEquals("233244123456", NumberCleaner.clean("233244123456"))
    @Test fun plusAndSpaces() = assertEquals("233244123456", NumberCleaner.clean("+233 24 412 3456"))
    @Test fun trunkZeroInBrackets() = assertEquals("233244123456", NumberCleaner.clean("+233 (0)244123456"))
    @Test fun doubleZeroPrefix() = assertEquals("233244123456", NumberCleaner.clean("00233244123456"))
    @Test fun foreignNumberKeepsItsOwnCode() = assertEquals("14155550123", NumberCleaner.clean("+1 (415) 555-0123"))
    @Test fun customDefaultCode() = assertEquals("2348012345678", NumberCleaner.clean("08012345678", "234"))
    @Test fun validity() { assertTrue(NumberCleaner.isValid("233244123456")); assertFalse(NumberCleaner.isValid("123")) }
}
