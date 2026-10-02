package com.example.periodwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class PeriodWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_ADD_PERIOD = "com.example.periodwidget.ACTION_ADD_PERIOD"
        const val ACTION_OPEN_APP = "com.example.periodwidget.ACTION_OPEN_APP"
        const val WORK_NAME = "widget_update_work"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        // 启动定期更新 WorkManager
        schedulePeriodicUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedulePeriodicUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // 获取数据（同步读取 Room，简单起见用协程）
        val views = buildWidgetViews(context, appWidgetId)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun buildWidgetViews(context: Context, appWidgetId: Int): RemoteViews {
        val packageName = context.packageName
        val views = RemoteViews(packageName, R.layout.widget_layout)

        // 点击打开主 Activity
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_APP
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, appWidgetId, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, openPending)

        // 点击 "+" 添加经期
        val addIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ADD_PERIOD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val addPending = PendingIntent.getActivity(
            context, appWidgetId + 1000, addIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.btn_add_period, addPending)

        // 这里先用占位数据，实际数据由 WidgetUpdateService 异步更新
        views.setTextViewText(R.id.tv_today_status, "加载中...")
        views.setTextViewText(R.id.tv_cycle_info, "")
        views.setTextViewText(R.id.tv_next_period, "")
        views.setViewVisibility(R.id.progress_bar, android.view.View.VISIBLE)

        return views
    }

    private fun schedulePeriodicUpdate(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateService>(15, TimeUnit.MINUTES)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    // 供 Service 调用的静态更新方法
    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, PeriodWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        for (id in ids) {
            updateWidget(context, appWidgetManager, id)
        }
    }
}