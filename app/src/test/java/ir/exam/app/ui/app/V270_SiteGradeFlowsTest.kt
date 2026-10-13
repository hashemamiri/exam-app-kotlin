package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V270 — سایت: نام پایه یکدست، اشتراک کلاس هنگام ساخت، جدول آزمون‌ها در آمار، تأیید نمره‌های خودکار. */
class V270_SiteGradeFlowsTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `site grade label share checkbox and exam table`() {
        val app = src("site/src/app.js")
        assertTrue("var GRADE_WORDS = ['', 'اول', 'دوم', 'سوم', 'چهارم', 'پنجم', 'ششم', 'هفتم', 'هشتم', 'نهم', 'دهم', 'یازدهم', 'دوازدهم'];" in app)
        assertTrue("var shareRow = k ? null : el('label', {class: 'row'" in app)
        assertTrue("rpcObj('native_teacher_share_class_v137', {p_class: r.id, p_share: true})" in app)
        val ex = src("site/src/extras.js")
        assertTrue("el('table', {class: 'tbl tbl-cyan rp-exams'}" in ex)
        assertTrue("onclick: function () { S.go('grading', {examId: e.id}); }" in ex)
        val adm = src("site/src/admin.js")
        assertTrue("text: '⚡ تأیید نمره‌های خودکار (سؤال‌های عینی)'" in adm)
        assertTrue("S.gradeLabel(k.grade)" in adm && "S.gradeLabel(s.grade)" in src("site/src/school.js"))
    }
}
