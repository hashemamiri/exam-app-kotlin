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
    fun `V286 - Word left the print menu and moved to exam cards with a separate flat fee`() {
        val pgs = src("app/src/main/assets/print/web/pgs_engine.js")
        assertTrue("pgsWordNow" !in pgs && "فایل Word نسخهٔ" !in pgs)
        val host = src("app/src/main/assets/print/web/webhost.js")
        assertTrue("pgsWordNow" !in host && "callBridge('print', 'word-'" !in host)
        assertTrue("window.wordExportBase64 = function (mode)" in host)
        assertTrue("callBridge('wordReady', b64, mode)" in host)
        // سرور: مبلغ ثابت ۵۰٬۰۰۰ تومان، idempotent، جدا از exam_print_payments
        val sql = src("supabase/migrations/20261014_native_charge_word_v286.sql")
        assertTrue("create or replace function public.native_charge_word_v286(" in sql)
        assertTrue("v_cost bigint := 50000;" in sql && "'exam:word:'" in sql && "exam_print_payments" !in sql)
        assertTrue(sql == src("sql/manual/SQL_NATIVE_CHARGE_WORD_V286.sql"))
        val repo = src("app/src/main/java/ir/exam/app/domain/repository/BillingRepository.kt")
        assertTrue("const val WORD_EXPORT_COST_TOMAN: Long = 50_000" in repo)
        assertTrue("rpc(\"native_charge_word_v286\"" in src("app/src/main/java/ir/exam/app/data/repository/SupabaseBillingRepository.kt"))
    }

    @Test
    fun `V286 - app flow builds first then charges then saves through SAF`() {
        val flow = src("app/src/main/java/ir/exam/app/ui/printing/WordExportFlow.kt")
        assertTrue("fun WordExportFlow(request: WordExportRequest?, onDone: () -> Unit)" in flow)
        assertTrue(flow.indexOf("onWordBytes = { bytes ->") < flow.indexOf(".chargeWord(request.examRef") && flow.indexOf(".chargeWord(request.examRef") < flow.indexOf("launcher.launch(fileName)"))
        assertTrue("ActivityResultContracts.CreateDocument(WORD_MIME)" in flow)
        val kt = src("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt")
        assertTrue("onWordBytes: ((ByteArray?) -> Unit)? = null" in kt)
        assertTrue("printMode = if (wordOnly) \"word\" else initialPrintMode" in kt)
        assertTrue("} else if (printMode == \"word\") {" in kt && "window.wordExportToBridge('student')" in kt)
        assertTrue("fun wordReady(base64: String?, mode: String?)" in kt)
        assertTrue("\"word-teacher\"" !in kt && "rawMode.startsWith(\"word-\")" !in kt)
        // کارت‌ها: آزمون آنلاین و آزمون چاپی
        val dash = src("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt")
        assertTrue("ExamCardAction(Icons.Outlined.Description, \"Word\")" in dash)
        assertTrue("WordExportRequest(exam.id, exam.title)" in dash && "WordExportRequest(rec.id, rec.title)" in dash)
        assertTrue("printableExam(exam.id, includeAnswerKey = false)" in dash)
        // تعرفه‌ها
        val wallet = src("app/src/main/java/ir/exam/app/ui/billing/WalletScreen.kt")
        assertTrue("\"فایل Word آزمون: هر فایل (جدا از چاپ)\" to ir.exam.app.domain.repository.WORD_EXPORT_COST_TOMAN" in wallet)
        assertTrue("startsWith(\"exam:word:\") -> \"فایل Word آزمون\"" in wallet)
    }

    @Test
    fun `V286 - site cards get a Word icon with the same build-charge-download order`() {
        val app = src("site/src/app.js")
        assertTrue("isWord" !in app && "doWord" !in app)
        assertTrue("chargeWord: function (examRef, title) { return rpcObj('native_charge_word_v286'" in app)
        assertTrue("var WORD_COST = 50000;" in app)
        assertTrue(app.indexOf("b64 = await buildWordDocx(payload)") < app.indexOf("r = await api.chargeWord(opts.examRef || 'local', title)") && app.indexOf("r = await api.chargeWord(") < app.indexOf("window.SiteExtras.download(name, new Blob([u8]"))
        assertTrue("w.wordExportBase64('student')" in app)
        assertTrue("html: WORD_ICON_HTML + '<span>Word</span>', onclick: function () { wordOnlineExam(x); }" in app)
        assertTrue("['فایل Word آزمون: هر فایل (جدا از چاپ)', '۵۰٬۰۰۰']" in src("site/src/admin.js"))
        val mob = src("site/src/mobile.js")
        assertTrue("act('word', 'Word', function () { S.wordOnlineExam(x); })" in mob)
        assertTrue("act('word', 'فایل Word (هزینهٔ جداگانه)'" in mob)
        assertTrue("title: 'فایل Word (هزینهٔ جداگانه)', html: WORD_ICON" in src("site/src/builder.js"))
    }
}
