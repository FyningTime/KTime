package technology.iatlas.ktime.ui.vacation

import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import technology.iatlas.ktime.data.VacationType
import technology.iatlas.ktime.data.repositories.SqliteCurrentDayRepository
import technology.iatlas.ktime.data.repositories.SqliteVacationRepository

/**
 * Vacation tab of the application.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationTab(database: org.jetbrains.exposed.sql.Database) {
    // Create repositories and ViewModel
    val vacationRepository = remember { SqliteVacationRepository(database) }
    val currentDayRepository = remember { SqliteCurrentDayRepository(database) }
    val viewModel = remember { VacationViewModel(vacationRepository, currentDayRepository) }

    // State for UI
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedVacation by remember { mutableStateOf<Pair<Int, LocalDate>?>(null) }
    var selectedMonth by remember { mutableStateOf(getCurrentMonth()) }
    var selectedYear by remember { mutableStateOf(getCurrentYear()) }

    // Load vacations when the composable is first created
    LaunchedEffect(Unit) {
        viewModel.loadVacations()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Month selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (selectedMonth == 1) {
                    selectedMonth = 12
                    selectedYear--
                } else {
                    selectedMonth--
                }
            }) {
                Text("<")
            }

            Text(
                text = "${getMonthName(selectedMonth)} $selectedYear",
                style = MaterialTheme.typography.titleLarge
            )

            IconButton(onClick = {
                if (selectedMonth == 12) {
                    selectedMonth = 1
                    selectedYear++
                } else {
                    selectedMonth++
                }
            }) {
                Text(">")
            }
        }

        // Calendar
        Calendar(
            month = selectedMonth,
            year = selectedYear,
            vacationDays = viewModel.vacationDays,
            onDateClick = { date ->
                // Check if this date already has a vacation
                if (viewModel.isVacationDay(date)) {
                    // Find the vacation for this date
                    val vacation = viewModel.vacations.find { (_, startDate) -> startDate == date }
                    if (vacation != null) {
                        selectedVacation = Pair(vacation.first.id!!, date)
                    }
                } else {
                    // Show add dialog with this date
                    showAddDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(bottom = 8.dp)
        )

        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Vacation")
            }
        }

        // Vacation list
        VacationList(
            vacations = viewModel.vacations,
            onEditClick = { id, date -> selectedVacation = Pair(id, date) },
            onDeleteClick = { id -> viewModel.deleteVacation(id) }
        )
    }

    // Add/Edit dialog
    if (showAddDialog || selectedVacation != null) {
        VacationDialog(
            vacationId = selectedVacation?.first,
            initialDate = selectedVacation?.second ?: LocalDate.parse("2023-01-01"),
            onDismiss = {
                showAddDialog = false
                selectedVacation = null
            },
            onSave = { startDate, endDate, type, notes ->
                if (selectedVacation != null) {
                    // Update existing vacation
                    viewModel.updateVacation(
                        id = selectedVacation!!.first,
                        endDate = endDate,
                        type = type,
                        notes = notes
                    )
                } else {
                    // Add new vacation
                    viewModel.addVacation(
                        startDate = startDate,
                        endDate = endDate,
                        type = type,
                        notes = notes
                    )
                }
                showAddDialog = false
                selectedVacation = null
            }
        )
    }
}

/**
 * Calendar component.
 */
