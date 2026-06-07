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
}
