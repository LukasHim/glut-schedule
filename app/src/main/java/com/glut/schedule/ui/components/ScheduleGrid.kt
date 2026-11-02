package com.glut.schedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glut.schedule.data.model.ClassPeriod
import com.glut.schedule.data.model.NOON_SECTIONS
import com.glut.schedule.data.model.activeClassPeriodSection
import com.glut.schedule.data.model.periodLabel
import com.glut.schedule.data.model.CourseBlock
import com.glut.schedule.data.model.SemesterAdjustment
import com.glut.schedule.data.model.adjustmentMarkerFor
import com.glut.schedule.data.model.readableMarkerColor
import com.glut.schedule.data.model.ScheduleWeek
import com.glut.schedule.data.model.visibleDayCount
import com.glut.schedule.data.model.visibleDayOfWeeks
import java.time.LocalDate
import java.time.LocalTime

private val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")

/**
 * 教务星期序号（1=周一 … 7=周日）在课表列里的下标；不在显示范围内返回 -1。
 *
 * 列顺序**只能**从 [visibleDayOfWeeks] 推导：显示周末时是「日、一…六」，隐藏周末时是
 * 「一…五」。曾经这里写成「周日特判 0、其余原样」，隐藏周末时周一就会落到第 2 列，
 * 整列课程与表头错开一格。表头、卡片定位、网格线必须共用同一份顺序。
 */
internal fun scheduleColumnIndexOf(dayOfWeek: Int, dayOrder: List<Int>): Int =
    dayOrder.indexOf(dayOfWeek)

/**
 * 固定节次高度内必须显式约束文字行高，并移除部分系统字体自带的超大上下留白。
 * 否则在 vivo 等使用手写系统字体且放大字号的设备上，第三行结束时间会被父布局裁切。
 */
