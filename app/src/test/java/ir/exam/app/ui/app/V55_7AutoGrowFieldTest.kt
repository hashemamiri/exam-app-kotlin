package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.7 — کشیده‌شدن کادر متن سؤال با محتوا (گزارش دستگاه):
 * «با افزودن فرمول یا درج شکل کادر کشیده نمی‌شود؛ اندازهٔ کادر تعداد مشخصی سطر
 * است؛ قابل اسکرول نیست؛ خود کادر درون یک پس‌زمینه است.»
 * ریشه (اندازه‌گیری Chromium):
 * - WebView ارتفاع ثابت 320dp داشت؛ صفحهٔ مرجع فقط ~۱۲۱px بود → بقیهٔ WebView
 *   پس‌زمینهٔ خاکستری صفحهٔ مرجع دیده می‌شد (همان «کادر درون پس‌زمینه»).
 * - سطح تایپ مرجع (qmf-surface) max-height:min(56vh,460px) دارد؛ در WebView
 *   کوتاه یعنی ~179px و بعد اسکرول داخلی — نه کشیده‌شدن.
 * رفع سه‌قسمتی:
 * ۱) HTML (فقط حالت nativeTools): سقف ارتفاع/اسکرول داخلی سطح تایپ حذف؛
 *    پس‌زمینهٔ صفحه شفاف؛
 * ۲) HTML: reportHeight با ResizeObserver + input + interval ارتفاع واقعی
 *    محتوا را به ExamEditorNative.onContentHeight می‌فرستد؛
 * ۳) Kotlin: FieldBridge.onContentHeight + ارتفاع پویا در QuestionTextWebSection
 *    (کف 150dp، سقف 4000dp) به‌جای 320dp ثابت — اسکرول با صفحهٔ اصلی برنامه.
 * تأیید Chromium: درج ۲ جدول → گزارش ۱۳۰→۱۵۶؛ متن طولانی → ۵۲۶؛ بدون اسکرول
 * داخلی پس از همگام‌سازی ارتفاع.
 */
class V55_7AutoGrowFieldTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val editorAsset by lazy {
        File(root(), "app/src/main/assets/question_editor/question_editor.html").readText()
    }
    private val webField by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt").readText()
    }
    private val webSection by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt").readText()
    }

    @Test
    fun `native mode removes inner caps and page background`() {
        // V55.8 — انتخاب صریح کاربر: کادر ارتفاع ثابت (سقف 260px) و اسکرول
        // «داخل خود کادر»؛ سقف نامحدود V55.7 جایگزین شد.
        assertTrue("max-height:260px !important" in editorAsset)
        assertTrue("overflow-y:auto !important" in editorAsset)
        assertTrue("html,body{background:transparent !important;}" in editorAsset)
        // فقط در حالت nativeTools؛ CSS مرجع خود فایل دست‌نخورده است.
        assertTrue("max-height:min(56vh,460px)" in editorAsset)
    }

    @Test
    fun `content height is reported to native continuously`() {
        assertTrue("reportHeight" in editorAsset)
        assertTrue("ExamEditorNative.onContentHeight" in editorAsset)
        assertTrue("ResizeObserver" in editorAsset)
    }

    @Test
    fun `compose grows the webview with content instead of fixed height`() {
        assertTrue("fun onContentHeight(height: Int)" in webField)
        assertTrue("heightIn(min = 120.dp)" in webSection)
        assertTrue("else 320.dp" !in webSection)
    }
}
