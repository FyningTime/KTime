package technology.iatlas.ktime.ui.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import technology.iatlas.ktime.data.Setting
import technology.iatlas.ktime.data.repositories.SettingRepository
import technology.iatlas.ktime.data.repositories.SettingsManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepository: MockSettingRepository
    private lateinit var settingsManager: SettingsManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = MockSettingRepository()
        settingsManager = SettingsManager(mockRepository)
        viewModel = SettingsViewModel(settingsManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test loadSettings loads settings from manager`() = runTest {
        // Given
        mockRepository.settingsToReturn = mapOf(
            "workHoursPerWeek" to "35",
            "locale" to "de-DE",
            "dateFormat" to "dd.MM.yyyy"
        )

        // When
        viewModel.loadSettings()
        testDispatcher.scheduler.advanceUntilIdle() // Wait for coroutines to complete

        // Then
        assertEquals("35", viewModel.settings["workHoursPerWeek"])
        assertEquals("de-DE", viewModel.settings["locale"])
        assertEquals("dd.MM.yyyy", viewModel.settings["dateFormat"])
    }

    @Test
    fun `test saveSettings updates settings in viewModel`() = runTest {
        // Given
        val workHoursPerWeek = "38"
        val locale = "fr-FR"
        val dateFormat = "MM/dd/yyyy"

        // When
        viewModel.saveSettings(workHoursPerWeek, locale, dateFormat)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for coroutines to complete

        // Then
        assertEquals(workHoursPerWeek, viewModel.settings["workHoursPerWeek"])
        assertEquals(locale, viewModel.settings["locale"])
        assertEquals(dateFormat, viewModel.settings["dateFormat"])

        // Verify settings were saved to the repository
        assertTrue(mockRepository.savedSettings.containsKey("workHoursPerWeek"))
        assertTrue(mockRepository.savedSettings.containsKey("locale"))
        assertTrue(mockRepository.savedSettings.containsKey("dateFormat"))
        assertEquals(workHoursPerWeek, mockRepository.savedSettings["workHoursPerWeek"])
        assertEquals(locale, mockRepository.savedSettings["locale"])
        assertEquals(dateFormat, mockRepository.savedSettings["dateFormat"])
    }

    // Mock implementation of SettingRepository for testing
    private class MockSettingRepository : SettingRepository {
        var settingsToReturn: Map<String, String> = emptyMap()
        val savedSettings: MutableMap<String, String> = mutableMapOf()

        override suspend fun getSetting(key: String): Setting? {
            return if (settingsToReturn.containsKey(key)) {
                Setting(0, key, settingsToReturn[key]!!)
            } else {
                null
            }
        }

        override suspend fun saveSetting(key: String, value: String) {
            savedSettings[key] = value
        }

        override suspend fun getAllSettings(): List<Setting> {
            return settingsToReturn.map { (key, value) ->
                Setting(0, key, value)
            }
        }

        override suspend fun deleteSetting(key: String) {
            savedSettings.remove(key)
        }
    }
}
