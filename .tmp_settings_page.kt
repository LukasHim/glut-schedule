@Composable
private fun SettingsPage(
    showWeekend: Boolean,
    onShowWeekendChange: (Boolean) -> Unit,
    showNoon: Boolean = false,
    onShowNoonChange: (Boolean) -> Unit = {},
    /** 当前来源是否有午1/午2 两节；没有时「显示中午」置灰（南宁、研究生）。 */
    showNoonEnabled: Boolean = true,
    showGridLines: Boolean = false,
    onShowGridLinesChange: (Boolean) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColorEnabled: Boolean = false,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    showBackground: Boolean = false,
    onShowBackgroundChange: (Boolean) -> Unit = {},
    reminderEnabled: Boolean = false,
    reminderMinutes: Int = 10,
    notificationPermissionGranted: Boolean = false,
    onReminderEnabledChange: (Boolean) -> Unit = {},
    onReminderMinutesChange: (Int) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    customBackgroundUri: String = "",
    customBackgroundCrop: NormalizedCropRect? = null,
    backgroundDimAmount: Float = 0.5f,
    backgroundStore: ScheduleBackgroundStore,
    hasCustomBackground: Boolean = false,
    onPickBackground: () -> Unit = {},
    onRecropBackground: () -> Unit = {},
    onBackgroundDimAmountChange: (Float) -> Unit = {},
    onClearBackground: () -> Unit = {},
    onBuiltInBackgrounds: () -> Unit = {},
    onCourseColors: () -> Unit = {},
    classPeriodsEnabled: Boolean = true,
    onClassPeriods: () -> Unit = {},
    onHolidayAdjustments: () -> Unit = {},
    onReset: () -> Unit = {}
) {
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    var backgroundPreviewRevision by remember(customBackgroundUri, customBackgroundCrop) { mutableIntStateOf(0) }
    var previewDimAmount by remember(backgroundDimAmount) { mutableStateOf(backgroundDimAmount) }
    var previewReminderMinutes by remember(reminderMinutes) { mutableIntStateOf(reminderMinutes) }
    LaunchedEffect(customBackgroundUri, customBackgroundCrop) {
        if (
            shouldUseCustomBackground(customBackgroundUri) &&
            backgroundStore.get(
                customBackgroundUri,
                customBackgroundCrop,
                metrics.widthPixels,
                metrics.heightPixels
            ) == null
        ) {
            backgroundStore.preload(
                customBackgroundUri,
                customBackgroundCrop,
                metrics.widthPixels,
                metrics.heightPixels
            )
            backgroundPreviewRevision++
        }
    }
    val previewBitmap = remember(customBackgroundUri, customBackgroundCrop, backgroundPreviewRevision) {
        if (shouldUseCustomBackground(customBackgroundUri)) {
            backgroundStore.get(
                customBackgroundUri,
                customBackgroundCrop,
                metrics.widthPixels,
                metrics.heightPixels
            )
        } else null
    }
    val dynamicColorAvailable = isDynamicColorAvailable()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { SettingsSectionTitle("显示") }
        item {
            SettingsCard {
                SettingsSwitchRow(
                    title = "显示周末",
                    checked = showWeekend,
                    onCheckedChange = onShowWeekendChange
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = "显示中午",
                    supporting = if (showNoonEnabled) null else "当前来源没有中午节次",
                    checked = showNoon,
                    enabled = showNoonEnabled,
                    onCheckedChange = onShowNoonChange
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = "显示网格线",
                    checked = showGridLines,
                    onCheckedChange = onShowGridLinesChange
                )
            }
        }

        item { SettingsSectionTitle("主题") }
        item {
            SettingsCard {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "主题模式",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ThemeMode.SYSTEM to "跟随系统",
                            ThemeMode.LIGHT to "浅色",
                            ThemeMode.DARK to "深色"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
                SettingsDivider()
                SettingsSwitchRow(
                    title = "动态颜色",
                    supporting = if (dynamicColorAvailable) {
                        "使用系统壁纸取色（Android 12+）"
                    } else {
                        "需要 Android 12 及以上系统"
                    },
                    checked = dynamicColorEnabled,
                    enabled = dynamicColorAvailable,
                    onCheckedChange = onDynamicColorChange
                )
            }
        }

        item { SettingsSectionTitle("课表外观") }
        item {
            SettingsCard {
                SettingsSwitchRow(
                    title = "显示课表背景",
                    supporting = "关闭后使用普通页面主题背景",
                    checked = showBackground,
                    onCheckedChange = onShowBackgroundChange
                )
                if (showBackground) {
                    SettingsDivider()
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            ScheduleBackgroundImage(
                                modifier = Modifier.matchParentSize(),
                                customBackgroundUri = customBackgroundUri,
                                customBackgroundBitmap = previewBitmap,
                                dimAmount = previewDimAmount
                            )
                            Column(
                                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "背景效果预览",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "软件工程 · 08:00",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "背景蒙黑度",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(previewDimAmount * 100).roundToInt()}%",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Slider(
                            value = previewDimAmount,
                            onValueChange = { previewDimAmount = snapBackgroundDimAmount(it) },
                            onValueChangeFinished = { onBackgroundDimAmountChange(previewDimAmount) },
                            valueRange = 0f..0.8f,
                            steps = 15
                        )
                    }
                }
                SettingsDivider()
                SettingsClickRow(
                    title = "画廊",
                    onClick = onBuiltInBackgrounds
                )
                SettingsDivider()
                SettingsClickRow(
                    title = "自定义背景",
                    onClick = onPickBackground
                )
                if (hasCustomBackground) {
                    SettingsDivider()
                    SettingsClickRow(
                        title = "重新裁剪",
                        onClick = onRecropBackground
                    )
                    SettingsDivider()
                    SettingsClickRow(
                        title = "恢复默认背景",
                        destructive = true,
                        onClick = onClearBackground
                    )
                }
            }
        }

        item { SettingsSectionTitle("上课提醒") }
        item {
            SettingsCard {
                SettingsSwitchRow(
                    title = "上课提醒",
                    supporting = "在课程开始前发送系统通知",
                    checked = reminderEnabled,
                    onCheckedChange = onReminderEnabledChange
                )
                if (reminderEnabled) {
                    SettingsDivider()
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "提前提醒时间",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "$previewReminderMinutes 分钟前",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Slider(
                            value = previewReminderMinutes.toFloat(),
                            onValueChange = { previewReminderMinutes = it.roundToInt() },
                            onValueChangeFinished = {
                                onReminderMinutesChange(previewReminderMinutes)
                            },
                            valueRange = 5f..30f,
                            steps = 4
                        )
                        if (!notificationPermissionGranted) {
                            Text(
                                "通知权限未授予，提醒可能不会显示。",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = onRequestNotificationPermission) {
                                Text("授予通知权限")
                            }
                        }
                    }
                }
            }
        }

        item { SettingsSectionTitle("课程") }
        item {
            SettingsCard {
                SettingsClickRow(
                    title = "课程卡片颜色",
                    onClick = onCourseColors
                )
                SettingsDivider()
                SettingsClickRow(
                    title = "上课时间",
                    enabled = classPeriodsEnabled,
                    onClick = onClassPeriods
                )
                SettingsDivider()
                SettingsClickRow(
                    title = "调休调课",
                    onClick = onHolidayAdjustments
                )
            }
        }

        item { SettingsSectionTitle("数据管理") }
        item {
            SettingsCard {
                SettingsClickRow(
                    title = "重置应用",
                    supporting = "恢复初次使用状态",
                    destructive = true,
                    onClick = onReset
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large
    ) {
        Column(content = { content() })
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 16.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    supporting: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                title,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        },
        supportingContent = supporting?.let {
            {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }
    )
}

@Composable
private fun SettingsClickRow(
    title: String,
    supporting: String? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                title,
                color = when {
                    destructive -> MaterialTheme.colorScheme.error
                    enabled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        },
        supportingContent = supporting?.let {
            {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        trailingContent = {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                },
                modifier = Modifier.size(20.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    )
}
