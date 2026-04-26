package com.example.workoutapp.data.repository

import com.example.workoutapp.data.remote.EspApiService
import com.example.workoutapp.data.remote.EspSensorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    private var currentIpAddress: String = "192.168.0.125"
    private var apiService: EspApiService? = null

    private fun getApiService(ipAddress: String): EspApiService {
        if (apiService == null || currentIpAddress != ipAddress) {
            currentIpAddress = ipAddress
            val retrofit = Retrofit.Builder()
                .baseUrl("http://$ipAddress/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            apiService = retrofit.create(EspApiService::class.java)
        }
        return apiService!!
    }

    fun pollSensorStatus(ipAddress: String, intervalMs: Long = 200): Flow<EspSensorData?> = flow {
        val service = getApiService(ipAddress)
        while (true) {
            try {
                val response = service.getStatus()
                if (response.isSuccessful) {
                    emit(response.body())
                } else {
                    emit(null)
                }
            } catch (_: Exception) {
                emit(null)
            }
            delay(intervalMs)
        }
    }

    suspend fun testConnection(ipAddress: String): Boolean {
        return try {
            val service = getApiService(ipAddress)
            val response = service.getStatus()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun discoverSensor(): String? = withContext(Dispatchers.IO) {
        val candidateIps = localSubnetCandidates()
        if (candidateIps.isEmpty()) return@withContext null

        val discoveryClient = okHttpClient.newBuilder()
            .connectTimeout(300, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .writeTimeout(300, TimeUnit.MILLISECONDS)
            .build()
        val semaphore = Semaphore(32)

        coroutineScope {
            candidateIps.map { ipAddress ->
                async {
                    semaphore.withPermit {
                        if (probeSensor(discoveryClient, ipAddress)) ipAddress else null
                    }
                }
            }.awaitAll().firstOrNull { it != null }
        }
    }

    suspend fun resetCounter(ipAddress: String): Boolean {
        return try {
            val service = getApiService(ipAddress)
            val response = service.resetCounter()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    private fun localSubnetCandidates(): List<String> {
        val localAddresses = NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .map { it.hostAddress }
            .filterNotNull()
            .filter { it.startsWith("192.168.") || it.startsWith("10.") || it.matches(Regex("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) }
            .toSet()

        return localAddresses.flatMap { localIp ->
            val prefix = localIp.substringBeforeLast('.')
            (1..254)
                .map { "$prefix.$it" }
                .filterNot { it == localIp }
        }.distinct()
    }

    private fun probeSensor(client: OkHttpClient, ipAddress: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("http://$ipAddress/status")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body?.string().orEmpty()
                body.contains("\"reps\"") && body.contains("\"state\"") && body.contains("\"dist\"")
            }
        } catch (_: Exception) {
            false
        }
    }
}
