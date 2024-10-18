package com.domaenv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_data")
data class SensorData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val temperature: Float,
    val humidity: Float,
    val co2: Float,
    val tvoc: Float,
    val timestamp: Long
)