package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هات‌فیکس V60.3.1 — گزارش دستگاه پس از اجرای SQL V60.3: «هنوز از مسیر
 * مدیر/معاون با گوگل مستقیم وارد پنل معلم می‌شود».
 *
 * ریشه (با مدرک از کد کلاینت): گارد realEmailStudent در repository فقط وقتی
 * نقش profile «دانش‌آموز» بود RPC حالت ثبت‌نام را صدا می‌زد؛ ولی trigger
 * قدیمی وب‌اپ profile حساب گوگلی تازه را با نقش «معلم» می‌سازد، پس منطق
 * سروری V60.3 (معلمِ خالی → نیازمند تکمیل) هرگز خوانده نمی‌شد.
 *
 * راه‌حل: «معلم بدون نام کاربری» هم کاندیدای setup است و از سرور پرسیده
 * می‌شود؛ تصمیم نهایی همچنان با تابع سروری V60.3 است (معلمِ واقعاً خالی با
 * ردیف نقش انتخابی در native_registration_roles). حساب‌های معلم واقعی
 * (username دارند یا کلاس/آزمون/دانش‌آموز/عضویت دارند) تحت تأثیر نیستند.
 */
class V60_3_1GoogleTeacherStateClientHotfixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val authRepo by lazy {
        File(root(), "app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt").readText()
    }

    @Test
    fun `teacher profiles without a username also ask the server for setup state`() {
        // شرط جدید: دانش‌آموز «یا» معلم بدون نام کاربری
        assertTrue("val setupCandidate = role == UserRole.STUDENT ||" in authRepo)
        assertTrue("(role == UserRole.TEACHER && profile.username.isNullOrBlank())" in authRepo)
        // فقط حساب‌های ایمیلی واقعی (نه دانش‌آموزان محلی)
        assertTrue("val realEmailAccount =" in authRepo)
        assertTrue("if (realEmailAccount && setupCandidate)" in authRepo)
        // تصمیم نهایی همچنان از سرور خوانده می‌شود
        assertTrue("native_my_registration_state_v1" in authRepo)
    }
}
