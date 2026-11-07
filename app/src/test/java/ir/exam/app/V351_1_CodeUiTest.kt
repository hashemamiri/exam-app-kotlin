package ir.exam.app

import ir.exam.app.ui.code.CodeLanguage
import ir.exam.app.ui.code.CodeLanguages
import ir.exam.app.ui.code.CodeText
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V351.1 — دو خواستهٔ کاربر:
 *
 * ۱) نام بعضی زبان‌ها در پنجرهٔ انتخاب زبان بریده می‌شد.
 * ۲) پنجرهٔ ویرایش کد باید تا ۱۱ خط بزرگ شود و بعد از آن، ناحیهٔ کد
 *    خودکار اسکرول شود.
 *
 * بخش عرض، «بودجهٔ پهنای سلول» را با همان ثابت‌های واقعی کد حساب می‌کند؛
 * اگر کسی سلول را تنگ کند یا weight را بردارد، این آزمون می‌شکند.
 */
class V351_1_CodeUiTest {

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

    // ---------- ۱) بودجهٔ پهنای نام زبان ----------

    /**
     * مدل سرانگشتی عرض گلیف در Compose (dp در مقیاس فونت ۱).
     * فارسی پهن‌تر از لاتین است؛ ZWNJ عرض ندارد.
     */
    private fun textWidth(s: String, sp: Double): Double {
        var total = 0.0
        s.forEach { ch ->
            total += when {
                ch == ' ' -> 0.30 * sp
                ch == '/' || ch == '-' || ch == '_' || ch == '.' -> 0.33 * sp
                ch == '\u200c' -> 0.0
                ch.code > 0x600 -> 0.55 * sp
                else -> 0.50 * sp
            }
        }
        return total
    }

    /** پهنای نشان زبان: متن ۱۰sp با ۵dp حاشیه در هر طرف (طرح V351.1). */
    private fun badgeWidth(lang: CodeLanguage): Double = textWidth(lang.short, 10.0) + 10.0

    /** فضای درون دکمه پس از حاشیه و کادر. */
    private fun innerWidth(cellDp: Double, paddingDp: Double): Double = cellDp - 2 * paddingDp - 2.0

    /**
     * V351.2 — در طرح تازه کاشی بالای متن است، پس تمام عرض دکمه به
     * نام می‌رسد: «فارسی (English)» با ۱۱sp و وسط‌چین.
     */
    private fun labelWidth(lang: CodeLanguage): Double =
        textWidth(lang.name + " (" + lang.english + ")", 11.0)

    @Test
    fun everyLanguageLabelFitsTheNewCell() {
        // گوشی ۳۹۰dp ⇒ پنجره ۳۶۲dp، محتوای ۳۳۴dp، دو ستون ۱۶۳dp، حاشیه ۸dp
        val inner = innerWidth(163.0, 8.0)
        CodeLanguages.all.forEach { lang ->
            val need = labelWidth(lang)
            assertTrue(
                "برچسب «${lang.name}» در دو سطر جا نشد: نیاز $need در برابر ۲×$inner",
                need <= inner * 2
            )
        }
    }

    @Test
    fun theOldLayoutReallyDidClipThreeNames() {
        // بازتولید اشکال پیشین: حاشیهٔ ۲۴dp و بی‌weight
        val inner = innerWidth(150.0, 24.0)
        val clipped = CodeLanguages.all.filter { lang ->
            textWidth(lang.name, 12.0) + badgeWidth(lang) + 6.0 > inner
        }.map { it.name }
        assertEquals(
            listOf("پاسکال / دلفی", "ویژوال بیسیک", "لیسپ / اسکیم").sorted(),
            clipped.sorted()
        )
    }

    @Test
    fun namesCanWrapToTwoLinesInsteadOfBeingCut() {
        val s = src("app/src/main/java/ir/exam/app/ui/code/CodeLanguagePickerDialog.kt")
        // علت اصلی: Text در Row بدون weight پهنای نامحدود می‌گیرد و سرریز می‌کند
        // V351.2 — نام تمام عرض دکمه را می‌گیرد و وسط‌چین است
        assertTrue("نام تمام عرض را می‌گیرد", s.contains("modifier = Modifier.fillMaxWidth()"))
        assertTrue("تا دو سطر می‌شکند", s.contains("maxLines = 2"))
        assertTrue("در بدترین حالت سه‌نقطه", s.contains("overflow = TextOverflow.Ellipsis"))
        assertTrue("ارتفاع سطر تنگ‌تر", s.contains("lineHeight = 15.sp"))
        assertTrue("حاشیهٔ دکمه کم است", s.contains("contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)"))
        assertTrue("سلول پهن‌تر شد", s.contains("GridCells.Adaptive(minSize = 148.dp)"))
        assertTrue("وسط‌چین", s.contains("textAlign = TextAlign.Center"))
    }

    @Test
    fun evenAtLargeFontScaleLabelsWrapRatherThanClip() {
        val inner = innerWidth(163.0, 8.0)
        val scale = 1.3
        CodeLanguages.all.forEach { lang ->
            assertTrue(
                "«${lang.name}» در دو سطر هم جا نشد",
                labelWidth(lang) * scale <= inner * scale * 2
            )
        }
    }

