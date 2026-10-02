package com.example.periodwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetUpdateService(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 获取数据库实例
            val app = applicationContext as PeriodWidgetApplication
            val dao = app.database.periodDao()

            // 同步读取数据
            val periods = dao.getAllPeriods().first()
            val stats = CycleCalculator.computeStats(periods)
            val todayStatus = CycleCalculator.getTodayStatus(periods, stats)

            // 构建所有小组件的 RemoteViews
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val componentName = ComponentName(applicationContext, PeriodWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in ids) {
                val views = buildWidgetRemoteViews(applicationContext, appWidgetId, periods, stats, todayStatus)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun buildWidgetRemoteViews(
        context: Context,
        appWidgetId: Int,
        periods: List<PeriodEntity>,
        stats: CycleStats,
        todayStatus: String
    ): RemoteViews {
        val packageName = context.packageName
        val views = RemoteViews(packageName, R.layout.widget_layout)

        // 根布局点击打开 App
        val openIntent = android.content.Intent(context, MainActivity::class.java).apply {
            action = PeriodWidgetProvider.ACTION_OPEN_APP
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = android.app.PendingIntent.getActivity(
            context, appWidgetId, openIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, openPending)

        // "+" 按钮点击添加经期
        val addIntent = android.content.Intent(context, MainActivity::class.java).apply {
            action = PeriodWidgetProvider.ACTION_ADD_PERIOD
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val addPending = android.app.PendingIntent.getActivity(
            context, appWidgetId + 1000, addIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.btn_add_period, addPending)

        // 设置文本
        views.setTextViewText(R.id.tv_today_status, todayStatus)
        views.setViewVisibility(R.id.progress_bar, android.view.View.GONE)

        // 周期信息
        val cycleInfo = "周期 ${stats.cycleLength}天  ·  经期 ${stats.periodLength}天"
        views.setTextViewText(R.id.tv_cycle_info, cycleInfo)

        // 下次经期
        val nextText = stats.nextPredicted?.let { "下次: $it" } ?: "暂无预测"
        views.setTextViewText(R.id.tv_next_period, nextText)

        // 设置状态图标和颜色
        val today = java.time.LocalDate.now()
        val dayStatus = CycleCalculator.getDayStatus(today, periods, stats)
        when (dayStatus.status) {
            DayStatus.DayStatusType.PERIOD -> {
                views.setImageViewResource(R.id.iv_status_icon, R.drawable.ic_period)
                views.setInt(R.id.tv_today_status, "setTextColor", context.getColor(R.color.period_red))
            }
            DayStatus.DayStatusType.OVULATION -> {
                views.setImageViewResource(R.id.iv_status_icon, R.drawable.ic_ovulation)
                views.setInt(R.id.tv_today_status, "setTextColor", context.getColor(R.color.ovulation_orange))
            }
            DayStatus.DayStatusType.FERTILE -> {
                views.setImageViewResource(R.id.iv_status_icon, R.drawable.ic_fertile)
                views.setInt(R.id.tv_today_status, "setTextColor", context.getColor(R.color.fertile_pink))
            }
            DayStatus.DayStatusType.NEXT_PERIOD -> {
                views.setImageViewResource(R.id.iv_status_icon, R.drawable.ic_next_period)
                views.setInt(R.id.tv_today_status, "setTextColor", context.getColor(R.color.next_blue))
            }
            else -> {
                views.setImageViewResource(R.id.iv_status_icon, R.drawable.ic_calendar)
                views.setInt(R.id.tv_today_status, "setTextColor", context.getColor(R.color.text_primary))
            }
        }

        return views
    }
}