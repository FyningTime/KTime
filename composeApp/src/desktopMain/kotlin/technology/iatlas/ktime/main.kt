package technology.iatlas.ktime

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import technology.iatlas.ktime.database.DatabaseManager

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