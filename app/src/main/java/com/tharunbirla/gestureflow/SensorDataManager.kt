package com.tharunbirla.gestureflow

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

// Define a listener interface for filtered sensor data
interface SensorDataListener {
    fun onFilteredSensorData(accelerometerData: FloatArray, gyroscopeData: FloatArray)
}

class SensorDataManager(private val context: Context, private val listener: SensorDataListener) : SensorEventListener {

    private val TAG = "SensorDataManager"

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    // Arrays to hold filtered sensor data
    private val filteredAccelerometerData = FloatArray(3)
    private val filteredGyroscopeData = FloatArray(3)

    // Low-pass filter coefficient (adjust as needed for smoothing)
    private val ALPHA = 0.8f // 0.0 to 1.0; closer to 1.0 means less smoothing

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (accelerometer == null) {
            Log.e(TAG, "Accelerometer not found on this device.")
        }
        if (gyroscope == null) {
            Log.e(TAG, "Gyroscope not found on this device.")
        }
    }

    fun startListening() {
        accelerometer?.let {
            // Register with SENSOR_DELAY_GAME for a good balance of responsiveness and battery usage
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Accelerometer listener registered.")
        }
        gyroscope?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Gyroscope listener registered.")
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        Log.d(TAG, "Sensor listeners unregistered.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Apply low-pass filter to accelerometer data
                for (i in 0..2) {
                    filteredAccelerometerData[i] = filteredAccelerometerData[i] + ALPHA * (event.values[i] - filteredAccelerometerData[i])
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                // Apply low-pass filter to gyroscope data
                for (i in 0..2) {
                    filteredGyroscopeData[i] = filteredGyroscopeData[i] + ALPHA * (event.values[i] - filteredGyroscopeData[i])
                }
            }
        }
        // Pass filtered data to the listener (which will be our GestureDetector)
        listener.onFilteredSensorData(filteredAccelerometerData, filteredGyroscopeData)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Can be used to monitor sensor accuracy changes if needed
        Log.d(TAG, "Sensor accuracy changed: ${sensor?.name}, accuracy: $accuracy")
    }
}