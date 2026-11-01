package com.glut.schedule.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 二选一按钮（Pill 风格，选中时整块填充强调色）。
 *
 * 从「上课时间」页的雁山/屏风切换抽取而来，供需要**独立按钮**观感的场景复用。
 * 需要「标签 + 控件」一行式布局时用 [SegmentedPillRow] 的分段控件观感，两者视觉不同。
 */
@Composable
fun SegmentedPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    selectedTextColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) accent else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (selected) null else BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = label,
            color = if (selected) selectedTextColor else unselectedTextColor,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 紧凑分段控件：外层一块浅色轨道，选中项是嵌在轨道里的白底块。
 *
 * 与 [SegmentedPill] 的取舍不同——这里两个选项**不铺满整行**，而是按内容宽度排布、
 * 整体靠右，方便放进「标签在左、控件在右」的一行式布局（导入页的「导入模式」，
 * 与它上面的「南宁分校」行同构）。铺满整行时两块等宽的实心色块视觉重量过大，
 * 和旁边的开关行不成比例。
 *
 * 轨道与选中块使用 MD3 的 surfaceContainerHighest / surface，随深浅主题自适应。
 * [selectedIndex] 越界时不选中任何一项。
 */
@Composable
fun SegmentedPillRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    activeBackground: Color = MaterialTheme.colorScheme.surface
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                modifier = Modifier.clickable { onSelect(index) },
                color = if (selected) activeBackground else Color.Transparent,
                shape = RoundedCornerShape(11.dp)
            ) {
                Text(
                    text = label,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
