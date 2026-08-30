package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V63.8 — بازنگری V63.7 به درخواست کاربر (این فایل بازنویسی شد):
 * ۱) سربرگ رسمی «فقط» بالای صفحهٔ اول خروجی چاپ؛ در ویرایشگر هیچ سربرگی
 *    نیست. ۲) امضای دبیر/مدیر فقط پایان صفحهٔ آخر چاپ؛ در ویرایشگر نیست.
 * ۳) هدر/فوتر ویرایشگر حذف؛ هر سؤال سطر «سؤال N (بارم نمره)» بالای متن
 *    خودش دارد. ۴) هم‌مقیاسی سطر ویرایش و چاپ (ضریب عرض ۵۱۵pt چاپ).
 * ۵) بدون لکه/دستگیرهٔ آبی؛ لمس = انتخاب، کشیدن شیء انتخابی = جابجایی
 *    آزاد (تصویر خودکار حالت free می‌گیرد).
 */
class V63_7EditorPrintParityTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }
    private val pdfAdapter by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }

    @Test
    fun `print header only on page one and signatures only on the last page`() {
        assertTrue("if (pageNumber == 1) drawHeader(canvas, pageNumber, totalPages)" in pdfAdapter)
        // V68.8.1 — موتور چاپ پیوسته شد؛ سطر شروع محتوا dstTop نام گرفت (قبلاً var y)
        assertTrue("val dstTop = if (pageNumber == 1) CONTENT_TOP else LATER_CONTENT_TOP" in pdfAdapter)
        assertTrue("if (pageNumber == totalPages) canvas.drawText(printable.footerNote" in pdfAdapter)
        // ظرفیت صفحات بدون سربرگ بیشتر است
        assertTrue("CONTENT_BOTTOM - LATER_CONTENT_TOP" in pdfAdapter)
    }

    @Test
    fun `editor pages are bare paper with per-question number and score lines`() {
        assertTrue("fun WordPaperChrome()" in editor)
        assertFalse("HeaderPreview(header)" in editor)
        assertFalse("نام و امضای دبیر" in editor)
        assertFalse("headerFor" in editor)
        // سطر شماره/بارم بالای متن هر سؤال (از V63.4 درجا هم ویرایش می‌شود)
        assertTrue("\"سؤال \$row     (\"" in editor)
    }

    @Test
    fun `editor line width matches the printed line width`() {
        // V69.0 — چاپ: عرض محتوا 595-2x40=515pt؛ ویرایشگر همان نسبت را با /515f اعمال می‌کند
        assertTrue("/ 515f" in editor)
        assertTrue("val fontSize = (question.fontSizeSp.coerceIn(8f, 30f) * printScale).sp" in editor)
        assertFalse("* zoom * 0.75f" in editor)
    }

    @Test
    fun `objects select on tap and drag freely without blue handles`() {
        assertFalse("fun ResizeHandle(" in editor)
        assertTrue("detectTapGestures(onTap = { onSelect() })" in editor)
        // V63.9 — درگ فقط وقتی قفل باز است؛ آفست زنده مستقل از حالت free.
        assertTrue("if (selected && !locked) Modifier.pointerInput(media.id, zoom)" in editor)
        assertTrue("onFreeMove()" in editor)
        assertTrue("builder.setImagePosition(questionId, \"free\")" in editor)
    }
}
