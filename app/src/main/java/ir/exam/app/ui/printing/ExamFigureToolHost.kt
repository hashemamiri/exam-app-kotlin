package ir.exam.app.ui.printing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ir.exam.app.core.figure.AtlasCatalog
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.GRAPH_FIGURES
import ir.exam.app.ui.figure.AtlasEditorDialog
import ir.exam.app.ui.figure.AtlasTypePickerDialog
import ir.exam.app.ui.figure.FigureKind
import ir.exam.app.ui.figure.FigurePickerDialog
import ir.exam.app.ui.figure.FigureTypePickerDialog
import ir.exam.app.ui.figure.PeriodicEditorDialog
import ir.exam.app.ui.figure.TableEditorDialog

/**
 * درخواست ویرایش یک شکل از پیش‌نمایش چاپ. خود شکل با همان ابزارهای بومیِ
 * سازنده ویرایش می‌شود و توکنِ نتیجه جایگزینِ همان توکن در موتور رندر خواهد شد.
 */
internal data class FigureToolRequest(
    val questionId: String,
    val tool: String,
    val initialSpecJson: String,
    val tokenStart: Int,
    val tokenEnd: Int
) {
    val isNative: Boolean get() = tool in NATIVE_TOOLS

    companion object {
        val NATIVE_TOOLS = setOf("figure", "graph", "table", "anatomy", "periodic", "physics", "chemistry")
    }
}

/** توکنِ متناظر با قرارداد `FigureCodec` و رندرر PDF. */
internal fun figureTokenOf(spec: FigureSpec): String = "%%FIG:" + spec.toJson() + "%%"

/**
 * تشخیص ابزار بومی از روی spec؛ هم آزمون‌ساز و هم overlay پیش‌نمایش PDF از
 * این نگاشت واحد استفاده می‌کنند. `k=img` عمداً ابزار ویرایش شکل ندارد.
 */
internal fun toolOfSpec(specJson: String): String? {
    val spec = FigureSpec.parse(specJson) ?: return null
    return when (spec.kind) {
        "t" -> "table"
        "p" -> "periodic"
        "a" -> "anatomy"
        "s" -> if (AtlasCatalog.scienceDomain(spec.type) == "chem") "chemistry" else "physics"
        "g" -> "graph"
        "" -> if (GRAPH_FIGURES.any { it.id == spec.type }) "graph" else "figure"
        else -> null
    }
}

/**
 * پنجرهٔ بومیِ متناظر با `request.tool` را نشان می‌دهد و در پایان توکن را
 * برای جایگزینیِ شکل انتخاب‌شده برمی‌گرداند.
 */
@Composable
internal fun ExamFigureToolHost(
    request: FigureToolRequest,
    onInsert: (token: String) -> Unit,
    onDismiss: () -> Unit
) {
    // spec فعلی برای پرکردن مقادیر پنجره به ابزار بومی داده می‌شود.
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

        // شکل و نمودار در صورت نیاز انتخاب نوع و سپس ویرایش را نشان می‌دهند.
        "figure" -> FigureToolFlow(FigureKind.GEOMETRY, onInsert, onDismiss, initial)
        "graph" -> FigureToolFlow(FigureKind.GRAPH, onInsert, onDismiss, initial)

        // آناتومی و فیزیک/شیمی نیز انتخاب نوع و سپس ویرایش دارند.
        "anatomy" -> AtlasToolFlow("a", "phys", onInsert, onDismiss, initial)
        "physics" -> AtlasToolFlow("s", "phys", onInsert, onDismiss, initial)
        "chemistry" -> AtlasToolFlow("s", "chem", onInsert, onDismiss, initial)

        else -> onDismiss()
    }
}

/** انتخاب نوع و ویرایش برای شکل و نمودار. */
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
            // در ویرایشِ شکل موجود، بستن یعنی انصراف کامل.
            onDismiss = { if (initialSpec != null) onDismiss() else picked = null },
            onInsert = { s -> onInsert(figureTokenOf(s)) }
        )
    }
}

/** انتخاب نوع و ویرایش برای k='a' و k='s'. */
@Composable
private fun AtlasToolFlow(
    kind: String,
    domain: String,
    onInsert: (String) -> Unit,
    onDismiss: () -> Unit,
    // در ویرایشِ شکل موجود، نوع از spec خوانده می‌شود.
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
            // در ساخت شکل تازه، بازگشت به انتخاب نوع می‌رود.
            onDismiss = { if (initialSpec != null) onDismiss() else pickedType = null },
            onInsert = { spec -> onInsert(figureTokenOf(spec)) }
        )
    }
}
