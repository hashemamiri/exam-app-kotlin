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
    fun `builder-30 asset keeps all render engines and all seven insert tools`() {
        // V76.1 — هر ۷ دکمهٔ درج برگشته‌اند
        for (needle in listOf(
            "title=\"درج شکل\"",
            "title=\"درج نمودار\"",
            "title=\"درج جدول\"",
            "title=\"درج آناتومی بدن\"",
            "title=\"درج جدول تناوبی\"",
            "title=\"درج فیزیک\"",
            "title=\"درج شیمی\""
        )) {
            assertTrue(needle, needle in assetText)
        }
        // دکمهٔ فرمول و موتورهای رندر سرِ جای خودند
        assertTrue("q-tool-btn is-fx formula-btn" in assetText)
        assertTrue("renderVisualTool" in assetText)
        assertTrue("renderRichText" in assetText)
        // ویرایشگر شکل دست‌نخورده است (نسخهٔ P1 آن را خنثی کرده بود)
        assertFalse("V30-P1: ویرایش شکل" in assetText)
        assertTrue("__r11LastFigOpen" in assetText)
        // فرمول: ویرایشگر اصلی نسخهٔ ۳۰ (MATH_EDITOR_HTML) مسیرِ اصلی است؛
        // گاردِ اشتباهِ V76.1 که همین مسیر را می‌دزدید حذف شده و پشتیبانِ ساده
        // فقط در timeoutِ شکستِ بوت به‌عنوان آخرین‌چاره صدا زده می‌شود.
        assertTrue("doc.write(MATH_EDITOR_HTML)" in assetText)
        assertFalse("if (!EXACT_MATH_EDITOR_B64)" in assetText)
        assertTrue("window.__openFallbackMathModal(window.__qmfActiveField || null, null)" in assetText)
        assertEquals(3, Regex("__openFallbackMathModal").findAll(assetText).count())
        assertTrue("id=\"mathModal\"" in assetText)
        // V76.2 — پنجره‌های موبایل: تمام‌صفحه و لمس‌پذیر + پیش‌نمایشِ هم‌عرض صفحه
        assertTrue("qmfMobileWindows" in assetText)
        assertTrue("qtype-grid{grid-template-columns:repeat(2,minmax(0,1fr))" in assetText)
        assertTrue(".pwo-body #printContent{zoom:.44}" in assetText)
        // چاپ از پل بومی می‌گذرد
        assertEquals(4, Regex(Regex.escape("window.ExamPrintNative.print")).findAll(assetText).count())
        // V76.3 — modeِ چاپ در لحظهٔ فراخوانی قفل می‌شود (رفع رقابتِ چاپ پشت‌سرهم)
        assertTrue("const __pm='teacher'" in assetText)
        assertTrue("const __pm='student'" in assetText)
        assertTrue("window.ExamPrintNative.print(__pm)" in assetText)
        // V76.3 — نوار HTML مخفی (هفت کنترل به نوار بومی اپ منتقل شد)
        assertTrue("qmfNativeBar" in assetText)
        assertTrue(".toolbar{display:none!important}" in assetText)
        // ذخیرهٔ بومی + ماندگاری فیلدهای سربرگ در ورود آزمون
        assertTrue("window.__qmfSaveNow" in assetText)
        // V78.2 — سومین مصرف‌کننده اضافه شد: __qmfDraftSnapshot (آینهٔ پیش‌نویس).
        // به‌جای شمارشِ شکننده، هر مصرف‌کننده جداگانه پین می‌شود.
        assertTrue("persistNow" in assetText)
        assertTrue("window.__qmfExportJson" in assetText)
        assertTrue("window.__qmfDraftSnapshot" in assetText)
        // پل میزبان + پاک‌سازی Cloudflare
        assertTrue("window.setExamData" in assetText)
        assertTrue("qmf_exam_autosave_azmoon_v1" in assetText)
        assertFalse("cloudflareinsights" in assetText)
        assertFalse("challenge-platform" in assetText)
    }

    @Test
    fun `builder-30 dialog has a native command bar for the seven controls`() {
        val dialog = source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt")
        // V76.3 — هفت فرمان بومی (نوار HTML فایل مخفی شده است)
        for (label in listOf("⚙ تنظیمات سربرگ", "💾 ذخیره", "📂 بازکردن", "🖨 چاپ دانشجو", "✅ چاپ استاد", "➕ سوال جدید", "👁 پیش‌نمایش")) {
            assertTrue(label, "NativeBarButton(\"$label\")" in dialog)
        }
        // V76.4 — تنظیمات سربرگ/ذخیره/سوال جدید پنجرهٔ بومی دارند؛ چاپ‌ها/چشم همان توابع فایل
        assertTrue("printStudent()" in dialog)
        assertTrue("printTeacher()" in dialog)
        assertTrue("togglePreviewWindow()" in dialog)
        assertTrue("window.__qmfSaveNow" in dialog)
        // بازکردن آزمون: انتخاب‌گر بومی + ورود با پل setExamData
        assertTrue("openExamPicker.launch" in dialog)
        assertTrue("window.setExamData(atob('" in dialog)
        assertTrue("webViewRef?.evaluateJavascript" in dialog)
        // V76.2 — متاوویوپورت فایل اعمال شود، اما overview mode خاموش بماند
        // (وگرنه WebView برای محتوای عریض A4 کل صفحه را zoom-out می‌کند)
        assertTrue("settings.useWideViewPort = true" in dialog)
        assertTrue("settings.loadWithOverviewMode = false" in dialog)
        // دکمهٔ دوربین 📷: input[type=file] فقط با onShowFileChooser در WebView کار می‌کند
        assertTrue("onShowFileChooser" in dialog)
        assertTrue("ActivityResultContracts.GetContent" in dialog)
        assertTrue("imagePicker.launch(\"image/*\")" in dialog)
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
