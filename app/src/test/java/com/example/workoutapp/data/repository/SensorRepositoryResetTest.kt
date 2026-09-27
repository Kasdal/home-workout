package com.example.workoutapp.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Proves that a plain-text body from the ESP is read as a String.
 *
 * The bug this guards: [EspApiService.resetCounter] returns Response<String>, and
 * the only registered converter was Gson. Gson cannot parse "OK" as JSON, so the
 * call threw *after* the ESP had already zeroed its counter. The repository
 * caught the exception and returned false, and the screen told the user the
 * reset had failed when it had in fact worked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SensorRepositoryResetTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: SensorRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = SensorRepository(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun host() = server.hostName.let { "$it:${server.port}" }

    @Test
    fun `reset reports success when the esp answers with plain text`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("OK")
        )

        val didReset = repository.resetCounter(host())

        assertTrue("plain-text body should be readable as a String", didReset)
        assertEquals("/reset", server.takeRequest().path)
    }

    @Test
    fun `reset reports success for an empty body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))

        assertTrue(repository.resetCounter(host()))
    }

    @Test
    fun `reset reports failure only when the esp actually rejects the call`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("nope"))

        assertFalse(repository.resetCounter(host()))
    }

    @Test
    fun `status still parses json after the scalars converter is added`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"reps":7,"state":"LIFTING","dist":1033}""")
        )

        val connected = repository.testConnection(host())

        assertTrue("Gson must still handle the data class", connected)
    }
}
