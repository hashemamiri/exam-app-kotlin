package ir.exam.app.ui.figure

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ir.exam.app.core.figure.AtlasCatalog
import ir.exam.app.core.figure.AtlasMark
import ir.exam.app.core.figure.AtlasMarkPainter
import ir.exam.app.core.figure.FigureSpec

/**
 * V53.3 — ویرایشگر کاملاً Native آناتومی (`k='a'`) و فیزیک/شیمی (`k='s'`).
 *
 * رفتار مرجع حفظ شده است:
 * - انتخاب نوع با دسته‌بندی (۱۵ دستهٔ آناتومی / ۸ فیزیک / ۶ شیمی + «همه»)؛
 * - کشیدن انگشت روی تصویر → فلش شماره‌دار جدید (حداکثر ۱۲ نشانه، مثل مرجع)؛
 * - برچسب اختیاری هر نشانه + سوییچ‌های عنوان/جای پاسخ/نمایش نام‌ها؛
 * - خروجی همان `{k, t, X:{title, lab, blank, mkName, marks[]}}` مرجع.
 */
/**
 * V55.12 — مرحلهٔ اول (درخواست کاربر: مثل «درج شکل»): پنجرهٔ انتخاب نوع
 * آناتومی/فیزیک/شیمی با دسته‌ها؛ با انتخاب نوع، پنجرهٔ ویرایش باز می‌شود و
 * دیگر داخل ویرایش، انتخاب نوع نمایش داده نمی‌شود.
 */
