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
 * «فرمول» عمداً در این مسیر نیست و همان ویرایشگر HTML را باز می‌کند
 * (تصمیم صریح کاربر).
 */
internal data class FigureToolRequest(val questionId: String, val tool: String) {

    /** آیا این ابزار مسیر بومی دارد؟ فرمول ندارد. */
    val isNative: Boolean get() = tool in NATIVE_TOOLS

    companion object {
        val NATIVE_TOOLS = setOf("figure", "graph", "table", "anatomy", "periodic", "physics", "chemistry")
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
    when (request.tool) {
        "table" -> TableEditorDialog(
            onDismiss = onDismiss,
            onInsert = { spec -> onInsert(figureTokenOf(spec)) }
        )

        "periodic" -> PeriodicEditorDialog(
            onDismiss = onDismiss,
            onInsert = { spec -> onInsert(figureTokenOf(spec)) }
        )

        // شکل و نمودار هم دو مرحله‌اند (انتخاب نوع ← ویرایش)، عیناً مثل
        // مسیرِ chooseType در آزمون‌سازِ آنلاین.
        "figure" -> FigureToolFlow(FigureKind.GEOMETRY, onInsert, onDismiss)
        "graph" -> FigureToolFlow(FigureKind.GRAPH, onInsert, onDismiss)

        // آناتومی و فیزیک/شیمی دو مرحله‌اند: اول انتخاب نوع، بعد ویرایش —
        // عیناً همان جریانی که در آزمون‌سازِ آنلاین وجود دارد.
        "anatomy" -> AtlasToolFlow(kind = "a", domain = "phys", onInsert = onInsert, onDismiss = onDismiss)
        "physics" -> AtlasToolFlow(kind = "s", domain = "phys", onInsert = onInsert, onDismiss = onDismiss)
        "chemistry" -> AtlasToolFlow(kind = "s", domain = "chem", onInsert = onInsert, onDismiss = onDismiss)

        else -> onDismiss()
    }
}

/** انتخاب نوع ← ویرایش ← درج، برای شکل و نمودار. */
@Composable
private fun FigureToolFlow(
    kind: FigureKind,
    onInsert: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var picked by remember { mutableStateOf<FigureSpec?>(null) }
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
            onDismiss = { picked = null },
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
    onDismiss: () -> Unit
) {
    var pickedType by remember { mutableStateOf<String?>(null) }
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
            presetType = type,
            // بازگشت از ویرایش به انتخابِ نوع، نه بستنِ کاملِ جریان
            onDismiss = { pickedType = null },
            onInsert = { spec -> onInsert(figureTokenOf(spec)) }
        )
    }
}
