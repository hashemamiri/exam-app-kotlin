package ir.exam.app.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.exam.app.core.ui.AppearanceSettings
import ir.exam.app.core.update.ApkUpdateManager
import ir.exam.app.core.update.UpdateUseCase
import ir.exam.app.data.repository.SupabaseAppUpdateRepository
import ir.exam.app.data.repository.SupabaseAuthRepository
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.model.UserRole
import ir.exam.app.ui.auth.AuthViewModel
import ir.exam.app.ui.auth.SignInScreen
import ir.exam.app.ui.billing.WalletScreen
import ir.exam.app.ui.builder.ExamBuilderScreen
import ir.exam.app.ui.builder.ExamBuilderViewModel
import ir.exam.app.ui.builder.ExamImportDraft
import ir.exam.app.ui.calendar.CalendarScreen
import ir.exam.app.ui.classes.SchoolLaunchAction
import ir.exam.app.ui.classes.SchoolManagementScreen
import ir.exam.app.ui.dashboard.TeacherDashboardScreen
import ir.exam.app.ui.grading.GradingScreen
import ir.exam.app.ui.profile.ProfileSettingsScreen
import ir.exam.app.ui.reports.ReportsScreen
import ir.exam.app.ui.reports.StudentResultsScreen
import ir.exam.app.ui.security.AppLockGate
import ir.exam.app.ui.student.StudentHomeScreen
import ir.exam.app.ui.update.AboutScreen
import ir.exam.app.ui.update.UpdateViewModel

private enum class MainPage {
    HOME, CALENDAR, SCHOOL, GRADING, REPORTS, STUDENT_RESULTS, WALLET, CARDS, SETTINGS, ABOUT, BUILDER
}

@Composable
fun ExamApp(appearance: AppearanceSettings = AppearanceSettings()) {
    val appContext = LocalContext.current.applicationContext
    val authViewModel = remember(appContext) {
        AuthViewModel(SupabaseAuthRepository(appContext))
    }
    val authState by authViewModel.state.collectAsState()
    val user = authState.user
    val restoreError = authState.restoreError

    when {
        authState.isRestoringSession -> {
            SessionLoadingScreen()
            return
        }
        restoreError != null && user == null -> {
            SessionRestoreErrorScreen(message = restoreError, onRetry = authViewModel::retrySessionRestore)
            return
        }
        user == null -> {
            SignInScreen(viewModel = authViewModel)
            return
        }
    }

    AppLockGate(user.id) {
        AuthenticatedExamApp(user, authViewModel, appearance)
    }
}

