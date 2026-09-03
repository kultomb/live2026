package com.liveproduction.core.usb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.liveproduction.core.usb.model.UsbDeviceStatus
import com.liveproduction.core.usb.model.UvcFormatDescriptor
import com.liveproduction.core.usb.model.UvcFrameFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UsbCaptureManager private constructor() {

    private val _usbStatus = MutableStateFlow(UsbDeviceStatus.NO_DEVICE_ATTACHED)
    val usbStatus: StateFlow<UsbDeviceStatus> = _usbStatus.asStateFlow()

    private val _supportedFormats = MutableStateFlow<List<UvcFormatDescriptor>>(emptyList())
    val supportedFormats: StateFlow<List<UvcFormatDescriptor>> = _supportedFormats.asStateFlow()

    private var activeDevice: UsbDevice? = null
    private var activeConnection: UsbDeviceConnection? = null

    fun inspectAttachedDevices(context: Context) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        if (usbManager == null) {
            Log.e(TAG, "UsbManager unavailable on device")
            return
        }

        val deviceList = usbManager.deviceList
        Log.i(TAG, "Attached USB devices count: ${deviceList.size}")

        var uvcDeviceFound: UsbDevice? = null
        for ((_, device) in deviceList) {
            if (isUvcDevice(device)) {
                uvcDeviceFound = device
                break
            }
        }

        if (uvcDeviceFound != null) {
            activeDevice = uvcDeviceFound
            val hasPermission = usbManager.hasPermission(uvcDeviceFound)
            Log.i(TAG, "Found UVC Device: ${uvcDeviceFound.deviceName}, Has Permission: $hasPermission")
            if (hasPermission) {
                _usbStatus.value = UsbDeviceStatus.CONNECTING
                openDeviceConnection(context, uvcDeviceFound)
            } else {
                _usbStatus.value = UsbDeviceStatus.DEVICE_ATTACHED_PERMISSION_REQUIRED
                requestUsbPermission(context, uvcDeviceFound)
            }
        } else {
            activeDevice = null
            _usbStatus.value = UsbDeviceStatus.NO_DEVICE_ATTACHED
        }
    }

    fun requestUsbPermission(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            flags
        )

        Log.i(TAG, "Requesting explicit USB permission for ${device.deviceName}")
        usbManager.requestPermission(device, permissionIntent)
    }

    fun onPermissionResult(device: UsbDevice?, granted: Boolean) {
        if (device != null && activeDevice?.deviceId == device.deviceId) {
            if (granted) {
                Log.i(TAG, "USB Permission granted by user for ${device.deviceName}")
                _usbStatus.value = UsbDeviceStatus.CONNECTING
            } else {
                Log.w(TAG, "USB Permission denied by user for ${device.deviceName}")
                _usbStatus.value = UsbDeviceStatus.ERROR
            }
        }
    }

    fun onUsbDeviceDetached(device: UsbDevice?) {
        if (device == null || activeDevice?.deviceId == device.deviceId) {
            Log.i(TAG, "Active UVC Device detached. Cleaning up connection handles...")
            closeConnection()
            activeDevice = null
            _usbStatus.value = UsbDeviceStatus.DISCONNECTED
        }
    }

    private fun openDeviceConnection(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return
        try {
            val connection = usbManager.openDevice(device)
            if (connection != null) {
                activeConnection = connection
                Log.i(TAG, "Successfully opened USB Device Connection to ${device.deviceName}")

                // Standard UVC HDMI Dongle Descriptor Formats (1080p30 / 720p60 MJPEG)
                _supportedFormats.value = listOf(
                    UvcFormatDescriptor(UvcFrameFormat.MJPEG, 1920, 1080, 30),
                    UvcFormatDescriptor(UvcFrameFormat.MJPEG, 1280, 720, 60),
                    UvcFormatDescriptor(UvcFrameFormat.YUY2, 1280, 720, 30)
                )

                _usbStatus.value = UsbDeviceStatus.STREAMING_ACTIVE
            } else {
                Log.e(TAG, "Failed to open UsbDeviceConnection (Null handle returned)")
                _usbStatus.value = UsbDeviceStatus.ERROR
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening USB connection", e)
            _usbStatus.value = UsbDeviceStatus.ERROR
        }
    }

    private fun closeConnection() {
        try {
            activeConnection?.close()
            activeConnection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing UsbDeviceConnection", e)
        }
    }

    private fun isUvcDevice(device: UsbDevice): Boolean {
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            if (usbInterface.interfaceClass == 14) { // USB Video Class
                return true
            }
        }
        return false
    }

    companion object {
        private const val TAG = "UsbCaptureManager"
        const val ACTION_USB_PERMISSION = "com.liveproduction.studio.USB_PERMISSION"

        @Volatile
        private var INSTANCE: UsbCaptureManager? = null

        fun getInstance(): UsbCaptureManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UsbCaptureManager().also { INSTANCE = it }
            }
        }
    }
}
