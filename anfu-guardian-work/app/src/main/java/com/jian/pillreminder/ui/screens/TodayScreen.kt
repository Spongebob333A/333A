package com.jian.pillreminder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jian.pillreminder.data.DoseStatus
import com.jian.pillreminder.data.Medication
import com.jian.pillreminder.domain.DoseItem
import com.jian.pillreminder.domain.ScheduleEngine
import com.jian.pillreminder.notify.Reminders
import com.jian.pillreminder.ui.MedViewModel
import com.jian.pillreminder.ui.components.CheckCircle
import com.jian.pillreminder.ui.components.DoseTimeline
import com.jian.pillreminder.ui.components.EmptyState
import com.jian.pillreminder.ui.components.GroupLabel
import com.jian.pillreminder.ui.components.MedBadge
import com.jian.pillreminder.ui.components.TimePickerDialog
import com.jian.pillreminder.ui.theme.medColorAt
import com.jian.pillreminder.ui.theme.PillDimens
import com.jian.pillreminder.ui.theme.PillPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    vm: MedViewModel,
    onAddMedication: () -> Unit,
    onOpenMedication: (String) -> Unit,
    permissionBanner: (@Composable () -> Unit)? = null,
    isElderMode: Boolean = true,
    canManageMedication: Boolean = true,
    allowDoseFeedback: Boolean = true
) {
    val doses by vm.dosesForSelectedDate.collectAsState()
    val date by vm.selectedDate.collectAsState()
    val now by vm.now.collectAsState()
    val lowStock by vm.lowStockMedications.collectAsState()

    // 正在挪时间的那一次服药，null = 没在挪
    var rescheduling by remember { mutableStateOf<DoseItem?>(null) }

    LaunchedEffect(Unit) { vm.refreshNow() }


    val taken = doses.count { it.status == DoseStatus.TAKEN }
    val total = doses.size
    val pending = doses.filter { it.status == DoseStatus.PENDING && !it.isOverdue(now) }
    val overdue = doses.filter { it.status == DoseStatus.PENDING && it.isOverdue(now) }
    val done = doses.filter { it.status != DoseStatus.PENDING }
    val nextDose = (overdue + pending).firstOrNull()
    val visibleOverdue = overdue.filterNot { it.key == nextDose?.key }
    val visiblePending = pending.filterNot { it.key == nextDose?.key }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = PillDimens.ScreenHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(PillDimens.SectionGap)
    ) {
        // 备份横幅已删（用户要求），只剩提醒体检横幅
        permissionBanner?.let { banner ->
            item { banner() }
        }

        item {
            DateHeader(
                date = date,
                onPrev = { vm.selectDate(date.minusDays(1)) },
                onNext = { vm.selectDate(date.plusDays(1)) },
                onToday = { vm.selectDate(LocalDate.now()) }
            )
        }

        if (isElderMode) {
            item {
                CompanionStatus(
                    taken = taken,
                    total = total,
                    nextDose = nextDose,
                    now = now
                )
            }
        }

        if (total > 0 && isElderMode && nextDose != null) {
            item {
                NextDoseHero(
                    item = nextDose,
                    now = now,
                    onTaken = { vm.toggleTaken(nextDose) },
                    onSnooze = { vm.snoozeDose(nextDose) },
                    onSkip = { vm.markSkipped(nextDose) }
                )
            }
        }

        if (total > 0) {
            item { ProgressSummary(taken = taken, total = total, doses = doses) }
        }

        if (lowStock.isNotEmpty()) {
            item { LowStockBanner(lowStock) }
        }

        if (total == 0) {
            val noMedsAtAll = vm.activeMedications.value.isEmpty()
            item {
                EmptyState(
                    icon = Icons.Filled.Today,
                    title = if (noMedsAtAll && isElderMode) "还没有用药计划" else if (noMedsAtAll) "还没有添加药品" else "这一天不用吃药",
                    subtitle = when {
                        noMedsAtAll && isElderMode -> "请家属先在家属端设置药品和提醒时间"
                        noMedsAtAll -> "添加药品后，这里会显示今天的服药安排"
                        else -> "按已确认的用药计划，今天没有需要服用的药"
                    }
                )
            }
            if (noMedsAtAll && canManageMedication) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = { vm.addSampleData() }) {
                            Text("先放两条示例看看效果")
                        }
                    }
                }
            }
        }

        if (visibleOverdue.isNotEmpty()) {
            item { GroupLabel("已错过", visibleOverdue.size, MaterialTheme.colorScheme.error) }
            items(visibleOverdue, key = { it.key }) { item ->
                DoseCard(
                    item, now,
                    onToggle = { vm.toggleTaken(item) },
                    onSkip = { vm.markSkipped(item) },
                    onOpen = { onOpenMedication(item.medication.id) },
                    onSnooze = { vm.snoozeDose(item) },
                    onReschedule = { rescheduling = item },
                    onClearReschedule = { vm.clearDoseReschedule(item) },
                    isElderMode = isElderMode,
                    canManageMedication = canManageMedication,
                    allowDoseFeedback = allowDoseFeedback
                )
            }
        }

        if (visiblePending.isNotEmpty()) {
            item { GroupLabel("待服用", visiblePending.size, MaterialTheme.colorScheme.primary) }
            items(visiblePending, key = { it.key }) { item ->
                DoseCard(
                    item, now,
                    onToggle = { vm.toggleTaken(item) },
                    onSkip = { vm.markSkipped(item) },
                    onOpen = { onOpenMedication(item.medication.id) },
                    onSnooze = { vm.snoozeDose(item) },
                    onReschedule = { rescheduling = item },
                    onClearReschedule = { vm.clearDoseReschedule(item) },
                    isElderMode = isElderMode,
                    canManageMedication = canManageMedication,
                    allowDoseFeedback = allowDoseFeedback
                )
            }
        }

        if (done.isNotEmpty()) {
            item { GroupLabel("已完成", done.size, MaterialTheme.colorScheme.onSurfaceVariant) }
            items(done, key = { it.key }) { item ->
                DoseCard(
                    item, now,
                    onToggle = { vm.toggleTaken(item) },
                    onSkip = { vm.markSkipped(item) },
                    onOpen = { onOpenMedication(item.medication.id) },
                    onSnooze = { vm.snoozeDose(item) },
                    onReschedule = { rescheduling = item },
                    onClearReschedule = { vm.clearDoseReschedule(item) },
                    isElderMode = isElderMode,
                    canManageMedication = canManageMedication,
                    allowDoseFeedback = allowDoseFeedback
                )
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    // 临时改这次的时间。挪过的先给撤销选项，免得用户找不到怎么恢复。
    rescheduling?.let { item ->
        val base = item.effectiveTime
        TimePickerDialog(
            initialHour = base.hour,
            initialMinute = base.minute,
            title = "改这一次的时间",
            supportingText = if (item.movedTo != null)
                "原定 ${item.time.format()}，现在是 ${item.movedTo.format()}。"
            else "只改今天这一次。",
            onDismiss = { rescheduling = null },
            onConfirm = { h, m ->
                vm.rescheduleDose(item, com.jian.pillreminder.data.TimeOfDay(h, m))
                rescheduling = null
            }
        )
    }
}

@Composable
private fun CompanionStatus(
    taken: Int,
    total: Int,
    nextDose: DoseItem?,
    now: java.time.LocalDateTime
) {
    val message = when {
        total == 0 -> "今天也慢慢来"
        taken == total -> "今天安排得很好"
        nextDose?.isOverdue(now) == true -> "不用着急，先确认这一顿"
        else -> "我会在需要时提醒你"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.AccessTime,
            contentDescription = null,
            tint = PillPalette.Indigo,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        today.plusDays(1) -> "明天"
        else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.headlineMedium)
            Text(
                date.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (date != today) {
            TextButton(onClick = onToday) { Text("回到今天") }
        }
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "前一天")
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "后一天")
        }
    }
}

