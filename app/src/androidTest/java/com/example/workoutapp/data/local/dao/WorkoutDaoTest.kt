package com.example.workoutapp.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.workoutapp.data.local.WorkoutDatabase
import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.local.entity.WorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {
    private lateinit var workoutDao: WorkoutDao
    private lateinit var db: WorkoutDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, WorkoutDatabase::class.java
        ).build()
        workoutDao = db.workoutDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetExercise() = runBlocking {
        val exercise = Exercise(name = "Push Up", weight = 0f)
        workoutDao.insertExercise(exercise)
        val allExercises = workoutDao.getAllExercises().first()
        assertEquals(allExercises[0].name, "Push Up")
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetSession() = runBlocking {
        val session = WorkoutSession(
            date = System.currentTimeMillis(),
            durationSeconds = 3600,
            totalWeightLifted = 1000f,
            caloriesBurned = 500f,
            totalVolume = 1000f
        )
        workoutDao.insertSession(session)
        val allSessions = workoutDao.getAllSessions().first()
        assertEquals(allSessions[0].durationSeconds, 3600L)
    }

    @Test
    @Throws(Exception::class)
    fun userMetricsOperations() = runBlocking {
        // Initial state should be empty
        val initialMetrics = workoutDao.getUserMetrics().first()
        assertNull(initialMetrics)

        // Insert
        val metrics = UserMetrics(weightKg = 75f, heightCm = 180f, age = 30, gender = "Male")
        workoutDao.insertUserMetrics(metrics)

        // Verify insertion
        val savedMetrics = workoutDao.getUserMetrics().first()
        assertNotNull(savedMetrics)
        assertEquals(75f, savedMetrics?.weightKg)
    }
}
