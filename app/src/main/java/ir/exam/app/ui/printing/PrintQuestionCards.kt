package ir.exam.app.ui.printing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.ui.math.QuestionToolIcons

/**
 * V88.9 — کارتِ بومیِ سؤال در آزمون‌سازِ چاپی.
 *
 * ظاهر و رفتار از کارتِ آزمون‌سازِ آنلاین گرفته شده: شمارهٔ دایره‌ایِ نئونی،
 * نشانِ نوعِ سؤال با رنگِ پاستلی، بارم، آیکن‌ها، و بازشدنِ آکاردئونی. ولی
 * داده همچنان در `questions` جاوااسکریپتِ صفحه است، پس موتورِ چاپ دست‌نخورده
 * می‌ماند و هر ویرایش از راهِ پل به همان‌جا برمی‌گردد.
 *
 * پنج کنترلِ مخصوصِ آزمونِ آنلاین (حساس‌به‌حروف، نمودارِ پاسخ، تصویرِ پاسخ،
 * خطای مجاز، ذخیره در بانک) عمداً نیستند: روی کاغذ کاری نمی‌کنند و نسخهٔ
 * چاپی داده‌شان را هم ندارد.
 */

/** همان نگاشتِ `QuestionType.pastelColor()` در آزمون‌سازِ آنلاین. */
fun printPastelColor(type: String): Color = when (type) {
    "multiple" -> Color(0xFFAEC6CF)
    "truefalse" -> Color(0xFFB4EEB4)
    "fill" -> Color(0xFFFDFD96)
    "numeric" -> Color(0xFFC3B1E1)
    "matching" -> Color(0xFFFFDAB9)
    else -> Color(0xFFFFD1DC)
}

/** هشت ابزارِ درج، همان‌ها و به همان ترتیبِ کارتِ آنلاین. */
val printInsertTools: List<Triple<String, String, ImageVector>> = listOf(
    Triple(FigureToolRequest.FORMULA, "فرمول", QuestionToolIcons.Formula),
    Triple("figure", "شکل", QuestionToolIcons.Figure),
    Triple("graph", "نمودار", QuestionToolIcons.Graph),
    Triple("table", "جدول", QuestionToolIcons.Table),
    Triple("anatomy", "آناتومی", QuestionToolIcons.Anatomy),
    Triple("periodic", "تناوبی", QuestionToolIcons.Periodic),
    Triple("physics", "فیزیک", QuestionToolIcons.Physics),
    Triple("chemistry", "شیمی", QuestionToolIcons.Chemistry)
)

