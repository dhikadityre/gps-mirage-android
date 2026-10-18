package com.dhikadityre.gpsmirage.core.model

import java.util.Locale

data class ReminderAlarm(
    val id: Long = 0L,
    val label: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<Int> = emptySet(), // 1 = Senin, ..., 7 = Minggu (ISO-8601)
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

    val repeatSummary: String
        get() = formatDaysSummary(daysOfWeek)

    companion object {
        fun formatDaysSummary(days: Set<Int>): String {
            if (days.isEmpty()) return "Sekali saja"
            if (days.size == 7) return "Setiap hari"
            if (days == setOf(1, 2, 3, 4, 5)) return "Hari kerja (Sen - Jum)"
            if (days == setOf(6, 7)) return "Akhir pekan (Sab - Min)"

            val dayNames = mapOf(
                1 to "Sen",
                2 to "Sel",
                3 to "Rab",
                4 to "Kam",
                5 to "Jum",
                6 to "Sab",
                7 to "Min"
            )
            return days.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
        }

        fun daysToBitmask(days: Set<Int>): Int {
            var mask = 0
            for (day in days) {
                if (day in 1..7) {
                    mask = mask or (1 shl (day - 1))
                }
            }
            return mask
        }

        fun bitmaskToDays(mask: Int): Set<Int> {
            val set = mutableSetOf<Int>()
            for (day in 1..7) {
                if ((mask and (1 shl (day - 1))) != 0) {
                    set.add(day)
                }
            }
            return set
        }
    }
}
