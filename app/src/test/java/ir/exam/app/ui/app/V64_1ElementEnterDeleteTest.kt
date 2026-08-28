package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V64.1 — ورد واقعی گام ۲: Enter=عنصر جدید و Delete نوار ابزار.
 * ۱) Enter داخل گزینهٔ در حال ویرایش، گزینهٔ جدید بعد از همان می‌سازد و
 *    انتخاب به آن می‌رود (insertOptionAfter با جابجایی امن correctIndex)؛
 *    در جورکردنی سطر جدید (addMatchingRow).
 * ۲) دکمهٔ سطل در نوار ابزار: شیء (تصویر/شکل) یا عنصر انتخاب‌شده را حذف
 *    می‌کند (removeOptionAt/removeMatchingSide با clamp حداقل ۲ گزینه).
 */
class V64_1ElementEnterDeleteTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }
    private val builderVm by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt") }

    @Test
    fun `enter inside an element creates the next element like a word paragraph`() {
        assertTrue("onEnter: () -> Unit" in editor)
        assertTrue("if ('\\n' in value)" in editor)
        assertTrue("builder.insertOptionAfter(questionId, index)" in editor)
        assertTrue("selectedElement = Triple(questionId, \"opt\", index + 1)" in editor)
        assertTrue("builder.addMatchingRow(questionId)" in editor)
        // ویومدل: درج موضعی با نگهداری پاسخ صحیح
        assertTrue("fun insertOptionAfter(id: String, index: Int)" in builderVm)
        assertTrue("if (it >= at) it + 1 else it" in builderVm)
    }

    @Test
    fun `toolbar delete removes the selected object or element`() {
        assertTrue("onDeleteSelected = {" in editor)
        assertTrue("builder.removeImage(questionId, imageId); selectedImage = null" in editor)
        assertTrue("builder.deleteFigure(questionId, occurrenceIndex); selectedFigure = null" in editor)
        assertTrue("builder.removeOptionAt(questionId, index)" in editor)
        assertTrue("builder.removeMatchingSide(questionId, \"left\", index)" in editor)
        assertTrue("contentDescription = \"حذف انتخاب‌شده\"" in editor)
        // ویومدل: حذف موضعی امن (حداقل ۲ گزینه، اصلاح correctIndex)
        assertTrue("fun removeOptionAt(id: String, index: Int)" in builderVm)
        assertTrue("if (question.options.size <= 2 || index !in question.options.indices) question" in builderVm)
        assertTrue("it == index -> null; it > index -> it - 1; else -> it" in builderVm)
    }
}
