# BUG PREVENTION & THREADING RULES

## 1. SINGLE OWNER COMPONENT RULE
- NEVER create a second instance of `CameraManager`, `UsbCaptureManager`, `StreamManager`, or `LiveSessionManager`.
- Use Dependency Injection (Hilt / Koin or explicit singleton scope) to enforce single instance ownership.

## 2. BUTTON SPAM & ASYNCHRONOUS STATE PROTECTION
- Every async button action (GO LIVE, SWITCH CAMERA, RECORD, CONNECT USB) MUST be guarded by state check.
- If `LiveSessionManager.state` is `STARTING` or `STOPPING`, button inputs MUST be ignored or disabled in UI.

## 3. THREADING & COROUTINE SCOPE RULES
- **NO GlobalScope**: Never use `GlobalScope.launch` or `GlobalScope.async`.
- **Structured Concurrency**: All coroutines must be launched within a lifecycle-aware scope (`viewModelScope`, `ServiceScope`, or a custom `CoroutineScope` with supervisor job tied to module lifecycle).
- **Explicit Dispatchers**:
  - `Dispatchers.Main`: UI rendering and state flow updates only.
  - `Dispatchers.IO`: File I/O, network socket reads/writes, KeyStore access.
  - Dedicated SingleThreadContext (`Executors.newSingleThreadExecutor()`): USB I/O, Camera callback processing, Audio capture loop.

## 4. MEMORY LEAK PREVENTION PROTOCOL
Before completing any pull request or module:
1. **Unregister Listeners**: Ensure all `BroadcastReceiver`, `NetworkCallback`, `UsbManager` listeners, and `DisplayListener` instances are unregistered in `onStop()` / `release()`.
2. **Surface & Texture Cleanup**: Explicitly call `.release()` on `Surface`, `SurfaceTexture`, `EGLDisplay`, and `MediaCodec`.
3. **Handler/Timer Cancellation**: Cancel all pending `Handler` messages and `ScheduledExecutorService` timers on teardown.

## 5. BUFFER OVERFLOW & LATENCY CONTROL
- Frame queues between Capture -> Mixer -> Encoder MUST have a strict max capacity limit (e.g. 4 frames).
- If queue is full when a new video frame arrives, drop the oldest frame. Latency buildup is unacceptable for live production.
