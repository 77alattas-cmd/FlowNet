package com.fn.has.code.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatterTest {

    @Test
    fun formatPort_returnsRawPortStringWithoutSeparators() {
        assertEquals("8080", NumberFormatter.formatPort(8080))
        assertEquals("8888", NumberFormatter.formatPort(8888))
        assertEquals("9090", NumberFormatter.formatPort(9090))
        assertEquals("5353", NumberFormatter.formatPort(5353))
    }

    @Test
    fun formatDataSize_formatsBytesToReadableUnitWithSeparators() {
        assertEquals("0 B", NumberFormatter.formatDataSize(0))
        assertEquals("1 KB", NumberFormatter.formatDataSize(1024))
        assertEquals("1 MB", NumberFormatter.formatDataSize(1024 * 1024))
        assertEquals("1.5 GB", NumberFormatter.formatDataSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun formatCount_addsThousandsSeparatorsCorrectly() {
        assertEquals("1,000", NumberFormatter.formatCount(1000))
        assertEquals("1,000,000", NumberFormatter.formatCount(1000000))
    }
}
