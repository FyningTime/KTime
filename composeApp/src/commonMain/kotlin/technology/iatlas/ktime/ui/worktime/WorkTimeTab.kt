package technology.iatlas.ktime.ui.worktime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mu.KotlinLogging
import technology.iatlas.ktime.data.WorkTimeWithDay
import technology.iatlas.ktime.data.repositories.SettingsManager
import technology.iatlas.ktime.data.repositories.SqliteCurrentDayRepository
import technology.iatlas.ktime.data.repositories.SqliteSettingRepository
import technology.iatlas.ktime.data.repositories.SqliteWorkTimeRepository

private val logger = KotlinLogging.logger {}

/**
 * Work time tab of the application.
 */
@Composable
fun WorkTimeTab(database: org.jetbrains.exposed.sql.Database) {
    // Create repositories and ViewModel
    val workTimeRepository = remember { SqliteWorkTimeRepository(database) }
    val currentDayRepository = remember { SqliteCurrentDayRepository(database) }
    val settingsRepository = remember { SqliteSettingRepository(database) }
    val settingsManager = remember { SettingsManager(settingsRepository) }
    val viewModel = remember { WorkTimeViewModel(workTimeRepository, currentDayRepository, settingsManager) }

    // State for UI
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedWorkTime by remember { mutableStateOf<WorkTimeWithDay?>(null) }

    // Load work times when the composable is first created
    LaunchedEffect(Unit) {
        viewModel.loadWorkTimes()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        // Overtime display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Overtime",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = String.format("%.2f hours", viewModel.overtime.value).replace(".", ","),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (viewModel.overtime.value >= 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
        }

        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(end = 4.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Work Time")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Work Time")
            }
        }

        // Work time table
        WorkTimeTable(
            workTimes = viewModel.workTimes,
            onEditClick = { selectedWorkTime = it },
            onDeleteClick = { viewModel.deleteWorkTime(it.id) }
        )
    }

    // Add/Edit dialog
    if (showAddDialog || selectedWorkTime != null) {
        WorkTimeDialog(
            workTime = selectedWorkTime,
            onDismiss = {
                showAddDialog = false
                selectedWorkTime = null
            },
            onSave = { date, startTime, endTime, breakDuration, notes ->
                if (selectedWorkTime != null) {
                    // Update existing work time
                    viewModel.updateWorkTime(
                        id = selectedWorkTime!!.id,
                        startTime = startTime,
                        endTime = endTime,
                        breakDuration = breakDuration,
                        notes = notes
                    )
                } else {
                    // Add new work time
                    viewModel.addWorkTime(
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        breakDuration = breakDuration,
                        notes = notes
                    )
                }
                showAddDialog = false
                selectedWorkTime = null
            }
        )
    }
}

/**
 * Work time table component.
 */
