package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V336.0 — مدیریت چندمدرسه‌ای کلاس‌ها و معلم‌ها:
 * ساخت کلاس مستقل در هر مدرسه (با معلم یا بدون معلم)، اختصاص بعدی معلم عضو همان مدرسه،
 * و کپی کلاس + دانش‌آموزان به مدرسهٔ دیگر بدون کپی آزمون/پاسخ.
 */
class V336_MultiSchoolClassesTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/admin.js").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `sql exposes school scoped class and teacher RPCs`() {
        val sql = src("supabase/migrations/20261103_native_multi_school_classes_v336.sql")
        listOf(
            "native_manager_school_teachers_v336",
            "native_manager_set_teacher_active_v336",
            "native_manager_save_school_class_v336",
            "native_manager_school_classes_v336",
            "native_manager_assign_class_teacher_v336",
            "native_manager_copy_class_to_school_v336"
        ).forEach { assertTrue(it, it in sql) }
        assertTrue("alter table public.classes alter column teacher_id drop not null" in sql)
        assertTrue("grant execute on function public.native_manager_save_school_class_v336(uuid, text, text, text, uuid) to authenticated" in sql)
        assertTrue("grant execute on function public.native_manager_copy_class_to_school_v336(uuid, uuid, uuid, text) to authenticated" in sql)
    }

    @Test
    fun `copy moves only class and students, not exams or answers`() {
        val sql = src("supabase/migrations/20261103_native_multi_school_classes_v336.sql")
        assertTrue("insert into public.class_members(class_id, student_id)" in sql)
        assertTrue("insert into public.school_students(school_id, student_id, created_by, shared_with_manager)" in sql)
        assertFalse("insert into public.exams" in sql)
        assertFalse("insert into public.answers" in sql)
        assertTrue("مدرسهٔ مقصد باید با مدرسهٔ مبدأ متفاوت باشد" in sql)
    }

    @Test
    fun `android manager school classes can create, assign and copy`() {
        val ui = src("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        assertTrue("کلاس جدید در این مدرسه" in ui)
        assertTrue("اختصاص معلم" in ui)
        assertTrue("کپی به مدرسهٔ دیگر" in ui)
        assertTrue("بدون معلم (اختصاص بعدی)" in ui)
        val vm = src("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt")
        assertTrue("native_manager_save_school_class_v336" in vm)
        assertTrue("native_manager_assign_class_teacher_v336" in vm)
        assertTrue("native_manager_copy_class_to_school_v336" in vm)
        assertTrue("native_manager_school_teachers_v336" in vm)
    }

    @Test
    fun `android teacher membership actions are school scoped`() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseManagerRepository.kt")
        assertTrue("native_manager_school_teachers_v336" in repo)
        assertTrue("native_manager_set_teacher_active_v336" in repo)
        val ui = src("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertTrue("schoolFilterId" in ui)
        assertTrue("repository.setTeacherActive(teacher.id, !teacher.active, schoolFilterId)" in ui)
    }

    @Test
    fun `site manager has per-school class management`() {
        val admin = src("site/src/admin.js")
        assertTrue("managerSchoolClassesPage" in admin)
        assertTrue("native_manager_save_school_class_v336" in admin)
        assertTrue("native_manager_assign_class_teacher_v336" in admin)
        assertTrue("native_manager_copy_class_to_school_v336" in admin)
        assertTrue("کلاس‌های مدرسه" in admin)
        assertTrue("بدون معلم (اختصاص بعدی)" in admin)
    }
}
