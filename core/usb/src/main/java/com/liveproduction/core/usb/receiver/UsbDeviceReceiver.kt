package com.liveproduction.core.usb.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.liveproduction.core.usb.UsbCaptureManager

class UsbDeviceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

        Log.i(TAG, "USB Broadcast received: Action=$action, Device=${device?.deviceName}")

        val usbCaptureManager = UsbCaptureManager.getInstance()

        when (action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                Log.i(TAG, "USB Device Attached: ${device?.deviceName}")
                usbCaptureManager.inspectAttachedDevices(context)
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                Log.i(TAG, "USB Device Detached: ${device?.deviceName}")
                usbCaptureManager.onUsbDeviceDetached(device)
            }
            UsbCaptureManager.ACTION_USB_PERMISSION -> {
                synchronized(this) {
                    val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    Log.i(TAG, "USB Permission result for ${device?.deviceName}: Granted=$permissionGranted")
                    usbCaptureManager.onPermissionResult(device, permissionGranted)
                }
            }
        }
    }

    companion object {
        private const val TAG = "UsbDeviceReceiver"
    }
}
