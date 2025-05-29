import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.exposed.sql.Database
import technology.iatlas.ktime.database.DatabaseManager
import technology.iatlas.ktime.ui.MainScreen

/**
 * Main application composable.
 */
@Composable
fun App(database: Database) {
    MaterialTheme {
        Surface {
            MainScreen(database)
        }
    }
}

/**
 * Main entry point of the application.
 */
fun main() {
    // Initialize the database
    val databaseManager = DatabaseManager()
    databaseManager.init()

    // Get the database instance
    val database = databaseManager.getDatabase()

    // Start the application
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "KTime - Work Time Tracker"
        ) {
            App(database)
        }
    }
}