@Composable
fun PrintQuestionCard(
    detail: PrintQuestionDetail,
    index: Int,
    expanded: Boolean,
    livePreviewHtml: String,
    /** V89.6 — CSSِ صفحه برای اینکه فرمول و شکل درست دیده شوند. */
    livePreviewCss: String,
    onToggle: () -> Unit,
    onEditField: (field: String, value: String) -> Unit,
    onEditOption: (index: Int, field: String, value: String) -> Unit,
    onOptionCount: (action: String, index: Int) -> Unit,
    onEditPair: (index: Int, side: String, value: String) -> Unit,
    onAction: (action: String) -> Unit,
    onOpenTool: (tool: String) -> Unit,
    onOpenImageStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = printPastelColor(detail.type)
    /* V89.3 — تا وقتی کاربر تایپ نکرده، متنِ خوانا نشان داده می‌شود؛ به‌محضِ
       ویرایش، متنِ واقعی (با توکن‌ها) می‌آید تا چیزی گم نشود. */
    var editingText by remember(detail.id) { mutableStateOf(false) }
    var text by remember(detail.id) { mutableStateOf(detail.text) }
    var score by remember(detail.id) { mutableStateOf(detail.score) }
    var answer by remember(detail.id) { mutableStateOf(detail.answer) }
    var lines by remember(detail.id) { mutableStateOf(detail.answerLines?.toString().orEmpty()) }
    var lineHeight by remember(detail.id) {
        mutableStateOf(detail.answerLineHeightCm?.toString().orEmpty())
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // ---- سرصفحه: همیشه یک سطرِ افقی، مثلِ کارتِ آنلاین ----
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(37.dp)
                        .drawBehind {
                            drawCircle(accent.copy(alpha = .30f), radius = size.minDimension * .48f)
                            drawCircle(
                                accent,
                                radius = size.minDimension * .39f,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        PersianDigits.convert(index),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    printQuestionTypeLabel(detail.type),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .background(accent, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                OutlinedTextField(
                    value = score,
                    onValueChange = { score = it; onEditField("score", it) },
                    placeholder = { Text("بارم") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(84.dp)
                )
                IconButton(onClick = { onAction("up") }) {
                    Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "انتقال به بالا")
                }
                IconButton(onClick = { onAction("down") }) {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "انتقال به پایین")
                }
                IconButton(onClick = { onAction("remove") }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "حذف سؤال",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ---- بدنه: با لمسِ کارت باز می‌شود ----
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    OutlinedTextField(
                        value = if (editingText || !detail.hasTokens) text else detail.displayText,
                        onValueChange = {
                            /* نخستین تغییر، متنِ واقعی را می‌آورد تا ویرایشِ
                               کاربر روی نسخهٔ خوانا نوشته نشود. */
                            if (!editingText && detail.hasTokens) {
                                editingText = true
                                text = detail.text
                            } else {
                                text = it
                                onEditField("text", it)
                            }
                        },
                        label = { Text("متن سؤال") },
                        minLines = 3,
                        supportingText = if (detail.hasTokens && !editingText) {
                            { Text("برای ویرایشِ متن، داخلِ کادر بنویسید") }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    /* نمایشِ زنده: همان خروجیِ `renderRichText`ِ صفحه، پس فرمول و
                       شکل دقیقاً همان‌طور دیده می‌شوند که چاپ خواهند شد. */
                    if (livePreviewHtml.isNotBlank()) {
                        ir.exam.app.ui.math.PrintRichTextPreview(
                            html = livePreviewHtml,
                            css = livePreviewCss,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ---- هشت ابزارِ درج + دوربین ----
                    /* V89.2 — آیکنِ برداری، نه متن. همان `QuestionToolIcons`ِ
                       آزمون‌سازِ آنلاین استفاده می‌شود تا هر دو یکی باشند. */
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        printInsertTools.take(4).forEach { (tool, label, icon) ->
                            IconButton(onClick = { onOpenTool(tool) }) {
                                Icon(icon, contentDescription = label)
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        printInsertTools.drop(4).forEach { (tool, label, icon) ->
                            IconButton(onClick = { onOpenTool(tool) }) {
                                Icon(icon, contentDescription = label)
                            }
                        }
                        // دکمهٔ تصویر: استودیوی ویرایشِ تصویر را باز می‌کند
                        IconButton(onClick = onOpenImageStudio) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = "استودیوی تصویر")
                        }
                    }

                    // ---- گزینه‌ها ----
                    if (detail.type == "multiple" || detail.type == "truefalse") {
                        Text("گزینه‌ها", style = MaterialTheme.typography.titleSmall)
                        detail.options.forEachIndexed { i, option ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RadioButton(
                                    selected = option.correct,
                                    onClick = { onEditOption(i, "correct", "true") }
                                )
                                OutlinedTextField(
                                    value = option.text,
                                    onValueChange = { onEditOption(i, "text", it) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                if (detail.type == "multiple") {
                                    IconButton(onClick = { onOptionCount("removeOption", i) }) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "حذف گزینه",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                        if (detail.type == "multiple") {
                            TextButton(onClick = { onOptionCount("addOption", 0) }) {
                                Icon(Icons.Outlined.Add, contentDescription = null)
                                Text("  افزودن گزینه")
                            }
                        }
                    }

                    // ---- جورکردنی ----
                    if (detail.type == "matching") {
                        Text("جفت‌ها", style = MaterialTheme.typography.titleSmall)
                        detail.pairs.forEachIndexed { i, pair ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedTextField(
                                    value = pair.left,
                                    onValueChange = { onEditPair(i, "left", it) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("↔")
                                OutlinedTextField(
                                    value = pair.right,
                                    onValueChange = { onEditPair(i, "right", it) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onOptionCount("removePair", i) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "حذف جفت",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        TextButton(onClick = { onOptionCount("addPair", 0) }) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Text("  افزودن جفت")
                        }
                    }

                    // ---- پاسخِ عددی ----
                    if (detail.type == "numeric") {
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it; onEditField("answer", it) },
                            label = { Text("پاسخ صحیح") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    /* ---- پایانِ کارت: چیدمانِ چاپ ----
                       فضای پاسخ، سطر، فاصلهٔ سطر و ترتیبِ گزینه‌ها — همان‌هایی
                       که نسخهٔ چاپی واقعاً رندر می‌کند. */
                    if (detail.type == "multiple") {
                        Text("ترتیب گزینه‌ها", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "1row" to "یک سطر",
                                "2rows" to "دو سطر",
                                "4rows" to "چهار سطر"
                            ).forEach { (value, label) ->
                                FilterChip(
                                    selected = detail.optionsLayout == value,
                                    onClick = { onEditField("optionsLayout", value) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    if (printTypeHasAnswerSpace(detail.type)) {
                        Text("فضای پاسخ", style = MaterialTheme.typography.titleSmall)
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = lines,
                                onValueChange = { lines = it; onEditField("answerLines", it) },
                                label = { Text("سطر") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lineHeight,
                                onValueChange = {
                                    lineHeight = it; onEditField("answerLineHeightCm", it)
                                },
                                label = { Text("فاصله (cm)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("lined" to "خط‌دار", "plain" to "ساده").forEach { (value, label) ->
                                FilterChip(
                                    selected = detail.answerStyle == value,
                                    onClick = { onEditField("answerStyle", value) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
