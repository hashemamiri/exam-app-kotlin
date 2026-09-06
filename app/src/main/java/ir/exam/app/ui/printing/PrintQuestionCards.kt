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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

/** هشت ابزارِ درج، همان‌ها و به همان ترتیبِ کارتِ آنلاین.
 *  V90 — فقط مرجع: خودِ `QuestionTextWebSection` این هشت ابزار را رندر می‌کند
 *  و کارت دیگر ردیفِ تکراریِ آن‌ها را نشان نمی‌دهد. */
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

/* V90 — مدل و تجزیهٔ پلِ کارتِ بومی. این‌ها قبلاً در `PrintQuestionEditorSheet.kt`
   بودند؛ با حذفِ آن پنجرهٔ مرده، مدلِ مشترک به همین‌جا منتقل شد تا کارت و
   دیالوگ همچنان از آن استفاده کنند. */

/** یک گزینهٔ چندگزینه‌ای یا صحیح/غلط. */
data class PrintOptionRow(val text: String, val correct: Boolean)

/** یک جفتِ جورکردنی. */
data class PrintPairRow(val left: String, val right: String)

/** عکسِ فوریِ یک سؤال، همان‌طور که پل می‌دهد. */
data class PrintQuestionDetail(
    val id: String = "",
    val type: String = "long",
    val text: String = "",
    val score: String = "",
    val optionsLayout: String = "2rows",
    val answerLines: Int? = null,
    val answerStyle: String = "lined",
    val answerLineHeightCm: Double? = null,
    val answer: String = "",
    /** V89.3 — متنِ خوانا برای نمایش؛ توکن‌ها با نشانهٔ کوتاه جایگزین شده‌اند. */
    val displayText: String = "",
    /** آیا متن شیءِ درج‌شده دارد؟ */
    val hasTokens: Boolean = false,
    val options: List<PrintOptionRow> = emptyList(),
    val pairs: List<PrintPairRow> = emptyList()
)

