package com.example.workoutapp.di

import android.app.Application
import androidx.room.Room
import com.example.workoutapp.data.local.WorkoutDatabase
import com.example.workoutapp.data.local.dao.WorkoutDao
import com.example.workoutapp.data.repository.WorkoutRepository
import com.example.workoutapp.data.repository.WorkoutRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWorkoutDatabase(app: Application): WorkoutDatabase {
        return Room.databaseBuilder(
            app,
            WorkoutDatabase::class.java,
            "workout_db"
        ).fallbackToDestructiveMigration().build()
    }


    @Provides
    @Singleton
    fun provideWorkoutDao(db: WorkoutDatabase): WorkoutDao {
        return db.workoutDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutRepository(dao: WorkoutDao): WorkoutRepository {
        return WorkoutRepositoryImpl(dao)
    }
}

