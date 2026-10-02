package ir.exam.app.ui.app

import ir.exam.app.ui.printing.PrintPayFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V199 — پرداخت هزینهٔ چاپ آزمون چاپی: اثر انگشت سربرگ (اپ = سایت)، RPCهای سرور، کارت‌ها و دکمهٔ چاپ قرمز/سبز. */
class V199_PrintPayTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `header fingerprint is sorted, skips course and duration and blanks, defaults template`() {
        val fp = PrintPayFingerprint.headerFingerprint(mapOf("f_school" to " مدرسه ", "f_course" to "ریاضی", "f_duration" to "60", "f_branch" to "", "f_examDate" to "1405/07/03", "opt_footerText" to "x"))
        assertEquals("f_examDate=1405/07/03\nf_headerTemplate=classic\nf_school=مدرسه", fp)
        assertEquals("f_headerTemplate=modern", PrintPayFingerprint.headerFingerprint(mapOf("f_headerTemplate" to "modern")))
        assertEquals("f_headerTemplate=classic", PrintPayFingerprint.headerFingerprint(mapOf("f_headerTemplate" to "")))
    }

    @Test
    fun `site fingerprint mirrors app rule`() {
        val a = source("site/src/app.js")
        assertTrue("k.indexOf('f_') === 0 && k !== 'f_course' && k !== 'f_duration'" in a)
        assertTrue("function ensurePrintPaid(printId, headerFp, title)" in a)
        assertTrue("rpcObj('native_print_quote_v199', {p_id: id, p_header: header || ''})" in a)
        assertTrue("rpcObj('native_print_pay_v199', {p_id: id, p_operation: uuid(), p_header: header || ''})" in a)
        assertTrue("rpcObj('native_print_pay_status_v199', {p_header: header || ''})" in a)
        assertTrue("if (ctx.printExam !== undefined) {" in a && "برای چاپ، ابتدا آزمون چاپی را ذخیره کنید." in a)
    }

    @Test
    fun `site cards have wallet and red-green printer, save is free`() {
        val b = source("site/src/builder.js")
        assertTrue("class: 'icon-btn pay-btn'" in b && "class: 'icon-btn print-btn ' + (payMap[r.id] ? (payMap[r.id].paid ? 'paid' : 'unpaid') : '')" in b)
        assertTrue("printExam: state.mode === 'print' ? (state.printId || '') : undefined, printDirty: state.mode === 'print' && !!state.printDirty" in b) // V201
        assertFalse("S.confirmDlg('هزینهٔ تصاویر'" in b)
        val m = source("site/src/mobile.js")
        assertTrue("act('wallet', 'پرداخت هزینهٔ چاپ این آزمون'" in m && "printAct.classList.add(ps && ps.paid ? 'paid' : 'unpaid')" in m)
        val css = source("site/src/site.css")
        assertTrue(".icon-btn.print-btn.unpaid{color:#dc2626" in css && ".icon-btn.print-btn.paid{color:#16a34a" in css)
    }

    @Test
    fun `preview print button colour comes from host`() {
        val w = source("app/src/main/assets/print/web/webhost.js")
        assertTrue("window.setPrintPaid = function (paid)" in w)
        val css = source("app/src/main/assets/print/web/pgs_style.css")
        assertTrue("body.print-pay-known .pgs-btn.primary{background:#dc2626;}" in css && "body.print-pay-known.print-paid .pgs-btn.primary{background:#16a34a;}" in css)
        val d = source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt")
        assertTrue("printPayExamId: String? = null" in d && "window.setPrintPaid&&window.setPrintPaid(" in d)
        assertTrue("PendingPrintPay(q, fire) { restore() }" in d)
        val c = source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt")
        assertTrue("Icons.Outlined.AccountBalanceWallet" in c && "Color(0xFF16A34A)" in c && "Color(0xFFDC2626)" in c)
    }

    @Test
    fun `sql migration defines pay functions and free save`() {
        val sql = source("supabase/migrations/20260925_native_print_pay_v199.sql")
        for (f in listOf("native_print_quote_v199", "native_print_pay_v199", "native_print_pay_status_v199", "native_print_due_v199", "native_print_header_hash_v199"))
            assertTrue(f, "create or replace function public.$f(" in sql)
        assertTrue("create table if not exists public.print_exam_payments" in sql)
        assertTrue("v_cost := 0; -- V199" in sql)
        assertEquals(sql, source("sql/manual/SQL_NATIVE_PRINT_PAY_V199.sql"))
    }

    @Test
    fun `v201 header hash from server profile and layout changes do not block print`() {
        val sql = source("supabase/migrations/20260926_native_print_header_hash_v201.sql")
        assertTrue("from public.profiles p where p.id = p_exam.teacher_id" in sql && "hdr_field" in sql)
        assertEquals(sql, source("sql/manual/SQL_NATIVE_PRINT_HEADER_HASH_V201.sql"))
        val vm = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")
        assertTrue("fun hasUnsavedChanges(): Boolean = cleanPrintFingerprint != null && printFingerprint(_state.value) != cleanPrintFingerprint" in vm)
        assertTrue("it.copy(figLayoutsJson = \"\", sepExtraPx = 0, textSpans = emptyList(), alignSpans = emptyList())" in vm)
        val b = source("site/src/builder.js")
        assertTrue("function mark() { state.dirty = true; state.printDirty = true; saveDraft(); }" in b)
        assertTrue("state.dirty = true; saveDraft(); /* V201" in b && "printDirty: state.mode === 'print' && !!state.printDirty" in b)
    }

    @Test
    fun `v202 every wallet debit confirms first and shows centered cost dialog with confirm button`() {
        val dlg = source("app/src/main/java/ir/exam/app/ui/common/CostDeductedDialog.kt")
        assertTrue("dismissOnBackPress = false, dismissOnClickOutside = false" in dlg)
        assertTrue("confirmLabel: String = \"تأیید\"" in dlg)
        // اپ: تکثیر، انتقال مدیر، بازیابی، چاپ V132/V199، پرداخت کارت چاپ
        listOf(
            "app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt",
            "app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt",
            "app/src/main/java/ir/exam/app/ui/portability/DataPortabilitySection.kt",
            "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt",
            "app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt"
        ).forEach { assertTrue(it, "ir.exam.app.ui.common.CostDeductedDialog(" in source(it)) }
        val pd = source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt")
        assertTrue("costDone = ir.exam.app.ui.common.costDeductedMessage(r.costToman, r.balanceToman) to { req.fire() }" in pd)
        assertFalse("barStatus = \"کسر \"" in pd)
        // سایت: infoDlg/costDoneDlg به‌جای toast
        val a = source("site/src/app.js")
        assertTrue("function infoDlg(title, body, okLabel)" in a)
        assertTrue("await costDoneDlg(r.cost || q.due, r.balance);" in a)
        assertTrue("return costDoneDlg(r.cost || cost, r.balance).then(function () { if (printCtx === ctx) doNative(); else ctx.busy = false; });" in a)
        assertFalse("toast('کسر '" in a)
        assertTrue("await S.costDoneDlg((raw && raw.cost) || 0, raw ? raw.balance : null" in source("site/src/builder.js"))
        assertTrue("await S.costDoneDlg(r.cost || 0, r.balance, 'کپی ساخته شد" in source("site/src/mobile.js"))
        assertTrue("await S.costDoneDlg(raw.amount || a, raw.manager_balance" in source("site/src/admin.js"))
        assertTrue("text: 'تأیید', onclick: function () { bg2.remove(); S.go('dashboard'); }" in source("site/src/extras.js"))
    }
}
