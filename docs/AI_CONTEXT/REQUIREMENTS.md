# REQUIREMENTS & CONSTRAINTS

## 1. FUNCTIONAL REQUIREMENTS

### FR-01: USB UVC Video Capture
- App MUST query attached USB devices via `UsbManager`.
- App MUST request explicit user permission before opening USB device endpoint.
- App MUST support UVC standard frame processing (MJPEG / YUY2 / NV21).
- App MUST handle hot-plug events (unplugging during live preview or active stream) without crashing.

### FR-02: Camera2 Integration
- App MUST enumerate camera devices via `CameraManager`.
- App MUST identify physical lenses (Ultra Wide, Wide, Telephoto) when exposed by `CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA`.
- App MUST support manual controls (zoom, focus, exposure) via Camera2 requests.

### FR-03: Video Composition & Switching
- App MUST switch active scene/source without disconnecting or resetting the RTMP encoder.
- App MUST render composed video onto OpenGL ES 2.0/3.0 Surface textures for preview and encoding.

### FR-04: Audio Processing & Synchronization
- App MUST capture audio from selected audio source (USB / Mic).
- App MUST synchronize Audio Presentation Timestamps (PTS) with Video PTS to prevent drift over multi-hour streams.

### FR-05: RTMP / RTMPS Streaming
- App MUST transmit H.264 video frames and AAC audio frames wrapped in FLV tags over RTMP/RTMPS sockets.
- App MUST implement configurable keyframe intervals (typically 2 seconds).

### FR-06: Resilience & Lifecycle
- App MUST gracefully handle Android activity lifecycle events (`onPause`, `onStop`, backgrounding) using a Foreground Service with `type="camera|connectedDevice|microphone"`.
- App MUST handle network drops by entering `RECONNECTING` state with bounded retries.

## 2. NON-FUNCTIONAL REQUIREMENTS

### NFR-01: Low Latency & High Frame Stability
- End-to-end capture-to-encoder latency MUST remain under 150ms on supported hardware.
- Frame drops due to buffer starvation MUST NOT exceed 1% under normal network conditions.

### NFR-02: Threading & Concurrency Safety
- Dedicated threads MUST be allocated for USB I/O, Camera capture, Audio capture, Video Encoding, and RTMP Socket I/O.
- Heavy operations (e.g., JPEG decoding or YUV conversion) MUST NEVER execute on the UI thread or USB I/O polling thread.

### NFR-03: Memory & Resource Leak Prevention
- Zero memory leakage during long streaming sessions (> 4 hours).
- All native resources (`Surface`, `MediaCodec`, `AudioRecord`, `UsbDeviceConnection`, `EGLContext`) MUST have an explicit `release()` protocol in their lifecycle owner.

### NFR-04: Security
- Stream keys and server credentials MUST be encrypted using `EncryptedSharedPreferences`.
- Stream keys MUST NEVER appear in `Logcat`, crash dumps, UI text logs, or version control.

### NFR-05: Capability Detection & Reality Rule
- Features MUST NOT assume hardware capabilities. Runtime checks MUST classify hardware support into: `SUPPORTED`, `PARTIALLY_SUPPORTED`, `NOT_SUPPORTED`, `REQUIRES_PERMISSION`, `REQUIRES_EXTERNAL_HARDWARE`.