@Composable
private fun AuthenticatedExamApp(
    user: AppUser,
    authViewModel: AuthViewModel,
    appearance: AppearanceSettings
) {
    val appContext = LocalContext.current.applicationContext
    val authState by authViewModel.state.collectAsState()
    val apkUpdateManager = remember(appContext) { ApkUpdateManager(appContext) }
    val updateViewModel = remember(user.id) {
        UpdateViewModel(UpdateUseCase(SupabaseAppUpdateRepository()), apkUpdateManager)
    }
    var page by rememberSaveable(user.id) { mutableStateOf(MainPage.HOME) }
    var menuOpen by rememberSaveable(user.id) { mutableStateOf(false) }
    var quickAddOpen by rememberSaveable(user.id) { mutableStateOf(false) }
    var walletRefreshKey by rememberSaveable(user.id) { mutableIntStateOf(0) }
    var dashboardRefreshKey by rememberSaveable(user.id) { mutableIntStateOf(0) }
    var cardsCycleKey by rememberSaveable(user.id) { mutableIntStateOf(0) }
    var editingExamId by remember(user.id) { mutableStateOf<String?>(null) }
    var importedExam by remember(user.id) { mutableStateOf<ExamImportDraft?>(null) }
    var schoolLaunchAction by remember(user.id) { mutableStateOf<SchoolLaunchAction?>(null) }
    var gradingPendingOnly by remember(user.id) { mutableStateOf(false) }
    var showSignOut by remember(user.id) { mutableStateOf(false) }

    fun closeTransientNavigation() {
        menuOpen = false
        quickAddOpen = false
    }

    fun openHome() {
        closeTransientNavigation()
        if (page == MainPage.HOME && user.role == UserRole.TEACHER) {
            dashboardRefreshKey += 1
        } else {
            page = MainPage.HOME
        }
    }

    fun openWallet() {
        closeTransientNavigation()
        if (page == MainPage.WALLET) walletRefreshKey += 1 else page = MainPage.WALLET
    }

    fun openCards() {
        closeTransientNavigation()
        if (page == MainPage.CARDS) cardsCycleKey += 1 else page = MainPage.CARDS
    }

    fun createStudent() {
        closeTransientNavigation()
        schoolLaunchAction = SchoolLaunchAction.CREATE_STUDENT
        page = MainPage.SCHOOL
    }

    fun createExam() {
        closeTransientNavigation()
        editingExamId = null
        importedExam = null
        page = MainPage.BUILDER
    }

    fun createClass() {
        closeTransientNavigation()
        schoolLaunchAction = SchoolLaunchAction.CREATE_CLASS
        page = MainPage.SCHOOL
    }

    LaunchedEffect(user.id, user.role) {
        val teacherOnly = setOf(
            MainPage.BUILDER,
            MainPage.SCHOOL,
            MainPage.GRADING,
            MainPage.REPORTS,
            MainPage.WALLET,
            MainPage.CARDS
        )
        if (user.role != UserRole.TEACHER && page in teacherOnly) page = MainPage.HOME
        if (user.role == UserRole.TEACHER && page == MainPage.STUDENT_RESULTS) page = MainPage.HOME
    }

    if (page == MainPage.BUILDER && user.role == UserRole.TEACHER) {
        val builderViewModel = remember(user.id, editingExamId, importedExam) {
            ExamBuilderViewModel(appContext, editingExamId, importedExam)
        }
        ExamBuilderScreen(
            viewModel = builderViewModel,
            onBack = {
                editingExamId = null
                importedExam = null
                page = MainPage.HOME
            }
        )
        return
    }

    BackHandler(enabled = menuOpen && !quickAddOpen) {
        menuOpen = false
    }
    BackHandler(enabled = !menuOpen && !quickAddOpen && page != MainPage.HOME) {
        page = MainPage.HOME
    }

    AuthenticatedShell(
        user = user,
        page = page,
        appearance = appearance,
        menuOpen = menuOpen,
        quickAddOpen = quickAddOpen,
        onToggleMenu = {
            if (quickAddOpen) quickAddOpen = false
            menuOpen = !menuOpen
        },
        onToggleAdd = {
            menuOpen = false
            quickAddOpen = !quickAddOpen
        },
        onCloseAdd = { quickAddOpen = false },
        onHome = ::openHome,
        onCalendar = { closeTransientNavigation(); page = MainPage.CALENDAR },
        onSchool = { closeTransientNavigation(); schoolLaunchAction = null; page = MainPage.SCHOOL },
        onGrading = { closeTransientNavigation(); gradingPendingOnly = false; page = MainPage.GRADING },
        onReports = { closeTransientNavigation(); page = MainPage.REPORTS },
        onStudentResults = { closeTransientNavigation(); page = MainPage.STUDENT_RESULTS },
        onWallet = ::openWallet,
        onCards = ::openCards,
        onSettings = { closeTransientNavigation(); page = MainPage.SETTINGS },
        onAbout = { closeTransientNavigation(); page = MainPage.ABOUT },
        onCreateStudent = ::createStudent,
        onCreateExam = ::createExam,
        onCreateClass = ::createClass,
        onSignOut = { closeTransientNavigation(); showSignOut = true }
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val enter = when (targetState) {
                    MainPage.WALLET -> fadeIn(tween(300)) + slideInVertically(tween(580)) { it / 5 }
                    MainPage.HOME -> fadeIn(tween(280)) + slideInHorizontally(tween(580)) { -it / 7 }
                    MainPage.CARDS -> fadeIn(tween(320)) + scaleIn(tween(660), initialScale = .90f)
                    else -> fadeIn(tween(260)) + slideInHorizontally(tween(520)) { it / 8 }
                }
                val exit = fadeOut(tween(170)) + slideOutHorizontally(tween(240)) { -it / 12 }
                (enter togetherWith exit).using(SizeTransform(clip = false))
            },
            label = "design69-page-transition"
        ) { targetPage ->
            when (targetPage) {
                MainPage.HOME -> when (user.role) {
                    UserRole.TEACHER -> TeacherDashboardScreen(
                        refreshKey = dashboardRefreshKey,
                        onCreateExam = ::createExam,
                        onEditExam = { id ->
                            editingExamId = id
                            importedExam = null
                            page = MainPage.BUILDER
                        },
                        onImportExam = { draft ->
                            editingExamId = null
                            importedExam = draft
                            page = MainPage.BUILDER
                        }
                    )
                    UserRole.STUDENT -> StudentHomeScreen(user.id)
                }
                MainPage.CALENDAR -> CalendarScreen(user.role)
                MainPage.SCHOOL -> if (user.role == UserRole.TEACHER) {
                    SchoolManagementScreen(
                        launchAction = schoolLaunchAction,
                        onLaunchActionConsumed = { schoolLaunchAction = null }
                    )
                }
                MainPage.GRADING -> if (user.role == UserRole.TEACHER) {
                    GradingScreen(initialPendingOnly = gradingPendingOnly)
                }
                MainPage.REPORTS -> if (user.role == UserRole.TEACHER) ReportsScreen()
                MainPage.STUDENT_RESULTS -> if (user.role == UserRole.STUDENT) StudentResultsScreen()
                MainPage.WALLET -> if (user.role == UserRole.TEACHER) WalletScreen(refreshKey = walletRefreshKey)
                MainPage.CARDS -> if (user.role == UserRole.TEACHER) {
                    TeacherManagementCardsScreen(
                        cycleKey = cardsCycleKey,
                        onStats = { page = MainPage.REPORTS },
                        onGrading = { gradingPendingOnly = false; page = MainPage.GRADING },
                        onPending = { gradingPendingOnly = true; page = MainPage.GRADING }
                    )
                }
                MainPage.SETTINGS -> ProfileSettingsScreen(
                    user,
                    appearance,
                    authViewModel::refreshCurrentUser
                )
                MainPage.ABOUT -> AboutScreen(updateViewModel, apkUpdateManager)
                MainPage.BUILDER -> Unit
            }
        }
    }

    if (showSignOut) {
        AlertDialog(
            onDismissRequest = { if (!authState.isLoading) showSignOut = false },
            title = { Text("خروج از حساب") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("از حساب فعلی خارج شوید؟ برای ورود دوباره به رمز یا OTP نیاز دارید.")
                    authState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(onClick = authViewModel::signOut, enabled = !authState.isLoading) {
                    Text(if (authState.isLoading) "در حال خروج..." else "خروج")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOut = false },
                    enabled = !authState.isLoading
                ) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun SessionLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator()
            Text("در حال بازیابی نشست ورود...")
        }
    }
}

