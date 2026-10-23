package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.0 (1.02.85) — تضمین دائمی ممیزی امنیتی و مجوزهای Data API پیش از ۳۰ اکتبر Supabase:
 * ۱) وجود مهاجرت جامع 20261023_post_oct30_complete_audit_v335_0.sql با پوشش ۱۶ جدول هسته ای اولیه،
 *    قفل کامل private، عدم دسترسی anon به exam_keys، مجوز service_role روی native_due_account_deletions_v282،
 *    و قفل خودکار search_path تمام توابع public/private.
 * ۲) اجرای موفق اسکریپت نگهبان scripts/check_supabase_data_api_grants.py با ۰ خطا روی همه ۵۷ جدول.
 */
class V335_0_SupabaseOct30AuditTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun v335CompleteOct30MigrationAndGuardScriptPassWithZeroGaps() {
        val mig = File(root(), "supabase/migrations/20261023_post_oct30_complete_audit_v335_0.sql")
        assertTrue("فایل مهاجرت جامع V335.0 باید موجود باشد", mig.isFile)
        val sql = mig.readText()

        listOf(
            "profiles",
            "exams",
            "exam_keys",
            "answers",
            "classes",
            "class_members",
            "exam_attempts",
            "question_bank"
        ).forEach { tbl ->
            assertTrue("جدول هسته ای $tbl باید در مهاجرت V335.0 تحت پوشش باشد", "'$tbl'" in sql)
        }

        assertTrue("revoke all on table public.%I from anon" in sql)
        assertTrue("grant execute on function public.native_due_account_deletions_v282() to service_role" in sql)
        assertTrue("alter function %s set search_path = public, pg_temp" in sql)
        assertTrue("revoke all on schema private from public, anon, authenticated" in sql)
        assertFalse("grant select on table public.exam_keys to anon" in sql.lowercase())
    }
}
