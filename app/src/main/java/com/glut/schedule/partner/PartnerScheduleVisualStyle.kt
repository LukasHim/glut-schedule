package com.glut.schedule.partner

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

internal data class PartnerCourseCardStyle(
    val surface: Color,
    val content: Color
)

internal data class PartnerCardColorStop(
    val position: Float,
    val color: Color
)

/**
 * 上下主体各保留 48% 纯色，仅在中线 4% 高度内做轻微羽化。
 * 这样既避免硬切，也不会像整卡渐变那样削弱双方身份色。
 */
internal fun partnerSplitCardStops(
    first: Color,
    second: Color
): List<PartnerCardColorStop> = listOf(
    PartnerCardColorStop(0f, first),
    PartnerCardColorStop(0.48f, first),
    PartnerCardColorStop(0.49f, lerp(first, second, 0.25f)),
    PartnerCardColorStop(0.5f, lerp(first, second, 0.5f)),
    PartnerCardColorStop(0.51f, lerp(first, second, 0.75f)),
    PartnerCardColorStop(0.52f, second),
    PartnerCardColorStop(1f, second)
)

/**
 * 情侣/基友课表页的配色。
 *
 * 页面整体跟随「该页自己的主题种子色」（见 [com.glut.schedule.ui.theme.seedColorScheme]），
 * 因此这里不再写死粉色：所有角色色都从当前 [MaterialTheme.colorScheme] 取，
 * 深浅主题、用户自定义种子色都会自动生效。
 *
 * 唯一保留固定色值的是「身份色」卡片：双方各选一种颜色，卡片必须始终保持可辨认，
 * 不能因为换主题就把两个人的颜色混在一起。
 */
internal object PartnerScheduleVisualStyle {
    val pagePrimaryText: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground
    val pageSecondaryText: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)

    val manageSheetSurface: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
    val manageSectionSurface: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer
    val managePrimaryText: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
    val manageSecondaryText: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val manageAccent: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val inviteSurface: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val inviteContent: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
    val inviteAction: Color
        @Composable get() = MaterialTheme.colorScheme.primary
    val feedbackSurface: Color
        @Composable get() = MaterialTheme.colorScheme.secondaryContainer
    val feedbackContent: Color
        @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
    val detailSurface: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
    val detailContent: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
    val detailAction: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val overlapBadgeSize = 22.dp

    /**
     * 身份色固定为浅色底 + 深色字：卡片是双方身份的标识，
     * 不随主题明暗反转，否则深色主题下八种身份色会全部糊成一片。
     */
    fun courseCard(identityColor: PartnerIdentityColor): PartnerCourseCardStyle =
        when (identityColor) {
            PartnerIdentityColor.PINK ->
                PartnerCourseCardStyle(surface = Color(0xFFFFDDE7), content = Color(0xFF7A2947))
            PartnerIdentityColor.BLUE ->
                PartnerCourseCardStyle(surface = Color(0xFFD5E8FB), content = Color(0xFF174A7A))
            PartnerIdentityColor.PURPLE ->
                PartnerCourseCardStyle(surface = Color(0xFFE9DDFB), content = Color(0xFF4F2A7F))
            PartnerIdentityColor.TEAL ->
                PartnerCourseCardStyle(surface = Color(0xFFD4F1EE), content = Color(0xFF155A55))
            PartnerIdentityColor.GREEN ->
                PartnerCourseCardStyle(surface = Color(0xFFDDF2D8), content = Color(0xFF2E5E2A))
            PartnerIdentityColor.ORANGE ->
                PartnerCourseCardStyle(surface = Color(0xFFFFE4CC), content = Color(0xFF7A3F0E))
            PartnerIdentityColor.RED ->
                PartnerCourseCardStyle(surface = Color(0xFFFFDAD6), content = Color(0xFF8C1D18))
            PartnerIdentityColor.GOLD ->
                PartnerCourseCardStyle(surface = Color(0xFFF7E7B2), content = Color(0xFF5F4B00))
        }
}

/** 颜色 → HSV（H 0..360，S/V 0..1），供主题色选择器复用。 */
internal fun colorToHsv(color: Color): Triple<Float, Float, Float> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return Triple(hsv[0], hsv[1], hsv[2])
}

/** HSV → 不透明颜色。 */
internal fun hsvToColor(hue: Float, saturation: Float, value: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
