package ir.exam.app.ui.printing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.ui.figure.AtlasEditorDialog
import ir.exam.app.ui.figure.AtlasTypePickerDialog
import ir.exam.app.ui.figure.FigureKind
import ir.exam.app.ui.figure.FigurePickerDialog
import ir.exam.app.ui.figure.FigureTypePickerDialog
import ir.exam.app.ui.figure.PeriodicEditorDialog
import ir.exam.app.ui.figure.TableEditorDialog

/**
 * V78.0 — پلِ ابزارهای درج بین آزمون‌سازِ چاپ (نسخهٔ ۳۰، HTML) و ویرایشگرهای
 * بومیِ موجود در `ui/figure` — همان‌هایی که آزمون‌سازِ آنلاین از V53 استفاده
 * می‌کند. هیچ ویرایشگری از نو نوشته نشده است؛ فقط سیم‌کشی شده.
 *
 * قرارداد یکسانِ دو نسخه: خروجی هر ابزار یک `FigureSpec` است که به‌صورت
 * توکنِ متنیِ `%%FIG:{json}%%` در متنِ سؤال درج می‌شود. رندرِ آن توکن همچنان
 * کارِ `renderFigToken` در HTML است، پس **خروجی چاپ هیچ تغییری نمی‌کند**.
 *
 * V82.0 — «فرمول» هم به همین شکل بومی شد، با این تفاوت که پنجره‌اش
 * (FormulaHostDialog) متنِ کاملِ سؤال را می‌گیرد و متنِ کامل برمی‌گرداند.
 *
 * V82.0 — `editIndex` یعنی «ویرایشِ nاُمین توکنِ همین سؤال» به‌جای درجِ تازه؛
 * دابل‌کلیک روی یک ابزارِ درج‌شده از این راه می‌آید.
 */
internal data class FigureToolRequest(
    val questionId: String,
    val tool: String,
    /** V82.0 — ویرایش: اندیسِ توکن در متنِ سؤال. null یعنی درجِ جدید. */
    val editIndex: Int? = null,
    /** V82.0 — spec موجود برای پیش‌پرکردنِ پنجره هنگام ویرایش. */
    val initialSpecJson: String? = null,
    /** V82.0 — محدودهٔ توکن در متن، برای جایگزینیِ دقیق. */
    val tokenStart: Int = -1,
    val tokenEnd: Int = -1
) {

    /** آیا این ابزار مسیر بومی دارد؟ */
    val isNative: Boolean get() = tool in NATIVE_TOOLS

    /** V82.0 — آیا این درخواستِ ویرایش است؟ */
    val isEdit: Boolean get() = editIndex != null && tokenEnd > tokenStart

    companion object {
        val NATIVE_TOOLS = setOf("figure", "graph", "table", "anatomy", "periodic", "physics", "chemistry")

        /**
         * V82.0 — «فرمول» هم پنجرهٔ بومی دارد (FormulaHostDialog) ولی قراردادش
         * فرق می‌کند: متنِ کاملِ سؤال را می‌گیرد و متنِ کامل برمی‌گرداند،
         * نه یک FigureSpec. برای همین از NATIVE_TOOLS جداست.
         */
        const val FORMULA = "formula"
        val ALL_TOOLS = NATIVE_TOOLS + FORMULA
    }
}

/** توکنِ متنیِ سازگار با `renderFigToken` و با `FigTokenVisuals.TOKEN`. */
internal fun figureTokenOf(spec: FigureSpec): String = "%%FIG:" + spec.toJson() + "%%"

/**
 * پنجرهٔ بومیِ متناظر با `request.tool` را نشان می‌دهد و در پایان توکن را
 * برمی‌گرداند. `onInsert` مسئول رساندن توکن به صفحه است.
 */
