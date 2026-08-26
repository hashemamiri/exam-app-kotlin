package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هات‌فیکس V61.0.1 — خطای اجرای SQL نسخهٔ V61.0:
 * «ERROR: 42702: column reference "school_id" is ambiguous».
 *
 * ریشه (با مدرک): جدول exams از V38 ستون school_id دارد
 * (native_manager_wallet_stats_v38.sql خط ۶). در تابع
 * native_exam_audience_schools_v61 کوئری join بین exam_audience_schools و
 * exams بود و jsonb_agg(school_id) بدون پیشوند نوشته شده بود؛ PostgreSQL
 * نمی‌داند school_id کدام جدول است. رفع: پیشوند صریح s.school_id.
 * بقیهٔ کوئری‌های فایل یا تک‌جدولی‌اند یا ستون فقط در یک جدول وجود دارد.
 */
class V61_0_1SqlAmbiguousSchoolIdHotfixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val migration by lazy {
        File(root(), "supabase/migrations/20260826_native_schools_audience_v61.sql").readText()
    }

    @Test
    fun `exam audience schools reader qualifies the school_id column`() {
        assertTrue("jsonb_agg(s.school_id::text)" in migration)
        assertFalse("select jsonb_agg(school_id::text)" in migration)
        // همان join دوجدولی که ابهام را می‌ساخت هنوز سر جای خود است
        assertTrue("from public.exam_audience_schools s" in migration)
        assertTrue("join public.exams e on e.id = s.exam_id and e.teacher_id = auth.uid()" in migration)
    }
}
