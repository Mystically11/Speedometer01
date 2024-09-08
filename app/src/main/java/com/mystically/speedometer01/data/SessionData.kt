package com.mystically.speedometer01.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "session_data")
data class SessionData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: String,
    val stopTime: String,
    val trip: Double,
    val maxSpeed: Float,
    val avgSpeed: Double,
    val totalTime: String
) : Parcelable
