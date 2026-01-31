package com.stockapp.core.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Utility for Korean stock market trading day calculations.
 * Handles date format conversion, trading day detection, and missing date analysis.
 */
object TradingDayUtil {

    // ========== Date Formats ==========

    /** API date format: YYYYMMDD */
    private val API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")

    /** DB date format: YYYY-MM-DD */
    private val DB_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ========== Korean Market Holidays ==========

    /**
     * Recurring holidays (fixed dates every year).
     * Format: "MM-dd"
     */
    private val RECURRING_HOLIDAYS = setOf(
        "01-01",  // New Year's Day (신정)
        "03-01",  // Independence Movement Day (삼일절)
        "05-05",  // Children's Day (어린이날)
        "06-06",  // Memorial Day (현충일)
        "08-15",  // Liberation Day (광복절)
        "10-03",  // National Foundation Day (개천절)
        "10-09",  // Hangul Day (한글날)
        "12-25",  // Christmas (성탄절)
        "12-31"   // Year-end closing (연말휴장)
    )

    /**
     * Lunar holidays for 2025.
     * These dates change every year based on lunar calendar.
     * Format: "MM-dd"
     */
    private val LUNAR_HOLIDAYS_2025 = setOf(
        "01-28", "01-29", "01-30",  // Seollal (설날)
        "05-05",                     // Buddha's Birthday (석가탄신일) - same as Children's Day in 2025
        "10-05", "10-06", "10-07"   // Chuseok (추석)
    )

    /**
     * Lunar holidays for 2026.
     * Format: "MM-dd"
     */
    private val LUNAR_HOLIDAYS_2026 = setOf(
        "02-16", "02-17", "02-18",  // Seollal (설날)
        "05-24",                     // Buddha's Birthday (석가탄신일)
        "09-24", "09-25", "09-26"   // Chuseok (추석)
    )

    /**
     * Special market closure dates (election days, temporary closures, etc.).
     * Format: "YYYY-MM-dd"
     */
    private val SPECIAL_CLOSURES = setOf(
        "2025-06-04",  // 지방선거 (example)
        "2026-03-09"   // 대통령선거 (example)
    )

    // ========== Date Format Conversion ==========

    /**
     * Parse API date format (YYYYMMDD) to LocalDate.
     *
     * @param apiDate Date string in YYYYMMDD format
     * @return LocalDate or null if parsing fails
     */
    fun parseApiDate(apiDate: String?): LocalDate? {
        if (apiDate.isNullOrBlank() || apiDate.length != 8) return null
        return try {
            LocalDate.parse(apiDate, API_DATE_FORMAT)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert LocalDate to DB date format (YYYY-MM-DD).
     *
     * @param date LocalDate to convert
     * @return Date string in YYYY-MM-DD format
     */
    fun toDbFormat(date: LocalDate): String = date.format(DB_DATE_FORMAT)

    /**
     * Convert API date format (YYYYMMDD) to DB date format (YYYY-MM-DD).
     *
     * @param apiDate Date string in YYYYMMDD format
     * @return Date string in YYYY-MM-DD format, or null if conversion fails
     */
    fun apiToDbFormat(apiDate: String?): String? {
        return parseApiDate(apiDate)?.let { toDbFormat(it) }
    }

    /**
     * Parse DB date format (YYYY-MM-DD) to LocalDate.
     *
     * @param dbDate Date string in YYYY-MM-DD format
     * @return LocalDate or null if parsing fails
     */
    fun parseDbDate(dbDate: String?): LocalDate? {
        if (dbDate.isNullOrBlank()) return null
        return try {
            LocalDate.parse(dbDate, DB_DATE_FORMAT)
        } catch (e: Exception) {
            null
        }
    }

    // ========== Trading Day Detection ==========

    /**
     * Check if a date is a Korean stock market trading day.
     * Returns false for weekends and holidays.
     *
     * @param date Date to check
     * @return true if it's a trading day, false otherwise
     */
    fun isTradingDay(date: LocalDate): Boolean {
        // Weekend check
        if (date.dayOfWeek == DayOfWeek.SATURDAY ||
            date.dayOfWeek == DayOfWeek.SUNDAY
        ) {
            return false
        }

        // Recurring holiday check
        val monthDay = String.format("%02d-%02d", date.monthValue, date.dayOfMonth)
        if (RECURRING_HOLIDAYS.contains(monthDay)) return false

        // Lunar holiday check (year-specific)
        val lunarHolidays = when (date.year) {
            2025 -> LUNAR_HOLIDAYS_2025
            2026 -> LUNAR_HOLIDAYS_2026
            else -> emptySet()
        }
        if (lunarHolidays.contains(monthDay)) return false

        // Special closure check
        val fullDate = toDbFormat(date)
        if (SPECIAL_CLOSURES.contains(fullDate)) return false

        return true
    }

    /**
     * Get the previous trading day from the given date.
     *
     * @param date Starting date
     * @return Previous trading day (may be same day if date is trading day at market open)
     */
    fun getPreviousTradingDay(date: LocalDate): LocalDate {
        var current = date.minusDays(1)
        while (!isTradingDay(current)) {
            current = current.minusDays(1)
        }
        return current
    }

    /**
     * Get the most recent trading day (today if trading day, otherwise previous).
     *
     * @param date Reference date (typically today)
     * @return Most recent trading day
     */
    fun getMostRecentTradingDay(date: LocalDate = LocalDate.now()): LocalDate {
        var current = date
        while (!isTradingDay(current)) {
            current = current.minusDays(1)
        }
        return current
    }

    // ========== Missing Date Analysis ==========

    /**
     * Get all trading days between two dates (inclusive).
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of trading days in chronological order
     */
    fun getTradingDaysBetween(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        if (startDate.isAfter(endDate)) return emptyList()

        val tradingDays = mutableListOf<LocalDate>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            if (isTradingDay(current)) {
                tradingDays.add(current)
            }
            current = current.plusDays(1)
        }
        return tradingDays
    }

    /**
     * Find missing trading days given a set of collected dates.
     *
     * @param collectedDates Set of collected dates in DB format (YYYY-MM-DD)
     * @param startDate Start of analysis range
     * @param endDate End of analysis range
     * @return List of missing trading days in DB format
     */
    fun findMissingTradingDays(
        collectedDates: Set<String>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<String> {
        val expectedTradingDays = getTradingDaysBetween(startDate, endDate)
        return expectedTradingDays
            .filter { day -> !collectedDates.contains(toDbFormat(day)) }
            .map { toDbFormat(it) }
    }

    /**
     * Calculate collection coverage statistics.
     *
     * @param collectedDates Set of collected dates in DB format (YYYY-MM-DD)
     * @param startDate Start of analysis range
     * @param endDate End of analysis range
     * @return Pair of (collected count, total trading days count)
     */
    fun calculateCoverage(
        collectedDates: Set<String>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Pair<Int, Int> {
        val expectedTradingDays = getTradingDaysBetween(startDate, endDate)
        val collectedCount = expectedTradingDays.count { day ->
            collectedDates.contains(toDbFormat(day))
        }
        return Pair(collectedCount, expectedTradingDays.size)
    }
}
