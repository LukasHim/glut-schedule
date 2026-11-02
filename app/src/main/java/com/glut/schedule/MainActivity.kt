package com.glut.schedule

import android.animation.ValueAnimator
import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glut.schedule.data.model.CourseColorMapper
import com.glut.schedule.data.model.ScheduleCourse
import com.glut.schedule.service.UpdateChecker
import com.glut.schedule.service.UpdateInfo
import com.glut.schedule.ui.navigation.DrawerItem
import com.glut.schedule.ui.navigation.campusDrawerItems
import com.glut.schedule.ui.navigation.isEnabledFor
import com.glut.schedule.ui.navigation.otherDrawerItems
import com.glut.schedule.ui.navigation.prepareDrawerSelection
import com.glut.schedule.ui.navigation.unsupportedHintFor
import com.glut.schedule.ui.pages.AboutScreen
import com.glut.schedule.ui.pages.FaqScreen
import com.glut.schedule.ui.pages.CourseTimeStatsScreen
import com.glut.schedule.ui.pages.CourseTimeStatsViewModel
import com.glut.schedule.ui.pages.CourseTimeStatsViewModelFactory
import com.glut.schedule.ui.pages.SemesterOverviewScreen
import com.glut.schedule.ui.pages.SemesterOverviewViewModel
import com.glut.schedule.ui.pages.SemesterOverviewViewModelFactory
import com.glut.schedule.ui.pages.DirectLoginScreen
import com.glut.schedule.ui.pages.DirectLoginViewModel
import com.glut.schedule.ui.pages.DirectLoginViewModelFactory
import com.glut.schedule.ui.pages.ExamScreen
import com.glut.schedule.ui.pages.ExamViewModel
import com.glut.schedule.ui.pages.ExamViewModelFactory
import com.glut.schedule.ui.pages.FitnessScoreScreen
import com.glut.schedule.ui.pages.FitnessScoreViewModel
import com.glut.schedule.ui.pages.FitnessScoreViewModelFactory
import com.glut.schedule.ui.pages.FinanceScreen
import com.glut.schedule.ui.pages.FinanceViewModel
import com.glut.schedule.ui.pages.FinanceViewModelFactory
import com.glut.schedule.ui.pages.FinanceViewModelRegistry
import com.glut.schedule.ui.pages.CampusImageScreen
import com.glut.schedule.ui.pages.CampusImageViewModel
import com.glut.schedule.ui.pages.CampusImageViewModelFactory
import com.glut.schedule.ui.pages.ScheduleScreen
import com.glut.schedule.partner.PartnerScheduleScreen
import com.glut.schedule.partner.PartnerScheduleViewModel
import com.glut.schedule.partner.PartnerScheduleViewModelFactory
import com.glut.schedule.ui.pages.ClassPeriodSettingsScreen
import com.glut.schedule.ui.pages.ScheduleViewModel
import com.glut.schedule.ui.pages.ScheduleViewModelFactory
import com.glut.schedule.ui.components.AdvancedColorSheet
import com.glut.schedule.ui.components.CourseColorPaletteSection
import com.glut.schedule.ui.theme.DEFAULT_PARTNER_THEME_SEED_HEX
import com.glut.schedule.ui.theme.DEFAULT_THEME_SEED_HEX
import com.glut.schedule.ui.theme.SeedColorPickerDialog
import com.glut.schedule.ui.theme.parseSeedColor
import com.glut.schedule.ui.theme.seedColorScheme
import com.glut.schedule.ui.theme.toSeedHex
import com.glut.schedule.ui.pages.ScoreDisplayPolicy
import com.glut.schedule.ui.pages.ScoreScreen
import com.glut.schedule.ui.pages.ScoreViewModel
import com.glut.schedule.ui.pages.ScoreViewModelFactory
import com.glut.schedule.ui.pages.ProfessionalScoreScreen
import com.glut.schedule.ui.pages.ProfessionalScoreViewModel
import com.glut.schedule.ui.pages.ProfessionalScoreViewModelFactory
import com.glut.schedule.ui.pages.GradeExamScreen
import com.glut.schedule.ui.pages.HolidayAdjustmentsScreen
import com.glut.schedule.ui.pages.HolidayAdjustmentsViewModel
import com.glut.schedule.ui.pages.HolidayAdjustmentsViewModelFactory
import com.glut.schedule.ui.pages.GradeExamViewModel
import com.glut.schedule.ui.pages.GradeExamViewModelFactory
import com.glut.schedule.ui.pages.StudyPlanScreen
import com.glut.schedule.ui.pages.StudyPlanViewModel
import com.glut.schedule.ui.pages.StudyPlanViewModelFactory
import com.glut.schedule.ui.theme.GlutScheduleTheme
import com.glut.schedule.data.settings.CampusType
import com.glut.schedule.data.settings.ThemeMode
import com.glut.schedule.data.settings.capabilitiesFor
import com.glut.schedule.service.reminder.ClassReminderNotifier
import com.glut.schedule.ui.theme.isDynamicColorAvailable
import com.glut.schedule.ui.theme.resolveDarkTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

private enum class SettingsSubPage(val title: String) {
    ROOT("设置"),
    COURSE_COLORS("课程卡片颜色"),
    CLASS_PERIODS("上课时间"),
    HOLIDAY_ADJUSTMENTS("调休调课")
}

class MainActivity : ComponentActivity() {
    private val firstFrameReady = AtomicBoolean(false)
    private var firstFramePreDrawListener: ViewTreeObserver.OnPreDrawListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15 会默认启用 edge-to-edge，旧系统需要显式关闭 decor fitting，
        // 才能让自定义背景铺到透明状态栏；各页面已有的系统栏 inset 继续保护交互控件。
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        val container = (application as ScheduleApplication).appContainer
        holdLaunchWindowUntilFirstFrameIsReady()

