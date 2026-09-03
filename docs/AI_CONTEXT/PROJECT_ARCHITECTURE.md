# PROJECT ARCHITECTURE & SINGLE OWNER RULES

## 1. LAYERED SYSTEM ARCHITECTURE

```
                      +-----------------------------------+
                      |         PRESENTATION LAYER        |
                      |  LiveScreen | Settings | Diagnost. |
                      +-----------------------------------+
                                        |
                                        v
                      +-----------------------------------+
                      |         APPLICATION LAYER         |
                      | LiveSessionManager | SceneManager |
                      +-----------------------------------+
                                        |
                                        v
                      +-----------------------------------+
                      |            MEDIA LAYER            |
                      | FrameRouter | VideoMixer | Encoder|
                      +-----------------------------------+
                                        |
                                        v
                      +-----------------------------------+
                      |        INFRASTRUCTURE LAYER       |
                      | UsbCapture | Camera2 | RTMP | Net |
                      +-----------------------------------+
```

## 2. SINGLE OWNER RULE TABLE
To eliminate duplicate code, uncontrolled workers, or multi-owner conflicts, every subsystem has ONE strict owner:

| Subsystem | Single Owner Class | Responsibilities |
|---|---|---|
| Live Session | `LiveSessionManager` | Manages overall state machine, session lifecycle, GO LIVE action. |
| USB / UVC Capture | `UsbCaptureManager` | Manages USB connection, permissions, UVC frame extraction, hot-plug. |
| Camera Capture | `CameraManager` | Controls Camera2 enumeration, multi-lens selection, session setup. |
| Audio Capture & Routing | `AudioManager` | Manages AudioRecord, USB Audio input, software gain & mute. |
| Frame Routing & Composition| `VideoPipelineManager` | Receives video frames, performs scene rendering via EGL/OpenGL. |
| Hardware Encoding | `EncoderManager` | Configures & runs MediaCodec for H.264 video and AAC audio. |
| RTMP Streaming | `StreamManager` | Manages RTMP connection socket, FLV tag packaging, send queue. |
| Network Recovery | `NetworkManager` | Monitors internet connectivity, RTT, bandwith, triggers retries. |
| Local Recording | `RecordingManager` | Controls MP4 container muxing via MediaMuxer. |
| App State / Storage | `AppStateManager` | Persists user settings, safe stream key retrieval. |
| Diagnostics & Metrics | `DiagnosticsManager` | Collects FPS, dropped frames, thermal pressure, battery info. |

## 3. LIVE SESSION STATE MACHINE
The core workflow is governed strictly by `LiveSessionManager`:

```
 [IDLE] ──(Prepare)──> [PREPARING] ──(Ready)──> [READY]
   ^                         |                     |
   |                       (Fail)                (Go Live)
   |                         v                     v
   +──────────────────── [FAILED] <─────────── [STARTING]
   |                         ^                     |
 (Stop)                    (Fail)             (Connected)
   |                         |                     v
 [STOPPING] <───────────────+─────────────── [LIVE]
                                                   | |
                                         (Net Drop)| |(Recovered)
                                                   v v
                                            [RECONNECTING]
```

- **IDLE**: No hardware capture active.
- **PREPARING**: Requesting USB permissions, initializing Camera2, setting up EGL context.
- **READY**: Preview active, hardware warm, waiting for GO LIVE command.
- **STARTING**: Connecting to RTMP server, sending stream headers/metadata.
- **LIVE**: Active streaming, encoding H.264 + AAC, monitoring network & thermal stats.
- **RECONNECTING**: Network lost, buffering frames locally while attempting exponential backoff reconnect.
- **STOPPING**: Releasing encoder, closing RTMP socket, saving recording.
- **FAILED**: Error state with clear diagnostic code.

## 4. MODULE STRUCTURE (GRADLE)
```
AndroidLiveApp/
├── app/                      # Main entry point & DI graph binding
├── core/
│   ├── media/                # OpenGL video compositing, FrameRouter, EGL
│   ├── camera/               # Camera2 enumeration & physical lens control
│   ├── usb/                  # USB Host & UVC frame parsing
│   ├── audio/                # Audio capture, mixer, PTS synchronization
│   ├── streaming/            # RTMP/RTMPS engine & FLV packager
│   ├── network/              # Connectivity monitoring & adaptive bitrate
│   └── diagnostics/          # Metrics, thermal, battery, performance monitoring
└── feature/
    ├── live/                 # Live Studio Screen & controls
    ├── camera/               # Camera setup & manual controls
    ├── settings/             # Stream destinations & encoder profiles
    └── diagnostics/          # System hardware diagnostic view
```
