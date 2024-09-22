package com.mystically.speedometer01

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar

class SettingsActivity : BaseActivity() {

    private lateinit var colorDropdown: Spinner
    private lateinit var speedDropdown: Spinner

    private lateinit var saveButton: Button

    private val colorOptions = arrayOf(
        "Blue", "Red", "Yellow", "Green", "Orange", "Purple", "Pink", "Cyan", "Dark Green"
    )
    private val speedOptions = arrayOf(
        "10", "20", "30", "40", "50", "60", "70", "80", "90", "100", "120", "140", "160", "180", "200", "250", "300", "350", "400", "500", "600", "700", "800", "900", "1000"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        colorDropdown = findViewById(R.id.colorDropdown)
        speedDropdown = findViewById(R.id.speedDropdown)

        saveButton = findViewById(R.id.saveButton)

        val toolbar: Toolbar = findViewById(R.id.bluds_toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        supportActionBar?.title = "Settings"

        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colorOptions)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorDropdown.adapter = colorAdapter

        val speedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speedOptions)
        speedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speedDropdown.adapter = speedAdapter

        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val currentTheme = sharedPreferences.getString("selectedTheme", "Blue")
        val currentMaxSpeed = sharedPreferences.getFloat("maxSpeed", 40f)

        colorDropdown.setSelection(colorOptions.indexOf(currentTheme))
        speedDropdown.setSelection(speedOptions.indexOf(currentMaxSpeed.toInt().toString()))

        saveButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Hold on...")
            builder.setMessage("The app will need to restart to apply the new settings. Are you sure? (This will reset your current session)")

            builder.setPositiveButton("Yes") { _: DialogInterface, _: Int ->

                val maxSpeedString = speedDropdown.selectedItem.toString()
                val maxSpeed = if (maxSpeedString.isNotEmpty()) maxSpeedString.toFloat() else currentMaxSpeed
                val selectedTheme = colorDropdown.selectedItem.toString()

                val editor = sharedPreferences.edit()
                editor.putFloat("maxSpeed", maxSpeed)
                editor.putString("selectedTheme", selectedTheme)
                editor.apply()

                finish()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finishAffinity()
            }

            builder.setNegativeButton("No") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }

            builder.create().show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
