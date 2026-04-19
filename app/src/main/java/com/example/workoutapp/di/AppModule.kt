package com.example.workoutapp.di

import android.app.Application
import androidx.room.Room
import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.data.local.WorkoutDatabase
import com.example.workoutapp.data.local.dao.WorkoutDao
import com.example.workoutapp.data.remote.FirestoreRepository
import com.example.workoutapp.data.remote.LegacyMigrationDataSource
import com.example.workoutapp.data.remote.RoomLegacyMigrationDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.workoutapp.data.repository.CloudWorkoutRepository
import com.example.workoutapp.data.repository.ExerciseRepository
import com.example.workoutapp.data.repository.ProfileRepository
import com.example.workoutapp.data.repository.RestDayRepository
import com.example.workoutapp.data.repository.SessionHistoryRepository
import com.example.workoutapp.data.repository.SettingsRepository
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

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE settings ADD COLUMN sensorEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE settings ADD COLUMN sensorIpAddress TEXT NOT NULL DEFAULT '192.168.0.125'")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercises ADD COLUMN exerciseType TEXT NOT NULL DEFAULT 'STANDARD'")
                database.execSQL("ALTER TABLE exercises ADD COLUMN usesSensor INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE exercises ADD COLUMN holdDurationSeconds INTEGER NOT NULL DEFAULT 30")
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE settings ADD COLUMN undoLastSetEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        return Room.databaseBuilder(
            app,
            WorkoutDatabase::class.java,
            "workout_db"
        )
        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
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
    fun provideLegacyMigrationDataSource(
        roomLegacyMigrationDataSource: RoomLegacyMigrationDataSource
    ): LegacyMigrationDataSource {
        return roomLegacyMigrationDataSource
    }

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
}
