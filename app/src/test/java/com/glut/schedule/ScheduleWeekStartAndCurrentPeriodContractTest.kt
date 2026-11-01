package com.glut.schedule

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 一周从周日开始 + 课表页每秒刷新时间状态 + 左侧节次列高亮「正在上的那一节」。
 *
 * 这三条行为散在三个文件里（模型、网格、页面），各自都能单独被改回旧口径：
 * 周次锚点退回周一会让第 X 周整体错开一天，页面漏掉秒级时钟会让高亮停在进入页面
 * 的那一刻，网格漏掉 `week.contains(today)` 会让翻到别的周时高亮继续乱跳。
 * 这里用源码契约把三者钉在一起。
 */
class ScheduleWeekStartAndCurrentPeriodContractTest {
    private fun source(path: String): String {
        val module = File("src/main/java/com/glut/schedule/$path")
        return (if (module.exists()) module else
            File("app/src/main/java/com/glut/schedule/$path")).readText()
    }

    private val models by lazy { source("data/model/ScheduleModels.kt") }
    private val grid by lazy { source("ui/components/ScheduleGrid.kt") }
    private val screen by lazy { source("ui/pages/ScheduleScreen.kt") }

    @Test
    fun weeksAreAnchoredOnSundayEverywhere() {
        assertTrue(models.contains("previousOrSame(DayOfWeek.SUNDAY)"))
        assertTrue(models.contains("val sunday: LocalDate"))
        // 显示周末时周日排在最左，然后才是周一~周六。
        assertTrue(models.contains("listOf(7, 1, 2, 3, 4, 5, 6)"))
        assertTrue(grid.contains("""dayNames = listOf("日", "一", "二", "三", "四", "五", "六")"""))

        // 周一起始的旧口径必须彻底消失，否则两套锚点会各算各的周次。
        val mainSources = File("src/main/java/com/glut/schedule")
            .takeIf { it.exists() } ?: File("app/src/main/java/com/glut/schedule")
        val leftovers = mainSources.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { file ->
                file.readText().let { text ->
                    text.contains("mondayOf(") || text.contains("normalizeSemesterStartMonday(")
                }
            }
            .map { it.name }
            .toList()
        assertTrue("周一起始的旧函数不应再出现：$leftovers", leftovers.isEmpty())
    }

    @Test
    fun schedulePageKeepsAClockTickingEverySecond() {
        assertTrue("课表页需要每秒重取当前时间", screen.contains("delay(1_000L)"))
        assertTrue(screen.contains("LocalDateTime.now()"))
        // 时钟只在页面这一处，网格与节次列都读它，避免两套时间源。
        assertFalse(grid.contains("delay(1_000L)"))
        assertTrue(screen.contains("now = now.toLocalTime()"))
        assertTrue(screen.contains("val today = now.toLocalDate()"))
    }

    @Test
    fun periodColumnHighlightsThePeriodContainingNow() {
        assertTrue(grid.contains("activeClassPeriodSection(visibleRows, now)"))
        // 只有正在看包含今天的那一周才高亮。
        assertTrue(grid.contains("highlightCurrentPeriod = week.contains(today)"))
        assertTrue(grid.contains("val isActive = period.section == activeSection"))
    }

    @Test
    fun partnerPageSharesTheSundayColumnOrder() {
        val partner = source("partner/PartnerScheduleScreen.kt")
        assertTrue(partner.contains("visibleDayOfWeeks(dayCount)"))
        assertTrue(partner.contains("dayOrder.indexOf(group.dayOfWeek)"))
    }
}
