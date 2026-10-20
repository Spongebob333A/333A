package com.jian.pillreminder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jian.pillreminder.data.SNOOZE_OPTIONS
import com.jian.pillreminder.data.UserRole

/** 单手机第一版只保留与提醒、短信预警和使用说明直接相关的设置。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    snoozeMinutes: Int,
    ongoingNotification: Boolean,
    currentRole: UserRole,
    largeText: Boolean,
    onSnoozeMinutesChange: (Int) -> Unit,
    onOngoingNotificationChange: (Boolean) -> Unit,
    onLargeTextChange: (Boolean) -> Unit,
    onOpenReminderSetup: () -> Unit,
    onOpenFamilyContact: () -> Unit,
    onSwitchRole: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            SettingsSection("使用角色") {
                Text(
                    "当前：${currentRole.label}。角色切换只改变本机的页面入口；跨手机账号绑定和数据同步需要后端支持。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = onSwitchRole, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("重新选择老人端 / 家属端")
                }
            }
            if (currentRole == UserRole.ELDER) {
                SettingsSection("显示") {
                    SwitchRow("老人端大字版", "放大文字并增加可读性；卡片会随文字自动换行", largeText, onLargeTextChange)
                }
            }
            SettingsSection("提醒") {
                SwitchRow("提醒保留到确认", "确认、跳过前不自动结束这次提醒", ongoingNotification, onOngoingNotificationChange)
                Spacer(Modifier.height(16.dp))
                Text("稍后提醒", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row {
                    SNOOZE_OPTIONS.filter { it in listOf(5, 15, 30) }.forEach { minutes ->
                        FilterChip(
                            selected = snoozeMinutes == minutes,
                            onClick = { onSnoozeMinutesChange(minutes) },
                            label = { Text("$minutes 分钟") }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = onOpenReminderSetup, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("检查提醒权限")
                }
            }
            if (currentRole == UserRole.FAMILY) {
                SettingsSection("家属短信预警") {
                    Text(
                        "老人未确认服药或库存低于提醒线时，由当前设备向预设家属号码发送短信。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(onClick = onOpenFamilyContact, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Text("设置家属短信联系人")
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) { Column(Modifier.padding(18.dp), content = content) }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
