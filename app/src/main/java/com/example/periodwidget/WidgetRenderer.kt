package com.example.periodwidget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Builds the RemoteViews for one widget instance.
 *
 * The two sizes use different layouts with different view ids, so each size gets
 * its own branch: RemoteViews.setTextViewText on an id the layout does not define
 * throws at bind time. Colours are resolved through Context.getColor because
 * setTextColor takes an ARGB int, not a colour resource id.
 */
object WidgetRenderer {

    private const val ADD_REQUEST_OFFSET = 1000

    fun render(
        context: Context,
        appWidgetId: Int,
        compact: Boolean,
        stats: CycleStats,
        kind: DayKind,
        todayStatus: String
    ): RemoteViews =
        if (compact) renderCompact(context, appWidgetId, stats, kind, todayStatus)
        else renderFull(context, appWidgetId, stats, kind, todayStatus)

    /** 2x1: status plus the next period date, nothing else fits. */
    private fun renderCompact(
        context: Context,
        appWidgetId: Int,
        stats: CycleStats,
        kind: DayKind,
        todayStatus: String
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_layout_small)
        val look = WidgetStyle.forKind(kind)

        views.setTextViewText(R.id.tv_today_status, todayStatus)
        views.setTextViewText(
            R.id.tv_next_period,
            stats.nextPredicted?.takeLast(5) ?: "--"
        )
        views.setImageViewResource(R.id.iv_status_icon, look.icon)
        views.setTextColor(R.id.tv_today_status, context.getColor(look.colorRes))
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context, appWidgetId))
        return views
    }

    /** 2x2: status, cycle summary, next period, quick-add chip. */
    private fun renderFull(
        context: Context,
        appWidgetId: Int,
        stats: CycleStats,
        kind: DayKind,
        todayStatus: String
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        val look = WidgetStyle.forKind(kind)

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
        views.setImageViewResource(R.id.iv_status_icon, look.icon)
        views.setTextColor(R.id.tv_today_status, context.getColor(look.colorRes))

        views.setOnClickPendingIntent(R.id.widget_root, openApp(context, appWidgetId))
        views.setOnClickPendingIntent(
            R.id.btn_add_period,
            openApp(context, appWidgetId + ADD_REQUEST_OFFSET, WIDGET_EXTRA_OPEN_ADD)
        )
        return views
    }

    private fun openApp(context: Context, requestCode: Int, extra: String? = null): PendingIntent {
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
}
