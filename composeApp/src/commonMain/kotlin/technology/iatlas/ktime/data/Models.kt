package technology.iatlas.ktime.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Data models for the KTime application.
 */

/**
 * Represents a day in the calendar.
 */
data class CurrentDay(
    val id: Int? = null,
    val date: LocalDate,
    val isWorkDay: Boolean,
    val targetHours: Float,
    val notes: String? = null
)

/**
 * Represents a work time entry.
 */
data class WorkTime(
    val id: Int? = null,
    val currentDayId: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val breakDuration: Int, // In minutes
    val notes: String? = null
) {
    /**
     * Calculates the duration of work in hours.
     */
    val duration: Float
        get() {
            val startMinutes = startTime.hour * 60 + startTime.minute
            val endMinutes = endTime.hour * 60 + endTime.minute
            val totalMinutes = endMinutes - startMinutes - breakDuration
            return totalMinutes / 60.0f
        }
}

/**
 * Types of vacation.
 */
enum class VacationType {
    VACATION, SICK_LEAVE, BUSINESS_TRIP, OTHER
}

/**
 * Represents a vacation entry.
 */
data class Vacation(
    val id: Int? = null,
    val currentDayId: Int,
    val endDate: LocalDate? = null,
    val type: VacationType,
    val notes: String? = null
)

/**
 * Represents a setting entry.
 */
data class Setting(
    val id: Int? = null,
    val key: String,
    val value: String
)

/**
 * Combined model for work time with day information.
 */
data class WorkTimeWithDay(
    val id: Int,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val breakDuration: Int,
    val duration: Float,
    val notes: String?
)