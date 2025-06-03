package technology.iatlas.ktime.ui.migration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import technology.iatlas.ktime.database.migration.DatabaseMigrationManager
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * ViewModel for the migration tab.
 */
class MigrationViewModel(private val database: Database) {
    // UI state
    var selectedFilePath by mutableStateOf("")
    var errorMessage by mutableStateOf("")
    var isMigrating by mutableStateOf(false)
    var migrationComplete by mutableStateOf(false)
    var previewData by mutableStateOf<DatabaseMigrationManager.MigrationPreview?>(null)

    private val migrationManager = DatabaseMigrationManager()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        migrationManager.setTargetDatabase(database)
    }

    /**
     * Opens a file chooser dialog to select the old database file.
     */
    fun selectFile() {
        val fileChooser = JFileChooser() // FIXME - Won't work with KMP, it's only desktop right now!
        fileChooser.dialogTitle = "Select FyningTime Database File"
        fileChooser.fileFilter = FileNameExtensionFilter("SQLite Database Files", "db", "sqlite", "sqlite3")

        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFilePath = fileChooser.selectedFile.absolutePath
            errorMessage = ""
            previewData = null
            migrationComplete = false
        }
    }

    /**
     * Analyzes the selected database file and prepares a preview of the data to be migrated.
     */
    fun analyzeDatabase() {
        if (selectedFilePath.isEmpty()) {
            errorMessage = "Please select a database file first."
            return
        }

        val file = File(selectedFilePath)
        if (!file.exists()) {
            errorMessage = "The selected file does not exist."
            return
        }

        isMigrating = true
        errorMessage = ""
        previewData = null
        migrationComplete = false

        coroutineScope.launch {
            try {
                val connected = migrationManager.connectToOldDatabase(selectedFilePath)
                if (!connected) {
                    errorMessage = "Failed to connect to the database. Make sure it's a valid SQLite database."
                    isMigrating = false
                    return@launch
                }

                val preview = migrationManager.analyzeDatabase()
                if (preview.workdays.isEmpty() && preview.worktimes.isEmpty() && preview.vacations.isEmpty()) {
                    errorMessage = "No data found in the database or the database format is not compatible."
                    isMigrating = false
                    return@launch
                }

                previewData = preview
                isMigrating = false
            } catch (e: Exception) {
                errorMessage = "Error analyzing database: ${e.message}"
                isMigrating = false
            }
        }
    }

    /**
     * Executes the migration based on the preview data.
     */
    fun executeMigration() {
        if (previewData == null) {
            errorMessage = "Please analyze the database first."
            return
        }

        isMigrating = true
        errorMessage = ""
        migrationComplete = false

        coroutineScope.launch {
            try {
                // Generate SQL statements for the migration
                val sqlStatements = migrationManager.generateMigrationSql(previewData!!)

                // Save the SQL statements to a file
                val sqlFilePath = "fyningtime2ktime.migration.sql"
                File(sqlFilePath).writeText(sqlStatements)

                // Show success message
                migrationComplete = true
                errorMessage =
                    "Migration SQL generated and saved to $sqlFilePath. You can execute this SQL file manually."

                isMigrating = false
            } catch (e: Exception) {
                errorMessage = "Error executing migration: ${e.message}"
                isMigrating = false
            } finally {
                migrationManager.closeConnection()
            }
        }
    }

}
