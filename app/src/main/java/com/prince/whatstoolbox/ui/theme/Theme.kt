package com.prince.whatstoolbox.ui.theme
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Brand palette
val Lavender = Color(0xFFE9E5FB); val Lilac = Color(0xFFDCD6F7); val Indigo = Color(0xFF3A2A9C)
val Violet = Color(0xFF6D5BE8); val Cream = Color(0xFFFFF0C8); val Amber = Color(0xFFF5A623)

private val Light = lightColorScheme(
    primary = Violet, onPrimary = Color.White, primaryContainer = Lilac, onPrimaryContainer = Indigo,
    secondary = Color(0xFF5C5890), onSecondary = Color.White, secondaryContainer = Cream, onSecondaryContainer = Color(0xFF3B2F00),
    tertiary = Indigo, onTertiary = Color.White, tertiaryContainer = Color(0xFFD7D0FF), onTertiaryContainer = Indigo,
    background = Lavender, onBackground = Color(0xFF1B1735), surface = Color.White, onSurface = Color(0xFF1B1735),
    surfaceVariant = Lilac, onSurfaceVariant = Color(0xFF47426B), outline = Color(0xFF7A74A8), outlineVariant = Color(0xFFC9C3EA))
private val Dark = darkColorScheme(
    primary = Color(0xFFB9AEFF), onPrimary = Color(0xFF2A1A8A), primaryContainer = Color(0xFF3F3294), onPrimaryContainer = Color(0xFFE5DFFF),
    secondary = Color(0xFFCBC2F5), onSecondary = Color(0xFF302A5C), secondaryContainer = Color(0xFF4D4220), onSecondaryContainer = Color(0xFFFFE9A8),
    tertiary = Color(0xFFD0C8FF), onTertiary = Color(0xFF2E2570), tertiaryContainer = Color(0xFF2E2570), onTertiaryContainer = Color(0xFFE5DFFF),
    background = Color(0xFF14112B), onBackground = Color(0xFFE8E4FF), surface = Color(0xFF1E1A3D), onSurface = Color(0xFFE8E4FF),
    surfaceVariant = Color(0xFF2C2756), onSurfaceVariant = Color(0xFFC9C3EA), outline = Color(0xFF938FB8), outlineVariant = Color(0xFF47426B))

private val base = Typography()
val AppTypography = Typography(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold))

/** [darkTheme] is already resolved from System/Light/Dark by the caller; [amoled] (pure black) only applies when dark. */
@Composable fun ToolboxTheme(darkTheme: Boolean, amoled: Boolean, dynamic: Boolean = false, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    var cs = when { dynamic && Build.VERSION.SDK_INT >= 31 -> if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        darkTheme -> Dark; else -> Light }
    if (darkTheme && amoled) cs = cs.copy(background = Color.Black, surface = Color(0xFF0B0B0B))
    MaterialTheme(colorScheme = cs, typography = AppTypography, shapes = Shapes(medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(28.dp)), content = content)
}
