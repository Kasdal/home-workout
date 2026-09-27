package com.example.workoutapp.model

/**
 * Denormalised per-exercise analytics, one instance per distinct exercise name.
 *
 * The History screen previously derived personal records, per-exercise PRs and
 * weight trends by reading every [SessionExercise] document the user had ever
 * logged. That read grew without bound: one document per exercise per workout,
 * re-read on every snapshot. This type collapses that into a single small
 * document per exercise, so the read scales with the number of distinct
 * exercises rather than with workout history.
 *
 * Everything here is an exact aggregate of the same inputs the previous
 * implementation used, so the numbers on screen do not change:
 *
 *  - [bestWeight] is the running maximum of `SessionExercise.weight`.
 *  - [totalVolume] is the running sum of `SessionExercise.volume`.
 *  - [sessionIds] holds the distinct session ids, so the count is exact rather
 *    than an increment that could double count a re-saved session.
 *  - [recent] keeps one entry per session, carrying the session maximum weight
 *    and volume sum, which is what the trend chart and the direction arrow read.
 *
 * [recent] is intentionally not capped. Truncating it would silently change the
 * trend, because the arrow compares the first and second half of the series.
 */
data class ExerciseStats(
    val name: String,
    val bestWeight: Float = 0f,
    val totalVolume: Float = 0f,
    val sessionIds: List<Int> = emptyList(),
    val recent: List<SessionPoint> = emptyList()
) {
    val sessionCount: Int get() = sessionIds.size

    /** One row per session, aggregated from that session's sets for this exercise. */
    data class SessionPoint(
        val sessionId: Int,
        val dateMillis: Long,
        val weight: Float,
        val volume: Float
    )

    /**
     * Folds a completed session's exercises for a single exercise name into this
     * aggregate. Idempotent for a given session id, so re-saving a session does
     * not inflate the totals.
     */
    fun mergedWith(sessionId: Int, dateMillis: Long, entries: List<SessionExercise>): ExerciseStats {
        if (entries.isEmpty()) return this

        val sessionVolume = entries.sumOf { it.volume.toDouble() }.toFloat()
        val sessionBestWeight = entries.maxOf { it.weight }
        val existing = recent.firstOrNull { it.sessionId == sessionId }

        val updatedRecent = if (existing == null) {
            recent + SessionPoint(sessionId, dateMillis, sessionBestWeight, sessionVolume)
        } else {
            recent.map {
                if (it.sessionId == sessionId) {
                    it.copy(weight = sessionBestWeight, volume = sessionVolume, dateMillis = dateMillis)
                } else {
                    it
                }
            }
        }.sortedBy { it.sessionId }

        val updatedVolume = if (existing == null) {
            totalVolume + sessionVolume
        } else {
            totalVolume - existing.volume + sessionVolume
        }

        return copy(
            bestWeight = maxOf(bestWeight, sessionBestWeight),
            totalVolume = updatedVolume,
            sessionIds = if (sessionId in sessionIds) sessionIds else sessionIds + sessionId,
            recent = updatedRecent
        )
    }
}
