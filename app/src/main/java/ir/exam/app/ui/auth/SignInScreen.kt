package ir.exam.app.ui.auth

import ir.exam.app.core.network.UserFacingError
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ir.exam.app.ui.common.PasswordVisibilityButton
import ir.exam.app.ui.common.passwordTransformation

/** برچسب مراحل بازیابی رمز (عیناً از ماژول یخی). */
private val RecoverySteps = listOf("ایمیل", "کد بازیابی", "رمز جدید")

/**
 * ورود، ثبت‌نام کادر مدرسه و بازیابی رمز؛ هیچ مسیر آزمایشی یا عبور مستقیم ندارد.
 *
 * V62.1 — چیدمان عیناً مثل ماژول azmoon-auth-compose:
 * - صفحهٔ خوش‌آمد با لوگوی گرادیانی بزرگ و «ورود به حساب» / «ساخت حساب جدید».
 * - ورود هر سه نقش در یک کارت با تب‌های سگمنتی لغزان؛ ثبت‌نام دو نقش با تب.
 * - بازیابی رمز: نوار مراحل با تیک انیمیشنی داخل کارت + برف؛ کد در OtpBoxes.
 * V62.1.2 — گزارش دستگاه: ورود پلکانی هر آیتم (StaggeredItem مثل ماژول)
 * جایگزین StaggeredEntranceِ یک‌جای کل کارت شد که نامحسوس بود.
 * تمام منطق (گوگل Credential Manager، نام کاربری کادر، کد دعوت ۶ حرفی/TCH،
 * قواعد رمز ۸ تا ۷۲، کد ۶ تا ۸ رقمی سوپابیس) همان مسیر تست‌شدهٔ
 * AuthViewModel/SupabaseAuthRepository است؛ فقط پوسته عوض شده است.
 */
@Composable
fun SignInScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()
    // V237 — حساب گوگلِ موجود: «ورود به‌عنوان …» / «انصراف»
    state.roleChoice?.let { choice ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::cancelRoleChoice,
            title = { Text("حساب از قبل وجود دارد") },
            text = { Text(choice.message) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = viewModel::confirmRoleChoice) { Text(choice.loginLabel) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = viewModel::cancelRoleChoice) { Text("انصراف") } }
        )
    }

    // V62.0 — پوستهٔ «یخی قطبی»: گرادیان با هاله و موج سه‌لایه، کارت شیشه‌ای،
    // برف در جریان بازیابی رمز.
    val recoveryFlow = state.screen in setOf(
        AuthScreen.RECOVERY, AuthScreen.RECOVERY_OTP, AuthScreen.RECOVERY_PASSWORD
    )
    Box(Modifier.fillMaxSize()) {
        IceBackdrop(Modifier.fillMaxSize())
        if (recoveryFlow) Snowfall(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            when (state.screen) {
                // خوش‌آمد ماژول: لوگو و دکمه‌ها مستقیم روی پس‌زمینه (مثل WelcomeScreen).
                AuthScreen.SIGN_IN -> LandingPane(state, viewModel)
                else -> IceAuthCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (state.screen) {
                            AuthScreen.SIGN_IN -> Unit
                            AuthScreen.LOGIN_ROLE,
                            AuthScreen.LOGIN_MANAGER,
                            AuthScreen.LOGIN_TEACHER,
                            AuthScreen.LOGIN_STUDENT -> LoginPane(state, viewModel)
                            AuthScreen.REGISTRATION_ROLE,
                            AuthScreen.TEACHER_REGISTER,
                            AuthScreen.MANAGER_REGISTER -> RegisterPane(state, viewModel)
                            AuthScreen.LOGIN_OTP -> OtpPane(
                                title = "ورود با کد یک‌بارمصرف",
                                hint = "کد ارسال‌شده به ${state.email} را وارد کنید.",
                                state = state,
                                onVerify = viewModel::verifyLoginOtp,
                                onResend = viewModel::sendLoginOtp,
                                onBack = viewModel::showLoginRole,
                                viewModel = viewModel
                            )
                            AuthScreen.TEACHER_REGISTER_OTP -> OtpPane(
                                title = "تأیید ایمیل معلم",
                                hint = "کد ارسال‌شده به ${state.email} را وارد کنید.",
                                state = state,
                                onVerify = viewModel::verifyTeacherRegistrationOtp,
                                onResend = viewModel::sendTeacherRegistrationOtp,
                                onBack = viewModel::showTeacherRegistration,
                                viewModel = viewModel
                            )
                            AuthScreen.TEACHER_REGISTER_SETUP -> TeacherSetupPane(state, viewModel)
                            AuthScreen.MANAGER_REGISTER_OTP -> OtpPane(
                                title = "تأیید ایمیل مدیر/معاون",
                                hint = "کد ارسال‌شده به ${state.email} را وارد کنید.",
                                state = state,
                                onVerify = viewModel::verifyManagerRegistrationOtp,
                                onResend = viewModel::sendManagerRegistrationOtp,
                                onBack = viewModel::showManagerRegistration,
                                viewModel = viewModel
                            )
                            AuthScreen.MANAGER_REGISTER_SETUP -> ManagerSetupPane(state, viewModel)
                            AuthScreen.RECOVERY -> RecoveryPane(state, viewModel)
                            AuthScreen.RECOVERY_OTP -> OtpPane(
                                title = "بررسی کد بازیابی",
                                hint = "کد ارسال‌شده به ${state.email} را وارد کنید.",
                                state = state,
                                onVerify = viewModel::verifyRecoveryOtp,
                                onResend = viewModel::sendRecoveryOtp,
                                onBack = viewModel::showRecovery,
                                viewModel = viewModel,
                                recoverySteps = true
                            )
                            AuthScreen.RECOVERY_PASSWORD -> RecoveryPasswordPane(state, viewModel)
                        }
                    }
                }
            }
            state.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    "خطا: $it",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }
    }
}

