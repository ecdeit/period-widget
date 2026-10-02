package com.example.periodwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Top-level so every caller can reach them without depending on companion
 * inheritance, which Kotlin does not provide through subclass names.
 */
const val WIDGET_ACTION_REFRESH = "com.example.periodwidget.action.REFRESH"
const val WIDGET_EXTRA_OPEN_ADD = "open_add_dialog"

/**
 * Shared behaviour for both widget sizes.
 *
 * This holds no rendering logic on purpose: onUpdate runs on the main thread, so
 * it must not touch the database. It only schedules [WidgetUpdateWorker], which
 * does the read and the RemoteViews work on a background dispatcher.
 */
abstract class BasePeriodWidgetProvider : AppWidgetProvider() {

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
        // The periodic job is shared by both sizes, so only stop it once no
        // widget of either size remains placed.
        if (!WidgetUpdateWorker.anyWidgetPlaced(context)) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WIDGET_ACTION_REFRESH) {
            WidgetUpdateWorker.enqueueNow(context)
        }
    }

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

    private companion object {
        const val WORK_NAME = "period_widget_periodic_refresh"
        const val REFRESH_INTERVAL_MINUTES = 30L
    }
}

/** 2x2 widget: status, cycle summary and the quick-add chip. */
class PeriodWidgetProvider : BasePeriodWidgetProvider()

/** 2x1 widget: status and the next period date. */
class PeriodWidgetSmallProvider : BasePeriodWidgetProvider()
