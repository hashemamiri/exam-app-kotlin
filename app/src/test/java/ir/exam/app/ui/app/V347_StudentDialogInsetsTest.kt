package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V347 — پنجرهٔ «دانش‌آموز جدید» (و ویرایش دانش‌آموز) زیر نوار اعلان می‌رفت.
 *
 * علت: هر دو پنجره `decorFitsSystemWindows = false` دارند (تمام‌صفحه، لبه‌به‌لبه)
 * ولی هیچ محافظ نوار سیستم نداشتند؛ فقط `imePadding()` و ۱۰dp فاصله.
 * V247 همین اشکال را در پنجره‌های شکل/جدول رفع کرده بود اما این دو جا مانده بودند.
 *
 * این آزمون یک جاروب کلی هم دارد: هر پنجرهٔ لبه‌به‌لبه در کل اپ باید محافظ
 * نوار سیستم داشته باشد، وگرنه سرِ آن زیر نوار وضعیت می‌رود.
 */
class V347_StudentDialogInsetsTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/AndroidManifest.xml").isFile }
    private fun src(p: String) = File(root(), p).readText()

    private val insetGuards = listOf(
        "systemBarsPadding", "statusBarsPadding", "safeDrawingPadding", "safeContentPadding",
        "WindowInsets.systemBars", "WindowInsets.statusBars", "WindowInsets.safeDrawing"
    )

    @Test
    fun `student create and edit dialogs keep clear of the status bar`() {
        val screen = src("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        assertTrue("import محافظ نوار سیستم", "import androidx.compose.foundation.layout.systemBarsPadding" in screen)

        // هر دو پنجره: ساخت گروهی («دانش‌آموز جدید») و ویرایش تکی
        val fixed = "Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 14.dp, vertical = 10.dp)"
        val count = Regex(Regex.escape(fixed)).findAll(screen).count()
        assertTrue("هر دو پنجرهٔ دانش‌آموز باید اصلاح شده باشند ولی $count مورد پیدا شد", count >= 2)

        // نسخهٔ معیوب نباید باقی مانده باشد
        assertTrue("نسخهٔ بدون محافظ نباید بماند",
            "Modifier.fillMaxSize().imePadding().padding(horizontal = 14.dp, vertical = 10.dp)" !in screen)

        // ترتیب مهم است: اول نوار سیستم، بعد صفحه‌کلید
        assertTrue("ترتیب محافظ‌ها", ".systemBarsPadding().imePadding()" in screen)
    }

    @Test
    fun `every edge to edge dialog in the app guards the system bars`() {
        val offenders = mutableListOf<String>()
        val marker = Regex("decorFitsSystemWindows\\s*=\\s*false")
        File(root(), "app/src/main/java").walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            marker.findAll(text).forEach { hit ->
                // پنجرهٔ محتوا بلافاصله بعد از تنظیمات می‌آید
                val body = text.substring(hit.range.last + 1, minOf(text.length, hit.range.last + 1400))
                if (insetGuards.none { it in body }) {
                    val line = text.substring(0, hit.range.first).count { it == '\n' } + 1
                    offenders.add("${file.name}:$line")
                }
            }
        }
        assertTrue(
            "این پنجره‌های لبه‌به‌لبه محافظ نوار سیستم ندارند و سرشان زیر نوار اعلان می‌رود: " + offenders.joinToString(),
            offenders.isEmpty()
        )
    }

    @Test
    fun `the V247 figure dialogs keep their existing guard`() {
        for (name in listOf("FigurePickerDialog", "PeriodicEditorDialog", "AtlasEditorDialog", "TableEditorDialog", "ZoomableFigureDialog")) {
            val text = src("app/src/main/java/ir/exam/app/ui/figure/$name.kt")
            assertTrue("$name باید محافظ V247 را نگه دارد", ".systemBarsPadding()" in text)
        }
    }
}
