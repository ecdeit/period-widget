package com.example.periodwidget

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

data class CycleStats(
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    val lastPeriodStart: String? = null,      // YYYY-MM-DD
    val nextPredicted: String? = null,        // YYYY-MM-DD
    val ovulationDay: String? = null,         // YYYY-MM-DD
    val fertileWindowStart: String? = null,   // YYYY-MM-DD
    val fertileWindowEnd: String? = null      // YYYY-MM-DD
)

data class DayStatus(
    val date: String,                         // YYYY-MM-DD
    val status: DayStatusType,
    val isToday: Boolean = false,
    val dayOfMonth: Int
) {
    enum class DayStatusType {
        PERIOD, OVULATION, FERTILE, NEXT_PERIOD, TODAY, PAST, FUTURE
    }
}

object CycleCalculator {

    private const val DEFAULT_CYCLE = 28
    private const val DEFAULT_PERIOD = 5
    private const val OVULATION_OFFSET = 14
    private const val FERTILE_START_OFFSET = 5
    private const val FERTILE_END_OFFSET = 1

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parseDate(str: String?): LocalDate? {
        return try { str?.let { LocalDate.parse(it, formatter) } } catch (e: Exception) { null }
    }

    fun formatDate(date: LocalDate?): String? = date?.format(formatter)

    fun daysBetween(a: LocalDate?, b: LocalDate?): Int {
        if (a == null || b == null) return 0
        return abs(ChronoUnit.DAYS.between(a, b)).toInt()
    }

    fun computeStats(periods: List<PeriodEntity>): CycleStats {
        if (periods.isEmpty()) {
            return CycleStats()
        }

        val sorted = periods.sortedBy { parseDate(it.startDate)!! }
        val last = sorted.last()
        val lastStart = parseDate(last.startDate)!!

        // 计算平均周期长度
        val cycleLengths = mutableListOf<Int>()
        val periodLengths = mutableListOf<Int>()

        for (i in 1 until sorted.size) {
            val prev = parseDate(sorted[i - 1].startDate)
            val curr = parseDate(sorted[i].startDate)
            if (prev != null && curr != null) {
                val diff = daysBetween(prev, curr)
                if (diff in 21..45) cycleLengths.add(diff)
            }
            val plen = sorted[i - 1].length
            if (plen in 2..10) periodLengths.add(plen)
        }
        // 最后一期的经期长度也加入
        val lastLen = last.length
        if (lastLen in 2..10) periodLengths.add(lastLen)

        val avgCycle = if (cycleLengths.isNotEmpty()) cycleLengths.average().roundToInt() else DEFAULT_CYCLE
        val avgPeriod = if (periodLengths.isNotEmpty()) periodLengths.average().roundToInt() else DEFAULT_PERIOD

        val nextStart = lastStart.plusDays(avgCycle.toLong())
        val ovulation = lastStart.plusDays((avgCycle - OVULATION_OFFSET).toLong())
        val fertileStart = ovulation.minusDays(FERTILE_START_OFFSET.toLong())
        val fertileEnd = ovulation.plusDays(FERTILE_END_OFFSET.toLong())

        return CycleStats(
            cycleLength = avgCycle,
            periodLength = avgPeriod,
            lastPeriodStart = formatDate(lastStart),
            nextPredicted = formatDate(nextStart),
            ovulationDay = formatDate(ovulation),
            fertileWindowStart = formatDate(fertileStart),
            fertileWindowEnd = formatDate(fertileEnd)
        )
    }

    fun getDayStatus(date: LocalDate, periods: List<PeriodEntity>, stats: CycleStats): DayStatus {
        val today = LocalDate.now()
        val isToday = date == today

        val periodStart = parseDate(stats.lastPeriodStart)
        val periodEnd = periodStart?.plusDays((stats.periodLength - 1).toLong())
        val nextStart = parseDate(stats.nextPredicted)
        val ovulation = parseDate(stats.ovulationDay)
        val fertileStart = parseDate(stats.fertileWindowStart)
        val fertileEnd = parseDate(stats.fertileWindowEnd)

        val inPeriod = periodStart != null && periodEnd != null && date in periodStart..periodEnd
        val inFertile = fertileStart != null && fertileEnd != null && date in fertileStart..fertileEnd
        val isOvulation = ovulation != null && date == ovulation
        val isNextPeriod = nextStart != null && date == nextStart

        val status = when {
            inPeriod -> DayStatus.DayStatusType.PERIOD
            isOvulation -> DayStatus.DayStatusType.OVULATION
            inFertile -> DayStatus.DayStatusType.FERTILE
            isNextPeriod -> DayStatus.DayStatusType.NEXT_PERIOD
            isToday -> DayStatus.DayStatusType.TODAY
            date < today -> DayStatus.DayStatusType.PAST
            else -> DayStatus.DayStatusType.FUTURE
        }

        return DayStatus(
            date = formatDate(date)!!,
            status = status,
            isToday = isToday,
            dayOfMonth = date.dayOfMonth
        )
    }

    fun generateCalendarMatrix(year: Int, month: Int, periods: List<PeriodEntity>, stats: CycleStats): List<List<DayStatus?>> {
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.plusMonths(1).minusDays(1)
        val startOffset = firstDay.dayOfWeek.value % 7 // 0=Sunday
        val daysInMonth = lastDay.dayOfMonth

        val matrix = mutableListOf<List<DayStatus?>>()
        var week = mutableListOf<DayStatus?>()

        // 前置空白
        repeat(startOffset) { week.add(null) }

        for (day in 1..daysInMonth) {
            val date = LocalDate.of(year, month, day)
            val dayStatus = getDayStatus(date, periods, stats)
            week.add(dayStatus)
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

    fun describeToday(periods: List<PeriodEntity>, stats: CycleStats): String {
        val today = LocalDate.now()
        val dayStatus = getDayStatus(today, periods, stats)
        return when (dayStatus.status) {
            DayStatus.DayStatusType.PERIOD -> "经期中 🩸 第 ${daysBetween(parseDate(stats.lastPeriodStart), today) + 1} 天"
            DayStatus.DayStatusType.OVULATION -> "排卵日 🥚 易孕期高峰"
            DayStatus.DayStatusType.FERTILE -> {
                val ov = parseDate(stats.ovulationDay)
                val daysToOv = ov?.let { daysBetween(today, it) } ?: 0
                if (daysToOv > 0) "易孕期 🌸 距排卵 $daysToOv 天" else "易孕期 🌸 排卵期结束"
            }
            DayStatus.DayStatusType.NEXT_PERIOD -> "下次经期预计今天 📅"
            DayStatus.DayStatusType.TODAY -> {
                val next = parseDate(stats.nextPredicted)
                val daysLeft = next?.let { daysBetween(today, it) } ?: 0
                if (daysLeft > 0) "距下次经期 $daysLeft 天" else "周期正常"
            }
            else -> "暂无数据，请记录经期"
        }
    }
}