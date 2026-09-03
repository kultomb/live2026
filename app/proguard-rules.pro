# ProGuard Rules for Professional Android Mobile Live Production App

# Preserve Native USB / UVC Class Entry Points
-keep class android.hardware.usb.** { *; }
-keepclassmembers class * extends android.hardware.usb.UsbDevice { *; }
-keep class com.liveproduction.core.usb.** { *; }

# Preserve EGL14 & OpenGL ES 3.0 Callbacks
-keep class android.opengl.** { *; }
-keep class javax.microedition.khronos.** { *; }
-keep class com.liveproduction.core.media.egl.** { *; }
-keep class com.liveproduction.core.media.opengl.** { *; }

# Preserve MediaCodec & AudioRecord Entry Points
-keep class android.media.MediaCodec** { *; }
-keep class android.media.MediaFormat** { *; }
-keep class android.media.AudioRecord** { *; }
-keep class com.liveproduction.core.media.encoder.** { *; }
-keep class com.liveproduction.core.audio.** { *; }

# Preserve EncryptedSharedPreferences & Security Crypto
-keep class androidx.security.crypto.** { *; }

# Preserve Jetpack Compose Annotations
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Structured Logging & Diagnostics Models
-keep class com.liveproduction.core.diagnostics.model.** { *; }
-keep class com.liveproduction.core.streaming.model.** { *; }
