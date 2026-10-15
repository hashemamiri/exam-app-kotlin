package ir.exam.app.core.primary

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V308 / ۱۴ — هم‌ترازی برچسب پایه/درس: اپ و سایت هر دو ذخیره، ویرایش و فیلتر بانک را دارند. */
class V308_Site_GradeSubjectTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists() && !File(dir, "settings.gradle").exists()) dir = dir.parentFile
        return dir ?: File("").absoluteFile
    }

    @Test
    fun `app editor and bank filter use grade and subject tags`() {
        val screen = File(root(), "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()
        assertTrue(screen.contains("if (primary) PrimaryTagEditor(question, viewModel)"))
        assertTrue(screen.contains("item.question.grade == tagGrade"))
        assertTrue(screen.contains("item.question.subject == tagSubject"))
        val codec = File(root(), "app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt").readText()
        assertTrue(codec.contains("if (question.grade in 1..6) values[\"grade\"] = JsonPrimitive(question.grade)"))
    }

    @Test
    fun `site builder stores grade and subject and filters bank by them`() {
        val builder = File(root(), "site/src/builder.js").readText()
        assertTrue(builder.contains("q.grade = Math.max(0, Math.min(6, Number(obj.grade) || 0)); q.subject = obj.subject ? String(obj.subject) : '';"))
        assertTrue(builder.contains("if (q.grade >= 1 && q.grade <= 6) v.grade = q.grade; else delete v.grade;"))
        assertTrue(builder.contains("function primaryTagEditor(q, redraw)"))
        assertTrue(builder.contains("(!gs.value || Number(qq.grade) === Number(gs.value)) && (!ss.value || qq.subject === ss.value)"))
    }
}
