package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V45.2 — رفع «Supabase public update RPC status: 404» در GitHub Actions:
 *
 * ۱) بررسی اتصال عمومی CI دقیقاً همان مسیر برنامه Kotlin را تست می‌کند:
 *    خواندن جدول app_version با کلید anon (RLS) — نه RPC جانبی.
 * ۲) RPC جانبی check_app_update فقط به‌صورت اطلاعاتی گزارش می‌شود و build را بلاک نمی‌کند.
 * ۳) فایل بازسازی دستی تابع برای کلاینت‌های دیگر (WebView قدیمی) موجود است.
 */
class V45_2CiUpdateCheckFixTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val workflow by lazy { source(".github/workflows/android.yml") }
    private val restoreSql by lazy {
        source("sql/manual/SQL_NATIVE_RESTORE_CHECK_APP_UPDATE_V452.sql")
    }

    // ============================================================
    // ۱) CI مسیر واقعی برنامه را چک می‌کند
    // ============================================================

    @Test
    fun `ci update check tests the app_version public read path`() {
        assertTrue("app_version?select=version_code" in workflow)
        assertTrue("is_active=eq.true" in workflow)
        assertTrue("Supabase app_version public status" in workflow)
        assertTrue("test \"${'$'}STATUS\" = \"200\"" in workflow)
    }

    // ============================================================
    // ۲) RPC جانبی دیگر CI را بلاک نمی‌کند
    // ============================================================

    @Test
    fun `optional check_app_update rpc is only informational`() {
        assertTrue("check_app_update RPC status (informational)" in workflow)
        assertTrue("RPC_STATUS=" in workflow)
        // دیگر هیچ test روی status این RPC وجود ندارد:
        assertFalse("test \"${'$'}RPC_STATUS\" = \"200\"" in workflow)
    }

    // ============================================================
    // ۳) فایل بازسازی دستی تابع
    // ============================================================

    @Test
    fun `manual restore sql recreates check_app_update idempotently`() {
        assertTrue("create or replace function public.check_app_update" in restoreSql)
        assertTrue("grant execute on function public.check_app_update(integer) to anon, authenticated" in restoreSql)
        assertTrue("eazwuyrymsvdkwckdpco.supabase.co" in restoreSql)
        assertTrue("to_regclass('public.app_version')" in restoreSql)
    }
}
