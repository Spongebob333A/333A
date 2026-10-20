package com.jian.pillreminder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jian.pillreminder.data.DoseStatus
import com.jian.pillreminder.domain.ScheduleEngine
import com.jian.pillreminder.notify.Reminders
import com.jian.pillreminder.ui.MedViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 仅保留最近 7 天的可核对记录，替代圆环、月历和多周期统计。 */
@Composable
fun RecentRecordsScreen(vm: MedViewModel) {
    val data by vm.data.collectAsState()
    val now = LocalDateTime.now()
    val days = remember(data, now.toLocalDate()) {
        (0L..6L).map { now.toLocalDate().minusDays(it) }.map { date ->
            date to ScheduleEngine.dosesForDate(data.medications, data.logs, date, data.doseOverrides)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("最近 7 天记录", style = MaterialTheme.typography.titleLarge)
            Text(
                "查看已确认、跳过和到期未确认的服药任务。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        days.forEach { (date, doses) ->
            item(key = date.toString()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(date.format(DateTimeFormatter.ofPattern("M 月 d 日")), style = MaterialTheme.typography.titleSmall)
                    if (doses.isEmpty()) {
                        Text("当天没有服药计划", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        doses.sortedBy { it.time }.forEach { dose ->
                            val label = when (dose.status) {
                                DoseStatus.TAKEN -> "已确认"
                                DoseStatus.SKIPPED -> "已跳过"
                                DoseStatus.PENDING -> if (dose.isOverdue(now)) "到期未确认" else "待服用"
                            }
                            val actual = dose.actedAtMillis?.let { millis ->
                                java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
                                    .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(dose.medication.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${dose.time.format()} · ${Reminders.formatDosage(dose.medication.dosage)}${dose.medication.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(label, style = MaterialTheme.typography.labelLarge)
                                        if (actual != null) Text("确认 $actual", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