/** 老人端的唯一主视觉：打开页面后先看到下一次要做什么。 */
@Composable
private fun NextDoseHero(
    item: DoseItem,
    now: java.time.LocalDateTime,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    val overdue = item.isOverdue(now)
    val med = item.medication
    val palette = medColorAt(med.colorIndex)
    val accent = if (overdue) PillPalette.Coral else PillPalette.Indigo
    val container by animateColorAsState(
        targetValue = if (overdue) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.30f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.78f)
        },
        label = "nextDoseContainer"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = container,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(148.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                AnimatedContent(targetState = overdue, label = "nextDoseLabel") { isLate ->
                    Text(
                        if (isLate) "这次还没有确认" else "下一次服药",
                        style = MaterialTheme.typography.titleMedium,
                        color = accent
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(item.effectiveTime.format(), style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MedBadge(
                        iconIndex = med.iconIndex,
                        container = palette.container(),
                        content = palette.content(),
                        size = 42.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            med.name.ifBlank { "未命名药品" },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "${Reminders.formatDosage(med.dosage)}${med.unit}" +
                                if (med.mealRelation.label != "无要求") " · ${med.mealRelation.label}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = onTaken, modifier = Modifier.weight(1.3f)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("已服用")
                    }
                    TextButton(onClick = onSnooze, modifier = Modifier.weight(1f)) { Text("稍后提醒") }
                    TextButton(onClick = onSkip, modifier = Modifier.weight(0.7f)) { Text("跳过") }
                }
            }
        }
    }
}

