package ir.exam.app.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import ir.exam.app.data.dto.ExamWriteDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.ui.builder.ExamBuilderState
import kotlinx.serialization.json.Json
import java.util.UUID

class SupabaseExamBuilderRepository {
    suspend fun create(state: ExamBuilderState): Result<String> = runCatching {
        val teacherId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: error("نشست معلم پیدا نشد. دوباره وارد شوید.")
        require(state.title.trim().isNotEmpty()) { "عنوان آزمون را وارد کنید." }
        require(state.questions.isNotEmpty()) { "حداقل یک سؤال اضافه کنید." }
        val code = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val dto = ExamWriteDto(
            id = UUID.randomUUID().toString(),
            teacherId = teacherId,
            title = state.title.trim(),
            subject = state.subject.trim(),
            duration = state.durationMinutes.toIntOrNull() ?: 0,
            code = code,
            totalScore = state.questions.sumOf { it.score },
            isOpen = false,
            shuffleQuestions = false,
            questions = Json.encodeToJsonElement(state.questions)
        )
        SupabaseProvider.client.from("exams").insert(dto)
        code
    }
}
