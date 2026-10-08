package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V168 — ریل دسکتاپ معلم کوتاه شد (داشبورد، آزمون‌ها، آزمون جدید، کیف پول، کارت‌ها، پروفایل)؛ صفحهٔ «منو» = منوی همبرگری اپ (تقویم، چاپ آزمون، دانش‌آموزان، کلاس‌ها، حساب، تنظیمات، خروج). */
class V168_SiteDesktopMenuTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `teacher rail and hamburger menu mirror the app`() {
        val a = source("site/src/app.js")
        assertTrue("['dashboard', '🏠', 'داشبورد'], ['exams', '📝', 'آزمون‌ها'], ['builder', '➕', 'آزمون جدید'], ['wallet', '👛', 'کیف پول'], ['cards', '🃏', 'کارت‌ها']\n    ]," in a)
        assertTrue("teacherMenu: [['calendar', '📅', 'تقویم', 'رویدادها و پیام‌ها'], ['print', '🖨', 'چاپ آزمون', 'آزمون‌های چاپی و برگه'], ['students', '🎓', 'دانش‌آموزان', 'فهرست و وضعیت'], ['classes', '🏫', 'کلاس‌ها', 'فهرست و مدیریت'], ['account', '👤', 'حساب', 'مشخصات و امنیت حساب'], ['settings', '⚙', 'تنظیمات', 'ظاهر، داده و درباره']]" in a)
        assertTrue("var menu = MENUS[user.role + 'Menu'] || MENUS.teacherMenu;" in a) // V171
        assertTrue("concat(items, MENUS.teacherMenu).filter(" in a)
        assertTrue("function pagePrint(c)" in a && "print: pagePrint" in a && "window.SiteBuilder.printExamsSection(function () { pagePrint(c); }, {noNewButton: true})" in a && "view.arg = {mode: 'print', fresh: true}; render();" in a)
    }
}

