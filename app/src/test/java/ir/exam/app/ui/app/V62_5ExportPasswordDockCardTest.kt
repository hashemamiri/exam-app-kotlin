package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V62.5 — چهار درخواست کاربر:
 * ۱) اکسل دانش‌آموزان دومرحله‌ای: اول انتخاب گروه با همان پنجرهٔ فیلتر، بعد
 *    انتخاب ستون‌ها؛ اگر پنل سازندهٔ حساب باشد (رمز در Vault دستگاه) گزینهٔ
 *    «رمز حساب» هم اضافه می‌شود.
 * ۲) تغییر رمز کارت حساب نیازمند «رمز فعلی» است + مسیر بازیابی با کد ایمیل.
 * ۳) داشبورد پیش‌فرض مدیر نباید دکمهٔ آمار (کارت‌ها) داک را روشن نشان دهد.
 * ۴) آیکن‌های سربرگ کارت سؤال ۳۸→۳۰dp تا «چندگزینه‌ای» کامل دیده شود.
 */
class V62_5ExportPasswordDockCardTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }
    private val profile by lazy { source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt") }
    private val profileVm by lazy { source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt") }
    private val profileRepo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt") }
    private val appShell by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }

    @Test
    fun `excel export runs in two steps with optional vault passwords`() {
        // دکمهٔ Excel دیگر مستقیم خروجی نمی‌گیرد؛ جریان دومرحله‌ای باز می‌شود
        assertTrue("exportStep = 1" in school)
        // مرحلهٔ ۱: همان پنجرهٔ فیلتر برای انتخاب گروه
        val step1 = school.substringAfter("if (exportStep == 1) {")
            .substringBefore("if (exportStep == 2) {")
        assertTrue("StudentFilterDialog(" in step1)
        assertTrue("filter = exportFilter" in step1)
        assertTrue("exportStep = 2" in step1)
        // مرحلهٔ ۲: انتخاب ستون‌ها روی گروه فیلترشده
        val step2 = school.substringAfter("if (exportStep == 2) {")
            .substringBefore("private fun SchoolsContent(")
        assertTrue("applyStudentFilter(" in step2)
        assertTrue("StudentExportColumnsDialog(" in step2)
        // ستون رمز فقط از Vault همین پنل (حساب‌های ساخته‌شده در این پنل)
        assertTrue("internal val StudentExportColumns" in school)
        val dialog = school.substringAfter("private fun StudentExportColumnsDialog(")
        assertTrue("رمز حساب (" in dialog)
        assertTrue("passwordCount" in dialog)
        val workbook = school.substringAfter("private fun studentWorkbook(")
            .substringBefore("private fun credentialWorkbook(")
        assertTrue("passwordOf: (StudentProfile) -> String?" in workbook)
        assertTrue("includePassword" in workbook)
    }

    @Test
    fun `password change now requires the current password with email recovery`() {
        // ViewModel: تأیید رمز فعلی قبل از تغییر + مسیر بازیابی
        assertTrue("fun changePassword(currentPassword: String, password: String, confirmation: String)" in profileVm)
        assertTrue("repository.verifyCurrentPassword(currentPassword).getOrThrow()" in profileVm)
        assertTrue("fun sendPasswordRecoveryOtp(email: String)" in profileVm)
        assertTrue("fun recoverPassword(email: String, code: String, password: String, confirmation: String)" in profileVm)
        // Repository: تأیید رمز با signIn دوباره؛ کد فقط به ایمیل خود حساب
        assertTrue("suspend fun verifyCurrentPassword(currentPassword: String)" in profileRepo)
        assertTrue("رمز فعلی نادرست است." in profileRepo)
        assertTrue("کد بازیابی فقط به ایمیل همین حساب ارسال می‌شود." in profileRepo)
        assertTrue("suspend fun verifyPasswordRecoveryOtp(email: String, code: String)" in profileRepo)
        // UI: فیلد رمز فعلی + لینک فراموشی + جملهٔ قدیمی حذف شد
        assertTrue("Text(\"رمز فعلی\")" in profile)
        assertTrue("رمز فعلی را فراموش کرده‌ام" in profile)
        assertTrue("onChangePassword(currentPassword, password, confirmation)" in profile)
        assertFalse("رمز قبلی قابل مشاهده یا بازیابی نیست." in profile)
    }

    @Test
    fun `manager default dashboard keeps the dock stats button unselected`() {
        assertTrue("managerDashboardActive: Boolean = false" in appShell)
        assertTrue("if (managerDashboardActive) TeacherDockSection.NONE" in appShell)
        assertTrue("page == MainPage.CARDS && managerCardsSection == \"status\"" in appShell)
    }

    @Test
    fun `question card header icons are tighter so the type label fits`() {
        val editor = builder.substringAfter("private fun QuestionEditor(")
            .substringBefore("private fun QuestionStyleControls(")
        assertTrue(".size(30.dp)" in editor)
        assertFalse(".size(38.dp)" in editor)
    }
}
