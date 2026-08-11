package ir.exam.app.data.repository

import ir.exam.app.data.local.ExamBuilderDraftDao
import ir.exam.app.data.local.ExamBuilderDraftEntity
import ir.exam.app.ui.builder.ExamBuilderState
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExamBuilderDraftStoreTest {
    @Test
    fun `builder draft survives serialization and can be cleared`() = runBlocking {
        val dao = FakeDraftDao()
        val store = ExamBuilderDraftStore(dao)
        val state = ExamBuilderState(
            title = "آزمون نیمه‌کاره",
            subject = "ریاضی",
            questions = listOf(QuestionDraft(type = QuestionType.ESSAY, text = "سؤال", score = 2.0)),
            audienceMode = "classes",
            audienceClasses = setOf("class-1")
        )

        store.save("teacher-1", state)
        val restored = store.load("teacher-1")
        assertEquals(state.title, restored?.title)
        assertEquals(state.questions, restored?.questions)
        assertEquals(setOf("class-1"), restored?.audienceClasses)

        store.clear("teacher-1")
        assertNull(store.load("teacher-1"))
    }
}

private class FakeDraftDao : ExamBuilderDraftDao {
    private var value: ExamBuilderDraftEntity? = null
    override suspend fun get(userId: String): ExamBuilderDraftEntity? = value?.takeIf { it.ownerUserId == userId }
    override suspend fun upsert(item: ExamBuilderDraftEntity) { value = item }
    override suspend fun delete(userId: String) { if (value?.ownerUserId == userId) value = null }
}
