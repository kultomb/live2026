# PRODUCT SPECIFICATION

## 1. VISION & PURPOSE
A professional-grade mobile live production studio app for Android, bringing broadcasting capabilities (similar to OBS Studio / Atem Mini) directly to mobile devices. It connects external HDMI cameras via USB UVC capture cards, interfaces with device cameras (multi-lens), mixes multi-source audio, applies scene layouts, encodes hardware-accelerated H.264/AAC, and streams reliably via RTMP/RTMPS.

## 2. KEY TARGET USERS & USE CASES
- Outdoor broadcasters & journalists using HDMI DSLR/mirrorless cameras connected via USB UVC to Android phones.
- Multi-camera live event streamers switching between HDMI video, front vlog camera, and rear wide/telephoto cameras.
- Content creators streaming to YouTube Live, Facebook Live, Twitch, and Custom RTMP/RTMPS destinations.

## 3. CORE FEATURES & FUNCTIONALITIES

### A. Video Source Input Management
1. **USB UVC HDMI Capture Input**:
   - Automatic detection of USB video class devices.
   - Format support: MJPEG, YUY2, NV21.
   - Dynamic resolution matching (720p, 1080p up to 60fps where hardware permits).
   - Hot-plug handling (attach, detach, grant permission, auto-reconnect).
2. **Android Camera2 Input**:
   - Query logical and physical camera characteristics.
   - Explicit identification of Front, Rear Main, Rear Ultra Wide, Rear Telephoto, Macro.
   - Hardware controls: Manual focus, exposure compensation, white balance, zoom crop region, torch toggle.
   - Zero-teardown camera switching (switch source without resetting the live stream encoder).

### B. Scene Production & Compositing
1. **Scene Presets**:
   - Single HDMI Fullscreen
   - Single Front Camera / Rear Camera Fullscreen
   - Picture-in-Picture (PiP) with customizable position and size
   - Split Screen (50/50, 70/30)
   - Overlays: Lower-third text, image/logo watermark, real-time clock/timer.
2. **Seamless Switching**:
   - Glitch-free transition between video sources (Cut, Fade/Crossfade).
   - Frame Router layer to isolate source capture from encoder pipeline.

### C. Audio Management & Mixing
1. **Audio Sources**:
   - USB Capture Card Audio (PCM via UVC/UAC)
   - External USB Microphone / Audio Interface
   - Android Internal Microphone
   - Bluetooth Headset (where supported by OS)
2. **Mixer Features**:
   - Multi-channel software gain control & volume sliders.
   - Mute / Solo toggles.
   - Real-time VU level meters with peak/clipping detection.
   - Sample rate conversion to unified 44.1kHz / 48kHz stereo AAC encoding.
   - Hardware A/V drift compensation & timestamp synchronization.

### D. Media Encoding & Recording
1. **Video Encoding**:
   - Hardware-accelerated `MediaCodec` (H.264 / AVC).
   - Dynamic bitrate adjustment (1 Mbps to 12 Mbps).
   - Keyframe interval (GOP size) control (1s - 2s for RTMP).
   - Support for 720p@30fps, 1080p@30fps, 1080p@60fps (based on device capabilities).
2. **Audio Encoding**:
   - Hardware `MediaCodec` (AAC-LC, 128kbps - 320kbps).
3. **Local MP4 Recording**:
   - Concurrent local recording onto device storage via `MediaMuxer` without re-encoding capture.
   - Storage space threshold warnings and safety auto-stop.

### E. RTMP / RTMPS Streaming Engine
1. **Protocols**: RTMP and RTMPS (TLS/SSL encryption).
2. **Destinations**: YouTube Live, Facebook Live, Custom RTMP/RTMPS endpoints.
3. **Stream Key Security**: Encrypted storage using Android KeyStore / EncryptedSharedPreferences; no keys in logs or crash reports.
4. **Network Resilience**:
   - Real-time upload bandwidth estimation & dropped-frame monitoring.
   - Bounded exponential backoff auto-reconnect (up to 5 attempts before user alert).
   - Frame buffer queue overflow protection (drop oldest video frame to prevent streaming latency build-up).

### F. Monitoring & Diagnostics UI
1. **Live Preview**: Independent high-framerate OpenGL Surface rendering.
2. **Real-time Status Overlay**:
   - Current FPS, actual encoded bitrate, dropped frame count, network RTT/throughput.
   - Battery level, temperature/thermal status, storage remaining.
   - Session state badge (IDLE, PREPARING, READY, LIVE, RECONNECTING, ERROR).

## 4. CONSTRAINTS & NON-GOALS FOR INITIAL PHASES
- **Non-Goal**: Fake camera lens switching when hardware HAL does not expose physical IDs.
- **Non-Goal**: Hidden background video capture without Android Foreground Service notification.
- **Constraint**: HDMI capture performance is hardware-bound by device USB host controller speed (USB 2.0 vs USB 3.0).
