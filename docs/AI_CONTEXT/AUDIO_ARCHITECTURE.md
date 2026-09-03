# AUDIO ARCHITECTURE & MIXING

## 1. AUDIO SOURCES & INPUT MANAGEMENT
- **Source A**: USB UAC Capture Card Audio / External USB Audio Interface (`AudioRecord` with `MediaRecorder.AudioSource.VOICE_PERFORMANCE` or Android USB Audio API).
- **Source B**: Android Internal Microphone (`MediaRecorder.AudioSource.MIC` / `CAMCORDER`).
- **Source C**: Bluetooth Headset Mic (SCO / LeAudio where available).

## 2. AUDIO PIPELINE ARCHITECTURE

```
+---------------------+     +---------------------+
| USB UAC Audio Input |     | Android Mic Input   |
| (44.1kHz / 48kHz)   |     | (44.1kHz / 48kHz)   |
+---------------------+     +---------------------+
           |                           |
           v                           v
   [Resampler 48k->48k]        [Resampler 44.1k->48k]
           |                           |
           v                           v
   [Software Gain/Mute]        [Software Gain/Mute]
           |                           |
           +-------------+-------------+
                         |
                         v
               +-------------------+
               |    AUDIO MIXER    |  <-- PCM sample addition, clipping limiter
               +-------------------+
                         |
           +-------------+-------------+
           |                           |
           v                           v
   [VU Meter Calculator]       [MediaCodec AAC Encoder]
   (Peak/RMS dB calculation)   (128 - 320 kbps Stereo)
                                       |
                                       v
                                [AAC FLV Packager]
```

## 3. SOFTWARE MIXING & GAIN CONTROL
- **PCM Resampling**: Standardize all incoming audio streams to 48,000 Hz 16-bit Stereo PCM using lightweight linear or cubic spline resampler.
- **Gain Scaling**: Apply per-channel gain multiplication:
  `SampleOut = ClampToInt16(SampleIn * GainFactor)`
- **Clipping Protection / Soft Limiter**: If composite PCM value exceeds +32767 or falls below -32768, apply a soft limiter curve to prevent unpleasant digital clipping distortion.
- **VU Level Metering**: Calculate Root Mean Square (RMS) dB values every 50ms for UI VU meters:
  `dB = 20 * log10(RMS / 32768.0)`

## 4. SAMPLE RATE & LATENCY RULES
- Native Audio buffer size calculated via `AudioRecord.getMinBufferSize()`.
- Run audio capture thread at `THREAD_PRIORITY_URGENT_AUDIO`.
- Encoder configured for AAC-LC profile (`MediaCodecInfo.CodecProfileLevel.AACObjectLC`), 48,000 Hz, 2 channels, 128 kbps default bitrate.
