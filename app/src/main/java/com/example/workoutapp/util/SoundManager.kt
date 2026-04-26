package com.example.workoutapp.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.workoutapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    /**
     * Play timer countdown sound based on selected type
     * @param soundType "beep", "chime", or "loud"
     * @param volume 0.0 to 1.0
     * @param enabled whether sounds are enabled
     */
    fun playTimerSound(
        soundType: String,
        volume: Float,
        enabled: Boolean,
        vibrationEnabled: Boolean = false,
        silentModeBehavior: String = "respect"
    ) {
        val resourceId = when (soundType.lowercase()) {
            "beep" -> R.raw.timer_beep
            "chime" -> R.raw.timer_chime
            "loud" -> R.raw.timer_loud
            else -> R.raw.timer_beep
        }

        playSound(resourceId, volume, enabled, vibrationEnabled, silentModeBehavior)
    }
    
    /**
     * Play celebration sound based on selected type
     * @param soundType "cheer" or "victory"
     * @param volume 0.0 to 1.0
     * @param enabled whether sounds are enabled
     */
    fun playCelebrationSound(
        soundType: String,
        volume: Float,
        enabled: Boolean,
        vibrationEnabled: Boolean = false,
        silentModeBehavior: String = "respect"
    ) {
        val resourceId = when (soundType.lowercase()) {
            "cheer" -> R.raw.celebration_cheer
            "victory" -> R.raw.celebration_victory
            else -> R.raw.celebration_cheer
        }

        playSound(resourceId, volume, enabled, vibrationEnabled, silentModeBehavior)
    }

    fun vibrateCue(enabled: Boolean) {
        if (enabled) vibrate(120L)
    }
    
    /**
     * Play sound from resource ID
     */
    private fun playSound(
        resourceId: Int,
        volume: Float,
        enabled: Boolean,
        vibrationEnabled: Boolean,
        silentModeBehavior: String
    ) {
        val ringerMode = audioManager.ringerMode
        val shouldVibrate = vibrationEnabled && (
            silentModeBehavior == "vibrate" ||
                ringerMode == AudioManager.RINGER_MODE_VIBRATE ||
                ringerMode == AudioManager.RINGER_MODE_SILENT
            )
        val shouldPlaySound = enabled && when (silentModeBehavior) {
            "always" -> true
            "vibrate" -> false
            else -> ringerMode == AudioManager.RINGER_MODE_NORMAL
        }

        if (shouldVibrate) vibrate(120L)
        if (!shouldPlaySound) return

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

    private fun vibrate(durationMs: Long) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {
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
