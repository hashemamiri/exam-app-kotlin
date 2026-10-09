package ir.exam.app.core.push

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * V232.6 — مقصد ناوبریِ ضربه روی اعلان. MainActivity از extras اینتنت (push_page/push_exam_id) پر می‌کند و
 * ExamApp پس از ورود کاربر آن را مصرف می‌کند (یک‌بار).
 */
data class PushTarget(val page: String, val examId: String?)

object PushNavigation {
    private val _pending = MutableStateFlow<PushTarget?>(null)
    val pending: StateFlow<PushTarget?> = _pending

    /** اگر اینتنت از اعلان آمده باشد، مقصد ثبت و extras پاک می‌شوند تا با چرخش صفحه دوباره اجرا نشود. */
    fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        val page = (intent.getStringExtra("push_page") ?: intent.getStringExtra("page"))?.takeIf { it.isNotBlank() } ?: return
        val examId = (intent.getStringExtra("push_exam_id") ?: intent.getStringExtra("exam_id"))?.takeIf { it.isNotBlank() }
        intent.removeExtra("push_page"); intent.removeExtra("push_exam_id"); intent.removeExtra("page"); intent.removeExtra("exam_id")
        _pending.value = PushTarget(page, examId)
    }

    fun clear() { _pending.value = null }
}
