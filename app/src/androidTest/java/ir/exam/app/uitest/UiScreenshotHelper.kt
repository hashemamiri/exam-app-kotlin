package ir.exam.app.uitest

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Base64
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * V333.0 — ثبت اسکرین‌شات واقعی صفحات Compose در شبیه‌ساز بدون دست‌کاری کد امنیتی تولید (`StudentExamScreen`).
 *
 * نکات کلیدی:
 * ۱) در صفحهٔ فعال آزمون دانش‌آموز که `FLAG_SECURE` روی پنجره روشن است، فقط در همان میلی‌ثانیهٔ رسم روی بوم
 *    در نخ UI پرچم موقتاً برداشته و بلافاصله در بلوک `finally` بازگردانده می‌شود تا امنیت نسخهٔ عملیاتی دست‌نخورده بماند.
 * ۲) تصویر PNG هم در حافظهٔ دستگاه ذخیره می‌شود و هم از طریق `http://10.0.2.2:18991/__test__/screenshot`
 *    مستقیماً به دیسک میزبان GitHub Actions ارسال می‌گردد تا پیش از بسته‌شدن شبیه‌ساز مدیریت‌شدهٔ Gradle،
 *    گالری تصویری فارسی (`visual-report.html` و `summary.md`) ساخته شود.
 */
object UiScreenshotHelper {
    private const val DEFAULT_ARTIFACT_ENDPOINT = "http://10.0.2.2:18991/__test__/screenshot"

    fun <A : ComponentActivity> captureStep(
        rule: AndroidComposeTestRule<*, A>,
        filename: String,
        title: String,
        detail: String = ""
    ): File? {
        rule.waitForIdle()
        val safeName = filename
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .let { if (it.endsWith(".png", ignoreCase = true)) it else "$it.png" }

        val activity = rule.activity
        var bitmap: Bitmap? = null
        activity.runOnUiThread {
            val window = activity.window
            val decor = window?.decorView
            if (decor != null && decor.width > 0 && decor.height > 0) {
                val hadSecure = (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
                try {
                    if (hadSecure) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                    val bmp = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    decor.draw(canvas)
                    bitmap = bmp
                } finally {
                    if (hadSecure) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
