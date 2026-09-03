# STREAMING ARCHITECTURE (RTMP / RTMPS)

## 1. RTMP / RTMPS ENGINE ARCHITECTURE
- **Protocol**: Real-Time Messaging Protocol (RTMP over TCP) and RTMPS (TLS/SSL encrypted socket connection).
- **Format**: FLV container packaging for Video (H.264 AVC NAL units: SPS, PPS, IDR, P-frames) and Audio (AAC AudioSpecificConfig, raw AAC frames).

```
 +-------------------------------------------------------+
 |                 MEDIA ENCODER OUTPUT                  |
 |  H.264 NAL Units (IDR, P-Frame) + AAC ADTS Frames     |
 +-------------------------------------------------------+
                            |
                            v
 +-------------------------------------------------------+
 |                  FLV TAG PACKAGER                     |
 |                                                       |
 |  - Video Tag (TagType 0x09, CompositionTime, AVC NALU)|
 |  - Audio Tag (TagType 0x08, AAC Raw/Header)           |
 |  - Metadata Tag (TagType 0x12, @setDataFrame)        |
 +-------------------------------------------------------+
                            |
                            v
 +-------------------------------------------------------+
 |                 RTMP SOCKET PACKETIZER                |
 |                                                       |
 |  - Chunking (Chunk Size 4096 bytes)                   |
 |  - Handshake (C0+C1 -> S0+S1+S2 -> C2)                |
 |  - Connect ('connect' AMF0 command)                   |
 |  - CreateStream ('createStream' AMF0)                 |
 |  - Publish ('publish' live stream key)                |
 +-------------------------------------------------------+
                            |
                            v  TLS / TCP Socket
 +-------------------------------------------------------+
 |            RTMP SERVER (YouTube / Facebook / Custom)   |
 +-------------------------------------------------------+
```

## 2. ADAPTIVE STREAM PROFILES
The stream manager allows choosing or auto-selecting encoding profiles based on bandwidth test:

| Profile | Resolution | Frame Rate | Target Bitrate | Keyframe Interval | Audio Bitrate |
|---|---|---|---|---|---|
| **720p LOW** | 1280x720 | 30 fps | 2,500 kbps | 2.0 s (60 frames) | 128 kbps |
| **1080p STANDARD** | 1920x1080 | 30 fps | 4,500 kbps | 2.0 s (60 frames) | 160 kbps |
| **1080p PRO 60FPS**| 1920x1080 | 60 fps | 7,500 kbps | 2.0 s (120 frames)| 192 kbps |
| **CUSTOM** | User defined| 24-60 fps | 1,000-12,000 k | 1.0 - 4.0 s | 96-320 kbps |

## 3. SOCIAL PLATFORM INTEGRATIONS
- Platform configurations encapsulate endpoints:
  - **YouTube Live**: `rtmp://a.rtmp.youtube.com/live2` or `rtmps://a.rtmps.youtube.com/live2`
  - **Facebook Live**: `rtmps://live-api-s.facebook.com:443/rtmp/`
  - **Custom RTMP / RTMPS**: User specifies custom RTMP URL and Stream Key.
- Platform layer is decoupled from low-level socket chunker.

## 4. CREDENTIAL SECURITY
- Stream keys are kept in `EncryptedSharedPreferences` backed by Android KeyStore.
- `StreamManager.toString()` and logs obscure stream key values (`rtmp://.../live2/****HIDDEN_KEY****`).
