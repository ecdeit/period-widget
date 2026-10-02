package com.example.periodwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {

    private val viewModel: PeriodViewModel by viewModels {
        val app = application as PeriodWidgetApplication
        PeriodViewModel.Factory(PeriodRepository(app.store))
    }

    private val openAddOnStart = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openAddOnStart.value = intent.getBooleanExtra(WIDGET_EXTRA_OPEN_ADD, false)

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    PeriodScreen(
                        viewModel = viewModel,
                        openAddOnStart = openAddOnStart.value,
                        onAddConsumed = { openAddOnStart.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(WIDGET_EXTRA_OPEN_ADD, false)) {
            openAddOnStart.value = true
        }
        // The widget may have gone stale while the app was closed.
        WidgetUpdateWorker.enqueueNow(applicationContext)
    }

    override fun onStop() {
        super.onStop()
        WidgetUpdateWorker.enqueueNow(applicationContext)
    }
}

@Composable
private fun PeriodScreen(
    viewModel: PeriodViewModel,
    openAddOnStart: Boolean,
    onAddConsumed: () -> Unit
) {
    val periods by viewModel.periods.observeAsState(emptyList())
    val stats by viewModel.stats.observeAsState(CycleStats())
    val todayStatus by viewModel.todayStatus.observeAsState("正在读取…")
    val context = LocalContext.current

    var showAdd by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }

    // The widget's quick-add button sets the extra on the launch intent, so the
    // dialog has to open once after the first composition. LaunchedEffect keeps
    // the state write out of the composition phase.
    LaunchedEffect(openAddOnStart) {
        if (openAddOnStart) {
            showAdd = true
            onAddConsumed()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TodayCard(todayStatus)
        Spacer(Modifier.height(16.dp))
        StatsRow(stats)
        Spacer(Modifier.height(16.dp))
        PredictionsCard(stats)
        Spacer(Modifier.height(16.dp))
        CalendarCard(today, periods, stats)
        Spacer(Modifier.height(16.dp))
        HistoryCard(periods, onRemove = { viewModel.removePeriod(it) })
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("记录经期", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showAdd) {
        AddPeriodDialog(
            defaultLength = stats.periodLength,
            onDismiss = { showAdd = false },
            onConfirm = { date, length ->
                viewModel.addPeriod(date, length)
                WidgetUpdateWorker.enqueueNow(context.applicationContext)
                showAdd = false
            }
        )
    }
}

@Composable
private fun TodayCard(status: String) {
    val color = when {
        status.contains("经期中") -> Color(0xFFE2725B)
        status.contains("排卵") -> Color(0xFF6B4E9E)
        status.contains("易孕") -> Color(0xFF2E8BB0)
        status.contains("预计经期") -> Color(0xFFB3555A)
        else -> Color(0xFFB08D3F)
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Event, contentDescription = null, tint = color)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("今日状态", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(status, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

@Composable
private fun StatsRow(stats: CycleStats) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatBox("平均周期", "${stats.cycleLength} 天", Modifier.weight(1f))
        StatBox("平均经期", "${stats.periodLength} 天", Modifier.weight(1f))
        StatBox("下次经期", stats.nextPredicted?.takeLast(5) ?: "--", Modifier.weight(1f))
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PredictionsCard(stats: CycleStats) {
    if (stats.nextPredicted == null) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("预测", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            PredictionLine("排卵日", stats.ovulationDay, Color(0xFF6B4E9E)) { daysLabel(stats.ovulationDay) }
            PredictionLine(
                "易孕期",
                stats.fertileWindowStart,
                Color(0xFF2E8BB0)
            ) { fertileLabel(stats) }
            PredictionLine("下次经期", stats.nextPredicted, Color(0xFFB3555A)) { "预计周期第 1 天" }
        }
    }
}

private fun parseIso(text: String?): LocalDate? =
    text?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun daysLabel(date: String?): String {
    val target = parseIso(date) ?: return ""
    val days = ChronoUnit.DAYS.between(LocalDate.now(), target)
    return when {
        days > 0 -> "还有 $days 天"
        days == 0L -> "就是今天"
        else -> "已过 ${-days} 天"
    }
}

private fun fertileLabel(stats: CycleStats): String {
    val start = parseIso(stats.fertileWindowStart) ?: return ""
    val end = parseIso(stats.fertileWindowEnd) ?: return ""
    val today = LocalDate.now()
    // The value column shows the start date, so this must speak about the start,
    // and it names the end date so the window is unambiguous.
    return when {
        today.isBefore(start) ->
            "还有 ${ChronoUnit.DAYS.between(today, start)} 天开始 · 到 ${end.monthValue}月${end.dayOfMonth}日"
        !today.isAfter(end) -> "易孕期中 · 到 ${end.monthValue}月${end.dayOfMonth}日"
        else -> "已结束"
    }
}

@Composable
private fun PredictionLine(label: String, value: String?, color: Color, subtitle: () -> String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Event, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value?.takeLast(5) ?: "--", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun CalendarCard(today: LocalDate, periods: List<PeriodEntity>, stats: CycleStats) {
    var monthOffset by remember { mutableStateOf(0) }
    val shown = remember(today, monthOffset) { today.plusMonths(monthOffset.toLong()) }
    // Recomputed when the data or the displayed month changes. The helper is a
    // pure function, so this needs no hidden state and reacts to observed data.
    val matrix = remember(shown.year, shown.monthValue, periods, stats) {
        CycleCalculator.generateCalendarMatrix(shown.year, shown.monthValue, periods, stats)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { monthOffset-- }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
                }
                Text("${shown.year}年${shown.monthValue}月", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { monthOffset++ }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                    Text(
                        it, Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            matrix.forEach { week ->
                Row(Modifier.fillMaxWidth()) { week.forEach { DayCell(it) } }
            }
            CalendarLegend()
        }
    }
}

@Composable
private fun RowScope.DayCell(day: DayStatus?) {
    if (day == null) {
        Box(Modifier.weight(1f).height(46.dp))
        return
    }
    val cell = CalendarPalette.cellFor(day.kind)
    val marked = day.kind != DayKind.ORDINARY

    Box(
        Modifier
            .weight(1f)
            .height(46.dp)
            .padding(2.dp)
            .then(if (marked) Modifier.background(cell.fill, CircleShape) else Modifier)
            // Today is a ring, not a fill, so it composes with the day's kind.
            .then(
                if (day.isToday) Modifier.border(2.dp, CalendarPalette.todayRing, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${day.dayOfMonth}",
            fontSize = 13.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                marked -> cell.content
                day.isToday -> CalendarPalette.todayRing
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/** Colour key for the calendar, matching the reference tracker's legend. */
@Composable
private fun CalendarLegend() {
    val items = listOf(
        "经期" to DayKind.PERIOD,
        "预测经期" to DayKind.PREDICTED_PERIOD,
        "排卵期" to DayKind.FERTILE,
        "排卵日" to DayKind.OVULATION
    )
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { (label, kind) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(CalendarPalette.cellFor(kind).fill, CircleShape)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(periods: List<PeriodEntity>, onRemove: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("历史记录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (periods.isEmpty()) {
                Text(
                    "还没有记录，点下面的按钮添加第一条",
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                periods.forEach { p ->
                    val end = parseIso(p.startDate)?.plusDays((p.length - 1).toLong())
                        ?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${p.startDate} → ${end ?: "?"}", fontSize = 14.sp)
                            Text(
                                "持续 ${p.length} 天",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onRemove(p.startDate) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFE91E63))
                        }
                    }
                }
            }
        }
    }
}

/**
 * A snapping wheel column.
 *
 * Not built on Material's DatePicker because the request was specifically for a
 * scroll wheel. It uses only stable APIs: a LazyColumn whose vertical content
 * padding is half the leftover height, so the item at the top of the viewport is
 * the centred one. After a drag settles, the list snaps to the nearest item and
 * that item is reported as the selection.
 */
@Composable
private fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 36.dp,
    visibleCount: Int = 5
) {
    val lastIndex = (items.size - 1).coerceAtLeast(0)
    val state = rememberLazyListState()
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    // Follow selection changes that originate outside the wheel, e.g. the day
    // count shrinking when a shorter month is chosen.
    LaunchedEffect(selectedIndex, items.size) {
        val target = selectedIndex.coerceIn(0, lastIndex)
        if (state.firstVisibleItemIndex != target || state.firstVisibleItemScrollOffset != 0) {
            state.scrollToItem(target)
        }
    }

    // Snap to the nearest row once scrolling stops, then publish the selection.
    LaunchedEffect(state, items.size) {
        snapshotFlow {
            Triple(
                state.isScrollInProgress,
                state.firstVisibleItemIndex,
                state.firstVisibleItemScrollOffset
            )
        }.collect { (scrolling, index, offset) ->
            if (scrolling) return@collect
            if (offset != 0) {
                val target = (if (offset > itemHeightPx / 2f) index + 1 else index)
                    .coerceIn(0, lastIndex)
                state.animateScrollToItem(target)
            } else {
                val settled = index.coerceIn(0, lastIndex)
                if (settled != selectedIndex) onSelected(settled)
            }
        }
    }

    Box(
        modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2))
        ) {
            items(items.size) { index ->
                val isCentred = index == selectedIndex
                Box(
                    Modifier.fillMaxWidth().height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        fontSize = if (isCentred) 17.sp else 14.sp,
                        fontWeight = if (isCentred) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCentred) onSurface else outline
                    )
                }
            }
        }
        // Selection band.
        Box(
            Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .border(1.dp, outline, RoundedCornerShape(6.dp))
        )
    }
}

