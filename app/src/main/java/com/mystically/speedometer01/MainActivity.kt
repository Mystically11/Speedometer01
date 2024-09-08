package com.mystically.speedometer01

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mystically.speedometer01.data.AppDatabase
import com.mystically.speedometer01.data.SessionData
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : BaseActivity(), SensorEventListener {

    // session data shit
    private val sessionHistory = mutableListOf<SessionData>()

    private var sessionStartTime: Long = 0
    private var sessionStopTime: Long = 0
    private var totalDistance = 0.0
    private var totalTime = 0.0
    private var maxSpeed = 0.0f
    private var isRecording = false

    // ui items
    private lateinit var speedometer: SpeedometerView
    private lateinit var tripText: TextView
    private lateinit var maxSpeedText: TextView
    private lateinit var avgSpeedText: TextView
    private lateinit var gpsSignalText: TextView

    // sensors
    private lateinit var sensorManager: SensorManager
    //private var magnetometerReading = FloatArray(3)

    // location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var previousLocation: Location? = null

    // Handler
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>

    // gnss service for gps strength counting
    private val gnssCallback = object : GnssStatus.Callback() {
        @SuppressLint("SetTextI18n")
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            super.onSatelliteStatusChanged(status)

            var usableSatelliteCount = 0

            for (i in (0 until status.satelliteCount)) {
                if (status.usedInFix(i)) {
                    usableSatelliteCount++
                }
            }

            val gpsSignal = when (usableSatelliteCount) {
                0 -> "No Signal"
                in 1..6 -> "Weak"
                in 7..15 -> "Moderate"
                else -> "Strong"
            }
            gpsSignalText.text = "GPS: $gpsSignal ($usableSatelliteCount)"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        speedometer = findViewById(R.id.speedometer)
        tripText = findViewById(R.id.tripText)
        maxSpeedText = findViewById(R.id.maxSpeedText)
        avgSpeedText = findViewById(R.id.avgSpeedText)
        gpsSignalText = findViewById(R.id.gpsSignalText)

        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        stopButton.visibility = View.GONE

        startButton.setOnClickListener {
            startSession()
            stopButton.visibility = View.VISIBLE
            startButton.visibility = View.GONE
        }

        stopButton.setOnClickListener {
            stopSession()
            startButton.visibility = View.VISIBLE
            stopButton.visibility = View.GONE
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magnetometer ->
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.registerGnssStatusCallback(gnssCallback, handler)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        loadFromSharedPreferences()

        settingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                recreate()
            }
        }

        val overflowMenu: ImageView = findViewById(R.id.overflowMenu)
        overflowMenu.setOnClickListener {
            showOverflow(it)
        }
    }

    private fun startSession() {

        initLocation()
        checkLocationPermissions()

        isRecording = true
        sessionStartTime = System.currentTimeMillis()

        totalDistance = 0.0
        maxSpeed = 0.0f
        totalTime = 0.0
    }

    private fun stopSession() {
        if (!isRecording) return

        isRecording = false
        sessionStopTime = System.currentTimeMillis()

        val durationMillis = sessionStopTime - sessionStartTime
        val durationString = formatDuration(durationMillis)
        val startTimeFormatted = formatTime(sessionStartTime)
        val stopTimeFormatted = formatTime(sessionStopTime)

        val avgSpeed = if (totalTime > 0) totalDistance / totalTime else 0.0

        val sessionData = SessionData(
            startTime = startTimeFormatted,
            stopTime = stopTimeFormatted,
            trip = totalDistance,
            maxSpeed = maxSpeed,
            avgSpeed = avgSpeed,
            totalTime = durationString
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.sessionDataDao().insertSession(sessionData)
        }

        totalDistance = 0.0
        maxSpeed = 0.0f
        totalTime = 0.0
        recreate()
    }

    private fun initLocation() {
            locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000
            )
                .setMinUpdateIntervalMillis(1000)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    val location = locationResult.lastLocation
                    if (location != null) {
                        updateSpeedAndTrip(location)
                    }
                }
            }
            requestLocationUpdates()
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }


    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            requestLocationUpdates()
        }
    }

    private fun updateSpeedAndTrip(location: Location) {
        val speedInMetersPerSecond = location.speed
        val speedInKmH = speedInMetersPerSecond * 3.6f

        speedometer.currentSpeed = speedInKmH

        if (speedInKmH > maxSpeed) {
            maxSpeed = speedInKmH
        }

        if (previousLocation != null) {
            val distanceTraveled = previousLocation!!.distanceTo(location) / 1000 // Convert meters to km
            totalDistance += distanceTraveled
        }

        totalTime += 2.0 / 3600.0

        maxSpeedText.text = String.format(Locale.getDefault(), "Max: %.0f km/h", maxSpeed)

        val avgSpeed = if (totalTime > 0) totalDistance / totalTime else 0
        avgSpeedText.text = String.format(Locale.getDefault(), "Avg: %.0f km/h", avgSpeed)

        previousLocation = location

        tripText.text = String.format(Locale.getDefault(), getString(R.string.trip_text) + ": %.2f km", totalDistance)
    }


    override fun onResume() {
        super.onResume()

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magnetometer ->
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.registerGnssStatusCallback(gnssCallback, handler)
        }
    }

    override fun onPause() {
        super.onPause()

        sensorManager.unregisterListener(this)
        locationManager.unregisterGnssStatusCallback(gnssCallback)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        /*when (event?.sensor?.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetometerReading = event.values.clone()
            }
        }*/
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun showOverflow(anchor: View) {
        val popup = PopupMenu(this@MainActivity, anchor)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.main_overflow_menu, popup.menu)

        try {
            val field = PopupMenu::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            val menuPopupHelper = field.get(popup)
            val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
            val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
            setForceIcons.invoke(menuPopupHelper, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_go_history -> {
                    val intent = Intent(this@MainActivity, HistoryActivity::class.java)
                    intent.putParcelableArrayListExtra("sessionHistory", ArrayList(sessionHistory))
                    startActivity(intent)
                    true
                }
                R.id.action_go_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    settingsLauncher.launch(intent)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // Great Utils
    private fun formatTime(timeMillis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timeMillis))
    }


    private fun formatDuration(durationMillis: Long): String {
        val seconds = durationMillis / 1000 % 60
        val minutes = durationMillis / (1000 * 60) % 60
        val hours = durationMillis / (1000 * 60 * 60)
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun loadFromSharedPreferences() {
        val sharedPref = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val maxSpeedSave = sharedPref.getFloat("maxSpeed", 40f)
        val speedometerView: SpeedometerView = findViewById(R.id.speedometer)

        speedometerView.setMaxSpeed(maxSpeedSave.toString())
    }
}