@Composable
private fun ProgressSummary(taken: Int, total: Int, doses: List<DoseItem>) {
    val progress = if (total == 0) 0f else taken.toFloat() / total
    val animated by animateFloatAsState(progress, label = "todayProgress")

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (taken == total) "今天的药都吃完了" else "今日服药情况", style = MaterialTheme.typography.titleMedium)
                Text("$taken / $total 次已完成", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Spacer(Modifier.height(14.dp))
        DoseTimeline(doses = doses)
    }
}

@Composable
private fun LowStockBanner(meds: List<Medication>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(PillPalette.Coral)
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            Icons.Filled.Inventory2,
            contentDescription = null,
            tint = PillPalette.Coral,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("有药快用完了", style = MaterialTheme.typography.titleSmall)
            Text(
                meds.joinToString("、") {
                    "${it.name} 剩 ${Reminders.formatDosage(it.stockRemaining ?: 0.0)}${it.unit}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DoseCard(
    item: DoseItem,
    now: java.time.LocalDateTime,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onOpen: () -> Unit,
    onSnooze: () -> Unit,
    onReschedule: () -> Unit,
    onClearReschedule: () -> Unit,
    isElderMode: Boolean,
    canManageMedication: Boolean,
    allowDoseFeedback: Boolean
) {
    val med = item.medication
    val palette = medColorAt(med.colorIndex)
    val isTaken = item.status == DoseStatus.TAKEN
    val isSkipped = item.status == DoseStatus.SKIPPED
    val isOverdue = item.isOverdue(now)

    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = androidx.compose.animation.core.spring())
            .then(
                if (isOverdue) {
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f))
                } else Modifier
            )
            .clickable(enabled = canManageMedication, onClick = onOpen)
    ) {
        Column(
            Modifier
                .padding(horizontal = 4.dp, vertical = PillDimens.RowVerticalPadding)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedBadge(
                    iconIndex = med.iconIndex,
                    container = palette.container(),
                    content = palette.content(),
                    size = 46.dp,
                    modifier = Modifier.alpha(if (isTaken || isSkipped) 0.55f else 1f)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            med.name.ifBlank { "未命名药品" },
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (isSkipped) TextDecoration.LineThrough else null,
                            color = if (isTaken || isSkipped)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                        )
                        // 示例药不会真的提醒，必须标出来，否则用户会以为提醒失灵了
                        if (med.isSample) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ) {
                                Text(
                                    "示例",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    val dose = Reminders.formatDosage(med.dosage) + med.unit
                    val meal = if (med.mealRelation.label == "无要求") "" else " · ${med.mealRelation.label}"
                    // 挪过时间的把原定时刻划掉再写新的，一眼能看出这次是临时调整过的
                    Text(
                        buildString {
                            if (item.movedTo != null) {
                                append(item.movedTo.format())
                                append("（原 ")
                                append(item.time.format())
                                append("）")
                            } else {
                                append(item.time.format())
                            }
                            append(" · ")
                            append(dose)
                            append(meal)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.movedTo != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.clip(CircleShape).clickable(enabled = allowDoseFeedback, onClick = onToggle).padding(4.dp)
                ) {
                    CheckCircle(checked = isTaken)
                }
            }

            if (med.note.isNotBlank() && !isTaken && !isSkipped) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(PillPalette.Sage)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        med.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AnimatedVisibility(visible = item.status == DoseStatus.PENDING && allowDoseFeedback) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isElderMode) {
                        TextButton(onClick = onSnooze) { Text("稍后提醒") }
                        TextButton(onClick = onSkip) { Text("跳过") }
                        FilledTonalButton(onClick = onToggle, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("已服用")
                        }
                    } else {
                        if (item.movedTo != null) {
                            TextButton(onClick = onClearReschedule) { Text("恢复原时间") }
                        }
                        IconButton(onClick = onReschedule) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = "临时改这次的时间",
                                modifier = Modifier.size(20.dp),
                                tint = if (item.movedTo != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onSkip) { Text("跳过") }
                        FilledTonalButton(onClick = onToggle) { Text("已服用") }
                    }
                }
            }

            if (isTaken && item.actedAtMillis != null) {
                Spacer(Modifier.height(4.dp))
                val t = java.time.Instant.ofEpochMilli(item.actedAtMillis)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                Text(
                    "已于 %02d:%02d 服用".format(t.hour, t.minute),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 60.dp)
                )
            }
            if (isSkipped) {
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "已跳过",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 60.dp)
                    )
                    TextButton(onClick = onSkip) { Text("撤销") }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}
