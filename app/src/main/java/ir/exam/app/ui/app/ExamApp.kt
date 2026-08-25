package ir.exam.app.ui.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.exam.app.BuildConfig
import ir.exam.app.core.ui.AppearanceSettings
import ir.exam.app.core.update.ApkUpdateManager
import ir.exam.app.core.update.UpdateUseCase
import ir.exam.app.data.repository.SupabaseAppUpdateRepository
import ir.exam.app.data.repository.SupabaseAuthRepository
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.model.UserRole
import ir.exam.app.ui.auth.AuthViewModel
import ir.exam.app.ui.auth.SignInScreen
import ir.exam.app.ui.bank.QuestionBankScreen
import ir.exam.app.ui.billing.WalletScreen
import ir.exam.app.ui.builder.ExamBuilderScreen
import ir.exam.app.ui.builder.ExamBuilderViewModel
import ir.exam.app.ui.builder.BankQuestionOption
import ir.exam.app.ui.builder.ExamImportDraft
import ir.exam.app.ui.calendar.CalendarScreen
import ir.exam.app.ui.classes.SchoolLaunchAction
import ir.exam.app.ui.classes.SchoolManagementScreen
import ir.exam.app.ui.dashboard.TeacherDashboardScreen
import ir.exam.app.ui.dashboard.TeacherManagerRequestsScreen
import ir.exam.app.ui.grading.GradingScreen
import ir.exam.app.ui.manager.ManagerStatsScreen
import ir.exam.app.ui.manager.ManagerTeachersScreen
import ir.exam.app.ui.manager.ManagerTeacherClassScreen
import ir.exam.app.ui.profile.ProfileSettingsDestination
import ir.exam.app.ui.profile.ProfileSettingsScreen
import ir.exam.app.ui.profile.SettingsSection
import ir.exam.app.ui.reports.ReportsScreen
import ir.exam.app.ui.reports.StudentResultsScreen
import ir.exam.app.ui.security.AppLockGate
import ir.exam.app.ui.student.StudentHomeScreen
import ir.exam.app.ui.update.AboutScreen
import ir.exam.app.ui.update.UpdateViewModel

private enum class MainPage {
    HOME, CALENDAR, SCHOOL, QUESTION_BANK, GRADING, REPORTS, STUDENT_RESULTS,
    WALLET, CARDS, REQUESTS, SETTINGS, BUILDER
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
    var page by rememberSaveable(user.id) {
        mutableStateOf(if (user.role == UserRole.MANAGER) MainPage.HOME else MainPage.CALENDAR)
    }
    var menuOpen by rememberSaveable(user.id) { mutableStateOf(false) }
    // V58.0.2 — آزمون فعال دانش‌آموز: هدر و منوی همبرگری پنهان می‌شوند.
    var studentExamActive by rememberSaveable(user.id) { mutableStateOf(false) }
    var quickAddOpen by rememberSaveable(user.id) { mutableStateOf(false) }
    var walletRefreshKey by rememberSaveable(user.id) { mutableIntStateOf(0) }
    var dashboardRefreshKey by rememberSaveable(user.id) { mutableIntStateOf(0) }
    var managerNewTeacherKey by rememberSaveable(user.id) { mutableIntStateOf(0) }
    var managerTeacherListKey by rememberSaveable(user.id) { mutableIntStateOf(0) }
    var managerTeacherId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var managerInviteHeader by rememberSaveable(user.id) { mutableStateOf(false) }
    var cardsCycleKey by rememberSaveable(user.id) { mutableIntStateOf(0) }
    var editingExamId by remember(user.id) { mutableStateOf<String?>(null) }
    var importedExam by remember(user.id) { mutableStateOf<ExamImportDraft?>(null) }
    var schoolLaunchAction by remember(user.id) { mutableStateOf<SchoolLaunchAction?>(null) }
    var schoolStudentsSelected by rememberSaveable(user.id) { mutableStateOf(false) }
    var profileDestination by rememberSaveable(user.id) {
        mutableStateOf(ProfileSettingsDestination.SETTINGS)
    }
    var settingsInitialSection by rememberSaveable(user.id) {
        mutableStateOf(SettingsSection.APPEARANCE)
    }
    var gradingPendingOnly by remember(user.id) { mutableStateOf(false) }
    var gradingGradedOnly by remember(user.id) { mutableStateOf(false) }
    var showSignOut by remember(user.id) { mutableStateOf(false) }
    var studentExamDialog by rememberSaveable(user.id) { mutableStateOf(false) }
    var studentExamCode by rememberSaveable(user.id) { mutableStateOf("") }
    var studentJoinRequestKey by rememberSaveable(user.id) { mutableIntStateOf(0) }