@Composable
private fun SessionRestoreErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("بازیابی نشست ورود کامل نشد", style = MaterialTheme.typography.titleLarge)
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            "اطلاعات ورود حذف نشده است. اتصال اینترنت را بررسی و دوباره تلاش کنید.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 20.dp)) {
            Text("تلاش دوباره")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell(
    user: AppUser,
    page: MainPage,
    appearance: AppearanceSettings,
    menuOpen: Boolean,
    quickAddOpen: Boolean,
    onToggleMenu: () -> Unit,
    onToggleAdd: () -> Unit,
    onCloseAdd: () -> Unit,
    onHome: () -> Unit,
    onCalendar: () -> Unit,
    onSchool: () -> Unit,
    onGrading: () -> Unit,
    onReports: () -> Unit,
    onStudentResults: () -> Unit,
    onWallet: () -> Unit,
    onCards: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onCreateStudent: () -> Unit,
    onCreateExam: () -> Unit,
    onCreateClass: () -> Unit,
    onSignOut: () -> Unit,
    content: @Composable () -> Unit
) {
    fun select(action: () -> Unit) = action()

    val menuCards = if (user.role == UserRole.TEACHER) {
        listOf(
            Design69MenuCard("داشبورد معلم", "مدیریت آزمون‌ها", Design69Icons.Dashboard, page == MainPage.HOME, onClick = { select(onHome) }),
            Design69MenuCard("تقویم و پیام‌ها", "رویدادها و پیام‌ها", Design69Icons.Calendar, page == MainPage.CALENDAR, onClick = { select(onCalendar) }),
            Design69MenuCard("کلاس و دانش‌آموز", "مدیریت مدرسه", Design69Icons.Classes, page == MainPage.SCHOOL, onClick = { select(onSchool) }),
            Design69MenuCard("تصحیح و حضور", "پاسخ‌ها و حضور", Design69Icons.Grading, page == MainPage.GRADING, onClick = { select(onGrading) }),
            Design69MenuCard("آمار و گزارش‌ها", "نمودار و خروجی", Design69Icons.Reports, page == MainPage.REPORTS, onClick = { select(onReports) }),
            Design69MenuCard("کیف پول", "موجودی و پرداخت", Design69Icons.Wallet, page == MainPage.WALLET, onClick = { select(onWallet) }),
            Design69MenuCard("تنظیمات", "حساب و ظاهر", Design69Icons.Settings, page == MainPage.SETTINGS, onClick = { select(onSettings) }),
            Design69MenuCard("درباره و بروزرسانی", "بررسی و دریافت واقعی APK", Design69Icons.InfoUpdate, page == MainPage.ABOUT, onClick = { select(onAbout) }),
            Design69MenuCard("آزمون جدید", "ورود مستقیم به سازنده", Design69Icons.ExamAdd, onClick = { select(onCreateExam) }),
            Design69MenuCard("خروج", "خروج امن و تعویض حساب", Design69Icons.Logout, danger = true, onClick = { select(onSignOut) })
        )
    } else {
        listOf(
            Design69MenuCard("داشبورد دانش‌آموز", "ورود و ادامه آزمون", Design69Icons.Dashboard, page == MainPage.HOME, onClick = { select(onHome) }),
            Design69MenuCard("تقویم و پیام‌ها", "رویدادها و پیام‌ها", Design69Icons.Calendar, page == MainPage.CALENDAR, onClick = { select(onCalendar) }),
            Design69MenuCard("نتایج من", "پاسخ‌ها و کارنامه", Design69Icons.Reports, page == MainPage.STUDENT_RESULTS, onClick = { select(onStudentResults) }),
            Design69MenuCard("تنظیمات", "حساب و ظاهر", Design69Icons.Settings, page == MainPage.SETTINGS, onClick = { select(onSettings) }),
            Design69MenuCard("درباره و بروزرسانی", "بررسی و دریافت واقعی APK", Design69Icons.InfoUpdate, page == MainPage.ABOUT, onClick = { select(onAbout) }),
            Design69MenuCard("خروج", "خروج امن و تعویض حساب", Design69Icons.Logout, danger = true, onClick = { select(onSignOut) })
        )
    }

    Neumorphic69Provider(depth = appearance.neumorphicDepth) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val colors = neumorphic69Colors
            Box(Modifier.fillMaxSize().background(colors.background)) {
                Scaffold(
                    containerColor = colors.background,
                    topBar = {
                        NeumorphicTopBar(
                            title = when {
                                quickAddOpen -> "افزودن سریع"
                                menuOpen -> "منوی اصلی"
                                else -> page.title(user.role)
                            },
                            subtitle = when {
                                quickAddOpen -> "یک عملیات واقعی جدید بسازید"
                                menuOpen -> "دسترسی سریع به بخش‌های سامانه"
                                else -> "${user.name.ifBlank { "کاربر" }} · ${if (user.role == UserRole.TEACHER) "حساب معلم" else "حساب دانش‌آموز"}"
                            },
                            navigationDescription = if (user.role == UserRole.STUDENT) {
                                if (menuOpen) "بستن منو" else "بازکردن منو"
                            } else null,
                            navigationIconContent = if (user.role == UserRole.STUDENT) {
                                { tint, modifier -> Design69MorphingMenuIcon(menuOpen, tint, modifier) }
                            } else null,
                            onNavigation = onToggleMenu
                        )
                    },
                    bottomBar = {
                        if (user.role == UserRole.TEACHER) {
                            TeacherBottomDock(
                                active = page.teacherDockSection(),
                                menuOpen = menuOpen,
                                quickAddOpen = quickAddOpen,
                                onMenu = onToggleMenu,
                                onWallet = onWallet,
                                onAdd = onToggleAdd,
                                onExams = onHome,
                                onCards = onCards
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.background)
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(Modifier.fillMaxSize().widthIn(max = 900.dp)) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = if (menuOpen) .10f else 1f }
                            ) {
                                content()
                            }
                            AnimatedVisibility(
                                visible = menuOpen,
                                modifier = Modifier.fillMaxSize(),
                                enter = fadeIn(tween(260)) + scaleIn(tween(420), initialScale = .985f),
                                exit = fadeOut(tween(190)) + scaleOut(tween(220), targetScale = .985f)
                            ) {
                                Box(Modifier.fillMaxSize().background(colors.background)) {
                                    Design69MainMenuScreen(
                                        user = user,
                                        cards = menuCards,
                                        onProfile = onSettings
                                    )
                                }
                            }
                        }
                    }
                }

                if (quickAddOpen && user.role == UserRole.TEACHER) {
                    Design69QuickAddOverlay(
                        onDismiss = onCloseAdd,
                        onCreateStudent = onCreateStudent,
                        onCreateExam = onCreateExam,
                        onCreateClass = onCreateClass
                    )
                }
            }
        }
    }
}

