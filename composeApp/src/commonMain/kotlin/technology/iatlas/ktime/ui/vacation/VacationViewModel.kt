package technology.iatlas.ktime.ui.vacation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import technology.iatlas.ktime.data.CurrentDay
import technology.iatlas.ktime.data.Vacation
import technology.iatlas.ktime.data.VacationType
import technology.iatlas.ktime.data.repositories.CurrentDayRepository
import technology.iatlas.ktime.data.repositories.VacationRepository

/**
 * ViewModel for the Vacation tab.
 */
class VacationViewModel(
    private val vacationRepository: VacationRepository,
    private val currentDayRepository: CurrentDayRepository
) {
    // Vacations as a mutable state
    private val _vacations = mutableStateListOf<Pair<Vacation, LocalDate>>()
    val vacations: List<Pair<Vacation, LocalDate>> = _vacations

    // Vacation days as a mutable state map (date -> vacation type)
    private val _vacationDays = mutableStateMapOf<LocalDate, VacationType>()
    val vacationDays: Map<LocalDate, VacationType> = _vacationDays

    // Coroutine scope for launching coroutines
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    // Load vacations from the database
    fun loadVacations() {
        viewModelScope.launch {
            try {
                // Load vacations from database
                val loadedVacations = vacationRepository.getAllVacationsWithDate()

                // Update state
                _vacations.clear()
                _vacations.addAll(loadedVacations)

                // Update vacation days map
                _vacationDays.clear()
                for ((vacation, startDate) in loadedVacations) {
                    _vacationDays[startDate] = vacation.type

                    // If vacation has an end date, add all days in between
                    if (vacation.endDate != null) {
                        // For now, just add start and end date
                        _vacationDays[startDate] = vacation.type
                        _vacationDays[vacation.endDate] = vacation.type
                    }
                }
            } catch (e: Exception) {
                // Handle database errors
                println("Error loading vacations: ${e.message}")
            }
        }
    }

    // Add a new vacation
    fun addVacation(
        startDate: LocalDate,
        endDate: LocalDate? = null,
        type: VacationType,
        notes: String? = null
    ) {
        viewModelScope.launch {
            try {
                // Get or create current day for start date
                val currentDay = currentDayRepository.getCurrentDayByDate(startDate) ?: createCurrentDay(startDate)

                // Create vacation
                val vacation = Vacation(
                    currentDayId = currentDay.id!!,
                    endDate = endDate,
                    type = type,
                    notes = notes
                )

                // Save vacation
                vacationRepository.saveVacation(vacation)

                // Reload vacations
                loadVacations()
            } catch (e: Exception) {
                // Handle database errors
                println("Error adding vacation: ${e.message}")
            }
        }
    }

    // Update an existing vacation
    fun updateVacation(
        id: Int,
        endDate: LocalDate? = null,
        type: VacationType,
        notes: String? = null
    ) {
        viewModelScope.launch {
            try {
                // Get existing vacation
                val existingVacation = vacationRepository.getVacationById(id)

                if (existingVacation != null) {
                    // Update vacation
                    val updatedVacation = existingVacation.copy(
                        endDate = endDate,
                        type = type,
                        notes = notes
                    )

                    // Save vacation
                    vacationRepository.saveVacation(updatedVacation)

                    // Reload vacations
                    loadVacations()
                }
            } catch (e: Exception) {
                // Handle database errors
                println("Error updating vacation: ${e.message}")
            }
        }
    }

    // Delete a vacation
    fun deleteVacation(id: Int) {
        viewModelScope.launch {
            try {
                // Delete vacation
                vacationRepository.deleteVacation(id)

                // Reload vacations
                loadVacations()
            } catch (e: Exception) {
                // Handle database errors
                println("Error deleting vacation: ${e.message}")
            }
        }
    }

    // Create a new current day
    private suspend fun createCurrentDay(date: LocalDate): CurrentDay {
        // Create current day
        val currentDay = CurrentDay(
            date = date,
            isWorkDay = false, // Vacation days are not work days
            targetHours = 0.0f // No target hours for vacation days
        )

        // Save current day
        return currentDayRepository.saveCurrentDay(currentDay)
    }

    // Check if a date is a vacation day
    fun isVacationDay(date: LocalDate): Boolean {
        return date in _vacationDays
    }

    // Get the vacation type for a date
    fun getVacationTypeForDate(date: LocalDate): VacationType? {
        return _vacationDays[date]
    }
}
