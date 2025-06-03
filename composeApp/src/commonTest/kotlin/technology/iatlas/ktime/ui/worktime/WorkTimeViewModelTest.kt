package technology.iatlas.ktime.ui.worktime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.BeforeTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import technology.iatlas.ktime.data.CurrentDay
import technology.iatlas.ktime.data.Setting
import technology.iatlas.ktime.data.WorkTime
import technology.iatlas.ktime.data.WorkTimeWithDay
import technology.iatlas.ktime.data.repositories.CurrentDayRepository
import technology.iatlas.ktime.data.repositories.SettingsManager
import technology.iatlas.ktime.data.repositories.SettingRepository
import technology.iatlas.ktime.data.repositories.WorkTimeRepository

/**
 * Simple test for WorkTimeViewModel that tests the getCurrentDate and getCurrentTime methods.
 * We can't test the coroutine-based methods without the kotlinx-coroutines-test module.
 */
class WorkTimeViewModelTest {

    // Mock repositories
    private lateinit var mockWorkTimeRepository: MockWorkTimeRepository
    private lateinit var mockCurrentDayRepository: MockCurrentDayRepository
    private lateinit var mockSettingRepository: MockSettingRepository
    private lateinit var settingsManager: SettingsManager

    // ViewModel to test
    private lateinit var viewModel: WorkTimeViewModel

    @BeforeTest
    fun setup() {
        // Initialize mock repositories
        mockWorkTimeRepository = MockWorkTimeRepository()
        mockCurrentDayRepository = MockCurrentDayRepository()
        mockSettingRepository = MockSettingRepository()

        // Create SettingsManager with mock repository
        settingsManager = SettingsManager(mockSettingRepository)

        // Initialize ViewModel with repositories
        viewModel = WorkTimeViewModel(
            workTimeRepository = mockWorkTimeRepository,
            currentDayRepository = mockCurrentDayRepository,
            settingsManager = settingsManager
        )

        // Set up test data
        val testDay = CurrentDay(
            id = 1,
            date = LocalDate(2023, 5, 15),
            isWorkDay = true,
            targetHours = 8.0f
        )
        mockCurrentDayRepository.currentDays[1] = testDay

        val testWorkTime = WorkTime(
            id = 1,
            currentDayId = 1,
            startTime = LocalTime(9, 0),
            endTime = LocalTime(17, 0),
            breakDuration = 30
        )
        mockWorkTimeRepository.workTimes[1] = testWorkTime

        val testWorkTimeWithDay = WorkTimeWithDay(
            id = 1,
            date = LocalDate(2023, 5, 15),
            startTime = LocalTime(9, 0),
            endTime = LocalTime(17, 0),
            breakDuration = 30,
            duration = 7.5f,
            notes = null
        )
        mockWorkTimeRepository.workTimesWithDay.add(testWorkTimeWithDay)

        // Set up settings
        mockSettingRepository.settings["workHoursPerWeek"] = "40.0"
    }

    @Test
    fun testGetCurrentDate() {
        // Get current date
        val currentDate = viewModel.getCurrentDate()

        // Verify current date is not null
        assertEquals(true, currentDate != null)
    }

    @Test
    fun testGetCurrentTime() {
        // Get current time
        val currentTime = viewModel.getCurrentTime()

        // Verify current time is not null
        assertEquals(true, currentTime != null)
    }

    // Mock implementations of repositories for testing

    class MockWorkTimeRepository : WorkTimeRepository {
        val workTimes = mutableMapOf<Int, WorkTime>()
        val workTimesWithDay = mutableListOf<WorkTimeWithDay>()
        private var nextId = 2 // Start with 2 since we already have ID 1 in setup

        override suspend fun getWorkTimeById(id: Int): WorkTime? {
            return workTimes[id]
        }

        override suspend fun getWorkTimesForDay(currentDayId: Int): List<WorkTime> {
            return workTimes.values.filter { it.currentDayId == currentDayId }
        }

        override suspend fun getAllWorkTimesWithDay(): List<WorkTimeWithDay> {
            return workTimesWithDay
        }

        override suspend fun saveWorkTime(workTime: WorkTime): WorkTime {
            val savedWorkTime = if (workTime.id == null) {
                // Insert new work time
                val id = nextId++
                workTime.copy(id = id).also {
                    workTimes[id] = it

                    // Also add to workTimesWithDay for testing
                    workTimesWithDay.add(
                        WorkTimeWithDay(
                            id = id,
                            date = LocalDate(2023, 5, 16), // Assuming this is for a new day
                            startTime = it.startTime,
                            endTime = it.endTime,
                            breakDuration = it.breakDuration,
                            duration = (it.endTime.hour * 60 + it.endTime.minute - it.startTime.hour * 60 - it.startTime.minute - it.breakDuration) / 60.0f,
                            notes = it.notes
                        )
                    )
                }
            } else {
                // Update existing work time
                workTime.also {
                    workTimes[it.id!!] = it

                    // Also update workTimesWithDay
                    val index = workTimesWithDay.indexOfFirst { wtd -> wtd.id == it.id!! }
                    if (index >= 0) {
                        workTimesWithDay[index] = workTimesWithDay[index].copy(
                            startTime = it.startTime,
                            endTime = it.endTime,
                            breakDuration = it.breakDuration,
                            duration = (it.endTime.hour * 60 + it.endTime.minute - it.startTime.hour * 60 - it.startTime.minute - it.breakDuration) / 60.0f,
                            notes = it.notes
                        )
                    }
                }
            }
            return savedWorkTime
        }

        override suspend fun deleteWorkTime(id: Int) {
            workTimes.remove(id)
            workTimesWithDay.removeIf { it.id == id }
        }
    }

    class MockCurrentDayRepository : CurrentDayRepository {
        val currentDays = mutableMapOf<Int, CurrentDay>()
        private var nextId = 2 // Start with 2 since we already have ID 1 in setup

        override suspend fun getCurrentDayByDate(date: LocalDate): CurrentDay? {
            return currentDays.values.find { it.date == date }
        }

        override suspend fun getAllCurrentDays(): List<CurrentDay> {
            return currentDays.values.toList()
        }

        override suspend fun saveCurrentDay(currentDay: CurrentDay): CurrentDay {
            val savedCurrentDay = if (currentDay.id == null) {
                // Insert new current day
                val id = nextId++
                currentDay.copy(id = id).also {
                    currentDays[id] = it
                }
            } else {
                // Update existing current day
                currentDay.also {
                    currentDays[it.id!!] = it
                }
            }
            return savedCurrentDay
        }

        override suspend fun deleteCurrentDay(id: Int) {
            currentDays.remove(id)
        }
    }

    class MockSettingRepository : SettingRepository {
        val settings = mutableMapOf<String, String>()

        override suspend fun getSetting(key: String): Setting? {
            return settings[key]?.let {
                Setting(
                    id = 1,
                    key = key,
                    value = it
                )
            }
        }

        override suspend fun saveSetting(key: String, value: String) {
            settings[key] = value
        }

        override suspend fun getAllSettings(): List<Setting> {
            return settings.map { (key, value) ->
                Setting(
                    id = 1,
                    key = key,
                    value = value
                )
            }
        }

        override suspend fun deleteSetting(key: String) {
            settings.remove(key)
        }
    }
}
