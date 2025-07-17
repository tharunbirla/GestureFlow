package com.tharunbirla.gestureflow

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import android.accessibilityservice.AccessibilityServiceInfo
import android.hardware.SensorManager
import android.view.accessibility.AccessibilityNodeInfo // Import for global actions

// Implement both SensorDataListener and GestureListener interfaces
class GestureFlowAccessibilityService : AccessibilityService(), SensorDataListener, GestureListener {

    private val TAG = "GestureFlowService"
    private lateinit var sensorDataManager: SensorDataManager
    private lateinit var gestureDetector: GestureDetector // Declare GestureDetector

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "GestureFlow Accessibility Service Connected!")

        // Initialize SensorDataManager, passing this service as the context and listener
        sensorDataManager = SensorDataManager(this, this)

        // Initialize GestureDetector, passing this service as the GestureListener
        gestureDetector = GestureDetector(this)

        sensorDataManager.startListening()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // ... (no changes here for now)
    }

    override fun onInterrupt() {
        Log.d(TAG, "GestureFlow Accessibility Service Interrupted.")
        sensorDataManager.stopListening() // Stop listening to sensors on interruption
        // No explicit stop needed for GestureDetector, as it's passive
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "GestureFlow Accessibility Service Destroyed.")
        sensorDataManager.stopListening() // Always unregister sensors when service is destroyed
    }

    /**
     * Helper function to perform global actions like Back, Home, Recents, etc.
     * This calls the superclass's performGlobalAction method.
     */
    fun triggerGlobalAction(action: Int): Boolean {
        // Log the action for debugging
        val actionName = when (action) {
            GLOBAL_ACTION_BACK -> "BACK"
            GLOBAL_ACTION_RECENTS -> "RECENTS (Multitasking)"
            GLOBAL_ACTION_NOTIFICATIONS -> "NOTIFICATIONS"
            else -> "UNKNOWN"
        }
        Log.d(TAG, "Attempting to perform global action: $actionName")
        return super.performGlobalAction(action)
    }

    // --- SensorDataListener implementation ---
    override fun onFilteredSensorData(accelerometerData: FloatArray, gyroscopeData: FloatArray) {
        // Pass the filtered data to our GestureDetector for processing
        gestureDetector.processSensorData(accelerometerData, gyroscopeData)
    }

    // --- GestureListener implementation ---
    override fun onGestureDetected(gesture: Gesture) {
        // This callback is triggered when GestureDetector identifies a gesture
        Log.d(TAG, "Detected gesture: $gesture")

        when (gesture) {
            Gesture.BACK -> {
                triggerGlobalAction(GLOBAL_ACTION_BACK)
            }
            Gesture.MULTITASKING -> {
                triggerGlobalAction(GLOBAL_ACTION_RECENTS) // GLOBAL_ACTION_RECENTS usually means multitasking
            }
            Gesture.NOTIFICATION_PULL_DOWN -> {
                triggerGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            }
            Gesture.NONE -> {
                // Do nothing
            }
        }
    }
}