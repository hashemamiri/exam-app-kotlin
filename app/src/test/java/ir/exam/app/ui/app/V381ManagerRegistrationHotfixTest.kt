package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V381ManagerRegistrationHotfixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    @Test
    fun `empty provisional teacher may convert but real teacher data blocks conversion`() {
        val root = root()
        val migration = File(root, "supabase/migrations/20260815_native_manager_registration_v381_hotfix.sql").readText()
        val copy = File(root, "sql/manual/SQL_NATIVE_MANAGER_REGISTRATION_V381_HOTFIX.sql").readText()
        assertEquals(migration, copy)
        assertTrue("v_profile.role not in ('student','teacher','manager')" in migration)
        assertTrue("v_profile.role = 'teacher'" in migration)
        assertTrue("from public.classes c where c.teacher_id = v_uid" in migration)
        assertTrue("from public.exams e where e.teacher_id = v_uid" in migration)
        assertTrue("s.teacher_id = v_uid and s.role = 'student'" in migration)
        assertTrue("sm.user_id = v_uid and sm.status = 'active'" in migration)
        assertTrue("این ایمیل قبلاً حساب معلم فعال دارد" in migration)
    }
}
