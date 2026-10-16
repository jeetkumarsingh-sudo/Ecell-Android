package dcrustm.ecell.mobile.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

fun formatTimeAgo(millis: Long, now: Long = System.currentTimeMillis()): String {
    val diff = now - millis
    val minutes = diff / MINUTE
    val hours = diff / HOUR
    val days = diff / DAY
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "yesterday"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

/** "closes in 3h", "closes in 2d" — for deadlines in the future. */
fun formatTimeLeft(deadline: Long, now: Long = System.currentTimeMillis()): String {
    val diff = deadline - now
    return when {
        diff <= 0 -> "ended"
        diff < HOUR -> "${(diff / MINUTE).coerceAtLeast(1)}m left"
        diff < DAY -> "${diff / HOUR}h left"
        else -> "${diff / DAY}d left"
    }
}

fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(millis))

/** Due-date label relative to today: "Due today", "Due tomorrow", "Due in 3d", "Overdue by 2d". */
fun formatDueLabel(dueAt: Long, now: Long = System.currentTimeMillis()): String {
    val today = startOfDay(now)
    val dueDay = startOfDay(dueAt)
    val days = (dueDay - today) / DAY
    return when {
        days < -1 -> "Overdue by ${-days}d"
        days == -1L -> "Overdue by 1d"
        days == 0L -> "Due today"
        days == 1L -> "Due tomorrow"
        days < 7 -> "Due in ${days}d"
        else -> "Due ${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(dueAt))}"
    }
}

/**
 * Material's DatePicker hands back UTC midnight of the picked date. Turn that into the last
 * millisecond of the same calendar date in the device's time zone — tasks are due "by end of day".
 */
fun localEndOfDayFromUtcDate(utcDateMillis: Long): Long {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcDateMillis
    }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