/**
 * Records a period with wheel pickers instead of a typed date.
 *
 * Typing YYYY-MM-DD was both slow and error-prone; the wheels cannot produce an
 * invalid date, the day column follows the chosen month (leap years included),
 * and the length defaults to the user's own average.
 */
@Composable
private fun AddPeriodDialog(
    defaultLength: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    val today = remember { LocalDate.now() }
    val years = remember(today) { ((today.year - 5)..(today.year + 1)).toList() }
    val lengthOptions = remember { (2..10).toList() }

    var yearIndex by remember { mutableStateOf(years.indexOf(today.year).coerceAtLeast(0)) }
    var monthIndex by remember { mutableStateOf(today.monthValue - 1) }
    var dayIndex by remember { mutableStateOf(today.dayOfMonth - 1) }
    var lengthIndex by remember {
        mutableStateOf(lengthOptions.indexOf(defaultLength).takeIf { it >= 0 } ?: 4)
    }

    val year = years[yearIndex]
    val month = monthIndex + 1
    val daysInMonth = remember(year, month) { YearMonth.of(year, month).lengthOfMonth() }
    val day = (dayIndex + 1).coerceIn(1, daysInMonth)
    val length = lengthOptions[lengthIndex.coerceIn(0, lengthOptions.lastIndex)]

    fun jumpTo(date: LocalDate) {
        years.indexOf(date.year).takeIf { it >= 0 }?.let { yearIndex = it }
        monthIndex = date.monthValue - 1
        dayIndex = date.dayOfMonth - 1
    }

    // Keep the day wheel inside the chosen month.
    LaunchedEffect(year, month) {
        if (dayIndex > daysInMonth - 1) dayIndex = daysInMonth - 1
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录经期") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { jumpTo(today) }) { Text("今天") }
                    TextButton(onClick = { jumpTo(today.minusDays(1)) }) { Text("昨天") }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WheelPicker(
                        items = years.map { it.toString() },
                        selectedIndex = yearIndex,
                        onSelected = { yearIndex = it },
                        modifier = Modifier.weight(1.3f)
                    )
                    WheelPicker(
                        items = (1..12).map { it.toString() },
                        selectedIndex = monthIndex,
                        onSelected = { monthIndex = it },
                        modifier = Modifier.weight(1f)
                    )
                    WheelPicker(
                        items = (1..daysInMonth).map { it.toString() },
                        selectedIndex = (dayIndex).coerceIn(0, daysInMonth - 1),
                        onSelected = { dayIndex = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "经期持续",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WheelPicker(
                    items = lengthOptions.map { "$it 天" },
                    selectedIndex = lengthIndex,
                    onSelected = { lengthIndex = it },
                    modifier = Modifier.fillMaxWidth(0.55f),
                    itemHeight = 32.dp,
                    visibleCount = 3
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    // Braces are required: Kotlin treats CJK characters as
                    // identifier characters, so "$year年" would parse as a
                    // variable named "year年".
                    "${year}年${month}月${day}日 · 持续 $length 天",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalDate.of(year, month, day).toString(), length)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
