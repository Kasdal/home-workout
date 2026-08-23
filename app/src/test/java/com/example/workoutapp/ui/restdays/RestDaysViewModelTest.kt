package com.example.workoutapp.ui.restdays

import com.example.workoutapp.data.repository.RestDayRepository
import com.example.workoutapp.model.RestDay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class RestDaysViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: RestDayRepository
    private lateinit var viewModel: RestDaysViewModel

    private val date: LocalDate = LocalDate.of(2026, 8, 22)

    private fun timestampOf(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.getRestDays() } returns MutableStateFlow(emptyList())
        viewModel = RestDaysViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveNote updates the existing rest day and never deletes it`() = runTest {
        val existing = RestDay(id = 7, date = timestampOf(date), note = null)
        coEvery { repository.getRestDayByDate(timestampOf(date)) } returns existing

        viewModel.selectDate(date)
        advanceUntilIdle()

        viewModel.updateNote("legs recovery")
        viewModel.saveNote()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.deleteRestDay(any()) }
        coVerify(exactly = 1) { repository.addRestDay(RestDay(id = 7, date = timestampOf(date), note = "legs recovery")) }
    }

    @Test
    fun `markRestDay adds the day with the typed note and does not delete`() = runTest {
        coEvery { repository.getRestDayByDate(timestampOf(date)) } returns null

        viewModel.markRestDay(date, "easy walk")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.deleteRestDay(any()) }
        coVerify(exactly = 1) { repository.addRestDay(RestDay(date = timestampOf(date), note = "easy walk")) }
    }

    @Test
    fun `markRestDay stores a blank note as null`() = runTest {
        val unmarkedDate = date.plusDays(1)
        coEvery { repository.getRestDayByDate(timestampOf(unmarkedDate)) } returns null

        viewModel.markRestDay(unmarkedDate, "   ")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.addRestDay(RestDay(date = timestampOf(unmarkedDate), note = null)) }
    }

    @Test
    fun `markRestDay is a no-op when the day is already marked`() = runTest {
        val existing = RestDay(id = 3, date = timestampOf(date), note = "keep me")
        coEvery { repository.getRestDayByDate(timestampOf(date)) } returns existing

        viewModel.markRestDay(date, "overwritten?")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.deleteRestDay(any()) }
        coVerify(exactly = 0) { repository.addRestDay(any()) }
    }

    @Test
    fun `removeRestDay deletes only via the explicit remove action`() = runTest {
        val existing = RestDay(id = 9, date = timestampOf(date), note = "rest")
        coEvery { repository.getRestDayByDate(timestampOf(date)) } returns existing

        viewModel.selectDate(date)
        advanceUntilIdle()

        viewModel.removeRestDay(date)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteRestDay(9) }
        assertEquals(null, viewModel.selectedDate.value)
    }

    @Test
    fun `selectDate preloads the existing note into the editor`() = runTest {
        val existing = RestDay(id = 5, date = timestampOf(date), note = "preloaded")
        coEvery { repository.getRestDayByDate(timestampOf(date)) } returns existing

        viewModel.selectDate(date)
        advanceUntilIdle()

        assertEquals("preloaded", viewModel.noteText.value)
    }
}
