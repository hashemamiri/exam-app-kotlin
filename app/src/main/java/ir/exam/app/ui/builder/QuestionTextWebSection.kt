package ir.exam.app.ui.builder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.math.FormulaTextCodec
import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
import ir.exam.app.ui.figure.AtlasFigureView
import ir.exam.app.ui.figure.InlineFigureView
import ir.exam.app.ui.math.NativeFormulaIcon
import ir.exam.app.ui.math.QuestionEditorFieldController
import ir.exam.app.ui.math.QuestionToolIcons

/**
 * V65.0 — کادر متن سؤال کاملاً Native Compose (بدون WebView).
 * نوار ۸ آیکن Native با ترتیب مرجع حفظ شده است. فرمول تمام‌صفحه همچنان
 * FormulaHostDialog است؛ شکل/جدول/اطلس همان ویرایشگرهای Native موجود.
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

    DisposableEffect(controller) {
        controller.nativeInsert = { specJson ->
            val spec = FigureSpec.parse(specJson)
            if (spec == null) {
                false
            } else {
                currentOnText(FigureCodec.insert(currentText, spec))
                true
            }
        }
        controller.nativeReplace = { specJson ->
            val occ = controller.pendingEditOccurrence
            val spec = FigureSpec.parse(specJson)
            if (occ == null || spec == null) {
                false
            } else {
                currentOnText(FigureCodec.replace(currentText, occ, spec))
                controller.pendingEditOccurrence = null
                true
            }
        }
        controller.nativeOpenFormula = {
            val value = currentText
            currentOpenFormula(value, value.length, value.length)
            true
        }
        onDispose {
            controller.nativeInsert = null
            controller.nativeReplace = null
            controller.nativeOpenFormula = null
        }
    }

    val parts = RichTextSplitter.split(text)
    Column(modifier) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            parts.forEachIndexed { index, part ->
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
                                .then(if (text.isBlank()) Modifier.fillMaxWidth() else Modifier),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) { inner() }
                            }
                        )
                    }
                    is RichSegment.Math -> {
                        val occ = FormulaTextCodec.occurrences(text).getOrNull(part.index)
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .widthIn(min = 36.dp)
                                .clickable {
                                    if (occ != null) {
                                        currentOpenFormula(text, occ.start, occ.endExclusive)
                                    } else {
                                        currentOpenFormula(text, text.length, text.length)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            NativeFormulaIcon(part.tex, Modifier.size(84.dp, 36.dp), 18.sp)
                        }
                    }
                    is RichSegment.Figure -> {
                        val spec = part.spec
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp)
                                .clickable {
                                    controller.pendingEditOccurrence = part.index
                                    currentEditToken(spec.toJson())
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
