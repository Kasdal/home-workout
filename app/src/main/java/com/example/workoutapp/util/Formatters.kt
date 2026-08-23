package com.example.workoutapp.util

import java.util.Locale

fun formatKg(value: Float, withUnit: Boolean = true): String {
    val number = if (value % 1f == 0f) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
    return if (withUnit) "$number kg" else number
}

fun formatDuration(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
        minutes > 0 -> if (secs > 0) "${minutes}m ${secs}s" else "${minutes}m"
        else -> "${secs}s"
    }
}
