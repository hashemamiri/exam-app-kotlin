package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V335.19 — نگهبان رگرسیون: پنجرهٔ «راهنمای تخته» باید برای هر ابزار و هر ابزار هندسی یک سطر راهنما
 * داشته باشد. بازنویسی تخته در نسخهٔ قبل بخش «ابزارهای هندسی» راهنما را حذف کرده بود و تست قدیمی
 * `V137_6` (که پین‌هایش با الگوی رشته‌ای نوشته شده و ابزار بررسی خودکار پین‌ها آن‌ها را نمی‌بیند)
 * در CI شکست. این تست همهٔ سطرها را با متن صریح می‌پاید تا این افت دوباره تکرار نشود.
 */
class V335_19_WhiteboardHelpParityTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt").isFile) {
            dir = dir.parentFile
        }
        return dir ?: File("")
    }

    private fun src(path: String): String = File(root(), path).readText()

    @Test
    fun helpDialogCoversEveryGeometricInstrumentAgain() {
        val board = src("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        assertTrue("HelpRow(BoardInstrument.NONE.icon" in board)
        assertTrue("HelpRow(BoardInstrument.RULER.icon" in board)
        assertTrue("HelpRow(BoardInstrument.SETSQUARE.icon" in board)
        assertTrue("HelpRow(BoardInstrument.PROTRACTOR.icon" in board)
        assertTrue("HelpRow(BoardInstrument.COMPASS.icon" in board)
        assertTrue("title = { Text(\"راهنمای تخته\") }" in board)
        assertTrue("Text(\"ابزارهای هندسی\", style = MaterialTheme.typography.titleSmall" in board)
    }

    @Test
    fun helpDialogCoversEveryBoardToolIncludingTheNewOnes() {
        val board = src("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        listOf("PEN", "HIGHLIGHT", "ERASER", "OBJ_ERASER", "SELECT", "LINE", "ARROW", "RECT", "CIRCLE", "TRIANGLE", "TEXT")
            .forEach { tool -> assertTrue(tool, "HelpRow(BoardTool.$tool.icon" in board) }
        // ابزارهای تازهٔ همین نسخه‌ها هم باید سطر راهنما داشته باشند (به‌صورت متن صریح).
        assertTrue("HelpRow(BoardTool.ARROW2.icon" in board)
        assertTrue("HelpRow(BoardTool.RTRIANGLE.icon" in board)
        assertTrue("HelpRow(BoardTool.DIAMOND.icon" in board)
        assertTrue("HelpRow(BoardTool.AXES.icon" in board)
        assertTrue("HelpRow(Icons.Outlined.AddBox" in board)
        // بخش انتخاب و ویرایش موارد درج‌شده در راهنما
        assertTrue("Text(\"انتخاب و ویرایش موارد درج‌شده\", style = MaterialTheme.typography.titleSmall" in board)
        assertTrue("HelpRow(Icons.Outlined.ContentCopy" in board)
        assertTrue("HelpRow(Icons.Outlined.CropSquare" in board)
    }
}
