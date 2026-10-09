package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V251 — داشبورد و کارنامهٔ سایت هنگام خطای سرور/اینترنت دیگر «۰ آزمون / ۰ تومان / هنوز نمره‌ای ثبت نشده» نشان نمی‌دهند. */
class V251_SiteDashboardErrorsTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, path).isFile) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `teacher dashboard reports failures instead of fake zeros`() {
        val a = source("site/src/app.js")
        assertTrue("function soft(pr, label) { return pr.catch(function (e) { failed.push({label: label, e: e}); return null; }); }" in a)
        assertTrue("if (failed.length === 4) throw failed[0].e;" in a)
        assertTrue("if (failed.length) c.appendChild(dashRetryAlert(failed, c));" in a)
        assertTrue("function cnt(x) { return Array.isArray(x) ? fa(x.length) : '—'; }" in a)
        assertTrue("statCard(r[3] ? money(r[3].balance) : '—', 'موجودی کیف پول', {panel: 'wallet'})" in a)
        assertTrue("function dashRetryAlert(failed, c) {" in a && "text: 'تلاش دوباره', onclick: function () { pageDashboard(c); }" in a)
        assertFalse("api.exams().catch(function () { return []; }), api.classes().catch(function () { return []; })" in a)
    }

    @Test
    fun `student dashboard and grades surface errors`() {
        val a = source("site/src/app.js")
        assertTrue("var g = (await api.myGrades()) || [];" in a)
        assertTrue("var r = await Promise.all([api.myGrades(), api.myAnswers().catch(function () { return []; })]);" in a)
        assertFalse("api.myGrades().catch(function () { return []; })" in a)
    }
}
