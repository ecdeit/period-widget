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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
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
        openAddOnStart.value = intent.getBooleanExtra(PeriodWidgetProvider.EXTRA_OPEN_ADD, false)

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
        if (intent.getBooleanExtra(PeriodWidgetProvider.EXTRA_OPEN_ADD, false)) {
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
        status.contains("经期中") -> Color(0xFFC2185B)
        status.contains("排卵") -> Color(0xFFE65100)
        status.contains("易孕") -> Color(0xFFAD1457)
        status.contains("预计今天") -> Color(0xFF1565C0)
        else -> Color(0xFF2E7D32)
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
            PredictionLine("排卵日", stats.ovulationDay, Color(0xFFE65100)) { daysLabel(stats.ovulationDay) }
            PredictionLine(
                "易孕期",
                stats.fertileWindowEnd?.takeLast(5),
                Color(0xFFAD1457)
            ) { fertileLabel(stats) }
            PredictionLine("下次经期", stats.nextPredicted, Color(0xFF1565C0)) { "预计周期第 1 天" }
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
    return when {
        today.isBefore(start) -> "还有 ${ChronoUnit.DAYS.between(today, start)} 天开始"
        !today.isAfter(end) -> "易孕期中"
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
        }
    }
}

/**
 * One calendar cell. Declared on RowScope because `Modifier.weight` is only
 * available inside a Row/Column scope, and the seven cells share the row width.
 */
@Composable
private fun RowScope.DayCell(day: DayStatus?) {
    if (day == null) {
        Box(Modifier.weight(1f).height(44.dp))
        return
    }
    val bg: Color
    val fg: Color
    when (day.status) {
        DayStatus.DayStatusType.PERIOD -> { bg = Color(0xFFFCE4EC); fg = Color(0xFFC2185B) }
        DayStatus.DayStatusType.OVULATION -> { bg = Color(0xFFFFF3E0); fg = Color(0xFFE65100) }
        DayStatus.DayStatusType.FERTILE -> { bg = Color(0xFFFCECEF); fg = Color(0xFFAD1457) }
        DayStatus.DayStatusType.NEXT_PERIOD -> { bg = Color(0xFFE3F2FD); fg = Color(0xFF1565C0) }
        DayStatus.DayStatusType.TODAY -> { bg = Color(0xFFE8F5E9); fg = Color(0xFF2E7D32) }
        else -> { bg = Color.Transparent; fg = MaterialTheme.colorScheme.onSurface }
    }
    val marked = day.status != DayStatus.DayStatusType.PAST && day.status != DayStatus.DayStatusType.FUTURE

    Box(
        Modifier
            .weight(1f)
            .height(44.dp)
            .padding(2.dp)
            .background(bg, CircleShape)
            .then(if (marked) Modifier.border(1.dp, fg, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${day.dayOfMonth}",
            fontSize = 13.sp,
            fontWeight = if (day.status == DayStatus.DayStatusType.TODAY) FontWeight.Bold else FontWeight.Normal,
            color = fg
        )
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

@Composable
private fun AddPeriodDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var dateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var lengthText by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录经期") },
        text = {
            Column {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("开始日期 YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { lengthText = it.filter(Char::isDigit) },
                    label = { Text("持续天数 (2-10)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val date = parseIso(dateText.trim())
                val length = lengthText.toIntOrNull()
                if (date != null && length != null && length in 2..10) {
                    onConfirm(dateText.trim(), length)
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