/**
 * V62.1 — صفحهٔ خوش‌آمد عیناً مثل WelcomeScreen ماژول: لوگوی گرادیانی بزرگ،
 * نام اپ، پیام خوش‌آمد، «ورود به حساب» و «ساخت حساب جدید» و یادآوری دانش‌آموز.
 */
@Composable
private fun LandingPane(state: AuthUiState, viewModel: AuthViewModel) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StaggeredItem(0) { BrandHero() }
        Spacer(Modifier.height(12.dp))
        StaggeredItem(1) {
            Text("آزمون آنلاین", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = IceInk)
        }
        StaggeredItem(2) {
            Text(
                "به سامانهٔ آزمون و ارزشیابی خوش آمدید",
                fontSize = 13.sp,
                color = IceTextSecondary
            )
        }
        Spacer(Modifier.height(26.dp))
        StaggeredItem(3) {
            IceButton(
                text = "ورود به حساب",
                enabled = !state.isLoading,
                onClick = viewModel::showLoginRole
            )
        }
        Spacer(Modifier.height(10.dp))
        StaggeredItem(4) {
            IceOutlinedButton(
                text = "ساخت حساب جدید",
                enabled = !state.isLoading,
                onClick = viewModel::showTeacherRegistration
            )
        }
        Spacer(Modifier.height(18.dp))
        StaggeredItem(5) {
            Text(
                "حساب دانش‌آموز را معلم می‌سازد؛ نام کاربری و رمز را از معلم خود دریافت کنید.",
                fontSize = 11.5.sp,
                color = IceTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * V62.1 — ورود سه‌نقشه در یک کارت مثل LoginScreen ماژول: Brand + تب‌های
 * سگمنتی لغزان؛ هر تب همان پنجرهٔ اختصاصی نقش (منطق V61.0) را نشان می‌دهد و
 * جابه‌جایی تب فقط صفحهٔ ViewModel را عوض می‌کند (LOGIN_ROLE = تب مدیر/معاون).
 */
@Composable
private fun LoginPane(state: AuthUiState, viewModel: AuthViewModel) {
    val selectedTab = when (state.screen) {
        AuthScreen.LOGIN_TEACHER -> 1
        AuthScreen.LOGIN_STUDENT -> 2
        else -> 0
    }
    StaggeredItem(0) { Brand() }
    StaggeredItem(1) {
        RoleTabs(
            labels = listOf("مدیر/معاون", "معلم", "دانش‌آموز"),
            selected = selectedTab,
            onSelect = { index ->
                when (index) {
                    0 -> viewModel.showManagerLogin()
                    1 -> viewModel.showTeacherLogin()
                    else -> viewModel.showStudentLogin()
                }
            }
        )
    }
    // V62.1.3 — گزارش دستگاه: بین مدیر↔معلم انیمیشن اجرا نمی‌شد. ریشه:
    // هر دو تب همان StaffLoginPane هستند و Compose آن را remount نمی‌کند؛
    // LaunchedEffect(Unit) داخل StaggeredItem هم فقط بار اول می‌دود. با
    // key(selectedTab) هویت محتوا با هر تب عوض و stagger از نو اجرا می‌شود.
    androidx.compose.runtime.key(selectedTab) {
        if (selectedTab == 2) {
            StudentLoginPane(state, viewModel)
        } else {
            StaffLoginPane(state, viewModel, managerRole = selectedTab == 0)
        }
    }
    StaggeredItem(9) { BackButtonRow(onBack = viewModel::showSignIn, enabled = !state.isLoading) }
}

/**
 * V61.0/V62.1 — محتوای ورود معلم یا مدیر/معاون + «ورود با گوگل» برای
 * حساب‌هایی که قبلاً با جیمیل ثبت‌نام کرده‌اند (جیمیل جدید به تکمیل ثبت‌نام می‌رود).
 */
@Composable
private fun StaffLoginPane(state: AuthUiState, viewModel: AuthViewModel, managerRole: Boolean) {
    StaggeredItem(2) {
        Text(
            if (managerRole) "ورود مدیر/معاون" else "ورود معلم",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = IceInk
        )
    }
    // V61.0 — ورود با گوگل؛ همان جریان Credential Manager ثبت‌نام (idToken). V237 — بالای فرم.
    StaggeredItem(3) {
        GoogleAuthButton(
            state = state,
            viewModel = viewModel,
            role = if (managerRole) "manager" else "teacher"
        ) { Text("ورود با گوگل") }
    }
    StaggeredItem(4) {
        IceField(
            value = state.email,
            onValueChange = viewModel::setEmail,
            hint = if (managerRole) "ایمیل یا نام کاربری مدیر/معاون" else "ایمیل یا نام کاربری معلم",
            // V132 — نام کاربریِ انتخاب‌شده در ثبت‌نام هم برای ورود کافی است.
            supporting = "می‌توانید به‌جای ایمیل، نام کاربری انگلیسی که هنگام ثبت‌نام انتخاب کردید را وارد کنید.",
            keyboardType = KeyboardType.Email
        )
    }
    StaggeredItem(5) { PasswordField("رمز عبور", state.password, viewModel::setPassword) }
    StaggeredItem(6) {
        IceButton(
            text = "ورود با رمز عبور",
            onClick = viewModel::signIn,
            enabled = state.email.isNotBlank() && state.password.isNotBlank(),
            loading = state.isLoading
        )
    }
    StaggeredItem(7) {
        IceOutlinedButton(
            text = "ورود با کد ایمیل",
            onClick = viewModel::sendLoginOtp,
            enabled = !state.isLoading && '@' in state.email
        )
    }
    StaggeredItem(8) {
        LinkTextButton("رمز را فراموش کرده‌ام", onClick = viewModel::showRecovery, enabled = !state.isLoading)
    }
}

/** V61.0/V62.1 — محتوای ورود دانش‌آموز با نام کاربری تحویلی معلم. */
@Composable
private fun StudentLoginPane(state: AuthUiState, viewModel: AuthViewModel) {
    StaggeredItem(2) {
        Text("ورود دانش‌آموز", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = IceInk)
    }
    StaggeredItem(3) {
        IceField(
            value = state.email,
            onValueChange = viewModel::setEmail,
            hint = "نام کاربری دانش‌آموز",
            supporting = "همان نام کاربری تحویلی از معلم را وارد کنید."
        )
    }
    StaggeredItem(4) { PasswordField("رمز عبور", state.password, viewModel::setPassword) }
    StaggeredItem(5) {
        IceButton(
            text = "ورود",
            onClick = viewModel::signIn,
            enabled = state.email.isNotBlank() && state.password.isNotBlank(),
            loading = state.isLoading
        )
    }
}

/** V61.0/V62.1 — دکمهٔ بازگشت وسط‌چین پایین همهٔ پنجره‌ها (لینک خاکستری ماژول). */
@Composable
private fun BackButtonRow(onBack: () -> Unit, enabled: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        LinkTextButton("بازگشت", onClick = onBack, enabled = enabled, gray = true)
    }
}

/**
 * V62.1 — ثبت‌نام دو نقش در یک کارت مثل SignupScreen ماژول: تب معلم اول و
 * مدیر/معاون دوم؛ حساب دانش‌آموز را معلم می‌سازد.
 */
@Composable
private fun RegisterPane(state: AuthUiState, viewModel: AuthViewModel) {
    val managerTab = state.screen == AuthScreen.MANAGER_REGISTER
    StaggeredItem(0) { Brand() }
    StaggeredItem(1) {
        RoleTabs(
            labels = listOf("معلم", "مدیر/معاون"),
            selected = if (managerTab) 1 else 0,
            onSelect = { index ->
                if (index == 0) viewModel.showTeacherRegistration()
                else viewModel.showManagerRegistration()
            }
        )
    }
    // V62.1.3 — مثل تب‌های ورود: بازاجرای stagger با هر تعویض تب.
    androidx.compose.runtime.key(managerTab) {
        if (managerTab) ManagerRegistrationPane(state, viewModel)
        else TeacherRegistrationPane(state, viewModel)
    }
}

@Composable
private fun TeacherRegistrationPane(state: AuthUiState, viewModel: AuthViewModel) {
    StaggeredItem(2) {
        Text("ثبت‌نام معلم", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = IceInk)
    }
    StaggeredItem(3) {
        Text(
            "دانش‌آموز نباید از این بخش ثبت‌نام کند؛ حساب دانش‌آموز را معلم می‌سازد.",
            fontSize = 13.sp,
            color = IceTextSecondary,
            lineHeight = 22.sp
        )
    }
    // V60.0 — ثبت‌نام با گوگل: انتخاب جیمیل ثبت‌شده روی گوشی (Credential Manager). V237 — بالای فرم.
    StaggeredItem(4) { GoogleRegisterButton(state = state, viewModel = viewModel, role = "teacher") }
    StaggeredItem(5) { IceField(state.fullName, viewModel::setFullName, hint = "نام و نام خانوادگی") }
    StaggeredItem(6) {
        IceField(state.email, viewModel::setEmail, hint = "ایمیل معلم", keyboardType = KeyboardType.Email)
    }
    StaggeredItem(7) {
        IceButton(
            text = "ارسال کد تأیید",
            onClick = viewModel::sendTeacherRegistrationOtp,
            enabled = state.fullName.trim().length >= 2 && '@' in state.email,
            loading = state.isLoading
        )
    }
    StaggeredItem(8) { BackButtonRow(onBack = viewModel::showSignIn, enabled = !state.isLoading) }
}

@Composable
private fun TeacherSetupPane(state: AuthUiState, viewModel: AuthViewModel) {
    StaggeredItem(0) {
        Text("تکمیل حساب معلم", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = IceInk)
    }
    StaggeredItem(1) {
        Text(
            "ایمیل تأیید شد. نام کاربری نمایشی و رمز ورود را تعیین کنید.",
            fontSize = 13.sp,
            color = IceTextSecondary,
            lineHeight = 22.sp
        )
    }
    StaggeredItem(2) {
        IceField(
            state.username,
            viewModel::setUsername,
            hint = "نام کاربری انگلیسی",
            supporting = "۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط؛ با همین نام کاربری (یا ایمیل) وارد می‌شوید."
        )
    }
    StaggeredItem(3) {
        IceField(
            state.teacherInviteCode,
            viewModel::setTeacherInviteCode,
            hint = "کد دعوت مدرسه (اختیاری)",
            supporting = "اگر مدیر مدرسه کد ۶ حرفی یا کد TCH داده است، آن را اینجا وارد کنید."
        )
    }
    StaggeredItem(4) { PasswordField("رمز جدید ۸ تا ۷۲ کاراکتر", state.newPassword, viewModel::setNewPassword) }
    StaggeredItem(5) { PasswordField("تکرار رمز جدید", state.confirmPassword, viewModel::setConfirmPassword) }
    StaggeredItem(6) {
        IceButton(
            text = "تکمیل ثبت‌نام و ورود",
            onClick = viewModel::completeTeacherRegistration,
            enabled = state.username.length >= 4 &&
                state.newPassword.length >= 8 && state.newPassword == state.confirmPassword,
            loading = state.isLoading
        )
    }
    StaggeredItem(7) {
        LinkTextButton("انصراف و پاک‌کردن حساب نیمه‌کاره", onClick = viewModel::abandonSignup, enabled = !state.isLoading, gray = true)
    }
}

@Composable
private fun ManagerRegistrationPane(state: AuthUiState, viewModel: AuthViewModel) {
    StaggeredItem(2) {
        Text("ثبت‌نام مدیر/معاون", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = IceInk)
    }
    StaggeredItem(3) {
        Text(
            "پس از تأیید ایمیل، یک مدرسهٔ مستقل ایجاد می‌شود.",
            fontSize = 13.sp,
            color = IceTextSecondary,
            lineHeight = 22.sp
        )
    }
    // V60.0 — ثبت‌نام با گوگل برای مدیر/معاون. V237 — بالای فرم.
    StaggeredItem(4) { GoogleRegisterButton(state = state, viewModel = viewModel, role = "manager") }
    StaggeredItem(5) { IceField(state.fullName, viewModel::setFullName, hint = "نام و نام خانوادگی") }
    StaggeredItem(6) {
        IceField(state.email, viewModel::setEmail, hint = "ایمیل مدیر/معاون", keyboardType = KeyboardType.Email)
    }
    StaggeredItem(7) {
        IceButton(
            text = "ارسال کد تأیید",
            onClick = viewModel::sendManagerRegistrationOtp,
            enabled = state.fullName.trim().length >= 2 && '@' in state.email,
            loading = state.isLoading
        )
    }
    StaggeredItem(8) { BackButtonRow(onBack = viewModel::showSignIn, enabled = !state.isLoading) }
}

@Composable
private fun ManagerSetupPane(state: AuthUiState, viewModel: AuthViewModel) {
    StaggeredItem(0) {
        Text("تکمیل حساب مدیر/معاون", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = IceInk)
    }
    StaggeredItem(1) {
        Text(
            "ایمیل تأیید شد. مدرسه و اطلاعات ورود را تعیین کنید.",
            fontSize = 13.sp,
            color = IceTextSecondary,
            lineHeight = 22.sp
        )
    }
    StaggeredItem(2) { IceField(state.schoolName, viewModel::setSchoolName, hint = "نام مدرسه") }
    StaggeredItem(3) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IceField(
                state.province, viewModel::setProvince, hint = "استان",
                modifier = Modifier.weight(1f)
            )
            IceField(
                state.city, viewModel::setCity, hint = "شهر",
                modifier = Modifier.weight(1f)
            )
        }
    }
    StaggeredItem(4) { IceField(state.username, viewModel::setUsername, hint = "نام کاربری انگلیسی") }
    StaggeredItem(5) { PasswordField("رمز جدید ۸ تا ۷۲ کاراکتر", state.newPassword, viewModel::setNewPassword) }
    StaggeredItem(6) { PasswordField("تکرار رمز جدید", state.confirmPassword, viewModel::setConfirmPassword) }
    StaggeredItem(7) {
        IceButton(
            text = "ساخت مدرسه و ورود",
            onClick = viewModel::completeManagerRegistration,
            enabled = state.schoolName.trim().length >= 2 &&
                state.username.length >= 4 && state.newPassword.length >= 8 &&
                state.newPassword == state.confirmPassword,
            loading = state.isLoading
        )
    }
    StaggeredItem(8) {
        LinkTextButton("انصراف و پاک‌کردن حساب نیمه‌کاره", onClick = viewModel::abandonSignup, enabled = !state.isLoading, gray = true)
    }
}

