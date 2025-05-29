package technology.iatlas.ktime.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import technology.iatlas.ktime.data.WorkTime
import technology.iatlas.ktime.data.WorkTimeWithDay
import technology.iatlas.ktime.database.CurrentDays
import technology.iatlas.ktime.database.WorkTimes

/**
 * Repository interface for work times.
 */
interface WorkTimeRepository {
    /**
     * Gets a work time by id.
     */
    suspend fun getWorkTimeById(id: Int): WorkTime?

    /**
     * Gets all work times for a current day.
     */
    suspend fun getWorkTimesForDay(currentDayId: Int): List<WorkTime>

    /**
     * Gets all work times with day information.
     */
    suspend fun getAllWorkTimesWithDay(): List<WorkTimeWithDay>

    /**
     * Saves a work time.
     */
    suspend fun saveWorkTime(workTime: WorkTime): WorkTime

    /**
     * Deletes a work time by id.
     */
    suspend fun deleteWorkTime(id: Int)
}

/**
 * SQLite implementation of the WorkTimeRepository.
 */
class SqliteWorkTimeRepository(private val database: Database) : WorkTimeRepository {
    override suspend fun getWorkTimeById(id: Int): WorkTime? = withContext(Dispatchers.IO) {
        transaction(database) {
            WorkTimes.selectAll().where { WorkTimes.id eq id }
                .map {
                    WorkTime(
                        id = it[WorkTimes.id],
                        currentDayId = it[WorkTimes.currentDayId],
                        startTime = LocalTime.parse(it[WorkTimes.startTime]),
                        endTime = LocalTime.parse(it[WorkTimes.endTime]),
                        breakDuration = it[WorkTimes.breakDuration],
                        notes = it[WorkTimes.notes]
                    )
                }
                .singleOrNull()
        }
    }

    override suspend fun getWorkTimesForDay(currentDayId: Int): List<WorkTime> = withContext(Dispatchers.IO) {
        transaction(database) {
            WorkTimes.selectAll().where { WorkTimes.currentDayId eq currentDayId }
                .map {
                    WorkTime(
                        id = it[WorkTimes.id],
                        currentDayId = it[WorkTimes.currentDayId],
                        startTime = LocalTime.parse(it[WorkTimes.startTime]),
                        endTime = LocalTime.parse(it[WorkTimes.endTime]),
                        breakDuration = it[WorkTimes.breakDuration],
                        notes = it[WorkTimes.notes]
                    )
                }
                .toList()
        }
    }

    override suspend fun getAllWorkTimesWithDay(): List<WorkTimeWithDay> = withContext(Dispatchers.IO) {
        transaction(database) {
            (WorkTimes innerJoin CurrentDays)
                .selectAll().where { WorkTimes.currentDayId eq CurrentDays.id }
                .map {
                    WorkTimeWithDay(
                        id = it[WorkTimes.id],
                        date = LocalDate.parse(it[CurrentDays.date]),
                        startTime = LocalTime.parse(it[WorkTimes.startTime]),
                        endTime = LocalTime.parse(it[WorkTimes.endTime]),
                        breakDuration = it[WorkTimes.breakDuration],
                        duration = calculateDuration(
                            LocalTime.parse(it[WorkTimes.startTime]),
                            LocalTime.parse(it[WorkTimes.endTime]),
                            it[WorkTimes.breakDuration]
                        ),
                        notes = it[WorkTimes.notes]
                    )
                }
                .toList()
        }
    }

    /**
     * Formats a LocalTime to HH:MM format
     */
    private fun formatTimeHHMM(time: LocalTime): String {
        return String.format("%02d:%02d", time.hour, time.minute)
    }

    override suspend fun saveWorkTime(workTime: WorkTime): WorkTime = withContext(Dispatchers.IO) {
        transaction(database) {
            if (workTime.id != null) {
                // Update existing work time
                WorkTimes.update({ WorkTimes.id eq workTime.id }) {
                    it[currentDayId] = workTime.currentDayId
                    it[startTime] = formatTimeHHMM(workTime.startTime)
                    it[endTime] = formatTimeHHMM(workTime.endTime)
                    it[breakDuration] = workTime.breakDuration
                    it[notes] = workTime.notes
                }
                workTime
            } else {
                // Insert new work time
                val id = WorkTimes.insert {
                    it[currentDayId] = workTime.currentDayId
                    it[startTime] = formatTimeHHMM(workTime.startTime)
                    it[endTime] = formatTimeHHMM(workTime.endTime)
                    it[breakDuration] = workTime.breakDuration
                    it[notes] = workTime.notes
                } get WorkTimes.id

                workTime.copy(id = id)
            }
        }
    }

    override suspend fun deleteWorkTime(id: Int) {
        withContext(Dispatchers.IO) {
            transaction(database) {
                // Use direct SQL for deletion
                val query = "DELETE FROM ${WorkTimes.tableName} WHERE ${WorkTimes.id.name} = $id"
                exec(query)
            }
        }
    }

    /**
     * Calculates the duration of work in hours.
     */
    private fun calculateDuration(startTime: LocalTime, endTime: LocalTime, breakDuration: Int): Float {
        val startMinutes = startTime.hour * 60 + startTime.minute
        val endMinutes = endTime.hour * 60 + endTime.minute
        val totalMinutes = endMinutes - startMinutes - breakDuration
        return totalMinutes / 60.0f
    }
}
