package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V291 — آیکن Word واضح (اپ + سایت)، فونت وزیرمتن در صفحه‌های ثابت سایت، دکمه‌های پنجرهٔ افزودن گروهی دانش‌آموز. */
class V291_WordIconFontsButtonsTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun wordIconIsTheSameClearGlyphInAppAndSite() {
        val icons = src("app/src/main/java/ir/exam/app/ui/app/Design69Icons.kt")
        assertTrue("val Word: ImageVector by lazy" in icons)
        assertTrue("strokePath(2.7f)" in icons)
        val site = src("site/src/app.js")
        assertTrue("<path stroke-width=\"2.7\" d=\"M6.8 11l1.9 7.8 3.3-6 3.3 6 1.9-7.8\"/>" in site)
        assertTrue("Icons.Outlined.Description" !in src("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt"))
        assertTrue("Icons.Outlined.Description" !in src("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt"))
    }

    @Test
    fun staticPagesEmbedVazirmatn() {
        for (p in listOf("site/terms.html", "site/privacy.html", "site/status.html")) {
            val h = src(p)
            assertTrue(p, "src:url(/fonts/Vazirmatn-400.woff2) format('woff2')" in h)
            assertTrue(p, "font-family:'Vazirmatn',Tahoma,'Segoe UI',sans-serif" in h)
        }
    }

    @Test
    fun bulkStudentButtonsFitOnOneRow() {
        val s = src("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        assertTrue("modifier = Modifier.weight(1.4f)\n                        ) { Text(\"ایجاد\", maxLines = 1, softWrap = false) }" in s)
        assertTrue("modifier = Modifier.weight(1.3f).semantics { contentDescription = \"ورود از اکسل\" }\n                        ) { Text(\"اکسل\", maxLines = 1, softWrap = false) }" in s)
        assertTrue("import androidx.compose.foundation.layout.PaddingValues" in s)
    }
}
