package com.example.workoutapp.data.repository

import com.example.workoutapp.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeActiveCategories(): Flow<List<Category>>
    suspend fun getActiveCategories(): List<Category>
    suspend fun upsertCategory(category: Category)
    /**
     * Marks [categoryId] as deleted and rewrites every exercise that pointed at it
     * to [reassignToCategoryId]. Throws [IllegalStateException] if [categoryId] is
     * the protected "legacy" category, or if [reassignToCategoryId] refers to a
     * category already marked deleted.
     */
    suspend fun deleteAndReassign(categoryId: String, reassignToCategoryId: String)
    suspend fun backfillLegacyAssignments(legacyCategoryId: String)
}
