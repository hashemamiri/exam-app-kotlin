package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V89.6 — فرمول در نمایشِ زنده بی‌شکل بود (CSS نداشت)، جابه‌جایی به بالا
 * ممکن نبود، و تغییرِ اندازه مقیاسِ پیش‌نمایش را لحاظ نمی‌کرد.
 */
class V89_6LivePreviewCssTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }
    private val cards by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/PrintQuestionCards.kt").readText()
    }
    private val webview by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt").readText()
    }

    @Test
    fun `the live preview carries the page's own stylesheet`() {
        // بدونِ CSS، `renderRichText` مارک‌آپِ درست می‌داد ولی کسر بی‌شکل بود
        assertTrue("window.__qmfPreviewCss = function" in asset)
        assertTrue("extraCss" in webview)
        assertTrue("<style>\${extraCss}</style>" in webview)
        // V89.8 — پیش‌نمایشِ جدا برداشته شد؛ پل و نمایشگر باقی‌اند.
        assertTrue("window.__qmfPreviewCss = function" in asset)
    }

    @Test
    fun `the stylesheet is fetched once, not per question`() {
        // V89.8 — بارگذارِ CSS با حذفِ پیش‌نمایشِ جدا برداشته شد.
        assertTrue("window.__qmfPreviewCss" in asset)
    }

    @Test
    fun `only rendering rules are taken, not page layout`() {
        assertTrue("\\.mathx|\\.mfrac|\\.msqrt|\\.qmf-fig" in asset)
    }

    @Test
    fun `the first touch makes a figure free so it can move anywhere`() {
        assertTrue("if (fig && !fig.classList.contains('fig-free'))" in asset)
        // مختصاتِ فعلی حفظ می‌شود وگرنه شیء می‌پرد
        assertTrue("x: Math.max(0, Math.round((pr2.right - fr.right) / k0))" in asset)
    }

    @Test
    fun `a free figure may rise above its natural position`() {
        // V93 — شناور فقط کفِ صفر دارد: سقفِ سلولِ اندازه‌گیری‌شده حذف شد
        // (سقفِ منفی شیء را به بالا می‌پراند و قفل می‌کرد)؛ سلول با minHeight
        // همراهِ شیء رشد می‌کند تا از سلولِ خودش بیرون نزند.
        assertTrue("Math.min(maxY, y)" !in asset)
        assertTrue("y = Math.max(0, y);" in asset)
        assertTrue("parent.style.minHeight = need + 'px';" in asset)
    }

    @Test
    fun `resizing converts pointer travel out of the scaled space`() {
        assertTrue("const dx = (e.clientX - drag.sx) / ks;" in asset)
        assertTrue("const dy = (e.clientY - drag.sy) / ks;" in asset)
    }
}
