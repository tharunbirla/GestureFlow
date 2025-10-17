package com.tharunbirla.gestureflow

import android.hardware.SensorManager
import android.util.Log

interface GestureListener {
    fun onGestureDetected(gesture: Gesture)
}

enum class Gesture {
    BACK,
    MULTITASKING,
    NOTIFICATION_PULL_DOWN,
    NONE // No gesture detected
}

class GestureDetector(private val listener: GestureListener, initialSensitivity: Float = 0.5f) {

    private val TAG = "GestureDetector"

    // --- Base thresholds (these represent "default" required peaks). They will be scaled by sensitivity.
    // Lower sensitivity value => more sensitive (smaller magnitude required). This matches the UI: "Lower = More Sensitive".
    private val BASE_ACCEL_BACK_Y = -15.0f // Negative for backward flick (e.g., pulling phone towards you)
    private val BASE_ACCEL_MULTITASKING_X = 15.0f // Positive for quick flick right (or negative for left)
    private val BASE_ACCEL_NOTIFICATION_Z = -15.0f // Negative for downward flick (e.g., quickly lowering phone)

    private val GYRO_THRESHOLD_DEGREES_PER_SEC = 50.0f // Optional, for future use or more complex gestures
    private val COOLDOWN_MILLIS = 700L // Time to wait after a gesture before detecting another (in ms)

    private var lastGestureDetectedTime: Long = 0

    // User-adjustable sensitivity: range 0.1..1.0 (lower = more sensitive). Default 0.5f.
    private var sensitivity: Float = initialSensitivity.coerceIn(0.1f, 1.0f)

    // Current effective thresholds (computed from base * sensitivity)
    private var accelThresholdBackY: Float = BASE_ACCEL_BACK_Y * sensitivity
    private var accelThresholdMultitaskingX: Float = BASE_ACCEL_MULTITASKING_X * sensitivity
    private var accelThresholdNotificationZ: Float = BASE_ACCEL_NOTIFICATION_Z * sensitivity

    /** Update sensitivity at runtime; gestures will use recalculated thresholds immediately. */
    fun setSensitivity(value: Float) {
        // New mapping: slider value 'value' ranges 0.1..1.0 where higher = more sensitive.
        // We map that to a multiplier that reduces thresholds as sensitivity increases.
        sensitivity = value.coerceIn(0.1f, 1.0f)
        // Compute a factor in range [1.0 .. 0.3] where 1.0 = least sensitive, 0.3 = most sensitive
        val min = 0.1f
        val max = 1.0f
        val factor = 1.0f - ((sensitivity - min) / (max - min)) * 0.7f
        accelThresholdBackY = BASE_ACCEL_BACK_Y * factor
        accelThresholdMultitaskingX = BASE_ACCEL_MULTITASKING_X * factor
        accelThresholdNotificationZ = BASE_ACCEL_NOTIFICATION_Z * factor
        // Log updated thresholds for debugging/tuning
        Log.d(TAG, "Sensitivity updated: $sensitivity. Thresholds: backY=$accelThresholdBackY, multitaskX=$accelThresholdMultitaskingX, notifZ=$accelThresholdNotificationZ")
    }

    // Store previous accelerometer values for motion detection (optional, but can help with velocity-based detection)
    // private var previousAccelX = 0f
    // private var previousAccelY = 0f
    // private var previousAccelZ = 0f

    fun processSensorData(accelerometerData: FloatArray, gyroscopeData: FloatArray) {
        val currentTime = System.currentTimeMillis()

        // Apply cool-down period
        if (currentTime - lastGestureDetectedTime < COOLDOWN_MILLIS) {
            return // Still in cool-down, ignore new data
        }

    val accelX = accelerometerData[0]
    val accelY = accelerometerData[1]
    val accelZ = accelerometerData[2]

        // Log filtered data for debugging and tuning
        // Log.d(TAG, "Accel: X=%.2f, Y=%.2f, Z=%.2f".format(accelX, accelY, accelZ))
        // Log.d(TAG, "Gyro: X=%.2f, Y=%.2f, Z=%.2f".format(gyroscopeData[0], gyroscopeData[1], gyroscopeData[2]))


        // --- Gesture Detection Logic ---
        var detectedGesture = Gesture.NONE

        // 1. Back Gesture (Example: Quick flick along Y-axis, pulling phone towards you)
        // Looking for a sharp negative peak in Y-acceleration
        if (accelY < accelThresholdBackY) {
            detectedGesture = Gesture.BACK
            Log.d(TAG, "Back Gesture Detected! Accel Y: $accelY threshold=$accelThresholdBackY")
        }
        // 2. Multitasking Gesture (Example: Quick flick along X-axis, e.g., to the right)
        // Looking for a sharp positive peak in X-acceleration
        else if (accelX > accelThresholdMultitaskingX) {
            detectedGesture = Gesture.MULTITASKING
            Log.d(TAG, "Multitasking Gesture Detected! Accel X: $accelX threshold=$accelThresholdMultitaskingX")
        }
        // 3. Notification Pull-down (Example: Quick flick along Z-axis, phone moving down)
        // Looking for a sharp negative peak in Z-acceleration
        else if (accelZ < accelThresholdNotificationZ) {
            detectedGesture = Gesture.NOTIFICATION_PULL_DOWN
            Log.d(TAG, "Notification Pull-down Gesture Detected! Accel Z: $accelZ threshold=$accelThresholdNotificationZ")
        }

        // Notify listener if a gesture was detected
        if (detectedGesture != Gesture.NONE) {
            listener.onGestureDetected(detectedGesture)
            lastGestureDetectedTime = currentTime // Reset cooldown timer
        }

        // Update previous values (if using velocity-based detection in the future)
        // previousAccelX = accelX
        // previousAccelY = accelY
        // previousAccelZ = accelZ
    }
}