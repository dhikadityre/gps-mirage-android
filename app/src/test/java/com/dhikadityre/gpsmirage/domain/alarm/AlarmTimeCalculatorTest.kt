package com.dhikadityre.gpsmirage.domain.alarm

import com.dhikadityre.gpsmirage.core.model.ReminderAlarm
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class AlarmTimeCalculatorTest {

    @Test
    fun `bitmask conversion matches days correctly`() {
        val days = setOf(1, 3, 5) // Senin, Rabu, Jumat
        val mask = ReminderAlarm.daysToBitmask(days)
        // 1 -> bit 0 (1), 3 -> bit 2 (4), 5 -> bit 4 (16) = 1 + 4 + 16 = 21
        assertEquals(21, mask)

        val restoredDays = ReminderAlarm.bitmaskToDays(mask)
        assertEquals(days, restoredDays)
    }

    @Test
    fun `repeatSummary formats common sets properly`() {
        assertEquals("Sekali saja", ReminderAlarm.formatDaysSummary(emptySet()))
        assertEquals("Setiap hari", ReminderAlarm.formatDaysSummary(setOf(1, 2, 3, 4, 5, 6, 7)))
        assertEquals("Hari kerja (Sen - Jum)", ReminderAlarm.formatDaysSummary(setOf(1, 2, 3, 4, 5)))
        assertEquals("Akhir pekan (Sab - Min)", ReminderAlarm.formatDaysSummary(setOf(6, 7)))
        assertEquals("Sen, Rab, Jum", ReminderAlarm.formatDaysSummary(setOf(1, 3, 5)))
    }

    @Test
    fun `one-time alarm schedules today if time in future`() {
        // Assume now is 10:00:00
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Alarm at 10:30 (empty repeat days -> one time)
        val trigger = AlarmTimeCalculator.calculateNextTriggerMillis(
            nowMillis = nowCal.timeInMillis,
            hour = 10,
            minute = 30,
            daysOfWeek = emptySet()
        )

        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(nowCal.get(Calendar.DAY_OF_YEAR), triggerCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(10, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, triggerCal.get(Calendar.MINUTE))
    }

    @Test
    fun `one-time alarm schedules tomorrow if time has already passed today`() {
        // Assume now is 11:00:00
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Alarm at 08:30 (already passed today)
        val trigger = AlarmTimeCalculator.calculateNextTriggerMillis(
            nowMillis = nowCal.timeInMillis,
            hour = 8,
            minute = 30,
            daysOfWeek = emptySet()
        )

        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger }
        val expectedDay = (nowCal.get(Calendar.DAY_OF_YEAR) % 365) + 1
        assertEquals(expectedDay, triggerCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(8, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, triggerCal.get(Calendar.MINUTE))
    }

    @Test
    fun `repeating alarm schedules today if matching day and future time`() {
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayIso = AlarmTimeCalculator.calendarDayToIso(nowCal.get(Calendar.DAY_OF_WEEK))

        val trigger = AlarmTimeCalculator.calculateNextTriggerMillis(
            nowMillis = nowCal.timeInMillis,
            hour = 14,
            minute = 0,
            daysOfWeek = setOf(todayIso)
        )

        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(nowCal.get(Calendar.DAY_OF_YEAR), triggerCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(14, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
    }

    @Test
    fun `repeating alarm advances to next week if today matching day but time has passed`() {
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayIso = AlarmTimeCalculator.calendarDayToIso(nowCal.get(Calendar.DAY_OF_WEEK))

        val trigger = AlarmTimeCalculator.calculateNextTriggerMillis(
            nowMillis = nowCal.timeInMillis,
            hour = 8,
            minute = 0,
            daysOfWeek = setOf(todayIso)
        )

        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger }
        // Difference should be 7 days
        val daysDiff = (triggerCal.timeInMillis - nowCal.timeInMillis) / (1000 * 60 * 60 * 24)
        assertTrue(daysDiff in 6..7)
        assertEquals(8, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
    }
}
