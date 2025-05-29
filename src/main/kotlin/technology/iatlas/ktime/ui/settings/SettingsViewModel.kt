package technology.iatlas.ktime.ui.settings

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import mu.KotlinLogging
import technology.iatlas.ktime.data.repositories.SettingsManager

private val logger = KotlinLogging.logger {}

/**
 * ViewModel for the Settings tab.
 */
class SettingsViewModel(private val settingsManager: SettingsManager) {
    // Settings as a mutable state
    private val _settings = mutableStateMapOf<String, String>()
    val settings = _settings

    // Coroutine scope for launching coroutines
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    // Load settings from the database
    fun loadSettings() {
        logger.debug { "Loading settings from database" }
        viewModelScope.launch {
            val defaultSettings = mapOf(
                "workHoursPerWeek" to "40",
                "workDayStartTime" to "09:00",
                "locale" to "en-US",
                "dateFormat" to "yyyy-MM-dd" // Default date format
            )

            try {
                // Load settings from database
                val loadedSettings = settingsManager.getAllSettings()
                logger.debug { "Successfully loaded settings from database: $loadedSettings" }

                // Apply loaded settings or defaults if not found
                _settings.clear()
                defaultSettings.forEach { (key, defaultValue) ->
                    _settings[key] = loadedSettings[key] ?: defaultValue
                }
                logger.info { "Settings initialized with values: $_settings" }
            } catch (e: Exception) {
                // Handle database errors
                logger.error(e) { "Error loading settings" }
                // Fall back to defaults
                _settings.clear()
                _settings.putAll(defaultSettings)
                logger.info { "Falling back to default settings: $defaultSettings" }
            }
        }
    }

    // Save settings to the database
    fun saveSettings(
        workHoursPerWeek: String,
        locale: String,
        dateFormat: String
    ) {
        logger.debug { "Saving settings: workHoursPerWeek=$workHoursPerWeek, locale=$locale, dateFormat=$dateFormat" }
        viewModelScope.launch {
            try {
                // Update local state
                val updatedSettings = mapOf(
                    "workHoursPerWeek" to workHoursPerWeek,
                    "locale" to locale,
                    "dateFormat" to dateFormat
                )

                // Save to database
                logger.debug { "Saving settings to database" }
                updatedSettings.forEach { (key, value) ->
                    settingsManager.saveSetting(key, value)
                }

                // Update local state after successful save
                _settings.clear()
                _settings.putAll(updatedSettings)
                logger.info { "Settings successfully saved and updated: $_settings" }
            } catch (e: Exception) {
                // Handle database errors
                logger.error(e) { "Error saving settings" }
            }
        }
    }

    // Get date format or default
    fun getDateFormat(): String {
        val format = settings["dateFormat"] ?: "yyyy-MM-dd"
        logger.debug { "Using date format: $format" }
        return format
    }

    // Format date using the configured format
    fun formatDate(date: LocalDate): String {
        logger.debug { "Formatting date: $date" }
        return try {
            val format = getDateFormat()
            val formattedDate = when (format) {
                "yyyy-MM-dd" -> "${date.year}-${
                    date.monthNumber.toString().padStart(2, '0')
                }-${date.dayOfMonth.toString().padStart(2, '0')}"

                "dd.MM.yyyy" -> "${date.dayOfMonth.toString().padStart(2, '0')}.${
                    date.monthNumber.toString().padStart(2, '0')
                }.${date.year}"

                "MM/dd/yyyy" -> "${date.monthNumber.toString().padStart(2, '0')}/${
                    date.dayOfMonth.toString().padStart(2, '0')
                }/${date.year}"

                "dd/MM/yyyy" -> "${date.dayOfMonth.toString().padStart(2, '0')}/${
                    date.monthNumber.toString().padStart(2, '0')
                }/${date.year}"

                else -> {
                    logger.warn { "Unknown date format: $format, falling back to ISO format" }
                    date.toString() // Fallback to ISO format
                }
            }
            logger.debug { "Date formatted as: $formattedDate" }
            formattedDate
        } catch (e: Exception) {
            logger.error(e) { "Error formatting date: $date" }
            date.toString() // Fallback to ISO format
        }
    }
}
