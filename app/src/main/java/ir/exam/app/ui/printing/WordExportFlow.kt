package ir.exam.app.ui.printing

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import ir.exam.app.core.io.DocumentIo
import ir.exam.app.core.network.UserFacingError
import ir.exam.app.data.repository.SupabaseBillingRepository
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.repository.WORD_EXPORT_COST_TOMAN
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * V286 — درخواست خروجی Word از روی کارت آزمون (آنلاین یا چاپی).
 * @param examRef شناسهٔ آزمون برای ثبت تراکنش (exams.id یا print_exams.id)
 * @param load سازندهٔ برگهٔ چاپ (همان چیزی که پیش‌نمایش/چاپ می‌گیرد)
 */
class WordExportRequest(
    val examRef: String,
    val title: String,
    val load: suspend () -> OfficialExamPrintable
)

/**
 * V286 — جریان کامل «فایل Word» با هزینهٔ جداگانه (۲۰٬۰۰۰ تومان برای هر نسخهٔ آزمون، مستقل از چاپ):
 * ۱) تأیید هزینه → ۲) ساخت فایل با موتور چاپ (بدون پیش‌نمایش؛ نسخهٔ دانشجو) → ۳) کسر از کیف پول
 * (native_charge_word_v286؛ اگر ساخت فایل شکست بخورد چیزی کسر نمی‌شود) → ۴) انتخاب محل ذخیره (SAF).
 * فقط در صورت موفقیت کسر، پنجرهٔ ذخیره باز می‌شود.
 */
@Composable
fun WordExportFlow(request: WordExportRequest?, onDone: () -> Unit) {
    if (request == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember(request) { mutableStateOf("confirm") } // confirm | load | build | charge | save | error:<msg> | ok
    var printable by remember(request) { mutableStateOf<OfficialExamPrintable?>(null) }
    var pending by remember(request) { mutableStateOf<ByteArray?>(null) }
    var message by remember(request) { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(WORD_MIME)) { uri ->
        val bytes = pending
        pending = null
        if (uri == null || bytes == null) { onDone(); return@rememberLauncherForActivityResult }
        scope.launch {
            DocumentIo.writeBytes(context, uri, bytes)
                .onSuccess { message = "فایل Word ذخیره شد."; step = "ok" }
                .onFailure { message = "ذخیرهٔ فایل Word ناموفق بود."; step = "error" }
        }
    }
    val fileName = request.title.ifBlank { "آزمون" }.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim() + ".docx"

    when (step) {
        "confirm" -> AlertDialog(
            onDismissRequest = onDone,
            title = { Text("فایل Word آزمون") },
            text = {
                Text(
                    "«${request.title.ifBlank { "آزمون" }}»\n" +
                        "هزینهٔ نخستین دریافت هر نسخهٔ آزمون: ${formatToman(WORD_EXPORT_COST_TOMAN)} تومان.\n" +
                        "پس از پرداخت، همین نسخه نامحدود و رایگان دانلود می‌شود؛ هر تغییر در آزمون نسخهٔ تازه‌ای است و هزینهٔ دوباره دارد. اگر این نسخه قبلاً پرداخت شده باشد، مبلغی کسر نمی‌شود."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    step = "load"
                    scope.launch {
                        runCatching { request.load() }
                            .onSuccess { printable = it; step = "build" }
                            .onFailure { message = UserFacingError.of(it, "دریافت آزمون ناموفق بود."); step = "error" }
                    }
                }) { Text("پرداخت و دریافت فایل") }
            },
            dismissButton = { TextButton(onClick = onDone) { Text("انصراف") } }
        )
        "load", "charge" -> AlertDialog(
            onDismissRequest = {},
            title = { Text("فایل Word آزمون") },
            text = { Text(if (step == "load") "در حال دریافت آزمون..." else "در حال کسر هزینه...") },
            confirmButton = {}
        )
        "build" -> ExamHtmlPrintDialog(
            printable = printable,
            onDismiss = { if (step == "build") onDone() },
            printPrepaid = true,
            onWordBytes = { bytes ->
                if (bytes == null) { message = "ساخت فایل Word ناموفق بود؛ چیزی کسر نشد."; step = "error" }
                else {
                    step = "charge"
                    scope.launch {
                        SupabaseBillingRepository().chargeWord(request.examRef, java.util.UUID.randomUUID().toString(), request.title, wordExportRevision(printable ?: error("آزمون در دسترس نیست")))
                            .onSuccess { r ->
                                pending = bytes
                                message = if (r.alreadyPaid) "این نسخه قبلاً پرداخت شده است؛ دانلود نامحدود رایگان." else "${formatToman(r.costToman)} تومان کسر شد" +
                                    (r.balanceToman?.let { "؛ موجودی: ${formatToman(it)} تومان" } ?: "") + "."
                                step = "save"
                                runCatching { launcher.launch(fileName) }
                                    .onFailure { pending = null; message = "پنجرهٔ ذخیرهٔ فایل باز نشد."; step = "error" }
                            }
                            .onFailure { message = UserFacingError.of(it, "کسر هزینهٔ فایل Word ناموفق بود."); step = "error" }
                    }
                }
            }
        )
        "save" -> Unit // پنجرهٔ سیستم (SAF) باز است
        else -> AlertDialog(
            onDismissRequest = onDone,
            title = { Text(if (step == "ok") "فایل Word" else "خطا") },
            text = { Text(message.orEmpty()) },
            confirmButton = { TextButton(onClick = onDone) { Text("باشه") } }
        )
    }
}

/** اثر انگشت پایدار محتوای نسخه؛ Bitmap را از toString حذف می‌کنیم و هش پیکسل‌های هر تصویر را جدا می‌آوریم. */
private fun wordExportRevision(exam: OfficialExamPrintable): String {
    val noBitmaps = exam.copy(questions = exam.questions.map { it.copy(images = emptyList()) })
    val imageHashes = exam.questions.flatMap { it.images }.map { bitmap ->
        val out = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "خواندن تصویر آزمون ناموفق بود" }
        MessageDigest.getInstance("SHA-256").digest(out.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    val canonical = noBitmaps.toString() + "\nimage-pixels:" + imageHashes.joinToString(",")
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
