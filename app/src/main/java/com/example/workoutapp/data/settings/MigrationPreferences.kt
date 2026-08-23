package com.example.workoutapp.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val migrationCompleteKey = booleanPreferencesKey("migration_complete")
    private val lastStartDestinationKey = stringPreferencesKey("last_start_destination")

    val migrationComplete: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[migrationCompleteKey] ?: false }

    val lastStartDestination: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[lastStartDestinationKey] }

    suspend fun markMigrationComplete(destination: String?) {
        dataStore.edit { prefs ->
            prefs[migrationCompleteKey] = true
            if (destination != null) {
                prefs[lastStartDestinationKey] = destination
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(migrationCompleteKey)
            prefs.remove(lastStartDestinationKey)
        }
    }
}
