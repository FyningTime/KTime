package technology.iatlas.ktime.ui.migration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.exposed.sql.Database
import technology.iatlas.ktime.database.migration.DatabaseMigrationManager

/**
 * Tab for database migration from FyningTime to KTime.
 */
@Composable
fun MigrationTab(database: Database) {
    val viewModel = remember { MigrationViewModel(database) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Database Migration",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "This tool allows you to migrate data from the old FyningTime SQLite database to KTime.",
            style = MaterialTheme.typography.bodyLarge
        )

        // File selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = viewModel.selectedFilePath,
                onValueChange = { viewModel.selectedFilePath = it },
                label = { Text("Database File Path") },
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { viewModel.selectFile() },
                enabled = !viewModel.isMigrating
            ) {
                Text("Browse")
            }
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.analyzeDatabase() },
                enabled = viewModel.selectedFilePath.isNotEmpty() && !viewModel.isMigrating
            ) {
                Text("Preview Migration")
            }

            Button(
                onClick = { viewModel.executeMigration() },
                enabled = viewModel.previewData != null && !viewModel.isMigrating
            ) {
                Text("Execute Migration")
            }
        }

        // Status and error messages
        if (viewModel.errorMessage.isNotEmpty()) {
            Text(
                text = viewModel.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (viewModel.isMigrating) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (viewModel.migrationComplete) {
            Text(
                text = "Migration completed successfully!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Preview data
        viewModel.previewData?.let { preview ->
            Text(
                text = "Preview of data to be migrated:",
                style = MaterialTheme.typography.titleLarge
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Workdays: ${preview.workdays.size}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(preview.workdays.take(5)) { workday ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Date: ${workday.date}")
                            Text("Target Hours: ${workday.targetHours}")
                            Text("Break Minutes: ${workday.breakMinutes}")
                        }
                    }
                }

                item {
                    Text(
                        text = "Work Times: ${preview.worktimes.size}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(preview.worktimes.take(5)) { worktime ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Type: ${worktime.type}")
                            Text("Timestamp: ${worktime.timestamp}")
                            Text("Workday ID: ${worktime.workdayId}")
                        }
                    }
                }

                item {
                    Text(
                        text = "Vacations: ${preview.vacations.size}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(preview.vacations.take(5)) { vacation ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Start Date: ${vacation.startDate}")
                            Text("End Date: ${vacation.endDate}")
                        }
                    }
                }

                item {
                    if (preview.workdays.size > 5 || preview.worktimes.size > 5 || preview.vacations.size > 5) {
                        Text(
                            text = "... and more (showing only first 5 items of each type)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