@Composable
private fun RecoveryPane(state: AuthUiState, viewModel: AuthViewModel) {
    // V62.1 — نوار مراحل داخل کارت مثل ForgotScreen ماژول (مرحلهٔ ۱: ایمیل).
    StaggeredItem(0) { StepIndicator(steps = RecoverySteps, current = 0) }
    StaggeredItem(1) {
        ScreenHeader(
            icon = Icons.Filled.Lock,
            title = "بازیابی رمز عبور",
            subtitle = "ایمیل حساب خود را وارد کنید تا کد بازیابی برایتان ارسال شود. " +
                "کد فقط به ایمیل حساب موجود فرستاده می‌شود و حساب تازه‌ای ساخته نمی‌شود."
        )
    }
    StaggeredItem(2) {
        IceField(
            state.email,
            viewModel::setEmail,
            hint = "ایمیل حساب",
            keyboardType = KeyboardType.Email
        )
    }
    StaggeredItem(3) {
        IceButton(
            text = "ارسال کد بازیابی",
            onClick = viewModel::sendRecoveryOtp,
            enabled = '@' in state.email,
            loading = state.isLoading
        )
    }
    StaggeredItem(4) { BackButtonRow(onBack = viewModel::showLoginRole, enabled = !state.isLoading) }
}

