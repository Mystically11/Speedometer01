package com.mystically.speedometer01

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        loadAppTheme()
        super.onCreate(savedInstanceState)
    }

    private fun loadAppTheme() {
        val sharedPref = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val selectedTheme = sharedPref.getString("selectedTheme", "Default")

        when (selectedTheme) {
            "Red" -> setTheme(R.style.DarkTheme_Red)
            "Blue" -> setTheme(R.style.DarkTheme)
            "Yellow" -> setTheme(R.style.DarkTheme_Yellow)
            "Orange" -> setTheme(R.style.DarkTheme_Orange)
            "Purple" -> setTheme(R.style.DarkTheme_Purple)
            "Green" -> setTheme(R.style.DarkTheme_Green)
            "Pink" -> setTheme(R.style.DarkTheme_Pink)
            "Cyan" -> setTheme(R.style.DarkTheme_Cyan)
            "Dark Green" -> setTheme(R.style.DarkTheme_DarkGreen)
            else -> setTheme(R.style.DarkTheme)
        }
    }
}