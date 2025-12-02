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
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE settings ADD COLUMN restTimerDuration INTEGER NOT NULL DEFAULT 30")
                database.execSQL("ALTER TABLE settings ADD COLUMN exerciseSwitchDuration INTEGER NOT NULL DEFAULT 90")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE session_exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        exerciseName TEXT NOT NULL,
                        weight REAL NOT NULL,
                        sets INTEGER NOT NULL,
                        reps INTEGER NOT NULL,
                        volume REAL NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX index_session_exercises_sessionId ON session_exercises(sessionId)")
            }
        }

        return Room.databaseBuilder(
            app,
            WorkoutDatabase::class.java,
            "workout_db"
        )
        .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
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

