package ir.exam.app.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.School
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.exam.app.core.update.ApkUpdateManager
import ir.exam.app.core.update.UpdateUseCase
import ir.exam.app.data.repository.SupabaseAppUpdateRepository
import ir.exam.app.data.repository.SupabaseAuthRepository
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.model.UserRole
import ir.exam.app.ui.auth.AuthViewModel
import ir.exam.app.ui.auth.SignInScreen
import ir.exam.app.ui.builder.ExamBuilderScreen
import ir.exam.app.ui.builder.ExamBuilderViewModel
import ir.exam.app.ui.dashboard.TeacherDashboardScreen
import ir.exam.app.ui.student.StudentHomeScreen
import ir.exam.app.ui.update.AboutScreen
import ir.exam.app.ui.update.UpdateViewModel
import kotlinx.coroutines.launch

private enum class MainPage { HOME, ABOUT, BUILDER }

@Composable
fun ExamApp() {
    val authViewModel = remember { AuthViewModel(SupabaseAuthRepository()) }
    val authState by authViewModel.state.collectAsState()
    val user = authState.user

    if (user == null) {
        SignInScreen(viewModel = authViewModel)
        return
    }

    val appContext = LocalContext.current.applicationContext
    val apkUpdateManager = remember(appContext) { ApkUpdateManager(appContext) }
    val updateViewModel = remember(user.id) {
        UpdateViewModel(
            useCase = UpdateUseCase(SupabaseAppUpdateRepository()),
            apkUpdateManager = apkUpdateManager
        )
    }
    val builderViewModel = remember(user.id) { ExamBuilderViewModel() }
    var page by remember(user.id) { mutableStateOf(MainPage.HOME) }

    LaunchedEffect(user.id, user.role) {
        if (user.role != UserRole.TEACHER && page == MainPage.BUILDER) {
            page = MainPage.HOME
        }
    }

    if (page == MainPage.BUILDER && user.role == UserRole.TEACHER) {
        ExamBuilderScreen(
            viewModel = builderViewModel,
            onBack = { page = MainPage.HOME }
        )
        return
    }

    BackHandler(enabled = page == MainPage.ABOUT) {
        page = MainPage.HOME
    }

    AuthenticatedDrawer(
        user = user,
        page = page,
        onHome = { page = MainPage.HOME },
        onAbout = { page = MainPage.ABOUT }
    ) {
        when (page) {
            MainPage.HOME -> when (user.role) {
                UserRole.TEACHER -> TeacherDashboardScreen(
                    onCreateExam = { page = MainPage.BUILDER }
                )
                UserRole.STUDENT -> StudentHomeScreen()
            }
            MainPage.ABOUT -> AboutScreen(
                viewModel = updateViewModel,
                apkUpdateManager = apkUpdateManager
            )
            MainPage.BUILDER -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedDrawer(
    user: AppUser,
    page: MainPage,
    onHome: () -> Unit,
    onAbout: () -> Unit,
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
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                    label = { Text("درباره و بروزرسانی") },
                    selected = page == MainPage.ABOUT,
                    onClick = { select(onAbout) },
                    icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (page == MainPage.ABOUT) {
                                "درباره و بروزرسانی"
                            } else if (user.role == UserRole.TEACHER) {
                                "داشبورد معلم"
                            } else {
                                "داشبورد دانش‌آموز"
                            }
                        )
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