fun periodColumnTextStyle(
    fontSize: TextUnit,
    lineHeight: TextUnit
): TextStyle = TextStyle(
    fontSize = fontSize,
    lineHeight = lineHeight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

data class ScheduleCalendarDay(
    val name: String,
    val date: LocalDate?,
    val isToday: Boolean
)

/**
 * 日期栏右下角的单字状态角标。
 *
 * 与小程序 `components/schedule-grid` 的 `dateMarker` 一致：一个日期只显示一个字，
 * 「调」优先于「休」——手动调课意味着该日确实追加了课程，比假期状态更需要被看到。
 */
internal enum class ScheduleDayMarker(val label: String) {
    HOLIDAY("休"),
    ADJUSTMENT("调")
}

/**
 * 课表网格线配色。
 *
 * 用半透明白而不是实色：课表可以铺任意自定义背景图，实色线在深色图上会白得刺眼、
 * 在浅色图上又完全看不见，半透明白在两种底上都只是「隐约可见」。
 * 时段分界（上午/中午/下午/晚上）比普通行线更亮更粗，让一天的分段一眼可辨。
 */
private val GridLineColor = Color.White.copy(alpha = 0.10f)
private val GridLineBoundaryColor = Color.White.copy(alpha = 0.26f)

/**
 * 判定某一天该显示哪个角标。
 *
 * `date` 为 null（未显示日期、或历史学期缺少权威日历时）一律不显示角标——
 * 角标依附于具体自然日，没有日期就没有落点。
 * 只标法定放假日：补班日（节假日接口里 `holiday: false`）不会进 [holidayDates]，因此天然无角标。
 */
internal fun scheduleDayMarker(
    date: LocalDate?,
    holidayDates: Set<LocalDate>,
    manualAdjustmentDates: Set<LocalDate>
): ScheduleDayMarker? = when {
    date == null -> null
    date in manualAdjustmentDates -> ScheduleDayMarker.ADJUSTMENT
    date in holidayDates -> ScheduleDayMarker.HOLIDAY
    else -> null
}

/** 首页与情侣/基友课表共用同一份日期映射，避免星期、日期和今日高亮发生漂移。 */
fun scheduleCalendarDays(
    week: ScheduleWeek,
    today: LocalDate,
    dayCount: Int,
    showCalendarDates: Boolean
): List<ScheduleCalendarDay> =
    visibleDayOfWeeks(dayCount).map { dayOfWeek ->
        val date = week.dateFor(dayOfWeek).takeIf { showCalendarDates }
        ScheduleCalendarDay(
            name = dayNames[dayOfWeek % 7],
            date = date,
            isToday = date == today
        )
    }

@Composable
fun ScheduleGrid(
    week: ScheduleWeek,
    today: LocalDate,
    periods: List<ClassPeriod>,
    blocks: List<CourseBlock>,
    showWeekend: Boolean,
    showNoon: Boolean = false,
    /** 是否绘制课表网格线；默认关闭，避免在自定义背景图上凭空多出一层线。 */
    showGridLines: Boolean = false,
    /**
     * 各时段的最后一个内部节次（上午/中午/下午/晚上依次排列），来自 `CampusCapabilities`。
     *
     * 只用来判断相邻两行是否跨时段，从而把分界缝画得比普通行线更重。
     * 默认空列表表示「不做时段区分」，此时所有行线粗细一致。
     */
    blockEndSections: List<Int> = emptyList(),
    /**
     * 该来源是否含「中午1/中午2」两个特殊节次。
     *
     * 过去这里靠 `periods.size <= 11` 推断「是不是南宁」，加入研究生（也是 12 节、
     * 也没有中午节次）后这条推断同时失效：它会把研究生的第 7-12 节当成桂林节次整体前移 2。
     * 默认值保持旧口径，调用方应按来源显式传入。
     */
    hasNoonSections: Boolean = periods.size > 11,
    showCalendarDates: Boolean = true,
    holidayDates: Set<LocalDate> = emptySet(),
    manualAdjustmentDates: Set<LocalDate> = emptySet(),
    /** 当前时刻；由课表页每秒刷新一次，用来定位「正在上的那一节」。 */
    now: LocalTime = LocalTime.now(),
    /** 长按卡片时回调当前**显示中**的那一张（冲突组里只有它是可见的），打开卡片管理弹层。 */
    onCourseLongClick: ((CourseBlock) -> Unit)? = null,
    /** 该学期的教务调课记录，用来给调课/补课产生的卡片打「调」/「补」角标。 */
    adjustments: List<SemesterAdjustment> = emptyList(),
    /**
     * false：课表铺在背景图上，文字固定用白色；
     * true：课表使用主题背景，文字与网格线全部走 MaterialTheme 角色色。
     */
    useSurfaceColors: Boolean = true,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val rowHeight = 74.dp
        val leftWidth = 52.dp
        val dayCount = visibleDayCount(showWeekend)
        // 表头、卡片定位、网格线共用这一份列顺序：隐藏周末时是「一…五」，
        // 显示周末时周日排在最左。任何一处自己算下标都会在隐藏周末时错位。
        val dayOrder = visibleDayOfWeeks(dayCount)
        val dayWidth = (maxWidth - leftWidth) / dayCount
        // 没有中午节次的来源（南宁、研究生）必须原样显示全部节次，不折叠也不偏移；
        // 桂林（14节）才按用户的「显示中午」开关决定是否折叠中间两行。
        val effectiveShowNoon = showNoon || !hasNoonSections
        // 可见节次行只有这一份，节次列、课程卡片定位和网格线全部按它算行号。
        // 各自再过滤一次的话，隐藏中午时线会比卡片整整错开两行。
        val visibleRows = remember(periods, effectiveShowNoon) {
            visiblePeriodRows(periods, effectiveShowNoon)
        }
        val visibleBlocks = remember(blocks, dayOrder, effectiveShowNoon) {
            blocks.filter { block ->
                block.occurrence.dayOfWeek in dayOrder &&
                    (effectiveShowNoon || block.occurrence.startSection !in NOON_SECTIONS)
            }
        }

        Column {
            ScheduleCalendarHeader(
                week = week,
                today = today,
                leftWidth = leftWidth,
                dayWidth = dayWidth,
                dayCount = dayCount,
                showCalendarDates = showCalendarDates,
                holidayDates = holidayDates,
                manualAdjustmentDates = manualAdjustmentDates,
                useSurfaceColors = useSurfaceColors
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                PeriodColumn(
                    visibleRows = visibleRows,
                    rowHeight = rowHeight,
                    width = leftWidth,
                    hasNoonSections = hasNoonSections,
                    now = now,
                    // 只有正在看「包含今天的那一周」才谈得上当前节次；翻到别的周时高亮必须消失。
                    highlightCurrentPeriod = week.contains(today),
                    useSurfaceColors = useSurfaceColors
                )
                TimetableBody(
                    visibleRows = visibleRows,
                    blocks = visibleBlocks,
                    rowHeight = rowHeight,
                    dayWidth = dayWidth,
                    dayCount = dayCount,
                    dayOrder = dayOrder,
                    showGridLines = showGridLines,
                    blockEndSections = blockEndSections,
                    onCourseLongClick = onCourseLongClick,
                    weekNumber = week.number,
                    adjustments = adjustments,
                    useSurfaceColors = useSurfaceColors,
                )
            }
        }
    }
}

