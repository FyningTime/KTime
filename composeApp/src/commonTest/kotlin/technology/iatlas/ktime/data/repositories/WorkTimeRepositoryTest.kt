package technology.iatlas.ktime.data.repositories

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.BeforeTest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import technology.iatlas.ktime.data.CurrentDay
import technology.iatlas.ktime.data.WorkTime
import technology.iatlas.ktime.data.WorkTimeWithDay

class WorkTimeRepositoryTest {

    private lateinit var workTimeRepository: MockWorkTimeRepository
    private lateinit var currentDayRepository: MockCurrentDayRepository

    @BeforeTest
    fun setup() {
        // Initialize mock repositories
        workTimeRepository = MockWorkTimeRepository()
        currentDayRepository = MockCurrentDayRepository()

        // Add test data
        runBlocking {
            // Create a test day
            val testDay = CurrentDay(
                date = LocalDate(2023, 5, 15),
                isWorkDay = true,
                targetHours = 8.0f,
                notes = "Test day"
            )
            val savedDay = currentDayRepository.saveCurrentDay(testDay)

            // Create a test work time
            val testWorkTime = WorkTime(
                currentDayId = savedDay.id!!,
                startTime = LocalTime(9, 0),
                endTime = LocalTime(17, 0),
                breakDuration = 30,
                notes = "Test work time"
            )
            workTimeRepository.saveWorkTime(testWorkTime)
        }
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
                val id = 1 // Use 1 for the first test entry
                workTime.copy(id = id).also {
                    workTimes[id] = it

                    // Also add to workTimesWithDay for testing
                    workTimesWithDay.add(
                        WorkTimeWithDay(
                            id = id,
                            date = LocalDate(2023, 5, 15),
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
                val id = 1 // Use 1 for the first test entry
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

    @Test
    fun testGetWorkTimeById() = runBlocking {
        // Get all work times to find the ID
        val allWorkTimes = workTimeRepository.getAllWorkTimesWithDay()
        val workTimeId = allWorkTimes.first().id

        // Get work time by ID
        val workTime = workTimeRepository.getWorkTimeById(workTimeId)

        // Verify work time
        assertNotNull(workTime)
        assertEquals(workTimeId, workTime.id)
        assertEquals(LocalTime(9, 0), workTime.startTime)
        assertEquals(LocalTime(17, 0), workTime.endTime)
        assertEquals(30, workTime.breakDuration)
        assertEquals("Test work time", workTime.notes)
    }

    @Test
    fun testGetWorkTimesForDay() = runBlocking {
        // Get all current days to find the ID
        val allDays = currentDayRepository.getAllCurrentDays()
        val dayId = allDays.first().id!!

        // Get work times for day
        val workTimes = workTimeRepository.getWorkTimesForDay(dayId)

        // Verify work times
        assertEquals(1, workTimes.size)
        assertEquals(dayId, workTimes.first().currentDayId)
        assertEquals(LocalTime(9, 0), workTimes.first().startTime)
        assertEquals(LocalTime(17, 0), workTimes.first().endTime)
    }

    @Test
    fun testGetAllWorkTimesWithDay() = runBlocking {
        // Get all work times with day
        val workTimesWithDay = workTimeRepository.getAllWorkTimesWithDay()

        // Verify work times with day
        assertEquals(1, workTimesWithDay.size)
        assertEquals(LocalDate(2023, 5, 15), workTimesWithDay.first().date)
        assertEquals(LocalTime(9, 0), workTimesWithDay.first().startTime)
        assertEquals(LocalTime(17, 0), workTimesWithDay.first().endTime)
        assertEquals(30, workTimesWithDay.first().breakDuration)
        assertEquals(7.5f, workTimesWithDay.first().duration)
    }

    @Test
    fun testSaveWorkTime() = runBlocking {
        // Get all current days to find the ID
        val allDays = currentDayRepository.getAllCurrentDays()
        val dayId = allDays.first().id!!

        // Create a new work time
        val newWorkTime = WorkTime(
            currentDayId = dayId,
            startTime = LocalTime(13, 0),
            endTime = LocalTime(18, 0),
            breakDuration = 15,
            notes = "New test work time"
        )

        // Save work time
        val savedWorkTime = workTimeRepository.saveWorkTime(newWorkTime)

        // Verify saved work time
        assertNotNull(savedWorkTime.id)
        assertEquals(dayId, savedWorkTime.currentDayId)
        assertEquals(LocalTime(13, 0), savedWorkTime.startTime)
        assertEquals(LocalTime(18, 0), savedWorkTime.endTime)
        assertEquals(15, savedWorkTime.breakDuration)
        assertEquals("New test work time", savedWorkTime.notes)

        // Verify work time was saved to database
        val workTime = workTimeRepository.getWorkTimeById(savedWorkTime.id!!)
        assertNotNull(workTime)
        assertEquals(savedWorkTime.id, workTime.id)
    }

    @Test
    fun testUpdateWorkTime() = runBlocking {
        // Get all work times to find the ID
        val allWorkTimes = workTimeRepository.getAllWorkTimesWithDay()
        val workTimeId = allWorkTimes.first().id

        // Get work time by ID
        val workTime = workTimeRepository.getWorkTimeById(workTimeId)!!

        // Update work time
        val updatedWorkTime = workTime.copy(
            startTime = LocalTime(10, 0),
            endTime = LocalTime(18, 0),
            breakDuration = 45,
            notes = "Updated test work time"
        )

        // Save updated work time
        val savedWorkTime = workTimeRepository.saveWorkTime(updatedWorkTime)

        // Verify updated work time
        assertEquals(workTimeId, savedWorkTime.id)
        assertEquals(LocalTime(10, 0), savedWorkTime.startTime)
        assertEquals(LocalTime(18, 0), savedWorkTime.endTime)
        assertEquals(45, savedWorkTime.breakDuration)
        assertEquals("Updated test work time", savedWorkTime.notes)

        // Verify work time was updated in database
        val retrievedWorkTime = workTimeRepository.getWorkTimeById(workTimeId)
        assertNotNull(retrievedWorkTime)
        assertEquals(LocalTime(10, 0), retrievedWorkTime.startTime)
        assertEquals("Updated test work time", retrievedWorkTime.notes)
    }

    @Test
    fun testDeleteWorkTime() = runBlocking {
        // Get all work times to find the ID
        val allWorkTimes = workTimeRepository.getAllWorkTimesWithDay()
        val workTimeId = allWorkTimes.first().id

        // Delete work time
        workTimeRepository.deleteWorkTime(workTimeId)

        // Verify work time was deleted
        val workTime = workTimeRepository.getWorkTimeById(workTimeId)
        assertNull(workTime)
    }
}
