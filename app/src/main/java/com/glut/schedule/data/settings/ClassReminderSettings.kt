package com.glut.schedule.data.settings

/** 默认提前 10 分钟提醒。 */
const val DEFAULT_REMINDER_MINUTES: Int = 10

/** 持久化前的边界收敛：过小会让通知来不及，过大则等于没有提醒。 */
fun sanitizeReminderMinutes(value: Int): Int = value.coerceIn(1, 120)