@Composable
fun ScheduleCalendarHeader(
    week: ScheduleWeek,
    today: LocalDate,
    leftWidth: Dp,
    dayWidth: Dp,
    dayCount: Int,
    showCalendarDates: Boolean = true,
    holidayDates: Set<LocalDate> = emptySet(),
    manualAdjustmentDates: Set<LocalDate> = emptySet(),
    useSurfaceColors: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth()) {
        MonthHeader(
            week = week,
            width = leftWidth,
            showCalendarDates = showCalendarDates,
            color = if (useSurfaceColors) colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.9f)
        )
        WeekDayHeader(
            week = week,
            today = today,
            dayWidth = dayWidth,
            dayCount = dayCount,
            showCalendarDates = showCalendarDates,
            holidayDates = holidayDates,
            manualAdjustmentDates = manualAdjustmentDates,
            useSurfaceColors = useSurfaceColors
        )
    }
}

@Composable
private fun MonthHeader(
    week: ScheduleWeek,
    width: Dp,
    showCalendarDates: Boolean,
    color: Color
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(
                start = scheduleGridMonthHeaderStartPaddingDp().dp,
                top = scheduleGridMonthHeaderTopPaddingDp().dp
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (showCalendarDates) scheduleGridMonthText(week.sunday) else "周次",
            color = color,
            fontSize = 12.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun scheduleGridMonthText(date: LocalDate): String {
    return "${date.monthValue}月"
}

fun scheduleGridMonthHeaderStartPaddingDp(): Int = 15

fun scheduleGridMonthHeaderTopPaddingDp(): Int = 6

@Composable
private fun WeekDayHeader(
    week: ScheduleWeek,
    today: LocalDate,
    dayWidth: Dp,
    dayCount: Int,
    showCalendarDates: Boolean,
    holidayDates: Set<LocalDate>,
    manualAdjustmentDates: Set<LocalDate>,
    useSurfaceColors: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val activeColor = if (useSurfaceColors) colorScheme.onSurface else Color.White
    val inactiveNameColor = if (useSurfaceColors) {
        colorScheme.onSurfaceVariant
    } else {
        Color.White.copy(alpha = 0.42f)
    }
    val inactiveDateColor = if (useSurfaceColors) {
        colorScheme.outline
    } else {
        Color.White.copy(alpha = 0.34f)
    }
    val todayContainer = if (useSurfaceColors) {
        colorScheme.primaryContainer
    } else {
        Color.White.copy(alpha = 0.22f)
    }
    val todayContainerContent = if (useSurfaceColors) {
        colorScheme.onPrimaryContainer
    } else {
        Color.White
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        scheduleCalendarDays(week, today, dayCount, showCalendarDates).forEach { item ->
            val marker = scheduleDayMarker(item.date, holidayDates, manualAdjustmentDates)
            // 用 Box 叠加而不是往列里塞第三行：角标绝对定位在右下角，
            // 不改变日期栏高度、列宽，也不会挤动下面的课程卡片。
            Box(
                modifier = Modifier
                    .width(dayWidth)
                    .padding(bottom = 4.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.name,
                        color = if (item.isToday) activeColor else inactiveNameColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    item.date?.let { date ->
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = if (item.isToday) todayContainerContent else inactiveDateColor,
                            fontSize = 12.sp,
                            modifier = if (item.isToday) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(todayContainer)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            } else {
                                Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            }
                        )
                    }
                }
                if (marker != null) {
                    Text(
                        text = marker.label,
                        color = when (marker) {
                            ScheduleDayMarker.ADJUSTMENT -> colorScheme.primary
                            ScheduleDayMarker.HOLIDAY -> colorScheme.tertiary
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * 当前实际显示的节次行。
 *
 * 有中午节次的来源（桂林）在关闭「显示中午」时要把午1/午2 两行整行拿掉；
 * 南宁与研究生没有这两节，`effectiveShowNoon` 恒为 true，因此原样返回全部节次。
 */
fun visiblePeriodRows(
    periods: List<ClassPeriod>,
    effectiveShowNoon: Boolean
): List<ClassPeriod> = if (effectiveShowNoon) periods else periods.filter { it.section !in NOON_SECTIONS }

/** 内部节次号在可见行里的下标；不在可见行里（例如被隐藏的午1/午2）返回 null。 */
fun scheduleRowIndex(section: Int, visibleRows: List<ClassPeriod>): Int? =
    visibleRows.indexOfFirst { it.section == section }.takeIf { it >= 0 }

/**
 * 内部节次号属于哪个时段（0 起：上午 / 中午 / 下午 / 晚上）。
 *
 * [blockEndSections] 装的是各时段的最后一个内部节次，所以「比我小的上界有几个」就是我的时段号。
 * 用时段号而不是节次号本身来比较，隐藏中午时 4 与 7 会自然判成不同时段，
 * 它们之间那条缝就并成了一条分界线，不会留下两条并排的线或画在错误的位置。
 */
fun scheduleTimeBlockIndex(section: Int, blockEndSections: List<Int>): Int =
    blockEndSections.count { it < section }

/**
 * 下缘需要画**加粗分界线**的行下标（0 起）。
 *
 * 横线画在行的下缘，所以判据必须是「本行与**下一行**的时段不同」—— 这条缝隙正好落在本行底部。
 * **不能拿上一行来比**：那会把加粗线整体压低一行，第 4/5 节之间的线画到 5/6 之间，
 * 研究生 12 节时就会看到加粗线跑在第 5 节、第 9 节的后面。
 *
 * 比的是时段号而不是节次号，于是桂林隐藏中午后「4 与 7 相邻」也会被判成一条分界线。
 */
fun gridBoldRowIndices(
    visibleRows: List<ClassPeriod>,
    blockEndSections: List<Int>
): List<Int> = visibleRows.indices.filter { index ->
    val next = visibleRows.getOrNull(index + 1)
    next != null &&
        scheduleTimeBlockIndex(visibleRows[index].section, blockEndSections) !=
        scheduleTimeBlockIndex(next.section, blockEndSections)
}

@Composable
private fun PeriodColumn(
    visibleRows: List<ClassPeriod>,
    rowHeight: Dp,
    width: Dp,
    hasNoonSections: Boolean = true,
    /** 当前时刻；由调用方每秒刷新，列本身不再自带时钟，避免两套时间源打架。 */
    now: LocalTime,
    /** 当前显示的这一周是否包含今天；为 false 时即使此刻正在上课也不高亮。 */
    highlightCurrentPeriod: Boolean = true,
    useSurfaceColors: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryText = if (useSurfaceColors) colorScheme.onSurface else Color.White
    val secondaryText = if (useSurfaceColors) {
        colorScheme.onSurfaceVariant
    } else {
        Color.White.copy(alpha = 0.56f)
    }
    val activeSection = if (highlightCurrentPeriod) {
        activeClassPeriodSection(visibleRows, now)
    } else {
        null
    }
    val activeBackground = if (useSurfaceColors) {
        colorScheme.primaryContainer
    } else {
        Color.White.copy(alpha = 0.18f)
    }
    val activeForeground = if (useSurfaceColors) {
        colorScheme.onPrimaryContainer
    } else {
        Color.White
    }
    // 有中午节次的来源（桂林）用 periodLabel() 把内部号映射成「午1/午2 + 5-12」；
    // 南宁与研究生没有中午节次，内部号即显示号，直排。
    Column(modifier = Modifier.width(width)) {
        visibleRows.forEach { period ->
            val isActive = period.section == activeSection
            Column(
                modifier = Modifier
                    .height(rowHeight)
                    // 左右各让 3dp 给高亮底色，再补 3dp 内边距——高亮前后文字位置完全一致，
                    // 不会因为进入/离开当前节次而左右跳动。
                    .padding(horizontal = 3.dp)
                    .then(
                        if (isActive) {
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(activeBackground)
                        } else {
                            Modifier
                        }
                    )
                    .padding(start = 3.dp, top = 7.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (hasNoonSections) period.periodLabel() else "${period.section}",
                    color = if (isActive) activeForeground else primaryText,
                    style = periodColumnTextStyle(fontSize = 14.sp, lineHeight = 16.sp),
                    maxLines = 1,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = period.startsAt,
                    color = if (isActive) activeForeground else secondaryText,
                    style = periodColumnTextStyle(fontSize = 9.sp, lineHeight = 11.sp),
                    maxLines = 1
                )
                Text(
                    text = period.endsAt,
                    color = if (isActive) activeForeground else secondaryText,
                    style = periodColumnTextStyle(fontSize = 9.sp, lineHeight = 11.sp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TimetableBody(
    visibleRows: List<ClassPeriod>,
    blocks: List<CourseBlock>,
    rowHeight: Dp,
    dayWidth: Dp,
    dayCount: Int,
    /** 与表头完全一致的列顺序（教务星期序号）。卡片横向位置只能按它算。 */
    dayOrder: List<Int> = visibleDayOfWeeks(dayCount),
    showGridLines: Boolean = false,
    blockEndSections: List<Int> = emptyList(),
    onCourseLongClick: ((CourseBlock) -> Unit)? = null,
    weekNumber: Int = 0,
    adjustments: List<SemesterAdjustment> = emptyList(),
    useSurfaceColors: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    val gridLineColor = if (useSurfaceColors) {
        colorScheme.outlineVariant.copy(alpha = 0.7f)
    } else {
        GridLineColor
    }
    val gridLineBoundaryColor = if (useSurfaceColors) {
        colorScheme.outline.copy(alpha = 0.85f)
    } else {
        GridLineBoundaryColor
    }
    val totalHeight = rowHeight * visibleRows.size
    Box(
        modifier = Modifier
            .size(width = dayWidth * dayCount, height = totalHeight)
            .drawBehind {
                if (!showGridLines) return@drawBehind
                val rowPx = rowHeight.toPx()
                val colPx = dayWidth.toPx()
                val normalStroke = 0.7.dp.toPx()
                val boundaryStroke = 1.6.dp.toPx()
                // 哪些行的下缘是时段分界。判据收在 gridBoldRowIndices 里：线画在**下缘**，
                // 就必须跟**下一行**比时段 —— 跟上一行比会把加粗线整体压低一行。
                val boldRows = gridBoldRowIndices(visibleRows, blockEndSections).toSet()

                // 横线：逐行下缘。行号与课程卡片共用同一份 visibleRows，隐藏中午时不会错位。
                visibleRows.forEachIndexed { index, _ ->
                    val isBoundary = index in boldRows
                    val stroke = if (isBoundary) boundaryStroke else normalStroke
                    // 末行贴着容器底边，线宽的一半会被裁掉，所以把 y 收进来半个线宽。
                    val y = (rowPx * (index + 1)).coerceAtMost(size.height - stroke / 2f)
                    drawLine(
                        color = if (isBoundary) gridLineBoundaryColor else gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = stroke
                    )
                }

                // 竖线：天与天之间的分隔，贯穿整个课表主体。用普通行线的配色 ——
                // 它标的是「换了一天」而不是时段分界，不该跟加粗线抢注意力。
                for (day in 1 until dayCount) {
                    val x = (colPx * day).coerceAtMost(size.width - normalStroke / 2f)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = normalStroke
                    )
                }
            }
    ) {
        val blockGroups = remember(blocks) { overlappingCourseBlockGroups(blocks) }
        blockGroups.forEach { group ->
            val groupKey = remember(group) { courseBlockGroupKey(group) }
            key(groupKey) {
                var activeIndex by remember { mutableStateOf(0) }
                val activeBlock = group[activeIndex % group.size]
                val nextBlock = if (group.size > 1) {
                    { activeIndex = (activeIndex + 1) % group.size }
                } else {
                    null
                }

                CourseCard(
                    block = activeBlock,
                    conflictCount = group.size,
                    onConflictClick = nextBlock,
                    // 交给上层的始终是 activeBlock：冲突组里只有当前显示的那一张是看得见的。
                    onLongClick = onCourseLongClick?.let { callback -> { callback(activeBlock) } },
                    marker = adjustmentMarkerFor(
                        occurrence = activeBlock.occurrence,
                        courseTitle = activeBlock.course.title,
                        weekNumber = weekNumber,
                        adjustments = adjustments
                    ),
                    modifier = Modifier
                        .offset(
                            x = dayWidth * scheduleColumnIndexOf(
                                activeBlock.occurrence.dayOfWeek,
                                dayOrder
                            ).coerceAtLeast(0) + 2.dp,
                            // 行号直接从可见行列表查，不再自己按节次号推算偏移：
                            // 「隐藏中午时减 3」与 visibleRows 是同一规则的两种写法，分开写迟早漂移。
                            y = rowHeight * (
                                scheduleRowIndex(activeBlock.occurrence.startSection, visibleRows) ?: 0
                            ) + 3.dp
                        )
                        .size(
                            width = dayWidth - 4.dp,
                            height = rowHeight * activeBlock.occurrence.sectionSpan - 6.dp
                        )
                )
            }
        }
    }
}

fun overlappingCourseBlockGroups(blocks: List<CourseBlock>): List<List<CourseBlock>> {
    return blocks
        .groupBy { it.occurrence.dayOfWeek }
        .toSortedMap()
        .flatMap { (_, dayBlocks) ->
            val sortedBlocks = dayBlocks.sortedWith(courseBlockPositionComparator())
            val groups = mutableListOf<List<CourseBlock>>()
            val currentGroup = mutableListOf<CourseBlock>()
            var currentEndSection = 0

            sortedBlocks.forEach { block ->
                if (currentGroup.isEmpty() || block.occurrence.startSection <= currentEndSection) {
                    currentGroup += block
                    currentEndSection = maxOf(currentEndSection, block.occurrence.endSection)
                } else {
                    groups += currentGroup.sortedWith(courseBlockDisplayComparator())
                    currentGroup.clear()
                    currentGroup += block
                    currentEndSection = block.occurrence.endSection
                }
            }

            if (currentGroup.isNotEmpty()) {
                groups += currentGroup.sortedWith(courseBlockDisplayComparator())
            }

            groups
        }
}

private fun courseBlockGroupKey(group: List<CourseBlock>): String {
    return group.joinToString(separator = "|") { block ->
        listOf(
            block.course.id,
            block.occurrence.id,
            block.occurrence.dayOfWeek,
            block.occurrence.startSection,
            block.occurrence.endSection
        ).joinToString(separator = ":")
    }
}

private fun courseBlockPositionComparator(): Comparator<CourseBlock> {
    return compareBy<CourseBlock> { it.occurrence.startSection }
        .thenBy { it.occurrence.endSection }
        .thenBy { it.course.title }
        .thenBy { it.occurrence.id }
}

private fun courseBlockDisplayComparator(): Comparator<CourseBlock> {
    return compareBy<CourseBlock> { it.occurrence.sectionSpan }
        .thenBy { it.occurrence.startSection }
        .thenBy { it.occurrence.endSection }
        .thenBy { it.course.title }
        .thenBy { it.occurrence.id }
}

@Composable
private fun CourseCard(
    block: CourseBlock,
    conflictCount: Int,
    onConflictClick: (() -> Unit)?,
    onLongClick: (() -> Unit)? = null,
    /** 「调」/「补」角标文本；为空表示这张卡不是调课/补课产生的。 */
    marker: String? = null,
    modifier: Modifier = Modifier
) {
    val color = remember(block.course.colorHex) { Color(android.graphics.Color.parseColor(block.course.colorHex)) }
    val titleSize = courseCardTitleTextSize(block.course.title)
    val titleLineHeight = courseCardTitleLineHeight()
    // 长按与「单击切换冲突课」共存：combinedClickable 保证长按时不会再触发 onClick，
    // 长按也不推进冲突索引，两者互不干扰。
    val clickableModifier = Modifier.combinedClickable(
        onClick = { if (conflictCount > 1) onConflictClick?.invoke() },
        onLongClick = onLongClick
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(color)
            .then(clickableModifier)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .semantics {
                contentDescription =
                    "${block.course.title}，${block.course.teacher}，${block.course.room}" +
                    (if (conflictCount > 1) "，共${conflictCount}门冲突课程，单击切换" else "") +
                    "，长按管理卡片"
            }
    ) {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = block.course.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = titleSize,
                lineHeight = titleLineHeight,
                maxLines = courseCardTitleMaxLines(block.occurrence.sectionSpan),
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${block.course.room}",
                color = Color.White,
                fontSize = courseCardRoomTextSize(),
                lineHeight = courseCardRoomLineHeight(),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = block.course.teacher,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = courseCardTeacherTextSize(),
                lineHeight = courseCardTeacherLineHeight()
            )
        }

        if (marker != null) {
            // 左下角与右下角的冲突角标互不干涉。字色按卡片底色自适应：底色是任意可选的，
            // 固定字色必然会在某些卡上糊掉（详见 readableMarkerColor 的注释）。
            Text(
                text = marker,
                modifier = Modifier.align(Alignment.BottomStart),
                color = Color(android.graphics.Color.parseColor(readableMarkerColor(block.course.colorHex))),
                fontSize = 10.sp,
                lineHeight = 10.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (conflictCount > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conflictCount.toString(),
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        lineHeight = 9.sp
                    )
                }
            }
        }

    }
}

fun courseCardTitleTextSize(title: String): TextUnit = 11.sp

fun courseCardTitleLineHeight(): TextUnit = 12.sp

fun courseCardRoomTextSize(): TextUnit = 10.sp

fun courseCardRoomLineHeight(): TextUnit = 11.sp

fun courseCardTeacherTextSize(): TextUnit = 10.sp

fun courseCardTeacherLineHeight(): TextUnit = 11.sp

fun courseCardTitleMaxLines(sectionSpan: Int): Int {
    return when {
        sectionSpan <= 1 -> 2
        sectionSpan == 2 -> 5
        else -> sectionSpan * 3
    }
}
