package technology.iatlas.ktime.database.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.BeforeTest

/**
 * Simple test for DatabaseMigrationManager that tests the helper methods.
 * We can't test the database-related methods without the H2 driver.
 */
class DatabaseMigrationManagerTest {

    private lateinit var migrationManager: DatabaseMigrationManager

    @BeforeTest
    fun setup() {
        // Initialize the migration manager
        migrationManager = DatabaseMigrationManager()
    }

    @Test
    fun testExtractTimeFromTimestamp() {
        assertEquals("09:30", migrationManager.extractTimeFromTimestamp("2023-05-15 09:30:00+0200"))
        assertEquals("17:45", migrationManager.extractTimeFromTimestamp("2023-05-15 17:45:00+0200"))
        assertEquals("00:00", migrationManager.extractTimeFromTimestamp("invalid"))
    }

    @Test
    fun testExtractDateFromTimestamp() {
        assertEquals("2023-05-15", migrationManager.extractDateFromTimestamp("2023-05-15 09:30:00+0200"))
        assertEquals("2023-07-01", migrationManager.extractDateFromTimestamp("2023-07-01 00:00:00+0200"))
    }

    @Test
    fun testParseTimeToHours() {
        // Use reflection to access the private method
        val parseTimeToHoursMethod =
            DatabaseMigrationManager::class.java.getDeclaredMethod("parseTimeToHours", String::class.java)
        parseTimeToHoursMethod.isAccessible = true

        // Test various time formats
        assertEquals(8.0f, parseTimeToHoursMethod.invoke(migrationManager, "8h0m0s") as Float)
        assertEquals(4.5f, parseTimeToHoursMethod.invoke(migrationManager, "4h30m0s") as Float)
        assertEquals(1.25f, parseTimeToHoursMethod.invoke(migrationManager, "1h15m0s") as Float)
        assertEquals(0.5f, parseTimeToHoursMethod.invoke(migrationManager, "0h30m0s") as Float)
    }

    @Test
    fun testParseBreakTimeToMinutes() {
        // Use reflection to access the private method
        val parseBreakTimeToMinutesMethod =
            DatabaseMigrationManager::class.java.getDeclaredMethod("parseBreakTimeToMinutes", String::class.java)
        parseBreakTimeToMinutesMethod.isAccessible = true

        // Test various break time formats
        assertEquals(30, parseBreakTimeToMinutesMethod.invoke(migrationManager, "30m") as Int)
        assertEquals(45, parseBreakTimeToMinutesMethod.invoke(migrationManager, "45m") as Int)
        assertEquals(60, parseBreakTimeToMinutesMethod.invoke(migrationManager, "60m") as Int)
        assertEquals(0, parseBreakTimeToMinutesMethod.invoke(migrationManager, "invalid") as Int)
    }
}