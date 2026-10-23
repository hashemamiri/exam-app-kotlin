package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

/**
 * V335.1 — ممیزی بصری و هم‌ترازی کامل سایر صفحات وب‌سایت با اپلیکیشن اندروید:
 * ۱. پاک‌سازی خودکار پنجره‌های مودال و منوهای شناور هنگام تغییر مسیر (`closeModalsOnNav`) و بستن با کلید `Escape` بدون حذف `#authBg`.
 * ۲. بازطراحی ویرایشگر تک‌سؤال بانک (`state.bankEdit`) با ستون راست درس و دسته‌ها، انتخابگر مستقیم نوع سؤال و ریل تک‌سؤالی بدون حذف خاموش سؤال دوم.
 * ۳. رفع باگ دوآرگومانی `el('div', [...])` در کارت‌های بانک سؤال (`school.js` و `builder.js`) + ایمن‌سازی `el(tag, attrs, children)`.
 * ۴. نمایش کامل تراشه‌های بازخورد تشویقی دبیر در پنل تصحیح بدون برش ناقص وسط کلمه و به‌روزرسانی متن راهنمای بانک خالی.
 */
class V335_1_WebParityAuditTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }

    private fun read(path: String): String = File(root(), path).readText()

    @Test
    fun `modals close cleanly on escape and route navigation without touching authBg`() {
        val app = read("site/src/app.js")
        assertTrue("closeModalsOnNav helper must exist", "function closeModalsOnNav()" in app)
        assertTrue("closeModalsOnNav must exclude static #authBg", ".modal-bg:not(#authBg)" in app)
        assertTrue("render() must invoke closeModalsOnNav()", "closeModalsOnNav();" in app)
        assertTrue("Escape handler must cancel open modals", "e.key === 'Escape'" in app && "topM._onCancel" in app)
        assertTrue("el() helper must safely handle array second argument", "if (Array.isArray(attrs)) { children = attrs; attrs = null; }" in app)
    }

    @Test
    fun `zero accidental two arg el div array calls remain across entire site source`() {
        val pat = Pattern.compile("\\bel\\(\\s*['\"][^'\"]+['\"]\\s*,\\s*\\[")
        val siteDir = File(root(), "site/src")
        val offenders = siteDir.listFiles { _, n -> n.endsWith(".js") }.orEmpty().flatMap { f ->
            val m = pat.matcher(f.readText())
            val hits = mutableListOf<String>()
            while (m.find()) hits += f.name
            hits
        }
        assertTrue("Found unguarded el(tag, [...]) calls: $offenders", offenders.isEmpty())
    }

    @Test
    fun `question bank single editor and grading feedback chips match android ux`() {
        val builder = read("site/src/builder.js")
        val school = read("site/src/school.js")
        val admin = read("site/src/admin.js")
        val e2e = read("scripts/web_ui_e2e.py")

        assertTrue("Desktop bank editor right panel must show subject & categories", "مشخصات سؤال بانک" in builder && "دسته‌های بانک سؤال" in builder)
        assertTrue("Bank editor must expose direct question type selector", "'aria-label': 'نوع سؤال'" in builder)
        assertTrue("Bank editor header must hide multi-question buttons", "state.bankEdit ? null : el('button', {class: 'icon-btn', title: 'افزودن به بانک سؤال'" in builder)

        assertFalse("Stale empty-bank copy must be removed", "ذخیرهٔ سؤال جاری در بانک» اضافه کنید" in school)
        assertTrue("Updated empty-bank guidance must match real UI buttons", "➕ سؤال جدید در بانک" in school && "افزودن به بانک سؤال" in school)

        assertFalse("Encouragement stamps must not slice mid-word at char 24", "st.slice(0, 24)" in admin)
        assertTrue("Web E2E suite must cover bank single-question flow", "08b_teacher_bank_builder.png" in e2e && "08c_teacher_bank_saved.png" in e2e)
    }
}
