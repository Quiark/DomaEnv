package com.domaenv

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.room.Room
import com.domaenv.data.AppDatabase
import com.domaenv.data.SensorData
import com.domaenv.data.SensorDataReceiver
import com.domaenv.data.SensorDataViewModel
import com.domaenv.ui.theme.Co2Color
import com.domaenv.ui.theme.DomaEnvTheme
import com.domaenv.ui.theme.HumidityColor
import com.domaenv.ui.theme.Purple40
import com.domaenv.ui.theme.PurpleGrey80
import com.domaenv.ui.theme.TempColor
import com.domaenv.ui.theme.TvocColor
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            application,
            AppDatabase::class.java, "sensor_data_db"
        ).build()

        val viewModel = SensorDataViewModel(this.application, db)
        setContent {
            DomaEnvTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainContent(modifier = Modifier.padding(innerPadding), viewModel = viewModel)
                }
            }
        }

        val sensorDataReceiver = SensorDataReceiver(this, db)
        sensorDataReceiver.startReceiving()
    }
}

@Composable
fun MainContent(modifier: Modifier = Modifier, viewModel: SensorDataViewModel) {
    val (latest, setLatest) = remember { mutableStateOf<SensorData?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        TimeSeriesPlot(viewModel = viewModel, setLatest = setLatest)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Latest Measurements", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LatestValues(latest)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun LatestValues(latest: SensorData?) {
    Row {
        Column(modifier = Modifier.padding(end = 16.dp)) {
            Text("Temperature:")
            Text("${latest?.temperature} °C", color = TempColor, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.padding(end = 16.dp)) {
            Text("Humidity:")
            Text("${latest?.humidity} %", color = HumidityColor, fontWeight = FontWeight.Bold)
        }
    }
    Row {
        Column(modifier = Modifier.padding(end = 16.dp)) {
            Text("CO2:")
            Text("${latest?.co2} ppm", color = Co2Color, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.padding(end = 16.dp)) {
            Text("TVOC:")
            Text("${latest?.tvoc} ppb", color = TvocColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TimeSeriesPlot(viewModel: SensorDataViewModel, setLatest: (SensorData?) -> Unit) {
    val sensorData by viewModel.sensorData.observeAsState(emptyList())
    val backgroundColor = MaterialTheme.colorScheme.secondaryContainer.toArgb()

    AndroidView(factory = { context ->
        LineChart(context).apply {
            data = generateLineData(sensorData)
            setBackgroundColor(backgroundColor)
            axisRight.isEnabled = true
        }
    }, update = { lineChart ->
        Log.i("TimeSeriesPlot", "Updating chart")
        lineChart.data = generateLineData(sensorData)
        lineChart.invalidate() // Refresh the chart
        setLatest(sensorData.firstOrNull())
    }, modifier = Modifier.fillMaxWidth().height(500.dp))
}

fun generateLineData(sensorDataList: List<SensorData>): LineData {
    val rev = sensorDataList.reversed()
    val entriesTemperature = rev.mapIndexed { index, data -> Entry(index.toFloat(), data.temperature) }
    val entriesHumidity = rev.mapIndexed { index, data -> Entry(index.toFloat(), data.humidity) }
    val entriesCO2 = rev.mapIndexed { index, data -> Entry(index.toFloat(), data.co2) }
    val entriesTVOC = rev.mapIndexed { index, data -> Entry(index.toFloat(), data.tvoc) }

    val dataSetTemperature = LineDataSet(entriesTemperature, "Temperature").apply {
        color = TempColor.toArgb()
        setDrawCircles(false)
        lineWidth = 2f
    }
    val dataSetHumidity = LineDataSet(entriesHumidity, "Humidity").apply {
        color = HumidityColor.toArgb()
        setDrawCircles(false)
        lineWidth = 2f
    }
    val dataSetCO2 = LineDataSet(entriesCO2, "CO2").apply {
        color = Co2Color.toArgb()
        setDrawCircles(false)
        lineWidth = 2f
        axisDependency = YAxis.AxisDependency.RIGHT
    }
    val dataSetTVOC = LineDataSet(entriesTVOC, "TVOC").apply {
        color = TvocColor.toArgb()
        setDrawCircles(false)
        lineWidth = 2f
    }


    return LineData(dataSetTemperature, dataSetHumidity, dataSetCO2, dataSetTVOC)
}