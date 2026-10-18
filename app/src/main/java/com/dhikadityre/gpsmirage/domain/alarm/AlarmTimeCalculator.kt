package com.dhikadityre.gpsmirage.domain.alarm

import java.util.Calendar

object AlarmTimeCalculator {

    /**
     * Menghitung epoch millis pemicu berikutnya untuk alarm.
     * @param nowMillis Waktu acuan sekarang dalam epoch millis
     * @param hour Jam alarm (0..23)
     * @param minute Menit alarm (0..59)
     * @param daysOfWeek Set hari berulang (1 = Sen, ..., 7 = Min). Kosong jika sekali saja.
     * @return Epoch millis trigger berikutnya
     */
    fun calculateNextTriggerMillis(
        nowMillis: Long,
        hour: Int,
        minute: Int,
        daysOfWeek: Set<Int>
    ): Long {
        // Jika tidak berulang (Sekali saja)
        if (daysOfWeek.isEmpty()) {
            val targetCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // Jika jam target sudah lewat hari ini (dengan buffer 2 detik), jadwalkan besok
            if (targetCal.timeInMillis <= nowMillis + 2000L) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return targetCal.timeInMillis
        }

        // Jika berulang pada hari tertentu (1 = Sen, ..., 7 = Min)
        for (dayOffset in 0..7) {
            val candidateCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val isoDay = calendarDayToIso(candidateCal.get(Calendar.DAY_OF_WEEK))
            if (daysOfWeek.contains(isoDay)) {
                if (dayOffset > 0 || candidateCal.timeInMillis > nowMillis + 2000L) {
                    return candidateCal.timeInMillis
                }
            }
        }

        // Fallback: 7 hari dari sekarang jika tidak ada yang cocok
        val fallbackCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return fallbackCal.timeInMillis
    }

    /**
     * Konversi hari Calendar Java (Sunday=1..Saturday=7) ke ISO-8601 (Monday=1..Sunday=7).
     */
    fun calendarDayToIso(calDay: Int): Int = when (calDay) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
}