        setContent {
            val themeMode by container.settingsStore.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val dynamicColorEnabled by container.settingsStore.dynamicColorEnabled
                .collectAsStateWithLifecycle(initialValue = false)
            val themeSeed by container.settingsStore.themeSeed
                .collectAsStateWithLifecycle(initialValue = "")
            val darkTheme = resolveDarkTheme(themeMode)
            @OptIn(ExperimentalMaterial3Api::class)
            GlutScheduleTheme(
                themeMode = themeMode,
                dynamicColorEnabled = dynamicColorEnabled,
                themeSeedHex = themeSeed
            ) {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var selectedItem by remember { mutableStateOf(DrawerItem.Schedule) }
                var showUpdateDialog by remember { mutableStateOf<UpdateDialogState?>(null) }
                var autoPopupUpdateVersion by remember { mutableStateOf<String?>(null) }
                var showResetConfirm by remember { mutableStateOf(false) }
                var settingsSubPage by remember { mutableStateOf(SettingsSubPage.ROOT) }
                var drawerGestureBlocked by remember { mutableStateOf(false) }
                var notificationPermissionGranted by remember {
                    mutableStateOf(ClassReminderNotifier.canPostNotifications(this@MainActivity))
                }
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    notificationPermissionGranted = granted
                    scope.launch { container.classReminderScheduler.refresh() }
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    notificationPermissionGranted =
                        ClassReminderNotifier.canPostNotifications(this@MainActivity)
                }

                // 返回键：先关抽屉 → 再回到课表主页 → 最后退出
                BackHandler(enabled = true) {
                    when {
                        drawerState.isOpen -> scope.launch { drawerState.close() }
                        settingsSubPage != SettingsSubPage.ROOT -> settingsSubPage = SettingsSubPage.ROOT
                        selectedItem != DrawerItem.Schedule -> selectedItem = DrawerItem.Schedule
                        else -> finish()
                    }
                }

                val scheduleViewModel: ScheduleViewModel = viewModel(
                    factory = ScheduleViewModelFactory(
                        repository = container.scheduleRepository,
                        settingsStore = container.settingsStore,
                        sessionStore = container.academicSessionStore,
                        loginService = container.academicLoginService,
                        semesterImportService = container.academicSemesterImportService,
                        apiProbeService = container.apiProbeService,
                        timorHolidayClient = container.timorHolidayClient
                    )
                )
                val examViewModel: ExamViewModel = viewModel(
                    factory = ExamViewModelFactory(
                        repository = container.scheduleRepository,
                        sessionStore = container.academicSessionStore,
                        examService = container.academicExamService,
                        loginService = container.academicLoginService
                    )
                )
                val scoreViewModel: ScoreViewModel = viewModel(
                    factory = ScoreViewModelFactory(
                        repository = container.scheduleRepository,
                        sessionStore = container.academicSessionStore,
                        loginService = container.academicLoginService,
                        scoreParser = container.scoreParser,
                        // 刷新时才读来源：ViewModel 是 Activity 级缓存实例，构造时还不知道会导入哪个来源。
                        campusTypeFlow = container.settingsStore.campusType
                    )
                )
                val professionalScoreViewModel: ProfessionalScoreViewModel = viewModel(
                    factory = ProfessionalScoreViewModelFactory(
                        repository = container.scheduleRepository,
                        settingsStore = container.settingsStore,
                        sessionStore = container.academicSessionStore,
                        loginService = container.academicLoginService,
                        scoreParser = container.scoreParser,
                        studyPlanParser = container.studyPlanParser
                    )
                )
                val gradeExamViewModel: GradeExamViewModel = viewModel(
                    factory = GradeExamViewModelFactory(
                        repository = container.scheduleRepository,
                        sessionStore = container.academicSessionStore,
                        loginService = container.academicLoginService,
                        gradeExamParser = container.gradeExamParser
                    )
                )
                val studyPlanViewModel: StudyPlanViewModel = viewModel(
                    factory = StudyPlanViewModelFactory(
                        repository = container.scheduleRepository,
                        sessionStore = container.academicSessionStore,
                        loginService = container.academicLoginService,
                        settingsStore = container.settingsStore,
                        studyPlanParser = container.studyPlanParser
                    )
                )
                val semesterOverviewViewModel: SemesterOverviewViewModel = viewModel(
                    factory = SemesterOverviewViewModelFactory(
                        repository = container.scheduleRepository,
                        settingsStore = container.settingsStore,
                        sessionStore = container.academicSessionStore,
                        scheduleParser = container.academicScheduleParser,
                        loginService = container.academicLoginService,
                        timorHolidayClient = container.timorHolidayClient
                    )
                )
                val courseTimeStatsViewModel: CourseTimeStatsViewModel = viewModel(
                    factory = CourseTimeStatsViewModelFactory(
                        sourceFlow = container.scheduleRepository.courseTimeSemesterSources
                    )
                )
                val holidayAdjustmentsViewModel: HolidayAdjustmentsViewModel = viewModel(
                    factory = HolidayAdjustmentsViewModelFactory(
                        repository = container.scheduleRepository,
                        settingsStore = container.settingsStore
                    )
                )
                val directLoginViewModel: DirectLoginViewModel = viewModel(
                    factory = DirectLoginViewModelFactory(
                        loginService = container.academicLoginService,
                        sessionStore = container.academicSessionStore,
                        credentialStore = container.credentialStore,
                        scheduleRepository = container.scheduleRepository,
                        settingsStore = container.settingsStore,
                        apiProbeService = container.apiProbeService,
                        academicExamService = container.academicExamService,
                        scoreParser = container.scoreParser,
                        timorHolidayClient = container.timorHolidayClient,
                        gradeExamParser = container.gradeExamParser,
                        studyPlanParser = container.studyPlanParser,
                        semesterImportService = container.academicSemesterImportService,
                        semesterBulkDownloadCoordinator = container.semesterBulkDownloadCoordinator
                    )
                )
                LaunchedEffect(container.semesterBulkDownloadCoordinator) {
                    container.semesterBulkDownloadCoordinator.completionEvents.collect { summary ->
                        val succeeded = summary.items.count {
                            it.status == com.glut.schedule.service.academic.SemesterDownloadItemStatus.SUCCEEDED
                        }
                        val failed = summary.items.size - succeeded
                        val skippedRows = summary.items.sumOf { it.skippedRowCount }
                        val text = if (summary.mode == com.glut.schedule.service.academic.SemesterDownloadMode.SINGLE) {
                            if (failed == 0) "学期课表已下载" else "学期课表下载失败"
                        } else if (failed == 0) {
                            "历史学期下载完成"
                        } else {
                            "下载完成：成功 $succeeded，失败 $failed"
                        }
                        val message = if (skippedRows > 0) "$text；已跳过 $skippedRows 条异常课程记录" else text
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            message,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                val fitnessScoreViewModel: FitnessScoreViewModel = viewModel(
                    factory = FitnessScoreViewModelFactory(
                        service = container.fitnessApiService,
                        store = container.fitnessStore,
                        parser = container.fitnessParser
                    )
                )
                val partnerScheduleViewModel: PartnerScheduleViewModel = viewModel(
                    factory = PartnerScheduleViewModelFactory(
                        repository = container.scheduleRepository,
                        settingsStore = container.settingsStore,
                        storage = container.partnerScheduleStore,
                        gateway = container.partnerScheduleApiService
                    )
                )
                val campusType by container.settingsStore.campusType.collectAsStateWithLifecycle(initialValue = CampusType.GUILIN)
                val partnerThemeSeed by container.settingsStore.partnerThemeSeed
                    .collectAsStateWithLifecycle(initialValue = "")
                val reminderEnabled by container.settingsStore.classReminderEnabled
                    .collectAsStateWithLifecycle(initialValue = false)
                val reminderMinutes by container.settingsStore.classReminderMinutes
                    .collectAsStateWithLifecycle(initialValue = 10)
                val campusInfoViewModel: CampusImageViewModel? =
                    if (selectedItem == DrawerItem.CampusInfo) {
                        viewModel(
                            key = "campus-info",
                            factory = CampusImageViewModelFactory(container.campusImageService)
                        )
                    } else null
                val financeViewModels = remember { FinanceViewModelRegistry() }
                val financeViewModel: FinanceViewModel? = if (selectedItem == DrawerItem.Finance) {
                    financeViewModels.register(viewModel<FinanceViewModel>(
                        key = "finance-${campusType.name}",
                        factory = FinanceViewModelFactory(
                            gateway = container.financeApiService,
                            store = container.financeStore,
                            campus = campusType
                        )
                    ))
                } else null

                LaunchedEffect(campusType) {
                    // 切换到新来源后，当前停留的页面可能在新来源下已经不可用
                    // （研究生的考试/专业成绩/考级/体测/校园信息/财务/情侣课表/课时统计都是禁用项）。
                    // 菜单虽然置灰了，但 selectedItem 仍是旧值，页面会继续渲染并对新来源发请求；
                    // 这里统一把用户送回课表，避免「禁用菜单仍在后台跑」的漏洞。
                    if (selectedItem != DrawerItem.Schedule && !selectedItem.isEnabledFor(campusType)) {
                        selectedItem = DrawerItem.Schedule
                    }
                }

                val isSchedulePage =
                    selectedItem == DrawerItem.Schedule || selectedItem == DrawerItem.PartnerSchedule

                DisposableEffect(darkTheme) {
                    applySystemBarStyle(darkTheme = darkTheme)
                    onDispose { }
                }

                // Red dot persists until user actually updates to the latest version
                val updateAvailableVersion by container.settingsStore.updateAvailableVersion.collectAsStateWithLifecycle(initialValue = "")
                val showUpdateDot = updateAvailableVersion.isNotBlank()
                    && UpdateChecker.compareVersions(updateAvailableVersion, BuildConfig.VERSION_NAME) > 0

                LaunchedEffect(Unit) {
                    val info = container.updateChecker.check(BuildConfig.VERSION_NAME)
                    if (info != null && info.isNewer) {
                        container.settingsStore.setUpdateAvailable(info.latestVersion)
                        val dismissedVersion = container.settingsStore.dismissedUpdatePopupVersion.first()
                        if (info.popup && dismissedVersion != info.latestVersion) {
                            autoPopupUpdateVersion = info.latestVersion
                            showUpdateDialog = UpdateDialogState.Idle(info)
                        }
                    }

                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = !drawerGestureBlocked,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.fillMaxWidth(0.75f),
                            drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            drawerContentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            DrawerHeader()
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                                    item { DrawerSectionLabel("常用") }
                                    items(listOf(DrawerItem.Schedule, DrawerItem.Exam, DrawerItem.StudyPlan, DrawerItem.SemesterOverview, DrawerItem.Import)) { item ->
                                        DrawerMenuItem(
                                            item = item,
                                            isSelected = selectedItem == item,
                                            enabled = item.isEnabledFor(campusType),
                                            unsupportedHint = item.unsupportedHintFor(campusType),
                                            onClick = {
                                                selectedItem = item
                                                settingsSubPage = SettingsSubPage.ROOT
                                                scope.launch { drawerState.close() }
                                            }
                                        )
                                    }
                                    item { DrawerSectionLabel("成绩") }
                                    items(listOf(DrawerItem.Score, DrawerItem.ProfessionalScore, DrawerItem.GradeExam, DrawerItem.FitnessScore)) { item ->
                                        DrawerMenuItem(
                                            item = item,
                                            isSelected = selectedItem == item,
                                            enabled = item.isEnabledFor(campusType),
                                            unsupportedHint = item.unsupportedHintFor(campusType),
                                            onClick = {
                                                selectedItem = prepareDrawerSelection(
                                                    current = selectedItem,
                                                    target = item,
                                                    onProfessionalScoreEntered = professionalScoreViewModel::resetAcademicYearSelection
                                                )
                                                settingsSubPage = SettingsSubPage.ROOT
                                                scope.launch { drawerState.close() }
                                            }
                                        )
                                    }
                                    item { DrawerSectionLabel("校园") }
                                    items(campusDrawerItems(campusType)) { item ->
                                        DrawerMenuItem(
                                            item = item,
                                            isSelected = selectedItem == item,
                                            enabled = item.isEnabledFor(campusType),
                                            unsupportedHint = item.unsupportedHintFor(campusType),
                                            onClick = {
                                                selectedItem = item
                                                settingsSubPage = SettingsSubPage.ROOT
                                                scope.launch { drawerState.close() }
                                            }
                                        )
                                    }
                                    item { DrawerSectionLabel("其他") }
                                    items(otherDrawerItems) { item ->
                                        DrawerMenuItem(
                                            item = item,
                                            isSelected = selectedItem == item,
                                            showDot = item == DrawerItem.About && showUpdateDot,
                                            onClick = {
                                                selectedItem = item
                                                settingsSubPage = SettingsSubPage.ROOT
                                                scope.launch { drawerState.close() }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            // 课程表页面不显示单独的 TopAppBar，菜单按钮集成在 ScheduleHeader 中
                            if (!isSchedulePage) {
                                TopAppBar(
                                    title = {
                                        Text(
                                            if (selectedItem == DrawerItem.Settings) settingsSubPage.title else selectedItem.title,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = {
                                            if (settingsSubPage != SettingsSubPage.ROOT) {
                                                settingsSubPage = SettingsSubPage.ROOT
                                            }
                                            else scope.launch { drawerState.open() }
                                        }) {
                                            Icon(
                                                imageVector = if (settingsSubPage != SettingsSubPage.ROOT) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.Menu,
                                                contentDescription = if (settingsSubPage != SettingsSubPage.ROOT) "返回" else "菜单",
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    },
                                    actions = {
                                        when (selectedItem) {
                                            DrawerItem.Exam -> {
                                                val examState by examViewModel.uiState.collectAsStateWithLifecycle()
                                                IconButton(
                                                    onClick = examViewModel::refreshExams,
                                                    enabled = !examState.isRefreshing
                                                ) {
                                                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                                                }
                                            }
                                            DrawerItem.Score -> {
                                                val scoreState by scoreViewModel.uiState.collectAsStateWithLifecycle()
                                                IconButton(
                                                    onClick = scoreViewModel::refreshScores,
                                                    enabled = !scoreState.isRefreshing
                                                ) {
                                                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                                                }
                                            }
                                            DrawerItem.ProfessionalScore -> {
                                                val professionalScoreState by professionalScoreViewModel.uiState.collectAsStateWithLifecycle()
                                                IconButton(
                                                    onClick = professionalScoreViewModel::refreshData,
                                                    enabled = !professionalScoreState.isRefreshing
                                                ) {
                                                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                                                }
                                            }
                                            DrawerItem.GradeExam -> {
                                                val gradeExamState by gradeExamViewModel.uiState.collectAsStateWithLifecycle()
                                                IconButton(
                                                    onClick = gradeExamViewModel::refresh,
                                                    enabled = !gradeExamState.isRefreshing
                                                ) {
                                                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                                                }
                                            }
                                            DrawerItem.FitnessScore -> {
                                                val fitnessState by fitnessScoreViewModel.uiState.collectAsStateWithLifecycle()
                                                IconButton(
                                                    onClick = fitnessScoreViewModel::refresh,
                                                    enabled = !fitnessState.isRefreshing
                                                ) {
                                                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                                                }
                                            }
                                            DrawerItem.CampusInfo -> campusInfoViewModel?.let { viewModel ->
                                                val campusImageState by viewModel.uiState.collectAsStateWithLifecycle()
                                                if (campusImageState.selectedType.isRemote) {
                                                    IconButton(
                                                        onClick = viewModel::refreshCurrent,
                                                        enabled = !campusImageState.isLoading
                                                    ) {
                                                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新校园信息")
                                                    }
                                                }
                                            }
                                            DrawerItem.Finance -> {
                                                financeViewModel?.let { viewModel ->
                                                    val financeState by viewModel.uiState.collectAsStateWithLifecycle()
                                                    IconButton(onClick = viewModel::toggleMoneyVisibility) {
                                                        Icon(
                                                            if (financeState.moneyVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                                            contentDescription = if (financeState.moneyVisible) "隐藏金额" else "显示金额"
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = viewModel::refresh,
                                                        enabled = !financeState.isRefreshing && !financeState.campusUnsupported
                                                    ) {
                                                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                                                    }
                                                }
                                            }
                                            DrawerItem.StudyPlan -> {
                                                val studyPlanState by studyPlanViewModel.uiState.collectAsStateWithLifecycle()
                                                IconButton(
                                                    onClick = studyPlanViewModel::refresh,
                                                    enabled = !studyPlanState.isRefreshing
                                                ) {
                                                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                                                }
                                            }
                                            else -> {}
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        },
                        // 课程表页面不消耗系统栏空间，让页面背景铺满全屏
                        contentWindowInsets = if (isSchedulePage) {
                            WindowInsets(0)
                        } else {
                            ScaffoldDefaults.contentWindowInsets
                        },
                        containerColor = MaterialTheme.colorScheme.background
                    ) { padding ->
                        Box(modifier = Modifier.padding(padding)) {
                            when (selectedItem) {
                                DrawerItem.Schedule -> ScheduleDestination(
                                    viewModel = scheduleViewModel,
                                    onFirstFrameReady = ::releaseFirstFrame,
                                    onImportClick = { selectedItem = DrawerItem.Import },
                                    onExamClick = { selectedItem = DrawerItem.Exam },
                                    onDrawerOpen = { scope.launch { drawerState.open() } }
                                )
                                DrawerItem.PartnerSchedule -> PartnerScheduleDestination(
                                    viewModel = partnerScheduleViewModel,
                                    themeSeedHex = partnerThemeSeed,
                                    darkTheme = darkTheme,
                                    onThemeSeedChange = { hex ->
                                        scope.launch {
                                            container.settingsStore.setPartnerThemeSeed(hex)
                                        }
                                    },
                                    onDrawerOpen = { scope.launch { drawerState.open() } }
                                )
                                DrawerItem.Score -> ScoreScreen(
                                    viewModel = scoreViewModel,
                                    // 研究生：第 1 学期是秋季，且门户不给绩点——两者都必须按来源切换。
                                    policy = ScoreDisplayPolicy.of(campusType)
                                )
                                DrawerItem.ProfessionalScore -> ProfessionalScoreScreen(viewModel = professionalScoreViewModel)
                                DrawerItem.GradeExam -> GradeExamScreen(viewModel = gradeExamViewModel)
                                DrawerItem.FitnessScore -> FitnessScoreScreen(
                                    viewModel = fitnessScoreViewModel,
                                    onTableGestureActive = { drawerGestureBlocked = it }
                                )
                                DrawerItem.CampusInfo -> campusInfoViewModel?.let {
                                    CampusImageScreen(
                                        viewModel = it,
                                        onImageGestureActive = { active -> drawerGestureBlocked = active }
                                    )
                                }
                                DrawerItem.CourseTimeStats -> CourseTimeStatsScreen(
                                    viewModel = courseTimeStatsViewModel,
                                    onImportClick = { selectedItem = DrawerItem.Import }
                                )
                                DrawerItem.Finance -> financeViewModel?.let {
                                    FinanceScreen(
                                        viewModel = it,
                                        onTableGestureActive = { drawerGestureBlocked = it }
                                    )
                                }
                                DrawerItem.StudyPlan -> StudyPlanScreen(viewModel = studyPlanViewModel)
                                DrawerItem.Exam -> ExamScreen(
                                    viewModel = examViewModel,
                                    onBack = { selectedItem = DrawerItem.Schedule }
                                )
                                DrawerItem.Import -> DirectLoginScreen(
                                    viewModel = directLoginViewModel,
                                    // 导入完成后回到课表页。抽屉此时可能还开着（用户从抽屉进来，
                                    // 没关抽屉就点了按钮），不关会出现「内容已经变了、抽屉还挡着」。
                                    onDone = {
                                        selectedItem = DrawerItem.Schedule
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                DrawerItem.Settings -> ScheduleSettingsDestination(
                                    viewModel = scheduleViewModel,
                                    holidayAdjustmentsViewModel = holidayAdjustmentsViewModel,
                                    subPage = settingsSubPage,
                                    themeMode = themeMode,
                                    dynamicColorEnabled = dynamicColorEnabled,
                                    themeSeedHex = themeSeed,
                                    onThemeModeChange = { mode ->
                                        scope.launch { container.settingsStore.setThemeMode(mode) }
                                    },
                                    onDynamicColorChange = { enabled ->
                                        scope.launch {
                                            container.settingsStore.setDynamicColorEnabled(enabled)
                                        }
                                    },
                                    onThemeSeedChange = { hex ->
                                        scope.launch { container.settingsStore.setThemeSeed(hex) }
                                    },
                                    reminderEnabled = reminderEnabled,
                                    reminderMinutes = reminderMinutes,
                                    notificationPermissionGranted = notificationPermissionGranted,
                                    onReminderEnabledChange = { enabled ->
                                        scope.launch {
                                            container.settingsStore.setClassReminderEnabled(enabled)
                                        }
                                        if (enabled &&
                                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !notificationPermissionGranted
                                        ) {
                                            notificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        }
                                    },
                                    onReminderMinutesChange = { minutes ->
                                        scope.launch {
                                            container.settingsStore.setClassReminderMinutes(minutes)
                                        }
                                    },
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        }
                                    },
                                    onSubPageChange = { settingsSubPage = it },
                                    onReset = { showResetConfirm = true }
                                )
                                DrawerItem.SemesterOverview -> SemesterOverviewScreen(viewModel = semesterOverviewViewModel)
                                DrawerItem.FAQ -> FaqScreen()
                                DrawerItem.About -> {
                                    val shareContext = LocalContext.current
                                    AboutScreen(
                                        updateChecker = container.updateChecker,
                                        updateAvailableVersion = updateAvailableVersion,
                                        onShowUpdateDialog = { info ->
                                            showUpdateDialog = UpdateDialogState.Idle(info)
                                        },
                                        onShare = { shareApk(shareContext) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Update dialog
                showUpdateDialog?.let { state ->
                    UpdateDialog(
                        state = state,
                        appUpdater = container.appUpdater,
                        onDismiss = {
                            autoPopupUpdateVersion?.let { version ->
                                scope.launch {
                                    container.settingsStore.markUpdatePopupDismissed(version)
                                }
                            }
                            autoPopupUpdateVersion = null
                            showUpdateDialog = null
                        },
                        onStateChange = { showUpdateDialog = it }
                    )
                }

                if (showResetConfirm) {
                    AlertDialog(
                        onDismissRequest = { showResetConfirm = false },
                        title = { Text("重置应用") },
                        text = { Text("将清除所有数据（课表、成绩、考试、教学计划、设置、登录凭证），恢复到初次使用状态。此操作不可撤销，确定继续吗？") },
                        confirmButton = {
                            TextButton(onClick = {
                                    showResetConfirm = false
                                    scope.launch {
                                        resetApplicationDataSafely(
                                            cancelActiveDownloads = {
                                                container.semesterBulkDownloadCoordinator
                                                    .cancelForAccountChange()
                                            },
                                            clearData = {
                                                directLoginViewModel.clearLoginState()
                                                fitnessScoreViewModel.clearData()
                                                financeViewModels.clearAll()
                                                container.financeStore.clearAll()
                                                container.scheduleRepository.clearAllData()
                                                container.settingsStore.clearAll()
                                                container.academicSessionStore.clearAll()
                                                container.credentialStore.clearCredentials()
                                            }
                                        )
                                    }
                                }
                            ) {
                                Text("确认重置")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetConfirm = false }) {
                                Text("取消")
                            }
                        }
                    )
                }
            }
        }
    }

    /**
     * 冷启动时继续保留系统启动窗口，直到 DataStore 已恢复且目标背景真正可绘制。
     * 这样首个应用帧不会先露出默认画作，再异步切换为用户选择的画廊背景。
     */
    private fun holdLaunchWindowUntilFirstFrameIsReady() {
        val decorView = window.decorView
        val listener = ViewTreeObserver.OnPreDrawListener { firstFrameReady.get() }
        firstFramePreDrawListener = listener
        decorView.viewTreeObserver.addOnPreDrawListener(listener)
    }

    private fun releaseFirstFrame() {
        if (!firstFrameReady.compareAndSet(false, true)) return
        removeFirstFramePreDrawListener()
        window.decorView.postInvalidateOnAnimation()
    }

    private fun removeFirstFramePreDrawListener() {
        val listener = firstFramePreDrawListener ?: return
        val observer = window.decorView.viewTreeObserver
        if (observer.isAlive) observer.removeOnPreDrawListener(listener)
        firstFramePreDrawListener = null
    }

    override fun onDestroy() {
        removeFirstFramePreDrawListener()
        super.onDestroy()
    }

    private fun applySystemBarStyle(darkTheme: Boolean) {
        window.statusBarColor = AndroidColor.TRANSPARENT
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        // 页面背景跟随应用主题，状态栏图标随之切换明暗。
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
        window.navigationBarColor = if (darkTheme) {
            AndroidColor.rgb(0x15, 0x13, 0x0C)
        } else {
            AndroidColor.rgb(0xFF, 0xF9, 0xEE)
        }
    }

}

// ---- Drawer Components ----

@Composable
private fun ScheduleDestination(
    viewModel: ScheduleViewModel,
    onFirstFrameReady: () -> Unit,
    onImportClick: () -> Unit,
    onExamClick: () -> Unit,
    onDrawerOpen: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isInitialized) {
        if (uiState.isInitialized) onFirstFrameReady()
    }
    ScheduleScreen(
        viewModel = viewModel,
        uiState = uiState,
        onImportClick = onImportClick,
        onExamClick = onExamClick,
        onDrawerOpen = onDrawerOpen
    )
}

@Composable
private fun PartnerScheduleDestination(
    viewModel: PartnerScheduleViewModel,
    themeSeedHex: String,
    darkTheme: Boolean,
    onThemeSeedChange: (String) -> Unit,
    onDrawerOpen: () -> Unit
) {
    val seed = parseSeedColor(themeSeedHex)
        ?: parseSeedColor(DEFAULT_PARTNER_THEME_SEED_HEX)
        ?: androidx.compose.ui.graphics.Color(0xFFD94F78)
    val scheme = remember(seed, darkTheme) { seedColorScheme(seed, darkTheme) }
    MaterialTheme(colorScheme = scheme) {
        PartnerScheduleScreen(
            viewModel = viewModel,
            themeSeedHex = themeSeedHex,
            onThemeSeedChange = onThemeSeedChange,
            onDrawerOpen = onDrawerOpen,
            onInviteExpired = viewModel::expireInviteSilently
        )
    }
}

@Composable
private fun ScheduleSettingsDestination(
    viewModel: ScheduleViewModel,
    holidayAdjustmentsViewModel: HolidayAdjustmentsViewModel,
    subPage: SettingsSubPage,
    themeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    themeSeedHex: String,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeSeedChange: (String) -> Unit,
    reminderEnabled: Boolean,
    reminderMinutes: Int,
    notificationPermissionGranted: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderMinutesChange: (Int) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSubPageChange: (SettingsSubPage) -> Unit,
    onReset: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val campusCapabilities = capabilitiesFor(uiState.campusType)
    // 来源在设置子页打开期间也可能被导入流程切换；研究生不能停留在作息编辑页。
    val effectiveSubPage = if (
        subPage == SettingsSubPage.CLASS_PERIODS && !campusCapabilities.supportsClassPeriodSettings
    ) SettingsSubPage.ROOT else subPage
    when (effectiveSubPage) {
        SettingsSubPage.COURSE_COLORS -> CourseColorsPage(
            // 用完整课表：隐藏掉的卡片也要能在这里改色。
            courses = uiState.allCourses,
            overrides = uiState.courseColorOverrides,
            onSetColor = viewModel::setCourseColorOverride,
            onRemoveColor = viewModel::removeCourseColorOverride,
            onResetAll = viewModel::clearCourseColorOverrides
        )
        SettingsSubPage.HOLIDAY_ADJUSTMENTS -> HolidayAdjustmentsScreen(
            viewModel = holidayAdjustmentsViewModel
        )
        SettingsSubPage.CLASS_PERIODS -> ClassPeriodSettingsScreen(
            campusType = uiState.campusType,
            guilinSubCampus = uiState.guilinSubCampus,
            overrides = uiState.classPeriodProfileOverrides,
            onSetPeriods = viewModel::setClassPeriods,
            onResetPeriods = viewModel::resetClassPeriods,
            onSetGuilinSubCampus = viewModel::setGuilinSubCampus
        )
        SettingsSubPage.ROOT -> SettingsPage(
            showWeekend = uiState.showWeekend,
            onShowWeekendChange = viewModel::setShowWeekend,
            showNoon = uiState.showNoon,
            onShowNoonChange = viewModel::setShowNoon,
            // 只有桂林含有午1/午2 两节；南宁与研究生没有这两行，开关置灰但保留在原位，
            // 让用户看得出「这个选项存在，只是当前来源用不上」，而不是整项消失。
            showNoonEnabled = campusCapabilities.hasNoonSections,
            showGridLines = uiState.showGridLines,
            onShowGridLinesChange = viewModel::setShowGridLines,
            themeMode = themeMode,
            dynamicColorEnabled = dynamicColorEnabled,
            themeSeedHex = themeSeedHex,
            onThemeModeChange = onThemeModeChange,
            onDynamicColorChange = onDynamicColorChange,
            onThemeSeedChange = onThemeSeedChange,
            reminderEnabled = reminderEnabled,
            reminderMinutes = reminderMinutes,
            notificationPermissionGranted = notificationPermissionGranted,
            onReminderEnabledChange = onReminderEnabledChange,
            onReminderMinutesChange = onReminderMinutesChange,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onCourseColors = { onSubPageChange(SettingsSubPage.COURSE_COLORS) },
            classPeriodsEnabled = campusCapabilities.supportsClassPeriodSettings,
            onClassPeriods = { onSubPageChange(SettingsSubPage.CLASS_PERIODS) },
            onHolidayAdjustments = { onSubPageChange(SettingsSubPage.HOLIDAY_ADJUSTMENTS) },
            onReset = onReset
        )
    }
}

@Composable
private fun DrawerHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.brand_menu_logo),
            contentDescription = "桂系一站式标志",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            "桂系一站式",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun DrawerMenuItem(
    item: DrawerItem,
    isSelected: Boolean,
    showDot: Boolean = false,
    /**
     * 当前来源是否支持这一项。不支持时入口**保留但置灰**，点击提示 [unsupportedHint]，
     * 而不是像南宁那样整项隐藏——用户能一眼看出「这个菜单存在，只是研究生没有」。
     */
    enabled: Boolean = true,
    unsupportedHint: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    NavigationDrawerItem(
        label = {
            Text(
                item.title,
                fontWeight = if (isSelected && enabled) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        selected = isSelected && enabled,
        icon = { Icon(item.icon, contentDescription = null) },
        badge = if (showDot) {
            { Badge(containerColor = colorScheme.error) }
        } else {
            null
        },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = colorScheme.primaryContainer,
            selectedTextColor = colorScheme.onPrimaryContainer,
            selectedIconColor = colorScheme.onPrimaryContainer,
            unselectedTextColor = if (enabled) {
                colorScheme.onSurface
            } else {
                colorScheme.onSurface.copy(alpha = 0.38f)
            },
            unselectedIconColor = if (enabled) {
                colorScheme.onSurfaceVariant
            } else {
                colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            }
        ),
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        onClick = {
            if (enabled) {
                onClick()
            } else if (!unsupportedHint.isNullOrBlank()) {
                android.widget.Toast.makeText(
                    context,
                    unsupportedHint,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    )
}

// ---- Settings Page ----

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
    themeSeedHex: String = "",
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    onThemeSeedChange: (String) -> Unit = {},
    reminderEnabled: Boolean = false,
    reminderMinutes: Int = 10,
    notificationPermissionGranted: Boolean = false,
    onReminderEnabledChange: (Boolean) -> Unit = {},
    onReminderMinutesChange: (Int) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onCourseColors: () -> Unit = {},
    classPeriodsEnabled: Boolean = true,
    onClassPeriods: () -> Unit = {},
    onHolidayAdjustments: () -> Unit = {},
    onReset: () -> Unit = {}
) {
    var previewReminderMinutes by remember(reminderMinutes) { mutableIntStateOf(reminderMinutes) }
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
                    val modes = listOf(
                        ThemeMode.SYSTEM to "跟随系统",
                        ThemeMode.LIGHT to "浅色",
                        ThemeMode.DARK to "深色"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = modes.size
                                )
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
                SettingsDivider()
                ThemeSeedColorRow(
                    themeSeedHex = themeSeedHex,
                    enabled = !dynamicColorEnabled,
                    onThemeSeedChange = onThemeSeedChange
                )
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
                            value = previewReminderMinutes.coerceIn(5, 30).toFloat(),
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
private fun ThemeSeedColorRow(
    themeSeedHex: String,
    enabled: Boolean,
    onThemeSeedChange: (String) -> Unit
) {
    val current = parseSeedColor(themeSeedHex)
        ?: parseSeedColor(DEFAULT_THEME_SEED_HEX)
        ?: MaterialTheme.colorScheme.primary
    var showPicker by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(current, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "主题色",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                when {
                    !enabled -> "动态颜色开启时改用系统壁纸取色"
                    themeSeedHex.isBlank() -> "默认黄色，可自定义种子颜色"
                    else -> "自定义 ${themeSeedHex.uppercase()}"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (themeSeedHex.isNotBlank()) {
            TextButton(onClick = { onThemeSeedChange("") }, enabled = enabled) {
                Text("恢复默认")
            }
        }
        TextButton(onClick = { showPicker = true }, enabled = enabled) {
            Text("自定义")
        }
    }
    if (showPicker) {
        SeedColorPickerDialog(
            initial = current,
            onDismiss = { showPicker = false },
            onConfirm = { color ->
                onThemeSeedChange(color.toSeedHex())
                showPicker = false
            }
        )
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
    val supportingSlot: (@Composable () -> Unit)? = if (supporting == null) {
        null
    } else {
        { Text(supporting!!, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
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
        supportingContent = supportingSlot,
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
    val supportingSlot: (@Composable () -> Unit)? = if (supporting == null) {
        null
    } else {
        { Text(supporting!!, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
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
        supportingContent = supportingSlot,
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

@Composable
private fun CourseColorsPage(
    courses: List<ScheduleCourse>,
    overrides: Map<String, String>,
    onSetColor: (String, String) -> Unit,
    onRemoveColor: (String) -> Unit,
    onResetAll: () -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    val card = MaterialTheme.colorScheme.surfaceContainerLow
    val entries = remember(courses, overrides) {
        courses.distinctBy { CourseColorMapper.colorKey(it.id, it.title) }
    }
    var selectedCourse by remember { mutableStateOf<ScheduleCourse?>(null) }
    var showAdvanced by remember { mutableStateOf(false) }

    if (entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(background), contentAlignment = Alignment.Center) {
            Text("暂未导入课表", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("已导入课程", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
        items(entries, key = { it.id }) { course ->
            val key = CourseColorMapper.colorKey(course.id, course.title)
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    selectedCourse = course
                    showAdvanced = false
                },
                color = card,
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        color = Color(AndroidColor.parseColor(course.colorHex)),
                        shape = RoundedCornerShape(8.dp)
                    ) {}
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(course.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (overrides.containsKey(key)) "已自定义" else "自动配色", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
        item {
            TextButton(modifier = Modifier.fillMaxWidth(), onClick = onResetAll) {
                Text("恢复全部默认颜色", color = MaterialTheme.colorScheme.error)
            }
        }
        // 「已隐藏的卡片」已迁到「调休调课」页：两者都是「我手动改动了课表」的叠加层。
    }

    selectedCourse?.let { course ->
        if (showAdvanced) {
            AdvancedColorSheet(
                initialColor = course.colorHex,
                onDismiss = {
                    showAdvanced = false
                    selectedCourse = null
                },
                onConfirm = { color ->
                    onSetColor(CourseColorMapper.colorKey(course.id, course.title), color)
                    selectedCourse = null
                }
            )
        } else {
            PresetColorSheet(
                course = course,
                onDismiss = { selectedCourse = null },
                onSelect = { color ->
                    onSetColor(CourseColorMapper.colorKey(course.id, course.title), color)
                    selectedCourse = null
                },
                onAdvanced = { showAdvanced = true },
                onRestore = {
                    onRemoveColor(CourseColorMapper.colorKey(course.id, course.title))
                    selectedCourse = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetColorSheet(
    course: ScheduleCourse,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onAdvanced: () -> Unit,
    onRestore: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column {
                Text(course.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("选择课程卡片颜色", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
            // 预设色网格与两个按钮已提取到共享组件，与长按卡片的「卡片管理」弹层共用同一套。
            CourseColorPaletteSection(
                onSelect = onSelect,
                onAdvanced = onAdvanced,
                onRestore = onRestore
            )
        }
    }
}

// AdvancedColorSheet 与它的 hexToHsv / hsvToHex 已移到 ui/components/CourseColorPalette.kt，
// 与长按卡片的「卡片管理」弹层共用。

// ---- Dialogs ----

private val EXPLICIT_ACTION_DIALOG_PROPERTIES = DialogProperties(
    dismissOnBackPress = false,
    dismissOnClickOutside = false
)

// ---- Update Dialog ----

private sealed class UpdateDialogState {
    data class Idle(val info: UpdateInfo) : UpdateDialogState()
    data class Downloading(
        val info: UpdateInfo,
        val progress: Float,
        val downloadedMB: String,
        val totalMB: String
    ) : UpdateDialogState()
    data class DownloadFailed(val info: UpdateInfo, val message: String) : UpdateDialogState()
    data class Done(val info: UpdateInfo, val apkFile: java.io.File) : UpdateDialogState()
}

@Composable
private fun UpdateDialog(
    state: UpdateDialogState,
    appUpdater: com.glut.schedule.service.AppUpdater,
    onDismiss: () -> Unit,
    onStateChange: (UpdateDialogState?) -> Unit
) {
    val scope = rememberCoroutineScope()
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    fun startDownload(info: UpdateInfo) {
        downloadJob?.cancel()
        downloadJob = scope.launch {
            onStateChange(
                UpdateDialogState.Downloading(
                    info = info,
                    progress = 0f,
                    downloadedMB = "0",
                    totalMB = "..."
                )
            )
            try {
                val apkFile = appUpdater.downloadApk(info) { downloaded, total ->
                    val progress = if (total > 0) downloaded.toFloat() / total else 0f
                    val dMB = "%.1f".format(downloaded / 1_000_000.0)
                    val tMB = if (total > 0) "%.1f".format(total / 1_000_000.0) else "?"
                    onStateChange(
                        UpdateDialogState.Downloading(
                            info = info,
                            progress = progress,
                            downloadedMB = dMB,
                            totalMB = tMB
                        )
                    )
                }
                onStateChange(UpdateDialogState.Done(info, apkFile))
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                onStateChange(
                    UpdateDialogState.DownloadFailed(
                        info = info,
                        message = e.message ?: "下载失败，请检查网络后重试"
                    )
                )
            } finally {
                if (downloadJob === coroutineContext[Job]) downloadJob = null
            }
        }
    }

    when (state) {
        is UpdateDialogState.Idle -> {
            AlertDialog(
                onDismissRequest = { },
                properties = EXPLICIT_ACTION_DIALOG_PROPERTIES,
                title = {
                    Text(
                        if (state.info.isNewer) "发现新版本 v${state.info.latestVersion}"
                        else "已是最新版本"
                    )
                },
                text = {
                    Column {
                        if (state.info.isNewer) {
                            Text("当前版本: v${BuildConfig.VERSION_NAME}")
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.info.releaseNotes.isNotBlank()) {
                                // 与公告弹窗同一个毛病：发布说明往往比公告还长，
                                // 而这两个弹窗都禁止点外关闭，更需要框内滚动。
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 300.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        state.info.releaseNotes,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text("当前已是最新版本 v${BuildConfig.VERSION_NAME}")
                        }
                    }
                },
                confirmButton = {
                    if (state.info.isNewer && state.info.apkDownloadUrl.isNotBlank()) {
                        TextButton(onClick = { startDownload(state.info) }) {
                            Text("立即更新")
                        }
                    }
                },
                dismissButton = {
                    if (!state.info.isForceUpdate) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                    }
                }
            )
        }

        is UpdateDialogState.Downloading -> {
            AlertDialog(
                onDismissRequest = { },
                properties = EXPLICIT_ACTION_DIALOG_PROPERTIES,
                title = { Text("正在下载 v${state.info.latestVersion}") },
                text = {
                    Column {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = { state.progress },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${state.downloadedMB} MB / ${state.totalMB} MB  (${(state.progress * 100).toInt()}%)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    if (!state.info.isForceUpdate) {
                        TextButton(onClick = {
                            downloadJob?.cancel()
                            downloadJob = null
                            onDismiss()
                            onStateChange(null)
                        }) {
                            Text("取消下载")
                        }
                    }
                }
            )
        }

        is UpdateDialogState.DownloadFailed -> {
            AlertDialog(
                onDismissRequest = { },
                properties = EXPLICIT_ACTION_DIALOG_PROPERTIES,
                title = { Text("下载失败") },
                text = { Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    TextButton(onClick = { startDownload(state.info) }) {
                        Text("重试")
                    }
                },
                dismissButton = {
                    if (!state.info.isForceUpdate) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                    }
                }
            )
        }

        is UpdateDialogState.Done -> {
            AlertDialog(
                onDismissRequest = { },
                properties = EXPLICIT_ACTION_DIALOG_PROPERTIES,
                title = { Text("下载完成") },
                text = { Text("v${state.info.latestVersion} 已下载，是否立即安装？") },
                confirmButton = {
                    TextButton(onClick = {
                        appUpdater.installApk(state.apkFile)
                        onDismiss()
                    }) {
                        Text("立即安装")
                    }
                },
                dismissButton = {
                    if (!state.info.isForceUpdate) {
                        TextButton(onClick = onDismiss) { Text("稍后") }
                    }
                }
            )
        }
    }
}

/** 复制已安装的 APK 到缓存目录，并通过系统选择器分享给用户指定的应用。 */
private fun shareApk(context: Context) {
    try {
        val sourceApk = File(context.packageCodePath)
        // 清理旧分享文件，避免系统选择器展示带重复后缀的 APK。
        context.cacheDir.listFiles()?.filter {
            it.name.startsWith("桂系一站式_v") && it.name.endsWith(".apk")
        }?.forEach { it.delete() }
        val sharedApk = File(context.cacheDir, "桂系一站式_v${BuildConfig.VERSION_NAME}.apk")
        sourceApk.copyTo(sharedApk, overwrite = true)
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            sharedApk
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_TEXT, "桂系一站式 — 查课表、成绩、考试安排超方便！")
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享到"))
    } catch (_: Exception) {
        // 分享属于辅助功能，异常时不影响 About 页和应用主流程。
    }
}
