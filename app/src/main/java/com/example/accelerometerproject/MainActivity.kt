package com.example.accelerometerproject

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var prefs: SharedPreferences

    private lateinit var buttonResetSteps: Button
    private lateinit var textViewSteps: TextView

    private var previousMagnitude = 0f
    private var stepCount = 0
    private val threshold = 12.5  // Sensitivity threshold for step detection
    private var lastStepTime = 0L
    private val cooldown = 300L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
        stepCount = prefs.getInt("step_count", 0)

        buttonResetSteps = findViewById(R.id.resetStepsButton)
        textViewSteps = findViewById(R.id.stepsCounter)

        buttonResetSteps.setOnClickListener {
            stepCount = 0
            saveSteps()
            updateStepDisplay()
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            textViewSteps.text = "El accelerómetro no está disponible en este dispositivo."
        }

        updateStepDisplay()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        saveSteps()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val currentTime = System.currentTimeMillis()
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate acceleration magnitude
            val magnitude = sqrt(x * x + y * y + z * z.toDouble())

            // Step detection logic:
            // 1. Check if magnitude crosses the threshold
            // 2. Ensure minimum time between steps (cooldown)
            // 3. Check if it's a peak (current magnitude greater than previous)
            if (magnitude > threshold &&
                (currentTime - lastStepTime) > cooldown &&
                magnitude > previousMagnitude) {
                stepCount++
                lastStepTime = currentTime
                updateStepDisplay()
            }
            previousMagnitude = magnitude.toFloat()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //Not necessary for this example
    }

    private fun updateStepDisplay() {
        textViewSteps.text = "Pasos: $stepCount"
    }

    private fun saveSteps() {
        prefs.edit().putInt("step_count", stepCount).apply()
    }
}