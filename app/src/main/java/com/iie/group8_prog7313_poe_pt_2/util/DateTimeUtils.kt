package com.iie.group8_prog7313_poe_pt_2.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateTimeUtils {

    // Clamps a timestamp to midnight of the same day so date-range queries are fully inclusive
    fun startOfDayMillis(utcMillis: Long): Long {
        val cal = Calendar.getInstance() // (Oracle, 2026)
        cal.timeInMillis = utcMillis
        cal.set(Calendar.HOUR_OF_DAY, 0) // (Oracle, 2026)
        cal.set(Calendar.MINUTE, 0) // (Oracle, 2026)
        cal.set(Calendar.SECOND, 0) // (Oracle, 2026)
        cal.set(Calendar.MILLISECOND, 0) // (Oracle, 2026)
        return cal.timeInMillis
    }

    // Clamps to the last millisecond of the day so expenses logged at any time on that day are included
    fun endOfDayMillis(utcMillis: Long): Long {
        val cal = Calendar.getInstance() // (Oracle, 2026)
        cal.timeInMillis = utcMillis
        cal.set(Calendar.HOUR_OF_DAY, 23) // (Oracle, 2026)
        cal.set(Calendar.MINUTE, 59) // (Oracle, 2026)
        cal.set(Calendar.SECOND, 59) // (Oracle, 2026)
        cal.set(Calendar.MILLISECOND, 999) // (Oracle, 2026)
        return cal.timeInMillis
    }

    // Uses device locale so dates display correctly for South African users (e.g. "14 Apr 2026")
    fun formatDateShort(millis: Long): String =
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(millis)

    // NEW: Check if two dates are on the same calendar day
    fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = startOfDayMillis(date1) }
        val cal2 = Calendar.getInstance().apply { timeInMillis = startOfDayMillis(date2) }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // NEW: Get days between two dates (handles month/year boundaries)
    fun daysBetween(startDate: Long, endDate: Long): Int {
        val cal1 = Calendar.getInstance().apply { timeInMillis = startOfDayMillis(startDate) }
        val cal2 = Calendar.getInstance().apply { timeInMillis = startOfDayMillis(endDate) }

        val diffInMillis = cal2.timeInMillis - cal1.timeInMillis
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }

    // Returns a human-readable string for how far away a target date is.
    fun formatTimeRemaining(targetDate: Long): String {
        val now = System.currentTimeMillis()
        if (targetDate <= now) return "Target date reached"
        val days = (targetDate - now) / (24 * 60 * 60 * 1000L)
        return when {
            days < 1  -> "Less than a day remaining"
            days < 7  -> "$days day${if (days == 1L) "" else "s"} remaining"
            days < 30 -> "${days / 7} week${if (days / 7 == 1L) "" else "s"} remaining"
            days < 365 -> "${days / 30} month${if (days / 30 == 1L) "" else "s"} remaining"
            else -> "${days / 365} year${if (days / 365 == 1L) "" else "s"} remaining"
        }
    }

    // NEW: Check if a date is within the current month
    fun isCurrentMonth(timestamp: Long): Boolean {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)

        cal.timeInMillis = timestamp
        val timestampYear = cal.get(Calendar.YEAR)
        val timestampMonth = cal.get(Calendar.MONTH)

        return currentYear == timestampYear && currentMonth == timestampMonth
    }
}

/*
  Reference list :
  - Oracle, 2026. Calendar (Java Platform SE) API documentation [online]. Available at:
    <https://docs.oracle.com/javase/8/docs/api/java/util/Calendar.html> [Accessed 20 April 2026].
 */
