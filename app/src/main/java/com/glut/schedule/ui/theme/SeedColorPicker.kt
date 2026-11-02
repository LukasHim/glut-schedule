package com.glut.schedule.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 种子色选择器：用色相 / 饱和度 / 亮度三个滑块挑选颜色。
 *
 * 设置页的应用主题色与情侣课表页主题色共用这一个弹窗，避免两处各写一份
 * HSV 往返逻辑后出现「同一颜色在两个页面显示不同」的问题。
 */
@Composable
fun SeedColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
    title: String = "选择主题色"
) {
    val initialHsv = remember(initial) { colorToHsv(initial) }
    var hue by remember { mutableStateOf(initialHsv.first) }
    var saturation by remember { mutableStateOf(initialHsv.second) }
    var value by remember { mutableStateOf(initialHsv.third) }
    val preview = hsvToColor(hue, saturation, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(preview),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        preview.toSeedHex(),
                        color = if (value > 0.5f) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text("色相", fontSize = 12.sp)
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f
                )
                Text("饱和度", fontSize = 12.sp)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f
                )
                Text("亮度", fontSize = 12.sp)
                Slider(
                    value = value,
                    onValueChange = { value = it.coerceAtLeast(0.05f) },
                    valueRange = 0f..1f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(preview) }) { Text("使用此颜色") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 颜色 → HSV（H 0..360，S/V 0..1）。 */
private fun colorToHsv(color: Color): Triple<Float, Float, Float> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return Triple(hsv[0], hsv[1], hsv[2])
}

/** HSV → 不透明颜色。 */
private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