    fun closeTransientNavigation() {
        menuOpen = false
        quickAddOpen = false
    }

    fun openHome() {
        closeTransientNavigation()
        if (user.role == UserRole.MANAGER) {
            managerInviteHeader = false
            managerTeacherId = null
            managerTeacherListKey += 1
        }
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
        schoolStudentsSelected = true
        schoolLaunchAction = SchoolLaunchAction.CREATE_STUDENT
        page = MainPage.SCHOOL
    }

    fun createManagerTeacher() {
        closeTransientNavigation()
        managerTeacherId = null
        managerInviteHeader = true
        managerNewTeacherKey += 1
        page = MainPage.HOME
    }

    fun createExam() {
        closeTransientNavigation()
        editingExamId = null
        importedExam = null
        page = MainPage.BUILDER
    }

    fun createClass() {
        closeTransientNavigation()
        schoolStudentsSelected = false
        schoolLaunchAction = SchoolLaunchAction.CREATE_CLASS
        page = MainPage.SCHOOL
    }

    LaunchedEffect(user.id, user.role) {
        val teacherOnly = setOf(
            MainPage.BUILDER,
            MainPage.SCHOOL,
            MainPage.QUESTION_BANK,
            MainPage.GRADING,
            MainPage.REPORTS,
            MainPage.WALLET,
            MainPage.CARDS,
            MainPage.REQUESTS
        )
        when (user.role) {
            UserRole.STUDENT -> if (page in teacherOnly) page = MainPage.HOME
            UserRole.TEACHER -> if (page == MainPage.STUDENT_RESULTS) page = MainPage.HOME
            UserRole.MANAGER -> if (page !in setOf(MainPage.HOME, MainPage.SCHOOL, MainPage.WALLET, MainPage.CARDS, MainPage.SETTINGS)) {
                page = MainPage.HOME
            }
        }
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
    val roleHomePage = if (user.role == UserRole.MANAGER) MainPage.HOME else MainPage.CALENDAR
    BackHandler(enabled = !menuOpen && !quickAddOpen && page != roleHomePage) {
        page = roleHomePage
    }

    AuthenticatedShell(
        user = user,
        page = page,
        appearance = appearance,
        profileDestination = profileDestination,
        schoolStudentsSelected = schoolStudentsSelected,
        managerInviteHeader = managerInviteHeader,
        menuOpen = menuOpen,
        quickAddOpen = quickAddOpen,
        studentExamActive = studentExamActive,
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
        onClasses = {
            closeTransientNavigation()
            schoolStudentsSelected = false
            schoolLaunchAction = SchoolLaunchAction.SHOW_CLASSES
            page = MainPage.SCHOOL
        },
        onStudents = {
            closeTransientNavigation()
            schoolStudentsSelected = true
            schoolLaunchAction = SchoolLaunchAction.SHOW_STUDENTS
            page = MainPage.SCHOOL
        },
        onStudentResults = { closeTransientNavigation(); page = MainPage.STUDENT_RESULTS },
        onWallet = ::openWallet,
        onCards = ::openCards,
        onProfile = {
            closeTransientNavigation()
            profileDestination = ProfileSettingsDestination.PROFILE
            page = MainPage.SETTINGS
        },
        onHeader = {
            closeTransientNavigation()
            profileDestination = ProfileSettingsDestination.HEADER
            page = MainPage.SETTINGS
        },
        onAccount = {
            closeTransientNavigation()
            profileDestination = ProfileSettingsDestination.ACCOUNT
            page = MainPage.SETTINGS
        },
        onData = {
            closeTransientNavigation()
            profileDestination = ProfileSettingsDestination.DATA
            page = MainPage.SETTINGS
        },
        onSettings = {
            closeTransientNavigation()
            profileDestination = ProfileSettingsDestination.SETTINGS
            settingsInitialSection = SettingsSection.APPEARANCE
            page = MainPage.SETTINGS
        },
        onCreateStudent = ::createStudent,
        onCreateExam = if (user.role == UserRole.MANAGER) ::createManagerTeacher else ::createExam,
        onCreateClass = ::createClass,
        onStudentExamJoin = { closeTransientNavigation(); studentExamDialog = true },
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
                    UserRole.STUDENT -> StudentHomeScreen(
                        userId = user.id,
                        initialJoinCode = studentExamCode.takeIf(String::isNotBlank),
                        joinRequestKey = studentJoinRequestKey,
                        onExamActiveChanged = { studentExamActive = it }
                    )
                    UserRole.MANAGER -> managerTeacherId?.let { teacherId ->
                        ManagerTeacherClassScreen(teacherId = teacherId, onBack = { managerTeacherId = null })
                    } ?: ManagerTeachersScreen(
                        newTeacherRequested = managerNewTeacherKey,
                        teacherListRequested = managerTeacherListKey,
                        inviteModeRequested = managerInviteHeader,
                        onManageTeacher = { managerTeacherId = it },
                        onInviteModeChanged = { managerInviteHeader = it }
                    )
                }
                MainPage.CALENDAR -> CalendarScreen(user.role)
                MainPage.SCHOOL -> if (user.role != UserRole.STUDENT) {
                    SchoolManagementScreen(
                        launchAction = schoolLaunchAction,
                        onLaunchActionConsumed = { schoolLaunchAction = null }
                    )
                }
                MainPage.QUESTION_BANK -> if (user.role == UserRole.TEACHER) {
                    QuestionBankScreen { item ->
                        editingExamId = null
                        importedExam = item.toExamImportDraft()
                        page = MainPage.BUILDER
                    }
                }
                MainPage.GRADING -> if (user.role == UserRole.TEACHER) {
                    GradingScreen(
                        initialPendingOnly = gradingPendingOnly,
                        initialGradedOnly = gradingGradedOnly
                    )
                }
                MainPage.REPORTS -> if (user.role == UserRole.TEACHER) ReportsScreen()
                MainPage.STUDENT_RESULTS -> if (user.role == UserRole.STUDENT) StudentResultsScreen()
                MainPage.WALLET -> when (user.role) {
                    UserRole.TEACHER -> WalletScreen(refreshKey = walletRefreshKey)
                    UserRole.MANAGER -> WalletScreen(refreshKey = walletRefreshKey)
                    UserRole.STUDENT -> Unit
                }
                MainPage.CARDS -> if (user.role == UserRole.MANAGER) {
                    ManagerStatsScreen()
                } else if (user.role == UserRole.TEACHER) {
                    TeacherManagementCardsScreen(
                        cycleKey = cardsCycleKey,
                        onStats = { page = MainPage.REPORTS },
                        onQuestionBank = { page = MainPage.QUESTION_BANK },
                        onGrading = {
                            gradingPendingOnly = false
                            gradingGradedOnly = false
                            page = MainPage.GRADING
                        },
                        onPending = {
                            gradingPendingOnly = true
                            gradingGradedOnly = false
                            page = MainPage.GRADING
                        },
                        onAnswers = {
                            gradingPendingOnly = false
                            gradingGradedOnly = true
                            page = MainPage.GRADING
                        },
                        onRequests = { page = MainPage.REQUESTS }
                    )
                }
                MainPage.REQUESTS -> if (user.role == UserRole.TEACHER) TeacherManagerRequestsScreen()
                MainPage.SETTINGS -> ProfileSettingsScreen(
                    user = user,
                    appearance = appearance,
                    destination = profileDestination,
                    initialSettingsSection = settingsInitialSection,
                    onProfileUpdated = authViewModel::refreshCurrentUser,
                    // V59.3 — refreshCurrentUser برای حساب حذف‌شده شکست می‌خورد و
                    // کاربر پشت صفحهٔ مرده می‌ماند؛ خروج محلی صفحهٔ ورود را می‌آورد.
                    onAccountDeleted = authViewModel::signOut,
                    onImportExam = { draft ->
                        editingExamId = null
                        importedExam = draft
                        page = MainPage.BUILDER
                    },
                    aboutContent = { AboutScreen(updateViewModel, apkUpdateManager) }
                )
                MainPage.BUILDER -> Unit
            }
        }
    }

    if (studentExamDialog && user.role == UserRole.STUDENT) {
        AlertDialog(
            onDismissRequest = { studentExamDialog = false },
            title = { Text("پیوستن به آزمون") },
            text = {
                OutlinedTextField(
                    value = studentExamCode,
                    onValueChange = { studentExamCode = it.uppercase().filter { c -> c in 'A'..'Z' || c.isDigit() }.take(12) },
                    label = { Text("کد آزمون") },
                    trailingIcon = {
                        IconButton(
                            enabled = studentExamCode.length in 4..12,
                            onClick = {
                                studentExamDialog = false
                                menuOpen = false
                                page = MainPage.HOME
                                studentJoinRequestKey += 1
                            }
                        ) { Icon(Icons.Outlined.Search, contentDescription = "جست‌وجوی آزمون") }
                    },
                    singleLine = true
                )
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { studentExamDialog = false }) { Text("انصراف") } }
        )
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

    // ورود به برنامه: اگر آپدیت جدید موجود باشد، یک پیغام روی صفحه ظاهر می‌شود.
    // این پنجره برخلاف نسخه قدیمی، هنگام دانلود باز می‌ماند و پیشرفت، «در انتظار
    // شبکه» و خطای واقعی را نشان می‌دهد؛ پس از دریافت هم نصب‌کننده خودکار باز می‌شود.
    val updateState by updateViewModel.state.collectAsState()
    var updatePromptDismissed by rememberSaveable(user.id) { mutableStateOf(false) }
    LaunchedEffect(user.id) { updateViewModel.check(BuildConfig.VERSION_CODE) }

    val latestApkPath by rememberUpdatedState(updateState.downloadedApkPath)
    fun openInstaller(path: String) {
        apkUpdateManager.launchInstaller(path)
            .onSuccess { updateViewModel.reportInstallerOpened() }
            .onFailure(updateViewModel::reportInstallError)
    }
    val updateInstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val path = latestApkPath
        if (path != null && apkUpdateManager.canRequestPackageInstalls()) {
            openInstaller(path)
        } else if (path != null) {
            updateViewModel.reportPermissionRequired()
        }
    }
    fun requestInstaller(path: String) {
        if (apkUpdateManager.canRequestPackageInstalls()) {
            openInstaller(path)
        } else {
            updateViewModel.reportPermissionRequired()
            try {
                updateInstallLauncher.launch(apkUpdateManager.unknownSourcesSettingsIntent())
            } catch (error: ActivityNotFoundException) {
                updateViewModel.reportInstallError(error)
            }
        }
    }

    // دانلود کامل شد → نصب‌کننده را خودکار باز کن (همان رفتار صفحه «درباره»).
    LaunchedEffect(updateState.autoInstallPending, updateState.downloadedApkPath) {
        val path = updateState.downloadedApkPath
        if (updateState.autoInstallPending && path != null) {
            updateViewModel.markAutoInstallHandled()
            requestInstaller(path)
        }
    }

    updateState.update?.takeIf { remote ->
        !updatePromptDismissed && updateState.downloadedApkPath == null
    }?.let { remote ->
        UpdatePromptDialog(
            remoteName = remote.name,
            notes = remote.notesFa.take(3),
            downloading = updateState.downloading,
            downloadFraction = updateState.downloadFraction,
            progressText = updateState.progressText,
            message = updateState.message,
            error = updateState.error,
            onDownload = updateViewModel::downloadAndInstall,
            onDismiss = { updatePromptDismissed = true },
            onBrowserFallback = {
                updatePromptDismissed = true
                appContext.openUrlSafely(remote.apkUrl, onFailure = updateViewModel::reportInstallError)
            }
        )
    }
}

