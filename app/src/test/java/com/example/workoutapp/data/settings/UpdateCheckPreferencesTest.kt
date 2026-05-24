package com.example.workoutapp.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateCheckPreferencesTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `defaults return null or zero`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempFolder.newFile("update_check_test_1.preferences_pb") }
        )
        val prefs = UpdateCheckPreferences(dataStore)
        assertEquals(0L, prefs.lastCheckTimestamp.first())
        assertNull(prefs.lastSeenVersion.first())
        assertNull(prefs.skippedVersion.first())
        assertNull(prefs.cachedChangelogMd.first())
        assertNull(prefs.cachedChangelogVersion.first())
    }

    @Test
    fun `setLastCheckTimestamp stores and retrieves value`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempFolder.newFile("update_check_test_2.preferences_pb") }
        )
        val prefs = UpdateCheckPreferences(dataStore)
        val timestamp = 1717000000000L
        prefs.setLastCheckTimestamp(timestamp)
        assertEquals(timestamp, prefs.lastCheckTimestamp.first())
    }

    @Test
    fun `setLastSeenVersion stores and retrieves value`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempFolder.newFile("update_check_test_3.preferences_pb") }
        )
        val prefs = UpdateCheckPreferences(dataStore)
        prefs.setLastSeenVersion("1.2.0")
        assertEquals("1.2.0", prefs.lastSeenVersion.first())
    }

    @Test
    fun `setSkippedVersion stores and retrieves value`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempFolder.newFile("update_check_test_4.preferences_pb") }
        )
        val prefs = UpdateCheckPreferences(dataStore)
        prefs.setSkippedVersion("1.3.0")
        assertEquals("1.3.0", prefs.skippedVersion.first())
    }

    @Test
    fun `setCachedChangelog stores both md and version`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempFolder.newFile("update_check_test_5.preferences_pb") }
        )
        val prefs = UpdateCheckPreferences(dataStore)
        prefs.setCachedChangelog("1.2.0", "## What's New\n- Feature A\n- Feature B")
        assertEquals("1.2.0", prefs.cachedChangelogVersion.first())
        assertEquals("## What's New\n- Feature A\n- Feature B", prefs.cachedChangelogMd.first())
    }
}
