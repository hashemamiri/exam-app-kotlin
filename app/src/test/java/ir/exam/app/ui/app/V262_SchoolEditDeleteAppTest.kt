package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V262 — پاریتهٔ اپ با سایت V260.9: مدیر در «مدارس» می‌تواند مدرسه را ویرایش/حذف کند
 * (native_manager_update_school_v1 / native_manager_delete_school_v1)؛ معلم برای کلاس مدیرساخته
 * به‌جای چشم «اشتراک با مدیر» چیپ «کلاس مدیر» می‌بیند (native_my_classes_flags_v1).
 */
class V262_SchoolEditDeleteAppTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `view model has update, delete school and manager-created flags`() {
        val vm = src("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt")
        assertTrue("\"native_manager_update_school_v1\"" in vm)
        assertTrue("\"native_manager_delete_school_v1\"" in vm)
        assertTrue("\"native_my_classes_flags_v1\"" in vm)
        assertTrue("val managerCreatedClassIds: Set<String> = emptySet()" in vm)
    }

    @Test
    fun `school cards offer edit and delete only for manager and class card shows manager chip`() {
        val screen = src("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        assertTrue("onEditSchool = if (managerTeacherPicker) { { editingSchool = it } } else null" in screen)
        assertTrue("onDeleteSchool = if (managerTeacherPicker) { { deletingSchool = it } } else null" in screen)
        assertTrue("title = { Text(\"ویرایش مدرسه\") }" in screen)
        assertTrue("title = { Text(\"حذف مدرسه\") }" in screen)
        assertTrue("if (item.id in managerCreatedIds) {" in screen && "\"کلاس مدیر\"" in screen)
    }
}
