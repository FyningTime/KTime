package technology.iatlas.ktime.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import mu.KotlinLogging
import technology.iatlas.ktime.data.repositories.SettingsManager

val klogger = KotlinLogging.logger {}

/**
 * Settings tab of the application.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(database: org.jetbrains.exposed.sql.Database) {
    // Create SettingsManager and ViewModel
    val settingsRepository = remember { technology.iatlas.ktime.data.repositories.SqliteSettingRepository(database) }
    val settingsManager = remember { SettingsManager(settingsRepository) }
    val viewModel = remember { SettingsViewModel(settingsManager) }

    // State for UI
    var workHoursPerWeek by remember { mutableStateOf("40") }
    var selectedLocale by remember { mutableStateOf("en-US") }
    var dateFormat by remember { mutableStateOf("yyyy-MM-dd") }
    val availableLocales = listOf("en-US", "de-DE", "fr-FR", "es-ES", "it-IT")
    val availableDateFormats = listOf("yyyy-MM-dd", "dd.MM.yyyy", "MM/dd/yyyy", "dd/MM/yyyy")

    // Load settings when the composable is first created
    LaunchedEffect(Unit) {
        viewModel.loadSettings()

        workHoursPerWeek = viewModel.settings["workHoursPerWeek"] ?: "40"
        selectedLocale = viewModel.settings["locale"] ?: "en-US"
        dateFormat = viewModel.settings["dateFormat"] ?: "yyyy-MM-dd"
    }

    // Observe settings from ViewModel
    LaunchedEffect(viewModel.settings) {
        klogger.debug { "Observing settings changes: ${viewModel.settings["workHoursPerWeek"]}" }
        workHoursPerWeek = viewModel.settings["workHoursPerWeek"] ?: "40"
        selectedLocale = viewModel.settings["locale"] ?: "en-US"
        dateFormat = viewModel.settings["dateFormat"] ?: "yyyy-MM-dd"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = viewModel.settings["workHoursPerWeek"] ?: workHoursPerWeek,
            onValueChange = { viewModel.settings["workHoursPerWeek"] = it },
            label = { Text("Work Hours Per Week") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Application Locale",
            style = MaterialTheme.typography.bodyLarge
        )

        // Locale selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            availableLocales.forEach { locale ->
                Button(
                    onClick = { selectedLocale = locale },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedLocale == locale)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedLocale == locale)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(locale)
                }
            }
        }

        // Date Format Section
        Text(
            text = "Date Format",
            style = MaterialTheme.typography.bodyMedium
        )

        // Date Format selection
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableDateFormats.forEach { format ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    val now = LocalDate.parse("2023-01-01") // Example date
                    Text("$format (Example: $now)")
                    RadioButton(
                        selected = dateFormat == format,
                        onClick = { dateFormat = format }
                    )
                }
            }
        }

        // Date Format Preview
        val today = LocalDate.parse("2023-01-01") // Example date
        val formattedDate = try {
            viewModel.formatDate(today)
        } catch (e: Exception) {
            "Invalid format"
        }

        Text(
            text = "Preview: $formattedDate",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    // Reset to defaults
                    workHoursPerWeek = "40"
                    selectedLocale = "de-DE"
                    dateFormat = "yyyy-MM-dd"

                    // Save settings
                    viewModel.saveSettings(
                        workHoursPerWeek = workHoursPerWeek,
                        locale = selectedLocale,
                        dateFormat = dateFormat
                    )
                },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Reset")
            }

            Button(
                onClick = {
                    // Save settings
                    viewModel.saveSettings(
                        workHoursPerWeek = viewModel.settings["workHoursPerWeek"] ?: "40",
                        locale = selectedLocale,
                        dateFormat = dateFormat
                    )
                }
            ) {
                Text("Save")
            }
        }
    }
}
