package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V61.2 — پنل مدیر/معاون:
 * - کارت وسط‌چین «داشبورد» زیر کارت پروفایل منوی همبرگری (featuredCard).
 * - داشبورد: اطلاعات مدرسه + آمار + پنل سریع.
 * - کارت‌های کلاس‌ها/دانش‌آموزان منو: «فهرست و مدیریت».
 * - ساخت کلاس جدید مدیر: کادر وسط‌چین «معلم» زیر نام کلاس که لیست معلم‌های
 *   عضو مدرسه را باز می‌کند و کلاس برای معلم انتخابی ساخته می‌شود (V40C).
 */
class V61_2ManagerDashboardClassTeacherTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val app by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }
    private val manager by lazy { source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt") }
    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }
    private val classesVm by lazy { source("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt") }

    @Test
    fun `manager menu has centered dashboard card under profile`() {
        assertTrue("featuredCard = if (user.role == UserRole.MANAGER) {" in app)
        assertTrue("\"داشبورد\", \"اطلاعات مدرسه و آمار\", Design69Icons.Dashboard," in app)
        // کارت‌های منو: فهرست و مدیریت
        val managerMenu = app.substringAfter("} else if (user.role == UserRole.MANAGER) {")
            .substringBefore("} else {")
        assertTrue("\"کلاس‌ها\", \"فهرست و مدیریت\", Design69Icons.Classes," in managerMenu)
        assertTrue("\"دانش‌آموزان\", \"فهرست و مدیریت\", Design69Icons.Students," in managerMenu)
    }

    @Test
    fun `dashboard shows school info and quick panel`() {
        // V62.6 — عنوان بخش‌بندی شد: «داشبورد» یا «کارنامه مدرسه».
        assertTrue("Text(if (reportMode) \"کارنامه مدرسه\" else \"داشبورد\", style = MaterialTheme.typography.headlineSmall)" in manager)
        assertTrue("summary.schoolName.ifBlank { \"مدرسه\" }" in manager)
        assertTrue("Text(\"پنل سریع\", style = MaterialTheme.typography.titleMedium)" in manager)
        assertTrue("private fun QuickPanelCard(" in manager)
        assertTrue("onQuickWallet = { page = MainPage.WALLET }" in app)
    }

    @Test
    fun `manager class editor picks a school teacher`() {
        assertTrue("managerTeacherPicker: Boolean = false" in school)
        assertTrue("managerTeacherPicker = user.role == UserRole.MANAGER" in app)
        assertTrue("Text(\"انتخاب معلم\") }" in school)
        assertTrue("teachers.firstOrNull { it.id == teacherId }?.name" in school)
        // ساخت برای معلم انتخابی از مسیر V40C و لیست معلم‌ها از V37
        assertTrue("native_manager_save_teacher_class_v40c" in classesVm)
        assertTrue("fun loadSchoolTeachers()" in classesVm)
        assertTrue("native_manager_teachers_v37" in classesVm)
        // بدون انتخاب معلم ذخیره غیرفعال است
        assertTrue("enabled = name.isNotBlank() && (teachers.isEmpty() || teacherId != null)" in school)
    }
}
