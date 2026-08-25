package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هات‌فیکس V60.2 — دو گزارش دستگاه:
 * ۱) «ثبت‌نام با گوگل از قسمت مدیر، اکانت معلم می‌سازد»: دو ریشه —
 *    الف) تابع قبلی نقش روی auth.users UPDATE می‌زد که مالک توابع Supabase
 *    اجازه‌اش را ندارد؛ خطا در بدنهٔ RPC برمی‌گشت و کلاینت آن را می‌بلعید →
 *    نقش ثبت نمی‌شد → pending_role پیش‌فرض teacher. حالا نقش در جدول public
 *    (native_registration_roles با RLS خود کاربر) ذخیره و در state خوانده
 *    می‌شود؛ کلاینت هم error بدنه را بررسی می‌کند.
 *    ب) signInWithGoogleIdToken مستقیم user را می‌نشاند؛ حالا از مسیر مشترک
 *    acceptAuthenticatedUser می‌رود تا حساب تازه به صفحهٔ تکمیل درست
 *    (معلم/مدیر بر اساس pendingRegistrationRole) برود.
 * ۲) آیکن رسمی گوگل: وکتور G چهاررنگ رسمی (GoogleLogo) با tint خنثی.
 */
class V60_2GoogleRoleLogoHotfixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val authVm by lazy { source("app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt") }
    private val signIn by lazy { source("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt") }
    private val logo by lazy { source("app/src/main/java/ir/exam/app/ui/auth/GoogleLogo.kt") }
    private val migration by lazy { source("supabase/migrations/20260825_native_registration_role_v60_2.sql") }

    @Test
    fun `registration role is stored in a public table not auth users`() {
        assertTrue("create table if not exists public.native_registration_roles" in migration)
        assertTrue("user_id = auth.uid()" in migration)
        assertTrue("on conflict (user_id) do update set role = excluded.role" in migration)
        // state اول جدول ما، بعد metadata (سازگاری عقب‌رو)
        assertTrue("select r.role from public.native_registration_roles r where r.user_id = p.id" in migration)
        assertFalse("update auth.users" in migration)
    }

    @Test
    fun `google sign in surfaces role errors and uses the shared accept path`() {
        assertTrue("val roleResult = runCatching {" in authVm)
        assertTrue("(roleResult?.get(\"error\") as? kotlinx.serialization.json.JsonPrimitive)" in authVm)
        assertTrue("acceptAuthenticatedUser(user)" in authVm)
        // تابع مردهٔ قبلی حذف شد
        assertFalse("completeGoogleRegistration" in authVm)
    }

    @Test
    fun `the button shows the official four color google logo`() {
        assertTrue("val GoogleLogo: ImageVector" in logo)
        for (color in listOf("0xFF4285F4", "0xFF34A853", "0xFFFBBC05", "0xFFEA4335")) {
            assertTrue("brand color $color missing", color in logo)
        }
        assertTrue("imageVector = GoogleLogo" in signIn)
        assertTrue("tint = androidx.compose.ui.graphics.Color.Unspecified" in signIn)
        assertFalse("AccountCircle" in signIn)
    }
}
