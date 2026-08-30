package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V70.2 — رفع «قالب نامعتبر است» هنگام بازکردن PDF مستقیم (فایل ۰ بایتی).
 *
 * ریشه: خروجی قبلاً مستقیم روی استریم SAF نوشته می‌شد؛ هر خطای میانی یا
 * provider که نوشتن را کامل ثبت نکند، فایل ۰ بایتی/ناقص در محل انتخاب‌شده
 * باقی می‌گذاشت و viewer خطای «قالب نامعتبر است» می‌داد. اکنون PDF کامل
 * ابتدا در حافظه (ByteArrayOutputStream) ساخته می‌شود و فقط پس از موفقیت
 * ساخت، بایت‌های کامل یک‌جا با flush صریح روی محل انتخابی نوشته می‌شوند
 * (الگوی اثبات‌شدهٔ خروجی‌های XLSX/JSON/CSV همین برنامه)؛ در صورت شکست
 * ساخت، فایل ۰ بایتیِ ایجادشده حذف می‌شود.
 */
class V70_2DirectPdfAtomicWriteTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val exporter by lazy {
        source("app/src/main/java/ir/exam/app/core/printing/DirectPdfExporter.kt")
    }

    @Test
    fun `pdf is built fully in memory before touching the target file`() {
        // ساخت کامل داخل ByteArrayOutputStream و گرفتن بایت‌ها پیش از بازکردن استریم SAF
        assertTrue("ByteArrayOutputStream" in exporter)
        assertTrue("buildPdf(withImages, buffer)" in exporter)
        assertTrue("buffer.toByteArray()" in exporter)
        // بازگشت به نوشتن مستقیم روی استریم SAF ممنوع (همان باگ فایل ۰ بایتی)
        assertFalse("stream.use { buildPdf(withImages, it) }" in exporter)
    }

    @Test
    fun `complete bytes are written once with explicit flush`() {
        assertTrue("openOutputStream(target)" in exporter)
        assertTrue("it.write(bytes)" in exporter)
        assertTrue("it.flush()" in exporter)
    }

    @Test
    fun `zero-byte placeholder is deleted when building fails`() {
        // وقتی ساخت ناموفق است، فایل ۰ بایتیِ انتخاب‌شده حذف می‌شود تا «قالب نامعتبر» ندهد
        assertTrue("contentResolver.delete(target, null, null)" in exporter)
        assertTrue(".onFailure {" in exporter)
    }

    @Test
    fun `export template and fonts stay intact`() {
        assertTrue("PageSize.A4" in exporter)
        assertTrue("BaseFont.IDENTITY_H" in exporter)
        assertTrue('fonts/bnazanin.ttf' in exporter)
        assertTrue('fonts/bnazanin_bold.ttf' in exporter)
        assertTrue("addMatching(" in exporter)
        assertTrue("includeAnswerKey" in exporter)
        assertTrue("PdfWriter.RUN_DIRECTION_RTL" in exporter)
        assertTrue("PersianTextShaper.shape" in exporter)
    }
}
