package com.example.workoutapp.data.local.room.entity

import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.RestDay
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.model.Settings
import com.example.workoutapp.model.UserMetrics
import com.example.workoutapp.model.WorkoutSession

fun RoomExercise.toDomain() = Exercise(id, name, weight, reps, sets, exerciseType, usesSensor, holdDurationSeconds, isDeleted, photoUri)
fun Exercise.toRoom() = RoomExercise(id, name, weight, reps, sets, exerciseType, usesSensor, holdDurationSeconds, isDeleted, photoUri)

fun RoomSettings.toDomain() = Settings(id, soundsEnabled, soundVolume, timerSoundType, celebrationSoundType, themeMode, tutorialCompleted, tutorialVersion, restTimerDuration, exerciseSwitchDuration, undoLastSetEnabled, sensorEnabled, sensorIpAddress)
fun Settings.toRoom() = RoomSettings(id, soundsEnabled, soundVolume, timerSoundType, celebrationSoundType, themeMode, tutorialCompleted, tutorialVersion, restTimerDuration, exerciseSwitchDuration, undoLastSetEnabled, sensorEnabled, sensorIpAddress)

fun RoomWorkoutSession.toDomain() = WorkoutSession(id, date, durationSeconds, totalWeightLifted, caloriesBurned, notes, isPaused, pausedAt, timeOfDay, totalVolume)
fun WorkoutSession.toRoom() = RoomWorkoutSession(id, date, durationSeconds, totalWeightLifted, caloriesBurned, notes, isPaused, pausedAt, timeOfDay, totalVolume)

fun RoomUserMetrics.toDomain() = UserMetrics(id, name, weightKg, heightCm, age, gender, isActive)
fun UserMetrics.toRoom() = RoomUserMetrics(id, name, weightKg, heightCm, age, gender, isActive)

fun RoomSessionExercise.toDomain() = SessionExercise(id, sessionId, exerciseName, weight, sets, reps, volume, sortOrder)
fun SessionExercise.toRoom() = RoomSessionExercise(id, sessionId, exerciseName, weight, sets, reps, volume, sortOrder)

fun RoomRestDay.toDomain() = RestDay(id, date, note)
fun RestDay.toRoom() = RoomRestDay(id, date, note)
