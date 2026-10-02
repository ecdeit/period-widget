package com.example.periodwidget

import androidx.annotation.ColorRes
import androidx.compose.ui.graphics.Color

/**
 * Calendar cell colours, shared by the app calendar and the widget so the two
 * can never drift apart.
 *
 * Layout mirrors the reference tracker the user supplied: a recorded period is a
 * solid warm fill, the predicted period is pale pink, the fertile window is sky
 * blue, ovulation day is lavender, and today is marked with a gold ring around
 * whatever fill the day already has.
 */
object CalendarPalette {

    data class Cell(val fill: Color, val content: Color)

    /** Resource ids, for RemoteViews which resolves colours via Context. */
    data class CellRes(@ColorRes val fill: Int, @ColorRes val content: Int)

    val todayRing = Color(0xFFD9B36C)

    /** Today is a ring, never a fill, so it is applied on top of the day's kind. */
    fun ringRes(): Int = R.color.cal_today_ring

    fun cellFor(kind: DayKind): Cell = when (kind) {
        DayKind.PERIOD -> Cell(Color(0xFFE2725B), Color.White)
        DayKind.PREDICTED_PERIOD -> Cell(Color(0xFFFBDAD5), Color(0xFFB3555A))
        DayKind.OVULATION -> Cell(Color(0xFFD9C2F0), Color(0xFF6B4E9E))
        DayKind.FERTILE -> Cell(Color(0xFF6FC3E8), Color.White)
        DayKind.ORDINARY -> Cell(Color.Transparent, Color(0xFF212121))
    }

    fun cellResFor(kind: DayKind): CellRes = when (kind) {
        DayKind.PERIOD -> CellRes(R.color.cal_period, R.color.cal_period_on)
        DayKind.PREDICTED_PERIOD -> CellRes(R.color.cal_predicted, R.color.cal_predicted_on)
        DayKind.OVULATION -> CellRes(R.color.cal_ovulation, R.color.cal_ovulation_on)
        DayKind.FERTILE -> CellRes(R.color.cal_fertile, R.color.cal_fertile_on)
        DayKind.ORDINARY -> CellRes(android.R.color.transparent, R.color.text_primary)
    }
}
