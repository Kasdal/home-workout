package com.example.workoutapp.data.remote

import retrofit2.Response
import retrofit2.http.GET

interface EspApiService {

    @GET("/status")
    suspend fun getStatus(): Response<EspSensorData>

    @GET("/reset")
    suspend fun resetCounter(): Response<String>
}
