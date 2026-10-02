package com.example.periodwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** WorkManager keeps its schedule across reboots, but the widget's first paint does not. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                WidgetUpdateWorker.enqueueNow(context)

                val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                    REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES
                ).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }
    }

    private companion object {
        const val WORK_NAME = "period_widget_periodic_refresh"
        const val REFRESH_INTERVAL_MINUTES = 30L
    }
}
