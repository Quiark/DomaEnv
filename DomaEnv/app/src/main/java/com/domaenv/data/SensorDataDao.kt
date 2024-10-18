package com.domaenv.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDataDao {
    @Insert
    suspend fun insert(sensorData: SensorData)

    @Query("SELECT * FROM sensor_data ORDER BY timestamp DESC LIMIT 50")
    fun getAll(): Flow<List<SensorData>>
}