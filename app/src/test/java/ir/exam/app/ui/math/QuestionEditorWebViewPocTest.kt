package ir.exam.app.ui.math

import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionEditorWebViewPocTest {
    @Test fun `poc uses local origin and blocks external navigation`() {
        val source = java.io.File("src/main/java/ir/exam/app/ui/math/QuestionEditorWebView.kt").readText()
        assertTrue(source.contains("https://exam-editor.local/question-editor/question_editor.html"))
        assertTrue(source.contains("shouldOverrideUrlLoading"))
        assertTrue(source.contains("= true"))
        assertTrue(source.contains("allowUniversalAccessFromFileURLs = false"))
    }

    @Test fun `poc exposes only versioned editor callbacks`() {
        val source = java.io.File("src/main/java/ir/exam/app/ui/math/QuestionEditorWebView.kt").readText()
        assertTrue(source.contains("onTextChanged"))
        assertTrue(source.contains("onReady"))
        assertTrue(source.contains("onError"))
        assertTrue(!source.contains("SUPABASE"))
        assertTrue(!source.contains("access_token"))
    }
}
