package ir.exam.app.ui.app

import ir.exam.app.domain.model.UserRole
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** رگرسیون V36: نقش مدیر، ثبت‌نام مدرسه، tenant و پوستهٔ اختصاصی. */
class V36ManagerFoundationTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `manager is a first class cached role`() {
        assertTrue(UserRole.MANAGER in UserRole.values())
        assertEquals(6, Design69MenuContract.MANAGER_CARD_COUNT)
        assertTrue(Design69MenuContract.isCompleteGrid(Design69MenuContract.MANAGER_CARD_COUNT))
    }

    @Test
    fun `manager hamburger exposes school classes and students`() {
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        val managerMenu = app.substringAfter("} else if (user.role == UserRole.MANAGER) {").substringBefore("} else {")
        assertTrue("کلاس‌ها" in managerMenu)
        assertTrue("دانش‌آموزان" in managerMenu)
        assertTrue("onClick = { select(onClasses) }" in managerMenu)
        assertTrue("onClick = { select(onStudents) }" in managerMenu)
    }

    @Test
    fun `signup first asks teacher or manager`() {
        val screen = source("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt")
        val viewModel = source("app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt")
        assertTrue("AuthScreen.REGISTRATION_ROLE" in screen)
        assertTrue("Text(\"معلم\")" in screen)
        assertTrue("Text(\"مدیر/معاون\")" in screen)
        assertTrue("ManagerRegistrationPane" in screen)
        assertTrue("ManagerSetupPane" in screen)
        assertTrue("نام مدرسه" in screen && "استان" in screen && "شهر" in screen)
        assertTrue("MANAGER_REGISTER_OTP" in viewModel)
        assertTrue("completeManagerRegistration" in viewModel)
    }

    @Test
    fun `manager registration creates one isolated school after verified email`() {
        val migration = source("supabase/migrations/20260815_native_school_manager_v36.sql")
        val copy = source("sql/manual/SQL_NATIVE_SCHOOL_MANAGER_V36.sql")
        assertEquals(migration, copy)
        listOf(
            "profiles_role_v36_check",
            "create table if not exists public.schools",
            "create table if not exists public.school_memberships",
            "ux_school_one_active_membership_v36",
            "native_complete_manager_registration_v36",
            "staff_role='manager'",
            "native_manager_school_summary_v36"
        ).forEach { assertTrue("missing $it", it in migration) }
        assertTrue("alter table public.schools enable row level security" in migration)
        assertTrue("alter table public.school_memberships enable row level security" in migration)
    }

    @Test
    fun `manager dock uses teachers and cards only show stats`() {
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("primaryLabel = if (user.role == UserRole.MANAGER) \"معلم‌ها\"" in app)
        assertTrue("Design69Icons.Students" in app)
        assertTrue("ManagerTeachersScreen" in app)
        assertTrue("ManagerStatsScreen" in app)
        assertTrue("UserRole.MANAGER -> WalletScreen" in app)
        assertTrue("createManagerTeacher" in app)
    }

    @Test
    fun `manager hamburger excludes calendar and header`() {
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        val managerMenu = app.substringAfter("} else if (user.role == UserRole.MANAGER) {")
            .substringBefore("} else {")
        assertTrue("\"حساب\"" in managerMenu)
        assertTrue("\"داده‌ها\"" in managerMenu)
        assertTrue("\"تنظیمات\"" in managerMenu)
        assertTrue("\"خروج\"" in managerMenu)
        assertFalse("\"تقویم\"" in managerMenu)
        assertFalse("\"سربرگ\"" in managerMenu)
    }

    @Test
    fun `v37 and v38 actions are not exposed prematurely`() {
        val manager = source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertTrue("ساخت کد دعوت" in manager)
        assertTrue("مبلغ باید مضرب ۱٬۰۰۰ تومان باشد" in manager)
        assertFalse("Deno.env.get" in manager)
    }
}
