package com.glut.schedule.data.settings

/**
 * 应用主题模式。
 *
 * 持久化只存稳定的 [storageValue]，与枚举名解耦：以后重命名枚举不会让老用户
 * 的设置回退到「跟随系统」。
 */
enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageValue(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}
