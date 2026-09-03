# CAMERA ARCHITECTURE (ANDROID CAMERA2)

## 1. DESIGN & API CHOICE
- **API Choice**: `android.hardware.camera2` (Camera2 API).
- **Rationale**: Direct low-level access to physical multi-lens camera IDs, explicit Surface target routing, manual exposure/focus control, and EGL SurfaceTexture integration without CameraX abstraction overhead.

## 2. CAMERA CHARACTERISTICS & LENS DISCOVERY
- Query `CameraManager.getCameraIdList()`.
- For each camera ID, inspect `CameraCharacteristics`:
  - `LENS_FACING`: `LENS_FACING_FRONT` vs `LENS_FACING_BACK`.
  - `REQUEST_AVAILABLE_CAPABILITIES`: Check for `LOGICAL_MULTI_CAMERA`.
  - Physical Lens Query: On Android 9+ (API 28+), call `CameraCharacteristics.getPhysicalCameraIds()` to discover:
    - Ultra Wide (typically ~13mm focal length equivalent)
    - Wide / Main (typically ~24mm - 26mm focal length equivalent)
    - Telephoto (typically >= 50mm focal length equivalent)
    - Macro

## 3. CAPABILITY MAP CLASSIFICATION
Devices expose camera configurations differently:
- **LOGICAL_MULTI_CAMERA**: Single logical ID managing multiple physical lenses (preferred on modern Samsung/Pixel devices).
- **SEPARATE_CAMERA_IDS**: Device exposes physical lenses as separate Camera IDs (e.g. ID `0` = Main, ID `2` = Wide, ID `3` = Telephoto).
- **SINGLE_REAR_CAMERA**: Only one rear camera exposed by manufacturer HAL.

`CameraManager` builds a unified runtime `VideoSourceCapabilities` map:
- `SOURCE_FRONT_CAMERA` (`SUPPORTED`)
- `SOURCE_REAR_MAIN` (`SUPPORTED`)
- `SOURCE_REAR_ULTRAWIDE` (`SUPPORTED` | `NOT_SUPPORTED`)
- `SOURCE_REAR_TELEPHOTO` (`SUPPORTED` | `NOT_SUPPORTED`)

## 4. ZERO-TEARDOWN HOT SWITCHING PIPELINE
When switching from Front Camera to Rear Main Camera or HDMI input during a live stream:
1. `CameraManager` initiates asynchronous session close for current camera.
2. `FrameRouter` detaches current input stream without stopping the `VideoMixer` or `MediaCodec` encoder.
3. `VideoMixer` renders the last valid frame (or a smooth transition effect) to keep the encoder fed with valid timestamps.
4. New camera `CaptureSession` is configured with the persistent `SurfaceTexture` owned by `FrameRouter`.
5. Once target camera frame arrives, `FrameRouter` routes new frames into `VideoMixer`.
6. Encoder receives continuous timestamped frames without encountering EGL/codec resets or keyframe stream tears.

## 5. HARDWARE CONTROLS
- **Zoom**: Scaled using `CaptureRequest.CONTROL_ZOOM_RATIO` (API 30+) or `SCALER_CROP_REGION` (legacy fallback).
- **Focus**: `CONTROL_AF_MODE_CONTINUOUS_VIDEO` with tap-to-focus `CONTROL_AF_REGIONS`.
- **Exposure**: `CONTROL_AE_MODE_ON` with manual `CONTROL_AE_EXPOSURE_COMPENSATION`.
- **Torch**: `FLASH_MODE_TORCH` when supported by lens characteristics.
