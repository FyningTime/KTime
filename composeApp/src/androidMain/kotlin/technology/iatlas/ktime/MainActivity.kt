package technology.iatlas.ktime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import technology.iatlas.ktime.database.DatabaseManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize the database
        val databaseManager = DatabaseManager()
        databaseManager.init()

        // Get the database instance
        val database = databaseManager.getDatabase()

        setContent {
            App(database)
        }
    }
}
