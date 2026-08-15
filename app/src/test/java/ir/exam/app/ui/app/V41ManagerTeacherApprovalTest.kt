package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V41ManagerTeacherApprovalTest {
    private fun root() = listOf(File("."), File("..")).first { File(it, "supabase/migrations").isDirectory }
    private fun source(path: String) = File(root(), path).readText()

    @Test fun `approval migration is additive auditable and expires after 24 hours`() {
        val migration = source("supabase/migrations/20260816_native_manager_teacher_approval_v41.sql")
        assertEquals(migration, source("sql/manual/SQL_NATIVE_MANAGER_TEACHER_APPROVAL_V41.sql"))
        listOf("manager_approval_requests", "interval '24 hours'", "approved", "rejected", "expired", "executed", "decided_at", "executed_at", "security definer").forEach { assertTrue(it in migration) }
    }

    @Test fun `teacher owns decision and class execution`() {
        val migration = source("supabase/migrations/20260816_native_manager_teacher_approval_v41.sql")
        assertTrue("teacher_id=auth.uid() for update" in migration)
        assertTrue("native_teacher_decide_manager_request_v41" in migration)
        assertTrue("delete from classes where id=r.target_id and teacher_id=auth.uid()" in migration)
    }

    @Test fun `student edge requires approval but membership rpc stays independent`() {
        val edge = source("supabase/functions/manage-student/index.ts")
        assertTrue("manager_approval_requests" in edge)
        assertTrue("current.teacher_id !== teacherId" in edge)
        assertTrue("approval_id: approvalId" in edge)
        val manager = source("app/src/main/java/ir/exam/app/data/repository/SupabaseManagerRepository.kt")
        assertTrue("native_manager_set_class_student_v40c" in manager)
        assertTrue("native_manager_change_teacher_class_v41" in manager)
    }

    @Test fun `teacher dashboard exposes manager request inbox`() {
        val ui = source("app/src/main/java/ir/exam/app/ui/dashboard/TeacherManagerRequestsScreen.kt")
        assertTrue("درخواست‌های مدیر" in ui)
        assertTrue("decide(request.id, true)" in ui)
        assertTrue("decide(request.id, false)" in ui)
    }
}
