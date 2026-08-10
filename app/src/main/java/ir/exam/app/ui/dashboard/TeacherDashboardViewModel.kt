package ir.exam.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.repository.SupabaseTeacherDashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherDashboardState(
    val loading: Boolean = true,
    val exams: List<ExamDashboardDto> = emptyList(),
    val error: String? = null
)

class TeacherDashboardViewModel(
    private val repository: SupabaseTeacherDashboardRepository = SupabaseTeacherDashboardRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherDashboardState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        repository.getMyExams()
            .onSuccess { exams -> _state.update { it.copy(loading = false, exams = exams) } }
            .onFailure { _state.update { it.copy(loading = false, error = "دریافت آزمون‌ها ناموفق بود.") } }
    }
}
