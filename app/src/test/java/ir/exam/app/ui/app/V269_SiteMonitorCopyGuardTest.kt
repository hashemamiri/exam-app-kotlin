package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V269 — سایت: بستر آزمون، قفل کپی/ثبت تلاش‌ها، پیش‌نمایش صوت (پین‌های سایت). */
class V269_SiteMonitorCopyGuardTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `student page reports platform and guards copy`() {
        val st = src("site/src/student.js")
        assertTrue("function platformName() { return window.SiteMobile && window.SiteMobile.active() ? 'mobile' : 'desktop'; }" in st)
        assertTrue("platform: platformName(), events: run.events" in st)
        assertTrue("function onCopyGuard(e) { if (!run || run.finished || inField(e.target)) return; e.preventDefault(); secEvent('copy_attempt');" in st)
        assertTrue("if (k === 'printscreen') { secEvent('screenshot_attempt');" in st)
        assertTrue("secEvent('print_attempt')" in st)
        assertTrue("window.addEventListener('blur', onBlur);\n    guardsOn();" in st)
        assertTrue("function stopAll() { guardsOff();" in st)
        val css = src("site/src/site.css")
        assertTrue("body.st-nocopy .st-exam{-webkit-user-select:none;user-select:none;-webkit-touch-callout:none}" in css)
        assertTrue("body.st-nocopy .st-exam input,body.st-nocopy .st-exam textarea" in css)
    }

    @Test
    fun `monitor tab shows platform and new labels`() {
        val a = src("site/src/admin.js")
        assertTrue("var PF = {app: 'اپ اندروید', desktop: 'سایت دسکتاپ', mobile: 'سایت گوشی'};" in a)
        assertTrue("text: 'بستر: ' + (PF[rep.platform] || 'نامشخص')" in a)
        assertTrue("copy_attempt: 'تلاش برای کپی متن'" in a)
    }

    @Test
    fun `builder preview has audio`() {
        assertTrue("if (q.audio) card.appendChild(el('audio', {controls: 'controls', src: q.audio, style: 'width:100%;margin:8px 0'}));" in src("site/src/builder.js"))
    }
}