@Composable
private fun RecoveryPasswordPane(state: AuthUiState, viewModel: AuthViewModel) {
    // V62.1 — مرحلهٔ ۳: رمز جدید؛ دو مرحلهٔ قبلی تیک انیمیشنی می‌گیرند.
    StaggeredItem(0) { StepIndicator(steps = RecoverySteps, current = 2) }
    StaggeredItem(1) {
        ScreenHeader(
            icon = Icons.Filled.Lock,
            title = "تعیین رمز تازه",
            subtitle = state.recoveredUsername?.let { "نام کاربری حساب: $it" }
        )
    }
    StaggeredItem(2) { PasswordField("رمز جدید ۸ تا ۷۲ کاراکتر", state.newPassword, viewModel::setNewPassword) }
    StaggeredItem(3) { PasswordField("تکرار رمز جدید", state.confirmPassword, viewModel::setConfirmPassword) }
    StaggeredItem(4) {
        IceButton(
            text = "ذخیره رمز و ورود",
            onClick = viewModel::saveRecoveredPassword,
            enabled = state.newPassword.length >= 8 && state.newPassword == state.confirmPassword,
            loading = state.isLoading
        )
    }
    StaggeredItem(5) {
        LinkTextButton("انصراف و پاک‌کردن حساب نیمه‌کاره", onClick = viewModel::abandonSignup, enabled = !state.isLoading, gray = true)
    }
}

