package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هات‌فیکس V58.0.1 — گزارش CI پس از V58.x:
 * «ZoomableFigureDialog.kt:13:43 Cannot access
 *  'val RowColumnParentData?.weight: Float': it is internal in file.»
 *
 * ریشه: پچ V58.0 هنگام بازنویسی ZoomableFigureDialog به‌اشتباه import
 * سطح‌بالای androidx.compose.foundation.layout.weight را اضافه کرد. weight
 * فقط تابع عضو RowScope/ColumnScope است؛ آن import به property داخلی
 * RowColumnParentData.weight اشاره می‌کند و کامپایل را می‌شکند.
 *
 * راه‌حل: حذف import — هر دو استفادهٔ فایل داخل scope درست‌اند
 * (Text داخل Row و BoxWithConstraints داخل Column) و بدون import کامپایل
 * می‌شوند. این تست همان الگو را برای کل سورس نیز قفل می‌کند.
 */
class V58_0_1WeightImportHotfixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val zoomDialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/figure/ZoomableFigureDialog.kt").readText()
    }

    @Test
    fun `zoom dialog no longer imports the internal weight extension`() {
        assertFalse("import androidx.compose.foundation.layout.weight" in zoomDialog)
        // استفاده‌های weight سر جای خود ماندند (داخل Row/Column).
        assertTrue("modifier = Modifier.weight(1f)" in zoomDialog)
        assertTrue("BoxWithConstraints(Modifier.fillMaxWidth().weight(1f))" in zoomDialog)
    }

    @Test
    fun `no source file imports the top level weight or align extensions`() {
        val bad = mutableListOf<String>()
        File(root(), "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                if ("import androidx.compose.foundation.layout.weight" in text ||
                    "import androidx.compose.foundation.layout.align" in text
                ) bad += file.name
            }
        assertTrue("scoped-modifier imports leaked into: $bad", bad.isEmpty())
    }
}
