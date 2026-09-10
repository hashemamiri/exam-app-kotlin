package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V137.3 — بازخورد کاربر روی V137/V137.2: تختهٔ معلم، اسکرول بدون پرش، کادر دقیق شیء،
 * حرکت کارت‌ها برای ۱۰ کارت، رسیدن تصویر تخته به معلم.
 */
class V137_3_FeedbackTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `teacher builder has whiteboard icon beside audio icon`() {
        val media = source("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt")
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("onOpenWhiteboard: (() -> Unit)? = null" in media)
        assertTrue("Icons.Outlined.Draw" in media)
        assertTrue(media.indexOf("Icons.Outlined.MusicNote,") < media.indexOf("Icons.Outlined.Draw,"))
        assertTrue("onOpenWhiteboard = { whiteboardOpen = true }" in builder)
        assertTrue("ir.exam.app.ui.student.StudentWhiteboardDialog(" in builder)
        assertTrue("questionId = \"teacher-\${question.id}\"" in builder)
        assertTrue("viewModel.addImages(question.id, uris)" in builder)
    }

    @Test
    fun `whiteboard compass is drawn as a real compass`() {
        val board = source("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        assertTrue("fun leg(tipX: Float, tipY: Float, topHalf: Float, tipHalf: Float)" in board)
        assertTrue("0xFFFACC15.toInt()" in board) // بدنهٔ زرد مداد
        assertTrue("knobH" in board) // دستگیرهٔ شیاردار روی لولا
        assertFalse("canvas.drawText(\"r\", apexX" in board)
    }

    @Test
    fun `card scroll uses smooth follower without final jump`() {
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("delta * 0.22f" in builder)
        assertTrue("repeat(75)" in builder)
        assertFalse("listState.animateScrollBy(" in builder)
        assertFalse("repeat(36)" in builder)
    }

    @Test
    fun `preview box is tightened to svg content and backgrounds removed`() {
        val js = source("app/src/main/assets/print/web/mainscript.js")
        val graph = source("app/src/main/assets/print/web/graph_fig.js")
        val css = source("app/src/main/assets/print/web/webhost.css")
        assertTrue("function tightenFigSvgs(fig)" in js)
        assertTrue("svg.getBBox()" in js)
        assertTrue(js.indexOf("tightenFigSvgs(fig);") > js.indexOf("function figNaturalSize(fig)"))
        assertTrue("data:image/svg+xml;base64," in graph)
        assertTrue("return xml.slice(xml.indexOf('<svg'))" in graph)
        assertTrue(".question-main-td .qmf-fig svg rect[fill=\"#fbfcfe\"]{fill:none !important;}" in css)
    }

    @Test
    fun `card fly animation is visible with ten cards`() {
        val cards = source("app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt")
        assertTrue("var flying by remember { mutableStateOf(false) }" in cards)
        assertTrue("val isFlying = flying && index == returningIndex" in cards)
        assertTrue("if (relative <= 2 || isFlying) {" in cards)
        assertTrue("val visualRelative = if (isFlying) 0 else relative" in cards)
        assertTrue("if (leavingStaysVisible) {" in cards)
        assertTrue("enterProgress.animateTo(1f" in cards)
        assertFalse("if (cards.size <= 3) {" in cards)
    }

    @Test
    fun `whiteboard answer images reach teacher`() {
        val vm = source("app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt")
        val grading = source("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt")
        assertTrue("uris.all { it.contains(\"/whiteboard/\") }" in vm)
        assertTrue("(fromWhiteboard || exam.questionPresentation[questionId]?.allowAnswerGraph == true)" in vm)
        assertTrue("fun GradingSubmission.answerImagesFor(questionId: String, index: Int)" in grading)
        assertTrue(grading.split("submission.answerImagesFor(question.id, index)").size - 1 == 2)
        assertFalse("submission.responseImages[question.id].orEmpty()" in grading)
    }
}
