package com.example.workoutapp.util

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor() {
    private var toneGenerator: ToneGenerator? = null
    
    /**
     * Play timer countdown sound based on selected type
     * @param soundType "beep", "chime", or "loud"
     * @param volume 0.0 to 1.0
     * @param enabled whether sounds are enabled
     */
    fun playTimerSound(soundType: String, volume: Float, enabled: Boolean) {
        if (!enabled) return
        
        val volumeInt = (volume * 100).toInt().coerceIn(0, 100)
        releaseToneGenerator()
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, volumeInt)
        
        val (tone, duration) = when (soundType.lowercase()) {
            "beep" -> ToneGenerator.TONE_CDMA_PIP to 150
            "chime" -> ToneGenerator.TONE_PROP_BEEP to 200
            "loud" -> ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE to 300
            else -> ToneGenerator.TONE_CDMA_PIP to 150
        }
        
        toneGenerator?.startTone(tone, duration)
    }
    
    /**
     * Play celebration sound based on selected type
     * @param soundType "cheer", "victory", or "congrats"
     * @param volume 0.0 to 1.0
     * @param enabled whether sounds are enabled
     */
    fun playCelebrationSound(soundType: String, volume: Float, enabled: Boolean) {
        if (!enabled) return
        
        val volumeInt = (volume * 100).toInt().coerceIn(0, 100)
        releaseToneGenerator()
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, volumeInt)
        
        val (tone, duration) = when (soundType.lowercase()) {
            "cheer" -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 800
            "victory" -> ToneGenerator.TONE_PROP_ACK to 1000
            "congrats" -> ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE to 1200
            else -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 800
        }
        
        toneGenerator?.startTone(tone, duration)
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
    
    /**
     * Release ToneGenerator resources
     */
    private fun releaseToneGenerator() {
        toneGenerator?.release()
        toneGenerator = null
    }
    
    /**
     * Release all resources - call when done with SoundManager
     */
    fun release() {
        releaseToneGenerator()
    }
}