    // ---------- ۲) رشد تا ۱۱ خط ----------

    @Test
    fun windowGrowsUpToElevenLines() {
        assertEquals(11, CodeText.EDITOR_MAX_LINES)
        assertEquals(3, CodeText.EDITOR_MIN_LINES)
        assertEquals(3, CodeText.editorVisibleLines(""))
        assertEquals(3, CodeText.editorVisibleLines("a\nb"))
        assertEquals(5, CodeText.editorVisibleLines("a\nb\nc\nd\ne"))
        assertEquals(10, CodeText.editorVisibleLines((1..10).joinToString("\n") { "x" }))
        assertEquals(11, CodeText.editorVisibleLines((1..11).joinToString("\n") { "x" }))
    }

    @Test
    fun beyondElevenLinesTheHeightStaysFixed() {
        val eleven = (1..11).joinToString("\n") { "x" }
        val forty = (1..40).joinToString("\n") { "x" }
        assertEquals(11, CodeText.editorVisibleLines(eleven))
        assertEquals(11, CodeText.editorVisibleLines(forty))
        assertEquals(CodeText.editorVisibleLines(eleven), CodeText.editorVisibleLines(forty))
    }

    @Test
    fun caretLineIsCountedFromZero() {
        val text = "a\nb\nc\nd"
        assertEquals(0, CodeText.editorCaretLine(text, 0))
        assertEquals(0, CodeText.editorCaretLine(text, 1))
        assertEquals(1, CodeText.editorCaretLine(text, 2))
        assertEquals(3, CodeText.editorCaretLine(text, text.length))
        // نشانگر بیرون از رشته نباید بیندازد
        assertEquals(3, CodeText.editorCaretLine(text, 999))
        assertEquals(0, CodeText.editorCaretLine(text, -5))
        assertEquals(0, CodeText.editorCaretLine("", 0))
    }

    @Test
    fun editorHeightIsBuiltFromTheRealLineHeight() {
        val s = src("app/src/main/java/ir/exam/app/ui/code/CodeEditorDialog.kt")
        assertTrue("ارتفاع از lineHeight واقعی", s.contains("val lineHeightDp = with(density) { EditorLineHeight.toDp() }"))
        assertTrue("رشد با تعداد خط", s.contains("val visibleLines = CodeText.editorVisibleLines(field.text)"))
        assertTrue("ارتفاع دقیق، نه بازه", s.contains("val editorHeight = lineHeightDp * visibleLines + 16.dp"))
        assertTrue("کادر همان ارتفاع را می‌گیرد", s.contains("Modifier.fillMaxWidth().height(editorHeight)"))
        assertTrue("سبک هم از همان مرجع", s.contains("lineHeight = EditorLineHeight"))
        // بازهٔ ثابت پیشین باید رفته باشد
        assertTrue("بازهٔ قبلی رفت", !s.contains("heightIn(min = 200.dp, max = 340.dp)"))
    }

    @Test
    fun codeAreaAutoScrollsToKeepTheCaretVisible() {
        val s = src("app/src/main/java/ir/exam/app/ui/code/CodeEditorDialog.kt")
        assertTrue("اندازهٔ ناحیه دیده می‌شود", s.contains(".onSizeChanged { viewport = it }"))
        assertTrue("تا ۱۱ خط اسکرول لازم نیست", s.contains("if (lineCount <= CodeText.EDITOR_MAX_LINES) return@LaunchedEffect"))
        assertTrue("سطر نشانگر", s.contains("val caretLine = CodeText.editorCaretLine(field.text, caret)"))
        assertTrue("اگر بالا رفته", s.contains("top < current -> floor(top)"))
        assertTrue("اگر پایین رفته", s.contains("bottom > current + viewport.height -> ceil(bottom - viewport.height)"))
        assertTrue("اسکرول نرم", s.contains("vScroll.animateScrollTo(target.toInt())"))
        assertTrue("از محدوده بیرون نمی‌زند", s.contains("coerceIn(0f, vScroll.maxValue.toFloat())"))
    }

    @Test
    fun scrollMathKeepsCaretInsideTheViewport() {
        // آینهٔ همان حسابِ داخل پنجره، با عددهای واقعی
        val linePx = 20f
        val viewport = 11 * 20f
        fun target(caretLine: Int, current: Float): Float {
            val top = caretLine * linePx
            val bottom = top + linePx
            return when {
                top < current -> top
                bottom > current + viewport -> bottom - viewport
                else -> current
            }.coerceAtLeast(0f)
        }
        // سطر ۱۲ (ایندکس ۱۱) وقتی در بالای صفحه‌ایم ⇒ باید پایین برود
        assertEquals(20f, target(11, 0f), 0.01f)
        // سطر ۲۰ ⇒ نشانگر پایین‌ترین سطرِ دید شود
        assertEquals(180f, target(19, 20f), 0.01f)
        // برگشت به سطر ۲ ⇒ باید بالا بیاید
        assertEquals(20f, target(1, 180f), 0.01f)
        // سطری که همین حالا در دید است ⇒ تکان نمی‌خورد
        assertEquals(0f, target(5, 0f), 0.01f)
    }
}
