package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V63.7 — ویرایشگر سند «همانند برگهٔ چاپ» (اسکرین‌شات‌های کاربر):
 * ۱) بالای هر صفحهٔ A4 همان سربرگ رسمی ۵سطری چاپ (HeaderPreview مشترک با
 *    پنجرهٔ سربرگ) + در صفحهٔ اول سطر «درس/مدت/بارم» داخل کادر.
 * ۲) عنوان سادهٔ بالای کاغذ حذف؛ پاصفحه مثل PDF: امضای دبیر/مدیر +
 *    «صفحهٔ N از M».
 * ۳) سربرگ از پروفایل معلم می‌آید (profilePrintHeader جدید) و درس/مدت از
 *    state خود آزمون.
 */
class V63_7EditorPrintParityTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }
    private val portability by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabasePortabilityRepository.kt") }

    @Test
    fun `every page starts with the official five-row header like print`() {
        assertTrue("HeaderPreview(header)" in editor)
        assertTrue("fun headerFor(pageIndex: Int)" in editor)
        assertTrue("val pageHeader = if (pageIndex == 0) firstHeader else headerFor(pageIndex)" in editor)
        // سطر مشخصات فقط صفحهٔ اول و داخل کادر مثل چاپ
        assertTrue("infoLine" in editor)
        assertTrue("\"درس: \" + state.subject.ifBlank { \"—\" }" in editor)
        // صفحه‌بندی جای سربرگ را کم می‌کند
        assertTrue("var used = firstHeader.height + gapPx" in editor)
        assertTrue("used = firstHeader.height + gapPx" in editor)
    }

    @Test
    fun `paper chrome mirrors the pdf footer instead of a plain title`() {
        assertTrue("نام و امضای دبیر:            نام و امضای مدیر:" in editor)
        assertTrue("صفحهٔ \$pageNumber از \$pageCount" in editor)
        // عنوان وسط‌چین قدیمی بالای کاغذ حذف شد
        assertFalse("fontSize = (11 * zoom).sp" in editor)
    }

    @Test
    fun `profile header feeds the editor preview`() {
        assertTrue("suspend fun profilePrintHeader(): Result<OfficialPrintHeader>" in portability)
        assertTrue("SupabasePortabilityRepository().profilePrintHeader()" in editor)
        assertTrue("printHeader.copy(" in editor)
        assertTrue("subject = state.subject" in editor)
    }
}
