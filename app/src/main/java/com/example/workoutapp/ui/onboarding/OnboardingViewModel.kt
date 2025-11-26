package com.example.workoutapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

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
