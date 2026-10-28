package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V282 — شرایط استفاده/حریم خصوصی (منبع یکتا) و حذف حساب با مهلت ۳۰ روز؛ دانش‌آموز فقط درخواست. */
class V282_LegalAndDeletionTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `legal texts are mirrored between site source and app`() {
        val py = src("site/src/legal_fa.py")
        val kt = src("app/src/main/java/ir/exam/app/core/legal/LegalTexts.kt")
        val headings = Regex("\\(\"([^\"]+)\", \"").findAll(py).map { it.groupValues[1] }.toList()
        assertTrue(headings.size >= 18)
        headings.forEach { h -> assertTrue(h, "\"$h\" to \"" in kt) }
        val updated = Regex("UPDATED = \"([^\"]+)\"").find(py)!!.groupValues[1]
        assertEquals(updated, Regex("UPDATED = \"([^\"]+)\"").find(kt)!!.groupValues[1])
        assertTrue("legal_fa.write_pages(SITE)" in src("site/build_site.py"))
        assertTrue("cp site/terms.html site/privacy.html site_out/" in src(".github/workflows/site.yml"))
        assertTrue("private fun LegalCard()" in src("app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt"))
        // V335.61 — خط «با ثبت‌نام، شرایط استفاده و حریم خصوصی…» از صفحهٔ ثبت‌نام (اپ و سایت) حذف شد؛
        // متن کامل همچنان در «درباره» (LegalCard) و صفحات terms/privacy موجود است.
        assertFalse("LegalConsentLine" in src("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt"))
        assertFalse("را می‌پذیرید" in src("site/src/mobile.js"))
        assertFalse("را می‌پذیرید" in src("site/src/app.js"))
    }

    @Test
    fun `account deletion is deferred for staff and request-only for students`() {
        val sql = src("supabase/migrations/20261014_native_account_deletion_v282.sql")
        for (f in listOf("native_my_deletion_state_v282", "native_request_account_deletion_v282", "native_cancel_account_deletion_v282", "native_student_request_deletion_v282", "native_due_account_deletions_v282"))
            assertTrue(f, "create or replace function public.$f(" in sql)
        assertTrue("if v_role not in ('teacher', 'manager') then return jsonb_build_object('error'" in sql)
        assertTrue("deletion_requested_at < now() - interval '30 days'" in sql)
        assertTrue("cron.schedule('account_purge_v282'" in sql)
        val edge = src("supabase/functions/manage-student/index.ts")
        assertTrue("const deleteStaffAccount = async (actorId: string) =>" in edge && "native_due_account_deletions_v282" in edge && "x-push-secret" in edge)
        val vm = src("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt")
        assertTrue("fun requestDeletion()" in vm && "fun cancelDeletion()" in vm && "fun studentRequestDeletion()" in vm)
        val scr = src("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        assertTrue("\"انصراف از حذف حساب\"" in scr && "\"درخواست حذف حساب\"" in scr && "\"ارسال درخواست حذف به معلم\"" in scr)
        val js = src("site/src/extras.js")
        assertTrue("native_request_account_deletion_v282" in js && "native_cancel_account_deletion_v282" in js && "native_student_request_deletion_v282" in js)
        assertTrue("\"students\" -> if (user.role == UserRole.TEACHER) MainPage.SCHOOL else null" in src("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt"))
    }
}
