package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V59.1 (پچ ۲ از ۲) — حذف کامل حساب معلم/مدیر:
 * ۱) دکمهٔ «حذف حساب» در بخش حساب (فقط کادر مدرسه؛ دانش‌آموز ندارد) با
 *    تأیید دومرحله‌ای و توضیح کامل عواقب.
 * ۲) قاعدهٔ کاربر: دانش‌آموزِ ساختهٔ این حساب که در لیست حساب دیگری هم هست
 *    حذف نمی‌شود؛ مالکیت کامل (profiles.teacher_id → کنترل رمز از مسیر
 *    manage-student) به قدیمی‌ترین لینک منتقل می‌شود. دانش‌آموز تک‌مالکه و
 *    کلاس‌های حساب حذف می‌شوند.
 * ۳) اجرا: SQL اتمیک native_prepare_account_deletion_v1 (فقط service_role)
 *    + اکشن delete_account در Edge manage-student (حذف auth دانش‌آموزان
 *    تک‌مالکه و سپس خود حساب) + signOut در کلاینت.
 */
class V59_1DeleteAccountTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val settings by lazy { source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt") }
    private val settingsVm by lazy { source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt") }
    private val repo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt") }
    private val edge by lazy { source("supabase/functions/manage-student/index.ts") }
    private val migration by lazy { source("supabase/migrations/20260825_native_delete_account_v59.sql") }

    @Test
    fun `account section offers guarded account deletion for staff only`() {
        assertTrue("if (role != UserRole.STUDENT) item {" in settings)
        assertTrue("title = \"حذف حساب\"" in settings)
        assertTrue("confirmDeleteAccount = true" in settings)
        assertTrue("بله، حساب حذف شود" in settings)
        assertTrue("این عمل قابل بازگشت نیست" in settings)
        // V59.3: onDone حالا onAccountDeleted است (خروج محلی به صفحهٔ ورود).
        assertTrue("onDeleteAccount = { viewModel.deleteAccount(onDone = onAccountDeleted) }" in settings)
        assertTrue("if (role == UserRole.STUDENT) return@launch" in settingsVm)
    }

    @Test
    fun `client calls the delete account edge action and signs out`() {
        assertTrue("suspend fun deleteAccount(): Result<Unit>" in repo)
        assertTrue("put(\"action\", \"delete_account\")" in repo)
        // V59.3: خروج محلی — سروری برای کاربر حذف‌شده 403 می‌داد.
        assertTrue("SignOutScope.LOCAL" in repo)
    }

    @Test
    fun `edge function transfers shared students then deletes the rest`() {
        assertTrue("action === 'delete_account'" in edge)
        assertTrue("native_prepare_account_deletion_v1" in edge)
        assertTrue("{ p_actor: teacherId }" in edge)
        assertTrue("deletable_students" in edge)
        // خود حساب در پایان حذف می‌شود
        assertTrue("await service.auth.admin.deleteUser(teacherId)" in edge)
    }

    @Test
    fun `sql migration is atomic transfer-first and service-role only`() {
        // انتقال مالکیت به قدیمی‌ترین لینک غیر از متقاضی
        assertTrue("order by l.created_at asc limit 1" in migration)
        assertTrue("set teacher_id = s.new_owner" in migration)
        // فقط دانش‌آموزانِ بدون لینک دیگر حذف‌شدنی می‌مانند
        assertTrue("where p.role = 'student' and p.teacher_id = v_uid" in migration)
        // کلاس‌ها و عضویت‌ها پاک می‌شوند
        assertTrue("delete from public.class_members cm" in migration)
        assertTrue("delete from public.classes where teacher_id = v_uid" in migration)
        // امنیت: بدون نشست فقط service_role؛ کاربر مستقیم grant ندارد
        assertTrue("coalesce(auth.uid(), p_actor)" in migration)
        assertTrue("grant execute on function public.native_prepare_account_deletion_v1(uuid) to service_role" in migration)
        assertFalse("to authenticated" in migration)
        // فقط کادر مدرسه
        assertTrue("v_role not in ('teacher', 'manager')" in migration)
    }
}
