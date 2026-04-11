package com.example.workoutapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _hasProfiles = MutableStateFlow(false)
    val hasProfiles: StateFlow<Boolean> = _hasProfiles.asStateFlow()

    init {
        observeProfiles()
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            repository.getAllUserMetrics().collect { profiles ->
                _hasProfiles.value = profiles.isNotEmpty()
            }
        }
    }

    fun saveMetrics(weight: Float, height: Float, age: Int, gender: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.saveUserMetrics(
                UserMetrics(
                    weightKg = weight,
                    heightCm = height,
                    age = age,
                    gender = gender
                )
            )
            onComplete()
        }
    }
}
