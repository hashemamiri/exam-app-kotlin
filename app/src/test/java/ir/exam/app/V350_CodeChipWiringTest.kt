package ir.exam.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V350 — سیم‌کشی «کد به‌عنوان شیء درون‌متنی» در اپ و سایت.
 *
 * این آزمون خودِ سورس را می‌سنجد (الگوی بقیهٔ آزمون‌های این پروژه): ابزار کد
 * باید در همهٔ گونه‌های سؤال و در گزینه‌ها باشد، لمس اول حذف را در دسترس
 * بگذارد و لمس دوم ویرایش را باز کند.
 */
class V350_CodeChipWiringTest {

    /**
     * V350.1 — دایرکتوری کاریِ تست واحد، ریشهٔ مخزن نیست (معمولاً پوشهٔ app است).
     * مثل V348_Site_CodeToolTest بالا می‌رویم تا settings.gradle.kts پیدا شود؛
     * مسیر نسبیِ تنها، فایل را پیدا نمی‌کرد و همهٔ این آزمون‌ها را می‌شکست.
     */
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

    // ---------- اپ: کادر متن سؤال ----------

    @Test
    fun questionFieldExposesCodeInsertAndEdit() {
        val s = src("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt")
        assertTrue("درج کد", s.contains("onInsertCode: (insertOffset: Int) -> Unit"))
        assertTrue("ویرایش کد", s.contains("onEditCodeToken: (occurrenceIndex: Int, lang: String, code: String, start: Int, end: Int) -> Unit"))
        assertTrue("شاخهٔ رندر کد", s.contains("is RichSegment.Code -> {"))
        assertTrue("کادر کد نمایش داده می‌شود", s.contains("CodeBlockView("))
        assertTrue("دکمهٔ نوار ابزار", s.contains("NativeCodeToolButton { onInsertCode(insertAtOffset) }"))
    }

    @Test
    fun questionCodeChipSelectsThenEdits() {
        val s = src("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt")
        val block = s.substring(s.indexOf("is RichSegment.Code -> {"))
        assertTrue("لمس اول انتخاب", block.contains("selectedPartIndex = index"))
        assertTrue("لمس دوم ویرایش", block.contains("currentEditCode("))
        assertTrue("ضربدر حذف", block.contains("TokenCloseButton(Modifier.align(Alignment.TopEnd))"))
        assertTrue("حذف از متن خام", block.contains("CodeBlockCodec.remove(currentText, codeOcc)"))
    }

    // ---------- اپ: گزینه‌ها و جورکردنی ----------

    @Test
    fun inlineEditorHasCodeChipWithDeleteAndEdit() {
        val s = src("app/src/main/java/ir/exam/app/ui/math/InlineMathTextEditor.kt")
        assertTrue("پارامتر درج", s.contains("onInsertCode: () -> Unit"))
        assertTrue("پارامتر ویرایش", s.contains("onEditCode: (occurrenceIndex: Int, lang: String, code: String) -> Unit"))
        assertTrue("پارامتر حذف", s.contains("onDeleteCode: (occurrenceIndex: Int) -> Unit"))
        assertTrue("تراشهٔ کد", s.contains("is RichSegment.Code -> CodeChip("))
        assertTrue("تعریف تراشه", s.contains("private fun CodeChip("))
        assertTrue("دکمهٔ </>", s.contains("ToolbarCodeButton(onInsertCode)"))
    }

    @Test
    fun codeChipUsesSelectThenEditLikeOtherTools() {
        val s = src("app/src/main/java/ir/exam/app/ui/math/InlineMathTextEditor.kt")
        val chip = s.substring(s.indexOf("private fun CodeChip("))
        assertTrue("وضعیت انتخاب", chip.contains("var selected by remember"))
        assertTrue("لمس دوم ویرایش", chip.contains("if (selected) { selected = false; onEdit() } else { selected = true }"))
        assertTrue("حذف فقط پس از انتخاب", chip.contains("if (selected) {"))
        assertTrue("ضربدر حذف", chip.contains("contentDescription = \"حذف کد\""))
        assertTrue("راهنمای لمس دوم", chip.contains("برای ویرایش دوباره لمس کنید"))
    }

