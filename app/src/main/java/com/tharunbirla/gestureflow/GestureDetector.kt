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

class GestureDetector(private val listener: GestureListener) {

    private val TAG = "GestureDetector"

    // --- Adjustable Thresholds (Tune these based on your device and desired sensitivity) ---
    private val ACCEL_THRESHOLD_BACK_Y = -15.0f // Negative for backward flick (e.g., pulling phone towards you)
    private val ACCEL_THRESHOLD_MULTITASKING_X = 15.0f // Positive for quick flick right (or negative for left)
    private val ACCEL_THRESHOLD_NOTIFICATION_Z = -15.0f // Negative for downward flick (e.g., quickly lowering phone)

    private val GYRO_THRESHOLD_DEGREES_PER_SEC = 50.0f // Optional, for future use or more complex gestures
    private val COOLDOWN_MILLIS = 700L // Time to wait after a gesture before detecting another (in ms)

    private var lastGestureDetectedTime: Long = 0

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
        if (accelY < ACCEL_THRESHOLD_BACK_Y) {
            detectedGesture = Gesture.BACK
            Log.d(TAG, "Back Gesture Detected! Accel Y: $accelY")
        }
        // 2. Multitasking Gesture (Example: Quick flick along X-axis, e.g., to the right)
        // Looking for a sharp positive peak in X-acceleration
        else if (accelX > ACCEL_THRESHOLD_MULTITASKING_X) {
            detectedGesture = Gesture.MULTITASKING
            Log.d(TAG, "Multitasking Gesture Detected! Accel X: $accelX")
        }
        // 3. Notification Pull-down (Example: Quick flick along Z-axis, phone moving down)
        // Looking for a sharp negative peak in Z-acceleration
        else if (accelZ < ACCEL_THRESHOLD_NOTIFICATION_Z) {
            detectedGesture = Gesture.NOTIFICATION_PULL_DOWN
            Log.d(TAG, "Notification Pull-down Gesture Detected! Accel Z: $accelZ")
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