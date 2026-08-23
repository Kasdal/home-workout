package com.example.workoutapp.ui.sensor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.workoutapp.data.remote.EspSensorData
import com.example.workoutapp.data.repository.SensorRepository
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.ui.workout.SensorConnectionQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SensorTestViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    localAppPreferencesRepository: LocalAppPreferencesRepository
) : ViewModel() {

    val configuredIp: StateFlow<String> = localAppPreferencesRepository.settings
        .map { it.sensorIpAddress }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _testResult = MutableStateFlow<Boolean?>(null)
    val testResult: StateFlow<Boolean?> = _testResult.asStateFlow()

    private val _liveData = MutableStateFlow<EspSensorData?>(null)
    val liveData: StateFlow<EspSensorData?> = _liveData.asStateFlow()

    private val _quality = MutableStateFlow(SensorConnectionQuality.LOST)
    val quality: StateFlow<SensorConnectionQuality> = _quality.asStateFlow()

    private var pollJob: Job? = null

    fun runConnectionTest(ipAddress: String) {
        viewModelScope.launch {
            _testResult.value = sensorRepository.testConnection(ipAddress)
        }
    }

    fun startLiveMonitoring(ipAddress: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var consecutiveFailures = 0
            sensorRepository.pollSensorStatus(ipAddress, intervalMs = 300).collect { data ->
                if (data != null) {
                    consecutiveFailures = 0
                } else {
                    consecutiveFailures++
                }
                _liveData.value = data
                _quality.value = when {
                    data != null -> SensorConnectionQuality.GOOD
                    consecutiveFailures < 3 -> SensorConnectionQuality.WEAK
                    else -> SensorConnectionQuality.LOST
                }
            }
        }
    }

    fun resetCounter(ipAddress: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            onDone(sensorRepository.resetCounter(ipAddress))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorTestScreen(
    navController: NavController,
    viewModel: SensorTestViewModel = hiltViewModel()
) {
    val configuredIp by viewModel.configuredIp.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val liveData by viewModel.liveData.collectAsState()
    val quality by viewModel.quality.collectAsState()

    var ipText by remember(configuredIp) { mutableStateOf(configuredIp.ifBlank { "192.168.0.125" }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensor Test") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Connect to the sensor on your network and confirm the readings before you trust it during a session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = ipText,
                onValueChange = { ipText = it },
                label = { Text("Sensor IP address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.runConnectionTest(ipText) }) {
                    Text("Test connection")
                }
                OutlinedButton(onClick = {
                    viewModel.resetCounter(ipText) { }
                }) {
                    Text("Reset counter")
                }
            }

            testResult?.let { ok ->
                Text(
                    text = if (ok) "Sensor reachable." else "Sensor not reachable at this address.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Live readout",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val data = liveData
                    if (data == null) {
                        Text(
                            text = "Waiting for data…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        SensorReadoutRow("Reps", data.reps.toString())
                        SensorReadoutRow("State", data.state)
                        SensorReadoutRow("Distance", "${data.dist}")
                    }
                    Text(
                        text = "Connection: ${quality.name}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = { viewModel.startLiveMonitoring(ipText) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start live monitoring")
            }
        }
    }
}

@Composable
private fun SensorReadoutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
