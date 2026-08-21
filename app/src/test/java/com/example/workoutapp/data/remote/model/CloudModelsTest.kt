package com.example.workoutapp.data.remote.model

import com.example.workoutapp.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudModelsTest {

    @Test
    fun `Exercise round-trips activeInSession through CloudExercise`() {
        val source = Exercise(
            id = 7,
            name = "Bench Press",
            weight = 60f,
            reps = 8,
            sets = 5,
            activeInSession = false,
            categoryId = "push"
        )

        val roundTrip = source.toCloud().toLocal()

        assertEquals(false, roundTrip.activeInSession)
        assertEquals("push", roundTrip.categoryId)
        assertEquals(source, roundTrip)
    }

    @Test
    fun `CloudExercise defaults preserve legacy behaviour when fields missing`() {
        val cloud = CloudExercise(id = 3, name = "Squat", weight = 80f)

        val local = cloud.toLocal()

        assertEquals(true, local.activeInSession)
        assertEquals(null, local.categoryId)
    }

    @Test
    fun `Category round-trips through CloudCategory preserving all fields`() {
        val source = com.example.workoutapp.model.Category(
            id = "legs",
            name = "Legs",
            iconName = "DirectionsRun",
            sortOrder = 2,
            isLegacy = false,
            isDeleted = false
        )

        val roundTrip = source.toCloud().toLocal()

        assertEquals(source, roundTrip)
    }

    @Test
    fun `Legacy CloudCategory flag survives round-trip`() {
        val source = com.example.workoutapp.model.Category(
            id = "legacy",
            name = "Legacy",
            iconName = "History",
            sortOrder = 999,
            isLegacy = true
        )

        val roundTrip = source.toCloud().toLocal()

        assertEquals(true, roundTrip.isLegacy)
        assertEquals(source, roundTrip)
    }
}
