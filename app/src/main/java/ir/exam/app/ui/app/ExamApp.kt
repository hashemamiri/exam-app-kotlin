package ir.exam.app.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
        onHome = { page = MainPage.HOME },
        onCalendar = { page = MainPage.CALENDAR },
        onSchool = { page = MainPage.SCHOOL },
        onGrading = { page = MainPage.GRADING },
        onReports = { page = MainPage.REPORTS },
        onStudentResults = { page = MainPage.STUDENT_RESULTS },
        onWallet = { page = MainPage.WALLET },
        onSettings = { page = MainPage.SETTINGS },
        onAbout = { page = MainPage.ABOUT },
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
            MainPage.SCHOOL -> if (user.role == UserRole.TEACHER) SchoolManagementScreen()
            MainPage.GRADING -> if (user.role == UserRole.TEACHER) GradingScreen()
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
    onHome: () -> Unit,
    onCalendar: () -> Unit,
    onSchool: () -> Unit,
    onGrading: () -> Unit,
    onReports: () -> Unit,
    onStudentResults: () -> Unit,
    onWallet: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onSignOut: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun select(action: () -> Unit) {
        action()
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                    ProfileAvatar(user.avatarUrl, user.name.ifBlank { "کاربر" }, 64)
                    Spacer(Modifier.height(12.dp))
                    Text("سامانه آزمون", style = MaterialTheme.typography.titleLarge)
                    Text(
                        user.name.ifBlank { "کاربر" },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        if (user.role == UserRole.TEACHER) "حساب معلم" else "حساب دانش‌آموز",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    label = {
                        Text(if (user.role == UserRole.TEACHER) "داشبورد معلم" else "داشبورد دانش‌آموز")
                    },
                    selected = page == MainPage.HOME,
                    onClick = { select(onHome) },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                NavigationDrawerItem(
                    label = { Text("تقویم و پیام‌ها") },
                    selected = page == MainPage.CALENDAR,
                    onClick = { select(onCalendar) },
                    icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                if (user.role == UserRole.TEACHER) {
                    NavigationDrawerItem(
                        label = { Text("کلاس‌ها و دانش‌آموزان") },
                        selected = page == MainPage.SCHOOL,
                        onClick = { select(onSchool) },
                        icon = { Icon(Icons.Outlined.Groups, contentDescription = null) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("تصحیح و حضور") },
                        selected = page == MainPage.GRADING,
                        onClick = { select(onGrading) },
                        icon = { Icon(Icons.Outlined.Assessment, contentDescription = null) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("آمار و گزارش‌ها") },
                        selected = page == MainPage.REPORTS,
                        onClick = { select(onReports) },
                        icon = { Icon(Icons.Outlined.BarChart, contentDescription = null) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("کیف پول و پرداخت") },
                        selected = page == MainPage.WALLET,
                        onClick = { select(onWallet) },
                        icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                } else {
                    NavigationDrawerItem(
                        label = { Text("نتایج و پاسخ‌های من") },
                        selected = page == MainPage.STUDENT_RESULTS,
                        onClick = { select(onStudentResults) },
                        icon = { Icon(Icons.Outlined.Assessment, contentDescription = null) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                NavigationDrawerItem(
                    label = { Text("پروفایل و تنظیمات") },
                    selected = page == MainPage.SETTINGS,
                    onClick = { select(onSettings) },
                    icon = { Icon(Icons.Outlined.ManageAccounts, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                NavigationDrawerItem(
                    label = { Text("درباره و بروزرسانی") },
                    selected = page == MainPage.ABOUT,
                    onClick = { select(onAbout) },
                    icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("خروج و تعویض حساب") },
                    selected = false,
                    onClick = { select(onSignOut) },
                    icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(page.title(user.role))
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "بازکردن منو")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                content()
            }
        }
    }
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
