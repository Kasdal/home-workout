package com.example.workoutapp.ui.history

data class WorkoutInsight(
    val type: InsightType,
    val title: String,
    val message: String,
    val emoji: String
)

enum class InsightType {
    ACHIEVEMENT,
    PROGRESS,
    STREAK,
    ENCOURAGEMENT
}