@Composable
private fun OtpPane(
    title: String,
    hint: String,
    state: AuthUiState,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel,
    recoverySteps: Boolean = false
) {
    // V62.1 — در جریان بازیابی، مرحلهٔ «کد بازیابی» فعال است (مثل VerifyCodeScreen ماژول).
    if (recoverySteps) StaggeredItem(0) { StepIndicator(steps = RecoverySteps, current = 1) }
    StaggeredItem(1) { ScreenHeader(icon = Icons.Filled.MailOutline, title = title, subtitle = hint) }
    // V62.0 — باکس‌های کد با Paste و Backspace طبیعی (فیلد مخفی)؛ کد سوپابیس
    // ۶ تا ۸ رقمی است و باکس‌ها با طول کد گسترده می‌شوند.
    StaggeredItem(2) {
        Text(
            "کد یک‌بارمصرف ۶ تا ۸ رقم",
            fontSize = 11.5.sp,
            color = IceTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    StaggeredItem(3) {
        OtpBoxes(
            value = state.otp,
            onValueChange = viewModel::setOtp,
            modifier = Modifier.fillMaxWidth()
        )
    }
    StaggeredItem(4) {
        IceButton(
            text = "تأیید کد",
            onClick = onVerify,
            enabled = state.otp.length in 6..8,
            loading = state.isLoading
        )
    }
    StaggeredItem(5) {
        IceOutlinedButton(text = "ارسال دوباره کد", onClick = onResend, enabled = !state.isLoading)
    }
    StaggeredItem(6) { BackButtonRow(onBack = onBack, enabled = !state.isLoading) }
}

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    IceField(
        value = value,
        onValueChange = onChange,
        hint = label,
        keyboardType = KeyboardType.Password,
        visualTransformation = passwordTransformation(visible),
        trailingIcon = {
            PasswordVisibilityButton(visible = visible, onToggle = { visible = !visible })
        }
    )
}

/**
 * V60.1 — «ثبت‌نام با گوگل» با Credential Manager مستقیم (مسیر رسمی مستندات
 * Supabase): پنجرهٔ انتخاب جیمیل‌های گوشی → idToken → ورود IDToken به
 * Supabase → ثبت نقش → نشستن user در state (ورود خودکار به برنامه).
 * V60.0 با پلاگین compose-auth بود که روی برخی دستگاه‌ها پس از انتخاب جیمیل
 * callback را گم می‌کرد («اتفاقی نمی‌افتد»).
 */
@Composable
private fun GoogleRegisterButton(state: AuthUiState, viewModel: AuthViewModel, role: String) {
    GoogleAuthButton(state = state, viewModel = viewModel, role = role, registering = true) {
        Text("ثبت‌نام با گوگل")
    }
}

/**
 * V61.0 — دکمهٔ مشترک گوگل برای ثبت‌نام و ورود؛ متن دکمه از بیرون می‌آید و
 * جریان Credential Manager یکی است (حساب موجود مستقیم وارد می‌شود و حساب
 * تازه به صفحهٔ تکمیل ثبت‌نام همان نقش می‌رود). V62.1: ظاهر یخی ماژول.
 */
@Composable
private fun GoogleAuthButton(
    state: AuthUiState,
    viewModel: AuthViewModel,
    role: String,
    registering: Boolean = false, // V237
    label: @Composable () -> Unit
) {
    if (ir.exam.app.BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
        Text(
            "ثبت‌نام با گوگل: کلید GOOGLE_WEB_CLIENT_ID در local.properties تنظیم نشده است.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var openingGooglePicker by remember(role, registering) { mutableStateOf(false) }
    var googleJob by remember { mutableStateOf<Job?>(null) }
    OutlinedButton(
        onClick = {
            if (openingGooglePicker || state.isLoading) return@OutlinedButton
            openingGooglePicker = true
            googleJob = scope.launch {
                val rawNonce = java.util.UUID.randomUUID().toString()
                val hashedNonce = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(rawNonce.toByteArray())
                    .joinToString("") { "%02x".format(it) }
                try {
                    val credentialManager = androidx.credentials.CredentialManager.create(context)
                    fun request(authorizedOnly: Boolean) = androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(
                            com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(authorizedOnly)
                                .setServerClientId(ir.exam.app.BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                .setNonce(hashedNonce)
                                .build()
                        )
                        .build()
                    // V335.61 — اول همهٔ حساب‌های گوگل گوشی، بعد حساب‌های مجاز؛ اگر پنجرهٔ گوگل بار اول
                    // باز نشود (NoCredentialException)، تلاش دوم انجام می‌شود و در غیر این صورت
                    // پیام دقیق همان خطا به کاربر نشان داده می‌شود.
                    val result = runCatching { credentialManager.getCredential(request = request(false), context = context) }
                        .getOrElse { first ->
                            if (first is androidx.credentials.exceptions.GetCredentialCancellationException) throw first
                            credentialManager.getCredential(request = request(true), context = context)
                        }
                    val googleCredential =
                        com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
                            .createFrom(result.credential.data)
                    viewModel.signInWithGoogleIdToken(googleCredential.idToken, rawNonce, role, registering)
                } catch (cancel: kotlinx.coroutines.CancellationException) {
                    // لغو با بستن پنجرهٔ انتظار؛ یا بستن پنجرهٔ گوگل توسط کاربر (خطا نیست).
                } catch (cancel: androidx.credentials.exceptions.GetCredentialCancellationException) {
                    // بستن پنجره توسط کاربر خطا نیست.
                } catch (error: Throwable) {
                    viewModel.reportGoogleError(googleSignInErrorMessage(error))
                } finally {
                    openingGooglePicker = false
                    googleJob = null
                }
            }
        },
        enabled = !state.isLoading && !openingGooglePicker,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = IceInk,
            disabledContainerColor = Color.White,
            disabledContentColor = IceDisabledText
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, IceStroke)
    ) {
        // V60.2 — لوگوی رسمی چهاررنگ گوگل؛ tint خنثی تا رنگ‌ها حفظ شوند.
        Icon(
            imageVector = GoogleLogo,
            contentDescription = null,
            tint = androidx.compose.ui.graphics.Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        // V335.61 — هیچ متن انتظاری روی دکمه نمی‌آید؛ تا بازشدن انتخاب‌گر گوگل، انیمیشن وسط صفحه نمایش داده می‌شود.
        label()
    }
    if (openingGooglePicker) {
        GooglePickerWaitingDialog(onCancel = {
            googleJob?.cancel()
            googleJob = null
            openingGooglePicker = false
        })
    }
}

/**
 * V335.61 — پنجرهٔ انتظارِ بازشدن انتخاب‌گر حساب گوگل: پس‌زمینهٔ تیرهٔ ملایم و انیمیشن چهارنقطه‌ای گوگل
 * در مرکز صفحه، بدون متن. اگر پس از ۱۲ ثانیه پنجرهٔ گوگل باز نشد، امکان لغو/تلاش دوباره داده می‌شود.
 */
@Composable
private fun GooglePickerWaitingDialog(onCancel: () -> Unit) {
    var longWait by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(12_000)
        longWait = true
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(Modifier.fillMaxSize().background(Color(0x5C0F172A)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GoogleDotsAnimation()
                if (longWait) {
                    Text("پنجرهٔ گوگل باز نشد؟ دوباره تلاش کنید.", fontSize = 12.sp, color = Color.White)
                    TextButton(onClick = onCancel) { Text("لغو", color = Color.White) }
                }
            }
        }
    }
}

/** V335.61 — انیمیشن چهارنقطه‌ای گوگل (آبی، قرمز، زرد، سبز) با موج پیاپی. */
@Composable
private fun GoogleDotsAnimation() {
    val googleColors = listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853))
    val transition = rememberInfiniteTransition(label = "google-wait")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1400, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "google-phase"
    )
    Canvas(Modifier.size(72.dp)) {
        val radius = size.minDimension * 0.13f
        val gap = size.minDimension * 0.30f
        googleColors.forEachIndexed { index, dotColor ->
            val p = (phase + index * 0.25f) % 1f
            val bump = if (p < 0.5f) p * 2f else (1f - p) * 2f
            drawCircle(
                color = dotColor.copy(alpha = 0.45f + 0.55f * bump),
                radius = radius * (0.65f + 0.35f * bump),
                center = Offset(size.width / 2f + (index - 1.5f) * gap, size.height / 2f)
            )
        }
    }
}

