package ir.exam.app

import ir.exam.app.ui.code.CodeLanguages
import ir.exam.app.ui.code.CodeText
import ir.exam.app.ui.code.CodeTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V349 — محیط کدنویسی: ۳۲ زبان، شکل پاسخ «هر دو»، نوار علائم،
 * قطعه‌کدهای آماده و کامل‌کردن خودکار. کد اجرا نمی‌شود.
 */
class V349_CodeToolTest {

    @Test
    fun thirtyTwoLanguagesWithUniqueIds() {
        assertEquals(32, CodeLanguages.all.size)
        assertEquals(32, CodeLanguages.all.map { it.id }.toSet().size)
        val expected = listOf(
            "python", "javascript", "typescript", "java", "c", "cpp", "csharp", "php",
            "go", "kotlin", "sql", "r", "html", "css", "rust", "swift", "ruby", "dart",
            "bash", "matlab", "asm", "pascal", "vb", "lua", "scala", "perl", "julia",
            "fortran", "prolog", "lisp", "groovy", "objc"
        )
        assertEquals(expected, CodeLanguages.all.map { it.id })
    }

    @Test
    fun everyLanguageHasItsOwnEnvironment() {
        for (lang in CodeLanguages.all) {
            assertTrue("no symbols: ${lang.id}", lang.symbols.size >= 12)
            assertTrue("no snippets: ${lang.id}", lang.snippets.size >= 4)
            assertTrue("no keywords: ${lang.id}", lang.keywords.size >= 15)
            assertTrue("no sample: ${lang.id}", lang.sample.isNotBlank())
            assertTrue("no extension: ${lang.id}", lang.ext.isNotBlank())
            assertTrue("no indent: ${lang.id}", lang.indent in 2..4)
            assertTrue("no comment style: ${lang.id}", lang.lineComment.isNotEmpty() || lang.blockOpen.isNotEmpty())
        }
    }

    @Test
    fun symbolOrderIsLanguageSpecific() {
        assertEquals(":", CodeLanguages.byIdOr("python").symbols.first())
        assertEquals("<", CodeLanguages.byIdOr("html").symbols.first())
        assertEquals("{", CodeLanguages.byIdOr("css").symbols.first())
        assertTrue(CodeLanguages.byIdOr("sql").symbols.contains("<>"))
        assertFalse(CodeLanguages.byIdOr("python").symbols == CodeLanguages.byIdOr("html").symbols)
    }

    @Test
    fun snippetsAreRealCodeOfThatLanguage() {
        assertTrue(CodeLanguages.byIdOr("sql").snippets.first().code.startsWith("SELECT"))
        assertTrue(CodeLanguages.byIdOr("python").snippets.first().code.startsWith("def "))
        assertTrue(CodeLanguages.byIdOr("html").snippets.first().code.startsWith("<!DOCTYPE"))
        assertTrue(CodeLanguages.byIdOr("rust").snippets.first().code.startsWith("fn "))
        // نشان جای نشانگر باید در قطعه‌ها باشد و پس از درج پاک شود
        val snip = CodeLanguages.byIdOr("python").snippets.first().code
        assertTrue(snip.contains(CodeText.CARET))
        val (value, caret) = CodeText.insert("", 0, 0, snip)
        assertFalse(value.contains(CodeText.CARET))
        assertEquals(4, caret)
    }

    @Test
    fun languageAliasesAndFallback() {
        assertEquals("python", CodeLanguages.byIdOr("py").id)
        assertEquals("cpp", CodeLanguages.byIdOr("c++").id)
        assertEquals("csharp", CodeLanguages.byIdOr("c#").id)
        assertEquals("bash", CodeLanguages.byIdOr("shell").id)
        assertEquals("objc", CodeLanguages.byIdOr("objective-c").id)
        assertEquals("lisp", CodeLanguages.byIdOr("scheme").id)
        assertEquals("python", CodeLanguages.byIdOr("klingon").id)
        assertEquals("python", CodeLanguages.byIdOr(null).id)
    }

