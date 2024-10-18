package com.domaenv.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SensorData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDataDao(): SensorDataDao
}