package technology.iatlas.ktime.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class ModelsTest {

    @Test
    fun testWorkTimeDurationCalculation() {
        // Test case 1: Simple 8-hour workday with 30-minute break
        val workTime1 = WorkTime(
            id = 1,
            currentDayId = 1,
            startTime = LocalTime(9, 0),
            endTime = LocalTime(17, 30),
            breakDuration = 30,
            notes = "Regular workday"
        )
        assertEquals(8.0f, workTime1.duration, "Duration should be 8 hours for 9:00-17:30 with 30min break")

        // Test case 2: Short workday with no break
        val workTime2 = WorkTime(
            id = 2,
            currentDayId = 1,
            startTime = LocalTime(13, 0),
            endTime = LocalTime(17, 0),
            breakDuration = 0,
            notes = "Half day"
        )
        assertEquals(4.0f, workTime2.duration, "Duration should be 4 hours for 13:00-17:00 with no break")

        // Test case 3: Long workday with long break
        val workTime3 = WorkTime(
            id = 3,
            currentDayId = 1,
            startTime = LocalTime(8, 0),
            endTime = LocalTime(20, 0),
            breakDuration = 120,
            notes = "Long day with long break"
        )
        assertEquals(10.0f, workTime3.duration, "Duration should be 10 hours for 8:00-20:00 with 2h break")
    }

    @Test
    fun testCurrentDayCreation() {
        val date = LocalDate(2023, 5, 15)
        val currentDay = CurrentDay(
            id = 1,
            date = date,
            isWorkDay = true,
            targetHours = 8.0f,
            notes = "Monday"
        )

        assertEquals(1, currentDay.id)
        assertEquals(date, currentDay.date)
        assertEquals(true, currentDay.isWorkDay)
        assertEquals(8.0f, currentDay.targetHours)
        assertEquals("Monday", currentDay.notes)
    }

    @Test
    fun testVacationCreation() {
        val startDate = LocalDate(2023, 7, 1)
        val endDate = LocalDate(2023, 7, 14)

        val vacation = Vacation(
            id = 1,
            currentDayId = 10,
            endDate = endDate,
            type = VacationType.VACATION,
            notes = "Summer vacation"
        )

        assertEquals(1, vacation.id)
        assertEquals(10, vacation.currentDayId)
        assertEquals(endDate, vacation.endDate)
        assertEquals(VacationType.VACATION, vacation.type)
        assertEquals("Summer vacation", vacation.notes)
    }

    @Test
    fun testSettingCreation() {
        val setting = Setting(
            id = 1,
            key = "workHoursPerWeek",
            value = "40.0"
        )

        assertEquals(1, setting.id)
        assertEquals("workHoursPerWeek", setting.key)
        assertEquals("40.0", setting.value)
    }

    @Test
    fun testWorkTimeWithDayCreation() {
        val date = LocalDate(2023, 5, 15)
        val startTime = LocalTime(9, 0)
        val endTime = LocalTime(17, 0)

        val workTimeWithDay = WorkTimeWithDay(
            id = 1,
            date = date,
            startTime = startTime,
            endTime = endTime,
            breakDuration = 30,
            duration = 7.5f,
            notes = "Regular workday"
        )

        assertEquals(1, workTimeWithDay.id)
        assertEquals(date, workTimeWithDay.date)
        assertEquals(startTime, workTimeWithDay.startTime)
        assertEquals(endTime, workTimeWithDay.endTime)
        assertEquals(30, workTimeWithDay.breakDuration)
        assertEquals(7.5f, workTimeWithDay.duration)
        assertEquals("Regular workday", workTimeWithDay.notes)
    }
}