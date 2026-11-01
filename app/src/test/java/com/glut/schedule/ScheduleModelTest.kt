package com.glut.schedule

import com.glut.schedule.data.model.CourseOccurrence
import com.glut.schedule.data.model.ScheduleCourse
import com.glut.schedule.data.model.ScheduleWeek
import com.glut.schedule.data.model.academicWeekForDate
import com.glut.schedule.data.model.historicalAcademicMaxWeek
import com.glut.schedule.data.model.academicMaxWeekForCalendar
import com.glut.schedule.data.model.academicMaxWeekForSemester
import com.glut.schedule.data.model.visibleDayCount
import com.glut.schedule.data.model.visibleDayOfWeeks
import com.glut.schedule.data.model.defaultClassPeriods
import com.glut.schedule.data.model.ClassPeriod
import com.glut.schedule.data.model.activeClassPeriodSection
import com.glut.schedule.data.model.scheduleWeekForNumber
import com.glut.schedule.data.model.clampAcademicWeek
import com.glut.schedule.data.model.isActiveInWeek
import com.glut.schedule.data.model.offsetSectionForNoon
import com.glut.schedule.data.model.normalizeRoomKey
import com.glut.schedule.data.model.AcademicSemester
import com.glut.schedule.data.model.SemesterCacheStatus
import com.glut.schedule.data.model.SemesterSeason
import com.glut.schedule.data.settings.CampusType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleModelTest {
    @Test
    fun historicalMaximumWeekPrefersPortalDropdownOverCourseOccurrences() {
        val course = ScheduleCourse(
            id = "internship",
            title = "实习",
            room = "",
            teacher = "教师",
            colorHex = "#4477AA",
            occurrences = listOf(
                CourseOccurrence("occ", "internship", 1, 1, 2, "1-16周", "")
            )
        )

        assertEquals(19, historicalAcademicMaxWeek(portalMaxWeek = 19, courses = listOf(course)))
        assertEquals(16, historicalAcademicMaxWeek(portalMaxWeek = null, courses = listOf(course)))
    }

    @Test
    fun historicalMaximumWeekIgnoresPersistedCalendarDates() {
        val course = ScheduleCourse(
            id = "internship",
            title = "实习",
            room = "",
            teacher = "教师",
            colorHex = "#4477AA",
            occurrences = listOf(
                CourseOccurrence("occ", "internship", 1, 1, 2, "1-16周", "")
            )
        )

        assertEquals(
            19,
            academicMaxWeekForSemester(
                isCurrentSemester = false,
                portalMaxWeek = 19,
                courses = listOf(course),
                semesterStartDate = LocalDate.of(2024, 9, 2),
                semesterEndDate = LocalDate.of(2024, 12, 22)
            )
        )
    }
    @Test
    fun scheduleWeek_movesBySevenDaysAndUpdatesWeekNumber() {
        val current = ScheduleWeek(number = 9, sunday = LocalDate.of(2026, 5, 3))

        assertEquals(8, current.previous().number)
        assertEquals(LocalDate.of(2026, 4, 26), current.previous().sunday)
        assertEquals(10, current.next().number)
        assertEquals(LocalDate.of(2026, 5, 10), current.next().sunday)
    }

    @Test
    fun scheduleWeekContainsOnlyItsOwnSundayToSaturdayRange() {
        val week = scheduleWeekForNumber(1, LocalDate.of(2026, 3, 9))

        assertEquals(LocalDate.of(2026, 3, 8), week.sunday)
        assertTrue(week.contains(LocalDate.of(2026, 3, 8)))
        assertTrue(week.contains(LocalDate.of(2026, 3, 14)))
        assertTrue(!week.contains(LocalDate.of(2026, 3, 7)))
        assertTrue(!week.contains(LocalDate.of(2026, 3, 15)))
    }

    @Test
    fun defaultClassPeriodsCoverFourteenSections() {
        val periods = defaultClassPeriods()

        assertEquals(14, periods.size)
        assertEquals("08:30", periods.first().startsAt)
        assertEquals("21:50", periods.last().endsAt)
    }

    @Test
    fun defaultClassPeriodsUseTheSelectedCampusTemplate() {
        val guilin = defaultClassPeriods(CampusType.GUILIN)
        val nanning = defaultClassPeriods(CampusType.NANNING)

        assertEquals((1..14).toList(), guilin.map { it.section })
        assertEquals("08:30", guilin.first().startsAt)
        assertEquals((1..11).toList(), nanning.map { it.section })
        assertEquals("08:40", nanning.first().startsAt)
    }

    @Test
    fun occurrenceHeightUsesSectionSpan() {
        val occurrence = CourseOccurrence(
            id = "logic-1",
            courseId = "logic",
            dayOfWeek = 2,
            startSection = 1,
            endSection = 2,
            weekText = "1-16周",
            note = ""
        )

        assertEquals(2, occurrence.sectionSpan)
    }

    @Test
    fun academicWeekIsClampedToNormalSemesterRange() {
        assertEquals(1, clampAcademicWeek(-1))
        assertEquals(1, clampAcademicWeek(0))
        assertEquals(1, clampAcademicWeek(1))
        assertEquals(9, clampAcademicWeek(9))
        assertEquals(22, clampAcademicWeek(22))
        assertEquals(22, clampAcademicWeek(23))
        assertEquals(22, clampAcademicWeek(100))
    }

    @Test
    fun academicWeekCountsFromTheConfiguredSemesterStartSunday() {
        // 一周从周日开始：2026-03-09（周一）归一到上一个周日 2026-03-08，那才是第 1 周的第一天。
        val semesterStartDate = LocalDate.of(2026, 3, 9)

        assertEquals(1, academicWeekForDate(LocalDate.of(2026, 3, 8), semesterStartDate))
        assertEquals(1, academicWeekForDate(LocalDate.of(2026, 3, 14), semesterStartDate))
        assertEquals(2, academicWeekForDate(LocalDate.of(2026, 3, 15), semesterStartDate))
        assertEquals(2, academicWeekForDate(LocalDate.of(2026, 3, 16), semesterStartDate))
        assertEquals(9, academicWeekForDate(LocalDate.of(2026, 5, 5), semesterStartDate))
    }

    @Test
    fun academicMaxWeekUsesCalendarEndDate() {
        val semesterStartDate = LocalDate.of(2026, 3, 9)
        // 第 19 周从 2026-07-12（周日）开始，其最后一天是周六 2026-07-18。
        val semesterEndDate = LocalDate.of(2026, 7, 18)

        assertEquals(19, academicMaxWeekForCalendar(semesterStartDate, semesterEndDate))
        assertEquals(19, academicWeekForDate(LocalDate.of(2026, 7, 27), semesterStartDate, maxWeek = 19))
    }

    @Test
    fun scheduleWeekForNumberStartsEachWeekOnSunday() {
        val semesterStartDate = LocalDate.of(2026, 3, 9)

        assertEquals(LocalDate.of(2026, 3, 8), scheduleWeekForNumber(1, semesterStartDate).sunday)
        assertEquals(LocalDate.of(2026, 5, 3), scheduleWeekForNumber(9, semesterStartDate).sunday)
    }

    @Test
    fun visibleDayOfWeeksPutsSundayFirstWhenTheWeekendIsShown() {
        assertEquals(listOf(1, 2, 3, 4, 5), visibleDayOfWeeks(5))
        assertEquals(listOf(7, 1, 2, 3, 4, 5, 6), visibleDayOfWeeks(7))
    }

    @Test
    fun activeClassPeriodSectionPicksThePeriodContainingNow() {
        val periods = listOf(
            ClassPeriod(1, "08:30", "09:15"),
            ClassPeriod(2, "09:20", "10:05")
        )

        assertEquals(1, activeClassPeriodSection(periods, LocalTime.of(8, 30)))
        assertEquals(1, activeClassPeriodSection(periods, LocalTime.of(9, 14)))
        // 区间左闭右开：下课铃一响就不再属于第 1 节。
        assertNull(activeClassPeriodSection(periods, LocalTime.of(9, 15)))
        // 下一节开始的那一刻立刻切到下一节。
        assertEquals(2, activeClassPeriodSection(periods, LocalTime.of(9, 20)))
        assertNull(activeClassPeriodSection(periods, LocalTime.of(8, 0)))
        // 两节之间的课间不高亮任何一行。
        assertNull(activeClassPeriodSection(periods, LocalTime.of(9, 17)))
        assertNull(activeClassPeriodSection(periods, LocalTime.of(10, 5)))
    }

    @Test
    fun activeClassPeriodSectionSkipsPeriodsWithoutUsableTimes() {
        // 研究生作息允许整份不填钟点：没有时间就永远不高亮，而不是当成「一直在上课」。
        val blank = listOf(ClassPeriod(1, "", ""), ClassPeriod(2, "", ""))
        assertNull(activeClassPeriodSection(blank, LocalTime.of(9, 0)))

        val unparsable = listOf(ClassPeriod(1, "08:30", "--:--"))
        assertNull(activeClassPeriodSection(unparsable, LocalTime.of(9, 0)))
    }

    @Test
    fun visibleDayCountDefaultsToWeekdaysOnlyUnlessWeekendIsEnabled() {
        assertEquals(5, visibleDayCount(showWeekend = false))
        assertEquals(7, visibleDayCount(showWeekend = true))
    }

    @Test
    fun occurrenceActiveWeekParsesRangesSinglesAndOddEvenWeeks() {
        assertTrue(occurrenceWithWeek("1-12周").isActiveInWeek(9))
        assertTrue(!occurrenceWithWeek("1-12周").isActiveInWeek(13))
        assertTrue(occurrenceWithWeek("第16周").isActiveInWeek(16))
        assertTrue(!occurrenceWithWeek("第16周").isActiveInWeek(15))
        assertTrue(occurrenceWithWeek("3-12,16-17").isActiveInWeek(16))
        assertTrue(!occurrenceWithWeek("3-12,16-17").isActiveInWeek(13))
        assertTrue(occurrenceWithWeek("5-13单周").isActiveInWeek(9))
        assertTrue(!occurrenceWithWeek("5-13单周").isActiveInWeek(10))
        assertTrue(occurrenceWithWeek("6-14双周").isActiveInWeek(10))
        assertTrue(!occurrenceWithWeek("6-14双周").isActiveInWeek(11))
        assertTrue(occurrenceWithWeek("1,4").isActiveInWeek(4))
        assertTrue(!occurrenceWithWeek("1,4").isActiveInWeek(2))
    }

    @Test
    fun occurrenceActiveWeekAppliesParityToEachSegmentOnly() {
        val occurrence = occurrenceWithWeek("6-14双,16-18")

        assertTrue(occurrence.isActiveInWeek(6))
        assertTrue(!occurrence.isActiveInWeek(7))
        assertTrue(occurrence.isActiveInWeek(14))
        assertTrue(occurrence.isActiveInWeek(16))
        assertTrue(occurrence.isActiveInWeek(17))
        assertTrue(occurrence.isActiveInWeek(18))
        assertTrue(!occurrence.isActiveInWeek(19))
    }

    @Test
    fun occurrenceActiveWeekAcceptsChineseRangeSeparators() {
        assertTrue(occurrenceWithWeek("6－14双周").isActiveInWeek(10))
        assertTrue(occurrenceWithWeek("16—18周").isActiveInWeek(17))
    }

    @Test
    fun noonOffsetOnlyAppliesFromPeriodFiveOnwardsAndOnlyWhenNoonExists() {
        // 桂林「中午1/2」夹在第4节与第5节之间，第 5 节起内部号 +2。
        // 这条规则由桂林与南宁两条解析路径共用，边界必须锁死，否则任一侧改动都会静默移位。
        assertEquals(1, offsetSectionForNoon(1, hasNoon = true))
        assertEquals(4, offsetSectionForNoon(4, hasNoon = true))
        assertEquals(7, offsetSectionForNoon(5, hasNoon = true))
        assertEquals(8, offsetSectionForNoon(6, hasNoon = true))
        // 南宁没有中午时段，节次直排，任何节次都不偏移。
        assertEquals(5, offsetSectionForNoon(5, hasNoon = false))
        assertEquals(12, offsetSectionForNoon(12, hasNoon = false))
    }

    /**
     * 教务的教室主数据里混过**全角字母**：出问题那条记录是 `05614Ｄ`（全角 Ｄ），
     * 而调课表里写的是半角 `05614D`。两者必须归一到同一个键，否则
     * 「调/补」角标（`AdjustmentMarker`）与「移除被调走的原周次」
     * （`AcademicScheduleParser`）都会静默落空 —— 不报错、不丢卡片，
     * 只是那一周本该消失的课还在。
     */
    @Test
    fun roomKeyNormalizesFullWidthLettersFromTheAcademicSystem() {
        assertEquals(normalizeRoomKey("05614D"), normalizeRoomKey("05614Ｄ"))
        // NFKC 之外仍要求「去空白 + 大写」。
        assertEquals(normalizeRoomKey("05614D"), normalizeRoomKey("  05614Ｄ  "))
        // 归一不得退化成前缀/包含匹配：相邻教室必须仍然是两个不同的键。
        assertNotEquals(normalizeRoomKey("06105D"), normalizeRoomKey("06106D"))
    }

    private fun occurrenceWithWeek(weekText: String): CourseOccurrence {
        return CourseOccurrence(
            id = "occ-$weekText",
            courseId = "course-$weekText",
            dayOfWeek = 1,
            startSection = 1,
            endSection = 2,
            weekText = weekText,
            note = ""
        )
    }
}
