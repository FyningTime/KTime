package technology.iatlas.ktime.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import technology.iatlas.ktime.data.Setting
import technology.iatlas.ktime.database.Settings

private val repositoryLogger = KotlinLogging.logger {}

/**
 * Repository interface for settings.
 */
interface SettingRepository {
    /**
     * Gets a setting by key.
     */
    suspend fun getSetting(key: String): Setting?

    /**
     * Saves a setting with the given key and value.
     */
    suspend fun saveSetting(key: String, value: String)

    /**
     * Gets all settings.
     */
    suspend fun getAllSettings(): List<Setting>

    /**
     * Deletes a setting by key.
     */
    suspend fun deleteSetting(key: String)
}

/**
 * SQLite implementation of the SettingRepository.
 */
class SqliteSettingRepository(private val database: Database) : SettingRepository {
    override suspend fun getSetting(key: String): Setting? = withContext(Dispatchers.IO) {
        repositoryLogger.debug { "Getting setting with key: $key" }
        try {
            val result = transaction(database) {
                Settings.selectAll().where { Settings.key eq key }
                    .map {
                        Setting(
                            id = it[Settings.id],
                            key = it[Settings.key],
                            value = it[Settings.value]
                        )
                    }
                    .singleOrNull()
            }
            if (result != null) {
                repositoryLogger.debug { "Found setting: $result" }
            } else {
                repositoryLogger.debug { "Setting with key '$key' not found" }
            }
            return@withContext result
        } catch (e: Exception) {
            repositoryLogger.error(e) { "Error getting setting with key: $key" }
            return@withContext null
        }
    }

    override suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        repositoryLogger.debug { "Saving setting: key=$key, value=$value" }
        try {
            transaction(database) {
                // Try to update first
                val updated = Settings.update({ Settings.key eq key }) {
                    it[Settings.value] = value
                }

                // If no rows updated, insert new setting
                if (updated == 0) {
                    repositoryLogger.debug { "Setting with key '$key' not found, inserting new setting" }
                    Settings.insert {
                        it[Settings.key] = key
                        it[Settings.value] = value
                    }
                    repositoryLogger.info { "Inserted new setting: key=$key, value=$value" }
                } else {
                    repositoryLogger.info { "Updated existing setting: key=$key, value=$value" }
                }
            }
        } catch (e: Exception) {
            repositoryLogger.error(e) { "Error saving setting: key=$key, value=$value" }
        }
    }

    override suspend fun getAllSettings(): List<Setting> = withContext(Dispatchers.IO) {
        repositoryLogger.debug { "Getting all settings" }
        try {
            val settings = transaction(database) {
                Settings.selectAll()
                    .map {
                        Setting(
                            id = it[Settings.id],
                            key = it[Settings.key],
                            value = it[Settings.value]
                        )
                    }
                    .toList()
            }
            repositoryLogger.debug { "Retrieved ${settings.size} settings" }
            return@withContext settings
        } catch (e: Exception) {
            repositoryLogger.error(e) { "Error getting all settings" }
            return@withContext emptyList()
        }
    }

    override suspend fun deleteSetting(key: String) {
        repositoryLogger.debug { "Deleting setting with key: $key" }
        withContext(Dispatchers.IO) {
            try {
                transaction(database) {
                    // Use a simpler approach with direct SQL
                    val query = "DELETE FROM ${Settings.tableName} WHERE ${Settings.key.name} = '$key'"
                    exec(query)
                    repositoryLogger.info { "Deleted setting with key: $key" }
                }
            } catch (e: Exception) {
                repositoryLogger.error(e) { "Error deleting setting with key: $key" }
            }
        }
    }
}

/**
 * Helper class to manage settings with type conversion.
 */
class SettingsManager(private val repository: SettingRepository) {
    private val logger = KotlinLogging.logger {}

    // Default values
    private val defaults = mapOf(
        "workHoursPerWeek" to "40",
        "locale" to "en-US",
        "dateFormat" to "yyyy-MM-dd"
    )

    /**
     * Gets an integer setting.
     */
    suspend fun getInt(key: String): Int {
        logger.debug { "Getting integer setting: $key" }
        val setting = repository.getSetting(key)
        val result = if (setting != null) {
            val intValue = setting.value.toIntOrNull()
            if (intValue != null) {
                logger.debug { "Found integer setting: $key = $intValue" }
                intValue
            } else {
                logger.warn { "Setting $key exists but value '${setting.value}' is not a valid integer, using default" }
                defaults[key]?.toIntOrNull() ?: 0
            }
        } else {
            logger.debug { "Setting $key not found, using default value: ${defaults[key]}" }
            defaults[key]?.toIntOrNull() ?: 0
        }
        return result
    }

    /**
     * Gets a float setting.
     */
    suspend fun getFloat(key: String): Float {
        logger.debug { "Getting float setting: $key" }
        val setting = repository.getSetting(key)
        val result = if (setting != null) {
            val floatValue = setting.value.toFloatOrNull()
            if (floatValue != null) {
                logger.debug { "Found float setting: $key = $floatValue" }
                floatValue
            } else {
                logger.warn { "Setting $key exists but value '${setting.value}' is not a valid float, using default" }
                defaults[key]?.toFloatOrNull() ?: 0f
            }
        } else {
            logger.debug { "Setting $key not found, using default value: ${defaults[key]}" }
            defaults[key]?.toFloatOrNull() ?: 0f
        }
        return result
    }

    /**
     * Gets a string setting.
     */
    suspend fun getString(key: String): String {
        logger.debug { "Getting string setting: $key" }
        val setting = repository.getSetting(key)
        val result = if (setting != null) {
            logger.debug { "Found string setting: $key = ${setting.value}" }
            setting.value
        } else {
            logger.debug { "Setting $key not found, using default value: ${defaults[key]}" }
            defaults[key] ?: ""
        }
        return result
    }

    /**
     * Saves a setting with the given key and value.
     */
    suspend fun saveSetting(key: String, value: Any) {
        logger.debug { "Saving setting: $key = $value" }
        try {
            repository.saveSetting(key, value.toString())
            logger.info { "Successfully saved setting: $key = $value" }
        } catch (e: Exception) {
            logger.error(e) { "Error saving setting: $key = $value" }
        }
    }

    /**
     * Gets all settings as a map.
     */
    suspend fun getAllSettings(): Map<String, String> {
        logger.debug { "Getting all settings" }
        try {
            val settings = repository.getAllSettings()
            val result = settings.associate { it.key to it.value }
            logger.debug { "Retrieved ${result.size} settings" }
            return result
        } catch (e: Exception) {
            logger.error(e) { "Error getting all settings, returning empty map" }
            return emptyMap()
        }
    }
}
