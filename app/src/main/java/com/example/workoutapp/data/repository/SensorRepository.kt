package com.example.workoutapp.data.repository

import com.example.workoutapp.data.remote.EspApiService
import com.example.workoutapp.data.remote.EspSensorData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

    suspend fun resetCounter(ipAddress: String): Boolean {
        return try {
            val service = getApiService(ipAddress)
            val response = service.resetCounter()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}
