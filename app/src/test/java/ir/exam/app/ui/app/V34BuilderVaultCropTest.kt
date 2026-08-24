package ir.exam.app.ui.app

import ir.exam.app.ui.image.CropEdgeKind
import ir.exam.app.ui.image.CropGeometry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** رگرسیون V34: ترتیب ابزارها، thumbnail، Vault رمز و gesture برش. */
class V34BuilderVaultCropTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `reorder is immediately after formula for multiple choice and matching`() {
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val multiple = builder.substringAfter("QuestionType.MULTIPLE_CHOICE ->")
            .substringBefore("QuestionType.TRUE_FALSE ->")
        assertTrue(multiple.indexOf("Icons.Outlined.Functions") < multiple.indexOf("ReorderDragButton("))
        assertTrue(multiple.indexOf("ReorderDragButton(") < multiple.indexOf("SingleImagePicker("))

        val tools = source("app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt")
            .substringAfter("private fun MatchingItemTools(")
            .substringBefore("fun MatchingQuestionEditor(")
        assertTrue(tools.indexOf("Icons.Outlined.Functions") < tools.indexOf("ReorderDragButton("))
        assertTrue(tools.indexOf("ReorderDragButton(") < tools.indexOf("SingleImagePicker("))
    }

    @Test
    fun `question image edit and delete controls are beside the thumbnail`() {
        val media = source("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt")
        val thumbnail = media.substringAfter("private fun CompactImageThumbnail(")
        assertTrue("Row(" in thumbnail)
        assertTrue("Modifier.size(30.dp).clickable(onClick = onView)" in thumbnail)
        assertTrue("IconButton(onClick = onEdit, modifier = Modifier.size(24.dp))" in thumbnail)
        assertTrue("Modifier.size(17.dp).clickable(onClick = onRemove)" in thumbnail)
    }

    @Test
    fun `outward drag grows every crop edge in its own direction`() {
        assertEquals(40f, CropGeometry.resizeDeltaForEdge(CropEdgeKind.LEFT, -40f), 0f)
        assertEquals(40f, CropGeometry.resizeDeltaForEdge(CropEdgeKind.RIGHT, 40f), 0f)
        assertEquals(40f, CropGeometry.resizeDeltaForEdge(CropEdgeKind.TOP, -40f), 0f)
        assertEquals(40f, CropGeometry.resizeDeltaForEdge(CropEdgeKind.BOTTOM, 40f), 0f)

        val left = CropGeometry.recenterAfterResize(CropEdgeKind.LEFT, 40f, 400f, 400f, .5f, .5f)
        val right = CropGeometry.recenterAfterResize(CropEdgeKind.RIGHT, 40f, 400f, 400f, .5f, .5f)
        val top = CropGeometry.recenterAfterResize(CropEdgeKind.TOP, 40f, 400f, 400f, .5f, .5f)
        val bottom = CropGeometry.recenterAfterResize(CropEdgeKind.BOTTOM, 40f, 400f, 400f, .5f, .5f)
        assertTrue(left.first < .5f && right.first > .5f)
        assertTrue(top.second < .5f && bottom.second > .5f)
    }

    @Test
    fun `crop handles resize alone and profile uses a circular frame`() {
        val editor = source("app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt")
        assertTrue("circular = forceSquare" in editor)
        assertTrue("if (circular) CircleShape" in editor)
        assertTrue("if (forceSquare) \"برش دایره‌ای پروفایل\"" in editor)
        // V55.14 — ناحیهٔ حرکت آزاد داخلی حفظ شد (padding بزرگ‌تر برای دستگیره‌های مرئی)؛
        // resize اکنون بردار (dx,dy) می‌گیرد و گوشه‌ها را هم پشتیبانی می‌کند.
        assertTrue(".pointerInput(circular)" in editor)
        assertTrue("CropGeometry.resizeDeltaForEdge(edge, dx)" in editor)
        assertTrue("CropGeometry.resizeDeltaForCorner(edge, dx, dy)" in editor)
    }

    @Test
    fun `student passwords persist only as keystore encrypted ciphertext`() {
        val vault = source("app/src/main/java/ir/exam/app/data/local/StudentPasswordVault.kt")
        val school = source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        val manifest = source("app/src/main/AndroidManifest.xml")
        assertTrue("AndroidKeyStore" in vault)
        assertTrue("AES/GCM/NoPadding" in vault)
        assertTrue("cipher.doFinal" in vault)
        assertTrue("cipher.updateAAD(entry.toByteArray" in vault)
        assertTrue("Base64.encodeToString(cipher.iv" in vault)
        assertTrue("passwordVault.read(student.id)" in school)
        assertTrue("passwordVault.write(credential.id, credential.password)" in school)
        assertTrue("android:allowBackup=\"false\"" in manifest)
    }

    @Test
    fun `new and current password fields have identical size`() {
        val school = source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        val edit = school.substringAfter("private fun StudentEditDialog(")
            .substringBefore("private data class BulkStudentDraft")
        val equalSizeMarker = "Modifier.weight(1f).height(64.dp)"
        assertTrue(Regex(Regex.escape(equalSizeMarker)).findAll(edit).count() == 2)
    }
}
