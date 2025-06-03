package technology.iatlas.ktime.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

/**
 * Manages the database connection and initialization.
 */
class DatabaseManager {
    private lateinit var database: Database

    /**
     * Initializes the database connection and creates the tables if they don't exist.
     */
    fun init(dbFilePath: String = "ktime.db") {
        // Ensure the directory exists
        val dbFile = File(dbFilePath)
        dbFile.parentFile?.mkdirs()

        // Connect to the database
        database = Database.connect(
            url = "jdbc:sqlite:$dbFilePath",
            driver = "org.sqlite.JDBC"
        )

        // Create tables if they don't exist
        transaction(database) {
            SchemaUtils.create(
                CurrentDays,
                WorkTimes,
                Vacations,
                Settings
            )
        }
    }

    /**
     * Gets the database instance.
     */
    fun getDatabase(): Database {
        if (!::database.isInitialized) {
            this.init()
        }
        return database
    }
}