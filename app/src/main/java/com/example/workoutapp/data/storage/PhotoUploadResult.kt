package com.example.workoutapp.data.storage

sealed interface PhotoUploadResult {
    data class Success(val photoUri: String) : PhotoUploadResult
    data object SourceUnreadable : PhotoUploadResult
    data class UploadFailed(val cause: Throwable) : PhotoUploadResult
}
