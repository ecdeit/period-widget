package com.example.periodwidget

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

/**
 * Icon and colour for the widget's status row, keyed by the same [DayKind] the
 * calendar uses so the widget and the app agree.
 *
 * These are resource ids, not resolved values: RemoteViews is built on a worker
 * thread and the caller resolves colours with Context.getColor. Passing a colour
 * resource straight to setTextColor would be read as an ARGB int and render the
 * wrong colour.
 */
object WidgetStyle {

    data class Look(@DrawableRes val icon: Int, @ColorRes val colorRes: Int)

    fun forKind(kind: DayKind): Look = when (kind) {
        DayKind.PERIOD -> Look(R.drawable.ic_period, R.color.cal_period)
        DayKind.PREDICTED_PERIOD -> Look(R.drawable.ic_next_period, R.color.cal_predicted_on)
        DayKind.OVULATION -> Look(R.drawable.ic_ovulation, R.color.cal_ovulation_on)
        DayKind.FERTILE -> Look(R.drawable.ic_fertile, R.color.cal_fertile)
        DayKind.ORDINARY -> Look(R.drawable.ic_calendar, R.color.text_secondary)
    }
}
