package technology.iatlas.ktime.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import technology.iatlas.ktime.data.CurrentDay
import technology.iatlas.ktime.database.CurrentDays

/**
 * Repository interface for current days.
 */
interface CurrentDayRepository {
    /**
     * Gets a current day by date.
     */
    suspend fun getCurrentDayByDate(date: LocalDate): CurrentDay?

    /**
     * Gets all current days.
     */
    suspend fun getAllCurrentDays(): List<CurrentDay>

    /**
     * Saves a current day.
     */
    suspend fun saveCurrentDay(currentDay: CurrentDay): CurrentDay

    /**
     * Deletes a current day by id.
     */
    suspend fun deleteCurrentDay(id: Int)
}

/**
 * SQLite implementation of the CurrentDayRepository.
 */
class SqliteCurrentDayRepository(private val database: Database) : CurrentDayRepository {
    override suspend fun getCurrentDayByDate(date: LocalDate): CurrentDay? = withContext(Dispatchers.IO) {
        transaction(database) {
            CurrentDays.selectAll().where { CurrentDays.date eq date.toString() }
                .map {
                    CurrentDay(
                        id = it[CurrentDays.id],
                        date = LocalDate.parse(it[CurrentDays.date]),
                        isWorkDay = it[CurrentDays.isWorkDay],
                        targetHours = it[CurrentDays.targetHours],
                        notes = it[CurrentDays.notes]
                    )
                }
                .singleOrNull()
        }
    }

    override suspend fun getAllCurrentDays(): List<CurrentDay> = withContext(Dispatchers.IO) {
        transaction(database) {
            CurrentDays.selectAll()
                .map {
                    CurrentDay(
                        id = it[CurrentDays.id],
                        date = LocalDate.parse(it[CurrentDays.date]),
                        isWorkDay = it[CurrentDays.isWorkDay],
                        targetHours = it[CurrentDays.targetHours],
                        notes = it[CurrentDays.notes]
                    )
                }
                .toList()
        }
    }

    override suspend fun saveCurrentDay(currentDay: CurrentDay): CurrentDay = withContext(Dispatchers.IO) {
        transaction(database) {
            if (currentDay.id != null) {
                // Update existing current day
                CurrentDays.update({ CurrentDays.id eq currentDay.id }) {
                    it[date] = currentDay.date.toString()
                    it[isWorkDay] = currentDay.isWorkDay
                    it[targetHours] = currentDay.targetHours
                    it[notes] = currentDay.notes
                }
                currentDay
            } else {
                // Insert new current day
                val id = CurrentDays.insert {
                    it[date] = currentDay.date.toString()
                    it[isWorkDay] = currentDay.isWorkDay
                    it[targetHours] = currentDay.targetHours
                    it[notes] = currentDay.notes
                } get CurrentDays.id

                currentDay.copy(id = id)
            }
        }
    }

    override suspend fun deleteCurrentDay(id: Int) {
        withContext(Dispatchers.IO) {
            transaction(database) {
                // Use direct SQL for deletion
                val query = "DELETE FROM ${CurrentDays.tableName} WHERE ${CurrentDays.id.name} = $id"
                exec(query)
            }
        }
    }
}
