package com.domaenv.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket

class SensorDataReceiver(context: Context, db: AppDatabase) {
    val sensorDataDao = db.sensorDataDao()

    fun startReceiving() {
        CoroutineScope(Dispatchers.IO).launch {
            val socket = DatagramSocket(3213)
            val buffer = ByteArray(24)
            Log.i("SensorDataReceiver", "Listening for packets")

            while (true) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                Log.i("SensorDataReceiver", "Received packet")

                try {
                    val buf = java.nio.ByteBuffer.wrap(buffer, 4, 16)
                    buf.order(java.nio.ByteOrder.LITTLE_ENDIAN)

                    val temperature = buf.float
                    val humidity = buf.float
                    val tvoc = buf.getInt()
                    val co2 = buf.getInt()

                    val timestamp = System.currentTimeMillis()

                    val sensorData = SensorData(
                        temperature = temperature,
                        humidity = humidity,
                        co2 = co2.toFloat(),
                        tvoc = tvoc.toFloat(),
                        timestamp = timestamp
                    )

                    sensorDataDao.insert(sensorData)
                } catch (e: Exception) {
                    Log.e("SensorDataReceiver", "Error parsing packet", e)
                }
            }
        }
    }
}