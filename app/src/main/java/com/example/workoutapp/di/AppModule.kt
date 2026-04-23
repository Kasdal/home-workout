package com.example.workoutapp.di

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.data.remote.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.workoutapp.data.repository.CloudWorkoutRepository
import com.example.workoutapp.data.repository.ExerciseRepository
import com.example.workoutapp.data.repository.ProfileRepository
import com.example.workoutapp.data.repository.RestDayRepository
import com.example.workoutapp.data.repository.SessionHistoryRepository
import com.example.workoutapp.data.repository.SettingsRepository
import com.example.workoutapp.data.settings.SyncedWorkoutSettingsStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideCloudWorkoutRepository(
        authManager: AuthManager,
        firestoreRepository: FirestoreRepository
    ): CloudWorkoutRepository {
        return CloudWorkoutRepository(
            authManager = authManager,
            firestoreRepository = firestoreRepository
        )
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        cloudWorkoutRepository: CloudWorkoutRepository
    ): ProfileRepository = cloudWorkoutRepository

    @Provides
    @Singleton
    fun provideSessionHistoryRepository(
        cloudWorkoutRepository: CloudWorkoutRepository
    ): SessionHistoryRepository = cloudWorkoutRepository

    @Provides
    @Singleton
    fun provideRestDayRepository(
        cloudWorkoutRepository: CloudWorkoutRepository
    ): RestDayRepository = cloudWorkoutRepository

    @Provides
    @Singleton
    fun provideExerciseRepository(
        cloudWorkoutRepository: CloudWorkoutRepository
    ): ExerciseRepository = cloudWorkoutRepository

    @Provides
    @Singleton
    fun provideSettingsRepository(
        cloudWorkoutRepository: CloudWorkoutRepository
    ): SettingsRepository = cloudWorkoutRepository

    @Provides
    @Singleton
    fun provideSyncedWorkoutSettingsStore(
        cloudWorkoutRepository: CloudWorkoutRepository
    ): SyncedWorkoutSettingsStore = cloudWorkoutRepository
}