/**
 * پنجره دریافت نسخه جدید با سه حالت:
 * ۱) آماده دریافت → دکمه «دریافت نسخه» + «بعداً»
 * ۲) در حال دانلود → نوار پیشرفت/متن «در انتظار اتصال اینترنت…» + «پنهان‌کردن»
 * ۳) خطا → متن خطای واقعی + «تلاش دوباره» + «دریافت با مرورگر»
 */
@Composable
private fun UpdatePromptDialog(
    remoteName: String,
    notes: List<String>,
    downloading: Boolean,
    downloadFraction: Float?,
    progressText: String?,
    message: String?,
    error: String?,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onBrowserFallback: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = {
            Text(if (downloading) "در حال دریافت نسخه $remoteName" else "بروزرسانی جدید")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!downloading) {
                    Text("نسخه $remoteName آماده دریافت است.")
                    notes.forEach { note ->
                        Text("• $note", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    downloadFraction?.let {
                        LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth())
                    } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    progressText?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            when {
                downloading -> TextButton(onClick = onDismiss) { Text("پنهان‌کردن") }
                error != null -> Button(onClick = onDownload) { Text("تلاش دوباره") }
                else -> Button(onClick = onDownload) { Text("دریافت نسخه") }
            }
        },
        dismissButton = {
            when {
                downloading -> Unit
                error != null -> TextButton(onClick = onBrowserFallback) { Text("دریافت با مرورگر") }
                else -> TextButton(onClick = onDismiss) { Text("بعداً") }
            }
        }
    )
}

