package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V253.1–V253.7 — سایت دسکتاپ: صفحهٔ آزمون دانش‌آموز، ریل، پیام نصب، تقویم دوستونه، رندر فرمول در کارنامه/تصحیح. (نام فایل *_Site*Test تا CI اپ اجرا نشود) */
class V253_SiteCalendarReportTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()
    private fun exists(path: String) = File(root(), path).exists()

    @Test
    fun `desktop student exam head does not cover question numbers and rail labels wrap`() {
        val css = source("site/src/site.css")
        assertTrue(".dk .st-head{top:12px}" in css && ".dk .st-nav{position:relative;z-index:1" in css)
        assertTrue("white-space:normal;max-width:66px;text-align:center;overflow:visible" in css)
        assertFalse("white-space:nowrap;max-width:66px;overflow:hidden;text-overflow:ellipsis" in css)
        assertTrue(".pwa-bar{bottom:auto;top:16px;inset-inline:0;margin-inline:auto;width:max-content" in css) // V253.2
    }

    @Test
    fun `desktop calendar shows month on one half and day messages on the other`() {
        val a = source("site/src/admin.js")
        assertTrue("else c.appendChild(el('div', {class: 'cal-wrap'}, [el('div', {class: 'card'}, [grid]), side]));" in a)
        // V253.4 — دسکتاپ: ناوبری ماه بالای تقویم، «پیام جدید» وسطِ بالای پیام‌ها، بدون «پیام برای این روز»
        assertTrue("[el('div', {class: 'cal-col'}, [navRow, el('div', {class: 'card'}, [grid])]), el('div', {class: 'cal-col'}, [el('div', {class: 'row cal-newrow', style: 'justify-content:center;margin-bottom:12px'}, [newBtn].filter(Boolean)), side])]" in a)
        assertTrue("html: isDk() ? S.esc('ماه بعد') + ' ' + S.emojiSvg('←') : S.esc('‹ ماه بعد')" in a && "text: isDk() ? '→ ماه قبل' : 'ماه قبل ›'" in a) // V253.7 / V256.1
        assertFalse("side.appendChild(el('div', {class: 'row', style: 'margin-top:12px'}, [el('button', {class: 'btn sm', text: '➕ پیام برای این روز'" in a)
        assertTrue("if (!isDk()) { if (newBtn) navRow.appendChild(newBtn); c.appendChild(navRow); }" in a)
        assertTrue("if (isDk()) showDay(d); else dayDlg(iso, y, m, d, ns, hs, isTeacher" in a)
        assertTrue("showDay(arg.d || (today.jy === y && today.jm === m ? today.jd : 1));" in a)
        val css = source("site/src/site.css")
        assertTrue(".dk .cal-nav>button:nth-child(3){order:1}" in css && ".dk .cal-nav>button:nth-child(1){order:3}" in css) // V253.5
        assertTrue(".cal-side{display:none}" in css && ".dk .cal-wrap{display:grid;grid-template-columns:1fr 1fr" in css && ".dk .cal-side{display:block" in css)
    }

    @Test
    fun `grading and report card render formulas with the exam engine`() {
        val a = source("site/src/admin.js")
        assertTrue("function richEl(tag, cls, text, style)" in a && a.split("richEl('div', 'g-qtext', q.text)").size == 4)
        assertFalse("el('div', {class: 'g-qtext', text: (q.text || '')" in a)
        assertTrue("rt != null ? richEl('span', '', rt) : el('i', {class: 'muted', text: 'بدون پاسخ'})" in a)
        assertTrue("[el('span', {text: 'پاسخ درست: '}), richEl('span', '', ct)]" in a && "el('td', {}, [richEl('span', '', responseText(q, r) || '—')])" in a)
        val st = source("site/src/student.js")
        assertTrue("mathCss: mathCss," in st && "',.math-rich ' + x.trim()" in st)
        assertTrue(".math-rich.rendered{white-space:normal}" in source("site/src/site.css"))
    }
}
