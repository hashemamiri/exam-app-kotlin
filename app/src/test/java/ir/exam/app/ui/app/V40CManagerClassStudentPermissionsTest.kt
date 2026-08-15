package ir.exam.app.ui.app
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class V40CManagerClassStudentPermissionsTest{
 private fun root()=listOf(File("."),File("..")).first{File(it,"app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile}
 private fun source(p:String)=File(root(),p).readText()
 @Test fun `sql exposes school students and manager class operations`(){val s=source("supabase/migrations/20260815_native_manager_class_students_v40c.sql");assertEquals(s,source("SQL_NATIVE_MANAGER_CLASS_STUDENTS_V40C.sql"));listOf("exists(select 1 from me join public.school_students","can_manage boolean","native_manager_teacher_classes_v40c","native_manager_save_teacher_class_v40c","native_manager_delete_teacher_class_v40c","native_manager_class_roster_v40c","native_manager_school_students_v40c","native_manager_set_class_student_v40c").forEach{assertTrue(it in s)}}
 @Test fun `manager enter opens class and roster management`(){val app=source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt");val ui=source("app/src/main/java/ir/exam/app/ui/manager/ManagerTeacherClassScreen.kt");assertTrue("ManagerTeacherClassScreen" in app);listOf("کلاس جدید برای معلم","حذف کلاس","دانش‌آموزان کلاس","افزودن از فهرست دانش‌آموزان مدرسه","حذف از کلاس").forEach{assertTrue(it in ui)}}
 @Test fun `roster delete removes membership while list delete removes account`(){val school=source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt");assertTrue("onDelete = { viewModel.removeStudent(it.id) }" in school);assertTrue("membershipOnlyDelete = true" in school);assertTrue("حذف از کلاس" in school);assertTrue("حذف حساب دانش‌آموز" in school);assertTrue("student.canManageAccount" in school)}
 @Test fun `deleting class never deletes student account`(){val s=source("supabase/migrations/20260815_native_manager_class_students_v40c.sql");val body=s.substringAfter("native_manager_delete_teacher_class_v40c").substringBefore("native_manager_class_roster_v40c");assertTrue("delete from public.classes" in body);assertFalse("delete from public.profiles" in body);assertFalse("deleteUser" in body)}
 @Test fun `manager edge access remains school scoped`(){val e=source("supabase/functions/manage-student/index.ts");assertTrue("teacher?.role === 'manager'" in e);assertTrue("school_memberships" in e);assertTrue("school_students" in e)}
}