/** بازکردن امن نشانی در مرورگر به‌عنوان مسیر جایگزین وقتی دانلودکننده سیستم ناموفق است. */
private fun android.content.Context.openUrlSafely(url: String, onFailure: (Throwable) -> Unit) {
    runCatching {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure(onFailure)
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
    profileDestination: ProfileSettingsDestination,
    schoolStudentsSelected: Boolean,
    managerInviteHeader: Boolean,
    menuOpen: Boolean,
    quickAddOpen: Boolean,
    studentExamActive: Boolean = false,
    onToggleMenu: () -> Unit,
    onToggleAdd: () -> Unit,
    onCloseAdd: () -> Unit,
    onHome: () -> Unit,
    onCalendar: () -> Unit,
    onClasses: () -> Unit,
    onStudents: () -> Unit,
    onStudentResults: () -> Unit,
    onWallet: () -> Unit,
    onCards: () -> Unit,
    onProfile: () -> Unit,
    onHeader: () -> Unit,
    onAccount: () -> Unit,
    onData: () -> Unit,
    onSettings: () -> Unit,
    onCreateStudent: () -> Unit,
    onCreateExam: () -> Unit,
    onCreateClass: () -> Unit,
    onStudentExamJoin: () -> Unit,
    onSignOut: () -> Unit,
    content: @Composable () -> Unit
) {
    fun select(action: () -> Unit) = action()

    val menuCards = if (user.role == UserRole.TEACHER) {
        listOf(
            Design69MenuCard(
                "دانش‌آموزان", "فهرست و وضعیت", Design69Icons.Students,
                page == MainPage.SCHOOL && schoolStudentsSelected,
                onClick = { select(onStudents) }
            ),
            Design69MenuCard(
                "کلاس‌ها", "فهرست و مدیریت", Design69Icons.Classes,
                page == MainPage.SCHOOL && !schoolStudentsSelected,
                onClick = { select(onClasses) }
            ),
            Design69MenuCard(
                "تقویم", "رویدادها و پیام‌ها", Design69Icons.Calendar,
                page == MainPage.CALENDAR, onClick = { select(onCalendar) }
            ),
            Design69MenuCard(
                "سربرگ", "اطلاعات رسمی چاپ آزمون", Design69Icons.Header,
                page == MainPage.SETTINGS && profileDestination == ProfileSettingsDestination.HEADER,
                onClick = { select(onHeader) }
            ),
            Design69MenuCard(
                "حساب", "مشخصات و امنیت حساب", Design69Icons.Account,
                page == MainPage.SETTINGS && profileDestination == ProfileSettingsDestination.ACCOUNT,
                onClick = { select(onAccount) }
            ),
            Design69MenuCard(
                "داده‌ها", "پشتیبان و بازیابی داده‌ها", Design69Icons.Data,
                page == MainPage.SETTINGS && profileDestination == ProfileSettingsDestination.DATA,
                onClick = { select(onData) }
            ),
            Design69MenuCard(
                "تنظیمات", "ظاهر و فهرست تغییرات", Design69Icons.Settings,
                page == MainPage.SETTINGS && profileDestination == ProfileSettingsDestination.SETTINGS,
                onClick = { select(onSettings) }
            ),
            Design69MenuCard(
                "خروج", "خروج امن و تعویض حساب", Design69Icons.Logout,
                danger = true, onClick = { select(onSignOut) }
            )
        )
    } else if (user.role == UserRole.MANAGER) {
        // مدیر/معاون عمداً تقویم و سربرگ ندارد.
        listOf(
            Design69MenuCard(
                "کلاس‌ها", "فهرست و مدیریت کلاس‌های مدرسه", Design69Icons.Classes,
                page == MainPage.SCHOOL && !schoolStudentsSelected,
                onClick = { select(onClasses) }
            ),
            Design69MenuCard(
                "دانش‌آموزان", "فهرست و مدیریت دانش‌آموزان مدرسه", Design69Icons.Students,
                page == MainPage.SCHOOL && schoolStudentsSelected,
                onClick = { select(onStudents) }
            ),
            Design69MenuCard(
                "حساب", "مشخصات و امنیت حساب", Design69Icons.Account,
                page == MainPage.SETTINGS && profileDestination == ProfileSettingsDestination.ACCOUNT,
                onClick = { select(onAccount) }
            ),
            Design69MenuCard(
                "داده‌ها", "داده‌های مدرسه", Design69Icons.Data,
                page == MainPage.SETTINGS && profileDestination == ProfileSettingsDestination.DATA,
                onClick = { select(onData) }
            ),
            Design69MenuCard(
                "تنظیمات", "ظاهر و فهرست تغییرات", Design69Icons.Settings,
                page == MainPage.SETTINGS && profileDestination == ProfileSettingsDestination.SETTINGS,
                onClick = { select(onSettings) }
            ),
            Design69MenuCard(
                "خروج", "خروج امن و تعویض حساب", Design69Icons.Logout,
                danger = true, onClick = { select(onSignOut) }
            )
        )
    } else {
        // ترتیب دقیق دانش‌آموز: آزمون/نتایج، تقویم/حساب، تنظیمات/خروج؛ بدون داده‌ها.
        listOf(
            Design69MenuCard(
                "آزمون", "ورود با کد آزمون", Design69Icons.Exams,
                onClick = { select(onStudentExamJoin) }
            ),
            Design69MenuCard(
                "نتایج من", "پاسخ‌ها و کارنامه", Design69Icons.Reports,
                page == MainPage.STUDENT_RESULTS, onClick = { select(onStudentResults) }
            ),
            Design69MenuCard(
                "تقویم", "رویدادها و پیام‌ها", Design69Icons.Calendar,
                page == MainPage.CALENDAR, onClick = { select(onCalendar) }
            ),
            Design69MenuCard(
                "حساب", "مشخصات و امنیت حساب", Design69Icons.Account,
                page == MainPage.SETTINGS && profileDestination == ProfileSettingsDestination.ACCOUNT,
                onClick = { select(onAccount) }
            ),
            Design69MenuCard(
                "تنظیمات", "ظاهر و فهرست تغییرات", Design69Icons.Settings,
                page == MainPage.SETTINGS && profileDestination == ProfileSettingsDestination.SETTINGS,
                onClick = { select(onSettings) }
            ),
            Design69MenuCard(
                "خروج", "خروج امن و تعویض حساب", Design69Icons.Logout,
                danger = true, onClick = { select(onSignOut) }
            )
        )
    }

    Neumorphic69Provider(depth = appearance.neumorphicDepth) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val colors = neumorphic69Colors
            Box(Modifier.fillMaxSize().background(colors.background)) {
                Scaffold(
                    containerColor = colors.background,
                    topBar = {
                        // V58.0.2 — در حین آزمون دانش‌آموز، هدر «خانه دانش‌آموز» و
                        // دکمهٔ منوی همبرگری حذف می‌شوند (درخواست کاربر).
                        if (!menuOpen && !(user.role == UserRole.STUDENT && studentExamActive)) {
                            TopAppBar(
                                title = {
                                    Text(
                                        if (user.role == UserRole.MANAGER && page == MainPage.HOME && managerInviteHeader) "کدهای دعوت معلم"
                                        else page.sectionTitle(user.role, profileDestination, schoolStudentsSelected)
                                    )
                                },
                                navigationIcon = {
                                    if (user.role == UserRole.STUDENT) {
                                        IconButton(onClick = onToggleMenu) {
                                            Design69MorphingMenuIcon(
                                                open = false,
                                                tint = colors.muted,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (user.role != UserRole.STUDENT) {
                            TeacherBottomDock(
                                active = page.teacherDockSection(),
                                menuOpen = menuOpen,
                                quickAddOpen = quickAddOpen,
                                onMenu = onToggleMenu,
                                onWallet = onWallet,
                                onAdd = onToggleAdd,
                                onExams = onHome,
                                onCards = onCards,
                                primaryLabel = if (user.role == UserRole.MANAGER) "معلم‌ها" else "آزمون‌ها",
                                primaryIcon = if (user.role == UserRole.MANAGER) {
                                    Design69Icons.Students
                                } else Design69Icons.Exams
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
                                enter = fadeIn(tween(110)),
                                exit = fadeOut(tween(90))
                            ) {
                                Box(Modifier.fillMaxSize().background(colors.background)) {
                                    Design69MainMenuScreen(
                                        user = user,
                                        cards = menuCards,
                                        onProfile = onProfile
                                    )
                                }
                            }
                        }
                    }
                }

                if (quickAddOpen && user.role != UserRole.STUDENT) {
                    Design69QuickAddOverlay(
                        onDismiss = onCloseAdd,
                        onCreateStudent = onCreateStudent,
                        onCreateExam = onCreateExam,
                        onCreateClass = onCreateClass,
                        primaryTitle = if (user.role == UserRole.MANAGER) "دعوت معلم" else "آزمون جدید",
                        primaryIcon = if (user.role == UserRole.MANAGER) Design69Icons.PersonAdd else Design69Icons.ExamAdd
                    )
                }
            }
        }
    }
}

private fun MainPage.sectionTitle(
    role: UserRole,
    profileDestination: ProfileSettingsDestination,
    schoolStudentsSelected: Boolean
): String = when (this) {
    MainPage.HOME -> when (role) {
        UserRole.TEACHER -> "آزمون‌ها"
        UserRole.MANAGER -> "معلم‌ها"
        UserRole.STUDENT -> "خانه دانش‌آموز"
    }
    MainPage.CALENDAR -> "تقویم"
    MainPage.SCHOOL -> if (schoolStudentsSelected) "دانش‌آموزان" else "کلاس‌ها"
    MainPage.QUESTION_BANK -> "بانک سؤال"
    MainPage.GRADING -> "تصحیح پاسخ‌ها"
    MainPage.REPORTS -> "گزارش‌ها"
    MainPage.STUDENT_RESULTS -> "نتایج من"
    MainPage.WALLET -> "کیف پول"
    MainPage.CARDS -> if (role == UserRole.MANAGER) "آمار مدرسه" else "مدیریت"
    MainPage.REQUESTS -> "درخواست‌ها"
    MainPage.SETTINGS -> when (profileDestination) {
        ProfileSettingsDestination.PROFILE -> "پروفایل"
        ProfileSettingsDestination.HEADER -> "سربرگ"
        ProfileSettingsDestination.ACCOUNT -> "حساب"
        ProfileSettingsDestination.DATA -> "داده‌ها"
        ProfileSettingsDestination.SETTINGS -> "تنظیمات"
    }
    MainPage.BUILDER -> "ساخت آزمون"
}

private fun MainPage.teacherDockSection(): TeacherDockSection = when (this) {
    MainPage.HOME -> TeacherDockSection.EXAMS
    MainPage.WALLET -> TeacherDockSection.WALLET
    MainPage.CARDS, MainPage.REQUESTS, MainPage.QUESTION_BANK, MainPage.GRADING, MainPage.REPORTS ->
        TeacherDockSection.CARDS
    else -> TeacherDockSection.NONE
}

private fun BankQuestionOption.toExamImportDraft(): ExamImportDraft = ExamImportDraft(
    title = "آزمون جدید از بانک سؤال",
    subject = subject.orEmpty(),
    durationMinutes = 0,
    negativeMarking = 0.0,
    shuffleQuestions = false,
    shuffleOptions = false,
    teacherMessage = "",
    attemptsAllowed = 1,
    attemptOnTimeout = false,
    gradePolicy = "last",
    attemptCooldown = 0,
    questions = listOf(question.copy(id = java.util.UUID.randomUUID().toString())),
    exportedBy = "بانک سؤال"
)
