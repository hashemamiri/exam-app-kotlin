package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V60.4 — گزارش دستگاه (با عکس): «کد دعوت از داخل پنل معلم کار می‌کند اما
 * هنگام ایجاد حساب خطای "کد دعوت معتبر نیست." می‌دهد».
 *
 * ریشه (با مدرک): مدیر از V40B فقط کد کوتاه ۶ حرفی می‌سازد
 * (native_manager_create_teacher_invites_v40b → کد ۶ کاراکتری A-Z0-9)؛ ولی
 * مسیر ثبت‌نامِ با کد دعوت (completeInvitedTeacherRegistration) از V37 فقط
 * کد بلند TCH- (طول ≥۶۰) را می‌پذیرفت. پیوستن از داخل پنل چون از
 * native_join_school_v39 می‌رود سالم بود.
 *
 * راه‌حل: مسیر ثبت‌نام حالا کد کوتاه را هم می‌پذیرد — اول تکمیل حساب معلم
 * (native_complete_teacher_registration_v1) و بعد پیوستن به مدرسه
 * (native_join_school_v39؛ همان RPC مسیر سالم). کد بلند TCH- قدیمی مثل قبل
 * از native_complete_teacher_registration_v37 می‌رود.
 */
class V60_4ShortInviteRegistrationTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val authRepo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt") }
    private val signIn by lazy { source("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt") }

    @Test
    fun `registration accepts the six character manager code`() {
        // تشخیص نوع کد
        assertTrue("val isShortCode = shortCode.matches(Regex(\"^[A-Z0-9]{6}$\"))" in authRepo)
        // کد کوتاه: تکمیل حساب v1 سپس پیوستن v39 (همان مسیر سالم داخل پنل)
        assertTrue("if (isShortCode) {" in authRepo)
        assertTrue("\"native_join_school_v39\"" in authRepo)
        // کد بلند TCH- قدیمی همچنان از مسیر V37
        assertTrue("code.startsWith(\"TCH-\") && code.length >= 60" in authRepo)
        assertTrue("native_complete_teacher_registration_v37" in authRepo)
        // حروف کوچک هم پذیرفته می‌شود (uppercase قبل از تطبیق)
        assertTrue("val shortCode = code.uppercase()" in authRepo)
    }

    @Test
    fun `field hint mentions the six character code`() {
        assertTrue("اگر مدیر مدرسه کد ۶ حرفی یا کد TCH داده است، آن را اینجا وارد کنید." in signIn)
    }
}
