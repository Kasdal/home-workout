package com.example.workoutapp.util

import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.CalorieCategory
import com.example.workoutapp.model.ExerciseType
import com.example.workoutapp.model.UserMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieCalculatorTest {

    private fun calories(
        completedSets: Map<Int, Int>,
        exercises: List<Exercise>,
        userMetrics: UserMetrics? = null,
        restSecondsBetweenSets: Int = 30,
        restSecondsBetweenExercises: Int = 60,
        elapsedSeconds: Long = 1800,
        intensity: String = "normal"
    ): Float {
        return CalorieCalculator.calculateCalories(
            completedSets = completedSets,
            exercises = exercises,
            userMetrics = userMetrics,
            restSecondsBetweenSets = restSecondsBetweenSets,
            restSecondsBetweenExercises = restSecondsBetweenExercises,
            elapsedSeconds = elapsedSeconds,
            intensity = intensity
        )
    }

    private fun estimate(
        completedSets: Map<Int, Int>,
        exercises: List<Exercise>,
        userMetrics: UserMetrics? = null,
        restSecondsBetweenSets: Int = 30,
        restSecondsBetweenExercises: Int = 60,
        elapsedSeconds: Long = 1800,
        intensity: String = "normal"
    ): CalorieEstimate {
        return CalorieCalculator.calculateEstimate(
            completedSets = completedSets,
            exercises = exercises,
            userMetrics = userMetrics,
            restSecondsBetweenSets = restSecondsBetweenSets,
            restSecondsBetweenExercises = restSecondsBetweenExercises,
            elapsedSeconds = elapsedSeconds,
            intensity = intensity
        )
    }

    @Test
    fun `no completed sets returns minimum calories`() {
        val result = calories(emptyMap(), emptyList())
        assertEquals(1f, result, 0.01f)
    }

    @Test
    fun `no completed sets estimate returns minimum calories with standard metadata`() {
        val result = estimate(emptyMap(), emptyList(), intensity = "HARD")

        assertEquals(1f, result.calories, 0.01f)
        assertEquals(1, result.formulaVersion)
        assertEquals(CalorieEstimateMode.STANDARD_MET, result.mode)
        assertEquals(70f, result.userWeightKg, 0.01f)
        assertEquals(1f, result.metCorrectionFactor, 0.01f)
        assertEquals(0f, result.activeSeconds, 0.01f)
        assertEquals(0, result.restSeconds)
        assertEquals(0f, result.activeCalories, 0.01f)
        assertEquals(0f, result.restCalories, 0.01f)
        assertEquals("hard", result.intensity)
    }

    @Test
    fun `standard estimate returns split calorie values for elapsed workout`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 70f, reps = 10, sets = 1, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = estimate(
            completedSets = mapOf(1 to 1),
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 70f, gender = "Other"),
            elapsedSeconds = 60
        )

        assertEquals(5.39f, result.calories, 0.01f)
        assertEquals(30f, result.activeSeconds, 0.01f)
        assertEquals(30, result.restSeconds)
        assertEquals(4.29f, result.activeCalories, 0.01f)
        assertEquals(1.10f, result.restCalories, 0.01f)
        assertEquals(CalorieEstimateMode.STANDARD_MET, result.mode)
        assertEquals(1f, result.metCorrectionFactor, 0.01f)
        assertEquals("normal", result.intensity)
    }

    @Test
    fun `higher body weight increases calories`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Squat", weight = 100f, reps = 5, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )

        val lightUser = calories(mapOf(1 to 4), exercises, UserMetrics(weightKg = 60f, age = 30))
        val heavyUser = calories(mapOf(1 to 4), exercises, UserMetrics(weightKg = 100f, age = 30))

        assertTrue(heavyUser > lightUser)
    }

    @Test
    fun `more sets increase calories because active and rest time increase`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Row", weight = 60f, reps = 10, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )

        val fewSets = calories(mapOf(1 to 2), exercises, UserMetrics(weightKg = 70f, age = 30))
        val manySets = calories(mapOf(1 to 4), exercises, UserMetrics(weightKg = 70f, age = 30))

        assertTrue(manySets > fewSets)
    }

    @Test
    fun `positive elapsed time ignores programmed rest timer settings`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 80f, reps = 8, sets = 4, exerciseType = ExerciseType.STANDARD.name),
            Exercise(id = 2, name = "Plank", weight = 0f, reps = 1, sets = 3, exerciseType = ExerciseType.HOLD.name, holdDurationSeconds = 60)
        )

        val shortRests = calories(
            completedSets = mapOf(1 to 4, 2 to 3),
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 75f, age = 30),
            restSecondsBetweenSets = 15,
            restSecondsBetweenExercises = 30,
            elapsedSeconds = 1800
        )
        val longRests = calories(
            completedSets = mapOf(1 to 4, 2 to 3),
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 75f, age = 30),
            restSecondsBetweenSets = 30,
            restSecondsBetweenExercises = 60,
            elapsedSeconds = 1800
        )

        assertEquals(shortRests, longRests, 0.01f)
    }

    @Test
    fun `zero elapsed time uses programmed rest timer settings`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 80f, reps = 8, sets = 4, exerciseType = ExerciseType.STANDARD.name),
            Exercise(id = 2, name = "Plank", weight = 0f, reps = 1, sets = 3, exerciseType = ExerciseType.HOLD.name, holdDurationSeconds = 60)
        )

        val shortRests = estimate(
            completedSets = mapOf(1 to 4, 2 to 3),
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 75f, age = 30),
            restSecondsBetweenSets = 15,
            restSecondsBetweenExercises = 30,
            elapsedSeconds = 0
        )
        val longRests = estimate(
            completedSets = mapOf(1 to 4, 2 to 3),
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 75f, age = 30),
            restSecondsBetweenSets = 30,
            restSecondsBetweenExercises = 60,
            elapsedSeconds = 0
        )

        assertEquals(105, shortRests.restSeconds)
        assertEquals(210, longRests.restSeconds)
        assertTrue(longRests.calories > shortRests.calories)
    }

    @Test
    fun `missing profile defaults estimate user weight to 70kg`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Curl", weight = 10f, reps = 10, sets = 1, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = estimate(mapOf(1 to 1), exercises, userMetrics = null, elapsedSeconds = 30)

        assertEquals(70f, result.userWeightKg, 0.01f)
    }

    @Test
    fun `unknown intensity normalizes and uses normal multiplier`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Curl", weight = 10f, reps = 10, sets = 1, exerciseType = ExerciseType.STANDARD.name)
        )

        val normal = estimate(mapOf(1 to 1), exercises, UserMetrics(weightKg = 70f), elapsedSeconds = 30, intensity = "normal")
        val unknown = estimate(mapOf(1 to 1), exercises, UserMetrics(weightKg = 70f), elapsedSeconds = 30, intensity = "  Tempo  ")

        assertEquals(normal.calories, unknown.calories, 0.01f)
        assertEquals("normal", unknown.intensity)
    }

    @Test
    fun `heavy standard load caps MET at 7 point 5`() {
        val cappedLoad = listOf(
            Exercise(id = 1, name = "Deadlift", weight = 120f, reps = 10, sets = 1, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = estimate(
            completedSets = mapOf(1 to 1),
            exercises = cappedLoad,
            userMetrics = UserMetrics(weightKg = 60f, gender = "Other"),
            elapsedSeconds = 30
        )

        assertEquals(3.94f, result.calories, 0.01f)
        assertEquals(3.94f, result.activeCalories, 0.01f)
        assertEquals(0f, result.restCalories, 0.01f)
    }

    @Test
    fun `resistance calorie categories select different MET baselines`() {
        fun exercise(category: CalorieCategory) = Exercise(
            id = 1,
            name = category.name,
            weight = 35f,
            reps = 10,
            sets = 1,
            exerciseType = ExerciseType.STANDARD.name,
            calorieCategory = category.name
        )

        val user = UserMetrics(weightKg = 70f, gender = "Other")
        val light = estimate(mapOf(1 to 1), listOf(exercise(CalorieCategory.LIGHT_RESISTANCE)), user, elapsedSeconds = 30)
        val moderate = estimate(mapOf(1 to 1), listOf(exercise(CalorieCategory.MODERATE_RESISTANCE)), user, elapsedSeconds = 30)
        val heavy = estimate(mapOf(1 to 1), listOf(exercise(CalorieCategory.HEAVY_COMPOUND)), user, elapsedSeconds = 30)

        assertTrue(light.activeCalories < moderate.activeCalories)
        assertTrue(heavy.activeCalories > moderate.activeCalories)
    }

    @Test
    fun `bodyweight calorie categories select different MET baselines`() {
        fun exercise(category: CalorieCategory) = Exercise(
            id = 1,
            name = category.name,
            weight = 0f,
            reps = 15,
            sets = 1,
            exerciseType = ExerciseType.BODYWEIGHT.name,
            calorieCategory = category.name
        )

        val user = UserMetrics(weightKg = 70f, gender = "Other")
        val light = estimate(mapOf(1 to 1), listOf(exercise(CalorieCategory.LIGHT_BODYWEIGHT)), user, elapsedSeconds = 30)
        val vigorous = estimate(mapOf(1 to 1), listOf(exercise(CalorieCategory.VIGOROUS_BODYWEIGHT)), user, elapsedSeconds = 30)

        assertTrue(light.activeCalories < vigorous.activeCalories)
    }

    @Test
    fun `null calorie category keeps previous defaults by exercise type`() {
        val user = UserMetrics(weightKg = 70f, gender = "Other")
        val defaultStandard = Exercise(id = 1, name = "Bench", weight = 70f, reps = 10, sets = 1, exerciseType = ExerciseType.STANDARD.name)
        val moderateStandard = defaultStandard.copy(calorieCategory = CalorieCategory.MODERATE_RESISTANCE.name)
        val defaultBodyweight = Exercise(id = 1, name = "Push-up", weight = 0f, reps = 15, sets = 1, exerciseType = ExerciseType.BODYWEIGHT.name)
        val vigorousBodyweight = defaultBodyweight.copy(calorieCategory = CalorieCategory.VIGOROUS_BODYWEIGHT.name)
        val defaultHold = Exercise(id = 1, name = "Plank", weight = 0f, reps = 1, sets = 1, exerciseType = ExerciseType.HOLD.name, holdDurationSeconds = 30)
        val isometricHold = defaultHold.copy(calorieCategory = CalorieCategory.ISOMETRIC_HOLD.name)

        assertEquals(
            estimate(mapOf(1 to 1), listOf(moderateStandard), user, elapsedSeconds = 30).activeCalories,
            estimate(mapOf(1 to 1), listOf(defaultStandard), user, elapsedSeconds = 30).activeCalories,
            0.01f
        )
        assertEquals(
            estimate(mapOf(1 to 1), listOf(vigorousBodyweight), user, elapsedSeconds = 30).activeCalories,
            estimate(mapOf(1 to 1), listOf(defaultBodyweight), user, elapsedSeconds = 30).activeCalories,
            0.01f
        )
        assertEquals(
            estimate(mapOf(1 to 1), listOf(isometricHold), user, elapsedSeconds = 30).activeCalories,
            estimate(mapOf(1 to 1), listOf(defaultHold), user, elapsedSeconds = 30).activeCalories,
            0.01f
        )
    }

    @Test
    fun `complete male profile uses personalized MET correction`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 70f, reps = 10, sets = 1, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = estimate(
            completedSets = mapOf(1 to 1),
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 70f, heightCm = 175f, age = 40, gender = "Male"),
            elapsedSeconds = 60
        )

        assertEquals(CalorieEstimateMode.PERSONALIZED_MET, result.mode)
        assertEquals(1.08f, result.metCorrectionFactor, 0.01f)
        assertEquals(5.82f, result.calories, 0.01f)
    }

    @Test
    fun `other gender falls back to standard MET correction`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 70f, reps = 10, sets = 1, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = estimate(
            completedSets = mapOf(1 to 1),
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 70f, heightCm = 175f, age = 40, gender = "Other"),
            elapsedSeconds = 60
        )

        assertEquals(CalorieEstimateMode.STANDARD_MET, result.mode)
        assertEquals(1f, result.metCorrectionFactor, 0.01f)
    }

    @Test
    fun `invalid profile values fall back to standard MET correction`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 70f, reps = 10, sets = 1, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = estimate(
            completedSets = mapOf(1 to 1),
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 70f, heightCm = 0f, age = 40, gender = "Male"),
            elapsedSeconds = 60
        )

        assertEquals(CalorieEstimateMode.STANDARD_MET, result.mode)
        assertEquals(1f, result.metCorrectionFactor, 0.01f)
    }

    @Test
    fun `personalized MET correction is clamped`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 70f, reps = 10, sets = 1, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = estimate(
            completedSets = mapOf(1 to 1),
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 220f, heightCm = 120f, age = 90, gender = "Female"),
            elapsedSeconds = 60
        )

        assertEquals(CalorieEstimateMode.PERSONALIZED_MET, result.mode)
        assertEquals(1.25f, result.metCorrectionFactor, 0.01f)
    }

    @Test
    fun `bodyweight work burns more than hold work for same set count`() {
        val bodyweight = listOf(
            Exercise(id = 1, name = "Push-up", weight = 0f, reps = 15, sets = 4, exerciseType = ExerciseType.BODYWEIGHT.name)
        )
        val hold = listOf(
            Exercise(id = 1, name = "Plank", weight = 0f, reps = 1, sets = 4, exerciseType = ExerciseType.HOLD.name, holdDurationSeconds = 20)
        )

        val bodyweightCalories = calories(mapOf(1 to 4), bodyweight, UserMetrics(weightKg = 70f, age = 30))
        val holdCalories = calories(mapOf(1 to 4), hold, UserMetrics(weightKg = 70f, age = 30))

        assertTrue(bodyweightCalories > holdCalories)
    }

    @Test
    fun `longer actual duration increases calories`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 80f, reps = 8, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )

        val shortSession = calories(mapOf(1 to 4), exercises, UserMetrics(weightKg = 80f), elapsedSeconds = 900)
        val longSession = calories(mapOf(1 to 4), exercises, UserMetrics(weightKg = 80f), elapsedSeconds = 3600)

        assertTrue(longSession > shortSession)
    }

    @Test
    fun `hard intensity increases calories`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Squat", weight = 100f, reps = 8, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )

        val easy = calories(mapOf(1 to 4), exercises, UserMetrics(weightKg = 80f), intensity = "easy")
        val hard = calories(mapOf(1 to 4), exercises, UserMetrics(weightKg = 80f), intensity = "hard")

        assertTrue(hard > easy)
    }

    @Test
    fun `heavier standard load increases calories`() {
        val light = listOf(
            Exercise(id = 1, name = "Curl", weight = 10f, reps = 10, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )
        val heavy = listOf(
            Exercise(id = 1, name = "Deadlift", weight = 120f, reps = 10, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )

        val lightCalories = calories(mapOf(1 to 4), light, UserMetrics(weightKg = 80f))
        val heavyCalories = calories(mapOf(1 to 4), heavy, UserMetrics(weightKg = 80f))

        assertTrue(heavyCalories > lightCalories)
    }

    @Test
    fun `realistic hour-long structured workout stays in realistic range`() {
        val exercises = buildList {
            repeat(11) { index ->
                add(
                    Exercise(
                        id = index + 1,
                        name = "Lift ${index + 1}",
                        weight = 50f,
                        reps = 13,
                        sets = 4,
                        exerciseType = ExerciseType.STANDARD.name
                    )
                )
            }
            add(Exercise(id = 12, name = "Push-up", weight = 0f, reps = 15, sets = 2, exerciseType = ExerciseType.BODYWEIGHT.name))
            add(Exercise(id = 13, name = "Plank", weight = 0f, reps = 1, sets = 1, exerciseType = ExerciseType.HOLD.name, holdDurationSeconds = 180))
        }

        val completedSets = (1..11).associateWith { 4 }.toMutableMap().apply {
            this[12] = 2
            this[13] = 1
        }

        val result = calories(
            completedSets = completedSets,
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 100f, age = 44),
            restSecondsBetweenSets = 30,
            restSecondsBetweenExercises = 60
        )

        assertTrue(result in 250f..450f)
    }
}
