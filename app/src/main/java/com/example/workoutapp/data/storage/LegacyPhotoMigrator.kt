package com.example.workoutapp.data.storage

import android.net.Uri
import com.example.workoutapp.data.repository.ExerciseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyPhotoMigrator @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val photoProcessor: PhotoProcessor,
    private val photoUploader: PhotoUploader,
    private val sourceOpener: SourceOpener
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            exerciseRepository.getExercises()
                .map { exercises -> exercises.mapNotNull { it.photoUri?.takeIf(::isLegacy) } }
                .distinctUntilChanged()
                .collect { legacyUris -> migrateAll(legacyUris) }
        }
    }

    private fun isLegacy(uri: String): Boolean =
        uri.startsWith("content://") || uri.startsWith("file://")

    private suspend fun migrateAll(uris: List<String>) {
        uris.forEach { uriString ->
            if (attempted.add(uriString)) {
                migrateOne(uriString)
            }
        }
    }

    private suspend fun migrateOne(uriString: String) {
        if (!sourceOpener.canOpen(uriString)) {
            Timber.w("Legacy photo uri unresolvable, leaving as-is: %s", uriString)
            return
        }
        val current = exerciseRepository.getExercises().first()
            .firstOrNull { it.photoUri == uriString } ?: return
        try {
            val bytes = photoProcessor.compressToJpeg(Uri.parse(uriString))
            val url = photoUploader.uploadExercisePhoto(current.id, bytes)
            exerciseRepository.updateExercise(current.copy(photoUri = url))
        } catch (e: Exception) {
            Timber.w(e, "Failed to migrate legacy photo for exercise %d", current.id)
        }
    }

    private val attempted = mutableSetOf<String>()
}
