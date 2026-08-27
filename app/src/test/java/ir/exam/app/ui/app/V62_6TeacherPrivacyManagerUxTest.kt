package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V62.6 — هشت درخواست کاربر:
 * ۱) حریم خصوصی معلم: دانش‌آموزان/کلاس‌های ساختهٔ معلم فقط با تأیید خودش
 *    (سوییچ اشتراک، قابل تغییر) برای مدیر قابل مشاهده باشند.
 * ۲) کلاسِ پنل مدیر: فقط لیست اعضا + دکمهٔ + با «افزودن جدید/افزودن موجود»
 *    و فهرست موجود با فیلتر (بخش قدیمی افزودن از فهرست حذف).
 * ۳) هدر: «کلاس‌های نام معلم» و داخل کلاس نام کلاس (به‌جای «معلم‌ها»).
 * ۴) ریپل خاکستری کارت معلم حذف شود.
 * ۵) کد دعوت با انتخاب مدرسهٔ مقصد.
 * ۶) رفع باگ فیلتر مدیر: بخش کلاس‌ها خالی باز می‌شد.
 * ۷) کارت مدارس داک بدون «بازگشت به کلاس‌ها»؛ کارنامه/وضعیت منوی مستقل.
 * ۸) بازگشت از پنجرهٔ کد دعوت → داشبورد.
 */
class V62_6TeacherPrivacyManagerUxTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val sql by lazy { source("supabase/migrations/20260827_native_teacher_privacy_invite_school_v62_6.sql") }
    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }
    private val classesVm by lazy { source("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt") }
    private val appShell by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }
    private val foundation by lazy { source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt") }
    private val teacherClass by lazy { source("app/src/main/java/ir/exam/app/ui/manager/ManagerTeacherClassScreen.kt") }
    private val managerRepo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseManagerRepository.kt") }
    private val schoolRepo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt") }

    @Test
    fun `teacher creations stay hidden from the manager until shared and it is reversible`() {
        // SQL: ستون اشتراک + توابع تغییر + گاردهای دید مدیر
        assertTrue(sql == source("sql/manual/SQL_NATIVE_TEACHER_PRIVACY_INVITE_SCHOOL_V62_6.sql"))
        for (needle in listOf(
            "add column if not exists shared_with_manager boolean not null default false",
            "native_teacher_share_class_v62",
            "native_teacher_share_student_v62",
            "c.shared_with_manager or coalesce(c.created_by,c.teacher_id)<>c.teacher_id",
            "ss.shared_with_manager or ss.created_by=auth.uid()"
        )) assertTrue(needle, needle in sql)
        // کلاینت: سوییچ اشتراک روی کارت کلاس معلم؛ هر لحظه قابل تغییر
        assertTrue("fun setClassShared(id: String, shared: Boolean)" in classesVm)
        assertTrue("native_teacher_share_class_v62" in schoolRepo)
        assertTrue("native_teacher_share_student_v62" in schoolRepo)
        assertTrue("قابل مشاهده برای مدیر مدرسه" in school)
        assertTrue("onShareChanged = if (!managerTeacherPicker) {" in school)
    }

    @Test
    fun `manager class roster uses a plus dialog with filtered existing picker`() {
        // بخش قدیمی «افزودن از فهرست...» به‌عنوان لیست باز حذف شد؛ + جایگزین است
        assertTrue("FloatingActionButton(" in teacherClass)
        assertTrue("Text(\"افزودن جدید\")" in teacherClass)
        assertTrue("Text(\"افزودن موجود\")" in teacherClass)
        assertTrue("label={Text(\"فیلتر نام یا نام کاربری\")}" in teacherClass)
        assertTrue("onCreateStudent = ::createStudent" in appShell)
        // هدر پویا: کلاس‌های معلم / نام کلاس
        assertTrue("onTitleChanged(if(selected==null)\"کلاس‌های ${'$'}{teacherName.ifBlank{\"معلم\"}}\" else roster?.className.orEmpty().ifBlank{\"کلاس\"})" in teacherClass)
        assertTrue("managerClassHeader != null) managerClassHeader" in appShell)
    }

    @Test
    fun `teacher card has no ripple and invites pick a target school`() {
        // ریپل کارت معلم حذف (الگوی V62.1.4)
        val teacherCard = foundation.substringAfter("teachers.forEach { teacher ->")
            .substringBefore("expandedTeacher == teacher.id) null else teacher.id")
        assertTrue("indication = null" in teacherCard)
        // کد دعوت: لیست مدارس و RPC جدید با مدرسه
        assertTrue("معلم به کدام مدرسه بپیوندد؟" in foundation)
        assertTrue("repository.createInvites(inviteCount, inviteSchoolId)" in foundation)
        assertTrue("native_manager_create_teacher_invites_v62" in managerRepo)
        assertTrue("suspend fun managerSchools()" in managerRepo)
        assertTrue("native_manager_create_teacher_invites_v62" in sql)
        // بازگشت از پنجرهٔ کد دعوت → داشبورد
        assertTrue("onInviteBack = ::openManagerDashboard" in appShell)
        assertTrue("onInviteBack?.invoke() ?: reloadTeachers()" in foundation)
    }

    @Test
    fun `manager filter classes load and cards get their own menus`() {
        // رفع باگ فیلتر: کلاس‌های مدیر از RPC مدرسه
        assertTrue("fun loadManagerFilterClasses()" in classesVm)
        assertTrue("native_manager_school_classes_v62" in classesVm)
        assertTrue("if (managerTeacherPicker) state.managerFilterClasses else state.classes" in school)
        assertTrue("native_manager_school_classes_v62" in sql)
        // کارت مدارس داک: بدون بازگشت به کلاس‌ها
        assertTrue("showBackToClasses: Boolean = true" in school)
        assertTrue("showBackToClasses = !(schoolsFromDock && managerTeacherPicker)" in school)
        // کارنامه/وضعیت: منوی مستقل
        assertTrue("section: String = \"status\"" in foundation)
        assertTrue("val reportMode = section == \"report\"" in foundation)
        assertTrue("Text(\"پنل کارنامه\", style = MaterialTheme.typography.titleMedium)" in foundation)
        assertTrue("section = managerCardsSection ?: \"status\"" in appShell)
    }
}
