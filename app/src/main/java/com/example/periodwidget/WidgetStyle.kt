package com.example.periodwidget

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

/**
 * Single source of truth for "what icon and colour does a given day get".
 *
 * These are resource IDs, not resolved values: RemoteViews is built on the
 * worker thread where the caller decides when to resolve them. Whoever calls
 * [forStatus] must run the colour through Context.getColor before handing it to
 * RemoteViews.setTextColor, which takes an ARGB int, not a resource id.
 */
object WidgetStyle {

    data class Look(@DrawableRes val icon: Int, @ColorRes val colorRes: Int)

    fun forStatus(status: DayStatus.DayStatusType): Look = when (status) {
        DayStatus.DayStatusType.PERIOD -> Look(R.drawable.ic_period, R.color.period_red)
        DayStatus.DayStatusType.OVULATION -> Look(R.drawable.ic_ovulation, R.color.ovulation_orange)
        DayStatus.DayStatusType.FERTILE -> Look(R.drawable.ic_fertile, R.color.fertile_pink)
        DayStatus.DayStatusType.NEXT_PERIOD -> Look(R.drawable.ic_next_period, R.color.next_blue)
        DayStatus.DayStatusType.TODAY -> Look(R.drawable.ic_calendar, R.color.today_green)
        DayStatus.DayStatusType.PAST, DayStatus.DayStatusType.FUTURE ->
            Look(R.drawable.ic_calendar, R.color.text_secondary)
    }
}
