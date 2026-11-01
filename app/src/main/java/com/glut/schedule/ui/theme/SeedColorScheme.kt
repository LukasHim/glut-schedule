package com.glut.schedule.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * 从用户选择的种子色生成一套 Material Design 3 配色。
 *
 * 这里不引入 Material Color Utilities：只用到「一个种子色 → 一组色调（tone）」
 * 这一件事，用 CIELAB 的 LCh 表示即可近似复现 MD3 的 tonal palette。
 * 色相与彩度取自种子色，亮度严格按 MD3 的 tone 表取值（浅色 40/90/10，
 * 深色 80/30/90……），因此无论种子是灰还是荧光绿，页面都能保持可读对比度。
 */

/** 伙伴/情侣课表页的默认主题种子色（沿用旧版身份粉）。 */
const val DEFAULT_PARTNER_THEME_SEED_HEX: String = "#D94F78"

/** 把 `#RRGGBB` / `RRGGBB` 解析成不透明颜色；非法输入返回 null。 */
fun parseSeedColor(value: String?): Color? {
    val normalized = value?.trim()?.removePrefix("#") ?: return null
    if (normalized.length != 6) return null
    val rgb = normalized.toLongOrNull(16) ?: return null
    return Color(
        red = ((rgb shr 16) and 0xFF) / 255f,
        green = ((rgb shr 8) and 0xFF) / 255f,
        blue = (rgb and 0xFF) / 255f
    )
}

/** 把颜色格式化成 `#RRGGBB`，用于持久化。 */
fun Color.toSeedHex(): String {
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}

/** 根据种子色生成完整 MD3 配色。 */
fun seedColorScheme(seed: Color, darkTheme: Boolean): ColorScheme {
    val lab = seed.toLab()
    val seedHue = normalizeHue(Math.toDegrees(kotlin.math.atan2(lab.b, lab.a)))
    val seedChroma = kotlin.math.sqrt(lab.a * lab.a + lab.b * lab.b)

    val primaryChroma = seedChroma.coerceIn(28.0, 48.0)
    val secondaryChroma = (seedChroma * 0.34).coerceIn(8.0, 16.0)
    val tertiaryChroma = (seedChroma * 0.5).coerceIn(16.0, 32.0)
    val neutralChroma = (seedChroma * 0.04).coerceIn(0.0, 4.0)
    val neutralVariantChroma = (seedChroma * 0.14).coerceIn(0.0, 8.0)

    val primary = TonalPalette(seedHue, primaryChroma)
    val secondary = TonalPalette(seedHue, secondaryChroma)
    val tertiary = TonalPalette(normalizeHue(seedHue + 60.0), tertiaryChroma)
    val neutral = TonalPalette(seedHue, neutralChroma)
    val neutralVariant = TonalPalette(seedHue, neutralVariantChroma)

    return if (darkTheme) {
        darkColorScheme(
            primary = primary.tone(80),
            onPrimary = primary.tone(20),
            primaryContainer = primary.tone(30),
            onPrimaryContainer = primary.tone(90),
            secondary = secondary.tone(80),
            onSecondary = secondary.tone(20),
            secondaryContainer = secondary.tone(30),
            onSecondaryContainer = secondary.tone(90),
            tertiary = tertiary.tone(80),
            onTertiary = tertiary.tone(20),
            tertiaryContainer = tertiary.tone(30),
            onTertiaryContainer = tertiary.tone(90),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = neutral.tone(6),
            onBackground = neutral.tone(90),
            surface = neutral.tone(6),
            onSurface = neutral.tone(90),
            surfaceVariant = neutralVariant.tone(30),
            onSurfaceVariant = neutralVariant.tone(80),
            surfaceTint = primary.tone(80),
            inverseSurface = neutral.tone(90),
            inverseOnSurface = neutral.tone(20),
            inversePrimary = primary.tone(40),
            outline = neutralVariant.tone(60),
            outlineVariant = neutralVariant.tone(30),
            scrim = Color.Black,
            surfaceBright = neutral.tone(24),
            surfaceDim = neutral.tone(6),
            surfaceContainer = neutral.tone(12),
            surfaceContainerHigh = neutral.tone(17),
            surfaceContainerHighest = neutral.tone(22),
            surfaceContainerLow = neutral.tone(10),
            surfaceContainerLowest = neutral.tone(4)
        )
    } else {
        lightColorScheme(
            primary = primary.tone(40),
            onPrimary = primary.tone(100),
            primaryContainer = primary.tone(90),
            onPrimaryContainer = primary.tone(10),
            secondary = secondary.tone(40),
            onSecondary = secondary.tone(100),
            secondaryContainer = secondary.tone(90),
            onSecondaryContainer = secondary.tone(10),
            tertiary = tertiary.tone(40),
            onTertiary = tertiary.tone(100),
            tertiaryContainer = tertiary.tone(90),
            onTertiaryContainer = tertiary.tone(10),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = neutral.tone(98),
            onBackground = neutral.tone(10),
            surface = neutral.tone(98),
            onSurface = neutral.tone(10),
            surfaceVariant = neutralVariant.tone(90),
            onSurfaceVariant = neutralVariant.tone(30),
            surfaceTint = primary.tone(40),
            inverseSurface = neutral.tone(20),
            inverseOnSurface = neutral.tone(95),
            inversePrimary = primary.tone(80),
            outline = neutralVariant.tone(50),
            outlineVariant = neutralVariant.tone(80),
            scrim = Color.Black,
            surfaceBright = neutral.tone(98),
            surfaceDim = neutral.tone(87),
            surfaceContainer = neutral.tone(94),
            surfaceContainerHigh = neutral.tone(92),
            surfaceContainerHighest = neutral.tone(90),
            surfaceContainerLow = neutral.tone(96),
            surfaceContainerLowest = neutral.tone(100)
        )
    }
}

