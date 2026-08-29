package ir.exam.app.ui.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.math.FormulaTextCodec
import ir.exam.app.core.math.NativeMathSvgRenderer
import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
import ir.exam.app.ui.figure.AtlasFigureView
import ir.exam.app.ui.figure.InlineFigureView
import ir.exam.app.ui.math.NativeFormulaView
import ir.exam.app.ui.math.QuestionEditorFieldController
import ir.exam.app.ui.math.QuestionToolIcons

/**
 * V65.0 — کادر متن سؤال کاملاً Native Compose (بدون WebView).
 * نوار ۸ آیکن Native با ترتیب مرجع حفظ شده است. فرمول تمام‌صفحه همچنان
 * FormulaHostDialog است؛ شکل/جدول/اطلس همان ویرایشگرهای Native موجود.
 *
 * V67.0 — مکان‌نما بدون لمس قبلی روی بخش متنی آماده است؛ درج اشیای
 * درون‌متنی در محل آخرین بخش فعال رخ می‌دهد و پس از درج، مکان‌نما به
 * ادامهٔ متن می‌رود؛ فرمول‌ها با اندازهٔ طبیعی رندر می‌شوند (بزرگ‌ها در
 * جعبهٔ تمام‌عرض اسکرول‌شونده، بدون کوچک‌شدن) و هر شیء با لمس انتخاب و
 * با × حذف و با لمس دوم ویرایش می‌شود.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionTextWebSection(
    text: String,
    controller: QuestionEditorFieldController,
    onTextChanged: (String) -> Unit,
    onInsertFigure: () -> Unit,
    onInsertGraph: () -> Unit,
    onInsertTable: () -> Unit,
    onInsertPeriodic: () -> Unit,
    onInsertAnatomy: () -> Unit,
    onInsertPhysics: () -> Unit,
    onInsertChemistry: () -> Unit,
    onEditFigureToken: (String) -> Unit = {},
    onOpenFormula: (text: String, selStart: Int, selEnd: Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val currentText by rememberUpdatedState(text)
    val currentOnText by rememberUpdatedState(onTextChanged)
    val currentOpenFormula by rememberUpdatedState(onOpenFormula)
    val currentEditToken by rememberUpdatedState(onEditFigureToken)

    // V67.0 — وضعیت مکان‌نما و انتخاب:
    // focusedTextSegment: آخرین بخش متنی که کاربر در آن تایپ کرده (محل درج).
    // focusAtOffset: پس از درج/ویرایش توکن، مکان‌نما باید این آفست را باز کند.
    // selectedPartIndex: شیء درون‌متنی انتخاب‌شده (لمس اول = انتخاب، دوم = ویرایش).
    var focusedTextSegment by remember { mutableStateOf<Int?>(null) }
    var focusAtOffset by remember { mutableStateOf<Int?>(null) }
    var postInsertFocus by remember { mutableStateOf<Int?>(null) }
    var selectedPartIndex by remember(text) { mutableStateOf<Int?>(null) }

    // occurrenceها و قطعه‌بندی فقط هنگام تغییر خود متن محاسبه شوند، نه در هر
    // بازترکیب ناشی از تغییر Stateهای دیگر کارت سؤال.
    val formulas = remember(text) { FormulaTextCodec.occurrences(text) }
    val figures = remember(text) { FigureCodec.occurrences(text) }
    val parts = remember(text, formulas, figures) {
        RichTextSplitter.split(text, formulas, figures)
    }
    // V67.0 — بازهٔ آفست هر بخش در متن خام برای درج در محل مکان‌نما.
    val segRanges = remember(text, parts, formulas, figures) {
        RichTextSplitter.segmentSourceRanges(parts, formulas, figures)
    }

    // V67.0 — محل درج = پایان آخرین بخش متنی فعال؛ وگرنه انتهای متن.
    val insertAtOffset = focusedTextSegment
        ?.let { idx -> (parts.getOrNull(idx) as? RichSegment.Text)?.let { segRanges[idx].last + 1 } }
        ?: text.length
    val currentInsertAt by rememberUpdatedState(insertAtOffset)
    val currentFigures by rememberUpdatedState(figures)

    DisposableEffect(controller) {
        controller.nativeInsert = { specJson ->
            val spec = FigureSpec.parse(specJson)
            if (spec != null) {
                val at = currentInsertAt
                currentOnText(FigureCodec.insertAt(currentText, spec, at))
                // مکان‌نما درست بعد از توکن جدید می‌نشیند تا ادامهٔ تایپ ترتیب را حفظ کند.
                focusAtOffset = at + FigureCodec.token(spec).length + 1
                true
            } else {
                false
            }
        }
        controller.nativeReplace = { specJson ->
            val occ = controller.pendingEditOccurrence
            val spec = FigureSpec.parse(specJson)
            if (occ != null && spec != null) {
                currentOnText(FigureCodec.replace(currentText, occ, spec))
                controller.pendingEditOccurrence = null
                val start = currentFigures.getOrNull(occ)?.start
                if (start != null) focusAtOffset = start + FigureCodec.token(spec).length + 1
                true
            } else {
                false
            }
        }
        controller.nativeOpenFormula = {
            val value = currentText
            currentOpenFormula(value, currentInsertAt, currentInsertAt)
            true
        }
        onDispose {
            controller.nativeInsert = null
            controller.nativeReplace = null
            controller.nativeOpenFormula = null
        }
    }

    // V67.1 — پنجرهٔ فرمول متنِ تغییرکرده را برمی‌گرداند؛ مکان‌نما بلافاصله
    // بعد از ناحیهٔ تغییر (بعد از فرمول درج‌شده) می‌نشیند.
    LaunchedEffect(text) {
        controller.pendingCaretOffset?.let { off ->
            focusAtOffset = off
            controller.pendingCaretOffset = null
        }
    }

    // V67.0 — حل مکان‌نمای معلق پس از درج/ویرایش: بخشی که آفست را در بر می‌گیرد.
    val pendingFocusIndex = focusAtOffset?.let { off ->
        parts.indices.firstOrNull { i ->
            parts[i] is RichSegment.Text && segRanges[i].first <= off && off <= segRanges[i].last + 1
        }
    }
    if (pendingFocusIndex != null) {
        postInsertFocus = pendingFocusIndex
        focusAtOffset = null
    }
    val lastTextIndex = parts.indexOfLast { it is RichSegment.Text }
    val autoFocusIndex = (pendingFocusIndex ?: postInsertFocus ?: lastTextIndex)
        .let { if (parts.getOrNull(it) is RichSegment.Text) it else lastTextIndex }
    val caretFocus = remember { FocusRequester() }
    // مکان‌نما بدون لمس قبلی هم پیدا باشد؛ پس از هر درج/حذف توکن دوباره برقرار می‌شود.
    LaunchedEffect(autoFocusIndex, parts.size) {
        if (autoFocusIndex >= 0) runCatching { caretFocus.requestFocus() }
    }

    Column(modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // فرمول پهن‌تر از سطر = «بزرگ»: جعبهٔ تمام‌عرض اسکرول‌شونده، بدون کوچک‌شدن.
            val rowMaxWidth = maxWidth - 8.dp
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                parts.forEachIndexed { index, part ->
                    // key پایدار مانع جابه‌جایی فوکوس BasicTextFieldها هنگام تغییر متن می‌شود.
                    key(index) {
                    val selected = selectedPartIndex == index
                    when (part) {
                        is RichSegment.Text -> {
                            BasicTextField(
                                value = part.text,
                                onValueChange = { newText ->
                                    currentOnText(RichTextSplitter.reconstruct(parts, index, newText))
                                },
                                modifier = Modifier
                                    .widthIn(min = 12.dp)
                                    .heightIn(min = 28.dp)
                                    .then(if (text.isBlank()) Modifier.fillMaxWidth() else Modifier)
                                    .then(
                                        if (index == autoFocusIndex) {
                                            Modifier.focusRequester(caretFocus)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            focusedTextSegment = index
                                            selectedPartIndex = null
                                        }
                                    },
                                textStyle = MaterialTheme.typography.bodyLarge,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) { inner() }
                                }
                            )
                        }
                        is RichSegment.Math -> {
                            val occ = formulas.getOrNull(part.index)
                            val density = LocalDensity.current
                            val mathFontSize = 18.sp
                            val mathPx = with(density) { mathFontSize.toPx() }
                            // اندازهٔ طبیعی فرمول برای تصمیم درجا/اسکرول؛ رندر نهایی همان اندازه را می‌گیرد.
                            val doc = remember(part.tex, mathPx) {
                                NativeMathSvgRenderer.render(tex = part.tex, fontSizePx = mathPx)
                            }
                            val naturalWidth = with(density) { doc.widthPx.toDp() }
                            val naturalHeight = with(density) { doc.heightPx.toDp() }
                            val boxHeight = naturalHeight.coerceAtLeast(36.dp)
                            val bigFormula = naturalWidth > rowMaxWidth
                            Box(
                                modifier = Modifier
                                    .then(
                                        if (bigFormula) {
                                            Modifier.fillMaxWidth().heightIn(min = 36.dp)
                                        } else {
                                            Modifier.height(boxHeight)
                                        }
                                    )
                                    .then(
                                        if (selected) {
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        if (selected) {
                                            selectedPartIndex = null
                                            if (occ != null) {
                                                currentOpenFormula(text, occ.start, occ.endExclusive)
                                            } else {
                                                currentOpenFormula(text, text.length, text.length)
                                            }
                                        } else {
                                            selectedPartIndex = index
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // V67.0 — فرمول با اندازهٔ طبیعی؛ بزرگ‌ها داخل اسکرول افقی جعبهٔ خودشان.
                                NativeFormulaView(
                                    tex = part.tex,
                                    fontSize = mathFontSize,
                                    modifier = if (bigFormula) {
                                        Modifier.fillMaxWidth()
                                    } else {
                                        Modifier.height(naturalHeight)
                                    }
                                )
                                if (selected) {
                                    TokenCloseButton(Modifier.align(Alignment.TopEnd)) {
                                        currentOnText(FormulaTextCodec.delete(currentText, part.index))
                                        selectedPartIndex = null
                                    }
                                }
                            }
                        }
                        is RichSegment.Figure -> {
                            val spec = part.spec
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 80.dp)
                                    .then(
                                        if (selected) {
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        if (selected) {
                                            selectedPartIndex = null
                                            controller.pendingEditOccurrence = part.index
                                            currentEditToken(spec.toJson())
                                        } else {
                                            selectedPartIndex = index
                                        }
                                    }
                            ) {
                                if (spec.kind in setOf("a", "s")) {
                                    AtlasFigureView(
                                        spec = spec,
                                        modifier = Modifier.fillMaxWidth(),
                                        contentDescription = "شکل",
                                        showBlanks = false
                                    )
                                } else {
                                    InlineFigureView(
                                        spec,
                                        Modifier.fillMaxWidth().height(120.dp),
                                        contentDescription = "شکل"
                                    )
                                }
                                if (selected) {
                                    TokenCloseButton(Modifier.align(Alignment.TopEnd)) {
                                        currentOnText(FigureCodec.delete(currentText, part.index))
                                        selectedPartIndex = null
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            NativeToolButton(QuestionToolIcons.Formula, "درج فرمول") { controller.openTool("formula") }
            NativeToolButton(QuestionToolIcons.Figure, "درج شکل", onInsertFigure)
            NativeToolButton(QuestionToolIcons.Graph, "درج نمودار", onInsertGraph)
            NativeToolButton(QuestionToolIcons.Table, "درج جدول", onInsertTable)
            NativeToolButton(QuestionToolIcons.Anatomy, "درج آناتومی بدن", onInsertAnatomy)
            NativeToolButton(QuestionToolIcons.Periodic, "درج جدول تناوبی", onInsertPeriodic)
            NativeToolButton(QuestionToolIcons.Physics, "درج فیزیک", onInsertPhysics)
            NativeToolButton(QuestionToolIcons.Chemistry, "درج شیمی", onInsertChemistry)
        }
    }
}

@Composable
private fun NativeToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/** V67.0 — دکمهٔ × حذف شیء درون‌متنی انتخاب‌شده. */
@Composable
private fun TokenCloseButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(30.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Close,
            contentDescription = "حذف",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}
