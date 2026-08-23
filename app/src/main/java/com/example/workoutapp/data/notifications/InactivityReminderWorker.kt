package com.example.workoutapp.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.workoutapp.R
import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.data.repository.RestDayRepository
import com.example.workoutapp.data.repository.SessionHistoryRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object ReminderWork {
    const val CHANNEL_ID = "inactivity_reminders"
    const val CHANNEL_NAME = "Training reminders"
    const val UNIQUE_WORK_NAME = "inactivity_reminder_check"
    const val PREFS_FILE = "reminder_prefs"
    const val PREF_ENABLED = "enabled"
    const val INACTIVITY_THRESHOLD_DAYS = 2L
    const val NOTIFICATION_ID = 4001
}

fun isReminderEnabled(context: Context): Boolean {
    return context.getSharedPreferences(ReminderWork.PREFS_FILE, Context.MODE_PRIVATE)
        .getBoolean(ReminderWork.PREF_ENABLED, false)
}

fun setReminderEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(ReminderWork.PREFS_FILE, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(ReminderWork.PREF_ENABLED, enabled)
        .apply()
}

fun ensureNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        ReminderWork.CHANNEL_ID,
        ReminderWork.CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT
    )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

fun hasPostNotificationsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderWorkerEntryPoint {
    fun authManager(): AuthManager
    fun sessionHistoryRepository(): SessionHistoryRepository
    fun restDayRepository(): RestDayRepository
}

class InactivityReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!isReminderEnabled(applicationContext)) {
                return Result.success()
            }

            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ReminderWorkerEntryPoint::class.java
            )
            val uid = entryPoint.authManager().currentUserId() ?: return Result.success()

            val today = LocalDate.now()
            if (isScheduledRestDay(entryPoint.restDayRepository(), today)) {
                return Result.success()
            }

            val daysSinceLastSession = daysSinceLastSession(entryPoint.sessionHistoryRepository())
            if (daysSinceLastSession == null || daysSinceLastSession < ReminderWork.INACTIVITY_THRESHOLD_DAYS) {
                return Result.success()
            }

            postNotification()
            Result.success()
        } catch (_: Exception) {
            // Reminders must never crash the app; the next periodic run retries.
            Result.retry()
        }
    }

    private suspend fun isScheduledRestDay(
        restDayRepository: RestDayRepository,
        date: LocalDate
    ): Boolean {
        val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return restDayRepository.getRestDayByDate(timestamp) != null
    }

    private suspend fun daysSinceLastSession(repository: SessionHistoryRepository): Long? {
        val sessions = repository.getSessions().first()
        if (sessions.isEmpty()) return null
        val lastMillis = sessions.maxOf { it.date }
        val lastDate = Instant.ofEpochMilli(lastMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return ChronoUnit.DAYS.between(lastDate, LocalDate.now())
    }

    private fun postNotification() {
        if (!hasPostNotificationsPermission(applicationContext)) return

        val notification = NotificationCompat.Builder(applicationContext, ReminderWork.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.reminder_notification_title))
            .setContentText(applicationContext.getString(R.string.reminder_notification_body))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(ReminderWork.NOTIFICATION_ID, notification)
    }
}
