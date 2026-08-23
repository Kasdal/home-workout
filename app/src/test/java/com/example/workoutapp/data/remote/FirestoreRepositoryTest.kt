package com.example.workoutapp.data.remote

import com.example.workoutapp.data.remote.model.CloudMigrationMeta
import com.example.workoutapp.data.settings.WorkoutSessionSettings
import com.example.workoutapp.model.Exercise
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
            WorkoutSessionSettings(restTimerDuration = 45, exerciseSwitchDuration = 120, undoLastSetEnabled = false, calorieIntensity = "hard"),
            syncedWorkoutSettingsEvent(settingsSnapshot(45, 120, false, "hard"), null)
        )
    }

    @Test
    fun `allocation starts at the seed when the counter doc does not exist`() {
        assertEquals(8L, resolveAllocationStart(counterExists = false, storedNextId = null, staleSeed = 8L))
    }

    @Test
    fun `allocation prefers a committed counter value over a stale seed`() {
        assertEquals(12L, resolveAllocationStart(counterExists = true, storedNextId = 12L, staleSeed = 11L))
    }

    @Test
    fun `concurrent allocators seeded from the same max never receive duplicate ids`() {
        val maxStoredId = 10L
        val count = 3

        val deviceASeed = maxStoredId + 1
        val deviceBSeed = maxStoredId + 1

        val startA = resolveAllocationStart(counterExists = false, storedNextId = null, staleSeed = deviceASeed)
        val nextAfterA = startA + count

        val startB = resolveAllocationStart(counterExists = true, storedNextId = nextAfterA, staleSeed = deviceBSeed)
        val idsB = startB until (startB + count)

        assertEquals(11L, startA)
        assertTrue(idsB.none { id -> id >= startA && id < nextAfterA })
        assertEquals(14L, startB)
    }

    @Test
    fun `performInitialMigration aborts with zero writes when account already has data`() {
        val fixture = migrationFixture(
            counts = mapOf(
                "profiles" to 1,
                "exercises" to 3,
                "sessions" to 0,
                "sessionExercises" to 5,
                "restDays" to 0
            )
        )

        assertThrows(MigrationConflictException::class.java) {
            runBlocking {
                fixture.repository.performInitialMigration(
                    uid = "user-123",
                    userMetrics = emptyList(),
                    exercises = listOf(Exercise(name = "Squat", weight = 60f)),
                    sessions = emptyList(),
                    sessionExercises = emptyList(),
                    restDays = emptyList(),
                    settings = null,
                    force = true
                )
            }
        }

        verify(exactly = 0) { fixture.firestore.batch() }
        verify(exactly = 0) { fixture.metaDoc.set(any<CloudMigrationMeta>()) }
    }

    @Test
    fun `performInitialMigration imports into an empty account`() {
        val fixture = migrationFixture(
            counts = mapOf(
                "profiles" to 0,
                "exercises" to 0,
                "sessions" to 0,
                "sessionExercises" to 0,
                "restDays" to 0
            )
        )

        val batch = mockk<WriteBatch>()
        every { fixture.firestore.batch() } returns batch
        every { batch.set(any(), any<Any>()) } returns batch
        every { batch.commit() } returns Tasks.forResult<Void>(null)
        every { fixture.metaDoc.set(any<CloudMigrationMeta>()) } returns Tasks.forResult<Void>(null)

        runBlocking {
            fixture.repository.performInitialMigration(
                uid = "user-123",
                userMetrics = emptyList(),
                exercises = listOf(Exercise(name = "Squat", weight = 60f)),
                sessions = emptyList(),
                sessionExercises = emptyList(),
                restDays = emptyList(),
                settings = null,
                force = true
            )
        }

        verify(exactly = 1) { fixture.firestore.batch() }
        verify(exactly = 1) { fixture.metaDoc.set(any<CloudMigrationMeta>()) }
    }

    private data class MigrationFixture(
        val repository: FirestoreRepository,
        val firestore: FirebaseFirestore,
        val metaDoc: DocumentReference
    )

    private fun migrationFixture(counts: Map<String, Int>): MigrationFixture {
        val firestore = mockk<FirebaseFirestore>()
        val usersCol = mockk<CollectionReference>()
        val rootDoc = mockk<DocumentReference>()

        every { firestore.collection("users") } returns usersCol
        every { usersCol.document("user-123") } returns rootDoc

        counts.forEach { (name, count) -> stubRemoteCount(rootDoc, name, count) }

        val metaCol = mockk<CollectionReference>()
        val metaDoc = mockk<DocumentReference>()
        every { rootDoc.collection("meta") } returns metaCol
        every { metaCol.document("migration") } returns metaDoc

        return MigrationFixture(FirestoreRepository(firestore), firestore, metaDoc)
    }

    private fun stubRemoteCount(rootDoc: DocumentReference, collectionName: String, count: Int) {
        val collection = mockk<CollectionReference>()
        val snapshot = mockk<QuerySnapshot>()
        every { snapshot.size() } returns count
        every { collection.get() } returns Tasks.forResult(snapshot)
        every { collection.document(any()) } returns mockk()
        every { rootDoc.collection(collectionName) } returns collection
    }

    private fun missingSettingsSnapshot(): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { snapshot.exists() } returns false
        return snapshot
    }

    private fun settingsSnapshot(
        restTimerDuration: Int,
        exerciseSwitchDuration: Int,
        undoLastSetEnabled: Boolean,
        calorieIntensity: String = "normal"
    ): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { snapshot.exists() } returns true
        every { snapshot.getLong("restTimerDuration") } returns restTimerDuration.toLong()
        every { snapshot.getLong("exerciseSwitchDuration") } returns exerciseSwitchDuration.toLong()
        every { snapshot.getBoolean("undoLastSetEnabled") } returns undoLastSetEnabled
        every { snapshot.getString("calorieIntensity") } returns calorieIntensity
        return snapshot
    }
}
