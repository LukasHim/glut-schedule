package com.glut.schedule.service.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.glut.schedule.ScheduleApplication
import kotlinx.coroutines.CancellationException

/**
 * 单次提醒检查。
 *
 * 不直接携带「该提醒哪门课」：任务执行时可能已经跨过导入/调休导致的课表变化，
 * 重新规划一次才能保证通知内容与当前课表一致。
 */
class ClassReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? ScheduleApplication ?: return Result.success()
        return try {
            application.appContainer.classReminderScheduler.refresh()
            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // 数据源短暂不可用时重试一次；仍然失败就交给下一次排程，不阻塞队列。
            if (runAttemptCount < 1) Result.retry() else Result.success()
        }
    }
}
