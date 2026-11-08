package com.pepperonas.brutus.util

import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * The Material date picker speaks in UTC midnights; the alarms live in the phone's time zone.
 * Converting through the calendar date (year, month, day) instead of adding offsets keeps the day
 * right on both sides of a DST change and in time zones west of UTC.
 */
object VacationDates {

    private val UTC = TimeZone.getTimeZone("UTC")

    /** Today, as the picker's UTC midnight — the earliest day that can be chosen. */
    fun todayUtcMidnight(now: Long = System.currentTimeMillis()): Long = toPickerMillis(now)

    /** The local calendar day of [localMillis] as the picker's UTC midnight. */
    fun toPickerMillis(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(UTC).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** The picker's chosen day as local noon of that calendar day — safely inside the day. */
    fun fromPickerMillis(utcMidnight: Long): Long {
        val utc = Calendar.getInstance(UTC).apply { timeInMillis = utcMidnight }
        return Calendar.getInstance().apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0)
        }.timeInMillis
    }

    fun format(localMillis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(localMillis))
}
