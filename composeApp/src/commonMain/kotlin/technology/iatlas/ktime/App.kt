package technology.iatlas.ktime

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import org.jetbrains.exposed.sql.Database
import technology.iatlas.ktime.ui.MainScreen

/**
 * Main application composable.
 */
@Composable
fun App(database: Database) { // FIXME - will not work with KMP probably
    MaterialTheme {
        Surface {
            MainScreen(database)
        }
    }
}
