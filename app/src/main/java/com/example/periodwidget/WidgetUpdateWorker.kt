package com.example.periodwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
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
 * Both widget sizes are handled here because they share one data read; each
 * component gets the layout that matches its size.
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
            val today = LocalDate.now()
            val kind = CycleCalculator.getDayStatus(today, periods, stats).kind
            val todayStatus = CycleCalculator.describeToday(periods, stats)

            val manager = AppWidgetManager.getInstance(applicationContext)
            for (target in targets(applicationContext)) {
                for (id in manager.getAppWidgetIds(target.component)) {
                    manager.updateAppWidget(
                        id,
                        WidgetRenderer.render(
                            applicationContext, id, target.compact, stats, kind, todayStatus
                        )
                    )
                }
            }
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    private data class Target(val component: ComponentName, val compact: Boolean)

    private fun targets(context: Context): List<Target> = listOf(
        Target(ComponentName(context, PeriodWidgetProvider::class.java), compact = false),
        Target(ComponentName(context, PeriodWidgetSmallProvider::class.java), compact = true)
    )

    companion object {
        private const val ONE_SHOT_NAME = "period_widget_update_now"

        /** Replace any pending one-shot so rapid saves do not queue up workers. */
        fun enqueueNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_SHOT_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            )
        }

        /** True while at least one widget of either size is on the home screen. */
        fun anyWidgetPlaced(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            val components = listOf(
                ComponentName(context, PeriodWidgetProvider::class.java),
                ComponentName(context, PeriodWidgetSmallProvider::class.java)
            )
            return components.any { manager.getAppWidgetIds(it).isNotEmpty() }
        }
    }
}