@Composable
fun WorkTimeTable(
    workTimes: List<WorkTimeWithDay>,
    onEditClick: (WorkTimeWithDay) -> Unit,
    onDeleteClick: (WorkTimeWithDay) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Table header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            Text(
                text = "Date",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Hours",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Break",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Start",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "End",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center
            )
        }

        // Table rows
        if (workTimes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No work time entries yet")
            }
        } else {
            LazyColumn {
                items(workTimes) { workTime ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = workTime.date.toString(),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = String.format("%.2f", workTime.duration),
                            modifier = Modifier.weight(0.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = workTime.breakDuration.toString() + " Min",
                            modifier = Modifier.weight(0.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = formatTimeHHMM(workTime.startTime),
                            modifier = Modifier.weight(0.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = formatTimeHHMM(workTime.endTime),
                            modifier = Modifier.weight(0.5f),
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.weight(0.5f),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = { onEditClick(workTime) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onDeleteClick(workTime) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * Formats a LocalTime to HH:MM format
 */
private fun formatTimeHHMM(time: LocalTime): String {
    return String.format("%02d:%02d", time.hour, time.minute)
}

/**
 * Parses a string in HH:MM format to LocalTime
 */
private fun parseTimeHHMM(timeStr: String): LocalTime? {
    return try {
        // If the string already contains a colon, try to parse it directly
        if (timeStr.contains(":")) {
            // Try to parse as HH:MM
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                LocalTime(hour, minute)
            } else {
                null
            }
        } else if (timeStr.length == 4) {
            // Try to parse as HHMM
            val hour = timeStr.substring(0, 2).toInt()
            val minute = timeStr.substring(2, 4).toInt()
            LocalTime(hour, minute)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Work time dialog component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkTimeDialog(
    workTime: WorkTimeWithDay?,
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalTime, LocalTime, Int, String?) -> Unit
) {
    // Get current date and time
    val currentDate = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val currentTime = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    }

    var date by remember { mutableStateOf(workTime?.date ?: currentDate) }
    var startTime by remember { mutableStateOf(workTime?.startTime ?: currentTime) }

    // If adding for current day, set end time to current time, otherwise use default
    var endTime by remember {
        mutableStateOf(
            when {
                workTime != null -> workTime.endTime
                date == currentDate -> currentTime
                else -> LocalTime.parse("17:00")
            }
        )
    }

    var breakDuration by remember { mutableStateOf((workTime?.breakDuration ?: 30).toString()) }
    var notes by remember { mutableStateOf(workTime?.notes ?: "") }

    var startTimeError by remember { mutableStateOf(false) }
    var endTimeError by remember { mutableStateOf(false) }

    var startTimeText by remember { mutableStateOf(formatTimeHHMM(startTime)) }
    var endTimeText by remember { mutableStateOf(formatTimeHHMM(endTime)) }


    // Calculate break duration automatically when start time or end time changes
    LaunchedEffect(startTime, endTime) {
        if (workTime == null) { // Only auto-calculate for new entries
            calculateBreakDuration(startTime, endTime)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (workTime == null) "Add Work Time" else "Edit Work Time") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Date field
                OutlinedTextField(
                    value = date.toString(),
                    onValueChange = {
                        try {
                            date = LocalDate.parse(it)
                        } catch (e: Exception) {
                            // Invalid date format
                        }
                    },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Start time field
                OutlinedTextField(
                    value = startTimeText,
                    onValueChange = { input ->
                        startTimeText = input
                        parseTimeHHMM(input)?.let { parsedTime ->
                            startTimeError = false
                            startTime = parsedTime
                            calculateBreakDuration(startTime, endTime).also {
                                breakDuration = it
                            }
                        } ?: run {
                            startTimeError = true
                        }
                    },
                    supportingText = if (startTimeError) {
                        { Text("Bitte gültiges Format eingeben") }
                    } else null,

                    label = { Text("Start Time (HH:MM)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // End time field
                OutlinedTextField(
                    value = endTimeText,
                    onValueChange = { input ->
                        endTimeText = input
                        parseTimeHHMM(input)?.let { parsedTime ->
                            endTimeError = false
                            endTime = parsedTime
                            calculateBreakDuration(startTime, endTime).also {
                                breakDuration = it
                            }
                        } ?: run {
                            endTimeError = true
                        }
                    },
                    label = { Text("End Time (HH:MM)") },
                    isError = endTimeError,
                    supportingText = if (endTimeError) {
                        { Text("Bitte gültiges Format eingeben") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Break duration field
                OutlinedTextField(
                    value = breakDuration,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Break Duration (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!startTimeError && !endTimeError) {
                        onSave(
                            date,
                            startTime,
                            endTime,
                            breakDuration.toIntOrNull() ?: 0,
                            notes.ifBlank { null }
                        )
                    }

                },
                enabled = !startTimeError && !endTimeError
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun calculateBreakDuration(
    startTime: LocalTime,
    endTime: LocalTime
): String {
    val startMinutes = startTime.hour * 60 + startTime.minute
    val endMinutes = endTime.hour * 60 + endTime.minute
    val totalMinutes = endMinutes - startMinutes

    // Calcuate the break duration based on total work time
    return when (totalMinutes) {
        in 360..539 -> {
            // If between 6 and 8,99 hours, 30 minutes break
            "30"
        }

        in 0..359 -> {
            // If less than 6 hours, no break
            "0"
        }

        else -> {
            "45" // Default to 45 minutes if no valid time
        }
    }
}
