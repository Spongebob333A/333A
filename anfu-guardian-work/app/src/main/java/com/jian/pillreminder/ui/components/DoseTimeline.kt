package com.jian.pillreminder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jian.pillreminder.data.DoseStatus
import com.jian.pillreminder.domain.DoseItem
import com.jian.pillreminder.ui.theme.PillPalette

/**
 * A quiet schedule rail shared by the elder and family home screens.
 * It communicates state with a line and a few marks instead of another dashboard card.
 */
@Composable
fun DoseTimeline(
    doses: List<DoseItem>,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val visible = doses.sortedBy { it.effectiveTime.hour * 60 + it.effectiveTime.minute }.take(4)
    if (visible.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            visible.forEachIndexed { index, item ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    TimelineDot(item)
                }
                if (index < visible.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            visible.forEach { item ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        item.effectiveTime.format(),
                        style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        item.statusLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = item.statusColor()
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineDot(item: DoseItem) {
    val color by animateColorAsState(item.statusColor(), label = "doseTimelineColor")
    val filled = item.status == DoseStatus.TAKEN
    Box(
        modifier = Modifier
            .size(18.dp)
            .then(
                if (filled) {
                    Modifier.background(color, CircleShape)
                } else {
                    Modifier.border(1.5.dp, color, CircleShape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (filled) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

private fun DoseItem.statusLabel(): String = when (status) {
    DoseStatus.TAKEN -> "已完成"
    DoseStatus.SKIPPED -> "已跳过"
    DoseStatus.PENDING -> "待服用"
}

@Composable
private fun DoseItem.statusColor(): Color = when (status) {
    DoseStatus.TAKEN -> PillPalette.Sage
    DoseStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
    DoseStatus.PENDING -> PillPalette.Indigo
}
