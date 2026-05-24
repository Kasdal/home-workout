package com.example.workoutapp.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateCheckPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val lastCheckTimestampKey = longPreferencesKey("last_check_timestamp")
    private val lastSeenVersionKey = stringPreferencesKey("last_seen_version")
    private val skippedVersionKey = stringPreferencesKey("skipped_version")
    private val cachedChangelogMdKey = stringPreferencesKey("cached_changelog_md")
    private val cachedChangelogVersionKey = stringPreferencesKey("cached_changelog_version")

    val lastCheckTimestamp: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[lastCheckTimestampKey] ?: 0L }

    val lastSeenVersion: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[lastSeenVersionKey] }

    val skippedVersion: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[skippedVersionKey] }

    val cachedChangelogMd: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[cachedChangelogMdKey] }

    val cachedChangelogVersion: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[cachedChangelogVersionKey] }

    suspend fun setLastCheckTimestamp(timestamp: Long) {
        dataStore.edit { it[lastCheckTimestampKey] = timestamp }
    }

    suspend fun setLastSeenVersion(version: String) {
        dataStore.edit { it[lastSeenVersionKey] = version }
    }

    suspend fun setSkippedVersion(version: String) {
        dataStore.edit { it[skippedVersionKey] = version }
    }

    suspend fun setCachedChangelog(version: String, changelogMd: String) {
        dataStore.edit {
            it[cachedChangelogVersionKey] = version
            it[cachedChangelogMdKey] = changelogMd
        }
    }
}
