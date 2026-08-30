package ir.exam.app.core.printing

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V72.0.2 — رگرسیون ذخیرهٔ PDF روی SAF محلی و providerهای ابری. */
class V72_0_2SafPdfWriteTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `stream writer is primary and descriptor is only fallback`() {
        val writer = source("app/src/main/java/ir/exam/app/core/printing/VerifiedSafPdfWriter.kt")
        assertTrue("contentResolver.openOutputStream(target)" in writer)
        assertTrue("writeWithCompatibleStream" in writer)
        assertTrue("writeWithDurableDescriptor" in writer)
        assertTrue(writer.indexOf("writeWithCompatibleStream") < writer.indexOf("writeWithDurableDescriptor"))
    }

    @Test
    fun `cloud provider readback has enough time to settle`() {
        val writer = source("app/src/main/java/ir/exam/app/core/printing/VerifiedSafPdfWriter.kt")
        assertTrue("0L, 150L, 400L, 900L, 1_800L, 3_000L" in writer)
        assertTrue("expected.hasSameBytes(latest)" in writer)
    }
}
