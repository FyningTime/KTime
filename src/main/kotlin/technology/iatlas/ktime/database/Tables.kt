package technology.iatlas.ktime.database

import org.jetbrains.exposed.sql.Table

/**
 * Database tables for the KTime application.
 */

/**
 * CurrentDays table for storing information about each day.
 */
object CurrentDays : Table() {
    val id = integer("id").autoIncrement()
    val date = varchar("date", 10) // Format: YYYY-MM-DD
    val isWorkDay = bool("is_work_day")
    val targetHours = float("target_hours")
    val notes = varchar("notes", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

/**
 * WorkTimes table for storing work time entries.
 */
object WorkTimes : Table() {
    val id = integer("id").autoIncrement()
    val currentDayId = integer("current_day_id").references(CurrentDays.id)
    val startTime = varchar("start_time", 5) // Format: HH:MM
    val endTime = varchar("end_time", 5) // Format: HH:MM
    val breakDuration = integer("break_duration") // In minutes
    val notes = varchar("notes", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

/**
 * Vacations table for storing vacation entries.
 */
object Vacations : Table() {
    val id = integer("id").autoIncrement()
    val currentDayId = integer("current_day_id").references(CurrentDays.id)
    val endDate = varchar("end_date", 10).nullable() // Format: YYYY-MM-DD
    val type = varchar("type", 50)
    val notes = varchar("notes", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

/**
 * Settings table for storing application settings.
 */
object Settings : Table() {
    val id = integer("id").autoIncrement()
    val key = varchar("key", 50)
    val value = varchar("value", 255)

    override val primaryKey = PrimaryKey(id)

    // Add unique constraint to prevent duplicate keys
    init {
        uniqueIndex(key)
    }
}