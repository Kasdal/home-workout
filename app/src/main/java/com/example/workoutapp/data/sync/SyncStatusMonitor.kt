package com.example.workoutapp.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncStatusMonitor @Inject constructor() {

    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _hasPendingRetry = MutableStateFlow(false)
    val hasPendingRetry: StateFlow<Boolean> = _hasPendingRetry.asStateFlow()

    @Volatile
    private var pendingWrite: (suspend () -> Unit)? = null

    suspend fun <T> track(block: suspend () -> T): T {
        _status.update { it.copy(state = SyncState.SYNCING) }
        return try {
            block().also {
                _status.update {
                    it.copy(
                        state = SyncState.IDLE,
                        lastSyncedAtMillis = System.currentTimeMillis(),
                        lastErrorMessage = null
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            _status.update {
                it.copy(
                    state = SyncState.ERROR,
                    lastErrorMessage = t.message ?: t.javaClass.simpleName
                )
            }
            throw t
        }
    }

    suspend fun runTrackedWithRetry(label: String, block: suspend () -> Unit): Boolean {
        return try {
            track(block)
            pendingWrite = null
            _hasPendingRetry.value = false
            true
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            pendingWrite = block
            _hasPendingRetry.value = true
            false
        }
    }

    suspend fun retryPending(): Boolean {
        val write = pendingWrite ?: return true
        return runTrackedWithRetry("retry", write)
    }

    fun reportFailure(t: Throwable) {
        _status.update {
            it.copy(
                state = SyncState.ERROR,
                lastErrorMessage = t.message ?: t.javaClass.simpleName
            )
        }
    }

    fun clearPendingRetry() {
        pendingWrite = null
        _hasPendingRetry.value = false
    }
}
