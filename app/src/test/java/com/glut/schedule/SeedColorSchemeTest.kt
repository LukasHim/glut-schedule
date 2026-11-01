package com.glut.schedule

import androidx.compose.ui.graphics.Color
import com.glut.schedule.ui.theme.DEFAULT_PARTNER_THEME_SEED_HEX
import com.glut.schedule.ui.theme.parseSeedColor
import com.glut.schedule.ui.theme.seedColorScheme
import com.glut.schedule.ui.theme.toSeedHex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 情侣/基友页的主题色由用户自选，因此不能只验证默认粉：任意种子色生成的正文
 * 与主色都必须保持可读，否则用户选到浅黄或荧光绿时页面会看不清。
 */
class SeedColorSchemeTest {

    private val seeds = listOf(
        DEFAULT_PARTNER_THEME_SEED_HEX,
        "#D94F78",
        "#1E88E5",
        "#FFD400",
        "#00E676",
        "#8E24AA",
        "#000000",
        "#FFFFFF"
    ).mapNotNull(::parseSeedColor)

    @Test
    fun `种子色生成的浅色与深色配色都保持正文对比度`() {
        seeds.forEach { seed ->
            listOf(false, true).forEach { dark ->
                val scheme = seedColorScheme(seed, dark)
                assertReadable(scheme.onBackground, scheme.background)
                assertReadable(scheme.onSurface, scheme.surface)
                assertReadable(scheme.onSurface, scheme.surfaceContainerLow)
                assertReadable(scheme.onPrimary, scheme.primary)
                assertReadable(scheme.onPrimaryContainer, scheme.primaryContainer)
            }
        }
    }

    @Test
    fun `同一页面可以拥有与主主题不同的配色`() {
        val seed = parseSeedColor("#1E88E5")!!
        val light = seedColorScheme(seed, darkTheme = false)
        val dark = seedColorScheme(seed, darkTheme = true)

        assertNotEquals(light.primary, dark.primary)
        assertNotEquals(light.background, dark.background)
        assertNotEquals(
            seedColorScheme(parseSeedColor(DEFAULT_PARTNER_THEME_SEED_HEX)!!, false).primary,
            light.primary
        )
    }

    @Test
    fun `颜色与十六进制字符串可以互相还原`() {
        val color = Color(0xFF1E88E5)
        assertEquals("#1E88E5", color.toSeedHex())
        assertEquals(color, parseSeedColor(color.toSeedHex()))
        assertEquals(color, parseSeedColor("#1e88e5"))
        assertNull(parseSeedColor(""))
        assertNull(parseSeedColor("#12345"))
    }

    private fun assertReadable(foreground: Color, background: Color) {
        assertTrue(
            "普通文本对比度应不低于 4.5:1，实际为 ${contrastRatio(foreground, background)}",
            contrastRatio(foreground, background) >= 4.5
        )
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)
    }
}
