package com.example.workoutapp.data.sync

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SyncStatusMonitorTest {

    @Test
    fun `successful write records sync time and clears error`() = runTest {
        val monitor = SyncStatusMonitor()

        val saved = monitor.runTrackedWithRetry("op") { }

        assertTrue(saved)
        assertEquals(SyncState.IDLE, monitor.status.value.state)
        assertNotNull(monitor.status.value.lastSyncedAtMillis)
        assertNull(monitor.status.value.lastErrorMessage)
        assertFalse(monitor.hasPendingRetry.value)
    }

    @Test
    fun `failed write sets error state and remembers retry`() = runTest {
        val monitor = SyncStatusMonitor()

        val saved = monitor.runTrackedWithRetry("op") { throw IOException("rules denied") }

        assertFalse(saved)
        assertEquals(SyncState.ERROR, monitor.status.value.state)
        assertEquals("rules denied", monitor.status.value.lastErrorMessage)
        assertTrue(monitor.hasPendingRetry.value)
    }

    @Test
    fun `retry runs stored write and clears pending on success`() = runTest {
        val monitor = SyncStatusMonitor()
        var attempts = 0
        monitor.runTrackedWithRetry("op") {
            if (attempts++ == 0) throw IOException("offline")
        }

        val retried = monitor.retryPending()

        assertTrue(retried)
        assertEquals(SyncState.IDLE, monitor.status.value.state)
        assertFalse(monitor.hasPendingRetry.value)
    }

    @Test
    fun `retry with nothing pending succeeds without writes`() = runTest {
        val monitor = SyncStatusMonitor()

        assertTrue(monitor.retryPending())
    }

    @Test
    fun `track rethrows failure after recording error state`() = runTest {
        val monitor = SyncStatusMonitor()

        var thrown: Throwable? = null
        try {
            monitor.track<Unit> { throw IllegalStateException("boom") }
        } catch (t: Throwable) {
            thrown = t
        }

        assertTrue(thrown is IllegalStateException)
        assertEquals(SyncState.ERROR, monitor.status.value.state)
    }

    @Test
    fun `reportFailure marks error without creating a retry`() = runTest {
        val monitor = SyncStatusMonitor()

        monitor.reportFailure(IOException("quota exceeded"))

        assertEquals(SyncState.ERROR, monitor.status.value.state)
        assertEquals("quota exceeded", monitor.status.value.lastErrorMessage)
        assertFalse(monitor.hasPendingRetry.value)
    }
}
