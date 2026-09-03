# MEDIA PIPELINE ARCHITECTURE

## 1. VIDEO FRAME PIPELINE

```
+-------------------+      +--------------------+
| USB UVC Capture   |      | Android Camera2    |
| (MJPEG/YUY2/NV21) |      | (SurfaceTexture)   |
+-------------------+      +--------------------+
          |                          |
          v                          v
    [YUV/NV21 Buffer]         [OES Texture]
          |                          |
          +------------+-------------+
                       |
                       v
             +-------------------+
             |    FRAME ROUTER   |  <-- Decouples source capture from processing
             +-------------------+
                       |
                       v
             +-------------------+
             |    VIDEO MIXER    |  <-- Scene compositing, PiP, Split Screen, Overlays
             |   (OpenGL ES 3.0) |
             +-------------------+
                       |
         +-------------+-------------+
         |                           |
         v                           v
  [Preview Surface]         [Encoder Input Surface]
  (Display on UI)           (Hardware H.264 MediaCodec)
                                     |
                                     v
                            [H.264 Bitstream]
                                     |
                     +---------------+---------------+
                     |                               |
                     v                               v
             [Stream Manager]               [Recording Manager]
             (RTMP/RTMPS FLV)               (MediaMuxer MP4)
```

## 2. AUDIO FRAME PIPELINE

```
+-----------------------+      +-----------------------+
| USB Audio Input       |      | Android Microphone    |
| (PCM 16-bit 44.1/48k) |      | (AudioRecord PCM)     |
+-----------------------+      +-----------------------+
            |                              |
            v                              v
      [Audio Buffer]                 [Audio Buffer]
            |                              |
            +--------------+---------------+
                           |
                           v
                 +-------------------+
                 |    AUDIO ROUTER   |
                 +-------------------+
                           |
                           v
                 +-------------------+
                 |    AUDIO MIXER    |  <-- Software gain, Mute, Channel mapping
                 +-------------------+
                           |
                           v
                 +-------------------+
                 |  MEDIACODEC AAC   |  <-- Audio Encoder (128-320 kbps AAC-LC)
                 +-------------------+
                           |
                           v
                 [AAC Audio Frames]
                           |
           +---------------+---------------+
           |                               |
           v                               v
   [Stream Manager]               [Recording Manager]
   (RTMP FLV Tag)                 (MediaMuxer MP4)
```

## 3. TIMESTAMP SYNCHRONIZATION (PTS/DTS)
- Video frames from USB UVC or Camera2 receive a high-resolution monotonic timestamp (`System.nanoTime()`).
- Audio frames read from `AudioRecord` / USB Audio receive buffer timestamps based on sample count accumulation and monotonic clock.
- **Timestamp Alignment**:
  - `VideoPTS = (FrameNanoTime - SessionStartNanoTime) / 1000` (microseconds)
  - `AudioPTS = (AudioSampleCount * 1_000_000) / SampleRate` (microseconds)
- The `FrameRouter` monitors `AudioPTS` vs `VideoPTS` drift. If drift exceeds 40ms, a timestamp normalization offset is calculated to prevent video/audio desynchronization during multi-hour streams.

## 4. PRODUCER-CONSUMER BUFFER & RING BUFFER DESIGN
- USB UVC capture runs on a dedicated high-priority thread `UsbIoThread`.
- Received raw frames are written into a lock-free fixed-capacity Ring Buffer (`UvcRingBuffer`, depth = 4 frames).
- If the consumer thread (`VideoProcessingWorker`) is busy rendering/compositing, the oldest unread frame in the ring buffer is dropped automatically. This prevents unbounded latency growth and memory allocation thrashing.