    @Test
    fun optionInsertMenuHasCodeTool() {
        val s = src("app/src/main/java/ir/exam/app/ui/builder/OptionInsertTools.kt")
        assertTrue("عضو تازهٔ منو", s.contains("CODE(\"کد\")"))
        assertTrue("آیکن منو", s.contains("OptionInsertTool.CODE -> QuestionToolIcons.Code"))
        val icons = src("app/src/main/java/ir/exam/app/ui/math/QuestionToolIcons.kt")
        assertTrue("آیکن </> ساخته شده", icons.contains("val Code: ImageVector by lazy"))
        assertTrue("همان مسیر SVG سایت", icons.contains("moveTo(8.6f, 8.2f); lineTo(4.4f, 12f); lineTo(8.6f, 15.8f)"))
    }

    @Test
    fun builderWiresQuestionOptionsAndMatching() {
        val s = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("هدف کد", s.contains("private data class CodeTarget("))
        assertTrue("وضعیت هدف", s.contains("var codeTarget by remember(question.id)"))
        assertTrue("متن سؤال", s.contains("onInsertCode = { codeTarget = CodeTarget(\"question\") }"))
        assertTrue("ویر متن سؤال", s.contains("onEditCodeToken = { occurrence, lang, code, _, _ ->"))
        assertTrue("گزینه", s.contains("onInsertCode = { codeTarget = CodeTarget(\"option\", index) }"))
        assertTrue("حذف گزینه", s.contains("CodeBlockCodec.remove(option, occ)"))
        assertTrue("منوی گزینه", s.contains("codeTarget = CodeTarget(ref.field, ref.index)"))
        assertTrue("جورکردنی", s.contains("codeTarget = CodeTarget("))
        assertTrue("پنجرهٔ مشترک", s.contains("FieldCodeEditor("))
    }

    @Test
    fun matchingPairsGetCodeToo() {
        val s = src("app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt")
        assertTrue("پارامتر ویرایش کد", s.contains("onCodeEdit: (side: String, index: Int, occurrence: Int?, lang: String, code: String) -> Unit"))
        assertEquals("هر دو طرف چپ و راست", 2, Regex("onInsertCode = \\{ onCodeEdit\\(").findAll(s).count())
        assertEquals(2, Regex("onDeleteCode = \\{ occurrence ->").findAll(s).count())
    }

    @Test
    fun fieldEditorInsertsOrReplacesInPlace() {
        val s = src("app/src/main/java/ir/exam/app/ui/builder/FieldCodeEditor.kt")
        assertTrue("درج تازه", s.contains("CodeBlockCodec.insertAt(source, code, pickedLang, source.length)"))
        assertTrue("ویرایش در همان جا", s.contains("CodeBlockCodec.replace(source, occ, code, pickedLang)"))
        assertTrue("انتخاب زبان برای درج تازه", s.contains("CodeLanguagePickerDialog("))
        assertTrue("ویرایش مستقیم زبان ذخیره‌شده", s.contains("mutableStateOf(!editing && initialLang.isBlank())"))
    }

    // ---------- اپ: نمایش دانش‌آموز، چاپ، خواندن ----------

    @Test
    fun studentViewRendersCodeInsteadOfRawFences() {
        val s = src("app/src/main/java/ir/exam/app/ui/math/NativeMathText.kt")
        assertTrue("تشخیص کد", s.contains("val hasCode = flat.any { it is RichSegment.Code }"))
        assertTrue("مسیر رندر فعال می‌شود", s.contains("if (!hasMath && !hasFigure && !hasCode) {"))
        assertTrue("کادر کد", s.contains("codeSeg.code"))
        assertTrue("سطر مستقل", s.contains("is RichSegment.Code -> Unit"))
    }

