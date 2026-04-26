package com.example.workoutapp.data.remote.model

import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.RestDay
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.model.Settings
import com.example.workoutapp.model.UserMetrics
import com.example.workoutapp.model.WorkoutSession

data class CloudUserMetrics(
    val id: Int = 0,
    val name: String? = null,
    val weightKg: Float = 0f,
    val heightCm: Float = 170f,
    val age: Int = 30,
    val gender: String = "Male",
    val isActive: Boolean = false
)

data class CloudExercise(
    val id: Int = 0,
    val name: String = "",
    val weight: Float = 0f,
    val reps: Int = 13,
    val sets: Int = 4,
    val exerciseType: String = "STANDARD",
    val usesSensor: Boolean = true,
    val holdDurationSeconds: Int = 30,
    val deleted: Boolean = false,
    val photoUri: String? = null,
    val sortOrder: Int = Int.MAX_VALUE
)

data class CloudWorkoutSession(
    val id: Int = 0,
    val date: Long = 0L,
    val durationSeconds: Long = 0L,
    val totalWeightLifted: Float = 0f,
    val caloriesBurned: Float = 0f,
    val notes: String? = null,
    val isPaused: Boolean = false,
    val pausedAt: Long? = null,
    val timeOfDay: Int = 0,
    val totalVolume: Float = 0f
)

data class CloudSettings(
    val id: Int = 1,
    val soundsEnabled: Boolean = true,
    val soundVolume: Float = 1f,
    val timerSoundType: String = "beep",
    val celebrationSoundType: String = "cheer",
    val themeMode: String = "dark",
    val tutorialCompleted: Boolean = false,
    val tutorialVersion: Int = 1,
    val restTimerDuration: Int = 30,
    val exerciseSwitchDuration: Int = 90,
    val undoLastSetEnabled: Boolean = true,
    val sensorEnabled: Boolean = false,
    val sensorIpAddress: String = "192.168.0.125",
    val calorieIntensity: String = "normal"
)

data class CloudRestDay(
    val id: Int = 0,
    val date: Long = 0L,
    val note: String? = null
)

data class CloudSessionExercise(
    val id: Int = 0,
    val sessionId: Int = 0,
    val exerciseName: String = "",
    val weight: Float = 0f,
    val sets: Int = 0,
    val reps: Int = 0,
    val volume: Float = 0f,
    val sortOrder: Int = 0
)

data class CloudMigrationMeta(
    val migrationComplete: Boolean = false,
    val backupImportPending: Boolean = false,
    val migratedAt: Long = 0L,
    val userMetricsCount: Int = 0,
    val exercisesCount: Int = 0,
    val sessionsCount: Int = 0,
    val sessionExercisesCount: Int = 0,
    val restDaysCount: Int = 0,
    val schemaVersion: Int = 1
)

fun UserMetrics.toCloud() = CloudUserMetrics(
    id = id,
    name = name,
    weightKg = weightKg,
    heightCm = heightCm,
    age = age,
    gender = gender,
    isActive = isActive
)

fun CloudUserMetrics.toLocal() = UserMetrics(
    id = id,
    name = name,
    weightKg = weightKg,
    heightCm = heightCm,
    age = age,
    gender = gender,
    isActive = isActive
)

fun Exercise.toCloud() = CloudExercise(
    id = id,
    name = name,
    weight = weight,
    reps = reps,
    sets = sets,
    exerciseType = exerciseType,
    usesSensor = usesSensor,
    holdDurationSeconds = holdDurationSeconds,
    deleted = isDeleted,
    photoUri = photoUri,
    sortOrder = sortOrder
)

fun CloudExercise.toLocal() = Exercise(
    id = id,
    name = name,
    weight = weight,
    reps = reps,
    sets = sets,
    exerciseType = exerciseType,
    usesSensor = usesSensor,
    holdDurationSeconds = holdDurationSeconds,
    isDeleted = deleted,
    photoUri = photoUri,
    sortOrder = sortOrder
)

fun WorkoutSession.toCloud() = CloudWorkoutSession(
    id = id,
    date = date,
    durationSeconds = durationSeconds,
    totalWeightLifted = totalWeightLifted,
    caloriesBurned = caloriesBurned,
    notes = notes,
    isPaused = isPaused,
    pausedAt = pausedAt,
    timeOfDay = timeOfDay,
    totalVolume = totalVolume
)

fun CloudWorkoutSession.toLocal() = WorkoutSession(
    id = id,
    date = date,
    durationSeconds = durationSeconds,
    totalWeightLifted = totalWeightLifted,
    caloriesBurned = caloriesBurned,
    notes = notes,
    isPaused = isPaused,
    pausedAt = pausedAt,
    timeOfDay = timeOfDay,
    totalVolume = totalVolume
)

fun Settings.toCloud() = CloudSettings(
    id = id,
    soundsEnabled = soundsEnabled,
    soundVolume = soundVolume,
    timerSoundType = timerSoundType,
    celebrationSoundType = celebrationSoundType,
    themeMode = themeMode,
    tutorialCompleted = tutorialCompleted,
    tutorialVersion = tutorialVersion,
    restTimerDuration = restTimerDuration,
    exerciseSwitchDuration = exerciseSwitchDuration,
    undoLastSetEnabled = undoLastSetEnabled,
    sensorEnabled = sensorEnabled,
    sensorIpAddress = sensorIpAddress,
    calorieIntensity = calorieIntensity
)

fun CloudSettings.toLocal() = Settings(
    id = id,
    soundsEnabled = soundsEnabled,
    soundVolume = soundVolume,
    timerSoundType = timerSoundType,
    celebrationSoundType = celebrationSoundType,
    themeMode = themeMode,
    tutorialCompleted = tutorialCompleted,
    tutorialVersion = tutorialVersion,
    restTimerDuration = restTimerDuration,
    exerciseSwitchDuration = exerciseSwitchDuration,
    undoLastSetEnabled = undoLastSetEnabled,
    sensorEnabled = sensorEnabled,
    sensorIpAddress = sensorIpAddress,
    calorieIntensity = calorieIntensity
)

fun RestDay.toCloud() = CloudRestDay(
    id = id,
    date = date,
    note = note
)

fun CloudRestDay.toLocal() = RestDay(
    id = id,
    date = date,
    note = note
)

fun SessionExercise.toCloud() = CloudSessionExercise(
    id = id,
    sessionId = sessionId,
    exerciseName = exerciseName,
    weight = weight,
    sets = sets,
    reps = reps,
    volume = volume,
    sortOrder = sortOrder
)

fun CloudSessionExercise.toLocal() = SessionExercise(
    id = id,
    sessionId = sessionId,
    exerciseName = exerciseName,
    weight = weight,
    sets = sets,
    reps = reps,
    volume = volume,
    sortOrder = sortOrder
)