// V223 — در همین فایل (الگوی V1*_Site*Test در paths-ignore اندروید)
/** V223 — دسکتاپ: آزمون‌های چاپی از «آزمون‌ها» حذف؛ دکمهٔ «آزمون‌های چاپی» کنار «آزمون جدید»؛ در «چاپ آزمون» دکمهٔ «آزمون‌های آنلاین» و حذف دکمهٔ تکراری داخل کارت. */
class V223_SiteDesktopPrintButtonsTest {
    private fun src(rel: String): String = File(listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }, rel).readText()

    @Test
    fun examsPageLinksToPrintCenterInsteadOfEmbedding() {
        val a = src("site/src/app.js")
        assertFalse("printExamsSection(function () { pageExams(c); })" in a)
        assertTrue("text: '🖨 آزمون‌های چاپی', onclick: function () { var M = window.SiteMobile; if (M && M.printExamsSheet) M.printExamsSheet({online: true});" in a)
        val b = src("site/src/builder.js")
        assertTrue("else if (arg.fromPrintId) { var prec = await printExamGet(arg.fromPrintId); state = blankState('online');" in b)
        val m = src("site/src/mobile.js")
        assertTrue("if (opts.online) go('builder', {fromPrintId: r.id, fresh: true}); else go('builder', {mode: 'print', printId: r.id});" in m)
    }

    @Test
    fun printPageHasOnlineExamsAndNoDuplicateNewButton() {
        val a = src("site/src/app.js")
        assertTrue("text: '📝 آزمون‌های آنلاین'" in a && "M.printOnlineSheet(list, status);" in a)
        assertTrue("printExamsSection(function () { pagePrint(c); }, {noNewButton: true})" in a)
        val b = src("site/src/builder.js")
        assertTrue("opts.noNewButton ? null : el('button', {class: 'btn soft sm', text: '➕ آزمون چاپی جدید'" in b)
        val m = src("site/src/mobile.js")
        assertTrue("async function printOnlineSheet(list, status) {" in m && "printOnlineSheet: printOnlineSheet" in m)
    }

    @Test
    fun desktopSheetsAreCenteredTwoColumnAndIconsAreVector() {
        val css = src("site/src/site.css")
        assertTrue(".dk .m-sheet-bg.dkc{align-items:center;" in css && ".dk .m-sheet-dk{border-radius:24px;max-width:820px;" in css)
        assertTrue(".dk .m-sheet-dk .m-row{width:calc(50% - 6px);" in css)
        val m = src("site/src/mobile.js")
        assertTrue("var dkc = document.body.classList.contains('dk') ? ' dkc' : '';" in m)
        val b = src("site/src/builder.js")
        assertTrue("title: 'ویرایش', html: EDIT_ICON," in b && "title: 'حذف', html: TRASH_ICON," in b)
    }

    @Test
    fun v224StudentsDesktopLikeApp() {
        val sc = src("site/src/school.js")
        assertTrue("function newStudentsDialog(classes, defaultClass, done, afterCreate) {" in sc)
        assertTrue("if (!isEdit) return newStudentsDialog(classes, defaultClass, done, afterCreate);" in sc)
        assertTrue("function suggestUsername(first, last, suffix) {" in sc)
        assertTrue("var acts = el('div', {class: 'acts acts-text-btns'});" in sc)
        assertTrue("el('button', {class: 'btn light sm', text: 'افزودن به کلاس'," in sc)
        assertFalse("text: '👥 افزودن گروهی'" in sc)
        assertFalse("text: '👥 گروهی'" in sc)
        assertTrue("M.studentFilterDialog(stFilter, classes, schools, meta, function (f) {" in sc)
        assertTrue("M.studentFilterDialog(aeFilter, aeClasses, [], aeMeta, function (f) {" in sc)
        val ad = src("site/src/admin.js")
        assertFalse("text: '👥 افزودن گروهی'" in ad)
        val m = src("site/src/mobile.js")
        assertTrue("studentFilterDialog: studentFilterDialog, applyStudentFilter: applyFilter, filterActive: filterActive" in m)
    }

    @Test
    fun v224_1EditStudentDialogLikeApp() {
        val js = File("site/src/school.js").readText()
        val css = File("site/src/site.css").readText()
        assertTrue(js.contains("function editStudentDialog(s, done)"))
        assertTrue(js.contains("return editStudentDialog(s, done);"))
        assertTrue(js.contains("'رمز جدید اختیاری'") && js.contains("'رمز فعلی'"))
        assertTrue(js.contains("function rememberPw(creds)"))
        assertTrue(js.contains("class: 'st-sq red'") && js.contains("class: 'st-sq ok'"))
        assertTrue(css.contains(".st-sq.red{background:#E5484D}") && css.contains(".st-sq.ok{background:#25A86B}"))
    }

    @Test
    fun v224_2PasswordVaultAndCardNumbers() {
        val js = File("site/src/school.js").readText()
        val css = File("site/src/site.css").readText()
        assertTrue(js.contains("var PW_VAULT_KEY = 'st-pw-vault';"))
        assertTrue(js.contains("function knownPasswordOf(s)") && js.contains("var current = knownPasswordOf(s);"))
        assertTrue(js.contains("made.push({id: res.id,"))
        assertTrue(js.contains("class: 'chip st-num' + (i === active ? ' cur' : '')"))
        assertTrue(js.contains("strip.addEventListener('wheel'") && js.contains("scrollIntoView({block: 'nearest', inline: 'center'"))
        assertTrue(css.contains(".chip.st-num.cur{background:#F59E0B"))
    }

    @Test
    fun v225RailQuickAddAndCredentialDialog() {
        val app = File("site/src/app.js").readText()
        val mobile = File("site/src/mobile.js").readText()
        val school = File("site/src/school.js").readText()
        val css = File("site/src/site.css").readText()
        assertTrue(app.contains("class: 'dk-rail-item dk-rail-plus'") && app.contains("function dkQuickAdd(anchor)"))
        assertTrue(app.contains("window.SiteMobile.quickAddItems({withPrint: true})"))
        assertTrue(mobile.contains("function quickAddItems(opts)") && mobile.contains("quickAddItems: quickAddItems"))
        assertTrue(mobile.contains("items.push(['آزمون چاپی جدید'"))
        assertTrue(school.contains("class: 'modal cred-modal'") && school.contains("class: 'cred-card'"))
        assertTrue(css.contains(".dk-qa-item{") && css.contains("@keyframes dkQaIn") && css.contains(".cred-val{"))
    }
}