    @Test
    fun printAndSpeechHandleCode() {
        val print = src("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt")
        assertTrue("چاپ بلوک کد", print.contains("is RichSegment.Code -> {"))
        assertTrue("بی‌بک‌تیک روی کاغذ", print.contains("codeLabel + \"\\n\" + rich.code"))
        val speech = src("app/src/main/java/ir/exam/app/core/speech/ReadAloudText.kt")
        assertTrue("کد خوانده نمی‌شود", speech.contains("is RichSegment.Code -> out.append(\" بلوک کد \")"))
    }

    // ---------- سایت: همان رفتار ----------

    @Test
    fun siteTokenizerTreatsCodeAsAnObject() {
        val s = src("site/src/builder.js")
        assertTrue("توکن کد اول می‌آید", s.contains("var TOKEN_RE = /```([A-Za-z0-9_+#.\\-]*)[ \\t]*\\r?\\n([\\s\\S]*?)```|"))
        assertTrue("برچسب تراشه", s.contains("function codeChipLabel(langId)"))
        assertTrue("ساخت تراشهٔ کد", s.contains("chip(m[0], codeChipLabel(m[1]), 'code')"))
        assertTrue("کادر رنگی روی دسکتاپ", s.contains("c.classList.add('code-live');"))
    }

    @Test
    fun siteChipEditsOnSecondClickAndDeletesOnCross() {
        val s = src("site/src/builder.js")
        assertTrue("ویرایش تراشهٔ کد", s.contains("else if (c.classList.contains('code') && window.SiteCode) {"))
        assertTrue("ذخیره در همان جای متن", s.contains("ta.value = raw.slice(0, idx) + next + raw.slice(idx + tok.length);"))
        assertTrue("ضربدر حذف روی گوشی هم", s.contains("if ((WYSIWYG || c.classList.contains('code')) && !c.querySelector('.b-chip-x'))"))
    }

    @Test
    fun siteInsertMenuAndToolbarShareOneImplementation() {
        val s = src("site/src/builder.js")
        assertTrue("تابع مشترک", s.contains("function insertCode(ta, o) {"))
        assertTrue("منوی گزینه", s.contains("item('code', 'کد', 'is-code', function () { insertCode(ta, {title: 'کد در ' + (o.label || 'این کادر')}); }),"))
        assertTrue("نوار ابزار سؤال", s.contains("insertCode(ta, {title: 'کد در متن سؤال', doneLabel: 'افزودن کد به سؤال'"))
    }

    @Test
    fun siteStylesTheInTextCodeBox() {
        val s = src("site/src/site.css")
        assertTrue("تراشهٔ کد", s.contains(".b-chip.code{background:#1B2035"))
        assertTrue("کادر زندهٔ کد", s.contains(".b-chip.code-live .cx-block{"))
        assertTrue("چپ‌به‌راست", s.contains(".b-chip.code-live{background:transparent;color:inherit;padding:0;white-space:normal;direction:ltr"))
    }

    @Test
    fun siteLineCountIsPersian() {
        val s = src("site/src/code.js")
        assertTrue("شمار خط فارسی", s.contains("faNum(lines.length) + ' خط</span>'"))
        assertTrue("هلپر رقم فارسی", s.contains("function faNum(n) {"))
        assertTrue("عبارت انگلیسی رفت", !s.contains("' lines'"))
    }

    // ---------- نگهبان‌های تازه ----------

    @Test
    fun newGuardsArePresent() {
        assertTrue(File(root(), "scripts/check_missing_imports.py").exists())
        assertTrue(File(root(), "scripts/check_sealed_when.py").exists())
        assertTrue(File(root(), "scripts/check_code_chips.py").exists())
        assertTrue(File(root(), "scripts/check_test_paths.py").exists())
    }
}