/** V335.61 — پیام فارسی متناسب با نوع خطای Credential Manager (بدون وابستگی به کلاس‌های اختصاصی نسخه‌ها). */
private fun googleSignInErrorMessage(error: Throwable): String {
    if (ir.exam.app.BuildConfig.IS_BETA) {
        return "ورود با گوگل در نسخهٔ آزمایشی فعال نیست (بستهٔ آزمایشی در گوگل ثبت نشده)؛ با نام کاربری و رمز وارد شوید."
    }
    val kind = error::class.java.simpleName
    val detail = error.message?.takeIf { it.isNotBlank() } ?: "بدون جزئیات"
    return when {
        kind.contains("NoCredential") ->
            "هیچ حساب گوگلی برای ورود پیدا نشد. یک حساب Google در تنظیمات گوشی اضافه کنید و دوباره تلاش کنید."
        kind.contains("ProviderConfiguration") || kind.contains("Configuration") ->
            "سرویس Google Play این گوشی آماده یا به‌روز نیست؛ Google Play Services را به‌روزرسانی کنید یا با نام کاربری و رمز وارد شوید."
        kind.contains("Unsupported") ->
            "این گوشی از روش جدید ورود گوگل پشتیبانی نمی‌کند؛ با نام کاربری و رمز وارد شوید."
        kind.contains("Interrupted") ->
            "درخواست ورود گوگل نیمه‌کاره ماند؛ دوباره تلاش کنید."
        kind.contains("Unknown") ->
            "گوگل پنجرهٔ انتخاب حساب را باز نکرد. جزئیات: $detail"
        else ->
            "ورود گوگل ناموفق بود ($kind): $detail"
    }
}
