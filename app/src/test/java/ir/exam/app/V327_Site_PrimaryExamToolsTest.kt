package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V327 — پوشش ابزارهای هفت‌گانهٔ آزمون دبستان در وب و رندر بومی. */
class V327_Site_PrimaryExamToolsTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun allSevenPrimaryToolsAreRegisteredOnBothRenderers() {
        val ids = listOf("axpaintgrid", "axletterforms", "axsyllablechart", "axmagnet", "axsenses", "axseedgrow", "axbodyparts")
        val web = File(root(), "app/src/main/assets/print/web/graph_fig.js").readText()
        val native = File(root(), "app/src/main/java/ir/exam/app/core/figure/AdvancedMathSvgRenderer.kt").readText()
        assertTrue("axpaintgrid" in web); assertTrue("axpaintgrid" in native)
        assertTrue("axletterforms" in web); assertTrue("axletterforms" in native)
        assertTrue("axsyllablechart" in web); assertTrue("axsyllablechart" in native)
        assertTrue("axmagnet" in web); assertTrue("axmagnet" in native)
        assertTrue("axsenses" in web); assertTrue("axsenses" in native)
        assertTrue("axseedgrow" in web); assertTrue("axseedgrow" in native)
        assertTrue("axbodyparts" in web); assertTrue("axbodyparts" in native)
    }

    @Test
    fun studentExamSupportsPaintPassageDictationAndCamera() {
        val models = File(root(), "app/src/main/java/ir/exam/app/domain/model/ExamModels.kt").readText()
        val codec = File(root(), "app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt").readText()
        val student = File(root(), "app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt").readText()
        assertTrue("پاساژ در مدل سؤال نیست", "val passage: String" in models)
        assertTrue("حالت رنگ‌آمیزی در کدک نیست", "question.mode == \"paint\"" in codec)
        assertTrue("تنظیم مکث املا کدگذاری نمی‌شود", "dict_gap" in codec)
        assertTrue("خانه‌های رنگی تعاملی نیستند", "PrimaryPaintAnswer" in student)
        assertTrue("عکس مستقیم پاسخ در صفحهٔ آزمون نیست", "TakePicturePreview()" in student)
        assertTrue("متن روخوانی نمایش داده نمی‌شود", "متن روخوانی" in student)
    }
}
