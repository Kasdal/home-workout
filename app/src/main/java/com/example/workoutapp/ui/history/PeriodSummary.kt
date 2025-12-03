package com.example.workoutapp.ui.history

data class PeriodSummary(
    val totalWorkouts: Int,
    val totalVolume: Float,
    val totalDuration: Long, // seconds
    val avgWorkoutDuration: Long, // seconds
    val periodLabel: String // "This Week", "Last Week", etc.
)

data class SummaryComparison(
    val current: PeriodSummary,
    val previous: PeriodSummary,
    val volumeChangePercent: Float,
    val frequencyChange: Int,
    val durationChangePercent: Float
)
