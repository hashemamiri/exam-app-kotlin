package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V143 — سایت فاز ۶: گزارش کلاس + Excel، صدور/وارد کردن آزمون، پشتیبان/بازیابی، صوت سؤال، گوگل، بازیابی رمز، حذف حساب. */
class V143_SitePhase6Test {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `extras module mirrors app contracts`() {
        val js = source("site/src/extras.js")
        // ExamPackageCodec
        assertTrue("'EXAMPKG1'" in js && "'.azmoon'" in js && "_app: 'exam-system'" in js && "_kind: 'exam'" in js && "_v: 2" in js)
        assertTrue("root._app !== 'exam-system'" in js && "root._kind !== 'exam'" in js)
        // Portability backup
        assertTrue("native_export_backup_v3" in js && "native_restore_backup_v3" in js && "p_bundle" in js && "memberships" in js)
        assertTrue("root._app !== 'exam-native'" in js && "root._kind !== 'backup'" in js && "20 * 1024 * 1024" in js)
        // audio (V135: m4a ≤ 3MB در audio/<teacher>/<exam>)
        assertTrue("3 * 1024 * 1024" in js && "'audio/' + S.user().id + '/' + examId" in js && "MediaRecorder" in js)
        // auth extras
        assertTrue("/auth/v1/authorize?provider=google" in js && "native_set_registration_role_v1" in js)
        assertTrue("action: 'delete_account'" in js && "عملیات ناشناخته" in js)
        assertTrue("sendRecoveryOtp" in js && "verifyRecoveryOtp" in js && "changePassword" in js)
        // xlsx بدون کتابخانه
        assertTrue("[Content_Types].xml" in js && "xl/workbook.xml" in js && "rightToLeft=\"1\"" in js)
    }

    @Test
    fun `app wires phase 6`() {
        val app = source("site/src/app.js")
        assertTrue("SiteExtras.reportsPage" in app && "SiteExtras.exportExamDlg" in app && "SiteExtras.importExam" in app)
        assertTrue("SiteExtras.backupCard" in app && "SiteExtras.deleteAccountCard" in app && "SiteExtras.googleButton" in app && "SiteExtras.recoveryFlow" in app && "SiteExtras.handleOAuthReturn" in app)
        assertTrue("['reports', '📈', 'گزارش‌ها']" in app)
        assertTrue("verifyRecoveryOtp: async function" in app && "native_my_profile" in app)
        val b = source("site/src/builder.js")
        assertTrue("arg.importPkg" in b && "SiteExtras.audioDlg" in b)
        assertTrue("extras.js" in source("site/build_site.py"))
        assertTrue("window.SiteExtras" in source("site/index.html"))
    }
}
