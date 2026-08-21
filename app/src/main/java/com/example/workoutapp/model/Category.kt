package com.example.workoutapp.model

data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val sortOrder: Int = Int.MAX_VALUE,
    val isLegacy: Boolean = false,
    val isDeleted: Boolean = false
) {
    companion object {
        const val LEGACY_ID = "legacy"
    }
}
