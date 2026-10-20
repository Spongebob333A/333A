package com.jian.pillreminder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/** 轻微回弹只用于状态反馈和选中态，不用于页面装饰。 */
val ExpressiveSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

/**
 * Brand tokens for 药时安心.
 *
 * These are intentionally separate from Material's semantic roles. Screens can use a
 * small amount of brand color for identity while keeping most of the canvas quiet.
 */
object PillPalette {
    val Ink = Color(0xFF292722)
    val Canvas = Color(0xFFFBF8F3)
    val Indigo = Color(0xFF2D4779)
    val Sage = Color(0xFF6F8377)
    val Periwinkle = Color(0xFF7785B8)
    val Coral = Color(0xFFC87963)
    val Mist = Color(0xFFE9E5DD)
    val Paper = Color(0xFFFFFCF8)
}

/** Shared spacing keeps screens airy without creating a new container around every item. */
object PillDimens {
    val ScreenHorizontal = 22.dp
    val SectionGap = 22.dp
    val CompactGap = 10.dp
    val RowVerticalPadding = 14.dp
}

/** 药时安心的品牌色板：暖米白画布、深墨正文、鼠尾草完成色、雾霾蓝导航色和砖红风险色。 */
private val SeedLight = lightColorScheme(
    primary = PillPalette.Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4EAF4),
    onPrimaryContainer = Color(0xFF182A4A),
    secondary = PillPalette.Sage,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE8DF),
    onSecondaryContainer = Color(0xFF17352C),
    tertiary = PillPalette.Coral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3DED4),
    onTertiaryContainer = Color(0xFF4A2116),
    error = Color(0xFFB24E4F),
    onError = Color.White,
    errorContainer = Color(0xFFF6DAD7),
    onErrorContainer = Color(0xFF4A161A),
    background = PillPalette.Canvas,
    onBackground = PillPalette.Ink,
    surface = PillPalette.Canvas,
    onSurface = PillPalette.Ink,
    surfaceVariant = Color(0xFFECE9E3),
    onSurfaceVariant = Color(0xFF68645E),
    surfaceContainerLowest = Color(0xFFFFFCF8),
    surfaceContainerLow = Color(0xFFF5F1EB),
    surfaceContainer = Color(0xFFF0ECE5),
    surfaceContainerHigh = Color(0xFFE9E4DC),
    surfaceContainerHighest = Color(0xFFE1DBD2),
    outline = Color(0xFF8E8981),
    outlineVariant = Color(0xFFD5D0C8)
)

private val SeedDark = darkColorScheme(
    primary = Color(0xFFA9CDB9),
    onPrimary = Color(0xFF18372A),
    primaryContainer = Color(0xFF315443),
    onPrimaryContainer = Color(0xFFDDE8DF),
    secondary = Color(0xFFBEC8EE),
    onSecondary = Color(0xFF293250),
    secondaryContainer = Color(0xFF424B70),
    onSecondaryContainer = Color(0xFFE2E6F5),
    tertiary = Color(0xFFFFB69A),
    onTertiary = Color(0xFF522211),
    tertiaryContainer = Color(0xFF733D2A),
    onTertiaryContainer = Color(0xFFFFDBCC),
    error = Color(0xFFFFB3AE),
    onError = Color(0xFF5E1118),
    errorContainer = Color(0xFF7B2A2D),
    onErrorContainer = Color(0xFFFFDAD7),
    background = Color(0xFF181917),
    onBackground = Color(0xFFE9E5DF),
    surface = Color(0xFF181917),
    onSurface = Color(0xFFE9E5DF),
    surfaceVariant = Color(0xFF47453F),
    onSurfaceVariant = Color(0xFFC9C4BC),
    surfaceContainerLowest = Color(0xFF121311),
    surfaceContainerLow = Color(0xFF211F1C),
    surfaceContainer = Color(0xFF292723),
    surfaceContainerHigh = Color(0xFF34312C),
    surfaceContainerHighest = Color(0xFF3E3B35),
    outline = Color(0xFF98928A),
    outlineVariant = Color(0xFF47453F)
)

data class MedColor(
    val name: String,
    val light: Color,
    val onLight: Color,
    val dark: Color,
    val onDark: Color
) {
    @Composable
    fun container(): Color = if (isSystemInDarkTheme()) dark else light

    @Composable
    fun content(): Color = if (isSystemInDarkTheme()) onDark else onLight
}

/** 药品图标色只做识别辅助，避免整页被一套低饱和色淹没。 */
val MedColors = listOf(
    MedColor("鼠尾草", Color(0xFFDDE8DF), Color(0xFF315443), Color(0xFF315443), Color(0xFFDDE8DF)),
    MedColor("雾霾蓝", Color(0xFFE2E6F5), Color(0xFF303C68), Color(0xFF424B70), Color(0xFFE2E6F5)),
    MedColor("杏仁", Color(0xFFF3DED4), Color(0xFF6E3524), Color(0xFF733D2A), Color(0xFFFFDBCC)),
    MedColor("淡紫", Color(0xFFE8E0F0), Color(0xFF514263), Color(0xFF514263), Color(0xFFE8E0F0)),
    MedColor("柠檬", Color(0xFFF3E8B9), Color(0xFF5B4D13), Color(0xFF5B4D13), Color(0xFFF3E8B9)),
    MedColor("雾灰", Color(0xFFE3E2DE), Color(0xFF4C4A45), Color(0xFF4C4A45), Color(0xFFE3E2DE)),
    MedColor("青绿", Color(0xFFD4E7E0), Color(0xFF245347), Color(0xFF245347), Color(0xFFD4E7E0)),
    MedColor("粉棕", Color(0xFFF0D7D0), Color(0xFF6B3931), Color(0xFF6B3931), Color(0xFFF0D7D0))
)

fun medColorAt(index: Int): MedColor = MedColors[index.mod(MedColors.size)]

@Composable
fun PillReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    largeText: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> SeedDark
        else -> SeedLight
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale * if (largeText) 1.22f else 1f
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PillTypography,
            shapes = PillShapes,
            content = content
        )
    }
}
