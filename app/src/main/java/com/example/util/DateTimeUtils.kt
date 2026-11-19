package com.example.util

import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * High-performance date & time helpers compatible with minSdk 24.
 * Uses pure integer arithmetic for calendar math (Sakamoto's day-of-week, leap-year table,
 * ASCII digit parsing) and caches system wall-clock day lookups to guarantee 60+ FPS scrolling.
 */
object DateTimeUtils {

    private val monthNamesNom = arrayOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    private val monthNamesGen = arrayOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    private val monthNamesShort = arrayOf(
        "янв", "фев", "мар", "апр", "май", "июн",
        "июл", "авг", "сен", "окт", "ноя", "дек"
    )

    val weekdayNamesShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    private val sakamotoTable = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)

    @Volatile
    private var cachedDayTimestamp: Long = 0L
    @Volatile
    private var cachedTodayStr: String = "2026-01-01"
    @Volatile
    private var cachedYesterdayStr: String = "2025-12-31"
    @Volatile
    private var cachedMonthStr: String = "2026-01"
    @Volatile
    private var cachedYear: Int = 2026
    @Volatile
    private var cachedMonth: Int = 1

    private fun refreshWallClockCacheIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - cachedDayTimestamp < 15_000L) return
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        cachedYear = y
        cachedMonth = m
        cachedTodayStr = formatDate(y, m, d)
        cachedMonthStr = formatMonth(y, m)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cachedYesterdayStr = formatDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        cachedDayTimestamp = now
    }

    fun todayStr(): String {
        refreshWallClockCacheIfNeeded()
        return cachedTodayStr
    }

    fun yesterdayStr(): String {
        refreshWallClockCacheIfNeeded()
        return cachedYesterdayStr
    }

    fun currentMonthStr(): String {
        refreshWallClockCacheIfNeeded()
        return cachedMonthStr
    }

    fun formatDate(year: Int, month1to12: Int, day: Int): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month1to12, day)

    fun formatMonth(year: Int, month1to12: Int): String =
        String.format(Locale.US, "%04d-%02d", year, month1to12)

    fun parseYear(dateOrMonth: String): Int {
        if (dateOrMonth.length >= 4) {
            val d0 = dateOrMonth[0] - '0'
            val d1 = dateOrMonth[1] - '0'
            val d2 = dateOrMonth[2] - '0'
            val d3 = dateOrMonth[3] - '0'
            if (d0 in 0..9 && d1 in 0..9 && d2 in 0..9 && d3 in 0..9) {
                return d0 * 1000 + d1 * 100 + d2 * 10 + d3
            }
        }
        refreshWallClockCacheIfNeeded()
        return cachedYear
    }

    fun parseMonth(dateOrMonth: String): Int {
        if (dateOrMonth.length >= 7) {
            val d0 = dateOrMonth[5] - '0'
            val d1 = dateOrMonth[6] - '0'
            if (d0 in 0..9 && d1 in 0..9) {
                val m = d0 * 10 + d1
                if (m in 1..12) return m
            }
        }
        refreshWallClockCacheIfNeeded()
        return cachedMonth
    }

    fun parseDay(date: String): Int {
        if (date.length >= 10) {
            val d0 = date[8] - '0'
            val d1 = date[9] - '0'
            if (d0 in 0..9 && d1 in 0..9) {
                val d = d0 * 10 + d1
                if (d in 1..31) return d
            }
        }
        return 1
    }

    fun monthOf(date: String): String =
        if (date.length >= 7) date.substring(0, 7) else currentMonthStr()

    private fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    fun daysInMonth(year: Int, month1to12: Int): Int =
        when (month1to12) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }

    fun daysInMonth(monthStr: String): Int =
        daysInMonth(parseYear(monthStr), parseMonth(monthStr))

    /**
     * Pure integer math (Sakamoto's algorithm) returning 0 for Monday .. 6 for Sunday.
     */
    fun dayOfWeekMon0(date: String): Int {
        var y = parseYear(date)
        val m = parseMonth(date).coerceIn(1, 12)
        val d = parseDay(date).coerceIn(1, 31)
        if (m < 3) y -= 1
        val dowSun0 = (y + y / 4 - y / 100 + y / 400 + sakamotoTable[m - 1] + d) % 7
        return (dowSun0 + 6) % 7
    }

    fun firstDayOfWeekMon0(monthStr: String): Int =
        dayOfWeekMon0("$monthStr-01")

    fun shiftMonth(monthStr: String, deltaMonths: Int): String {
        val totalMonths = parseYear(monthStr) * 12 + (parseMonth(monthStr) - 1) + deltaMonths
        val newYear = totalMonths.floorDiv(12)
        val newMonth = totalMonths.mod(12) + 1
        return formatMonth(newYear, newMonth)
    }

    fun clampedDateInMonth(monthStr: String, day: Int): String {
        val y = parseYear(monthStr)
        val m = parseMonth(monthStr)
        val maxD = daysInMonth(y, m)
        return formatDate(y, m, day.coerceIn(1, maxD))
    }

    fun defaultDateForMonth(monthStr: String): String {
        val today = todayStr()
        return if (monthOf(today) == monthStr) today else "$monthStr-01"
    }

    fun formatMonthLabel(monthStr: String): String {
        val y = parseYear(monthStr)
        val m = parseMonth(monthStr).coerceIn(1, 12)
        return "${monthNamesNom[m - 1]} $y"
    }

    fun formatDateHuman(date: String): String {
        refreshWallClockCacheIfNeeded()
        if (date == cachedTodayStr) return "Сегодня"
        if (date == cachedYesterdayStr) return "Вчера"
        val y = parseYear(date)
        val m = parseMonth(date).coerceIn(1, 12)
        val d = parseDay(date)
        return if (y == cachedYear) "$d ${monthNamesGen[m - 1]}" else "$d ${monthNamesShort[m - 1]} $y"
    }

    fun formatShortDayMonth(date: String): String {
        val m = parseMonth(date).coerceIn(1, 12)
        val d = parseDay(date)
        return "$d ${monthNamesShort[m - 1]}"
    }

    fun dateToEpochMillis(date: String): Long {
        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(parseYear(date), parseMonth(date) - 1, parseDay(date), 0, 0, 0)
        return cal.timeInMillis
    }

    fun epochMillisToDate(millis: Long): String {
        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = millis
        return formatDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Calculates shift duration in hours from "HH:mm" to "HH:mm".
     * Supports overnight shifts across midnight (e.g. 17:00 -> 01:00 = 8.0 hours).
     */
    fun calculateShiftHours(startTime: String, endTime: String): Double {
        val startMin = timeToMinutes(startTime)
        val endMin = timeToMinutes(endTime)
        val diffMin = if (endMin > startMin) {
            endMin - startMin
        } else if (endMin < startMin) {
            (24 * 60 - startMin) + endMin
        } else {
            0
        }
        return (diffMin / 60.0 * 100.0).roundToInt() / 100.0
    }

    fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return h * 60 + m
    }

    fun formatTime(hour: Int, minute: Int): String =
        String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun formatHours(hours: Double): String {
        val rounded = (hours * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) {
            "${rounded.toInt()} ч"
        } else {
            "${String.format(Locale.US, "%.1f", rounded).replace('.', ',')} ч"
        }
    }

    fun elapsedDaysInPeriod(startDate: String, endDate: String): Int {
        val today = todayStr()
        val effectiveEnd = if (today < endDate) today else endDate
        if (effectiveEnd < startDate) return 1
        val diffMillis = dateToEpochMillis(effectiveEnd) - dateToEpochMillis(startDate)
        return (((diffMillis + 43_200_000L) / 86_400_000L).toInt() + 1).coerceAtLeast(1)
    }
}
