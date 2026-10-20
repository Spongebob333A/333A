package com.jian.pillreminder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.jian.pillreminder.domain.MedicationRiskAnalyzer

@Composable
fun MedicationRiskReviewDialog(
    medicationName: String,
    findings: List<MedicationRiskAnalyzer.Finding>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("用药风险预检") },
        text = {
            Column {
                if (findings.isEmpty()) {
                    Text("「$medicationName」未發現重名或備註關鍵字提示。")
                } else {
                    findings.forEach { finding ->
                        Text(finding.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                        Text(finding.detail, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "这是本机演示预检，尚未接入 AI 或权威药品相互作用数据库，无法判断药品是否可以同时服用。请按说明书或咨询医生、药师核实。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("我已核对，继续保存") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("返回修改") }
        }
    )
}
