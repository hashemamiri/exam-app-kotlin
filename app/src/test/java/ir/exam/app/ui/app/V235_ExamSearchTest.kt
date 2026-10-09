package ir.exam.app.ui.app

import ir.exam.app.core.ui.ExamListFilter
import ir.exam.app.core.ui.examMatches
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V235 — ذره‌بین کنار «+» (آزمون‌ها) و سمت چپ «آزمون‌های آنلاین» (چاپ)؛ فیلد مثل دانش‌آموزان با آیکن فیلتر در سمت دیگر. */
class V235_ExamSearchTest {
    private fun src(rel: String): String = File(listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }, rel).readText()

    @Test
    fun matching() {
        assertTrue(examMatches("", "a"))
        assertTrue(examMatches("ریاضی", "آزمون", "ریاضی هفتم", null))
        assertTrue(examMatches("ab12", "x", null, "AB12CD"))
        assertFalse(examMatches("فیزیک", "ریاضی", null, null))
        assertTrue(ExamListFilter(status = "open").isActive && !ExamListFilter().isActive)
    }

    @Test
    fun wiring() {
        val bar = src("app/src/main/java/ir/exam/app/core/ui/ExamSearchBar.kt")
        assertTrue("leadingIcon = {" in bar && "Icons.Outlined.FilterList" in bar && "trailingIcon = {" in bar && "Icons.Outlined.Close" in bar)
        assertTrue("tint = if (filter.isActive) Color(0xFFD32F2F) else LocalContentColor.current" in bar)
        val dash = src("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt")
        assertTrue("ExamSearchIcon(open = examSearchOpen, onOpen = { examSearchOpen = true })" in dash && "items(visibleExams, key = { it.id })" in dash && "showStatus = true" in dash)
        val print = src("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt")
        assertTrue("ExamSearchIcon(open = printSearchOpen, onOpen = { printSearchOpen = true })" in print && "items(shownExams, key = { \"local-\" + it.id })" in print && "showStatus = false" in print)
    }
}
