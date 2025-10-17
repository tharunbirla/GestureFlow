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
    private val PREFS_NAME = "gestureflow_prefs"
    private val PREF_KEY_SENSITIVITY = "sensitivity"
    private var prefsListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "GestureFlow Accessibility Service Connected!")

        // Initialize SensorDataManager, passing this service as the context and listener
        sensorDataManager = SensorDataManager(this, this)

        // Read saved sensitivity from SharedPreferences and initialize GestureDetector with it
        val sharedPrefs = getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val initialSensitivity = sharedPrefs.getFloat(PREF_KEY_SENSITIVITY, 0.5f)
        gestureDetector = GestureDetector(this, initialSensitivity)

        // Listen for preference changes so we can update sensitivity at runtime
        prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_KEY_SENSITIVITY) {
                val newVal = sharedPrefs.getFloat(PREF_KEY_SENSITIVITY, 0.5f)
                gestureDetector.setSensitivity(newVal)
                Log.d(TAG, "SharedPreferences changed: sensitivity=$newVal")
            }
        }
        prefsListener?.let { sharedPrefs.registerOnSharedPreferenceChangeListener(it) }

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
        // Unregister preference listener
        try {
            prefsListener?.let { getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister prefs listener: ${e.message}")
        }
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

        // Check user preferences for whether each gesture is enabled
        val prefs = getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        when (gesture) {
            Gesture.BACK -> {
                val enabled = prefs.getBoolean("gesture_back_enabled", true)
                if (enabled) triggerGlobalAction(GLOBAL_ACTION_BACK)
                else Log.d(TAG, "Back gesture disabled by user preferences")
            }
            Gesture.MULTITASKING -> {
                val enabled = prefs.getBoolean("gesture_multitasking_enabled", true)
                if (enabled) triggerGlobalAction(GLOBAL_ACTION_RECENTS)
                else Log.d(TAG, "Multitasking gesture disabled by user preferences")
            }
            Gesture.NOTIFICATION_PULL_DOWN -> {
                val enabled = prefs.getBoolean("gesture_notifications_enabled", true)
                if (enabled) triggerGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                else Log.d(TAG, "Notification pull-down gesture disabled by user preferences")
            }
            Gesture.NONE -> {
                // Do nothing
            }
        }
    }
}