    @Test
    fun fenceRoundTripKeepsCodeIntact() {
        val code = "def f():\n    return 1"
        val stored = CodeText.fence(code, "python")
        assertEquals("```python\n" + code + "\n```", stored)
        val back = CodeText.parseFence(stored)
        assertNotNull(back)
        assertEquals("python", back!!.lang)
        assertEquals(code, back.code)
    }

    @Test
    fun tripleBacktickInsideCodeDoesNotBreakTheBlock() {
        val code = "s = \"```\"\nprint(s)"
        val stored = CodeText.fence(code, "python")
        val back = CodeText.parseFence(stored)
        assertNotNull(back)
        assertEquals(code, back!!.code)
    }

    @Test
    fun parseFenceReturnsNullForPlainText() {
        assertNull(CodeText.parseFence("پاسخ معمولی بدون کد"))
        assertFalse(CodeText.hasFence("پاسخ معمولی"))
        assertTrue(CodeText.hasFence("```python\nx = 1\n```"))
    }

    @Test
    fun bothFormsStoreTextThenCode() {
        val stored = CodeText.compose("توضیح من", CodeText.fence("x = 1", "python"))
        val parts = CodeText.split(stored)
        assertEquals(2, parts.size)
        assertFalse(parts[0].isCode)
        assertTrue(parts[1].isCode)
        assertTrue(parts[0].text.contains("توضیح من"))
        assertEquals("x = 1", parts[1].text.trim())
        assertEquals("توضیح من", CodeText.textPartOf(stored))
    }

    @Test
    fun composeHandlesEmptySides() {
        assertEquals("فقط متن", CodeText.compose("فقط متن", ""))
        val f = CodeText.fence("x = 1", "python")
        assertEquals(f, CodeText.compose("", f))
        assertEquals("", CodeText.compose("", ""))
    }

    @Test
    fun composeStripsFencesFromStudentText() {
        val stored = CodeText.compose("متن با ``` شبه‌کد", CodeText.fence("x = 1", "python"))
        // تنها یک بلوک کد باید وجود داشته باشد
        assertEquals(1, CodeText.split(stored).count { it.isCode })
    }

    @Test
    fun insertReplacesSelectionAndPlacesCaret() {
        val (value, caret) = CodeText.insert("abc", 1, 2, "XY")
        assertEquals("aXYc", value)
        assertEquals(3, caret)
    }

    @Test
    fun enterKeepsIndentationAndAddsOneLevelAfterColon() {
        val text = "def f():"
        val (value, caret) = CodeText.enterInsert(text, text.length, 4)
        assertEquals("def f():\n    ", value)
        assertEquals(13, caret)
    }

    @Test
    fun enterUsesLanguageIndentWidth() {
        val text = "function f() {"
        val (value, _) = CodeText.enterInsert(text, text.length, 2)
        assertEquals("function f() {\n  ", value)
    }

    @Test
    fun enterKeepsPreviousIndentWithoutOpener() {
        val text = "    x = 1"
        val (value, _) = CodeText.enterInsert(text, text.length, 4)
        assertEquals("    x = 1\n    ", value)
    }

    @Test
    fun tabIndentsAndShiftTabRemoves() {
        val (value, caret) = CodeText.tab("x", 0, 4, false)
        assertEquals("    x", value)
        assertEquals(4, caret)
        val (back, backCaret) = CodeText.tab("    x", 4, 4, true)
        assertEquals("x", back)
        assertEquals(0, backCaret)
    }

    @Test
    fun autoClosePairs() {
        assertEquals(')', CodeText.autoClose('('))
        assertEquals(']', CodeText.autoClose('['))
        assertEquals('}', CodeText.autoClose('{'))
        assertNull(CodeText.autoClose('x'))
    }

    @Test
    fun suggestionsNeedTwoLettersAndMatchPrefix() {
        assertTrue(CodeText.suggestions("python", "d", 1).isEmpty())
        val list = CodeText.suggestions("python", "de", 2)
        assertTrue(list.contains("def"))
        assertTrue(CodeText.suggestions("python", "xyzq", 4).isEmpty())
    }

