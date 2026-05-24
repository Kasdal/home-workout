package com.example.workoutapp.data.remote

import com.example.workoutapp.data.settings.UpdateCheckPreferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckerTest {

    private val prefs = mockk<UpdateCheckPreferences>(relaxed = true)
    private val client = mockk<OkHttpClient>()
    private val checker = UpdateChecker(prefs, client)

    @Test
    fun `checkForUpdate returns null when version is current`() = runTest {
        every { prefs.lastCheckTimestamp } returns flowOf(0L)
        every { prefs.lastSeenVersion } returns flowOf("1.5.0")
        every { prefs.skippedVersion } returns flowOf(null)
        coEvery { prefs.setLastCheckTimestamp(any()) } just runs

        val mockResponse = mockk<Response>()
        val mockBody = mockk<ResponseBody>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockBody
        every { mockBody.string() } returns """
            {
                "tag_name": "v1.5.0",
                "name": "v1.5.0",
                "html_url": "https://github.com/Kasdal/home-workout/releases/tag/v1.5.0",
                "body": "Release notes here"
            }
        """.trimIndent()
        every { mockResponse.close() } returns Unit

        val call = mockk<okhttp3.Call>()
        every { client.newCall(any<Request>()) } returns call
        every { call.execute() } returns mockResponse

        val result = checker.checkForUpdate("1.5.0")
        assertNull(result)
    }

    @Test
    fun `checkForUpdate returns UpdateInfo when newer version available`() = runTest {
        every { prefs.lastCheckTimestamp } returns flowOf(0L)
        every { prefs.lastSeenVersion } returns flowOf("1.0.0")
        every { prefs.skippedVersion } returns flowOf(null)
        coEvery { prefs.setLastCheckTimestamp(any()) } just runs

        val mockResponse = mockk<Response>()
        val mockBody = mockk<ResponseBody>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockBody
        every { mockBody.string() } returns """
            {
                "tag_name": "v1.5.0",
                "name": "v1.5.0",
                "html_url": "https://github.com/Kasdal/home-workout/releases/tag/v1.5.0",
                "body": "Release notes here"
            }
        """.trimIndent()
        every { mockResponse.close() } returns Unit

        val call = mockk<okhttp3.Call>()
        every { client.newCall(any<Request>()) } returns call
        every { call.execute() } returns mockResponse

        val result = checker.checkForUpdate("1.0.0")
        assertEquals("1.5.0", result?.version)
        assertEquals("https://github.com/Kasdal/home-workout/releases/tag/v1.5.0", result?.downloadUrl)
        assertEquals("Release notes here", result?.changelog)
    }

    @Test
    fun `checkForUpdate skips when within 24h cooldown`() = runTest {
        val recentTimestamp = System.currentTimeMillis() - 12 * 60 * 60 * 1000
        every { prefs.lastCheckTimestamp } returns flowOf(recentTimestamp)

        val result = checker.checkForUpdate("1.0.0")
        assertNull(result)
    }

    @Test
    fun `checkForUpdate skips version user chose to skip`() = runTest {
        every { prefs.lastCheckTimestamp } returns flowOf(0L)
        every { prefs.skippedVersion } returns flowOf("1.5.0")
        coEvery { prefs.setLastCheckTimestamp(any()) } just runs

        val mockResponse = mockk<Response>()
        val mockBody = mockk<ResponseBody>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockBody
        every { mockBody.string() } returns """
            {
                "tag_name": "v1.5.0",
                "name": "v1.5.0",
                "html_url": "https://github.com/Kasdal/home-workout/releases/tag/v1.5.0",
                "body": "Release notes"
            }
        """.trimIndent()
        every { mockResponse.close() } returns Unit

        val call = mockk<okhttp3.Call>()
        every { client.newCall(any<Request>()) } returns call
        every { call.execute() } returns mockResponse

        val result = checker.checkForUpdate("1.0.0")
        assertNull(result)
    }

    @Test
    fun `checkForUpdate handles API error gracefully`() = runTest {
        every { prefs.lastCheckTimestamp } returns flowOf(0L)
        every { prefs.skippedVersion } returns flowOf(null)
        coEvery { prefs.setLastCheckTimestamp(any()) } just runs

        val mockResponse = mockk<Response>()
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code } returns 403
        every { mockResponse.close() } returns Unit

        val call = mockk<okhttp3.Call>()
        every { client.newCall(any<Request>()) } returns call
        every { call.execute() } returns mockResponse

        val result = checker.checkForUpdate("1.0.0")
        assertNull(result)
    }

    @Test
    fun `stripVPrefix removes leading v from tag name`() = runTest {
        every { prefs.lastCheckTimestamp } returns flowOf(0L)
        every { prefs.lastSeenVersion } returns flowOf("1.0.0")
        every { prefs.skippedVersion } returns flowOf(null)
        coEvery { prefs.setLastCheckTimestamp(any()) } just runs

        val mockResponse = mockk<Response>()
        val mockBody = mockk<ResponseBody>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockBody
        every { mockBody.string() } returns """
            {
                "tag_name": "v2.0.0-beta",
                "name": "v2.0.0-beta",
                "html_url": "https://github.com/Kasdal/home-workout/releases/tag/v2.0.0-beta",
                "body": "Beta release"
            }
        """.trimIndent()
        every { mockResponse.close() } returns Unit

        val call = mockk<okhttp3.Call>()
        every { client.newCall(any<Request>()) } returns call
        every { call.execute() } returns mockResponse

        val result = checker.checkForUpdate("1.0.0")
        assertEquals("2.0.0-beta", result?.version)
    }
}
