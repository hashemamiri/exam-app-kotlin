package ir.exam.app.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V86.7 — در مسیرِ «چاپ آزمون ← آزمون جدید»، کارتِ «مشخصات آزمون» جای خود را
 * به «تنظیمات سربرگ» می‌دهد. مسیرِ «ایجاد آزمون آنلاین» نباید تغییر کند.
 */
class V86_7PrintHeaderButtonTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val builder by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()
    }
    private val app by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").readText()
    }
    private val center by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt").readText()
    }
    private val store by lazy {
        File(root(), "app/src/main/java/ir/exam/app/data/local/PrintHeaderStore.kt").readText()
    }

    @Test
    fun `the builder takes a print flag that defaults to the online behaviour`() {
        assertTrue("printMode: Boolean = false" in builder)
    }

    @Test
    fun `the print route shows the header button instead of the exam settings`() {
        assertTrue("if (printMode) {" in builder)
        assertTrue("Text(\"تنظیمات سربرگ\")" in builder)
        // و مشخصات آزمون فقط در شاخهٔ غیرچاپ
        assertTrue("Text(if (settingsExpanded) \"بستن مشخصات آزمون\" else \"مشخصات آزمون\")" in builder)
    }

    @Test
    fun `the online route still shows audience and exam settings`() {
        assertTrue("ExamSettingsCard(state, viewModel)" in builder)
        assertTrue("AudienceCard(state, viewModel)" in builder)
    }

    @Test
    fun `the dialog is the same native one the printable builder uses`() {
        assertTrue("ir.exam.app.ui.printing.HeaderSettingsDialog(" in builder)
        assertTrue("ir.exam.app.ui.printing.loadHeaderSchema(headerContext)" in builder)
    }

    @Test
    fun `a missing schema still gives the user a way out`() {
        assertTrue("قالب‌های سربرگ خوانده نشد" in builder)
    }

    @Test
    fun `only print entry points turn the flag on`() {
        assertTrue("printMode = builderCameFromPrint" in app)
        // V86.8 مسیرِ دومِ چاپ را افزود (بازکردنِ آزمونِ چاپیِ ذخیره‌شده).
        // هر دو باید در بلوکِ ExamPrintCenterScreen باشند، نه جای دیگر.
        val center = app.substringAfter("ExamPrintCenterScreen(").substringBefore("MainPage.SETTINGS")
        assertEquals(
            Regex("builderCameFromPrint = true").findAll(app).count(),
            Regex("builderCameFromPrint = true").findAll(center).count()
        )
    }

    @Test
    fun `every other route into the builder clears the flag`() {
        // اگر یکی از این‌ها جا بیفتد، آزمونِ آنلاین سربرگِ چاپ را نشان می‌دهد
        val lines = app.split("\n")
        lines.forEachIndexed { i, line ->
            if ("page = MainPage.BUILDER" in line) {
                val ctx = lines.subList(maxOf(0, i - 8), i + 1).joinToString("\n")
                assertTrue(
                    "route at line ${i + 1} does not set builderCameFromPrint",
                    "builderCameFromPrint" in ctx
                )
            }
        }
    }

    @Test
    fun `the header values are kept on the device and reach the printed sheet`() {
        assertTrue("print_header_fields" in store)
        assertTrue("fun read(): Map<String, String>" in store)
        assertTrue("ir.exam.app.data.local.PrintHeaderStore(context.applicationContext)" in center)
        // V86.9 — نگاشت به printHeaderOf منتقل شد تا پیش‌نمایش و چاپ یکی باشند
        val store = File(root(), "app/src/main/java/ir/exam/app/data/local/PrintHeaderStore.kt").readText()
        assertTrue("school = fields[\"f_branch\"].orEmpty()" in store)
        assertTrue("subject = fields[\"f_course\"].orEmpty()" in store)
    }

    @Test
    fun `the header templates and logo are untouched`() {
        val schema = File(root(), "app/src/main/assets/print/header_settings_schema.json")
        assertTrue(schema.isFile)
        // قالب‌ها همچنان از همان schema می‌آیند، نه از کدِ تازه
        assertTrue("schema = headerSchema" in builder)
    }
}