private val detailJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** خروجیِ `__qmfQuestionDetail` را می‌خواند. `{}` یعنی سؤال پیدا نشد. */
fun parsePrintQuestionDetail(raw: String?): PrintQuestionDetail? {
    val body = raw?.trim().orEmpty()
    if (body.isEmpty() || body == "{}" || body == "\"{}\"") return null
    return runCatching {
        val o: JsonObject = detailJson.parseToJsonElement(body).jsonObject
        fun str(k: String) = o[k]?.jsonPrimitive?.contentOrNull.orEmpty()
        PrintQuestionDetail(
            id = str("id"),
            type = str("type").ifBlank { "long" },
            text = str("text"),
            score = str("score"),
            optionsLayout = str("optionsLayout").ifBlank { "2rows" },
            answerLines = o["answerLines"]?.jsonPrimitive?.intOrNull,
            answerStyle = str("answerStyle").ifBlank { "lined" },
            answerLineHeightCm = o["answerLineHeightCm"]?.jsonPrimitive?.doubleOrNull,
            answer = str("answer"),
            displayText = str("displayText"),
            hasTokens = o["hasTokens"]?.jsonPrimitive?.booleanOrNull ?: false,
            options = o["options"]?.jsonArray?.map { el ->
                val it = el.jsonObject
                PrintOptionRow(
                    text = it["text"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    correct = it["correct"]?.jsonPrimitive?.booleanOrNull ?: false
                )
            }.orEmpty(),
            pairs = o["pairs"]?.jsonArray?.map { el ->
                val it = el.jsonObject
                PrintPairRow(
                    left = it["left"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    right = it["right"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )
            }.orEmpty()
        )
    }.getOrNull()
}

/**
 * V89.2 — کلِ فهرستِ سؤال‌ها از یک فراخوانی. هر عضو همان شکلی است که
 * `parsePrintQuestionDetail` می‌فهمد، پس منطقِ تجزیه یکی می‌ماند.
 */
fun parsePrintQuestionList(raw: String?): List<PrintQuestionDetail> {
    val body = raw?.trim().orEmpty()
    if (body.isEmpty() || body == "[]" || body == "\"[]\"") return emptyList()
    return runCatching {
        detailJson.parseToJsonElement(body).jsonArray.mapNotNull { el ->
            parsePrintQuestionDetail(el.toString())
        }
    }.getOrDefault(emptyList())
}

/** برچسبِ فارسیِ هر نوع سؤال. */
fun printQuestionTypeLabel(type: String): String = when (type) {
    "multiple" -> "چندگزینه‌ای"
    "truefalse" -> "صحیح/غلط"
    "fill" -> "جای خالی"
    "numeric" -> "عددی"
    "matching" -> "جورکردنی"
    else -> "تشریحی"
}

/** آیا این نوع، فضای پاسخ دارد؟ */
fun printTypeHasAnswerSpace(type: String): Boolean = type == "long" || type == "fill"

@Composable
fun PrintQuestionCard(
    detail: PrintQuestionDetail,
    index: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEditField: (field: String, value: String) -> Unit,
    onEditOption: (index: Int, field: String, value: String) -> Unit,
    onOptionCount: (action: String, index: Int) -> Unit,
    onEditPair: (index: Int, side: String, value: String) -> Unit,
    onAction: (action: String) -> Unit,
    onOpenTool: (tool: String, cursor: Int) -> Unit,
    onOpenImageStudio: () -> Unit,
    /* V90 — لمسِ دوم روی یک شکلِ درون‌متنی: مشخصاتِ توکن و بازهٔ آن برای
       بازکردنِ همان پنجرهٔ بومی در حالتِ ویرایش. */
    onEditFigure: (specJson: String, occurrenceIndex: Int, start: Int, end: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = printPastelColor(detail.type)
    /* V89.8 — کنترلرِ همان بخشِ متنِ آنلاین؛ درجِ بومی از راهِ آن انجام
       می‌شود و محلِ مکان‌نما را خودش نگه می‌دارد. */
    val fieldController = remember(detail.id) {
        ir.exam.app.ui.math.QuestionEditorFieldController()
    }
    var text by remember(detail.id) { mutableStateOf(detail.text) }
    var score by remember(detail.id) { mutableStateOf(detail.score) }
    /* V90 — متنِ کارت حالتِ «کنترل‌شده» دارد: خودِ کارت آن را نگه می‌دارد و
       وقتی درج/ویرایشِ اشیاء از بیرون متنِ صفحه را عوض کرد، از روی
       `detail.text` هم‌گام می‌شود. */
    LaunchedEffect(detail.text) { text = detail.text }
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

                    /* V89.8 — همان بخشِ متنِ آزمون‌سازِ آنلاین. اشیاء
                       **درونِ** کادر و در محلِ خودشان دیده می‌شوند، نه به‌صورت
                       یک پیش‌نمایشِ جدا زیرِ کادر (گزارشِ کاربر). قالبِ توکن در
                       هر دو سازنده یکی است (`%%FIG:{json}%%`)، پس این بخش
                       بدونِ تغییر کار می‌کند و `QuestionTextWebSection` هیچ
                       وابستگی‌ای به `ExamBuilderViewModel` ندارد. */
                    ir.exam.app.ui.builder.QuestionTextWebSection(
                        text = text,
                        controller = fieldController,
                        onTextChanged = { value ->
                            text = value
                            onEditField("text", value)
                        },
                        onInsertFigure = { off -> onOpenTool("figure", off) },
                        onInsertGraph = { off -> onOpenTool("graph", off) },
                        onInsertTable = { off -> onOpenTool("table", off) },
                        onInsertPeriodic = { off -> onOpenTool("periodic", off) },
                        onInsertAnatomy = { off -> onOpenTool("anatomy", off) },
                        onInsertPhysics = { off -> onOpenTool("physics", off) },
                        onInsertChemistry = { off -> onOpenTool("chemistry", off) },
                        onOpenFormula = { _, selStart, _ -> onOpenTool(FigureToolRequest.FORMULA, selStart) },
                        onEditFigureToken = { specJson, occurrenceIndex, start, end ->
                            onEditFigure(specJson, occurrenceIndex, start, end)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    /* V90 — نوارِ تکراریِ هشت‌ابزار حذف شد: `QuestionTextWebSection`
                       خودش همان هشت ابزار را دارد و درجِ آن‌ها با محلِ مکان‌نما
                       انجام می‌شود. فقط دکمهٔ دوربین (استودیوی تصویر) می‌ماند. */
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                    IconButton(onClick = { onOptionCount("remove", i) }) {
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
                            TextButton(onClick = { onOptionCount("add", 0) }) {
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
