package com.coconut.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DnsScheduleTest {

    @Test
    fun testScheduleWindowInsideAndOutside() {
        val manager = DnsScheduleManagerFake()

        // 10:59 PM (22:59) -> Expected: Off (false)
        val cal1059PM = createCalendar(22, 59)
        assertFalse("10:59 PM must be outside schedule window", manager.isInsideScheduleWindow(23, 0, 6, 0, cal1059PM))

        // 11:00 PM (23:00) -> Expected: On (true)
        val cal1100PM = createCalendar(23, 0)
        assertTrue("11:00 PM must be inside schedule window", manager.isInsideScheduleWindow(23, 0, 6, 0, cal1100PM))

        // 11:30 PM (23:30) -> Expected: On (true)
        val cal1130PM = createCalendar(23, 30)
        assertTrue("11:30 PM must be inside schedule window", manager.isInsideScheduleWindow(23, 0, 6, 0, cal1130PM))

        // 12:00 AM midnight (00:00) -> Expected: On (true)
        val cal1200AM = createCalendar(0, 0)
        assertTrue("Midnight must be inside schedule window", manager.isInsideScheduleWindow(23, 0, 6, 0, cal1200AM))

        // 5:59 AM (05:59) -> Expected: On (true)
        val cal0559AM = createCalendar(5, 59)
        assertTrue("5:59 AM must be inside schedule window", manager.isInsideScheduleWindow(23, 0, 6, 0, cal0559AM))

        // 6:00 AM (06:00) -> Expected: Off (false)
        val cal0600AM = createCalendar(6, 0)
        assertFalse("6:00 AM must be outside schedule window", manager.isInsideScheduleWindow(23, 0, 6, 0, cal0600AM))

        // 12:00 PM noon (12:00) -> Expected: Off (false)
        val cal1200PM = createCalendar(12, 0)
        assertFalse("12:00 PM must be outside schedule window", manager.isInsideScheduleWindow(23, 0, 6, 0, cal1200PM))
    }

    @Test
    fun testCalculateNextBoundaryEpochMillis() {
        val manager = DnsScheduleManagerFake()

        // At 10:59 PM (outside window), next boundary must be 11:00 PM today
        val cal1059PM = createCalendar(22, 59)
        val nextFrom1059 = manager.calculateNextBoundaryEpochMillis(23, 0, 6, 0, cal1059PM)
        val targetCal = Calendar.getInstance(cal1059PM.timeZone).apply { timeInMillis = nextFrom1059 }
        assertEquals(23, targetCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, targetCal.get(Calendar.MINUTE))

        // At 11:30 PM (inside window), next boundary must be 6:00 AM tomorrow
        val cal1130PM = createCalendar(23, 30)
        val nextFrom1130 = manager.calculateNextBoundaryEpochMillis(23, 0, 6, 0, cal1130PM)
        val targetCalInside = Calendar.getInstance(cal1130PM.timeZone).apply { timeInMillis = nextFrom1130 }
        assertEquals(6, targetCalInside.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, targetCalInside.get(Calendar.MINUTE))
        assertTrue(nextFrom1130 > cal1130PM.timeInMillis)
    }

    private fun createCalendar(hour: Int, minute: Int): Calendar {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 8)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private class DnsScheduleManagerFake {
        fun isInsideScheduleWindow(
            startHour: Int,
            startMinute: Int,
            endHour: Int,
            endMinute: Int,
            calendar: Calendar
        ): Boolean {
            val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            val startMinutes = startHour * 60 + startMinute
            val endMinutes = endHour * 60 + endMinute

            return if (startMinutes > endMinutes) {
                currentMinutes >= startMinutes || currentMinutes < endMinutes
            } else {
                currentMinutes in startMinutes until endMinutes
            }
        }

        fun calculateNextBoundaryEpochMillis(
            startHour: Int,
            startMinute: Int,
            endHour: Int,
            endMinute: Int,
            nowCalendar: Calendar
        ): Long {
            val isInside = isInsideScheduleWindow(startHour, startMinute, endHour, endMinute, nowCalendar)
            val target = Calendar.getInstance(nowCalendar.timeZone).apply {
                timeInMillis = nowCalendar.timeInMillis
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (isInside) {
                target.set(Calendar.HOUR_OF_DAY, endHour)
                target.set(Calendar.MINUTE, endMinute)
                if (target.timeInMillis <= nowCalendar.timeInMillis) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else {
                target.set(Calendar.HOUR_OF_DAY, startHour)
                target.set(Calendar.MINUTE, startMinute)
                if (target.timeInMillis <= nowCalendar.timeInMillis) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return target.timeInMillis
        }
    }
}

