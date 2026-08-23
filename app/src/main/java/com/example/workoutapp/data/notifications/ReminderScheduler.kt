package com.example.workoutapp.data.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    /** Schedules or cancels the daily inactivity check. */
    fun setEnabled(context: Context, enabled: Boolean) {
        setReminderEnabled(context, enabled)
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(ReminderWork.UNIQUE_WORK_NAME)
            return
        }

        ensureNotificationChannel(context)
        val request = PeriodicWorkRequestBuilder<InactivityReminderWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            ReminderWork.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
