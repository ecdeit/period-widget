package com.example.periodwidget

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

data class CycleStats(
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    val lastPeriodStart: String? = null,      // ISO date
    val nextPredicted: String? = null,        // ISO date
    val ovulationDay: String? = null,         // ISO date
    val fertileWindowStart: String? = null,   // ISO date
    val fertileWindowEnd: String? = null      // ISO date
)

/**
 * What kind of day a calendar cell is.
 *
 * "Today" is deliberately not a kind: a day can be today *and* a period day, so
 * today is a flag on [DayStatus] that draws a ring around whatever fill applies.
 * The same applies to past/future, which carried no visual meaning.
 */
enum class DayKind { PERIOD, PREDICTED_PERIOD, OVULATION, FERTILE, ORDINARY }

data class DayStatus(
    val date: String,
    val kind: DayKind,
    val isToday: Boolean,
    val dayOfMonth: Int
)

object CycleCalculator {

    private const val DEFAULT_CYCLE = 28
    private const val DEFAULT_PERIOD = 5
    private const val OVULATION_OFFSET = 14
    private const val FERTILE_START_OFFSET = 5
    private const val FERTILE_END_OFFSET = 1
    private const val MIN_PLAUSIBLE_CYCLE = 21
    private const val MAX_PLAUSIBLE_CYCLE = 45
    private const val MIN_PLAUSIBLE_PERIOD = 2
    private const val MAX_PLAUSIBLE_PERIOD = 10

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parseDate(str: String?): LocalDate? =
        try {
            str?.let { LocalDate.parse(it, formatter) }
        } catch (e: Exception) {
            null
        }

    fun formatDate(date: LocalDate?): String? = date?.format(formatter)

    fun daysBetween(a: LocalDate?, b: LocalDate?): Int {
        if (a == null || b == null) return 0
        return abs(ChronoUnit.DAYS.between(a, b)).toInt()
    }

    fun computeStats(periods: List<PeriodEntity>): CycleStats {
        if (periods.isEmpty()) return CycleStats()

        val sorted = periods.sortedBy { parseDate(it.startDate) ?: LocalDate.MIN }
        val lastStart = parseDate(sorted.last().startDate) ?: return CycleStats()

        val cycleLengths = mutableListOf<Int>()
        val periodLengths = mutableListOf<Int>()

        for (i in 1 until sorted.size) {
            val prev = parseDate(sorted[i - 1].startDate)
            val curr = parseDate(sorted[i].startDate)
            if (prev != null && curr != null) {
                val diff = daysBetween(prev, curr)
                if (diff in MIN_PLAUSIBLE_CYCLE..MAX_PLAUSIBLE_CYCLE) cycleLengths.add(diff)
            }
            sorted[i - 1].length
                .takeIf { it in MIN_PLAUSIBLE_PERIOD..MAX_PLAUSIBLE_PERIOD }
                ?.let { periodLengths.add(it) }
        }
        sorted.last().length
            .takeIf { it in MIN_PLAUSIBLE_PERIOD..MAX_PLAUSIBLE_PERIOD }
            ?.let { periodLengths.add(it) }

        val avgCycle = if (cycleLengths.isNotEmpty()) {
            cycleLengths.average().roundToInt()
        } else DEFAULT_CYCLE
        val avgPeriod = if (periodLengths.isNotEmpty()) {
            periodLengths.average().roundToInt()
        } else DEFAULT_PERIOD

        val ovulation = lastStart.plusDays((avgCycle - OVULATION_OFFSET).toLong())

        return CycleStats(
            cycleLength = avgCycle,
            periodLength = avgPeriod,
            lastPeriodStart = formatDate(lastStart),
            nextPredicted = formatDate(lastStart.plusDays(avgCycle.toLong())),
            ovulationDay = formatDate(ovulation),
            fertileWindowStart = formatDate(ovulation.minusDays(FERTILE_START_OFFSET.toLong())),
            fertileWindowEnd = formatDate(ovulation.plusDays(FERTILE_END_OFFSET.toLong()))
        )
    }

