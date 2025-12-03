package com.example.workoutapp.ui.history

data class WorkoutInsight(
    val type: InsightType,
    val title: String,
    val message: String,
    val emoji: String
)

enum class InsightType {
    ACHIEVEMENT,    // Milestones reached (100th workout, etc.)
    PROGRESS,       // Improvements detected (volume up, etc.)
    STREAK,         // Streak-related insights
    ENCOURAGEMENT   // General motivation
}
