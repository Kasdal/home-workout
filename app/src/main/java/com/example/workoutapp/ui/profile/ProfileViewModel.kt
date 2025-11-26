package com.example.workoutapp.ui.profile

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
class ProfileViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    val allProfiles = repository.getAllUserMetrics()
    
    private val _currentProfile = MutableStateFlow<UserMetrics?>(null)
    val currentProfile: StateFlow<UserMetrics?> = _currentProfile.asStateFlow()

    init {
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        viewModelScope.launch {
            repository.getUserMetrics().collect { profile ->
                _currentProfile.value = profile
            }
        }
    }

    fun addProfile(name: String, weight: Float, height: Float, age: Int, gender: String) {
        viewModelScope.launch {
            val profile = UserMetrics(
                name = name,
                weightKg = weight,
                heightCm = height,
                age = age,
                gender = gender,
                isActive = false
            )
            repository.addUserMetrics(profile)
        }
    }

    fun updateProfile(profile: UserMetrics) {
        viewModelScope.launch {
            repository.updateUserMetrics(profile)
        }
    }

    fun setActiveProfile(profileId: Int) {
        viewModelScope.launch {
            repository.setActiveProfile(profileId)
            loadCurrentProfile()
        }
    }

    fun deleteProfile(profileId: Int) {
        viewModelScope.launch {
            repository.deleteUserMetrics(profileId)
        }
    }
}
