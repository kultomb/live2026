# USB UVC HDMI CAPTURE ARCHITECTURE

## 1. OVERVIEW & USB HOST ARCHITECTURE
External HDMI cameras output uncompressed HDMI signals. Common HDMI-to-USB capture dongles (e.g. Cam Link 4K, MS2109/MS2130 budget capture cards) present themselves as standard **USB Video Class (UVC)** and **USB Audio Class (UAC)** devices.

Android app acts as **USB Host** via `android.hardware.usb.UsbManager` or native `libusb` / `libuvc`.

```
 +-------------------------------------------------------+
 |               USB HDMI CAPTURE HARDWARE               |
 +-------------------------------------------------------+
                            |
                            v  USB 2.0 / USB 3.0 Bus
 +-------------------------------------------------------+
 |               ANDROID USB HOST PIPELINE               |
 |                                                       |
 |  1. UsbManager BroadcastReceiver (ATTACH / DETACH)   |
 |  2. Permission Request (ACTION_USB_PERMISSION)        |
 |  3. UsbDeviceConnection / UsbInterface Open           |
 |  4. UVC Streaming Isochronous / Bulk Endpoint Read    |
 +-------------------------------------------------------+
                            |
                            v  Raw USB Packets
 +-------------------------------------------------------+
 |               UVC FRAME PARSER THREAD                 |
 |                                                       |
 |  - MJPEG Frame Decompressor (TurboJPEG / Libjpeg-turbo)|
 |  - YUY2 / NV21 Pixel Format Converter                 |
 |  - Ring Buffer Producer (Drop oldest on overflow)      |
 +-------------------------------------------------------+
                            |
                            v  OpenGL YUV/RGB Texture
 +-------------------------------------------------------+
 |               FRAME ROUTER & VIDEO MIXER              |
 +-------------------------------------------------------+
```

## 2. FORMAT SUPPORT & CONVERSION
- **MJPEG (Motion JPEG)**: Most common format for USB 2.0 capture cards delivering 1080p30 or 720p60.
  - Native TurboJPEG decompression converts MJPEG payload into YUV420P / NV21 / RGBA byte buffers or EGL surface textures.
- **YUY2 (Uncompressed YUV 4:2:2)**: High-quality format used in USB 3.0 capture cards (720p60 / 1080p60).
  - Converted via OpenGL Fragment Shader or SIMD C++ function directly to RGBA/NV21.
- **NV12 / NV21**: Standard Android MediaCodec input format.

## 3. HOT-PLUG & RECONNECT LIFECYCLE
1. **USB Attach**: `UsbManager.ACTION_USB_DEVICE_ATTACHED` fires -> `UsbCaptureManager` validates vendor ID / product ID and UVC interface descriptor.
2. **Permission Check**: If permission not granted, request via `PendingIntent`. Upon grant -> open device connection.
3. **Stream Init**: Configure USB Isochronous/Bulk transfer endpoint, allocate USB transfer buffers (e.g., 8 x 64KB buffers).
4. **USB Detach**: `UsbManager.ACTION_USB_DEVICE_DETACHED` fires:
   - Immediately interrupt `UsbIoThread`.
   - Release USB interfaces and connection handles.
   - Switch active scene seamlessly to fallback camera (e.g., Rear Main) without breaking live stream session.
   - Set status badge to `USB_DISCONNECTED` and wait for re-attach.

## 4. PERFORMANCE & THREADING RULES
- **Rule 1**: USB polling thread (`UsbIoThread`) MUST ONLY read raw USB packets into memory buffers.
- **Rule 2**: Frame decoding (MJPEG decompression) MUST take place on worker thread pool (`UvcDecoderPool`).
- **Rule 3**: Zero dynamic byte array allocation in the frame parsing loop. Pre-allocate fixed frame buffers during initialization.
