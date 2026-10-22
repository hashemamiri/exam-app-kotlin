package ir.exam.app.uitest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Base64
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.junit.AssumptionViolatedException

/**
 * V333.1 — ابزار ثبت اسکرین‌شات واقعی صفحات Compose، مدیریت کیبورد نرم‌افزاری، اسکرول LazyColumn
 * و عیب‌یابی تصویری خودکار در لحظهٔ خطا (`99_failure_*.png`) روی شبیه‌ساز Pixel 6 (API 34).
 */
object UiScreenshotHelper {
    private const val DEFAULT_ARTIFACT_ENDPOINT = "http://10.0.2.2:18991/__test__/screenshot"

    /**
     * بستن قطعی کیبورد نرم‌افزاری اندروید (IME) پیش از لمس دکمه‌های شناور پایین صفحه و پیش از اسکرین‌شات،
     * تا پنجرهٔ کیبورد نه مانع برخورد لمس به دکمه‌های پایین (`ذخیره آزمون` / `ورود`) شود و نه نیمی از تصویر را بپوشاند.
     */
    fun <A : ComponentActivity> hideKeyboard(rule: AndroidComposeTestRule<*, A>) {
        runCatching {
            val activity = rule.activity
            activity.runOnUiThread {
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val focusView = activity.currentFocus ?: activity.window?.decorView
                if (focusView != null) {
                    imm?.hideSoftInputFromWindow(focusView.windowToken, 0)
                    focusView.clearFocus()
                }
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            rule.waitForIdle()
        }
    }

    /**
     * اسکرول امن برای هر دو نوع فهرست (`Column.verticalScroll` و `LazyColumn` که آیتم‌های پایین صفحه را
     * تا پیش از اسکرول اصلاً Compose نمی‌کند).
     */
    fun <A : ComponentActivity> scrollUntilTextVisible(
        rule: AndroidComposeTestRule<*, A>,
        text: String,
        substring: Boolean = false,
        maxSwipes: Int = 8
    ) {
        hideKeyboard(rule)
        repeat(maxSwipes) {
            if (rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()) {
                return
            }
            runCatching {
                rule.onAllNodes(isRoot()).onFirst().performTouchInput { swipeUp() }
                rule.waitForIdle()
            }
        }
        rule.waitUntil(10_000) {
            rule.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** استخراج متن‌های قابل‌مشاهده روی صفحه (شامل پنجره‌های AlertDialog) برای عیب‌یابی دقیق. */
    fun <A : ComponentActivity> dumpVisibleTexts(rule: AndroidComposeTestRule<*, A>): String = runCatching {
        val out = LinkedHashSet<String>()
        fun walk(node: androidx.compose.ui.semantics.SemanticsNode) {
            if (node.config.contains(SemanticsProperties.Text)) {
                node.config[SemanticsProperties.Text].forEach { t ->
                    val clean = t.text.trim()
                    if (clean.isNotEmpty()) out.add(clean)
                }
            }
            if (node.config.contains(SemanticsProperties.EditableText)) {
                val edit = node.config[SemanticsProperties.EditableText].text.trim()
                if (edit.isNotEmpty()) out.add("[$edit]")
            }
            node.children.forEach(::walk)
        }
        rule.onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes().forEach(::walk)
        out.take(40).joinToString(" | ")
    }.getOrDefault("")

    /**
     * اجرای بلوک تست همراه ثبت خودکار اسکرین‌شات و فهرست متن‌های صفحه در صورت بروز هرگونه خطا یا تغییر ناخواستهٔ UI.
     */
    fun <A : ComponentActivity> runWithFailureCapture(
        rule: AndroidComposeTestRule<*, A>,
        testName: String,
        block: () -> Unit
    ) {
        try {
            block()
        } catch (skip: AssumptionViolatedException) {
            throw skip
        } catch (t: Throwable) {
            val visible = dumpVisibleTexts(rule)
            runCatching {
                captureStep(
                    rule = rule,
                    filename = "99_failure_${testName}.png",
                    title = "❌ تصویر لحظهٔ خطا در $testName",
                    detail = "متن‌های روی صفحه: ${visible.take(220)}"
                )
            }
            throw AssertionError(
                "خطا در تست رابط کاربری ($testName) — متن‌های قابل‌مشاهده روی صفحه: [$visible] — علت اصلی: ${t.message}",
                t
            )
        }
    }

    fun <A : ComponentActivity> captureStep(
        rule: AndroidComposeTestRule<*, A>,
        filename: String,
        title: String,
        detail: String = ""
    ): File? {
        hideKeyboard(rule)
        rule.waitForIdle()
        val safeName = filename
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .let { if (it.endsWith(".png", ignoreCase = true)) it else "$it.png" }

        val activity = rule.activity
        var hadSecure = false
        activity.runOnUiThread {
            val window = activity.window
            hadSecure = window != null && (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        }

        var bitmap: Bitmap? = if (!hadSecure) {
            // ثبت تصویر واقعی خروجی GPU سیستم (شامل AlertDialogها و پنجره‌های شناور روی صفحه)
            runCatching {
                InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            }.getOrNull()
        } else {
            null
        }

        if (bitmap == null) {
            activity.runOnUiThread {
                val window = activity.window
                val decor = window?.decorView
                if (decor != null && decor.width > 0 && decor.height > 0) {
                    val secureNow = (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
                    try {
                        if (secureNow) {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                        val bmp = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        decor.draw(canvas)
                        bitmap = bmp
                    } finally {
                        if (secureNow) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
                }
            }
        }

        val captured = bitmap ?: return null
        val pngBytes = ByteArrayOutputStream().use { out ->
            captured.compress(Bitmap.CompressFormat.PNG, 92, out)
            out.toByteArray()
        }
        val width = captured.width
        val height = captured.height
        captured.recycle()

        val targetCtx = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = (targetCtx.externalMediaDirs.firstOrNull() ?: targetCtx.filesDir)
            .let { File(it, "ui-screenshots").apply { mkdirs() } }
        val outFile = File(dir, safeName).apply { writeBytes(pngBytes) }

        val customEndpoint = InstrumentationRegistry.getArguments()
            .getString("screenshotEndpoint")
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_ARTIFACT_ENDPOINT
        streamToHostCollector(
            endpoint = customEndpoint,
            filename = safeName,
            title = title,
            detail = detail,
            width = width,
            height = height,
            pngBytes = pngBytes
        )
        return outFile
    }

    private fun streamToHostCollector(
        endpoint: String,
        filename: String,
        title: String,
        detail: String,
        width: Int,
        height: Int,
        pngBytes: ByteArray
    ) {
        runCatching {
            val payload = JSONObject()
                .put("filename", filename)
                .put("title", title)
                .put("detail", detail)
                .put("width", width)
                .put("height", height)
                .put("png_base64", Base64.encodeToString(pngBytes, Base64.NO_WRAP))
                .toString()
                .toByteArray(Charsets.UTF_8)

            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3_000
                readTimeout = 5_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            try {
                conn.outputStream.use { it.write(payload) }
                conn.responseCode
            } finally {
                conn.disconnect()
            }
        }
    }
}
