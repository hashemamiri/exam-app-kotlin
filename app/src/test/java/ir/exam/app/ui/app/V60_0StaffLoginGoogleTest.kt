package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V60.0 — دو درخواست کاربر:
 * ۱) «معلم/مدیر نمی‌توانند با نام کاربری وارد شوند»: passwordLoginEmail هر
 *    شناسهٔ بدون @ را به دامنهٔ دانش‌آموز می‌برد. حالا برای نام کاربری، اول
 *    نگاشت کادر مدرسه از سرور (native_staff_login_email_v1 → ایمیل واقعی
 *    Auth) و اگر نبود همان مسیر دانش‌آموز. پیام تابع سرور برای نام کاربری
 *    ناموجود همان «ورود ناموفق» است تا شمارش نام کاربری ممکن نشود.
 * ۲) «ثبت‌نام با گوگل»: پلاگین compose-auth (Credential Manager) + دکمه با
 *    آیکن در پنل‌های ثبت‌نام معلم و مدیر؛ پس از موفقیت نقش انتخابی روی
 *    metadata ثبت (native_set_registration_role_v1) و جریان تکمیل ثبت‌نام
 *    موجود v12 (requires_teacher_setup) ادامه می‌یابد. GOOGLE_WEB_CLIENT_ID
 *    از local.properties می‌آید (secret در کد نیست).
 */
class V60_0StaffLoginGoogleTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val authRepo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt") }
    private val signIn by lazy { source("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt") }
    private val authVm by lazy { source("app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt") }
    private val provider by lazy { source("app/src/main/java/ir/exam/app/data/remote/SupabaseProvider.kt") }
    private val gradle by lazy { source("app/build.gradle.kts") }
    private val migration by lazy { source("supabase/migrations/20260825_native_staff_login_google_v60.sql") }

    @Test
    fun `staff usernames resolve to their real auth email before password login`() {
        assertTrue("staffLoginEmail(clean) ?: AuthIdentifier.passwordLoginEmail(clean)" in authRepo)
        assertTrue("native_staff_login_email_v1" in authRepo)
        // سرور: فقط نقش کادر؛ پیام خنثی برای نام ناموجود
        assertTrue("p.role in ('teacher', 'manager')" in migration)
        assertTrue("ایمیل/نام کاربری یا رمز عبور نادرست است." in migration)
        assertTrue("grant execute on function public.native_staff_login_email_v1(text) to anon, authenticated" in migration)
    }

    @Test
    fun `google registration button exists on both staff panes`() {
        assertTrue("GoogleRegisterButton(state = state, viewModel = viewModel, role = \"teacher\")" in signIn)
        assertTrue("GoogleRegisterButton(state = state, viewModel = viewModel, role = \"manager\")" in signIn)
        assertTrue("rememberSignInWithGoogle" in signIn)
        assertTrue("Text(\"ثبت‌نام با گوگل\")" in signIn)
        // آیکن دارد و بستن توسط کاربر خطا نیست
        assertTrue("Icons.Outlined.AccountCircle" in signIn)
        assertTrue("NativeSignInResult.ClosedByUser -> Unit" in signIn)
    }

    @Test
    fun `google flow registers the chosen role then refreshes the account`() {
        assertTrue("fun completeGoogleRegistration(role: String)" in authVm)
        assertTrue("native_set_registration_role_v1" in authVm)
        assertTrue("repository.refreshCurrentUser().getOrThrow()" in authVm)
        assertTrue("registration_role" in migration)
    }

    @Test
    fun `compose auth plugin is installed with a properties-based client id`() {
        assertTrue("io.github.jan-tennert.supabase:compose-auth:3.1.4" in gradle)
        assertTrue("GOOGLE_WEB_CLIENT_ID" in gradle)
        assertTrue("googleNativeLogin(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)" in provider)
        // بدون کلید، پیام راهنما به‌جای کرش
        assertTrue("GOOGLE_WEB_CLIENT_ID در local.properties تنظیم نشده است" in signIn)
    }
}
