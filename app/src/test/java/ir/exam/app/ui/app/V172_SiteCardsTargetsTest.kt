package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V172 — کارت‌های «کارت‌ها» همان صفحه‌های اپ را باز می‌کنند: آمار/کارنامه (ReportsScreen section)، درخواست‌ها (صفحهٔ مستقل، نه داشبورد)، عنوان مثل اپ. */
class V172_SiteCardsTargetsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `management cards open the same screens as the app`() {
        val a = source("site/src/app.js")
        assertTrue("window.SiteExtras.reportsPage(c, view.arg); }, requests: pageRequests," in a)
        assertTrue("async function pageRequests(c)" in a && "managerRequestsCard(true)" in a)
        assertTrue("title = {reports: a.section === 'grades' ? 'کارنامه' : 'آمار', grading: a.filter === 'pending' ? 'مانده' : (a.filter === 'graded' ? 'پاسخ' : 'تصحیح'), bank: 'بانک سؤال', requests: 'درخواست‌ها'}[view.panel] || ''" in a)
        val x = source("site/src/extras.js")
        assertTrue("async function reportsPage(c, arg)" in x && "text: gradesOnly ? 'کارنامه و لیست نمرات' : 'آمار و تحلیل آزمون‌ها'" in x)
        assertTrue("window.SiteAdmin.questionAnalysis(anBody, e.id)" in x)
        assertTrue("questionAnalysis: function (body, examId) { return tabAnalysis(body, {examId: examId}); }" in source("site/src/admin.js"))
        assertTrue("async function managerRequestsCard(alwaysShow)" in source("site/src/school.js"))
        val m = source("site/src/mobile.js")
        assertTrue("function () { go('requests'); }]" in m && "p === 'requests') return 'cards'" in m)
        assertTrue("go('dashboard', {requests: true})" !in m)
    }
}
