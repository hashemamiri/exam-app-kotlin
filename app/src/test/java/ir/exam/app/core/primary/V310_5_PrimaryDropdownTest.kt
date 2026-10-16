package ir.exam.app.core.primary

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V310.5 — «نوع ابتدایی» و «برچسب ابتدایی» در اپ منوی کشویی‌اند؛ برچسب با عنوان «برای فیلتر بانک سوال»؛
 * تیک/لغو «حالت ابتدایی» در سایت، ویرایشگر را بازسازی می‌کند.
 */
class V310_5_PrimaryDropdownTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists() && !File(dir, "settings.gradle").exists()) dir = dir.parentFile
        return dir ?: File("").absoluteFile
    }

    private fun screen(): String = File(root(), "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()

    @Test
    fun `app primary type and tag are dropdowns`() {
        val s = screen()
        assertTrue(s.contains("private fun PrimaryDropdown("))
        assertTrue(s.contains("PrimaryDropdown(\n            selectedValue = question.mode,"))
        assertTrue(s.contains("PrimaryDropdown(\n                selectedValue = question.grade.toString(),"))
        assertTrue(s.contains("PrimaryDropdown(\n                selectedValue = question.subject,"))
    }

    @Test
    fun `primary tag label matches site`() {
        val s = screen()
        val site = File(root(), "site/src/builder.js").readText()
        assertTrue(s.contains("برچسب ابتدایی (برای فیلتر بانک سوال)"))
        assertTrue(site.contains("برچسب ابتدایی (برای فیلتر بانک سوال)"))
    }

    @Test
    fun `site primary toggle redraws the editor`() {
        val site = File(root(), "site/src/builder.js").readText()
        assertTrue(site.contains("state.primaryMode = v; mark(); if (typeof drawList === 'function') drawList(); if (typeof drawEditor === 'function') drawEditor();"))
    }
}
