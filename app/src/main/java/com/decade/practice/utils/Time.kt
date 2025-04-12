package com.decade.practice.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val ONE_MINUTE_SECONDS = 60
const val ONE_HOUR_SECONDS = 60 * ONE_MINUTE_SECONDS

const val ONE_WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000

fun formatRelativeTime(timestamp: Long): String {
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL
    ).toString()
}

fun formatTime(timestamp: Long): String {
    val date = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    val current = Calendar.getInstance()

    if (current.timeInMillis - date.timeInMillis > ONE_WEEK_MILLIS)
        return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(date.time)

    if (current.get(Calendar.DAY_OF_MONTH) != date.get(Calendar.DAY_OF_MONTH))
        return SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(date.time)

    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date.time)
}