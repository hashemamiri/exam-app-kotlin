package ir.exam.app.ui.printing

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V285 — خروجی Word (.docx) از پیش‌نمایش چاپ؛ یک پیاده‌سازی مشترک در موتور چاپ (اپ + سایت). */
class V285_WordExportTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `engine module builds docx with OMML math, images and tables`() {
        val js = src("app/src/main/assets/print/web/word_export.js")
        assertTrue("window.ExamWord = {build: build, buildBase64: buildBase64" in js)
        assertTrue("'<m:f><m:num>'" in js && "<m:rad>" in js && "<m:nary>" in js && "<m:sSubSup>" in js && "<m:m>" in js)
        assertTrue("function zip(files)" in js && "function crc32(u8)" in js) // بدون کتابخانه
        assertTrue("word/media/" in js && "<w:drawing>" in js) // تصویر و SVG → PNG داخل سند
        assertTrue("answer-line-row" in js && "w:val=\"dotted\"" in js) // خط‌های پاسخ
        assertTrue("<w:bidiVisual/>" in js && "<w:bidi/>" in js) // راست‌به‌چپ
        assertTrue("function pageSetup()" in js && "opt_paper" in js) // کاغذ/حاشیه از تنظیمات صفحه
        // ترتیب بارگذاری: پیش از webhost.js (build_site.py همین ترتیب را برای سایت می‌خواند)
        val html = src("app/src/main/assets/print/exam_print_renderer.html")
        assertTrue(html.indexOf("web/word_export.js") in 1 until html.indexOf("web/webhost.js"))
    }

    @Test
    fun `preview print menu offers Word for student and teacher through the payment bridge`() {
        val pgs = src("app/src/main/assets/print/web/pgs_engine.js")
        assertTrue("pgsWordNow(\\'student\\')\">📄 فایل Word نسخهٔ دانشجو" in pgs)
        assertTrue("pgsWordNow(\\'teacher\\')\">📄 فایل Word نسخهٔ استاد / کلید" in pgs)
        val host = src("app/src/main/assets/print/web/webhost.js")
        assertTrue("callBridge('print', 'word-' + mode)" in host) // همان دروازهٔ پرداخت چاپ
        assertTrue("window.wordExportBase64 = function (mode)" in host)
        assertTrue("callBridge('wordReady', b64, mode)" in host)
        assertTrue("window.pgsWordNow = function (mode)" in host)
    }

    @Test
    fun `app saves docx through SAF after the usual print charge`() {
        val kt = src("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt")
        assertTrue("internal const val WORD_MIME = \"application/vnd.openxmlformats-officedocument.wordprocessingml.document\"" in kt)
        assertTrue("ActivityResultContracts.CreateDocument(WORD_MIME)" in kt)
        assertTrue("fun wordReady(base64: String?, mode: String?)" in kt)
        assertTrue("\"word-teacher\" -> \"word-teacher\"" in kt && "\"word-student\" -> \"word-student\"" in kt)
        assertTrue("val isWord = rawMode.startsWith(\"word-\")" in kt)
        assertTrue("window.wordExportToBridge('\" + mode + \"')" in kt)
        assertTrue("DocumentIo.writeBytes(context, uri, bytes)" in kt)
    }

    @Test
    fun `site downloads docx from the engine iframe`() {
        val app = src("site/src/app.js")
        assertTrue("var isWord = /^word-/.test(String(mode || ''))" in app)
        assertTrue("w.wordExportBase64(mode)" in app)
        assertTrue("var fire = isWord ? doWord : doNative;" in app)
        assertTrue("'application/vnd.openxmlformats-officedocument.wordprocessingml.document'" in app)
    }
}