    /** The recorded period covering [date], if any. */
    private fun coveringPeriod(date: LocalDate, periods: List<PeriodEntity>): PeriodEntity? =
        periods.firstOrNull { p ->
            val start = parseDate(p.startDate) ?: return@firstOrNull false
            val end = start.plusDays((p.length - 1).toLong())
            !date.isBefore(start) && !date.isAfter(end)
        }

    /**
     * Classify one day.
     *
     * Every recorded period is checked, not just the most recent one, so earlier
     * months show their real period days. Precedence follows what the day means
     * clinically: a recorded period outranks everything, then the single
     * ovulation day, then the fertile window, then the predicted period.
     */
    private fun classify(date: LocalDate, periods: List<PeriodEntity>, stats: CycleStats): DayKind {
        if (coveringPeriod(date, periods) != null) return DayKind.PERIOD

        val ovulation = parseDate(stats.ovulationDay)
        if (ovulation != null && date == ovulation) return DayKind.OVULATION

        val fertileStart = parseDate(stats.fertileWindowStart)
        val fertileEnd = parseDate(stats.fertileWindowEnd)
        if (fertileStart != null && fertileEnd != null &&
            !date.isBefore(fertileStart) && !date.isAfter(fertileEnd)
        ) {
            return DayKind.FERTILE
        }

        val nextStart = parseDate(stats.nextPredicted)
        if (nextStart != null) {
            val predictedEnd = nextStart.plusDays((stats.periodLength - 1).toLong())
            if (!date.isBefore(nextStart) && !date.isAfter(predictedEnd)) {
                return DayKind.PREDICTED_PERIOD
            }
        }

        return DayKind.ORDINARY
    }

    fun getDayStatus(date: LocalDate, periods: List<PeriodEntity>, stats: CycleStats): DayStatus =
        DayStatus(
            date = formatDate(date)!!,
            kind = classify(date, periods, stats),
            isToday = date == LocalDate.now(),
            dayOfMonth = date.dayOfMonth
        )

    fun generateCalendarMatrix(
        year: Int,
        month: Int,
        periods: List<PeriodEntity>,
        stats: CycleStats
    ): List<List<DayStatus?>> {
        val firstDay = LocalDate.of(year, month, 1)
        val daysInMonth = firstDay.lengthOfMonth()
        val startOffset = firstDay.dayOfWeek.value % 7  // 0 = Sunday

        val matrix = mutableListOf<List<DayStatus?>>()
        var week = mutableListOf<DayStatus?>()

        repeat(startOffset) { week.add(null) }

        for (day in 1..daysInMonth) {
            week.add(getDayStatus(LocalDate.of(year, month, day), periods, stats))
            if (week.size == 7) {
                matrix.add(week)
                week = mutableListOf()
            }
        }
        if (week.isNotEmpty()) {
            while (week.size < 7) week.add(null)
            matrix.add(week)
        }
        return matrix
    }

    /** One-line description of today, used by both the widget and the app header. */
    fun describeToday(periods: List<PeriodEntity>, stats: CycleStats): String {
        val today = LocalDate.now()
        return when (classify(today, periods, stats)) {
            DayKind.PERIOD -> {
                val start = coveringPeriod(today, periods)?.let { parseDate(it.startDate) }
                val dayIndex = daysBetween(start, today) + 1
                "经期中 · 第 $dayIndex 天"
            }
            DayKind.OVULATION -> "排卵日 · 易孕期高峰"
            DayKind.FERTILE -> {
                val ovulation = parseDate(stats.ovulationDay)
                val daysToOvulation = ovulation?.let { daysBetween(today, it) } ?: 0
                if (daysToOvulation > 0) "易孕期 · 距排卵 $daysToOvulation 天" else "易孕期 · 即将结束"
            }
            DayKind.PREDICTED_PERIOD -> "预计经期"
            DayKind.ORDINARY -> {
                val next = parseDate(stats.nextPredicted)
                when {
                    next == null -> "暂无数据，请记录经期"
                    daysBetween(today, next) > 0 -> "距下次经期 ${daysBetween(today, next)} 天"
                    else -> "周期正常"
                }
            }
        }
    }
}
