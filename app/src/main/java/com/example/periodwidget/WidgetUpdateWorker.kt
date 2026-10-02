package com.example.periodwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Reads the database off the main thread and re-renders every placed widget.
 *
 * The provider itself cannot do this: onUpdate runs on the main thread, so a
 * database read there would block the UI. WorkManager is the supported place.
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as PeriodWidgetApplication
            val periods = app.store.all()
            val stats = CycleCalculator.computeStats(periods)
            val status = CycleCalculator.getDayStatus(LocalDate.now(), periods, stats).status
            val todayStatus = CycleCalculator.describeToday(periods, stats)

            val manager = AppWidgetManager.getInstance(applicationContext)
            val ids = manager.getAppWidgetIds(
                ComponentName(applicationContext, PeriodWidgetProvider::class.java)
            )
            for (id in ids) {
                manager.updateAppWidget(
                    id,
                    buildViews(applicationContext, id, stats, status, todayStatus)
                )
            }
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    private fun buildViews(
        context: Context,
        appWidgetId: Int,
        stats: CycleStats,
        status: DayStatus.DayStatusType,
        todayStatus: String
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        views.setTextViewText(R.id.tv_today_status, todayStatus)
        views.setTextViewText(
            R.id.tv_cycle_info,
            context.getString(R.string.widget_cycle_info, stats.cycleLength, stats.periodLength)
        )
        views.setTextViewText(
            R.id.tv_next_period,
            stats.nextPredicted?.let { context.getString(R.string.widget_next_period, it) }
                ?: context.getString(R.string.widget_no_prediction)
        )

        val look = WidgetStyle.forStatus(status)
        views.setImageViewResource(R.id.iv_status_icon, look.icon)
        // setTextColor takes an ARGB int, so the resource id must be resolved.
        views.setTextColor(R.id.tv_today_status, context.getColor(look.colorRes))

        // Tap anywhere on the widget body to open the app.
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context, appWidgetId))

        // The quick-add chip jumps straight to the add dialog.
        views.setOnClickPendingIntent(
            R.id.btn_add_period,
            openAppIntent(context, appWidgetId + ADD_REQUEST_OFFSET, PeriodWidgetProvider.EXTRA_OPEN_ADD)
        )
        return views
    }

    private fun openAppIntent(
        context: Context,
        requestCode: Int,
        extra: String? = null
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (extra != null) putExtra(extra, true)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val ADD_REQUEST_OFFSET = 1000
        private const val ONE_SHOT_NAME = "period_widget_update_now"

        /** Replace any pending one-shot so rapid saves do not queue up workers. */
        fun enqueueNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_SHOT_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            )
        }
    }
}
