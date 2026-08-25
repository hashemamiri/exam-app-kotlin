package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هات‌فیکس V60.3 — گزارش دستگاه: «با جیمیل جدید از مسیر مدیر، مستقیم وارد
 * پنل معلم می‌شود».
 *
 * ریشه (با مدرک از V38.1): trigger قدیمی وب‌اپ profile هر حساب ایمیلی تازه
 * را با role='teacher' می‌سازد. requires_teacher_setup فقط role='student'
 * را چک می‌کرد → برای کاربر گوگلی تازه false می‌شد → بدون صفحهٔ تکمیل،
 * مستقیم پنل معلم. مسیر OTP مدیر در V38.1 با پذیرش «معلم خالی» دور زده
 * شده بود؛ مسیر گوگل نه.
 *
 * راه‌حل: state ثبت‌نام حالا «معلم خالی» (بدون username/کلاس/آزمون/
 * دانش‌آموز/عضویت) را که نقش انتخابی ثبت‌نام دارد نیازمند setup می‌داند؛
 * pending_role از جدول نقش‌ها مسیر مدیر/معلم درست را باز می‌کند. توابع
 * تکمیل موجود (v37/v38.1) از قبل profile معلم خالی را می‌پذیرند.
 */
class V60_3GoogleEmptyTeacherStateHotfixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val migration by lazy {
        File(root(), "supabase/migrations/20260825_native_google_role_state_v60_3.sql").readText()
    }

    @Test
    fun `empty trigger-made teachers with a chosen role still require setup`() {
        assertTrue("p.role = 'teacher'" in migration)
        assertTrue("coalesce(p.username, '') = ''" in migration)
        assertTrue("from public.native_registration_roles r" in migration)
        // فقط معلمِ واقعاً خالی: هیچ کلاس/آزمون/دانش‌آموز/عضویت فعالی ندارد
        assertTrue("not exists (select 1 from public.classes c where c.teacher_id = p.id)" in migration)
        assertTrue("not exists (select 1 from public.exams e where e.teacher_id = p.id)" in migration)
        assertTrue("where s.teacher_id = p.id and s.role = 'student'" in migration)
        assertTrue("where sm.user_id = p.id and sm.status = 'active'" in migration)
        // مسیر قدیمی student دست‌نخورده
        assertTrue("(p.role = 'student' and p.teacher_id is null)" in migration)
        // pending_role همچنان اول از جدول نقش‌ها
        assertTrue("select r.role from public.native_registration_roles r where r.user_id = p.id" in migration)
    }
}
