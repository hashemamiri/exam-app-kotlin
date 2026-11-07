package ir.exam.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V349 — محیط اختصاصی هر زبان در سایت: ۳۲ زبان، بخش علائم،
 * قطعه‌کدهای آماده، کامل‌کردن خودکار و شکل پاسخ «هر دو».
 * همچنین هم‌سانی جدول زبان‌های سایت و اپ را نگهبانی می‌کند.
 */
class V349_Site_CodeEnvironmentTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(name: String): String = File(root(), "site/src/$name").readText()

    private val ids = listOf(
        "python", "javascript", "typescript", "java", "c", "cpp", "csharp", "php",
        "go", "kotlin", "sql", "r", "html", "css", "rust", "swift", "ruby", "dart",
        "bash", "matlab", "asm", "pascal", "vb", "lua", "scala", "perl", "julia",
        "fortran", "prolog", "lisp", "groovy", "objc"
    )

    @Test
    fun siteOffersThirtyTwoLanguages() {
        val c = src("code.js")
        for (id in ids) assertTrue("Missing language $id", c.contains("id: '$id'"))
    }

    /** جدول اپ از سایت تولید می‌شود؛ اگر یکی جلو بیفتد این آزمون می‌شکند. */
    @Test
    fun appAndSiteShareTheSameLanguageTable() {
        val js = src("code.js")
        val kt = File(root(), "app/src/main/java/ir/exam/app/ui/code/CodeLanguages.kt").readText()
        val jsIds = Regex("\\{id: '([a-z0-9]+)', name:").findAll(js).map { it.groupValues[1] }.toList()
        val ktIds = Regex("id = \"([a-z0-9]+)\", name =").findAll(kt).map { it.groupValues[1] }.toList()
        assertEquals(32, jsIds.size)
        assertEquals("app and site language tables drifted", jsIds, ktIds)
        assertTrue(
            "CodeLanguages.kt must stay generated",
            kt.contains("scripts/gen_code_langs.py")
        )
        assertTrue(File(root(), "scripts/gen_code_langs.py").exists())
    }

    @Test
    fun perLanguageEnvironmentIsExposed() {
        val c = src("code.js")
        for (member in listOf(
            "symbolsFor: symbolsFor", "snippetsFor: snippetsFor", "completions: completions",
            "var SYM_FAMILY", "var LANG_FAMILY", "var SNIPPETS",
            "function symbolsFor(", "function snippetsFor(", "function completions("
        )) {
            assertTrue("Missing per-language API: $member", c.contains(member))
        }
    }

    @Test
    fun symbolBarAndSnippetsAreWiredIntoTheEditor() {
        val c = src("code.js")
        for (needle in listOf(
            "class: 'cx-keys'", "class: 'cx-snips'", "function buildRows(",
            "function insertText(", "buildRows(); acHide();"
        )) {
            assertTrue("Missing editor wiring: $needle", c.contains(needle))
        }
    }

    @Test
    fun autocompleteIsWiredAndKeyboardControlled() {
        val c = src("code.js")
        for (needle in listOf(
            "class: 'cx-ac'", "function acShow(", "function acAccept(", "function acHide(",
            "paint(); acShow();", "if (acOpen) {", "e.key === 'Escape'"
        )) {
            assertTrue("Missing autocomplete wiring: $needle", c.contains(needle))
        }
        // بزرگی حروف نوشتهٔ کاربر باید حفظ شود
        assertTrue(c.contains("w = w.toUpperCase()"))
    }

    @Test
    fun teacherCanAskForTextAndCodeTogether() {
        val b = src("builder.js")
        assertTrue("Missing multi-select handler", b.contains("function setForms("))
        assertTrue("Missing both flag", b.contains("q.codeWithText"))
        assertTrue("Must serialize answer_form", b.contains("v.answer_form = 'both'"))
        assertTrue("Must load answer_form", b.contains("q.codeWithText = obj.answer_form === 'both'"))
        assertTrue("Must clear answer_form when not code", b.contains("delete v.answer_form"))
        // دست‌کم یک شکل پاسخ باید روشن بماند
        assertTrue(b.contains("if (!wantText && !wantCode) return;"))
    }

    @Test
    fun studentGetsTwoSeparateBoxes() {
        val s = src("student.js")
        assertTrue("Missing both-mode class", s.contains("cx-answer-both"))
        assertTrue("Missing compose helper", s.contains("function compose("))
        assertTrue("Must load both flag", s.contains("q.codeWithText = obj.answer_form === 'both'"))
        assertTrue("Text box must be labelled", s.contains("توضیح متنی شما"))
        assertTrue("Code box must be labelled", s.contains("پاسخ کد شما"))
    }

    @Test
    fun newUiHasStylesAndPrintRules() {
        val css = src("site.css")
        for (needle in listOf(".cx-keys{", ".cx-key{", ".cx-snips{", ".cx-ac{", ".cx-ac-item{", ".cx-answer-both{")) {
            assertTrue("Missing style: $needle", css.contains(needle))
        }
        assertTrue("Toolbars must not print", css.contains(".cx-keys,.cx-snips,.cx-ac{display:none!important}"))
    }

    @Test
    fun codeIsStillNeverExecuted() {
        val c = src("code.js")
        for (forbidden in listOf("eval(", "new Function", "XMLHttpRequest", "fetch(", "import(")) {
            assertTrue("Code tool must not execute or send code: $forbidden", !c.contains(forbidden))
        }
        val kt = File(root(), "app/src/main/java/ir/exam/app/ui/code/CodeText.kt").readText()
        for (forbidden in listOf("Runtime.getRuntime", "ProcessBuilder", "ScriptEngine", "HttpURLConnection")) {
            assertTrue("App code tool must not execute code: $forbidden", !kt.contains(forbidden))
        }
    }
}
