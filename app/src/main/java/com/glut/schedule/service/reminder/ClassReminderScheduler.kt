package com.glut.schedule.service.reminder

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.glut.schedule.ScheduleApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * 上课提醒的排程器。
 *
 * 每次只排**下一次**提醒：触发后 Worker 会重新读取课表并排下一节。这样课程被
 * 重新导入、调休规则变化、提醒时间调整时，只需要 REPLACE 掉唯一的一条任务，
 * 不会在任务队列里留下大量已经过期的提醒。
 *
 * 进程被杀、设备重启后 WorkManager 会自行恢复这条一次性任务；应用升级后同理。
 * 因此不需要额外的 BOOT_COMPLETED 接收器。
 */
class ClassReminderScheduler(private val context: Context) {
    private val mutex = Mutex()
    private val statePreferences =
        context.getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE)

    suspend fun refresh(now: LocalDateTime = LocalDateTime.now()) {
        mutex.withLock {
            val container = (context.applicationContext as? ScheduleApplication)
                ?.appContainer
                ?: return
            val settings = container.settingsStore
            try {
                if (!settings.classReminderEnabled.first()) {
                    cancel()
                    clearRemembered()
                    return
                }
                val minutes = settings.classReminderMinutes.first()
                val semesterId = container.scheduleRepository.currentSemester.first()?.id.orEmpty()
                val manualDayCopies = settings.manualDayCopies.first()[semesterId].orEmpty()
                val reminders = ClassReminderPlanner.remindersFor(
                    now = now,
                    courses = container.scheduleRepository.currentCourses.first(),
                    classPeriods = container.scheduleRepository.currentClassPeriods.first(),
                    semesterStartMonday = settings.semesterStartMonday.first(),
                    semesterEndDate = settings.semesterEndDate.first(),
                    manualDayCopies = manualDayCopies,
                    minutesBefore = minutes
                )
                val remembered = rememberedKeys()
                reminders
                    .filter { reminder ->
                        !reminder.triggerAt.isAfter(now) &&
                            reminder.startAt.isAfter(now) &&
                            reminder.key !in remembered
                    }
                    .forEach { reminder ->
                        // 权限被拒绝时不写去重标记：用户打开权限后，下一轮仍能补上提醒。
                        if (ClassReminderNotifier.notify(context, reminder, minutes)) {
                            remember(reminder.key)
                        }
                    }
                pruneRemembered(now)
                val nextTrigger = reminders.firstOrNull { it.triggerAt.isAfter(now) }?.triggerAt
                enqueue(nextTrigger ?: nextMorning(now), now)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                // 读取课表失败不应该让提醒链彻底断掉：先排到次日零点再重试。
                Log.w(TAG, "上课提醒排程失败，改为次日重试", error)
                enqueue(nextMorning(now), now)
            }
        }
    }

    fun cancel() {
        runCatching { WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME) }
    }

    private fun enqueue(triggerAt: LocalDateTime, now: LocalDateTime) {
        val delayMillis = Duration.between(now, triggerAt).toMillis().coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ClassReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private fun nextMorning(now: LocalDateTime): LocalDateTime =
        LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.of(0, 5))

    private fun rememberedKeys(): Set<String> =
        statePreferences.getStringSet(KEY_REMEMBERED, emptySet()).orEmpty()

    private fun remember(key: String) {
        val updated = rememberedKeys().toMutableSet().apply { add(key) }
        statePreferences.edit().putStringSet(KEY_REMEMBERED, updated).apply()
    }

    private fun clearRemembered() {
        statePreferences.edit().remove(KEY_REMEMBERED).apply()
    }

    /** 只保留今天与明天的去重键，避免这组字符串随着学期无限增长。 */
    private fun pruneRemembered(now: LocalDateTime) {
        val validDates = setOf(
            now.toLocalDate().toString(),
            now.toLocalDate().plusDays(1).toString()
        )
        val pruned = rememberedKeys().filterTo(mutableSetOf()) { key ->
            key.substringBefore('|') in validDates
        }
        statePreferences.edit().putStringSet(KEY_REMEMBERED, pruned).apply()
    }

    companion object {
        const val WORK_NAME = "class-reminder-next"
        const val WORK_TAG = "class-reminder"
        private const val STATE_PREFERENCES = "class_reminder_state"
        private const val KEY_REMEMBERED = "remembered"
        private const val TAG = "ClassReminder"
    }
}
