package com.example.workoutapp.data.repository

import com.example.workoutapp.data.local.entity.RestDay
import kotlinx.coroutines.flow.Flow

interface RestDayRepository {
    fun getRestDays(): Flow<List<RestDay>>
    suspend fun addRestDay(restDay: RestDay)
    suspend fun deleteRestDay(restDayId: Int)
    suspend fun getRestDayByDate(date: Long): RestDay?
}
