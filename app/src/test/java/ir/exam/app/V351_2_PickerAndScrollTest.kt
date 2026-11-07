package ir.exam.app

import ir.exam.app.ui.code.CodeLanguageIcons
import ir.exam.app.ui.code.CodeLanguages
import ir.exam.app.ui.code.CodeText
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil
import kotlin.math.floor

/**
 * V351.2 — خواسته‌های کاربر:
 *
 * پنجرهٔ انتخاب زبان: «بستن» ضربدر قرمز و هم‌سطر عنوان، پنجرهٔ بلندتر،
 * حذف جملهٔ توضیحی، آیکن SVG برای هر زبان، نام انگلیسی در پرانتز جلوی
 * نام فارسی، و وسط‌چین.
 *
 * صفحهٔ کدنویسی: پس از خط ۱۱ اسکرول کم بود و خط بعدی نصفه دیده می‌شد.
 */
class V351_2_PickerAndScrollTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(path: String): String {
        val f = File(root(), path)
        assertTrue("فایل پیدا نشد: $path", f.exists())
        return f.readText()
    }

    private fun picker() = src("app/src/main/java/ir/exam/app/ui/code/CodeLanguagePickerDialog.kt")

    // ---------- ۱) ضربدر قرمز هم‌سطر عنوان ----------

    @Test
    fun closeIsARedCrossOnTheTitleRow() {
        val s = picker()
        assertTrue("دکمهٔ متنی بستن رفت", !s.contains("OutlinedButton(onClick = onDismiss) { Text(\"بستن\") }"))
        assertTrue("ضربدر قرمز", s.contains("Surface(color = Color(0xFFE5484D)"))
        assertTrue("آیکن ضربدر", s.contains("Icons.Outlined.Close"))
        assertTrue("برچسب دسترس‌پذیری بستن", s.contains("contentDescription = \"بستن\""))
        assertTrue("سفید روی قرمز", s.contains("tint = Color.White"))
        // عنوان در همان Row است، نه بیرون آن
        val rowAt = s.indexOf("horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)")
        val crossAt = s.indexOf("Icons.Outlined.Close")
        val titleAt = s.indexOf("Text(title, style = MaterialTheme.typography.titleMedium)")
        assertTrue("ردیف عنوان هست", rowAt > 0)
        assertTrue("ضربدر بعد از شروع ردیف", crossAt > rowAt)
        assertTrue("عنوان بعد از ضربدر و در همان ردیف", titleAt > crossAt)
        assertTrue("ردیف وسط‌چین", s.contains("Alignment.CenterHorizontally"))
    }

    // ---------- ۲) پنجرهٔ بلندتر و بی جملهٔ توضیحی ----------

    @Test
    fun windowIsTallerAndTheBlurbIsGone() {
        val s = picker()
        assertTrue("جملهٔ توضیحی رفت", !s.contains("محیط نوشتن کد با رنگی‌شدن دستورها"))
        assertTrue("دیگر «کد اجرا نمی‌شود» در این پنجره نیست", !s.contains("کد اجرا نمی‌شود"))
        assertTrue("شبکه همهٔ ارتفاع باقی‌مانده را می‌گیرد", s.contains("modifier = Modifier.fillMaxWidth().weight(1f)"))
        assertTrue("سقف بلندِ پیشین رفت", !s.contains("heightIn(max = 380.dp)"))
        // همان قاب بلند پنجرهٔ دانش‌آموز
        assertTrue("قاب تمام‌قد", s.contains("Modifier.fillMaxSize().systemBarsPadding().imePadding()"))
        assertTrue("تا ارتفاع در دسترس", s.contains("heightIn(max = availableHeight)"))
        assertTrue("عرض هم‌اندازهٔ پنجرهٔ دانش‌آموز", s.contains("widthIn(max = 620.dp)"))
    }

    // ---------- ۳) آیکن SVG برای هر زبان ----------

    @Test
    fun everyLanguageHasAnSvgIcon() {
        assertEquals(32, CodeLanguageIcons.count)
        assertEquals(CodeLanguages.all.size, CodeLanguageIcons.count)
        // دادهٔ مسیر سنجیده می‌شود، نه API کامپوز؛ تست واحد باید خالص بماند.
        val allowed = "MmLlHhVvCcSsQqTtAaZz0123456789.,-+eE ".toSet()
        CodeLanguages.all.forEach { lang ->
            assertTrue("نشان ندارد: " + lang.id, CodeLanguageIcons.has(lang.id))
            val d = CodeLanguageIcons.pathOf(lang.id)
            assertTrue("مسیر خالی: " + lang.id, d.length >= 12)
            assertTrue("مسیر با moveto شروع نمی‌شود: " + lang.id, d.startsWith("M") || d.startsWith("m"))
            assertTrue(
                "نویسهٔ نامعتبر در مسیر " + lang.id,
                d.all { it in allowed }
            )
            assertEquals("ساختار مسیر " + lang.id + " نامعتبر است", "", svgPathError(d))
        }
    }

    /**
     * اعتبارسنجی ساختار مسیر SVG: هر فرمان باید شمار پارامتر درست بگیرد.
     * مختصات نمی‌توانند کرانه داشته باشند، چون فرمان‌های کوچک نسبی‌اند و
     * عدد اعشاری می‌تواند بی‌صفر آغاز شود (مثل .175).
     */
    private fun svgPathError(d: String): String {
        val arity = mapOf(
            'M' to 2, 'L' to 2, 'T' to 2, 'H' to 1, 'V' to 1,
            'C' to 6, 'S' to 4, 'Q' to 4, 'A' to 7, 'Z' to 0
        )
        val number = Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?")
        var i = 0
        var cmd: Char? = null
        var n = 0
        while (i < d.length) {
            val ch = d[i]
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == ',') { i++; continue }
            val up = ch.uppercaseChar()
            if (arity.containsKey(up)) {
                cmd = if (up == 'Z') null else up
                i++; n = 0
                continue
            }
            val m = number.matchAt(d, i)
                ?: return "توکن نامعتبر در $i"
            if (cmd == null) return "عدد بی‌فرمان در $i"
            i = m.range.last + 1
            n++
            if (n == arity.getValue(cmd)) {
                n = 0
                if (cmd == 'M') cmd = 'L'
            }
        }
        return if (n == 0) "" else "پارامتر ناقص برای $cmd ($n)"
    }

    @Test
    fun iconSourceLivesInTheRepoNotInTemp() {
        // مولد باید خودبسنده باشد؛ ساخت نباید به شبکه یا /tmp نیاز داشته باشد.
        val gen = src("scripts/gen_code_lang_icons.py")
        assertTrue("منبع داخل مخزن", gen.contains("docs/tools/code_lang_icons.json"))
        assertTrue("به /tmp تکیه نمی‌کند", !gen.contains("/tmp/langicons"))
        val data = src("docs/tools/code_lang_icons.json")
        CodeLanguages.all.forEach { lang ->
            assertTrue("منبع نشان " + lang.id, data.contains("\"" + lang.id + "\": {"))
            assertTrue("مسیر نشان " + lang.id, data.contains(CodeLanguageIcons.pathOf(lang.id)))
        }
    }

    @Test
    fun unknownLanguageFallsBackInsteadOfCrashing() {
        assertTrue(!CodeLanguageIcons.has("no-such-language"))
        // نشان عمومی برمی‌گردد، نه استثنا
        assertEquals(CodeLanguageIcons.pathOf("asm"), CodeLanguageIcons.pathOf("no-such-language"))
    }

    @Test
    fun fiveLanguagesUseDrawnMarksAndTheRestAreOfficial() {
        val drawn = CodeLanguages.all.filter { CodeLanguageIcons.isStroked(it.id) }.map { it.id }
        assertEquals(listOf("csharp", "objc", "asm", "vb", "prolog").sorted(), drawn.sorted())
        assertEquals(27, CodeLanguageIcons.count - drawn.size)
    }

    @Test
    fun iconsSitInTheirOwnBrandColourTile() {
        val s = picker()
        assertTrue("کاشی رنگ خود زبان", s.contains("val tile = Color(lang.colorArgb)"))
        assertTrue("نشان از کتابخانهٔ SVG", s.contains("CodeLanguageIcons.of(lang.id)"))
        assertTrue("کاشی ۳۴dp", s.contains("modifier = Modifier.size(34.dp)"))
        assertTrue("نشان ۲۲dp با ۶dp حاشیه ⇒ وسط کاشی", s.contains("Modifier.padding(6.dp).size(22.dp)"))
        // روی رنگ روشن، نشان تیره تا خوانا بماند
        assertTrue("سنجش روشنی رنگ", s.contains("tile.luminance() > 0.6f"))
        assertTrue("نشان تیره", s.contains("Color(0xFF111111)"))
    }

    // ---------- ۴) نام انگلیسی در پرانتز و وسط‌چین ----------

    @Test
    fun englishNameFollowsThePersianNameInParentheses() {
        val s = picker()
        assertTrue("قالب برچسب", s.contains("lang.name + \" (\" + lang.english + \")\""))
        assertTrue("وسط‌چین", s.contains("textAlign = TextAlign.Center"))
        assertTrue("ستون وسط‌چین", s.contains("horizontalAlignment = Alignment.CenterHorizontally"))
    }

    @Test
    fun allThirtyTwoEnglishNamesArePresentAndCorrect() {
        val expected = mapOf(
            "python" to "Python", "javascript" to "JavaScript", "typescript" to "TypeScript",
            "java" to "Java", "c" to "C", "cpp" to "C++", "csharp" to "C#", "php" to "PHP",
            "go" to "Go", "kotlin" to "Kotlin", "sql" to "SQL", "r" to "R", "html" to "HTML",
            "css" to "CSS", "rust" to "Rust", "swift" to "Swift", "ruby" to "Ruby",
            "dart" to "Dart", "bash" to "Bash", "matlab" to "MATLAB", "asm" to "Assembly",
            "pascal" to "Pascal", "vb" to "Visual Basic", "lua" to "Lua", "scala" to "Scala",
            "perl" to "Perl", "julia" to "Julia", "fortran" to "Fortran", "prolog" to "Prolog",
            "lisp" to "Lisp", "groovy" to "Groovy", "objc" to "Objective-C"
        )
        assertEquals(32, expected.size)
        CodeLanguages.all.forEach { lang ->
            assertEquals("نام انگلیسی " + lang.id, expected.getValue(lang.id), lang.english)
        }
    }

    @Test
    fun siteAndAppShareTheSameEnglishNames() {
        val js = src("site/src/code.js")
        CodeLanguages.all.forEach { lang ->
            val at = js.indexOf("{id: '" + lang.id + "'")
            assertTrue("زبان در سایت هست: " + lang.id, at > 0)
            val line = js.substring(at, js.indexOf('\n', at))
            assertTrue("نام انگلیسی سایت برای " + lang.id, line.contains("en: '" + lang.english + "'"))
        }
    }

    // ---------- ۵) رفع کم‌اسکرولی پس از خط ۱۱ ----------

    @Test
    fun paddingIsOutsideTheScrollViewport() {
        val s = src("app/src/main/java/ir/exam/app/ui/code/CodeEditorDialog.kt")
        val padAt = s.indexOf(".padding(vertical = 8.dp)")
        val sizeAt = s.indexOf(".onSizeChanged { viewport = it }")
        val scrollAt = s.indexOf(".verticalScroll(vScroll)")
        assertTrue("حاشیه هست", padAt > 0)
        assertTrue("اندازه‌گیری بعد از حاشیه", sizeAt > padAt)
        assertTrue("اسکرول بعد از اندازه‌گیری", scrollAt > sizeAt)
    }

    @Test
    fun caretPositionComesFromTheRealTextLayout() {
        val s = src("app/src/main/java/ir/exam/app/ui/code/CodeEditorDialog.kt")
        assertTrue("چیدمان واقعی گرفته می‌شود", s.contains("onTextLayout = { textLayout = it }"))
        assertTrue("جعبهٔ نشانگر", s.contains("val box = layout.getCursorRect(caret)"))
        assertTrue("کرانهٔ ایمن", s.contains("caret <= layout.layoutInput.text.length"))
        assertTrue("گردش به بالا", s.contains("ceil(bottom - viewport.height)"))
        assertTrue("گردش به پایین", s.contains("floor(top)"))
        assertTrue("چیدمان در کلید اثر", s.contains("viewport, editorHeight, textLayout)"))
    }

    @Test
    fun theOldMathReallyDidUnderScrollByThePadding() {
        // بازتولید اشکال پیشین: مبدأ متن ۸dp پایین‌تر بود ولی حساب نمی‌شد
        val linePx = 20f
        val padPx = 8f
        // پیش‌تر onSizeChanged بیرونِ حاشیه بود، پس ارتفاع گزارش‌شده
        // شامل همان ۱۶dp حاشیه هم می‌شد.
        val viewport = 11 * linePx + 2 * padPx
        val caretLine = 11 // خط دوازدهم
        val oldTarget = (caretLine + 1) * linePx - viewport          // بی توجه به حاشیه
        val trueBottom = padPx + (caretLine + 1) * linePx            // جای واقعی
        val needed = trueBottom - viewport
        assertEquals(4f, oldTarget, 0.01f)
        assertEquals(12f, needed, 0.01f)
        assertTrue("کم اسکرول می‌شد", oldTarget < needed)
        assertEquals(8f, needed - oldTarget, 0.01f) // دقیقاً اندازهٔ حاشیه
    }

    @Test
    fun theNewMathNeverLeavesTheCaretLineHalfVisible() {
        val linePx = 20f
        val viewport = 11 * linePx
        // با حاشیهٔ بیرون‌شده، مبدأ متن صفر است؛ جای واقعی از چیدمان می‌آید
        fun target(top: Float, bottom: Float, current: Float): Float {
            val v = when {
                top < current -> floor(top)
                bottom > current + viewport -> ceil(bottom - viewport)
                else -> current
            }
            return v.coerceIn(0f, 4000f)
        }
        // خط دوازدهم با باد فونت (۲۰٫۶px به‌جای ۲۰)
        val real = 20.6f
        val top = 11 * real
        val bottom = top + real
        val t = target(top, bottom, 0f)
        assertTrue("خط کامل دیده می‌شود", t + viewport >= bottom)
        assertTrue("بیش از لازم اسکرول نمی‌شود", t <= bottom - viewport + 1f)
        // صد خط پشت سر هم: هر خط باید کامل در دید بیفتد
        for (line in 11 until 100) {
            val a = line * real
            val b = a + real
            val cur = (line - 11) * real
            val tt = target(a, b, cur)
            assertTrue("خط $line نصفه می‌ماند", tt + viewport >= b - 0.001f)
            assertTrue("خط $line از بالا بیرون می‌زند", tt <= a + 0.001f)
        }
    }

    @Test
    fun growthContractIsUnchanged() {
        assertEquals(11, CodeText.EDITOR_MAX_LINES)
        assertEquals(3, CodeText.EDITOR_MIN_LINES)
        assertEquals(11, CodeText.editorVisibleLines((1..40).joinToString("\n") { "x" }))
    }
}
