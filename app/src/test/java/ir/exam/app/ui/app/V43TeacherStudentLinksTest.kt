package ir.exam.app.ui.app
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class V43TeacherStudentLinksTest {
 private fun root()=listOf(File("."),File("..")).first{File(it,"supabase/migrations").isDirectory}
 private fun source(p:String)=File(root(),p).readText()
 @Test fun `teacher list uses explicit links without ownership transfer`(){val sql=source("supabase/migrations/20260816_native_teacher_student_links_v43.sql");assertEquals(sql,source("sql/manual/SQL_NATIVE_TEACHER_STUDENT_LINKS_V43.sql"));listOf("teacher_student_links","primary key(teacher_id,student_id)","source_class_id","p.teacher_id=auth.uid()","in_my_list boolean").forEach{assertTrue(it in sql)};assertTrue("update profiles set teacher_id" !in sql)}
 @Test fun `adoption requires current membership in teachers class`(){val sql=source("supabase/migrations/20260816_native_teacher_student_links_v43.sql");assertTrue("c.teacher_id=auth.uid()" in sql);assertTrue("cm.student_id=p_student" in sql);assertTrue("native_teacher_add_class_student_to_list_v43" in sql)}
 @Test fun `manager-created teacher class accepts only teachers own linked students`(){val sql=source("supabase/migrations/20260816_native_teacher_student_links_v43.sql");assertTrue("add_students_to_class" in sql);assertTrue("teacher_id=auth.uid()" in sql);assertTrue("exists(select 1 from teacher_student_links" in sql)}
 @Test fun `roster offers explicit add to my list action`(){val ui=source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt");assertTrue("!student.inMyList" in ui);assertTrue("افزودن به لیست دانش‌آموزان من" in ui);assertTrue("onAddToMyList(student.id)" in ui)}
}
