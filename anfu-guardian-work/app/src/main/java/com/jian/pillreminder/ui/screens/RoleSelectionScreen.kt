package com.jian.pillreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jian.pillreminder.data.UserRole

/** 首次打开页：品牌先建立信任，角色入口用列表而不是两张巨大卡片。 */
@Composable
fun RoleSelectionScreen(
    largeText: Boolean,
    onLargeTextChange: (Boolean) -> Unit,
    onChoose: (UserRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null, modifier = Modifier.size(21.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("药时安心", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "把每天的服药安排，交给一个可靠的提醒。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.size(54.dp))
            Text("这部手机由谁使用？", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.size(8.dp))
            Text(
                "先选择使用方式，之后也可以在设置里切换。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(28.dp))

            RoleRow(
                title = "老人端",
                detail = "看清下一次服药，完成后点一下即可。",
                icon = Icons.Filled.Person,
                tint = MaterialTheme.colorScheme.primary,
                onClick = { onChoose(UserRole.ELDER) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RoleRow(
                title = "家属端",
                detail = "管理药品、库存和家人的用药安排。",
                icon = Icons.Filled.FamilyRestroom,
                tint = MaterialTheme.colorScheme.secondary,
                onClick = { onChoose(UserRole.FAMILY) }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.ToggleOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("老人端大字模式", style = MaterialTheme.typography.titleSmall)
                    Text("只放大文字，不改变页面结构", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = largeText, onCheckedChange = onLargeTextChange)
            }
            Text(
                "家属可以先完成设置，再把这部手机交给老人使用。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun RoleRow(
    title: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = tint.copy(alpha = 0.14f),
            contentColor = tint
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(23.dp)) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(2.dp))
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("进入  ›", style = MaterialTheme.typography.labelLarge, color = tint)
    }
}
