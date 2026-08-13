package ir.exam.app.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
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
import ir.exam.app.ui.profile.ProfileAvatar
import ir.exam.app.ui.profile.ProfileSettingsScreen
import ir.exam.app.ui.reports.ReportsScreen
import ir.exam.app.ui.reports.StudentResultsScreen
import ir.exam.app.ui.security.AppLockGate
import ir.exam.app.ui.student.StudentHomeScreen
import ir.exam.app.ui.update.AboutScreen
import ir.exam.app.ui.update.UpdateViewModel
import kotlinx.coroutines.launch

private enum class MainPage {
    HOME, CALENDAR, SCHOOL, GRADING, REPORTS, STUDENT_RESULTS, WALLET, SETTINGS, ABOUT, BUILDER
}

private data class DrawerCardSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val selected: Boolean = false,
    val danger: Boolean = false,
    val action: () -> Unit
)

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

    AppLockGate(user.id) { AuthenticatedExamApp(user,authViewModel,appearance) }
}

@Composable
private fun AuthenticatedExamApp(user:AppUser,authViewModel:AuthViewModel,appearance:AppearanceSettings){
    val appContext=LocalContext.current.applicationContext
    val authState by authViewModel.state.collectAsState()
    val apkUpdateManager = remember(appContext) { ApkUpdateManager(appContext) }
    val updateViewModel = remember(user.id) {
        UpdateViewModel(UpdateUseCase(SupabaseAppUpdateRepository()), apkUpdateManager)
    }
    var page by remember(user.id) { mutableStateOf(MainPage.HOME) }
    var editingExamId by remember(user.id) { mutableStateOf<String?>(null) }
    var importedExam by remember(user.id) { mutableStateOf<ExamImportDraft?>(null) }
    var schoolLaunchAction by remember(user.id) { mutableStateOf<SchoolLaunchAction?>(null) }
    var gradingPendingOnly by remember(user.id) { mutableStateOf(false) }
    var showSignOut by remember(user.id) { mutableStateOf(false) }

    LaunchedEffect(user.id, user.role) {
        val teacherOnly = setOf(MainPage.BUILDER, MainPage.SCHOOL, MainPage.GRADING, MainPage.REPORTS, MainPage.WALLET)
        if (user.role != UserRole.TEACHER && page in teacherOnly) page = MainPage.HOME
        if (user.role == UserRole.TEACHER && page == MainPage.STUDENT_RESULTS) page = MainPage.HOME
    }

    if (page == MainPage.BUILDER && user.role == UserRole.TEACHER) {
        val builderViewModel = remember(user.id, editingExamId, importedExam) {
            ExamBuilderViewModel(appContext, editingExamId, importedExam)
        }
        ExamBuilderScreen(
            viewModel = builderViewModel,
            onBack = { editingExamId = null; importedExam = null; page = MainPage.HOME }
        )
        return
    }

    BackHandler(enabled = page != MainPage.HOME) {
        page = MainPage.HOME
    }

    AuthenticatedDrawer(
        user = user,
        page = page,
        appearance = appearance,
        onHome = { page = MainPage.HOME },
        onCalendar = { page = MainPage.CALENDAR },
        onSchool = { schoolLaunchAction = null; page = MainPage.SCHOOL },
        onGrading = { gradingPendingOnly = false; page = MainPage.GRADING },
        onReports = { page = MainPage.REPORTS },
        onStudentResults = { page = MainPage.STUDENT_RESULTS },
        onWallet = { page = MainPage.WALLET },
        onSettings = { page = MainPage.SETTINGS },
        onAbout = { page = MainPage.ABOUT },
        onCreateStudent = { schoolLaunchAction = SchoolLaunchAction.CREATE_STUDENT; page = MainPage.SCHOOL },
        onCreateExam = { editingExamId = null; importedExam = null; page = MainPage.BUILDER },
        onCreateClass = { schoolLaunchAction = SchoolLaunchAction.CREATE_CLASS; page = MainPage.SCHOOL },
        onExams = { page = MainPage.HOME },
        onStats = { page = MainPage.REPORTS },
        onPending = { gradingPendingOnly = true; page = MainPage.GRADING },
        onSignOut = { showSignOut = true }
    ) {
        when (page) {
            MainPage.HOME -> when (user.role) {
                UserRole.TEACHER -> TeacherDashboardScreen(
                    onCreateExam = { editingExamId = null; importedExam = null; page = MainPage.BUILDER },
                    onEditExam = { id -> editingExamId = id; importedExam = null; page = MainPage.BUILDER },
                    onImportExam = { draft -> editingExamId = null; importedExam = draft; page = MainPage.BUILDER }
                )
                UserRole.STUDENT -> StudentHomeScreen(user.id)
            }
            MainPage.CALENDAR -> CalendarScreen(user.role)
            MainPage.SCHOOL -> if (user.role == UserRole.TEACHER) SchoolManagementScreen(
                launchAction = schoolLaunchAction,
                onLaunchActionConsumed = { schoolLaunchAction = null }
            )
            MainPage.GRADING -> if (user.role == UserRole.TEACHER) GradingScreen(initialPendingOnly = gradingPendingOnly)
            MainPage.REPORTS -> if (user.role == UserRole.TEACHER) ReportsScreen()
            MainPage.STUDENT_RESULTS -> if (user.role == UserRole.STUDENT) StudentResultsScreen()
            MainPage.WALLET -> if (user.role == UserRole.TEACHER) WalletScreen()
            MainPage.SETTINGS -> ProfileSettingsScreen(user, appearance, authViewModel::refreshCurrentUser)
            MainPage.ABOUT -> AboutScreen(updateViewModel, apkUpdateManager)
            MainPage.BUILDER -> Unit
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
                TextButton(onClick = { showSignOut = false }, enabled = !authState.isLoading) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun SessionLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
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
private fun SessionRestoreErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "بازیابی نشست ورود کامل نشد",
            style = MaterialTheme.typography.titleLarge
        )
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
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text("تلاش دوباره")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedDrawer(
    user: AppUser,
    page: MainPage,
    appearance: AppearanceSettings,
    onHome: () -> Unit,
    onCalendar: () -> Unit,
    onSchool: () -> Unit,
    onGrading: () -> Unit,
    onReports: () -> Unit,
    onStudentResults: () -> Unit,
    onWallet: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onCreateStudent: () -> Unit,
    onCreateExam: () -> Unit,
    onCreateClass: () -> Unit,
    onExams: () -> Unit,
    onStats: () -> Unit,
    onPending: () -> Unit,
    onSignOut: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun select(action: () -> Unit) {
        action()
        scope.launch { drawerState.close() }
    }

    val drawerCards = if (user.role == UserRole.TEACHER) {
        listOf(
            DrawerCardSpec("داشبورد معلم", "مدیریت آزمون‌ها", Icons.AutoMirrored.Outlined.Assignment, page == MainPage.HOME) { select(onHome) },
            DrawerCardSpec("تقویم و پیام‌ها", "رویدادها و پیام‌ها", Icons.Outlined.CalendarMonth, page == MainPage.CALENDAR) { select(onCalendar) },
            DrawerCardSpec("کلاس و دانش‌آموز", "مدیریت مدرسه", Icons.Outlined.Groups, page == MainPage.SCHOOL) { select(onSchool) },
            DrawerCardSpec("تصحیح و حضور", "پاسخ‌ها و حضور", Icons.Outlined.Assessment, page == MainPage.GRADING) { select(onGrading) },
            DrawerCardSpec("آمار و گزارش‌ها", "نمودار و خروجی", Icons.Outlined.BarChart, page == MainPage.REPORTS) { select(onReports) },
            DrawerCardSpec("کیف پول", "موجودی و پرداخت", Icons.Outlined.AccountBalanceWallet, page == MainPage.WALLET) { select(onWallet) },
            DrawerCardSpec("تنظیمات", "حساب و ظاهر", Icons.Outlined.ManageAccounts, page == MainPage.SETTINGS) { select(onSettings) },
            DrawerCardSpec("درباره و بروزرسانی", "نسخه و دریافت APK", Icons.Outlined.Info, page == MainPage.ABOUT) { select(onAbout) },
            DrawerCardSpec("آزمون جدید", "ورود مستقیم به سازنده", Icons.Outlined.PostAdd) { select(onCreateExam) },
            DrawerCardSpec("خروج", "خروج و تعویض حساب", Icons.AutoMirrored.Outlined.Logout, danger = true) { select(onSignOut) }
        )
    } else {
        listOf(
            DrawerCardSpec("داشبورد دانش‌آموز", "ورود و ادامه آزمون", Icons.Outlined.Home, page == MainPage.HOME) { select(onHome) },
            DrawerCardSpec("تقویم و پیام‌ها", "رویدادها و پیام‌ها", Icons.Outlined.CalendarMonth, page == MainPage.CALENDAR) { select(onCalendar) },
            DrawerCardSpec("نتایج من", "پاسخ‌ها و کارنامه", Icons.Outlined.Assessment, page == MainPage.STUDENT_RESULTS) { select(onStudentResults) },
            DrawerCardSpec("تنظیمات", "حساب و ظاهر", Icons.Outlined.ManageAccounts, page == MainPage.SETTINGS) { select(onSettings) },
            DrawerCardSpec("درباره و بروزرسانی", "نسخه و دریافت APK", Icons.Outlined.Info, page == MainPage.ABOUT) { select(onAbout) },
            DrawerCardSpec("خروج", "خروج و تعویض حساب", Icons.AutoMirrored.Outlined.Logout, danger = true) { select(onSignOut) }
        )
    }

    val expectedDrawerCount = if (user.role == UserRole.TEACHER) {
        NeumorphicDrawerContract.TEACHER_CARD_COUNT
    } else {
        NeumorphicDrawerContract.STUDENT_CARD_COUNT
    }
    require(drawerCards.size == expectedDrawerCount && NeumorphicDrawerContract.hasCompleteRows(drawerCards.size))

    Neumorphic69Provider(depth = appearance.neumorphicDepth) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val colors = neumorphic69Colors
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(colors.background)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            NeumorphicPressable(
                                onClick = { select(onSettings) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(NeumorphicDrawerContract.PROFILE_HEIGHT_DP.dp),
                                radius = 29.dp,
                                depth = LocalNeumorphic69Depth.current + 2.dp,
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                                ) {
                                    ProfileAvatar(user.avatarUrl, user.name.ifBlank { "کاربر" }, 76)
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            if (user.role == UserRole.TEACHER) "پروفایل معلم" else "پروفایل دانش‌آموز",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.accent
                                        )
                                        Text(
                                            user.name.ifBlank { "کاربر" },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.ink,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                        Text(
                                            "مشاهده و ویرایش حساب و تنظیمات",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.muted,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                    Icon(
                                        Icons.Outlined.ChevronLeft,
                                        contentDescription = "بازکردن پروفایل",
                                        tint = colors.accent,
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(
                                "دسترسی سریع",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.ink,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Spacer(Modifier.height(5.dp))
                            drawerCards
                                .chunked(NeumorphicDrawerContract.COLUMNS)
                                .forEachIndexed { rowIndex, rowCards ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowCards.forEach { card ->
                                            NeumorphicDrawerMenuCard(
                                                title = card.title,
                                                subtitle = card.subtitle,
                                                icon = card.icon,
                                                selected = card.selected,
                                                danger = card.danger,
                                                onClick = card.action,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowCards.size < NeumorphicDrawerContract.COLUMNS) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                    if (rowIndex != drawerCards.lastIndex / NeumorphicDrawerContract.COLUMNS) {
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }
                            Spacer(Modifier.height(22.dp))
                        }
                    }
                }
            ) {
                Scaffold(
                    containerColor = colors.background,
                    topBar = {
                        NeumorphicTopBar(
                            title = page.title(user.role),
                            subtitle = "${user.name.ifBlank { "کاربر" }} · ${if (user.role == UserRole.TEACHER) "حساب معلم" else "حساب دانش‌آموز"}",
                            navigationIcon = if (user.role == UserRole.TEACHER) null else Icons.Outlined.Menu,
                            navigationDescription = if (user.role == UserRole.TEACHER) null else "بازکردن منو",
                            onNavigation = { scope.launch { drawerState.open() } }
                        )
                    },
                    bottomBar = {
                        if (user.role == UserRole.TEACHER) {
                            TeacherBottomDock(
                                active = page.teacherDockSection(),
                                onMenu = { scope.launch { drawerState.open() } },
                                onWallet = onWallet,
                                onCreateStudent = onCreateStudent,
                                onCreateExam = onCreateExam,
                                onCreateClass = onCreateClass,
                                onExams = onExams,
                                onStats = onStats,
                                onGrading = onGrading,
                                onPending = onPending
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
                        Box(
                            Modifier
                                .fillMaxSize()
                                .widthIn(max = 900.dp)
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

private fun MainPage.teacherDockSection(): TeacherDockSection = when (this) {
    MainPage.HOME -> TeacherDockSection.EXAMS
    MainPage.WALLET -> TeacherDockSection.WALLET
    MainPage.GRADING, MainPage.REPORTS -> TeacherDockSection.CARDS
    else -> TeacherDockSection.MENU
}

private fun MainPage.title(role: UserRole): String = when (this) {
    MainPage.HOME -> if (role == UserRole.TEACHER) "داشبورد معلم" else "داشبورد دانش‌آموز"
    MainPage.CALENDAR -> "تقویم و پیام‌ها"
    MainPage.SCHOOL -> "کلاس‌ها و دانش‌آموزان"
    MainPage.GRADING -> "تصحیح و حضور"
    MainPage.REPORTS -> "آمار و گزارش‌ها"
    MainPage.STUDENT_RESULTS -> "نتایج و پاسخ‌های من"
    MainPage.WALLET -> "کیف پول و پرداخت"
    MainPage.SETTINGS -> "پروفایل و تنظیمات"
    MainPage.ABOUT -> "درباره و بروزرسانی"
    MainPage.BUILDER -> "ساخت آزمون"
}
