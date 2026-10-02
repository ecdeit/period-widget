package com.example.periodwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * The widget host callback.
 *
 * This deliberately holds no rendering logic. onUpdate runs on the main thread,
 * so it cannot touch Room; it only schedules [WidgetUpdateWorker], which does
 * the read and the RemoteViews work on a background dispatcher.
 */
class PeriodWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdateWorker.enqueueNow(context)
        schedulePeriodicRefresh(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedulePeriodicRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            WidgetUpdateWorker.enqueueNow(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.periodwidget.action.REFRESH"
        const val EXTRA_OPEN_ADD = "open_add_dialog"
        private const val WORK_NAME = "period_widget_periodic_refresh"
        private const val REFRESH_INTERVAL_MINUTES = 30L

        /** Call after any data change so the widget never shows stale numbers. */
        fun refresh(context: Context) = WidgetUpdateWorker.enqueueNow(context)

        private fun schedulePeriodicRefresh(context: Context) {
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
