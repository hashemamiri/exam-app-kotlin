package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V61.4 — سه اصلاح پس از تست دستگاه V61.0:
 * ۱) خطای عکس کاربر «Could not choose the best candidate function»: از V40C
 *    دو overload از native_add_student_to_classes_v22 (jsonb قدیمی V22 و
 *    uuid[] جدید) وجود داشت و PostgREST انتخاب نمی‌کرد → drop نسخهٔ jsonb.
 * ۲) مخاطبان پیام تقویم و آزمون: فقط «همه، مدارس، کلاس‌ها»، عنوان و دکمه‌ها
 *    وسط‌چین؛ دکمهٔ «دانش‌آموزان» حذف (داده‌های قدیمی students پابرجا).
 * ۳) چندمدرسه‌ای: معلم عضویت نامحدود (حذف unique index تک‌عضویتی V36 و گارد
 *    join)، مدیر چند مدرسه می‌سازد (native_manager_create_school_v61 + دکمهٔ
 *    «ساخت مدرسه جدید» در نمای مدارس مدیر).
 */
class V61_4MultiSchoolCenterAudienceTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val migration by lazy { source("supabase/migrations/20260826_native_multi_school_v61_1.sql") }
    private val calendar by lazy { source("app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }
    private val classesVm by lazy { source("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt") }

    @Test
    fun `duplicate v22 overload is dropped`() {
        assertTrue("drop function if exists public.native_add_student_to_classes_v22(uuid, jsonb);" in migration)
        // سلامت‌سنجی داخل فایل هست
        assertTrue("to_regprocedure('public.native_add_student_to_classes_v22(uuid,jsonb)') is null" in migration)
    }

    @Test
    fun `audience pickers are centered without students button`() {
        // تقویم: وسط‌چین + فقط سه گزینه
        val calAudience = calendar.substringAfter("// V61.1 — مخاطبان و دکمه‌ها وسط‌چین")
            .substringBefore("if (editor.audience == CalendarAudience.SCHOOLS)")
        assertTrue("horizontalAlignment = Alignment.CenterHorizontally" in calAudience)
        assertFalse("CalendarAudience.STUDENTS" in calAudience)
        // آزمون: وسط‌چین + فقط سه چیپ
        val builderAudience = builder.substringAfter("// V61.1 — عنوان و دکمه‌ها وسط‌چین")
            .substringBefore("if (state.audienceMode == \"schools\")")
        assertTrue("Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)" in builderAudience)
        assertFalse("\"students\" to" in builderAudience)
        // مسیر دادهٔ قدیمی students حذف نشده است
        assertTrue("if (state.audienceMode == \"students\")" in builder)
        assertTrue("if (editor.audience == CalendarAudience.STUDENTS)" in calendar)
    }

    @Test
    fun `teachers join unlimited schools and managers create many`() {
        assertTrue("drop index if exists public.ux_school_one_active_membership_v36;" in migration)
        assertTrue("قبلاً عضو همین مدرسه هستید" in migration)
        assertFalse("این معلم قبلاً عضو یک مدرسه است" in migration)
        assertTrue("native_manager_create_school_v61" in migration)
        // لیست مدارس مدیر = ساخته‌شده‌ها یا عضویت؛ کلاس‌های مدرسه برای مدیر همه
        assertTrue("s.created_by=auth.uid()" in migration)
        // UI: دکمهٔ ساخت مدرسه فقط برای مدیر
        assertTrue("onCreateSchool = if (managerTeacherPicker) {" in school)
        assertTrue("Text(\"ساخت مدرسه جدید\")" in school)
        assertTrue("fun createSchool(name: String, province: String, city: String)" in classesVm)
        assertTrue("native_manager_create_school_v61" in classesVm)
    }
}
