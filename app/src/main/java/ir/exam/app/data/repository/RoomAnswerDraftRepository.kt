package ir.exam.app.data.repository

import ir.exam.app.data.local.AnswerDraftDao
import ir.exam.app.data.local.AnswerDraftEntity
import ir.exam.app.domain.model.StudentDraft
import ir.exam.app.domain.repository.AnswerDraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * V252 — پیش‌نویس پاسخ‌ها به حساب دانش‌آموز گره خورد.
 * ریشه: جدول answer_drafts فقط با examId کلید می‌خورد و در خروج از حساب پاک نمی‌شد؛ روی تبلت مشترک،
 * دانش‌آموز بعدی با همان کد آزمون پاسخ‌های نفر قبلی را از پیش پرشده می‌دید (و می‌توانست همان را بفرستد).
 * حالا کلید سطر «<کاربر>|<آزمون>» است (بدون تغییر اسکیمای Room). سطرهای قدیمیِ بدون کاربر فقط یک بار برای
 * اولین بارگذاری همان آزمون به کلید جدید منتقل و حذف می‌شوند.
 */
class RoomAnswerDraftRepository(private val dao: AnswerDraftDao, private val ownerUserId: String = "") : AnswerDraftRepository {
    private fun key(examId: String): String = draftKey(ownerUserId, examId)

    override fun observe(examId: String): Flow<StudentDraft> =
        dao.observe(key(examId)).map { entity ->
            entity?.answersJson?.let(StudentDraftJsonCodec::decode) ?: StudentDraft()
        }

    override suspend fun load(examId: String): StudentDraft {
        dao.find(key(examId))?.let { return StudentDraftJsonCodec.decode(it.answersJson) }
        if (ownerUserId.isBlank()) return StudentDraft()
        // سطر قدیمی (پیش از V252) → انتقال به کلید کاربر و حذف سطر بدون کاربر
        val legacy = dao.find(examId) ?: return StudentDraft()
        dao.upsert(legacy.copy(examId = key(examId)))
        dao.delete(examId)
        return StudentDraftJsonCodec.decode(legacy.answersJson)
    }

    override suspend fun save(examId: String, draft: StudentDraft) {
        dao.upsert(
            AnswerDraftEntity(
                examId = key(examId),
                answersJson = StudentDraftJsonCodec.encode(draft),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clear(examId: String) {
        dao.delete(key(examId))
        if (ownerUserId.isNotBlank()) dao.delete(examId)
    }

    companion object {
        fun draftKey(ownerUserId: String, examId: String): String =
            if (ownerUserId.isBlank()) examId else "$ownerUserId|$examId"
    }
}
