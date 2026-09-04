package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V76.0 — نسخهٔ 30 به‌عنوان میزبان چاپ/ویرایش آزمون:
 * ۱) payload ریست «آزمون جدید».
 * ۲) توکن تصویر %%FIG k:img برای انتقال تصاویر خصوصی با data-URL.
 * ۳) قرارداد صفحهٔ چاپ: فقط مداد + پرینتر، «آزمون جدید» جای «سربرگ».
 * ۴) asset نسخهٔ 30: خنثی‌شدن ویرایشگر شکل با حفظ موتور رندر، چاپ بومی،
 *    پاک‌سازی Cloudflare، پل ورود آزمون و بنر پیش‌نویس.
 * ۵) بارگذار چاپ رسمی هم تصاویر خصوصی را با توکن می‌خواند.
 */
class V76_0Builder30HostTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val printCenter by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }
    private val payloadSource by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt") }
    private val inlinerSource by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlImageInliner.kt") }
    private val controllerSource by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPrintController.kt") }
    private val assetText by lazy { File(root(), "app/src/main/assets/print/exam_print.html").readText() }

    @Test
    fun `null printable produces the reset payload for a fresh new exam`() {
        val json = ir.exam.app.ui.printing.ExamHtmlPrintPayloadBuilder.build(null).toString()
        assertTrue("\"reset\":true" in json.replace(" ", ""))
        assertFalse("questions" in json)
        assertFalse("f_course" in json)
    }

    @Test
    fun `image token matches the builder-30 inline img spec`() {
        val token = ir.exam.app.ui.printing.ExamHtmlImageInliner
            .imageToken("data:image/jpeg;base64,QUJD")
        // قالب توکن: %%FIG:{"k":"img","src":"...","w":420}%%
        assertTrue(token.startsWith(" %%FIG:{"))
        assertTrue(token.endsWith("}%%"))
        assertTrue("\"k\"" in token && "\"img\"" in token)
        assertTrue("\"w\":420" in token.replace(" ", ""))
        assertTrue("data:image/jpeg;base64,QUJD" in token)
        // بازخوانی با همان regex موتور نسخهٔ 30 (tokenRe روی %%FIG:...%%)
        val re = Regex("%%FIG:([\\s\\S]*?)%%")
        val extracted = re.find(token)?.groupValues?.get(1)
        assertTrue(extracted != null && "\"k\":\"img\"" in extracted.replace(" ", ""))
    }

    @Test
    fun `print center keeps only pencil and printer icons with the new exam button`() {
        // فقط دو آیکن؛ هیچ دکمهٔ متنی چاپ یا سربرگ بومی باقی نماند
        assertFalse("Text(\"چاپ برگه\")" in printCenter)
        assertFalse("Text(\"چاپ با کلید\")" in printCenter)
        assertFalse("بستن سربرگ" in printCenter)
        assertFalse("fun PrintHeaderDialog(" in printCenter)
        assertFalse("fun HeaderPreview(" in printCenter)
        assertFalse("viewModel.preparePrint" in printCenter)
        assertFalse("OfficialPrintController" in printCenter)
        assertTrue("Text(\"آزمون جدید\")" in printCenter)
        assertTrue("contentDescription = \"ویرایش آزمون\"" in printCenter)
        assertTrue("contentDescription = \"چاپ آزمون\"" in printCenter)
        // مسیر چاپ همان مسیر واحد نسخهٔ 30 است و چیدمان محلی چاپ را هم می‌خواند
        assertTrue("portability.printableExam(examId, false, header, layoutStore.read(examId))" in printCenter)
        assertTrue("ExamHtmlImageInliner.inline(" in printCenter)
        assertTrue("onEditExamDocument: (String) -> Unit" in printCenter)
    }

    @Test
    fun `builder-30 asset keeps render engines while insertion icons are gone`() {
        // آیکن‌های درج حذف شده‌اند
        for (needle in listOf(
            "title=\"درج شکل\"",
            "title=\"درج نمودار\"",
            "title=\"درج جدول\"",
            "title=\"درج آناتومی بدن\"",
            "title=\"درج جدول تناوبی\"",
            "title=\"درج فیزیک\"",
            "title=\"درج شیمی\""
        )) {
            assertFalse(needle, needle in assetText)
        }
        // دکمهٔ فرمول و موتورهای رندر باقی‌اند
        assertTrue("q-tool-btn is-fx formula-btn" in assetText)
        assertTrue("renderVisualTool" in assetText)
        assertTrue("renderRichText" in assetText)
        // ویرایشگر شکل خنثی شده (early return) بدون شکستن مسیر رندر
        val figEditor = assetText.substringAfter("function openQmfFigEditor(fig) {")
        val figHead = figEditor.substringBefore("if (!fig)")
        assertTrue("/* V30-P1" in figHead)
        assertTrue("return false;" in figHead)
        // چاپ از پل بومی می‌گذرد
        assertEquals(4, Regex(Regex.escape("window.ExamPrintNative.print")).findAll(assetText).count())
        assertTrue("printMode==='teacher'?'teacher':'student'" in assetText)
        // پل میزبان + پاک‌سازی Cloudflare
        assertTrue("window.setExamData" in assetText)
        assertTrue("qmf_exam_autosave_azmoon_v1" in assetText)
        assertFalse("cloudflareinsights" in assetText)
        assertFalse("challenge-platform" in assetText)
    }

    @Test
    fun `official print controller loads private images through the authorized loader`() {
        // V76.0 — بعد از خصوصی‌شدن باکت، بارگذار سادهٔ Coil تصویر خصوصی نمی‌بیند
        assertTrue("PrivateImageLoader.create(appContext)" in controllerSource)
        assertFalse("ImageLoader(appContext)" in controllerSource)
        // اینلاینر تصاویر هم همان بارگذار احرازهویت‌شده را استفاده می‌کند
        assertTrue("PrivateImageLoader.create(context)" in inlinerSource)
        assertTrue("const val MAX_IMAGES = 24" in inlinerSource)
    }
}
