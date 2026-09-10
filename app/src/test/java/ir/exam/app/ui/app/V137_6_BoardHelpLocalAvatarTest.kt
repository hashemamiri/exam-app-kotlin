package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V137.6 — راهنمای تخته، شماره‌گذاری گونیا، عکس پروفایل فقط محلی. */
class V137_6_BoardHelpLocalAvatarTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `whiteboard has help dialog covering every icon and numbered set-square`() {
        val board = source("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        assertTrue("Icons.AutoMirrored.Outlined.HelpOutline" in board)
        assertTrue("private fun HelpRow(" in board)
        assertTrue("title = { Text(\"راهنمای تخته\") }" in board)
        listOf("PEN", "HIGHLIGHT", "ERASER", "OBJ_ERASER", "SELECT", "LINE", "ARROW", "RECT", "CIRCLE", "TRIANGLE", "TEXT")
            .forEach { assertTrue(it, "HelpRow(BoardTool.$it.icon" in board) }
        listOf("NONE", "RULER", "SETSQUARE", "PROTRACTOR", "COMPASS")
            .forEach { assertTrue(it, "HelpRow(BoardInstrument.$it.icon" in board) }
        assertTrue("if (i % 10 == 0 && i > 0) canvas.drawText(FigureDigits.apply((i / 10).toString()), x, -len - 3f * density, txt)" in board)
        assertTrue("val txtL = Paint(txt).apply { textAlign = Paint.Align.LEFT }" in board)
    }

    @Test
    fun `avatars are local only and teacher visibility switch is gone`() {
        assertTrue(File(root(), "app/src/main/java/ir/exam/app/data/local/LocalAvatarStore.kt").isFile)
        val vm = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt")
        assertTrue("LocalAvatarStore.save(appContext, profile.id, uri)" in vm)
        assertFalse("repository.uploadAvatar(uri)" in vm)
        assertFalse("fun setAvatarPublic" in vm)
        val screen = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        assertFalse("نمایش عکس به دانش‌آموزان" in screen)
        assertFalse("onAvatarPublic" in screen)
        assertTrue("fun ProfileAvatar(url: String?, name: String, sizeDp: Int, userId: String? = null, version: Int = 0)" in screen)
        assertTrue("LocalAvatarStore.path(ctx, userId)" in screen)
        assertTrue("userId = user.id" in source("app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt"))
        assertTrue("userId = user.id" in source("app/src/main/java/ir/exam/app/ui/app/TabletDesktopShell.kt"))
    }
}
