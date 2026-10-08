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
        val js = src("site/src/school.js")
        val css = src("site/src/site.css")
        assertTrue(js.contains("function editStudentDialog(s, done)"))
        assertTrue(js.contains("return editStudentDialog(s, done);"))
        assertTrue(js.contains("'رمز جدید اختیاری'") && js.contains("'رمز فعلی'"))
        assertTrue(js.contains("function rememberPw(creds)"))
        assertTrue(js.contains("class: 'st-sq red'") && js.contains("class: 'st-sq ok'"))
        assertTrue(css.contains(".st-sq.red{background:#E5484D}") && css.contains(".st-sq.ok{background:#25A86B}"))
    }

    @Test
    fun v224_2PasswordVaultAndCardNumbers() {
        val js = src("site/src/school.js")
        val css = src("site/src/site.css")
        assertTrue(js.contains("var PW_VAULT_KEY = 'st-pw-vault';"))
        assertTrue(js.contains("function knownPasswordOf(s)") && js.contains("var current = knownPasswordOf(s);"))
        assertTrue(js.contains("made.push({id: res.id,"))
        assertTrue(js.contains("class: 'chip st-num' + (i === active ? ' cur' : '')"))
        assertTrue(js.contains("strip.addEventListener('wheel'") && js.contains("scrollIntoView({block: 'nearest', inline: 'center'"))
        assertTrue(css.contains(".chip.st-num.cur{background:#F59E0B"))
    }

    @Test
    fun v225RailQuickAddAndCredentialDialog() {
        val app = src("site/src/app.js")
        val mobile = src("site/src/mobile.js")
        val school = src("site/src/school.js")
        val css = src("site/src/site.css")
        assertTrue(app.contains("class: 'dk-rail-item dk-rail-plus'") && app.contains("function dkQuickAdd(anchor)"))
        assertTrue(app.contains("window.SiteMobile.quickAddItems({withPrint: true})"))
        assertTrue(mobile.contains("function quickAddItems(opts)") && mobile.contains("quickAddItems: quickAddItems"))
        assertTrue(mobile.contains("items.push(['آزمون چاپی جدید'"))
        assertTrue(school.contains("class: 'modal cred-modal'") && school.contains("class: 'cred-card'"))
        assertTrue(css.contains(".cred-val{"))
    }

    @Test
    fun v225_1StudentsSearchInToolbarRow() {
        val js = src("site/src/school.js")
        assertTrue(js.contains("class: 'row st-toolbar'"))
        assertTrue(js.contains("[q, el('span', {class: 'grow'}), cnt, filterBtn, el('button', {class: 'btn', text: '➕ دانش‌آموز جدید'"))
    }

    @Test
    fun v226RadialQuickAddAndSkyLogin() {
        val app = src("site/src/app.js")
        val mobile = src("site/src/mobile.js")
        val css = src("site/src/site.css")
        // منوی افزودن سریع دسکتاپ = radialMenu (طرح ۱۷۱) با رنگ‌های پاستلی و کارت‌های بزرگ‌تر
        assertTrue(app.contains("cls: 'dk-qa-radial'") && app.contains("window.SiteMobile.radialMenu(function (key)"))
        assertTrue(mobile.contains("(opts.colors && opts.colors[r[0]]) || PASTEL[r[0]]"))
        assertTrue(css.contains(".dk .m-radial-bg.dk-qa-radial .m-radial-item{width:104px;height:104px"))
        assertTrue(css.contains(".dk .m-radial-bg.dk-qa-radial .m-radial-item .l{font-size:11px;font-weight:800;white-space:normal"))
        assertFalse(app.contains("dk-qa-bg"))
        // صفحهٔ ورود طرح ۲۶ «آسمانی و ابری»
        assertTrue(app.contains("class: 'lp lp-sky'") && app.contains("class: 'cloud c1'"))
        assertTrue(app.contains("var tiles = [['report', 'کارنامهٔ خودکار'], ['print', 'چاپ رسمی'], ['formula', 'ویرایشگر فرمول'], ['periodic', 'جدول تناوبی'], ['board', 'تختهٔ سفید'], ['school', 'مدیریت مدرسه']];"))
        assertTrue(css.contains(".lp.lp-sky{") && css.contains(".lp.lp-sky .card{background:rgba(255,255,255,.42)") && css.contains(".m-dark .lp.lp-sky{"))
    }

    @Test
    fun v227SchoolsButtonAndZebraRows() {
        val app = src("site/src/app.js")
        val mobile = src("site/src/mobile.js")
        val css = src("site/src/site.css")
        assertTrue(app.contains("text: '🏫 مدارس', onclick: function () { pageSchools(c); }"))
        assertTrue(app.contains("async function pageSchools(c)") && app.contains("async function pageSchoolClasses(c, school)"))
        assertTrue(app.contains("rpcObj('native_teacher_schools_v61', {})") && app.contains("rpc('native_teacher_school_classes_v61', {p_school: school.id})"))
        assertTrue(app.contains("text: 'پیوستن به مدرسه'") && app.contains("text: 'بازگشت به کلاس‌ها'") && app.contains("text: 'بازگشت به مدارس'"))
        assertTrue(mobile.contains("joinSchoolDialog: joinSchoolDialog, createSchoolDialog: createSchoolDialog"))
        assertTrue(css.contains(".dk table.tbl td{border-bottom:0}"))
        // V227.3 — ردیف‌ها یکی‌درمیان: فرد فیروزه‌ای کم‌رنگ، زوج زرشکی کم‌رنگ (همهٔ جدول‌ها)
        assertTrue(css.contains(".dk table.tbl{--zc:34,198,239;--zm:148,40,78;"))
        assertTrue(css.contains(".dk table.tbl tbody tr:nth-child(odd){--zb:var(--zc)}") && css.contains(".dk table.tbl tbody tr:nth-child(even){--zb:var(--zm)}"))
        assertTrue(css.contains(".dk table.tbl tbody tr td{background:rgba(var(--zb),.09)"))
    }

    @Test
    fun v228TemplatesLoginHintsContrastStatus() {
        val b = src("site/src/builder.js")
        assertTrue(b.contains("var TPL_KEY = 'examsite.examTemplates.v1'") && b.contains("text: 'ذخیره به‌عنوان قالب'") && b.contains("text: 'اعمال قالب'"))
        val app = src("site/src/app.js")
        assertTrue(app.contains("if (state.fails >= 3) m += ' — اگر رمز را فراموش کرده‌اید") && app.contains("if (state.fails >= 5) { b.disabled = true;"))
        assertTrue(app.contains("if (attrs.title && !attrs['aria-label']) e.setAttribute('aria-label', attrs.title)"))
        val m = src("site/src/mobile.js")
        assertTrue(m.contains("root.classList.toggle('m-hc', !!a.highContrast)") && m.contains("switchRow('حالت پرکنتراست'") && m.contains("href: '/status.html'"))
        val css = src("site/src/site.css")
        assertTrue(css.contains(".m-hc{--ink:#000;") && css.contains(".m-hc :focus-visible{outline:3px solid #ff9800!important"))
        val st = src("site/status.html")
        assertTrue(st.contains("/auth/v1/health") && st.contains("/rest/v1/") && st.contains("/storage/v1/status") && st.contains("info@onlineexam.ir"))
        assertTrue(src(".github/workflows/site.yml").contains("cp site/status.html site_out/status.html"))
    }

    @Test
    fun v230ManagerCompareExcelImportSchoolBankPdf() {
        val admin = src("site/src/admin.js")
        assertTrue(admin.contains("function classCompare(d) {") && admin.contains("S.api.managerClassStats().then(function (d) { cmp.innerHTML = ''; cmp.appendChild(classCompare(d)); })"))
        val app = src("site/src/app.js")
        assertTrue(app.contains("managerClassStats: function () { return rpcObj('native_manager_class_stats_v1', {}); }") && app.contains("function printSection(title, node) {") && app.contains("printSection: printSection,"))
        val school = src("site/src/school.js")
        assertTrue(school.contains("async function readSpreadsheet(file) {") && school.contains("new DecompressionStream('deflate-raw')") && school.contains("function rowsToStudents(rows) {") && school.contains("text: '📥 از اکسل'"))
        assertTrue(school.contains("S.rpcObj('native_bank_set_shared_v1', {p_id: it.id, p_shared: !it.shared})") && school.contains("S.rpcObj('native_school_bank_v1', {})") && school.contains("S.rpcObj('native_bank_copy_from_school_v1', {p_id: it.id})") && school.contains("text: '🏫 بانک مدرسه'"))
        assertTrue(src("site/src/extras.js").contains("if (S.printSection) S.printSection(cls ? 'گزارش کلاس ' + cls.name : 'گزارش کلاس', out)"))
        val css = src("site/src/site.css")
        assertTrue(css.contains("body.printing>*:not(#print-root){display:none!important}") && css.contains(".cmp-months{"))
        val sql = src("supabase/migrations/20261009_native_manager_class_stats_school_bank_v230.sql")
        listOf("native_manager_class_stats_v1()", "add column if not exists shared_school boolean", "native_bank_set_shared_v1(bigint,boolean)", "native_school_bank_v1()", "native_bank_copy_from_school_v1(bigint)", "'shared',q.shared_school").forEach { assertTrue(it, sql.contains(it)) }
    }
}
