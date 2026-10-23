package ir.exam.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.2 — نگهبان دائمی صفر بودن هشدارهای منسوخ (Deprecation) و OptIn در کد اصلی اندروید:
 * تضمین می‌کند هیچ‌یک از ۱۹ مورد API منسوخ/شرط تکراری و ۶ مورد `explicitNulls` بدون OptIn
 * در هیچ‌یک از فایل‌های `app/src/main` برنگردد.
 */
class V335_2_ZeroDeprecationAuditTest {

    private fun mainSourceRoot(): File {
        val candidates = listOf(
            File("src/main/java"),
            File("app/src/main/java")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("مسیر src/main/java یافت نشد: ${File(".").absolutePath}")
    }

    @Test
    fun noDeprecatedAndroidOrComposeApisRemainInMainSources() {
        val root = mainSourceRoot()
        val forbiddenTokens = listOf(
            "LocalImageLoader" to "به‌جای LocalImageLoader از ImageLoaderFactory و Coil.imageLoader(context) استفاده کنید",
            "LocalClipboardManager" to "به‌جای LocalClipboardManager از ClipboardManager اندروید استفاده کنید",
            "quadraticBezierTo(" to "به‌جای quadraticBezierTo از quadraticTo استفاده کنید",
            "SOFT_INPUT_ADJUST_RESIZE" to "به‌جای SOFT_INPUT_ADJUST_RESIZE از WindowCompat.setDecorFitsSystemWindows و imePadding استفاده کنید",
            "Icons.Outlined.RotateLeft" to "از Icons.AutoMirrored.Outlined.RotateLeft استفاده کنید",
            "Icons.Outlined.RotateRight" to "از Icons.AutoMirrored.Outlined.RotateRight استفاده کنید",
            "Icons.Outlined.ArrowBack" to "از Icons.AutoMirrored.Outlined.ArrowBack استفاده کنید",
            "Icons.Outlined.Login" to "از Icons.AutoMirrored.Outlined.Login استفاده کنید",
            "Icons.Outlined.KeyboardArrowLeft" to "از Icons.AutoMirrored.Outlined.KeyboardArrowLeft استفاده کنید",
            "Icons.Outlined.KeyboardArrowRight" to "از Icons.AutoMirrored.Outlined.KeyboardArrowRight استفاده کنید",
            "import androidx.compose.material.icons.outlined.RotateLeft" to "ایمپورت منسوخ RotateLeft",
            "import androidx.compose.material.icons.outlined.RotateRight" to "ایمپورت منسوخ RotateRight",
            "import androidx.compose.material.icons.outlined.ArrowBack" to "ایمپورت منسوخ ArrowBack",
            "import androidx.compose.material.icons.outlined.Login" to "ایمپورت منسوخ Login",
            "import androidx.compose.material.icons.outlined.KeyboardArrowLeft" to "ایمپورت منسوخ KeyboardArrowLeft",
            "import androidx.compose.material.icons.outlined.KeyboardArrowRight" to "ایمپورت منسوخ KeyboardArrowRight"
        )

        val violations = mutableListOf<String>()
        val ktFiles = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.sortedBy { it.path }.toList()
        assertTrue("فایل‌های Kotlin باید یافت شوند", ktFiles.size >= 250)

        for (file in ktFiles) {
            val text = file.readText(Charsets.UTF_8)
            val lines = text.lines()
            for ((idx, line) in lines.withIndex()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) continue
                for ((token, hint) in forbiddenTokens) {
                    if (line.contains(token)) {
                        violations += "${file.relativeTo(root).path}:${idx + 1}: یافت شد `$token` — $hint"
                    }
                }
                if (line.contains("explicitNulls") && !text.contains("ExperimentalSerializationApi")) {
                    violations += "${file.relativeTo(root).path}:${idx + 1}: استفاده از explicitNulls بدون ExperimentalSerializationApi"
                }
            }
        }

        assertTrue(
            "موارد منسوخ در کد اصلی یافت شد:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun examApplicationImplementsCoilImageLoaderFactory() {
        val appFile = File(mainSourceRoot(), "ir/exam/app/ExamApplication.kt")
        assertTrue("ExamApplication.kt باید وجود داشته باشد", appFile.isFile)
        val content = appFile.readText(Charsets.UTF_8)
        assertTrue("ExamApplication باید ImageLoaderFactory را پیاده‌سازی کند", content.contains("ImageLoaderFactory"))
        assertTrue("ExamApplication باید newImageLoader را تعریف کند", content.contains("override fun newImageLoader()"))
    }
}
