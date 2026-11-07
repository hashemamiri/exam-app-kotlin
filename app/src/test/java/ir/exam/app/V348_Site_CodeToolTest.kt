package ir.exam.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V348 — ابزار کدنویسی سایت (محیط شبیه‌سازی، بدون اجرای واقعی کد).
 * معلم کد را به سؤال می‌افزاید یا از دانش‌آموز پاسخ کد می‌خواهد؛ تصحیح دستی است.
 * این آزمون فقط فایل‌های سایت را می‌خواند (در CI اپ اجرا نمی‌شود).
 */
class V348_Site_CodeToolTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(name: String): String = File(root(), "site/src/$name").readText()

    private val languages = listOf(
        "python", "javascript", "typescript", "java", "c", "cpp",
        "csharp", "php", "go", "kotlin", "sql", "r"
    )

    @Test
    fun allTwelveLanguagesAreOffered() {
        val c = src("code.js")
        for (id in languages) {
            assertTrue("Missing language entry $id", c.contains("id: '$id'"))
        }
        assertTrue("Language list must be exported", c.contains("LANGS: LANGS"))
    }

    @Test
    fun publicApiIsComplete() {
        val c = src("code.js")
        for (member in listOf(
            "lang: lang", "highlight: highlight", "blockHtml: blockHtml", "split: split",
            "fence: fence", "parseFence: parseFence", "hasFence: hasFence",
            "open: open", "openWithPicker: openWithPicker", "pickLanguage: pickLanguage", "MAX: MAX"
        )) {
            assertTrue("Missing SiteCode export: $member", c.contains(member))
        }
    }

    /** وعدهٔ اصلی این نسخه: کد فقط نوشته و ذخیره می‌شود، اجرا نمی‌شود. */
    @Test
    fun editorNeverExecutesOrUploadsCode() {
        val c = src("code.js")
        for (forbidden in listOf("eval(", "new Function", "XMLHttpRequest", "fetch(", "import(")) {
            assertFalse("Code tool must not execute or send code: $forbidden", c.contains(forbidden))
        }
        assertTrue("User code must be HTML-escaped", c.contains("esc(text)"))
    }

    @Test
    fun builderInsertsStoresAndRestoresCodeQuestions() {
        val b = src("builder.js")
        assertTrue("Missing code toolbar button", b.contains("tool-btn is-code"))
        assertTrue("Missing code icon", b.contains("toolSvg('code')"))
        assertTrue("Missing code answer-mode section", b.contains("function codeModeSection"))
        assertTrue("Loader must accept code mode", b.contains("obj.mode === 'code'"))
        assertTrue("Serializer must emit code mode", b.contains("v.mode = 'code'"))
        assertTrue("Serializer must emit language", b.contains("v.code_lang"))
        assertTrue("Serializer must emit starter code", b.contains("v.code_starter"))
        // پاک‌سازی نهایی نباید حالت code را دور بیندازد (اشکال واقعی که در تست مرورگر پیدا شد).
        assertTrue(
            "Final mode cleanup must keep code mode",
            b.contains("['voice','steps','proof','construct','code'].indexOf(q.mode)")
        )
    }

    @Test
    fun studentCanAnswerInCodeAndSeeItHighlighted() {
        val s = src("student.js")
        assertTrue("Loader must accept code mode", s.contains("obj.mode === 'code'"))
        assertTrue("Missing code answer block", s.contains("function codeAnswerBlock"))
        // V349 — پاسخ با متن همراه ترکیب می‌شود، پس ذخیره از مسیر compose می‌گذرد
        assertTrue("Answer must be saved as a fenced block", s.contains("C.fence(r.code, r.lang)"))
        assertTrue("Answer must go through compose", s.contains("save(compose(textPart, C.fence(r.code, r.lang)))"))
        assertTrue("Starter code must reach the student editor", s.contains("starter: q.codeStarter"))
        assertTrue("Question renderer must be fence-aware", s.contains("function richHtmlPlain"))
    }

    @Test
    fun teacherGradingRendersStudentCode() {
        val a = src("admin.js")
        assertTrue("Grading view must render fenced code", a.contains("SiteCode.hasFence"))
    }

    @Test
    fun codeEditorIsLeftToRightInsideThePersianPage() {
        val css = src("site.css")
        val pane = css.lineSequence().first { it.startsWith(".cx-pane{") }
        assertTrue("Editor pane must be LTR", pane.contains("direction:ltr"))
        val highlight = css.lineSequence().first { it.startsWith(".cx-pane .cx-hl{") }
        assertTrue("Highlight layer must be LTR too", highlight.contains("direction:ltr"))
        val block = css.lineSequence().first { it.startsWith(".cx-block{") }
        assertTrue("Rendered code block must be LTR", block.contains("direction:ltr"))
    }

    @Test
    fun codePrintsOnWhiteAndFitsPhones() {
        val css = src("site.css")
        // V349.2 — سفید ثابتِ چاپ مثل قاب شکل به شکل #FFFFFF نوشته می‌شود (قاعدهٔ V218)
        assertTrue("Print must use a light background", css.contains(".cx-block{background:#FFFFFF"))
        // V352 — کد داخل خود متن تراشه است؛ استثنای V348 که پیش‌نمایش را
        // روی دسکتاپ برمی‌گرداند حذف شد. روی موبایل پیش‌نمایش می‌ماند.
        assertFalse("Desktop code preview exception is gone", css.contains(".b-live.cx-has-code"))
        assertTrue("Desktop preview stays hidden", css.contains(".dk .b-editor .b-live{display:none!important}"))
        assertTrue("Mobile toolbar must stay on one row", css.contains(".cx-bar{gap:6px;flex-wrap:nowrap}"))
    }

    @Test
    fun codeModuleIsBundledIntoTheSite() {
        val build = File(root(), "site/build_site.py").readText()
        assertTrue("code.js must be part of the panels bundle", build.contains("code.js"))
    }
}
