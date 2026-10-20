package com.jian.pillreminder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.jian.pillreminder.data.FamilyContact

/** 同一部老人手机中的联系人设置，不创建第二个账号或家属端。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyContactScreen(
    initial: FamilyContact,
    smsPermissionGranted: Boolean,
    onRequestSmsPermission: () -> Unit,
    onSave: (FamilyContact) -> Unit,
    onBack: () -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var phone by remember(initial) { mutableStateOf(initial.phone) }
    var missedDose by remember(initial) { mutableStateOf(initial.missedDoseSmsEnabled) }
    var lowStock by remember(initial) { mutableStateOf(initial.lowStockSmsEnabled) }
    var includeMedicationName by remember(initial) { mutableStateOf(initial.includeMedicationName) }
    val normalizedPhone = phone.filter { it.isDigit() || it == '+' }
    val needsPhone = missedDose || lowStock
    val phoneValid = normalizedPhone.filter { it.isDigit() }.length in 7..15

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("家属短信联系人") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "联系人无需安装 App。系统只会在老人手机上未确认服药或药品库存不足时发送短信。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                label = { Text("联系人称呼") },
                placeholder = { Text("例如：女儿") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' }.take(16) },
                label = { Text("手机号码") },
                isError = needsPhone && !phoneValid,
                supportingText = {
                    if (needsPhone && !phoneValid) Text("请填写有效手机号码")
                    else Text("仅保存于本机，用于发送预警短信")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ContactSwitch(
                        title = "未确认服药短信",
                        subtitle = "到点后 30 分钟仍未确认时，请联系人联系老人核实",
                        checked = missedDose,
                        onChange = { missedDose = it }
                    )
                    ContactSwitch(
                        title = "低库存补药短信",
                        subtitle = "库存低于每种药设定的提醒线时发送一次",
                        checked = lowStock,
                        onChange = { lowStock = it }
                    )
                    ContactSwitch(
                        title = "短信显示药名",
                        subtitle = "默认不显示药名，减少健康信息在短信中的暴露",
                        checked = includeMedicationName,
                        onChange = { includeMedicationName = it }
                    )
                }
            }
            if (!smsPermissionGranted) {
                Button(onClick = onRequestSmsPermission, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Filled.Sms, null)
                    Spacer(Modifier.width(6.dp))
                    Text("允许发送短信")
                }
                Text(
                    "未授权时，应用仍会记录本机提醒，但不会实际发送短信。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    onSave(FamilyContact(name.trim(), normalizedPhone, missedDose, lowStock, includeMedicationName))
                },
                enabled = !needsPhone || phoneValid,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("保存联系人设置") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContactSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
