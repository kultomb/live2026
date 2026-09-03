# KNOWN ISSUES & WORKAROUND LOG

## 1. HARDWARE / HAL WORKAROUND TRACKER

| Issue ID | Affected Hardware / OS | Description | Root Cause / Workaround |
|---|---|---|---|
| **ISSUE-001** | Low-cost MS2109 USB Capture dongles | Audio sample rate reported as 96kHz or invalid | Force software resampler to 48kHz stereo PCM in `AudioPipeline`. |
| **ISSUE-002** | Select MediaTek Devices (Android 11) | `Camera2` fails to open physical wide lens directly | Fall back to logical camera ID with manual zoom ratio crop. |
| **ISSUE-003** | Android 14 (API 34) Foreground Service | Strict service type enforcement for background streaming | Declare `foregroundServiceType="camera|connectedDevice|microphone"` in `AndroidManifest.xml`. |
| **ISSUE-004** | USB 2.0 Host Bandwidth Limit | MJPEG 1080p60 frame corruption due to USB packet loss | Cap USB UVC requested format to 1080p30 or 720p60 on USB 2.0 ports. |

## 2. ACTIVE BUG LIST
*(Currently 0 active bugs in initial setup phase)*
