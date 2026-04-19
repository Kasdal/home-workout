package com.example.workoutapp.data.remote

import com.example.workoutapp.data.settings.WorkoutSessionSettings
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirestoreRepositoryTest {

    @Test
    fun `syncedWorkoutSettingsEvent emits defaults when settings doc is missing`() {
        assertEquals(WorkoutSessionSettings(), syncedWorkoutSettingsEvent(missingSettingsSnapshot(), null))
    }

    @Test
    fun `syncedWorkoutSettingsEvent ignores listener errors`() {
        assertNull(syncedWorkoutSettingsEvent(settingsSnapshot(45, 120, false), mockk<FirebaseFirestoreException>(relaxed = true)))
    }

    @Test
    fun `syncedWorkoutSettingsEvent maps snapshot values`() {
        assertEquals(
            WorkoutSessionSettings(restTimerDuration = 45, exerciseSwitchDuration = 120, undoLastSetEnabled = false),
            syncedWorkoutSettingsEvent(settingsSnapshot(45, 120, false), null)
        )
    }

    private fun missingSettingsSnapshot(): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { snapshot.exists() } returns false
        return snapshot
    }

    private fun settingsSnapshot(restTimerDuration: Int, exerciseSwitchDuration: Int, undoLastSetEnabled: Boolean): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { snapshot.exists() } returns true
        every { snapshot.getLong("restTimerDuration") } returns restTimerDuration.toLong()
        every { snapshot.getLong("exerciseSwitchDuration") } returns exerciseSwitchDuration.toLong()
        every { snapshot.getBoolean("undoLastSetEnabled") } returns undoLastSetEnabled
        return snapshot
    }
}
