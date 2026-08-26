package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V62.3 — درخواست کاربر: «در کارت حساب بخش قفل برنامه، هنگام فعال/غیرفعال
 * کردن، قفل دستگاه را طلب کند.»
 * قبلاً Switch مستقیم setEnabled را صدا می‌زد؛ حالا لمس Switch اول
 * BiometricPrompt رسمی (اثر انگشت/چهره/الگو/PIN دستگاه) را باز می‌کند و
 * فقط پس از onAuthenticationSucceeded وضعیت هدف (pendingToggle) ذخیره
 * می‌شود؛ لغو یا خطا وضعیت را دست‌نخورده می‌گذارد.
 */
class V62_3LockToggleAuthTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val lockUi by lazy { source("app/src/main/java/ir/exam/app/ui/security/AppLockUi.kt") }

    @Test
    fun `switch demands the device credential before persisting the toggle`() {
        val settings = lockUi.substringAfter("fun AppLockSettings(")
            .substringBefore("private fun rememberSystemBiometricPrompt(")
        // لمس Switch دیگر مستقیم ذخیره نمی‌کند؛ اول پنجرهٔ امن دستگاه باز می‌شود
        assertTrue("pendingToggle = target" in settings)
        assertTrue("prompt?.authenticate(togglePromptInfo(target))" in settings)
        // ذخیره فقط داخل onSuccess با وضعیت نگه‌داشته‌شده
        val success = settings.substringAfter("onSuccess = {").substringBefore("onError = {")
        assertTrue("manager.setEnabled(userId, target)" in success)
        assertTrue("enabled = target" in success)
        // لغو/خطا وضعیت معلق را پاک می‌کند و سوییچ برنمی‌گردد
        val error = settings.substringAfter("onError = {").substringBefore("val body:")
        assertTrue("pendingToggle = null" in error)
        // مسیر قدیمی (ذخیرهٔ مستقیم در onCheckedChange) حذف شده است
        val switchBlock = settings.substringAfter("onCheckedChange = { target ->")
            .substringBefore("if (enabled) {")
        assertFalse("manager.setEnabled" in switchBlock)
    }

    @Test
    fun `the toggle prompt reuses the official system authenticators`() {
        assertTrue("private fun togglePromptInfo(enable: Boolean)" in lockUi)
        val promptInfo = lockUi.substringAfter("private fun togglePromptInfo(")
        assertTrue("فعال‌سازی قفل برنامه" in promptInfo)
        assertTrue("غیرفعال‌سازی قفل برنامه" in promptInfo)
        assertTrue(".setAllowedAuthenticators(SYSTEM_AUTHENTICATORS)" in promptInfo)
        // قرارداد V18/Neumorphic69 پابرجا: بدون PIN اختصاصی و بدون فیلد متنی
        assertFalse("پین جدید" in lockUi)
        assertFalse("OutlinedTextField" in lockUi)
    }
}
