package com.example.workoutapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    val settings = repository.getSettings()

    init {
        checkUserMetrics()
    }

    private fun checkUserMetrics() {
        viewModelScope.launch {
            if (authManager.currentUserId() == null) {
                _startDestination.value = null
                return@launch
            }

            repository.getUserMetrics().collect { metrics ->
                _startDestination.value = if (metrics != null) "workout" else "onboarding"
            }
        }
    }
}
