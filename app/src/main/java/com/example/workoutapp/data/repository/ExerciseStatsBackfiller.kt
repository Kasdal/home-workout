package com.example.workoutapp.data.repository

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rebuilds the denormalised per-exercise analytics for accounts that already have
 * history.
 *
 * The History screen now reads one small aggregate document per exercise instead
 * of every sessionExercise ever written. An account that predates that change has
 * no aggregate documents, so its personal records, volume totals and trends would
 * read as empty until it happened to log a new session.
 *
 * The backfill runs only when the aggregates are empty, and it is idempotent
 * because the merge is keyed on session id. That means a repeat run after a
 * configuration change, or a second launch, is harmless and needs no persisted
 * flag.
 */
@Singleton
class ExerciseStatsBackfiller @Inject constructor(
    private val sessionHistoryRepository: SessionHistoryRepository
) {
    private var attempted = false

    fun start(scope: CoroutineScope) {
        if (attempted) return
        attempted = true
        scope.launch {
            runCatching {
                if (sessionHistoryRepository.observeExerciseStats().first().isNotEmpty()) {
                    return@runCatching
                }
                val written = sessionHistoryRepository.backfillExerciseStats()
                if (written > 0) {
                    Log.i(TAG, "Rebuilt per-exercise analytics for $written exercises")
                }
            }.onFailure { error ->
                Log.w(TAG, "Per-exercise analytics backfill skipped", error)
            }
        }
    }

    private companion object {
        const val TAG = "ExerciseStatsBackfill"
    }
}
