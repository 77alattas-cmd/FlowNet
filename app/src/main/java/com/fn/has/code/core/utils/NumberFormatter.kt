package com.fn.has.code.core.utils

import java.text.NumberFormat
import java.util.Locale

object NumberFormatter {
    fun formatPort(port: Int): String {
        return port.toString()
    }

    fun formatCount(count: Long): String {
        return NumberFormat.getInstance(Locale.getDefault()).format(count)
    }

    fun formatCount(count: Int): String {
        return NumberFormat.getInstance(Locale.getDefault()).format(count.toLong())
    }
}
