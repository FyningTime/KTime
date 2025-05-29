package technology.iatlas.ktime.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import technology.iatlas.ktime.data.Vacation
import technology.iatlas.ktime.data.VacationType
import technology.iatlas.ktime.database.CurrentDays
import technology.iatlas.ktime.database.Vacations

/**
 * Repository interface for vacations.
 */
interface VacationRepository {
    /**
     * Gets a vacation by id.
     */
    suspend fun getVacationById(id: Int): Vacation?

    /**
     * Gets all vacations.
     */
    suspend fun getAllVacations(): List<Vacation>

    /**
     * Gets all vacations with date information.
     */
    suspend fun getAllVacationsWithDate(): List<Pair<Vacation, LocalDate>>

    /**
     * Saves a vacation.
     */
    suspend fun saveVacation(vacation: Vacation): Vacation

    /**
     * Deletes a vacation by id.
     */
    suspend fun deleteVacation(id: Int)
}

/**
 * SQLite implementation of the VacationRepository.
 */
class SqliteVacationRepository(private val database: Database) : VacationRepository {
    override suspend fun getVacationById(id: Int): Vacation? = withContext(Dispatchers.IO) {
        transaction(database) {
            Vacations.selectAll().where { Vacations.id eq id }
                .map {
                    Vacation(
                        id = it[Vacations.id],
                        currentDayId = it[Vacations.currentDayId],
                        endDate = it[Vacations.endDate]?.let { date -> LocalDate.parse(date) },
                        type = VacationType.valueOf(it[Vacations.type]),
                        notes = it[Vacations.notes]
                    )
                }
                .singleOrNull()
        }
    }

    override suspend fun getAllVacations(): List<Vacation> = withContext(Dispatchers.IO) {
        transaction(database) {
            Vacations.selectAll()
                .map {
                    Vacation(
                        id = it[Vacations.id],
                        currentDayId = it[Vacations.currentDayId],
                        endDate = it[Vacations.endDate]?.let { date -> LocalDate.parse(date) },
                        type = VacationType.valueOf(it[Vacations.type]),
                        notes = it[Vacations.notes]
                    )
                }
                .toList()
        }
    }

    override suspend fun getAllVacationsWithDate(): List<Pair<Vacation, LocalDate>> = withContext(Dispatchers.IO) {
        transaction(database) {
            (Vacations innerJoin CurrentDays)
                .selectAll().where { Vacations.currentDayId eq CurrentDays.id }
                .map {
                    val vacation = Vacation(
                        id = it[Vacations.id],
                        currentDayId = it[Vacations.currentDayId],
                        endDate = it[Vacations.endDate]?.let { date -> LocalDate.parse(date) },
                        type = VacationType.valueOf(it[Vacations.type]),
                        notes = it[Vacations.notes]
                    )
                    val date = LocalDate.parse(it[CurrentDays.date])
                    Pair(vacation, date)
                }
                .toList()
        }
    }

    override suspend fun saveVacation(vacation: Vacation): Vacation = withContext(Dispatchers.IO) {
        transaction(database) {
            if (vacation.id != null) {
                // Update existing vacation
                Vacations.update({ Vacations.id eq vacation.id }) {
                    it[currentDayId] = vacation.currentDayId
                    it[endDate] = vacation.endDate?.toString()
                    it[type] = vacation.type.name
                    it[notes] = vacation.notes
                }
                vacation
            } else {
                // Insert new vacation
                val id = Vacations.insert {
                    it[currentDayId] = vacation.currentDayId
                    it[endDate] = vacation.endDate?.toString()
                    it[type] = vacation.type.name
                    it[notes] = vacation.notes
                } get Vacations.id

                vacation.copy(id = id)
            }
        }
    }

    override suspend fun deleteVacation(id: Int) {
        withContext(Dispatchers.IO) {
            transaction(database) {
                // Use direct SQL for deletion
                val query = "DELETE FROM ${Vacations.tableName} WHERE ${Vacations.id.name} = $id"
                exec(query)
            }
        }
    }
}