package ir.exam.app.ui.math

import android.webkit.WebView
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
