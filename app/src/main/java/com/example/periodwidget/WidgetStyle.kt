package com.example.periodwidget

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import java.time.LocalDate

/** Single source of truth for "what colour/icon does today get". */
object WidgetStyle {

    data class Look(@DrawableRes val icon: Int, @ColorRes val color: Int)

    fun forToday(stats: CycleStats): Look {
        val periods = emptyList<PeriodEntity>()
        val status = CycleCalculator.getDayStatus(LocalDate.now(), periods, stats)
        return forStatus(status.status)
    }

    fun forStatus(status: DayStatus.DayStatusType): Look = when (status) {
        DayStatus.DayStatusType.PERIOD -> Look(R.drawable.ic_period, R.color.period_red)
        DayStatus.DayStatusType.OVULATION -> Look(R.drawable.ic_ovulation, R.color.ovulation_orange)
        DayStatus.DayStatusType.FERTILE -> Look(R.drawable.ic_fertile, R.color.fertile_pink)
        DayStatus.DayStatusType.NEXT_PERIOD -> Look(R.drawable.ic_next_period, R.color.next_blue)
        DayStatus.DayStatusType.TODAY -> Look(R.drawable.ic_calendar, R.color.today_green)
        else -> Look(R.drawable.ic_calendar, R.color.text_secondary)
    }
}
