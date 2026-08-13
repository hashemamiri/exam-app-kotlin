package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V18NavigationAccountTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    @Test
    fun `header grade is server persisted backed up and printed`() {
        val root = root()
        val sql = File(root, "supabase/migrations/20260813_native_navigation_account_v18.sql").readText()
        val profile = File(root, "app/src/main/java/ir/exam/app/domain/model/ProfileModels.kt").readText()
        val dto = File(root, "app/src/main/java/ir/exam/app/data/dto/NativeProfileDtos.kt").readText()
        val repository = File(root, "app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt").readText()
        val print = File(root, "app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt").readText()
        val portability = File(root, "app/src/main/java/ir/exam/app/data/repository/SupabasePortabilityRepository.kt").readText()

        listOf("hdr_grade", "native_save_profile", "native_export_backup_v2", "native_restore_backup_v2").forEach {
            assertTrue("missing SQL marker $it", it in sql)
        }
        assertTrue("val grade: String" in profile)
        assertTrue("@SerialName(\"hdr_grade\")" in dto)
        assertTrue("put(\"p_hdr_grade\"" in repository)
        assertTrue("پایه: ${'$'}{header.grade}" in print)
        assertTrue("native_export_backup_v2" in portability)
        assertTrue("native_restore_backup_v2" in portability)
    }

    @Test
    fun `question bank is independently managed with owner RPC`() {
        val root = root()
        val sql = File(root, "supabase/migrations/20260813_native_navigation_account_v18.sql").readText()
        val repository = File(root, "app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt").readText()
        val screen = File(root, "app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt").readText()
        val viewModel = File(root, "app/src/main/java/ir/exam/app/ui/bank/QuestionBankViewModel.kt").readText()

        assertTrue("native_bank_update_question_v1" in sql)
        assertTrue("where id = p_id and teacher_id = auth.uid()" in sql)
        assertTrue("revoke all on function public.native_bank_update_question_v1" in sql)
        assertTrue("updateBankQuestion" in repository)
        listOf("جست‌وجوی متن یا درس", "دسته جدید", "افزودن به آزمون", "ویرایش", "حذف").forEach {
            assertTrue("missing bank UI $it", it in screen)
        }
        assertTrue("visibleQuestions" in viewModel)
    }

    @Test
    fun `account uses verified email and system credential only`() {
        val root = root()
        val account = File(root, "app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt").readText()
        val repository = File(root, "app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt").readText()
        val lockUi = File(root, "app/src/main/java/ir/exam/app/ui/security/AppLockUi.kt").readText()
        val lockManager = File(root, "app/src/main/java/ir/exam/app/core/security/AppLockManager.kt").readText()

        listOf("مشخصات حساب", "تغییر نام کاربری", "تغییر ایمیل", "تغییر رمز عبور", "AppLockSettings").forEach {
            assertTrue("missing account feature $it", it in account)
        }
        assertTrue("auth.updateUser { email = clean }" in repository)
        assertTrue("BiometricPrompt" in lockUi)
        assertTrue("DEVICE_CREDENTIAL" in lockUi)
        assertFalse("custom PIN field returned", "پین جدید" in lockUi)
        assertFalse("PBKDF2 PIN storage returned", "SecretKeyFactory" in lockManager)
    }
}
