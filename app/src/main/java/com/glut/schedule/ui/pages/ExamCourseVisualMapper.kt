package com.glut.schedule.ui.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ExamCourseVisualCategory {
    Hardware,
    Code,
    Engineering,
    Math,
    Physics,
    LabScience,
    Language,
    PublicAffairs,
    BusinessLaw,
    PsychologyEducation,
    Digital,
    General
}

/**
 * 课程视觉的强调色「语义」，不是具体色值。
 *
 * 具体色值由调用方在 @Composable 里通过 MaterialTheme.colorScheme 解析；
 * 这样本文件保持纯函数，单元测试不依赖 Compose 主题。
 */
enum class ExamCourseAccent {
    Primary,
    Secondary,
    Tertiary
}

data class ExamCourseVisual(
    val icon: ImageVector,
    /** 语义强调色，实际色值在 @Composable 里从 MaterialTheme.colorScheme 解析。 */
    val accent: ExamCourseAccent,
    val category: ExamCourseVisualCategory
)

object ExamCourseVisualMapper {
    fun visualFor(courseName: String): ExamCourseVisual {
        val normalized = courseName.lowercase()
        val category = when {
            normalized.hasAny("大学物理", "物理实验") -> ExamCourseVisualCategory.Physics
            normalized.hasAny("马克思主义基本原理", "毛泽东思想", "中国近现代史纲要", "思想道德与法治") ->
                ExamCourseVisualCategory.PublicAffairs
            normalized.hasAny("高等数学", "线性代数", "概率论", "离散数学") -> ExamCourseVisualCategory.Math
            normalized.hasAny("大学英语", "英语") -> ExamCourseVisualCategory.Language

            normalized.hasAny("微机", "单片机", "嵌入式", "硬件", "芯片", "接口") ->
                ExamCourseVisualCategory.Hardware
            normalized.hasAny("计算机", "程序", "软件", "网络", "数据库", "java", "python", "web") ->
                ExamCourseVisualCategory.Code
            normalized.hasAny("物理", "力学", "光学", "电磁") -> ExamCourseVisualCategory.Physics
            normalized.hasAny("化学", "分析化学", "实验", "材料", "生物") -> ExamCourseVisualCategory.LabScience
            normalized.hasAny("机械", "机电", "工程", "制造", "制图", "电工", "电子", "自动化") ->
                ExamCourseVisualCategory.Engineering
            normalized.hasAny("数学", "逻辑", "概率", "统计") -> ExamCourseVisualCategory.Math
            normalized.hasAny("语言", "翻译", "写作", "阅读") -> ExamCourseVisualCategory.Language
            normalized.hasAny("政治", "思政", "马克思", "习近平", "中国", "社会", "近代史") ->
                ExamCourseVisualCategory.PublicAffairs
            normalized.hasAny("管理", "经济", "会计", "金融", "法律", "法学") ->
                ExamCourseVisualCategory.BusinessLaw
            normalized.hasAny("心理", "教育", "职业", "创新", "创业") ->
                ExamCourseVisualCategory.PsychologyEducation
            normalized.hasAny("数字") -> ExamCourseVisualCategory.Digital
            else -> ExamCourseVisualCategory.General
        }

        return when (category) {
            ExamCourseVisualCategory.Hardware -> ExamCourseVisual(
                icon = Icons.Outlined.Memory,
                accent = ExamCourseAccent.Secondary,
                category = category
            )
            ExamCourseVisualCategory.Code -> ExamCourseVisual(
                icon = Icons.Outlined.Terminal,
                accent = ExamCourseAccent.Primary,
                category = category
            )
            ExamCourseVisualCategory.Engineering -> ExamCourseVisual(
                icon = Icons.Outlined.Engineering,
                accent = ExamCourseAccent.Secondary,
                category = category
            )
            ExamCourseVisualCategory.Math -> ExamCourseVisual(
                icon = Icons.Outlined.Functions,
                accent = ExamCourseAccent.Primary,
                category = category
            )
            ExamCourseVisualCategory.Physics -> ExamCourseVisual(
                icon = Icons.Outlined.Bolt,
                accent = ExamCourseAccent.Primary,
                category = category
            )
            ExamCourseVisualCategory.LabScience -> ExamCourseVisual(
                icon = Icons.Outlined.Science,
                accent = ExamCourseAccent.Secondary,
                category = category
            )
            ExamCourseVisualCategory.Language -> ExamCourseVisual(
                icon = Icons.Outlined.Language,
                accent = ExamCourseAccent.Primary,
                category = category
            )
            ExamCourseVisualCategory.PublicAffairs -> ExamCourseVisual(
                icon = Icons.Outlined.Public,
                accent = ExamCourseAccent.Secondary,
                category = category
            )
            ExamCourseVisualCategory.BusinessLaw -> ExamCourseVisual(
                icon = Icons.Outlined.AccountBalance,
                accent = ExamCourseAccent.Tertiary,
                category = category
            )
            ExamCourseVisualCategory.PsychologyEducation -> ExamCourseVisual(
                icon = Icons.Outlined.Psychology,
                accent = ExamCourseAccent.Secondary,
                category = category
            )
            ExamCourseVisualCategory.Digital -> ExamCourseVisual(
                icon = Icons.Outlined.Calculate,
                accent = ExamCourseAccent.Primary,
                category = category
            )
            ExamCourseVisualCategory.General -> defaultVisual(courseName)
        }
    }

    private fun defaultVisual(courseName: String): ExamCourseVisual {
        val defaults = listOf(
            Icons.AutoMirrored.Outlined.MenuBook to ExamCourseAccent.Primary,
            Icons.Outlined.StarBorder to ExamCourseAccent.Secondary,
            Icons.Outlined.Functions to ExamCourseAccent.Primary,
            Icons.Outlined.Computer to ExamCourseAccent.Tertiary,
            Icons.Outlined.Calculate to ExamCourseAccent.Primary,
            Icons.Outlined.Engineering to ExamCourseAccent.Tertiary
        )
        val (icon, accent) = defaults[kotlin.math.abs(courseName.hashCode()) % defaults.size]
        return ExamCourseVisual(
            icon = icon,
            accent = accent,
            category = ExamCourseVisualCategory.General
        )
    }
}

/** 把语义强调色解析成当前主题下的实际颜色。 */
@Composable
fun ExamCourseAccent.toColor(): Color = when (this) {
    ExamCourseAccent.Primary -> MaterialTheme.colorScheme.primary
    ExamCourseAccent.Secondary -> MaterialTheme.colorScheme.secondary
    ExamCourseAccent.Tertiary -> MaterialTheme.colorScheme.tertiary
}

private fun String.hasAny(vararg keywords: String): Boolean {
    return keywords.any { contains(it, ignoreCase = true) }
}
