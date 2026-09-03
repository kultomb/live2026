# DEVICE COMPATIBILITY & REALITY MATRIX

## 1. IMPORTANT REALITY RULE ENFORCEMENT
Never assume an Android device supports a feature simply because the API exists. Every feature MUST execute capability detection at runtime and report one of:
- `SUPPORTED`: Fully supported and verified by hardware query.
- `PARTIALLY_SUPPORTED`: Works with fallback or reduced quality (e.g. 720p instead of 1080p60).
- `NOT_SUPPORTED`: Hardware/HAL does not support feature.
- `REQUIRES_PERMISSION`: Requires user action (e.g., USB permission dialog, Camera/Mic runtime permission).
- `REQUIRES_EXTERNAL_HARDWARE`: Requires plugged-in USB capture card / HDMI camera.

## 2. HARDWARE CAPABILITY MATRIX CHECKLIST
During app launch or diagnostics screen, `DiagnosticsManager` inspects:

| Feature | Query API / Mechanism | Target Output |
|---|---|---|
| **USB Host Mode** | `pm.hasSystemFeature(FEATURE_USB_HOST)` | `Boolean` |
| **UVC Format Support** | USB descriptor query on attached device | `MJPEG`, `YUY2`, `NV21` |
| **Logical Multi-Camera** | `CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA` | `Boolean` |
| **Physical Cameras** | `CameraCharacteristics.getPhysicalCameraIds()` | `Set<String>` |
| **H.264 Encoder Level** | `MediaCodecList` query for `video/avc` profile & level | Highest resolution/FPS |
| **AAC Encoder** | `MediaCodecList` query for `audio/mp4a-latm` | `Boolean` |
| **Thermal Headroom** | `PowerManager.getCurrentThermalStatus()` (API 29+) | `THERMAL_STATUS_*` |
| **Audio Recording** | `pm.hasSystemFeature(FEATURE_MICROPHONE)` | `Boolean` |

## 3. KNOWN MANUFACTURER GOTCHAS & LIMITATIONS
- **Samsung Knox / Battery Optimization**: Aggressively kills background services. Foreground service with explicit notification is strictly required.
- **Xiaomi / MIUI Permission Model**: Requires "Display pop-up windows while running in background" permission for certain overlay dialogs.
- **Google Pixel (Tensor SoC)**: Standard Camera2 logical multi-camera switching works seamlessly; USB UVC video bandwidth requires explicit buffer allocation.
- **Low-Cost MediaTek SoCs**: USB 2.0 host controller throughput might cap MJPEG decoding to 1080p@30fps or 720p@30fps.