@Composable
fun AtlasTypePickerDialog(
    kind: String, // "a" | "s"
    domain: String = "phys",
    onDismiss: () -> Unit,
    onTypeSelected: (String) -> Unit
) {
    val isAnatomy = kind == "a"
    val allTypes = if (isAnatomy) AtlasCatalog.ANATOMY_TYPES else AtlasCatalog.SCIENCE_TYPES
    val cats = when {
        isAnatomy -> AtlasCatalog.ANATOMY_CATS
        domain == "chem" -> AtlasCatalog.CHEM_CATS
        else -> AtlasCatalog.PHYS_CATS
    }
    val domainTypes = if (isAnatomy) allTypes else allTypes.filter {
        AtlasCatalog.scienceDomain(it.id) == domain
    }
    var category by remember { mutableStateOf("all") }
    val pickerTitle = when {
        isAnatomy -> "آناتومی بدن انسان"
        domain == "chem" -> "شیمی"
        else -> "فیزیک"
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("درج $pickerTitle", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("✕") }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    cats.forEach { cat ->
                        FilterChip(
                            selected = category == cat.id,
                            onClick = { category = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
                val visible = domainTypes.filter { category == "all" || it.cat == category }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(visible, key = { it.id }) { t ->
                        Card(Modifier.fillMaxWidth().clickable { onTypeSelected(t.id) }) {
                            Column(
                                Modifier.fillMaxWidth().padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                AtlasThumb(kind, t.id, Modifier.fillMaxWidth().height(96.dp))
                                Text(t.name, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AtlasEditorDialog(
    kind: String, // "a" | "s"
    domain: String = "phys", // فقط برای k='s': "phys" | "chem"
    initialSpec: FigureSpec? = null,
    // V55.12 — نوعِ ازپیش‌انتخاب‌شده در پنجرهٔ اول؛ ویرایش، انتخاب نوع ندارد.
    presetType: String? = null,
    onDismiss: () -> Unit,
    onInsert: (FigureSpec) -> Unit
) {
    val isAnatomy = kind == "a"
    val allTypes = if (isAnatomy) AtlasCatalog.ANATOMY_TYPES else AtlasCatalog.SCIENCE_TYPES
    val effectiveDomain = when {
        isAnatomy -> ""
        initialSpec != null -> AtlasCatalog.scienceDomain(initialSpec.type)
        else -> domain
    }
    val defaultType = when {
        initialSpec != null -> initialSpec.type
        presetType != null -> presetType
        isAnatomy -> "bodyF"
        effectiveDomain == "chem" -> "beak"
        else -> "cSim"
    }

    val typeId by remember { mutableStateOf(defaultType) }
    var title by remember {
        mutableStateOf(
            initialSpec?.xStr("title")
                ?: (allTypes.firstOrNull { it.id == defaultType }?.name ?: "")
        )
    }
    var showLabel by remember { mutableStateOf((initialSpec?.xStr("lab", "1") ?: "1") != "0") }
    var showBlanks by remember { mutableStateOf((initialSpec?.xStr("blank", "1") ?: "1") != "0") }
    var showMarkNames by remember { mutableStateOf((initialSpec?.xStr("mkName", "0") ?: "0") == "1") }
    var marks by remember { mutableStateOf(initialSpec?.marks() ?: emptyList()) }
    val isEdit = initialSpec != null
    val editorTitle = when {
        isAnatomy -> "آناتومی بدن انسان"
        effectiveDomain == "chem" -> "شیمی"
        else -> "فیزیک"
    }

    fun currentSpec(): FigureSpec = FigureSpec.buildAtlas(
        kind = kind,
        type = typeId,
        title = title.trim(),
        showLabel = showLabel,
        showBlanks = showBlanks,
        showMarkNames = showMarkNames,
        marks = marks
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "ویرایش $editorTitle" else "درج $editorTitle") },
        confirmButton = {
            TextButton(onClick = { onInsert(currentSpec()) }) {
                Text(if (isEdit) "اعمال تغییرات" else "درج در سؤال")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 580.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // V55.12 — انتخاب نوع در پنجرهٔ اول (AtlasTypePickerDialog) انجام
                // شده است؛ اینجا فقط ویرایش همان نوع.
                // کپشن آموزشی آناتومی
                if (isAnatomy) {
                    AtlasCatalog.anatomyType(typeId)?.caption?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(selected = showLabel, onClick = { showLabel = !showLabel }, label = { Text("نمایش عنوان") })
                    FilterChip(selected = showBlanks, onClick = { showBlanks = !showBlanks }, label = { Text("جای پاسخ") })
                    FilterChip(selected = showMarkNames, onClick = { showMarkNames = !showMarkNames }, label = { Text("نمایش نام‌ها") })
                }
                Text(
                    "برای افزودن نشانهٔ شماره‌دار، انگشت را از نقطهٔ موردنظر روی تصویر بکشید (حداکثر ۱۲ نشانه).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // بوم نشانه‌گذاری
                MarkingCanvas(
                    kind = kind,
                    typeId = typeId,
                    marks = marks,
                    onAddMark = { mark -> if (marks.size < 12) marks = marks + mark }
                )
                // V55.12 — درخواست کاربر: کادرهای برچسب تصاویر نمایش داده نشود؛
                // فقط ردیف فشردهٔ شماره + دکمهٔ حذف هر نشانه.
                if (marks.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        marks.sortedBy { it.n }.forEach { mark ->
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    Modifier.padding(start = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        AtlasMarkPainter.faNum(mark.n),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    IconButton(
                                        onClick = { marks = marks.filterNot { it.n == mark.n } },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "حذف نشانه ${AtlasMarkPainter.faNum(mark.n)}",
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (marks.isNotEmpty()) {
                    OutlinedButton(onClick = { marks = emptyList() }, modifier = Modifier.fillMaxWidth()) {
                        Text("پاک‌کردن همه نشانه‌ها")
                    }
                }
            }
        }
    )
}

/** شمارهٔ آزاد بعدی — همان nextMarkN مرجع. */
internal fun nextMarkNumber(marks: List<AtlasMark>): Int {
    val used = marks.map { it.n }.toSet()
    var n = 1
    while (n in used) n++
    return n
}

@Composable
private fun AtlasThumb(kind: String, typeId: String, modifier: Modifier = Modifier.size(52.dp)) {
    val context = LocalContext.current
    val path = AtlasCatalog.assetPath(
        FigureSpec.buildAtlas(kind, typeId, "", showLabel = false, showBlanks = false, showMarkNames = false, marks = emptyList())
    ) ?: return
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data("file:///android_asset/$path")
            .crossfade(false)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
private fun MarkingCanvas(
    kind: String,
    typeId: String,
    marks: List<AtlasMark>,
    onAddMark: (AtlasMark) -> Unit
) {
    val context = LocalContext.current
    val path = AtlasCatalog.assetPath(
        FigureSpec.buildAtlas(kind, typeId, "", showLabel = false, showBlanks = false, showMarkNames = false, marks = emptyList())
    ) ?: return
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .onSizeChanged { canvasSize = it }
            .pointerInput(typeId, marks.size) {
                detectDragGestures(
                    onDragStart = { offset -> dragStart = offset; dragEnd = offset },
                    onDragCancel = { dragStart = null; dragEnd = null },
                    onDrag = { change, _ -> change.consume(); dragEnd = change.position },
                    onDragEnd = {
                        val start = dragStart
                        val end = dragEnd
                        if (start != null && end != null && canvasSize.width > 0 && canvasSize.height > 0) {
                            val dx = end.x - start.x
                            val dy = end.y - start.y
                            // مثل مرجع: کشیدن خیلی کوتاه نشانه نمی‌سازد.
                            if (dx * dx + dy * dy >= 16f) {
                                onAddMark(
                                    AtlasMark(
                                        x1 = (start.x / canvasSize.width * 100f).coerceIn(0f, 100f),
                                        y1 = (start.y / canvasSize.height * 100f).coerceIn(0f, 100f),
                                        x2 = (end.x / canvasSize.width * 100f).coerceIn(0f, 100f),
                                        y2 = (end.y / canvasSize.height * 100f).coerceIn(0f, 100f),
                                        n = nextMarkNumber(marks)
                                    )
                                )
                            }
                        }
                        dragStart = null; dragEnd = null
                    }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/$path")
                .crossfade(false)
                .build(),
            contentDescription = "تصویر برای نشانه‌گذاری",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Canvas(Modifier.fillMaxSize()) {
            fun drawMark(x1: Float, y1: Float, x2: Float, y2: Float, n: Int?, draft: Boolean) {
                val color = if (draft) Color(0x99E4572E) else Color(0xFFE4572E)
                val start = Offset(x1, y1)
                val end = Offset(x2, y2)
                drawLine(color, start, end, strokeWidth = size.minDimension * 0.008f)
                val head = AtlasMarkPainter.arrowHead(x1, y1, x2, y2, size.minDimension * 0.030f)
                if (head.size == 6) {
                    drawPath(
                        Path().apply {
                            moveTo(head[0], head[1]); lineTo(head[2], head[3])
                            lineTo(head[4], head[5]); close()
                        },
                        color
                    )
                }
                if (n != null) {
                    // V55.12 — درخواست کاربر: شماره در «انتهای» پیکان (کنار سر فلش).
                    val radius = size.minDimension * 0.040f
                    drawCircle(color, radius, end)
                    drawCircle(Color.White, radius * 0.78f, end)
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            this.color = android.graphics.Color.parseColor("#C23B17")
                            textSize = radius * 1.15f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                        drawText(AtlasMarkPainter.faNum(n), end.x, end.y + radius * 0.40f, paint)
                    }
                }
            }
            marks.forEach { m ->
                drawMark(
                    m.x1 / 100f * size.width, m.y1 / 100f * size.height,
                    m.x2 / 100f * size.width, m.y2 / 100f * size.height,
                    m.n, draft = false
                )
            }
            val ds = dragStart
            val de = dragEnd
            if (ds != null && de != null) drawMark(ds.x, ds.y, de.x, de.y, null, draft = true)
        }
    }
}