@Composable
fun Calendar(
    month: Int,
    year: Int,
    vacationDays: Map<LocalDate, VacationType>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysInMonth = getDaysInMonth(month, year)
    val firstDayOfWeek = getFirstDayOfWeek(month, year)

    Column(modifier = modifier) {
        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 20.sp
                )
            }
        }

        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Empty cells for days before the first day of the month
            items((firstDayOfWeek - 1)) { _ ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(1.dp)
                        .border(0.5.dp, Color.LightGray)
                )
            }

            // Days of the month
            items(daysInMonth) { day ->
                val date = LocalDate(year, month, day + 1)
                //val isVacationDay = date in vacationDays
                val vacationType = vacationDays[date]

                // Check if this is the current date
                val isCurrentDate = date == getCurrentDate()

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(1.dp)
                        .border(0.5.dp, Color.LightGray)
                        .background(
                            when {
                                isCurrentDate -> Color.Blue // Current day is blue
                                vacationType == VacationType.VACATION -> Color(0xFFFFA500) // Orange for vacation
                                vacationType == VacationType.SICK_LEAVE -> Color(0xFFFFCCBC) // Light red
                                vacationType == VacationType.BUSINESS_TRIP -> Color(0xFFDCEDC8) // Light green
                                vacationType == VacationType.OTHER -> Color(0xFFF5F5F5) // Light gray
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onDateClick(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (day + 1).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 20.sp,
                        color = if (isCurrentDate) Color.White else Color.Unspecified // White text for current day
                    )
                }
            }
        }
    }
}

/**
 * Vacation list component.
 */
@Composable
fun VacationList(
    vacations: List<Pair<technology.iatlas.ktime.data.Vacation, LocalDate>>,
    onEditClick: (Int, LocalDate) -> Unit,
    onDeleteClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // List header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp)
        ) {
            Text(
                text = "Date",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Type",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "End Date",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center
            )
        }

        // List rows
        if (vacations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No vacations yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // ✅ Explizite Höhenbegrenzung
            ) {
                items(vacations) { (vacation, date) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = vacation.type.name,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = date.toString(),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = vacation.endDate?.toString() ?: "-",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.weight(0.5f),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = { onEditClick(vacation.id!!, date) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onDeleteClick(vacation.id!!) }) {
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
 * Vacation dialog component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationDialog(
    vacationId: Int?,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate?, VacationType, String?) -> Unit
) {
    var startDate by remember { mutableStateOf(initialDate) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedType by remember { mutableStateOf(VacationType.VACATION) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (vacationId == null) "Add Vacation" else "Edit Vacation") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Start date field
                OutlinedTextField(
                    value = startDate.toString(),
                    onValueChange = {
                        try {
                            startDate = LocalDate.parse(it)
                        } catch (e: Exception) {
                            // Invalid date format
                        }
                    },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // End date field
                OutlinedTextField(
                    value = endDate?.toString() ?: "",
                    onValueChange = {
                        try {
                            endDate = if (it.isBlank()) null else LocalDate.parse(it)
                        } catch (e: Exception) {
                            // Invalid date format
                        }
                    },
                    label = { Text("End Date (YYYY-MM-DD, optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Vacation type selection
                Text(
                    text = "Vacation Type",
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VacationType.values().forEach { type ->
                        Button(
                            onClick = { selectedType = type },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType == type)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedType == type)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(type.name)
                        }
                    }
                }

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
                    onSave(
                        startDate,
                        endDate,
                        selectedType,
                        if (notes.isBlank()) null else notes
                    )
                }
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

// Helper functions for calendar

/**
 * Gets the current month (1-12).
 */
fun getCurrentMonth(): Int {
    val now = Clock.System.now()
    val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
    return localDateTime.monthNumber
}

/**
 * Gets the current year.
 */
fun getCurrentYear(): Int {
    val now = Clock.System.now()
    val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
    return localDateTime.year
}

/**
 * Gets the current date.
 */
fun getCurrentDate(): LocalDate {
    val now = Clock.System.now()
    val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
    return localDateTime.date
}

/**
 * Gets the name of the month.
 */
fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }
}

/**
 * Gets the number of days in a month.
 */
fun getDaysInMonth(month: Int, year: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 0
    }
}

/**
 * Checks if a year is a leap year.
 */
fun isLeapYear(year: Int): Boolean {
    return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}

/**
 * Gets the day of the week for the first day of the month (1 = Monday, 7 = Sunday).
 */
fun getFirstDayOfWeek(month: Int, year: Int): Int {
    // Create a LocalDate for the first day of the month
    val firstDayOfMonth = LocalDate(year, month, 1)

    // Get the day of week (1 = Monday, 7 = Sunday)
    val dayOfWeek = firstDayOfMonth.dayOfWeek.value

    return dayOfWeek
}
