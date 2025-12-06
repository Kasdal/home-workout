package com.example.workoutapp.util

import android.content.Context
import android.media.MediaPlayer
import com.example.workoutapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * Play timer countdown sound based on selected type
     * @param soundType "beep", "chime", or "loud"
     * @param volume 0.0 to 1.0
     * @param enabled whether sounds are enabled
     */
    fun playTimerSound(soundType: String, volume: Float, enabled: Boolean) {
        if (!enabled) return
        
        val resourceId = when (soundType.lowercase()) {
            "beep" -> R.raw.timer_beep
            "chime" -> R.raw.timer_chime
            "loud" -> R.raw.timer_loud
            else -> R.raw.timer_beep
        }
        
        playSound(resourceId, volume)
    }
    
    /**
     * Play celebration sound based on selected type
     * @param soundType "cheer", "victory", or "congrats"
     * @param volume 0.0 to 1.0
     * @param enabled whether sounds are enabled
     */
    fun playCelebrationSound(soundType: String, volume: Float, enabled: Boolean) {
        if (!enabled) return
        
        val resourceId = when (soundType.lowercase()) {
            "cheer" -> R.raw.celebration_cheer
            "victory" -> R.raw.celebration_victory
            "congrats" -> R.raw.celebration_cheer  // Reuse cheer for congrats
            else -> R.raw.celebration_cheer
        }
        
        playSound(resourceId, volume)
    }
    
    /**
     * Play sound from resource ID
     */
    private fun playSound(resourceId: Int, volume: Float) {
        try {
            releaseMediaPlayer()
            
            mediaPlayer = MediaPlayer.create(context, resourceId)?.apply {
                setVolume(volume, volume)
                setOnCompletionListener { releaseMediaPlayer() }
                start()
            }
        } catch (e: Exception) {
            // Silently fail if sound file not found
            e.printStackTrace()
        }
    }
    
    /**
     * Release MediaPlayer resources
     */
    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
    
    /**
     * Release all resources - call when done with SoundManager
     */
    fun release() {
        releaseMediaPlayer()
    }
    
    /**
     * Legacy method for backward compatibility
     */
    @Deprecated("Use playTimerSound instead", ReplaceWith("playTimerSound(\"beep\", 1.0f, true)"))
    fun playCountdownBeep() {
        playTimerSound("beep", 1.0f, true)
    }

    /**
     * Legacy method for backward compatibility
     */
    @Deprecated("Use playCelebrationSound instead", ReplaceWith("playCelebrationSound(\"cheer\", 1.0f, true)"))
    fun playFinishedBeep() {
        playCelebrationSound("cheer", 1.0f, true)
    }
}
