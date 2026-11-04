package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V337.1 — داشبورد مدیر از حالت شلخته به بخش‌های جدا و هم‌چیدمان اپ/سایت تبدیل شد:
 * نمای کلی، کارنامه، آمار فارسی، حالت خطا/تلاش دوباره و مقایسهٔ کلاس‌ها با نام مدرسه.
 */
class V337_1_ManagerDashboardLayoutTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/admin.js").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `app dashboard is split into overview and report sections`() {
        val manager = src("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertTrue("onQuickSchools: (() -> Unit)? = null" in manager)
        assertTrue("ManagerDashboardOverview(" in manager)
        assertTrue("ManagerReportSection(" in manager)
        assertTrue("ManagerSchoolContextCard(summary)" in manager)
        assertTrue("ManagerMetricCard(" in manager)
        assertTrue("ManagerQuickActionCard(" in manager)
        assertTrue("ManagerClassComparison(expandedByDefault = reportMode)" in manager)
        assertTrue("Text(\"پنل سریع\", style = MaterialTheme.typography.titleMedium)" in manager)
        assertTrue("Text(\"پنل کارنامه\", style = MaterialTheme.typography.titleMedium)" in manager)
        assertTrue("onOpenReport?.let { QuickPanelCard(\"مشاهده کارنامه مدرسه\", it, Modifier.fillMaxWidth()) }" in manager)

        val app = src("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("onQuickSchools = { openManagerSchools() }," in app)
    }

    @Test
    fun `app dashboard formats numbers and handles refresh errors`() {
        val manager = src("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertTrue("private fun faManagerNumber(value: Any?): String = PersianDigits.convert(value)" in manager)
        assertTrue("private fun faManagerToman(value: Long): String =" in manager)
        assertTrue("private fun faManagerPercent(value: Double?): String {" in manager)
        assertTrue("private fun rememberManagerSummary(refreshKey: Int = 0): ManagerSummaryState {" in manager)
        assertTrue("val refreshing: Boolean = false," in manager)
        assertTrue("val notice: String? = null" in manager)
        assertTrue("به‌روزرسانی ناموفق بود؛ آخرین اطلاعات دریافت‌شده نمایش داده می‌شود." in manager)
        assertTrue("تلاش دوباره" in manager)
        assertTrue("it.str(\"school\")" in manager)
        assertTrue("StatChip(\"پراکندگی\", \"±${faManagerPercent(row.spreadPercent)}\", Modifier.weight(1f))" in manager)
    }

    @Test
    fun `site manager dashboard has overview and report tabs`() {
        val admin = src("site/src/admin.js")
        assertTrue("async function managerDashboardPage(c, arg) {" in admin)
        assertTrue("managerDashboardPage: managerDashboardPage" in admin)
        assertTrue("S.go('dashboard', {report: true})" in admin)
        assertTrue("class: 'mgr-dash'" in admin)
        assertTrue("text: 'نمای کلی'" in admin)
        assertTrue("text: 'کارنامه مدرسه'" in admin)
        assertTrue("text: 'دسترسی سریع'" in admin)
        assertTrue("cmpCard.appendChild(classCompare(stats));" in admin)
        assertTrue("['کلاس', 'مدرسه', 'معلم', 'دانش‌آموز', 'آزمون', 'پاسخ', 'میانگین درصد', 'پراکندگی', 'زیر ۵۰٪']" in admin)

        val appJs = src("site/src/app.js")
        assertTrue("window.SiteAdmin.managerDashboardPage(c, view.arg)" in appJs)

        val css = src("site/src/site.css")
        assertTrue(".mgr-dash{display:flex;flex-direction:column;gap:14px}" in css)
        assertTrue(".mgr-actions{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:10px}" in css)
        assertTrue("@media (max-width:700px) and (pointer:coarse){.mgr-dash .grid4,.mgr-dash .grid3{grid-template-columns:1fr 1fr}.mgr-actions{grid-template-columns:1fr 1fr}}" in css)
    }
}