private class TonalPalette(
    private val hue: Double,
    private val chroma: Double
) {
    fun tone(tone: Int): Color {
        var currentChroma = chroma
        repeat(48) {
            val color = labToColor(
                lightness = tone.toDouble(),
                a = currentChroma * cos(Math.toRadians(hue)),
                b = currentChroma * sin(Math.toRadians(hue))
            )
            if (color != null) return color
            currentChroma *= 0.9
        }
        return labToColor(tone.toDouble(), 0.0, 0.0) ?: Color.Black
    }
}

private data class Lab(val lightness: Double, val a: Double, val b: Double)

private const val D65_X = 0.95047
private const val D65_Y = 1.0
private const val D65_Z = 1.08883

private fun Color.toLab(): Lab {
    val r = linearize(red.toDouble())
    val g = linearize(green.toDouble())
    val b = linearize(blue.toDouble())

    val x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / D65_X
    val y = (0.2126729 * r + 0.7151522 * g + 0.0721750 * b) / D65_Y
    val z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / D65_Z

    val fx = pivotForward(x)
    val fy = pivotForward(y)
    val fz = pivotForward(z)
    return Lab(
        lightness = 116.0 * fy - 16.0,
        a = 500.0 * (fx - fy),
        b = 200.0 * (fy - fz)
    )
}

/** 转换回 sRGB；超出色域时返回 null，由调用方降彩度重试。 */
private fun labToColor(lightness: Double, a: Double, b: Double): Color? {
    val fy = (lightness + 16.0) / 116.0
    val fx = fy + a / 500.0
    val fz = fy - b / 200.0

    val x = D65_X * pivotInverse(fx)
    val y = D65_Y * pivotInverse(fy)
    val z = D65_Z * pivotInverse(fz)

    val r = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z
    val g = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z
    val bl = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z

    fun inGamut(value: Double) = value.isFinite() && value >= -0.0005 && value <= 1.0005
    if (!inGamut(r) || !inGamut(g) || !inGamut(bl)) return null

    return Color(
        red = deliver(r).toFloat().coerceIn(0f, 1f),
        green = deliver(g).toFloat().coerceIn(0f, 1f),
        blue = deliver(bl).toFloat().coerceIn(0f, 1f)
    )
}

private fun linearize(channel: Double): Double =
    if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

private fun deliver(channel: Double): Double =
    if (channel <= 0.0031308) 12.92 * channel else 1.055 * channel.pow(1.0 / 2.4) - 0.055

private fun pivotForward(value: Double): Double {
    val epsilon = 216.0 / 24389.0
    val kappa = 24389.0 / 27.0
    return if (value > epsilon) value.pow(1.0 / 3.0) else (kappa * value + 16.0) / 116.0
}

private fun pivotInverse(value: Double): Double {
    val epsilon = 216.0 / 24389.0
    val kappa = 24389.0 / 27.0
    val cube = value * value * value
    return if (cube > epsilon) cube else (116.0 * value - 16.0) / kappa
}

private fun normalizeHue(hue: Double): Double {
    val normalized = hue % 360.0
    return if (normalized < 0) normalized + 360.0 else normalized
}
