package com.example.workoutapp.data.sync

enum class SyncState {
    IDLE,
    SYNCING,
    ERROR
}

data class SyncStatus(
    val state: SyncState = SyncState.IDLE,
    val lastSyncedAtMillis: Long? = null,
    val lastErrorMessage: String? = null
)
