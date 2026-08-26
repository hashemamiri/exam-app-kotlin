package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V61.1 — «مدارس» برای معلم و مخاطب «مدارس»:
 * - دکمهٔ «مدارس» کنار «ساخت کلاس جدید»؛ کارت مدرسه → کلاس‌های معلم در آن
 *   مدرسه → با لمس کلاس، همان roster مدیریت دانش‌آموزان باز می‌شود.
 * - مخاطبان پیام تقویم به ترتیب: همه، مدارس، کلاس‌ها (و دانش‌آموزان مثل قبل).
 * - مخاطبان آزمون: همه، مدارس، کلاس‌ها، دانش‌آموزان؛ سرور مدرسه را به
 *   دانش‌آموزان ثبت‌شدهٔ همان مدرسه گسترش می‌دهد (انتخاب مدرسه = همهٔ
 *   دانش‌آموزان ثبت‌شده در مدرسه، حتی بدون کلاس).
 */
class V61_1TeacherSchoolsAudienceTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }
    private val classesVm by lazy { source("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt") }
    private val calendar by lazy { source("app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val builderRepo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt") }
    private val migration by lazy { source("supabase/migrations/20260826_native_schools_audience_v61.sql") }

    @Test
    fun `schools button and drill-down views exist`() {
        assertTrue("OutlinedButton(onClick = onSchools, modifier = Modifier.weight(1f)) { Text(\"مدارس\") }" in school)
        assertTrue("private fun SchoolsContent(" in school)
        assertTrue("private fun SchoolClassesContent(" in school)
        // لمس کلاس مدرسه = همان selectClass و roster موجود
        assertTrue("state.schoolsOpen && state.selectedSchool != null -> SchoolClassesContent(" in school)
        assertTrue("onOpen = viewModel::selectClass" in school)
        assertTrue("fun openSchools()" in classesVm)
        assertTrue("native_teacher_schools_v61" in classesVm)
        assertTrue("native_teacher_school_classes_v61" in classesVm)
    }

    @Test
    fun `calendar audience order is all schools classes`() {
        assertTrue("CalendarAudience.ALL -> \"همه\"" in calendar)
        assertTrue("CalendarAudience.SCHOOLS -> \"مدارس\"" in calendar)
        assertTrue("CalendarAudience.CLASSES -> \"کلاس‌ها\"" in calendar)
        val order = calendar.substringAfter("listOf(\n                            CalendarAudience.ALL,")
        assertTrue(order.indexOf("CalendarAudience.SCHOOLS") < order.indexOf("CalendarAudience.CLASSES"))
        assertTrue("if (editor.audience == CalendarAudience.SCHOOLS) {" in calendar)
    }

    @Test
    fun `exam audience includes schools and server expands to students`() {
        assertTrue("\"schools\" to \"مدارس\"" in builder)
        assertTrue("if (state.audienceMode == \"schools\") {" in builder)
        assertTrue("toggleAudienceSchool" in builder)
        assertTrue("put(\"schools\", buildJsonArray { state.audienceSchools.sorted()" in builderRepo)
        // سرور: گسترش مدرسه به دانش‌آموزان ثبت‌شده + نگه‌داشتن انتخاب مدرسه
        assertTrue("native_exam_school_students_v61" in migration)
        assertTrue("exam_audience_schools" in migration)
        assertTrue("school_students" in migration)
        // پیام تقویم: مدرسه = دانش‌آموزان ثبت‌شده در مدرسه (نه فقط عضو کلاس)
        assertTrue("calendar_note_schools" in migration)
        assertTrue("'schools'" in migration)
    }
}
