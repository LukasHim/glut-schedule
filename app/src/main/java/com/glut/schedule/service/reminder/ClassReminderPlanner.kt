package com.glut.schedule.service.reminder

import com.glut.schedule.data.model.ClassPeriod
import com.glut.schedule.data.model.ManualDayCopyRule
import com.glut.schedule.data.model.ScheduleCourse
import com.glut.schedule.widget.ScheduleWidgetSnapshotBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 一条待发送的上课提醒。
 *
 * [key] 由「日期 + 课次稳定 id」组成，用来跨 WorkManager 重跑、跨应用重启去重：
 * 同一节课只提醒一次。名字里刻意不包含课程标题——标题里可能有任意分隔符。
 */
data class ClassReminder(
    val key: String,
    val courseTitle: String,
    val room: String,
    val teacher: String,
    val startTime: String,
    val endTime: String,
    val startAt: LocalDateTime,
    val triggerAt: LocalDateTime
)

/**
 * 上课提醒规划器。
 *
 * 课表解析（周次、调休副本、节次时间）直接复用桌面小组件的快照构建器，保证
 * 「小组件里看到的课」与「提醒的课」是同一份口径：手动调休、隐藏中午、作息覆盖
 * 都不会在这里出现第二套规则。
 *
 * 只规划今天与明天：WorkManager 的单次任务在应用被杀后仍会保留，但没必要把
 * 整个学期的每一次课都排进任务队列——每次提醒触发后都会重新规划。
 */
object ClassReminderPlanner {
    fun remindersFor(
        now: LocalDateTime,
        courses: List<ScheduleCourse>,
        classPeriods: List<ClassPeriod>,
        semesterStartMonday: LocalDate,
        semesterEndDate: LocalDate,
        manualDayCopies: List<ManualDayCopyRule> = emptyList(),
        minutesBefore: Int
    ): List<ClassReminder> {
        val safeMinutes = minutesBefore.coerceAtLeast(0).toLong()
        val snapshot = ScheduleWidgetSnapshotBuilder.build(
            now = now,
            courses = courses,
            classPeriods = classPeriods,
            semesterStartMonday = semesterStartMonday,
            semesterEndDate = semesterEndDate,
            manualDayCopies = manualDayCopies
        )
        return (snapshot.todayCourses + snapshot.tomorrowCourses)
            .mapNotNull { item ->
                val start = parseTime(item.startTime) ?: return@mapNotNull null
                val startAt = LocalDateTime.of(item.date, start)
                ClassReminder(
                    key = "${item.date}|${item.stableId}",
                    courseTitle = item.title,
                    room = item.room,
                    teacher = item.teacher,
                    startTime = item.startTime,
                    endTime = item.endTime,
                    startAt = startAt,
                    triggerAt = startAt.minusMinutes(safeMinutes)
                )
            }
            .distinctBy { it.key }
            .sortedWith(compareBy(ClassReminder::triggerAt, ClassReminder::courseTitle))
    }

    /** 兼容 "08:00" 与 "8:00"；解析失败（旧数据/空作息）时该节课直接跳过。 */
    internal fun parseTime(value: String): LocalTime? {
        if (value.isBlank()) return null
        val candidates = listOf("HH:mm", "H:mm")
        for (pattern in candidates) {
            try {
                return LocalTime.parse(value, DateTimeFormatter.ofPattern(pattern))
            } catch (_: DateTimeParseException) {
                // 继续尝试下一种格式
            }
        }
        return try {
            LocalTime.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
