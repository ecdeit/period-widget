package com.example.periodwidget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.Composable
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: PeriodViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PeriodScreen(viewModel = viewModel, onNavigateToAdd = { handleAddPeriodIntent() })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.action?.let { action ->
            when (action) {
                PeriodWidgetProvider.ACTION_ADD_PERIOD -> showAddDialog = true
                PeriodWidgetProvider.ACTION_OPEN_APP -> { /* 正常打开 */ }
            }
        }
    }

    private var showAddDialog = false

    private fun handleAddPeriodIntent() {
        showAddDialog = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodScreen(
    viewModel: PeriodViewModel,
    onNavigateToAdd: () -> Unit
) {
    val periods by viewModel.periods.observeAsState(emptyList())
    val stats by viewModel.stats.observeAsState(CycleStats())
    val todayStatus by viewModel.todayStatus.observeAsState("加载中...")
    val scrollState = rememberScrollState()
    var showAddDialog by remember { mutableStateOf(false) }
    var addStartDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var addLength by remember { mutableStateOf(5) }

    val today = LocalDate.now()
    val currentYear = today.year
    val currentMonth = today.monthValue
    val calendarMatrix = viewModel.getCalendarMatrix(currentYear, currentMonth)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 今日状态卡片
        TodayStatusCard(status = todayStatus, stats = stats)

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))

        // 周期信息行
        CycleInfoRow(stats = stats)

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))

        // 预测信息
        PredictionsCard(stats = stats, periods = periods)

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))

        // 日历
        CalendarCard(
            year = currentYear,
            month = currentMonth,
            matrix = calendarMatrix,
            stats = stats
        )

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))

        // 历史记录
        HistoryCard(periods = periods, onRemove = { startDate ->
            viewModel.removePeriod(startDate)
        })

        // 底部添加按钮
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(24.dp))
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("+ 记录经期", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    // 添加经期对话框
    if (showAddDialog) {
        AddPeriodDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { startDate, length ->
                viewModel.addPeriod(startDate, length)
                showAddDialog = false
            },
            initialDate = addStartDate,
            initialLength = addLength,
            onDateChange = { addStartDate = it },
            onLengthChange = { addLength = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayStatusCard(status: String, stats: CycleStats) {
    val today = LocalDate.now()
    val (icon, color) = when {
        status.contains("经期") -> Pair(Icons.Default.Favorite, Color(0xFFE91E63))
        status.contains("排卵") -> Pair(Icons.Default.Egg, Color(0xFFFF9800))
        status.contains("易孕") -> Pair(Icons.Default.Spa, Color(0xFFE91E63))
        status.contains("下次经期预计今天") -> Pair(Icons.Default.Event, Color(0xFF2196F3))
        else -> Pair(Icons.Default.Schedule, Color(0xFF4CAF50))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = androidx.compose.ui.graphics.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.15f))
                        .clip(androidx.compose.ui.graphics.CircleShape)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                        tint = color
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text("今日状态", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(status, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                }
            }
        }
    }
}

@Composable
fun CycleInfoRow(stats: CycleStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoBox("平均周期", "${stats.cycleLength} 天", Icons.Default.CalendarMonth)
        InfoBox("平均经期", "${stats.periodLength} 天", Icons.Default.WaterDrop)
        InfoBox(
            stats.nextPredicted?.let { "下次经期" } ?: "无预测",
            stats.nextPredicted?.substring(5) ?: "--/--",
            Icons.Default.Event
        )
    }
}

@Composable
fun InfoBox(label: String, value: String, icon: ImageVector) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.height(4.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionsCard(stats: CycleStats, periods: List<PeriodEntity>) {
    if (stats.nextPredicted == null) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("预测信息", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.height(12.dp))

            PredictionRow(
                icon = Icons.Default.Egg,
                color = Color(0xFFFF9800),
                title = "排卵日",
                date = stats.ovulationDay?.substring(5) ?: "--/--",
                subtitle = stats.ovulationDay?.let {
                    val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(it))
                    if (days > 0) "距排卵 $days 天" else if (days == 0) "今天是排卵日" else "已过 ${-days} 天"
                } ?: ""
            )

            PredictionRow(
                icon = Icons.Default.Spa,
                color = Color(0xFFE91E63),
                title = "易孕期",
                date = "${stats.fertileWindowStart?.substring(5) ?: "--/--"} - ${stats.fertileWindowEnd?.substring(5) ?: "--/--"}",
                subtitle = {
                    val start = stats.fertileWindowStart?.let { LocalDate.parse(it) }
                    val end = stats.fertileWindowEnd?.let { LocalDate.parse(it) }
                    val today = LocalDate.now()
                    when {
                        start != null && end != null && today in start..end -> "易孕期中"
                        start != null && today < start -> "距易孕期 ${ChronoUnit.DAYS.between(today, start)} 天"
                        else -> "已结束"
                    }
                }()
            )

            PredictionRow(
                icon = Icons.Default.Event,
                color = Color(0xFF2196F3),
                title = "下次经期",
                date = stats.nextPredicted?.substring(5) ?: "--/--",
                subtitle = "预计周期第 1 天"
            )
        }
    }
}

