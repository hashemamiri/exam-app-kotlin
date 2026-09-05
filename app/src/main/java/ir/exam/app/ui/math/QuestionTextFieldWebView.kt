package ir.exam.app.ui.math

import android.webkit.WebView
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/**
 * هماهنگ‌کنندهٔ درج محتوای غنی در کادر متن سؤال (پاک‌سازی V74.0).
 *
 * از V65.0 کادر متن سؤال کاملاً Native (BasicTextField + RichTextSplitter) است و
 * کامپوزبل WebView قدیمی کادر متن (که تنها asset HTML قدیمی سؤال را بارگیری
 * می‌کرد) همراه با همان asset حذف شد. این فایل فقط [QuestionEditorFieldController]
 * را نگه می‌دارد؛ همان کلاسی که آزمون‌ساز و QuestionTextWebSection برای
 * درج/جایگزینی توکن (فرمول/شکل/جدول/اطلس) از مسیر Native لامبدا
 * (nativeInsert/nativeReplace/nativeOpenFormula) استفاده می‌کنند.
 * متدهای باقی‌مانده ابتدا مسیر Native را امتحان می‌کنند و فقط در صورت نبودِ آن،
 * fallback امن WebView را به کار می‌برند؛ هیچ token یا Secretی به WebView نمی‌رسد.
 */
class QuestionEditorFieldController {
    internal var webView: WebView? = null

    /** V65.0 — مسیر Native Compose برای درج/جایگزینی/فرمول بدون WebView. */
    internal var nativeInsert: ((String) -> Boolean)? = null
    internal var nativeReplace: ((String) -> Boolean)? = null
    internal var nativeOpenFormula: (() -> Boolean)? = null
    var pendingEditOccurrence: Int? = null

    /** V67.1 — آفست مکان‌نمای درخواستی پس از بازگشت متن از پنجرهٔ فرمول. */
    var pendingCaretOffset: Int? = null

    /** آخرین متنی که خود WebView گزارش کرده یا برایش push شده؛ برای جلوگیری از echo. */
    var lastJsValue: String = ""
        internal set

    /** V55.10 — آیا محتوای کادر داخل خودش اسکرول دارد؟ (گزارش از HTML). */
    @Volatile
    var innerScrollable: Boolean = false
        internal set

    /** بازکردن ابزارهای مرجع داخل WebView: formula / anatomy / periodic / physics / chemistry. */
    fun openTool(name: String): Boolean {
        if (name == "formula") {
            nativeOpenFormula?.invoke()?.let { return it }
        }
        val view = webView ?: return false
        val quoted = JSONObject.quote(name)
        view.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.openTool($quoted);", null)
        return true
    }

    /** درج توکن `%%FIG:{json}%%` ساخته‌شده در ویرایشگرهای Native در محل مکان‌نما. */
    fun insertFigureJson(specJson: String): Boolean {
        nativeInsert?.invoke(specJson)?.let { return it }
        val view = webView ?: return false
        val quoted = JSONObject.quote(specJson)
        view.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.insertToken($quoted);", null)
        return true
    }

    /** V53.3 — جایگزینی توکن در حال ویرایش (dblclick) با خروجی ویرایشگر Native. */
    fun applyEditedFigureJson(specJson: String): Boolean {
        nativeReplace?.invoke(specJson)?.let { return it }
        val view = webView ?: return false
        val quoted = JSONObject.quote(specJson)
        view.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.applyEditedToken($quoted);", null)
        return true
    }

    /** V53.3 — انصراف از ویرایش توکن dblclick. */
    fun cancelEditFigure() {
        webView?.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.cancelEditToken();", null)
    }

    /** V54.4 — بستن لایه‌های تمام‌صفحهٔ مرجع (ابزارها) با دکمهٔ برگشت سیستم. */
    fun closeOverlays() {
        webView?.evaluateJavascript("window.ExamEditorTools && ExamEditorTools.closeOverlays();", null)
    }

    /** همگام‌سازی متن از سمت Native (مثلاً افزودن سؤال از بانک) به کادر WebView. */
    fun setValue(text: String) {
        val view = webView ?: return
        lastJsValue = text
        val quoted = JSONObject.quote(text)
        view.evaluateJavascript("window.ExamEditor && ExamEditor.setValue($quoted);", null)
    }
}

/**
 * V88.9 — نمایشِ زندهٔ متنِ سؤال زیرِ کادرِ بومی.
 *
 * کاربر خواست کادرِ متن بومی باشد ولی فرمول و شکل «زنده» دیده شوند. متن در
 * `OutlinedTextField` تایپ می‌شود و خروجیِ `renderRichText`ِ همان صفحه اینجا
 * نمایش داده می‌شود — یعنی دقیقاً همان چیزی که چاپ خواهد شد، نه یک
 * بازپیاده‌سازیِ تقریبی.
 *
 * این یک نمایشگرِ فقط-خواندنی است: نه جاوااسکریپت اجرا می‌کند، نه به شبکه
 * می‌رود. بنابراین سطحِ حمله ندارد و با قانونِ «موتورِ رندر دست‌نخورده» هم
 * نمی‌سازد چون خودش موتور نیست، فقط خروجی را نشان می‌دهد.
 */
@android.annotation.SuppressLint("SetJavaScriptEnabled")
@androidx.compose.runtime.Composable
fun PrintRichTextPreview(
    html: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val document = androidx.compose.runtime.remember(html) { wrapPrintPreviewHtml(html) }
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier.heightIn(min = 48.dp, max = 260.dp),
        factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
            }
        },
        update = { view ->
            view.loadDataWithBaseURL(
                "https://exam-print.local/print/exam_print.html",
                document,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}

/** پوششِ کمینه با جهتِ راست‌به‌چپ و قلمِ خوانا. */
internal fun wrapPrintPreviewHtml(body: String): String = """
<!doctype html><html lang="fa" dir="rtl"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  body{margin:0;padding:6px 8px;font:15px/1.9 Tahoma,Arial,sans-serif;color:#111827;background:transparent}
  img,svg{max-width:100%;height:auto}
  .qmf-fig{display:inline-block;vertical-align:middle;max-width:100%}
</style></head><body>${body}</body></html>
""".trimIndent()
