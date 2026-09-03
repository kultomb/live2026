# ARCHITECTURE DECISION RECORDS (ADR)

## ADR-001: Architecture & Media Engine Core Design Choice
- **Date**: 2026-09-03
- **Status**: APPROVED
- **Context**: The app requires real-time HDMI capture (via USB UVC), multi-lens Android camera switching, software audio mixing, H.264/AAC MediaCodec encoding, and RTMP/RTMPS streaming.
- **Decision**:
  1. Use Android `Camera2` directly rather than `CameraX` to enable low-level multi-lens Surface control and seamless FrameRouter integration.
  2. Implement an explicit EGL / OpenGL ES 3.0 `VideoMixer` layer fed by a `FrameRouter` to decouple source switching from encoder lifecycle.
  3. Use Android hardware `MediaCodec` for zero-copy surface encoding (H.264 video, AAC audio).
  4. Enforce Single Owner pattern across all core managers (`LiveSessionManager`, `UsbCaptureManager`, `CameraManager`, `StreamManager`).
- **Consequences**:
  - (+) Zero encoder teardown when switching video sources or camera lenses.
  - (+) Ultra-low latency video pipeline (<150ms).
  - (-) Higher initial architectural setup requirement before UI building.

## ADR-002: Direct RTMP/RTMPS Packetizer vs Heavy External Libraries
- **Date**: 2026-09-03
- **Status**: APPROVED
- **Context**: Streaming requires sending FLV tags containing H.264 NAL units and AAC ADTS frames over TCP/TLS sockets.
- **Decision**: Implement a clean Kotlin/C++ streaming transport layer (`StreamManager`) with standard FLV packaging and TLS socket support to prevent dependency bloat and maintain full control over adaptive bitrate buffer management.
- **Consequences**:
  - (+) Full control over frame dropping and adaptive bitrate tuning under poor network conditions.
  - (+) No bloat or black-box native crash risks from unmaintained external RTMP binaries.
