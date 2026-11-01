package com.glut.schedule.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.glut.schedule.data.settings.ThemeMode

/*
 * Material Design 3 黄色默认配色。
 *
 * 色板以黄色为种子色（primary 为深琥珀 / 亮黄反转），所有角色都来自同一套
 * MD3 tonal palette：浅色主题 primary 取 40 色调、深色主题取 80 色调，容器色
 * 分别取 90 / 30 色调。页面不得再自带蓝色 #3F7DF6 之类的品牌色，一律使用
 * MaterialTheme.colorScheme 的角色色。
 */

private val YellowLightScheme = lightColorScheme(
    primary = Color(0xFF6C5E00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF8E287),
    onPrimaryContainer = Color(0xFF211B00),
    secondary = Color(0xFF665E40),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFE1BD),
    onSecondaryContainer = Color(0xFF211B05),
    tertiary = Color(0xFF4C6547),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCDEBC4),
    onTertiaryContainer = Color(0xFF09200A),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF9EE),
    onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFFFF9EE),
    onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFE9E2CF),
    onSurfaceVariant = Color(0xFF4A4739),
    surfaceTint = Color(0xFF6C5E00),
    inverseSurface = Color(0xFF343024),
    inverseOnSurface = Color(0xFFF8F0DE),
    inversePrimary = Color(0xFFDBC66D),
    outline = Color(0xFF7B7768),
    outlineVariant = Color(0xFFCCC6B5),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFF9EE),
    surfaceDim = Color(0xFFE0D9C7),
    surfaceContainer = Color(0xFFF5EEDC),
    surfaceContainerHigh = Color(0xFFEFE8D6),
    surfaceContainerHighest = Color(0xFFE9E2D0),
    surfaceContainerLow = Color(0xFFFBF4E2),
    surfaceContainerLowest = Color(0xFFFFFFFF)
)

private val YellowDarkScheme = darkColorScheme(
    primary = Color(0xFFDBC66D),
    onPrimary = Color(0xFF3A3000),
    primaryContainer = Color(0xFF533F00),
    onPrimaryContainer = Color(0xFFF8E287),
    secondary = Color(0xFFD2C5A4),
    onSecondary = Color(0xFF363018),
    secondaryContainer = Color(0xFF4E472C),
    onSecondaryContainer = Color(0xFFEFE1BD),
    tertiary = Color(0xFFB2CFA9),
    onTertiary = Color(0xFF1E361C),
    tertiaryContainer = Color(0xFF344D31),
    onTertiaryContainer = Color(0xFFCDEBC4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF15130C),
    onBackground = Color(0xFFE9E2D0),
    surface = Color(0xFF15130C),
    onSurface = Color(0xFFE9E2D0),
    surfaceVariant = Color(0xFF4A4739),
    onSurfaceVariant = Color(0xFFCCC6B5),
    surfaceTint = Color(0xFFDBC66D),
    inverseSurface = Color(0xFFE9E2D0),
    inverseOnSurface = Color(0xFF343024),
    inversePrimary = Color(0xFF6C5E00),
    outline = Color(0xFF959181),
    outlineVariant = Color(0xFF4A4739),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3C392C),
    surfaceDim = Color(0xFF15130C),
    surfaceContainer = Color(0xFF221F17),
    surfaceContainerHigh = Color(0xFF2D2A21),
    surfaceContainerHighest = Color(0xFF38352B),
    surfaceContainerLow = Color(0xFF1E1B13),
    surfaceContainerLowest = Color(0xFF100E08)
)

/** 支持动态取色的最低系统版本（Android 12）。 */
const val DYNAMIC_COLOR_MIN_SDK: Int = Build.VERSION_CODES.S

/** 动态颜色只在 Android 12 及以上、且用户主动开启时生效。 */
fun isDynamicColorAvailable(): Boolean = Build.VERSION.SDK_INT >= DYNAMIC_COLOR_MIN_SDK

/** 解析当前应使用的明暗模式；主题切换与状态栏配色共用这一份判定。 */
@Composable
fun resolveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun GlutScheduleTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColorEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = resolveDarkTheme(themeMode)
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColorEnabled && isDynamicColorAvailable() ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> YellowDarkScheme
        else -> YellowLightScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
