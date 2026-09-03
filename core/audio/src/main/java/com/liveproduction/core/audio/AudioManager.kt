package com.liveproduction.core.audio

import android.content.Context
import android.util.Log
import com.liveproduction.core.audio.mixer.AudioMixer
import com.liveproduction.core.audio.model.AudioMeterState
import com.liveproduction.core.audio.resampler.AudioResampler
import com.liveproduction.core.audio.sync.TimestampManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioManager private constructor() {

    val audioMixer = AudioMixer()
    val audioResampler = AudioResampler(targetSampleRate = 44100, targetChannels = 2)
    val timestampManager = TimestampManager()

    private val _audioMeterState = MutableStateFlow(AudioMeterState())
    val audioMeterState: StateFlow<AudioMeterState> = _audioMeterState.asStateFlow()

    fun startMicAudioRecording(context: Context) {
        MicAudioRecorder.getInstance().startRecording(context)
    }

    fun stopMicAudioRecording() {
        MicAudioRecorder.getInstance().stopRecording()
    }

    fun processAudioBuffer(
        inputPcm: ShortArray,
        inputSampleRate: Int = 44100,
        inputChannels: Int = 2
    ): ShortArray {
        // 1. Resample to 44.1kHz stereo
        val resampled = audioResampler.resamplePcm16(inputPcm, inputSampleRate, inputChannels)

        // 2. Mix, Gain & Soft Limit
        val currentGain = _audioMeterState.value.masterGainFactor
        val isMuted = _audioMeterState.value.isMuted
        val mixResult = audioMixer.mixAndScale(resampled, currentGain, isMuted)

        // 3. Update Meter State for UI
        _audioMeterState.value = _audioMeterState.value.copy(
            leftChannelDb = mixResult.leftDb,
            rightChannelDb = mixResult.rightDb,
            isClipping = mixResult.isClipping
        )

        return mixResult.mixedPcm
    }

    fun updateAudioLevel(levelPercent: Int) {
        val effectiveLevel = if (_audioMeterState.value.isMuted) 0 else levelPercent
        _audioMeterState.value = _audioMeterState.value.copy(levelPercent = effectiveLevel)
    }

    fun setMute(muted: Boolean) {
        Log.i(TAG, "Audio mute toggled: $muted")
        val current = _audioMeterState.value
        _audioMeterState.value = current.copy(
            isMuted = muted,
            levelPercent = if (muted) 0 else current.levelPercent
        )
    }

    fun setMasterGain(gain: Float) {
        Log.i(TAG, "Audio master gain set to: $gain")
        _audioMeterState.value = _audioMeterState.value.copy(masterGainFactor = gain)
    }

    companion object {
        private const val TAG = "AudioManager"

        @Volatile
        private var INSTANCE: AudioManager? = null

        fun getInstance(): AudioManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioManager().also { INSTANCE = it }
            }
        }
    }
}
