# DEBUG PROTOCOL & ROOT CAUSE ANALYSIS

## 1. MANDATORY 9-STEP DEBUG WORKFLOW
When diagnosing any issue or bug report, the AI agent MUST follow this exact sequence:

1. **REPRODUCE**: Identify exact steps, hardware setup (phone model, USB card, camera type), and environmental conditions.
2. **COLLECT EVIDENCE**: Inspect raw Logcat outputs, stack traces, frame rate counters, and network logs. Never guess.
3. **TRACE**: Follow the execution path across single owner managers (`UsbCaptureManager` -> `FrameRouter` -> `VideoMixer` -> `EncoderManager` -> `StreamManager`).
4. **IDENTIFY ROOT CAUSE**: Determine whether the defect is HAL/hardware limitations, threading race conditions, lifecycle tearing, or logic error.
5. **IMPACT ANALYSIS**: Evaluate if fixing the root cause affects other components (e.g. changing buffer size affects audio sync).
6. **MINIMAL SAFE FIX**: Apply the minimal targeted fix directly addressing the root cause. Avoid heavy refactoring during bug fixes.
7. **VALIDATE**: Run compilation, unit tests, and runtime verification.
8. **REGRESSION CHECK**: Verify that camera switching, USB hot-plug, and RTMP streaming still work cleanly.
9. **UPDATE PROJECT MEMORY**: Document the issue and fix in `KNOWN_ISSUES.md` and `CHANGELOG_AI.md`.

## 2. LOGGING TAG STRUCTURE
All system logs MUST use unified, structured tags for rapid filtering in Logcat:
- `[LIVE_SESSION]` - State machine transitions
- `[USB_UVC]` - Device connection, endpoints, frame extraction
- `[CAMERA2]` - Lens selection, session state, characteristics
- `[AUDIO_MIXER]` - Buffer read, gain, sample rate, VU metrics
- `[VIDEO_PIPELINE]` - EGL context, frame router, compositing
- `[ENCODER]` - MediaCodec format, keyframe requests, buffer flags
- `[RTMP_ENGINE]` - Socket state, FLV packaging, TCP write throughput
- `[NETWORK]` - Connectivity change, RTT, adaptive bitrate adjustment
- `[DIAGNOSTICS]` - FPS, dropped frames, thermal pressure, battery

## 3. LOGGING SECURITY RESTRICTION
- **NEVER LOG**: Stream keys, user passwords, OAuth tokens, or full socket payload dumps containing private data.
