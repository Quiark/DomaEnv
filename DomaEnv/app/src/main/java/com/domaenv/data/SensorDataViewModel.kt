package com.domaenv.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.launch

class SensorDataViewModel(application: Application, db: AppDatabase) : AndroidViewModel(application) {

    private val sensorDataDao = db.sensorDataDao()

    val sensorData: LiveData<List<SensorData>> = sensorDataDao.getAll().asLiveData()
    /*
    private val _sensorData = MutableLiveData<List<SensorData>>()

    init {
        viewModelScope.launch {
            _sensorData.value = sensorDataDao.getAll()
        }
    }
     */
}