    @Test
    fun suggestionsKeepUserUppercaseForSql() {
        val list = CodeText.suggestions("sql", "SEL", 3)
        assertTrue("expected SELECT in $list", list.contains("SELECT"))
    }

    @Test
    fun suggestionsArePerLanguage() {
        assertTrue(CodeText.suggestions("rust", "str", 3).contains("struct"))
        assertFalse(CodeText.suggestions("python", "str", 3).contains("struct"))
    }

    @Test
    fun acceptReplacesTheHalfTypedWord() {
        val (value, caret) = CodeText.accept("de", 2, "def")
        assertEquals("def", value)
        assertEquals(3, caret)
        val (mid, midCaret) = CodeText.accept("x = de", 6, "def")
        assertEquals("x = def", mid)
        assertEquals(7, midCaret)
    }

    @Test
    fun wordBeforeIgnoresDigitsAndSymbols() {
        assertEquals("def", CodeText.wordBefore("def", 3))
        assertEquals("", CodeText.wordBefore("123", 3))
        assertEquals("ab", CodeText.wordBefore("x = ab", 6))
        assertEquals("", CodeText.wordBefore("", 0))
    }

    @Test
    fun highlighterFindsKeywordCommentStringAndNumber() {
        val lang = CodeLanguages.byIdOr("python")
        val code = "def f():\n    # note\n    return 42"
        val spans = CodeTokens.spans(code, lang)
        assertTrue(spans.any { it.kind == CodeTokens.Kind.KEYWORD && code.substring(it.start, it.end) == "def" })
        assertTrue(spans.any { it.kind == CodeTokens.Kind.COMMENT })
        assertTrue(spans.any { it.kind == CodeTokens.Kind.NUMBER })
        val withText = "s = \"hello\""
        assertTrue(CodeTokens.spans(withText, lang).any { it.kind == CodeTokens.Kind.STRING })
    }

    @Test
    fun highlighterUsesEachLanguageCommentStyle() {
        val cases = listOf(
            "python" to "# note", "javascript" to "// note", "sql" to "-- note",
            "matlab" to "% note", "asm" to "; note", "vb" to "' note",
            "fortran" to "! note", "lisp" to "; note"
        )
        for ((id, line) in cases) {
            val spans = CodeTokens.spans(line, CodeLanguages.byIdOr(id))
            assertTrue("comment not found for $id", spans.any { it.kind == CodeTokens.Kind.COMMENT })
        }
    }

    @Test
    fun highlighterHandlesBlockComments() {
        val html = CodeTokens.spans("<!-- hi -->", CodeLanguages.byIdOr("html"))
        assertTrue(html.any { it.kind == CodeTokens.Kind.COMMENT })
        val css = CodeTokens.spans("/* hi */", CodeLanguages.byIdOr("css"))
        assertTrue(css.any { it.kind == CodeTokens.Kind.COMMENT })
    }

    @Test
    fun sqlKeywordsAreCaseInsensitive() {
        val lang = CodeLanguages.byIdOr("sql")
        for (word in listOf("select", "SELECT", "Select")) {
            val spans = CodeTokens.spans(word, lang)
            assertTrue("not keyword: $word", spans.any { it.kind == CodeTokens.Kind.KEYWORD })
        }
    }

    @Test
    fun spansStayInsideTextBounds() {
        for (lang in CodeLanguages.all) {
            for (span in CodeTokens.spans(lang.sample, lang)) {
                assertTrue("bad span in ${lang.id}", span.start >= 0 && span.end <= lang.sample.length)
                assertTrue("empty span in ${lang.id}", span.start < span.end)
            }
        }
    }

    @Test
    fun lineNumbersMatchLineCount() {
        assertEquals("1", CodeTokens.lineNumbers("x"))
        assertEquals("1\n2", CodeTokens.lineNumbers("x\ny"))
        assertEquals("1\n2\n3", CodeTokens.lineNumbers("x\ny\nz"))
    }

    @Test
    fun maxCodeLengthMatchesTheSite() {
        assertEquals(20000, CodeText.MAX)
    }
}
