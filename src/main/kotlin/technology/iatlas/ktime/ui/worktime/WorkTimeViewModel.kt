package technology.iatlas.ktime.ui.worktime

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mu.KotlinLogging
import technology.iatlas.ktime.data.CurrentDay
import technology.iatlas.ktime.data.WorkTime
import technology.iatlas.ktime.data.WorkTimeWithDay
import technology.iatlas.ktime.data.repositories.CurrentDayRepository
import technology.iatlas.ktime.data.repositories.SettingsManager
import technology.iatlas.ktime.data.repositories.WorkTimeRepository

private val logger = KotlinLogging.logger {}

/**
 * ViewModel for the Work Time tab.
 */
class WorkTimeViewModel(
    private val workTimeRepository: WorkTimeRepository,
    private val currentDayRepository: CurrentDayRepository,
    private val settingsManager: SettingsManager
) {
    // Work times as a mutable state
    private val _workTimes = mutableStateListOf<WorkTimeWithDay>()
    val workTimes: List<WorkTimeWithDay> = _workTimes

    // Overtime as a mutable state
    private val _overtime = mutableStateOf(0.0f)
    val overtime = _overtime

    // Coroutine scope for launching coroutines
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    // Load work times from the database
    fun loadWorkTimes() {
        logger.debug { "Loading work times from database" }
        viewModelScope.launch {
            try {
                // Load work times from database
                val loadedWorkTimes = workTimeRepository.getAllWorkTimesWithDay()
                logger.debug { "Successfully loaded ${loadedWorkTimes.size} work time entries" }

                // Update state
                _workTimes.clear()
                _workTimes.addAll(loadedWorkTimes)
                logger.debug { "Updated work times state with ${_workTimes.size} entries" }

                // Calculate overtime
                calculateOvertime()
            } catch (e: Exception) {
                // Handle database errors
                logger.error(e) { "Error loading work times" }
            }
        }
    }

    // Add a new work time entry
    fun addWorkTime(
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        breakDuration: Int,
        notes: String? = null
    ) {
        logger.debug { "Adding new work time entry: date=$date, startTime=$startTime, endTime=$endTime, breakDuration=$breakDuration, notes=$notes" }
        viewModelScope.launch {
            try {
                // Get or create current day
                val currentDay = currentDayRepository.getCurrentDayByDate(date) ?: createCurrentDay(date)
                logger.debug { "Using current day: $currentDay" }

                // Create work time
                val workTime = WorkTime(
                    currentDayId = currentDay.id!!,
                    startTime = startTime,
                    endTime = endTime,
                    breakDuration = breakDuration,
                    notes = notes
                )
                logger.debug { "Created work time object: $workTime" }

                // Save work time
                val savedWorkTime = workTimeRepository.saveWorkTime(workTime)
                logger.info { "Successfully saved work time entry with ID: ${savedWorkTime.id}" }

                // Reload work times
                loadWorkTimes()
            } catch (e: Exception) {
                // Handle database errors
                logger.error(e) { "Error adding work time entry" }
            }
        }
    }

    // Update an existing work time entry
    fun updateWorkTime(
        id: Int,
        startTime: LocalTime,
        endTime: LocalTime,
        breakDuration: Int,
        notes: String? = null
    ) {
        logger.debug { "Updating work time entry: id=$id, startTime=$startTime, endTime=$endTime, breakDuration=$breakDuration, notes=$notes" }
        viewModelScope.launch {
            try {
                // Get existing work time
                val existingWorkTime = workTimeRepository.getWorkTimeById(id)

                if (existingWorkTime != null) {
                    logger.debug { "Found existing work time: $existingWorkTime" }

                    // Update work time
                    val updatedWorkTime = existingWorkTime.copy(
                        startTime = startTime,
                        endTime = endTime,
                        breakDuration = breakDuration,
                        notes = notes
                    )
                    logger.debug { "Created updated work time object: $updatedWorkTime" }

                    // Save work time
                    workTimeRepository.saveWorkTime(updatedWorkTime)
                    logger.info { "Successfully updated work time entry with ID: $id" }

                    // Reload work times
                    loadWorkTimes()
                } else {
                    logger.warn { "Attempted to update non-existent work time with ID: $id" }
                }
            } catch (e: Exception) {
                // Handle database errors
                logger.error(e) { "Error updating work time entry with ID: $id" }
            }
        }
    }

    // Delete a work time entry
    fun deleteWorkTime(id: Int) {
        logger.debug { "Deleting work time entry with ID: $id" }
        viewModelScope.launch {
            try {
                // Delete work time
                workTimeRepository.deleteWorkTime(id)
                logger.info { "Successfully deleted work time entry with ID: $id" }

                // Reload work times
                loadWorkTimes()
            } catch (e: Exception) {
                // Handle database errors
                logger.error(e) { "Error deleting work time entry with ID: $id" }
            }
        }
    }

    // Calculate overtime
    private suspend fun calculateOvertime() {
        logger.debug { "Calculating overtime" }
        try {
            // Get work hours per week from settings
            val workHoursPerWeek = settingsManager.getFloat("workHoursPerWeek")
            logger.debug { "Work hours per week from settings: $workHoursPerWeek" }

            // Calculate target hours per day
            val targetHoursPerDay = workHoursPerWeek / 5.0f // Assuming 5 work days per week
            logger.debug { "Target hours per day: $targetHoursPerDay" }

            // Calculate total actual hours
            var totalActualHours = 0.0f
            for (workTime in _workTimes) {
                totalActualHours += workTime.duration
            }
            logger.debug { "Total actual hours worked: $totalActualHours" }

            // Calculate total target hours based on unique work days
            val uniqueWorkDays = _workTimes.map { it.date }.distinct()
            val totalTargetHours = uniqueWorkDays.size * targetHoursPerDay
            logger.debug { "Unique work days: ${uniqueWorkDays.size}, total target hours: $totalTargetHours" }

            // Calculate overtime
            _overtime.value = totalActualHours - totalTargetHours
            logger.info { "Calculated overtime: ${_overtime.value} hours" }
        } catch (e: Exception) {
            // Handle errors
            logger.error(e) { "Error calculating overtime" }
        }
    }

    /**
     * Formatiert die Pausendauer von Minuten in Stunden als Dezimalzahl.
     * Beispiel: 30 Minuten -> 0,5 Stunden
     */
    private fun formatBreakDurationAsHours(minutes: Int): String {
        val hours = minutes / 60.0f
        return String.format("%.1f", hours).replace(',', '.')
    }


    // Create a new current day
    private suspend fun createCurrentDay(date: LocalDate): CurrentDay {
        logger.debug { "Creating new current day for date: $date" }

        // Get work hours per week from settings
        val workHoursPerWeek = settingsManager.getFloat("workHoursPerWeek")
        logger.debug { "Work hours per week from settings: $workHoursPerWeek" }

        // Calculate target hours per day
        val targetHoursPerDay = workHoursPerWeek / 5.0f // Assuming 5 work days per week
        logger.debug { "Target hours per day: $targetHoursPerDay" }

        // Create current day
        val currentDay = CurrentDay(
            date = date,
            isWorkDay = true,
            targetHours = targetHoursPerDay
        )
        logger.debug { "Created current day object: $currentDay" }

        // Save current day
        val savedCurrentDay = currentDayRepository.saveCurrentDay(currentDay)
        logger.info { "Successfully saved current day with ID: ${savedCurrentDay.id}" }

        return savedCurrentDay
    }

    // Get current date
    fun getCurrentDate(): LocalDate {
        logger.debug { "Getting current date" }
        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val currentDate = localDateTime.date
        logger.debug { "Current date: $currentDate" }
        return currentDate
    }

    // Get current time
    fun getCurrentTime(): LocalTime {
        logger.debug { "Getting current time" }
        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val currentTime = localDateTime.time
        logger.debug { "Current time: $currentTime" }
        return currentTime
    }
}
