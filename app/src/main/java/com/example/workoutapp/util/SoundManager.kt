package com.example.workoutapp.util

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor() {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)

    fun playCountdownBeep() {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
    }

    fun playFinishedBeep() {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
    }
}
