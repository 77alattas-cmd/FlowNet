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

    fun formatDataSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.size - 1) {
            value /= 1024.0
            unitIndex++
        }
        val formatted = if (value % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", value)
        } else {
            String.format(Locale.US, "%.1f", value)
        }
        return "$formatted ${units[unitIndex]}"
    }

    fun formatDataSize(bytes: Int): String = formatDataSize(bytes.toLong())
}
