package com.example.workoutapp.ui.restdays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.local.entity.RestDay
import com.example.workoutapp.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class RestDaysViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _restDays = MutableStateFlow<List<RestDay>>(emptyList())
    val restDays: StateFlow<List<RestDay>> = _restDays.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val _noteText = MutableStateFlow("")
    val noteText: StateFlow<String> = _noteText.asStateFlow()

    init {
        loadRestDays()
    }

    private fun loadRestDays() {
        viewModelScope.launch {
            repository.getRestDays().collect { days ->
                _restDays.value = days
            }
        }
    }

    fun toggleRestDay(date: LocalDate) {
        viewModelScope.launch {
            val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val existingRestDay = repository.getRestDayByDate(timestamp)

            if (existingRestDay != null) {
                // Remove rest day
                repository.deleteRestDay(existingRestDay.id)
                if (_selectedDate.value == date) {
                    _selectedDate.value = null
                    _noteText.value = ""
                }
            } else {
                // Add rest day
                repository.addRestDay(RestDay(date = timestamp, note = null))
            }
        }
    }

    fun selectDate(date: LocalDate) {
        viewModelScope.launch {
            _selectedDate.value = date
            val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val restDay = repository.getRestDayByDate(timestamp)
            _noteText.value = restDay?.note ?: ""
        }
    }

    fun updateNote(note: String) {
        _noteText.value = note
    }

    fun saveNote() {
        viewModelScope.launch {
            val date = _selectedDate.value ?: return@launch
            val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val existingRestDay = repository.getRestDayByDate(timestamp)

            if (existingRestDay != null) {
                repository.addRestDay(existingRestDay.copy(note = _noteText.value.ifBlank { null }))
            }
        }
    }

    fun isRestDay(date: LocalDate): Boolean {
        val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return _restDays.value.any { it.date == timestamp }
    }

    fun getRestDaysThisWeek(): Int {
        val now = LocalDate.now()
        val startOfWeek = now.minusDays(now.dayOfWeek.value.toLong() - 1)
        val endOfWeek = startOfWeek.plusDays(6)
        
        return _restDays.value.count { restDay ->
            val date = LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(restDay.date),
                ZoneId.systemDefault()
            )
            date in startOfWeek..endOfWeek
        }
    }

    fun getRestDaysThisMonth(): Int {
        val now = LocalDate.now()
        return _restDays.value.count { restDay ->
            val date = LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(restDay.date),
                ZoneId.systemDefault()
            )
            date.month == now.month && date.year == now.year
        }
    }
}
