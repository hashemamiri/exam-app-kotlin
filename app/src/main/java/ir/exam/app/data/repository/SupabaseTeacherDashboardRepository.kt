package ir.exam.app.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.remote.SupabaseProvider

class SupabaseTeacherDashboardRepository {
    suspend fun getMyExams(): Result<List<ExamDashboardDto>> = runCatching {
        val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: error("نشست ورود پیدا نشد. دوباره وارد شوید.")
        SupabaseProvider.client.from("exams").select {
            filter { eq("teacher_id", userId) }
        }.decodeList<ExamDashboardDto>()
    }
}
