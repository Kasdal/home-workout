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
    @Provides
    @Singleton
    fun provideWorkoutDatabase(app: Application): WorkoutDatabase {
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE settings ADD COLUMN restTimerDuration INTEGER NOT NULL DEFAULT 30")
                database.execSQL("ALTER TABLE settings ADD COLUMN exerciseSwitchDuration INTEGER NOT NULL DEFAULT 90")
            }
        }

        return Room.databaseBuilder(
            app,
            WorkoutDatabase::class.java,
            "workout_db"
        )
        .addMigrations(MIGRATION_3_4)
        .fallbackToDestructiveMigration()
        .build()
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