private fun MainPage.teacherDockSection(): TeacherDockSection = when (this) {
    MainPage.HOME -> TeacherDockSection.EXAMS
    MainPage.WALLET -> TeacherDockSection.WALLET
    MainPage.CARDS, MainPage.GRADING, MainPage.REPORTS -> TeacherDockSection.CARDS
    else -> TeacherDockSection.NONE
}

private fun MainPage.title(role: UserRole): String = when (this) {
    MainPage.HOME -> if (role == UserRole.TEACHER) "داشبورد معلم" else "داشبورد دانش‌آموز"
    MainPage.CALENDAR -> "تقویم و پیام‌ها"
    MainPage.SCHOOL -> "کلاس‌ها و دانش‌آموزان"
    MainPage.GRADING -> "تصحیح و حضور"
    MainPage.REPORTS -> "آمار و گزارش‌ها"
    MainPage.STUDENT_RESULTS -> "نتایج و پاسخ‌های من"
    MainPage.WALLET -> "کیف پول و پرداخت"
    MainPage.CARDS -> "کارت‌های مدیریتی"
    MainPage.SETTINGS -> "پروفایل و تنظیمات"
    MainPage.ABOUT -> "درباره و بروزرسانی"
    MainPage.BUILDER -> "ساخت آزمون"
}
