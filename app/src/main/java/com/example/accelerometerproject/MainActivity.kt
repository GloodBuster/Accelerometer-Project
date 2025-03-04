package com.example.accelerometerproject

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var prefs: SharedPreferences
    private lateinit var sqliteAdapter: MySQLiteHelper

    private lateinit var buttonResetSteps: Button
    private lateinit var textViewSteps: TextView
    private lateinit var saveButton: Button

    private var previousMagnitude = 0f
    private var stepCount = 0
    private val threshold = 12.5  // Sensitivity threshold for step detection
    private var lastStepTime = 0L
    private val cooldown = 300L

    private lateinit var stepsChart: LineChart

    private var steps: List<Steps> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("step_prefs", Context.MODE_PRIVATE)
        stepCount = prefs.getInt("step_count", 0)
        sqliteAdapter = MySQLiteHelper(this)

        buttonResetSteps = findViewById(R.id.resetStepsButton)
        textViewSteps = findViewById(R.id.stepsCounter)
        saveButton = findViewById(R.id.saveButton)

        stepsChart = findViewById(R.id.stepsChart)

        buttonResetSteps.setOnClickListener {
            stepCount = 0
            saveSteps()
            updateStepDisplay()
        }

        saveButton.setOnClickListener {
            sqliteAdapter.addSteps(stepCount)
            Toast.makeText(this, "Pasos diarios guardados", Toast.LENGTH_SHORT).show()

            stepCount = 0
            saveSteps()
            updateStepDisplay()
            loadSteps()
            loadChartData()
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            textViewSteps.text = "El accelerómetro no está disponible en este dispositivo."
        }

        updateStepDisplay()
        setupChart()
        loadSteps()
        loadChartData()
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

    private fun loadSteps() {
        steps = sqliteAdapter.getSteps()
    }

    private fun setupChart() {
        stepsChart.apply {
            description.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f // Set granularity to 1 to avoid decimal values
            xAxis.valueFormatter = DateValueFormatter()
            xAxis.textColor = Color.parseColor("#D5EFF2")
            axisLeft.axisMinimum = 0f // Start Y axis from 0
            axisRight.isEnabled = false
            axisLeft.textColor = Color.parseColor("#D5EFF2")

            // Add these settings
            setNoDataText("No hay datos históricos")
            setTouchEnabled(true)
            setPinchZoom(true)
        }
    }

    private fun loadChartData() {
        val stepsList = sqliteAdapter.getSteps()
        steps = stepsList  // Update the class-level list

        if (stepsList.isNotEmpty()) {
            val entries = stepsList.mapIndexed { index, step ->
                Entry(index.toFloat(), step.steps.toFloat())
            }

            val dataSet = LineDataSet(entries, "Historial de Pasos").apply {
                color = Color.parseColor("#258697")
                valueTextColor = Color.parseColor("#D5EFF2")
                setDrawCircles(true)
                lineWidth = 2f
            }

            stepsChart.apply {
                data = LineData(dataSet)
                legend.textColor = Color.parseColor("#D5EFF2")
                xAxis.labelCount = stepsList.size.coerceAtMost(5) // Show max 5 labels
                notifyDataSetChanged()
                invalidate()
            }
        }
    }

    inner class DateValueFormatter : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            val index = value.toInt()
            return if (index >= 0 && index < steps.size) {
                // Show date in DD/MM format
                val dateParts = steps[index].date.split("-")
                if (dateParts.size == 3) "${dateParts[2]}/${dateParts[1]}" else ""
            } else {
                ""
            }
        }
    }
}