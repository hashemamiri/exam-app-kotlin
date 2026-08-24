package ir.exam.app.ui.app

import ir.exam.app.core.figure.AtlasBlankAnswerCodec
import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V57.0 — چهار درخواست سمت دانش‌آموز:
 * ۱) «سطر به سطر مثل معلم»: splitRows هر '\n' معلم را یک سطر مستقل می‌کند و
 *    NativeMathText به‌جای FlowRow سراسری، Column از سطرها می‌سازد.
 * ۲) «زوم شکل/نمودار/تصویر + شکل در سطر کامل»: هر شکل سطر تمام‌عرض خودش را
 *    دارد و در حالت دانش‌آموز (zoomableFigures) لمس آن ZoomableFigureDialog
 *    باز می‌کند (بزرگ‌نمایی ۱..۶ برابر + جابه‌جایی + دوضربه بازنشانی).
 * ۳) «جدول تناوبی افقی»: kind='p' گزینهٔ «نمایش افقی» (چرخش ۹۰ درجه) دارد.
 * ۴) «تایپ در کادرهای نامگذاری»: AtlasFigureView در حالت دانش‌آموز برای هر
 *    نشانهٔ پیکان‌دار OutlinedTextField دارد؛ پاسخ‌ها با AtlasBlankAnswerCodec
 *    به‌صورت «n) پاسخ» در همان TextAnswer ذخیره می‌شوند (بدون تغییر سرور).
 */
class V57_0StudentRichViewTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val mathText by lazy { source("app/src/main/java/ir/exam/app/ui/math/NativeMathText.kt") }
    private val atlasView by lazy { source("app/src/main/java/ir/exam/app/ui/figure/AtlasFigureView.kt") }
    private val zoomDialog by lazy { source("app/src/main/java/ir/exam/app/ui/figure/ZoomableFigureDialog.kt") }
    private val student by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt") }

    // ---------- تست‌های اجرایی JVM ----------

    @Test
    fun `teacher line breaks become student rows and figures take a full row`() {
        val fig = "%%FIG:{\"k\":\"g\",\"t\":\"line\"}%%"
        val rows = RichTextSplitter.splitRows("سطر یک\nسطر دو $fig ادامه\nسطر سه")
        // سطر ۱: متن؛ سطر ۲: «سطر دو» (شکل جدا شد)؛ سطر ۳: شکل؛ سطر ۴: «ادامه»؛ سطر ۵: «سطر سه»
        assertEquals(5, rows.size)
        assertTrue(rows[0].single() is RichSegment.Text)
        assertTrue(rows[2].single() is RichSegment.Figure)
        assertEquals("سطر سه", (rows[4].single() as RichSegment.Text).text)
    }

    @Test
    fun `formulas stay inline while intentional empty lines survive`() {
        val rows = RichTextSplitter.splitRows("الف ${'$'}x${'$'} ب\n\nج")
        assertEquals(3, rows.size)
        assertTrue(rows[0].any { it is RichSegment.Math })
        assertTrue(rows[1].isEmpty()) // اینتر دوم معلم = سطر خالی عمدی
        // سطر خالی که ویرایشگر بعد از توکن شکل می‌گذارد حذف می‌شود:
        val fig = "%%FIG:{\"k\":\"t\"}%%"
        val figRows = RichTextSplitter.splitRows("قبل\n$fig\nبعد")
        assertEquals(listOf(false, true, false), figRows.map { it.singleOrNull() is RichSegment.Figure })
    }

    @Test
    fun `atlas blank answers round trip inside one text answer`() {
        val merged = AtlasBlankAnswerCodec.merge(mapOf(2 to "بطن چپ", 1 to "دهلیز راست"), "توضیح آزاد")
        assertEquals("1) دهلیز راست\n2) بطن چپ\nتوضیح آزاد", merged)
        assertEquals(mapOf(1 to "دهلیز راست", 2 to "بطن چپ"), AtlasBlankAnswerCodec.parse(merged))
        assertEquals("توضیح آزاد", AtlasBlankAnswerCodec.freeText(merged))
        // پاک‌کردن یک کادر، بقیه را نگه می‌دارد
        assertEquals("2) بطن چپ", AtlasBlankAnswerCodec.format(mapOf(1 to " ", 2 to "بطن چپ")))
    }

    // ---------- تست‌های اتصال UI ----------

    @Test
    fun `student text renders row by row with zoomable figures`() {
        assertTrue("RichTextSplitter.splitRows(source)" in mathText)
        assertTrue("zoomableFigures: Boolean = false" in mathText)
        assertTrue("ZoomableFigureDialog(" in mathText)
        assertTrue("zoomableFigures = true" in student)
        // شکل‌ها همیشه سطر مستقل؛ در FlowRow سطری رندر نمی‌شوند
        assertTrue("is RichSegment.Figure -> Unit" in mathText)
    }

    @Test
    fun `zoom dialog pinches pans resets and rotates the periodic table`() {
        assertTrue("detectTransformGestures" in zoomDialog)
        assertTrue("coerceIn(1f, 6f)" in zoomDialog)
        assertTrue("detectTapGestures(onDoubleTap" in zoomDialog)
        assertTrue("\"نمایش افقی\"" in zoomDialog)
        assertTrue("rotationZ = 90f" in zoomDialog)
        assertTrue("rotatable = spec.kind == \"p\"" in mathText)
    }

    @Test
    fun `students type inside the atlas naming boxes saved as one text answer`() {
        assertTrue("blankAnswers: Map<Int, String>? = null" in atlasView)
        assertTrue("onBlankAnswer: ((Int, String) -> Unit)? = null" in atlasView)
        assertTrue("OutlinedTextField(" in atlasView)
        assertTrue("نام بخش " in atlasView)
        // زوم اطلس فقط با لمس خود تصویر تا کادرهای تایپ آزاد بمانند
        assertTrue("onImageTap: (() -> Unit)? = null" in atlasView)
        assertTrue("AtlasBlankAnswerCodec.merge(" in student)
        assertTrue("AtlasBlankAnswerCodec.freeText(" in student)
        // برچسب معلم‌داده (mkName) کادر تایپ نمی‌گیرد
        assertTrue("!(markNames && mark.label.isNotBlank())" in atlasView)
        assertFalse("android.webkit" in atlasView)
    }
}