@Composable
internal fun ExamFigureToolHost(
    request: FigureToolRequest,
    onInsert: (token: String) -> Unit,
    onDismiss: () -> Unit
) {
    // V82.0 — هنگام ویرایش، spec موجود به همان پنجره داده می‌شود تا کاربر
    // مقادیر قبلی را ببیند؛ همان ویرایشگرها، فقط با initialSpec.
    val initial = remember(request.initialSpecJson) {
        request.initialSpecJson?.let { FigureSpec.parse(it) }
    }
    when (request.tool) {
        "table" -> TableEditorDialog(
            initialSpec = initial,
            onDismiss = onDismiss,
            onInsert = { spec -> onInsert(figureTokenOf(spec)) }
        )

        "periodic" -> PeriodicEditorDialog(
            initialSpec = initial,
            onDismiss = onDismiss,
            onInsert = { spec -> onInsert(figureTokenOf(spec)) }
        )

        // شکل و نمودار هم دو مرحله‌اند (انتخاب نوع ← ویرایش)، عیناً مثل
        // مسیرِ chooseType در آزمون‌سازِ آنلاین.
        "figure" -> FigureToolFlow(FigureKind.GEOMETRY, onInsert, onDismiss, initial)
        "graph" -> FigureToolFlow(FigureKind.GRAPH, onInsert, onDismiss, initial)

        // آناتومی و فیزیک/شیمی دو مرحله‌اند: اول انتخاب نوع، بعد ویرایش —
        // عیناً همان جریانی که در آزمون‌سازِ آنلاین وجود دارد.
        "anatomy" -> AtlasToolFlow("a", "phys", onInsert, onDismiss, initial)
        "physics" -> AtlasToolFlow("s", "phys", onInsert, onDismiss, initial)
        "chemistry" -> AtlasToolFlow("s", "chem", onInsert, onDismiss, initial)

        else -> onDismiss()
    }
}

/** انتخاب نوع ← ویرایش ← درج، برای شکل و نمودار. */
@Composable
private fun FigureToolFlow(
    kind: FigureKind,
    onInsert: (String) -> Unit,
    onDismiss: () -> Unit,
    // V82.0 — ویرایش: نوع از قبل معلوم است، پس پنجرهٔ انتخابِ نوع رد می‌شود.
    initialSpec: FigureSpec? = null
) {
    var picked by remember(initialSpec) { mutableStateOf(initialSpec) }
    val spec = picked
    if (spec == null) {
        FigureTypePickerDialog(
            kind = kind,
            onDismiss = onDismiss,
            onTypeSelected = { picked = it }
        )
    } else {
        FigurePickerDialog(
            initialSpec = spec,
            initialKind = kind,
            // در حالتِ ویرایش، بستن یعنی انصراف کامل (پنجرهٔ انتخابِ نوعی در کار نیست)
            onDismiss = { if (initialSpec != null) onDismiss() else picked = null },
            onInsert = { s -> onInsert(figureTokenOf(s)) }
        )
    }
}

/** انتخاب نوع ← ویرایش ← درج، برای k='a' و k='s'. */
@Composable
private fun AtlasToolFlow(
    kind: String,
    domain: String,
    onInsert: (String) -> Unit,
    onDismiss: () -> Unit,
    // V82.0 — ویرایش: نوع از خودِ spec می‌آید و انتخابِ نوع رد می‌شود.
    initialSpec: FigureSpec? = null
) {
    var pickedType by remember(initialSpec) { mutableStateOf(initialSpec?.type) }
    val type = pickedType
    if (type == null) {
        AtlasTypePickerDialog(
            kind = kind,
            domain = domain,
            onDismiss = onDismiss,
            onTypeSelected = { pickedType = it }
        )
    } else {
        AtlasEditorDialog(
            kind = kind,
            domain = domain,
            initialSpec = initialSpec,
            presetType = type,
            // بازگشت از ویرایش به انتخابِ نوع، نه بستنِ کاملِ جریان
            onDismiss = { if (initialSpec != null) onDismiss() else pickedType = null },
            onInsert = { spec -> onInsert(figureTokenOf(spec)) }
        )
    }
}
