package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V61.5 — پنج اصلاح درخواستی:
 * ۱) عمل چهارم «مدرسه جدید» در پنجرهٔ + (مدیر: ساخت مدرسه؛ معلم: عضویت با
 *    کد دعوت — SchoolLaunchAction.CREATE_SCHOOL).
 * ۲) متن «مرحله V37» کارت داده‌های مدیر (عکس کاربر) با توضیح واقعی جایگزین شد.
 * ۳) سطل زباله روی همهٔ کارت‌های کد دعوت؛ حذف کد استفاده‌نشده = ابطال فوری با
 *    پیام صریح؛ کد استفاده‌شده زمان‌سنج ندارد («زمان‌سنج متوقف شد»).
 * ۴) فیلتر لیست دانش‌آموزان: پایه/کلاس/دختر/پسر/عضونشده/مدرسه/معلم(مدیر)
 *    ترکیبی؛ جست‌وجو فقط داخل نتیجهٔ فیلتر فعال.
 * ۵) نام مدرسهٔ سربرگ معلم از لیست مدارس عضو + «سایر» تایپ در همان فیلد.
 */
class V61_5QuickSchoolFilterInviteTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val add by lazy { source("app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt") }
    private val app by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }
    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }
    private val classesVm by lazy { source("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt") }
    private val manager by lazy { source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt") }
    private val profile by lazy { source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt") }

    @Test
    fun `quick add has a fourth school action for both roles`() {
        assertTrue("const val ACTION_COUNT = 4" in add)
        assertTrue("title = \"مدرسه جدید\"" in add)
        assertTrue("onCreateSchool = ::createSchool" in app)
        assertTrue("SchoolLaunchAction.CREATE_SCHOOL" in school)
        // مدیر → ساخت مدرسه؛ معلم → عضویت با کد دعوت
        assertTrue("if (managerTeacherPicker) creatingSchool = true else joiningSchool = true" in school)
        assertTrue("عضویت در مدرسه جدید" in school)
        assertTrue("joinRepository.join(joinCode)" in school)
    }

    @Test
    fun `manager data card text is real and invite cards behave as requested`() {
        assertTrue("نیازی به پشتیبان‌گیری دستی نیست" in profile)
        // سطل زباله همهٔ کارت‌ها + پیام ابطال فوری کد استفاده‌نشده
        assertTrue("کارت حذف شد و کد استفاده‌نشده بلافاصله منقضی شد." in manager)
        // زمان‌سنج کد استفاده‌شده متوقف است
        assertTrue("کد استفاده شد؛ زمان‌سنج متوقف شد." in manager)
        assertTrue("if (used) return" in manager)
    }

    @Test
    fun `student list filter is combinable and search respects it`() {
        assertTrue("internal fun applyStudentFilter(" in school)
        assertTrue("private fun StudentFilterDialog(" in school)
        assertTrue("Icons.Outlined.FilterList" in school)
        // ترتیب: اول فیلتر بعد جست‌وجو
        assertTrue("filteredStudents(\n                        applyStudentFilter(state.students, state.studentFilter, state.classes, state.filterMeta),\n                        state.query\n                    )" in school)
        // گزینه‌ها
        for (needle in listOf("Text(\"عضو نشده\")", "Text(\"مدرسه\")", "showTeacherFilter")) {
            assertTrue(needle, needle in school)
        }
        assertTrue("data class StudentListFilter(" in classesVm)
        assertTrue("native_student_filter_meta_v61" in classesVm)
    }

    @Test
    fun `teacher header school picks from memberships with custom typing`() {
        assertTrue("native_teacher_schools_v61" in profile)
        assertTrue("customLabel = \"سایر مدرسه\"" in profile)
        assertTrue("label = \"نام مدرسه\"" in profile)
    }
}
