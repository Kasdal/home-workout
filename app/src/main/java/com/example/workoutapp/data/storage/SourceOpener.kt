package com.example.workoutapp.data.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

fun interface SourceOpener {
    fun canOpen(uriString: String): Boolean
}

@Singleton
class AndroidSourceOpener @Inject constructor(
    @ApplicationContext private val context: Context
) : SourceOpener {
    override fun canOpen(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }
}
