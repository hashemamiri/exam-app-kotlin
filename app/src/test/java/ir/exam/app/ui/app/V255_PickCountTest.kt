package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V255 — «پیک تصادفی»: N سؤال از کل برای هر دانش‌آموز؛ نمره از جمع بارم سؤال‌های نمایش‌داده‌شده. */
class V255_PickCountTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `server migration picks subset per attempt and remaps compact responses`() {
        val sql = source("supabase/migrations/20261010_native_pick_count_v255.sql")
        assertTrue("alter table public.exams add column if not exists pick_count integer not null default 0;" in sql)
        assertTrue("alter table public.exam_attempts add column if not exists shown_q jsonb;" in sql)
        assertTrue("where v_shown is null or v_shown @> to_jsonb((ord - 1)::int);" in sql)
        assertTrue("'total_score', case when v_shown is null then v_exam.total_score else v_total end," in sql)
        assertTrue("v_full := v_full || jsonb_build_array(coalesce(p_responses -> v_pos, '\"\"'::jsonb));" in sql)
        assertTrue("v_meta := v_meta || jsonb_build_object('shown_q', v_shown, 'max_score', v_max, 'pick_count', coalesce(v_exam.pick_count,0));" in sql)
        assertTrue("'total_score', coalesce(public.native_answer_max_score_v255(v_answer.meta), v_exam.total_score, 0)," in sql)
        assertTrue("pick_count = greatest(0, coalesce(nullif(btrim(coalesce(v_payload->>'pick_count','')), '')::integer, 0))" in sql)
    }

    @Test
    fun `app builder stores and sends pick_count`() {
        assertTrue("val pickCount: String = \"\"," in source("app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt"))
        assertTrue("fun setPickCount(value: String)" in source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt"))
        assertTrue("label = { Text(\"نمایش تصادفی چند سؤال از کل؟ (خالی = همه)\") }," in source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt"))
        val repo = source("app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt")
        assertTrue("put(\"pick_count\", state.pickCount.toIntOrNull()?.coerceIn(0, 9999) ?: 0)" in repo)
        assertTrue("pickCount = exam.pickCount.takeIf { it > 0 }?.toString().orEmpty()," in repo)
        assertTrue("@SerialName(\"pick_count\") val pickCount: Int = 0" in source("app/src/main/java/ir/exam/app/data/dto/ExamDetailDto.kt"))
        assertTrue("put(\"pick_count\", source.pickCount.coerceIn(0, 9999))" in source("app/src/main/java/ir/exam/app/data/repository/ExamPackageCodec.kt"))
    }

    @Test
    fun `app grading and reports respect shown questions and max score`() {
        assertTrue("val meta: JsonElement? = null" in source("app/src/main/java/ir/exam/app/data/dto/GradingDtos.kt"))
        val m = source("app/src/main/java/ir/exam/app/domain/model/GradingModels.kt")
        assertTrue("fun isShown(index: Int): Boolean = shownQuestions == null || index in shownQuestions" in m)
        assertTrue("if (!submission.isShown(index)) {" in source("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt"))
        assertTrue("val max = attempts.maxByOrNull { it.totalGrade }?.maxScore ?: exam.totalScore" in source("app/src/main/java/ir/exam/app/ui/reports/ReportsViewModel.kt"))
    }

    @Test
    fun `site builder grading and reports support pick_count`() {
        val b = source("site/src/builder.js")
        assertTrue("inp('نمایش تصادفی چند سؤال از کل؟ (خالی = همه)', state.pickCount || ''" in b)
        assertTrue("pick_count: Math.max(0, Math.min(9999, parseInt(state.pickCount, 10) || 0))," in b)
        assertTrue("st.pickCount = exam.pick_count ? String(exam.pick_count) : '';" in b)
        val a = source("site/src/admin.js")
        assertTrue("if (shownQ && shownQ.indexOf(i) < 0) {" in a && "' از ' + fa(S.fmtScore(maxScore)); }" in a)
        assertTrue("function answerMax(a, examTotal)" in source("site/src/extras.js"))
        assertTrue("function mergeAnswerMax(grades, answers)" in source("site/src/app.js"))
    }
}
