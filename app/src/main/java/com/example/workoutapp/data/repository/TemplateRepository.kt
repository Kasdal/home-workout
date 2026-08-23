package com.example.workoutapp.data.repository

import com.example.workoutapp.model.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun observeTemplates(): Flow<List<WorkoutTemplate>>
    suspend fun saveTemplate(template: WorkoutTemplate)
    suspend fun deleteTemplate(templateId: String)
}