@Composable
fun PredictionRow(icon: ImageVector, color: Color, title: String, date: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f))
                .clip(androidx.compose.ui.graphics.CircleShape)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp).align(Alignment.Center))
        }
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(date, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun CalendarCard(year: Int, month: Int, matrix: List<List<DayStatus?>>, stats: CycleStats) {
    val monthName = LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("yyyy年M月"))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* TODO: 上月 */ }) { Icon(Icons.Default.ChevronLeft, contentDescription = "上月") }
                Text(monthName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { /* TODO: 下月 */ }) { Icon(Icons.Default.ChevronRight, contentDescription = "下月") }
            }

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.height(8.dp))

            // 星期标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                    Text(day, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp, TextAlign.Center))
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.height(4.dp))

            // 日历网格
            Column {
                matrix.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { dayStatus ->
                            DayCell(dayStatus = dayStatus, stats = stats)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(dayStatus: DayStatus?, stats: CycleStats) {
    if (dayStatus == null) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.size(40.dp))
        return
    }

    val color = when (dayStatus.status) {
        DayStatus.DayStatusType.PERIOD -> Color(0xFFE91E63)
        DayStatus.DayStatusType.OVULATION -> Color(0xFFFF9800)
        DayStatus.DayStatusType.FERTILE -> Color(0xFFE91E63)
        DayStatus.DayStatusType.NEXT_PERIOD -> Color(0xFF2196F3)
        DayStatus.DayStatusType.TODAY -> Color(0xFF4CAF50)
        else -> Color(0xFF212121)
    }
    val bgColor = when (dayStatus.status) {
        DayStatus.DayStatusType.PERIOD -> Color(0xFFFCE4EC)
        DayStatus.DayStatusType.OVULATION -> Color(0xFFFFF3E0)
        DayStatus.DayStatusType.FERTILE -> Color(0xFFFCE4EC)
        DayStatus.DayStatusType.NEXT_PERIOD -> Color(0xFFE3F2FD)
        DayStatus.DayStatusType.TODAY -> Color(0xFFE8F5E9)
        else -> Color.White
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(40.dp)
            .background(bgColor)
            .clip(androidx.compose.ui.graphics.CircleShape)
            .border(if (dayStatus.isToday) 2.dp else (if (dayStatus.status != DayStatus.DayStatusType.PAST && dayStatus.status != DayStatus.DayStatusType.FUTURE) 1.dp else 0.dp), color)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("${dayStatus.dayOfMonth}", fontSize = 14.sp, fontWeight = if (dayStatus.isToday) FontWeight.Bold else FontWeight.Medium, color = color)
            if (dayStatus.status == DayStatus.DayStatusType.OVULATION) {
                Icon(Icons.Default.Egg, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
            } else if (dayStatus.status == DayStatus.DayStatusType.FERTILE) {
                Icon(Icons.Default.Spa, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
            }
        }
    )
}

@Composable
fun HistoryCard(periods: List<PeriodEntity>, onRemove: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("历史记录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.height(12.dp))

            if (periods.isEmpty()) {
                Text("暂无记录", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp))
            } else {
                Column {
                    periods.forEach { p ->
                        val endDate = LocalDate.parse(p.startDate).plusDays((p.length - 1).toLong())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp).padding(end = 12.dp))
                                Column {
                                    Text("${p.startDate} - ${endDate.toString()}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("持续 ${p.length} 天", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPeriodDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit,
    initialDate: String,
    initialLength: Int,
    onDateChange: (String) -> Unit,
    onLengthChange: (Int) -> Unit
) {
    var date by remember { mutableStateOf(initialDate) }
    var length by remember { mutableStateOf(initialLength) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加经期记录") },
        text = {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                TextField(
                    value = date,
                    onValueChange = { date = it; onDateChange(it) },
                    label = { Text("开始日期 (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                TextField(
                    value = length.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { length = it; onLengthChange(it) } },
                    label = { Text("经期天数 (2-10)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(date, length) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// 图标对象（简化版，实际项目用 material-icons-extended）
object Icons {
    object Default {
        val Favorite = androidx.compose.material.icons.Icons.Default.Favorite
        val Egg = androidx.compose.material.icons.Icons.Default.Egg
        val Spa = androidx.compose.material.icons.Icons.Default.Spa
        val Event = androidx.compose.material.icons.Icons.Default.Event
        val Schedule = androidx.compose.material.icons.Icons.Default.Schedule
        val CalendarMonth = androidx.compose.material.icons.Icons.Default.CalendarMonth
        val WaterDrop = androidx.compose.material.icons.Icons.Default.WaterDrop
        val ChevronLeft = androidx.compose.material.icons.Icons.Default.ChevronLeft
        val ChevronRight = androidx.compose.material.icons.Icons.Default.ChevronRight
        val Delete = androidx.compose.material.icons.Icons.Default.Delete
    }
}