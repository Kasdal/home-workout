package com.example.workoutapp.ui.history

import androidx.lifecycle.ViewModel
import com.example.workoutapp.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: WorkoutRepository
) : ViewModel() {
    val sessions = repository.getSessions()
}
