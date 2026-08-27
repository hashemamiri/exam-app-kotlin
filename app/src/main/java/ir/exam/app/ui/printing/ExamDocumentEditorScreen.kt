package ir.exam.app.ui.printing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.exam.app.core.printing.WordPageLayout
import ir.exam.app.ui.builder.ExamBuilderViewModel
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import ir.exam.app.ui.math.NativeMathText

/**
 * V63.0 — ویرایشگر سند آزمون (Word-مانند)؛ پچ ۱ از ۳.
 *
 * این صفحه عمداً صفحهٔ «ایجاد آزمون» نیست: همهٔ سؤال‌های آزمون پشت‌سرهم روی
 * برگه‌های A4 با اندازهٔ واقعی و صفحه‌بندی خودکار چیده می‌شوند (موتور
 * `WordPageLayout`) و روی کارت هر سؤال یک آیکن مداد است که ویرایش همان سؤال را
 * باز می‌کند.
 *
 * پچ ۱: مداد + صفحه‌بندی/نمایش واقعی + ویرایش متن و بارم.
 * پچ ۲ (بعدی): جابه‌جایی و تغییر اندازهٔ تصویر/شکل/نمودار/جدول با دستگیره.
 * پچ ۳ (بعدی): ویرایش و اندازهٔ متن به سبک ورد (انتخاب بخشی از متن).
 */
@Composable
fun ExamDocumentEditorScreen(
    builder: ExamBuilderViewModel,
    onBack: () -> Unit
) {
    val state by builder.state.collectAsState()
    var zoom by remember { mutableStateOf(1.6f) }
    var editingQuestionId by remember { mutableStateOf<String?>(null) }
    var confirmSave by remember { mutableStateOf(false) }

    val document = remember(state.questions) { WordPageLayout.documentOf(state.questions) }
    val editing = state.questions.firstOrNull { it.id == editingQuestionId }

    Column(Modifier.fillMaxSize().background(Color(0xFFECEFF3))) {
        DocumentEditorTopBar(
            title = state.title.ifBlank { "آزمون" },
            questionCount = state.questions.size,
            pageCount = document.pageCount,
            zoom = zoom,
            saving = state.saving,
            onZoomOut = { zoom = (zoom - 0.2f).coerceIn(0.6f, 3f) },
            onZoomIn = { zoom = (zoom + 0.2f).coerceIn(0.6f, 3f) },
            onSave = { confirmSave = true },
            onBack = onBack
        )

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        state.uploadProgress?.let {
            Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        state.savedCode?.let { code ->
            Text(
                "تغییرات ذخیره شد. کد آزمون: $code",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.questions.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("این آزمون هنوز سؤالی ندارد.")
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(document.pages, key = { it.number }) { page ->
                    WordPageView(
                        title = state.title.ifBlank { "آزمون" },
                        page = page,
                        pageCount = document.pageCount,
                        zoom = zoom,
                        questions = state.questions,
                        editingQuestionId = editingQuestionId,
                        onEditQuestion = { editingQuestionId = it }
                    )
                }
            }
        }
    }

    editing?.let { question ->
        QuestionTextEditorDialog(
            question = question,
            row = document.pages.flatMap { page -> page.blocks }
                .firstOrNull { it.questionId == question.id }?.row ?: 1,
            onDismiss = { editingQuestionId = null },
            onApply = { text, score ->
                builder.updateText(question.id, text)
                builder.updateScore(question.id, score)
                editingQuestionId = null
            }
        )
    }

    if (confirmSave) {
        AlertDialog(
            onDismissRequest = { confirmSave = false },
            title = { Text("تأیید ذخیره تغییرات") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("همهٔ تغییرات این سند روی همان آزمون ذخیره می‌شود.")
                    Text(
                        "سرور فقط سؤال‌های مشمول تغییر را محاسبه می‌کند. سقف این ذخیره " +
                            state.maximumChargeToman.toTomanText() + " تومان است."
                    )
                    Text("ذخیره و کسر موجودی در یک تراکنش انجام می‌شود؛ اگر ذخیره شکست بخورد، مبلغی کم نخواهد شد.")
                }
            },
            confirmButton = {
                Button(onClick = { confirmSave = false; builder.save() }) { Text("تأیید و ذخیره") }
            },
            dismissButton = { TextButton(onClick = { confirmSave = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun DocumentEditorTopBar(
    title: String,
    questionCount: Int,
    pageCount: Int,
    zoom: Float,
    saving: Boolean,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.Close, contentDescription = "بستن ویرایشگر سند")
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    "$questionCount سؤال · $pageCount صفحهٔ A4",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (saving) CircularProgressIndicator(Modifier.height(18.dp).width(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onZoomOut) {
                    Icon(Icons.Outlined.Remove, contentDescription = "کوچک‌نمایی")
                }
                Text("${(zoom * 100).toInt()}٪", style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = onZoomIn) {
                    Icon(Icons.Outlined.Add, contentDescription = "بزرگ‌نمایی")
                }
            }
            Button(onClick = onSave) { Text("ذخیره") }
        }
    }
}

@Composable
private fun WordPageView(
    title: String,
    page: WordPageLayout.WordPage,
    pageCount: Int,
    zoom: Float,
    questions: List<QuestionDraft>,
    editingQuestionId: String?,
    onEditQuestion: (String) -> Unit
) {
    Card(
        Modifier
            .width(WordPageLayout.mmToDp(WordPageLayout.PAGE_WIDTH_MM, zoom).dp)
            .height(WordPageLayout.mmToDp(WordPageLayout.PAGE_HEIGHT_MM, zoom).dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(WordPageLayout.mmToDp(WordPageLayout.MARGIN_MM, zoom).dp)
        ) {
            // سرصفحهٔ سند
            Text(
                title,
                fontSize = (11 * zoom).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(WordPageLayout.mmToDp(3f, zoom).dp))
            androidx.compose.material3.HorizontalDivider()
            Spacer(Modifier.height(WordPageLayout.mmToDp(3f, zoom).dp))

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WordPageLayout.mmToDp(WordPageLayout.BLOCK_GAP_MM, zoom).dp)
            ) {
                page.blocks.forEach { block ->
                    val question = questions.firstOrNull { it.id == block.questionId } ?: return@forEach
                    WordQuestionBlock(
                        block = block,
                        question = question,
                        zoom = zoom,
                        highlighted = editingQuestionId == question.id,
                        onEdit = { onEditQuestion(question.id) }
                    )
                }
            }

            // پاصفحهٔ سند
            androidx.compose.material3.HorizontalDivider()
            Text(
                "صفحهٔ ${page.number} از $pageCount",
                fontSize = (9 * zoom).sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF666666),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WordQuestionBlock(
    block: WordPageLayout.WordBlock,
    question: QuestionDraft,
    zoom: Float,
    highlighted: Boolean,
    onEdit: () -> Unit
) {
    val fontSize = (question.fontSizeSp.coerceIn(8f, 30f) * zoom * 0.75f).sp
    Column(
        Modifier
            .fillMaxWidth()
            .height(WordPageLayout.mmToDp(block.heightMm, zoom).dp)
            .then(
                if (highlighted) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary)
                } else Modifier
            )
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "سؤال ${block.row}     (${scoreText(question.score)} نمره)",
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "ویرایش سؤال ${block.row}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        NativeMathText(
            source = question.text,
            fontSize = fontSize,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )

        when (question.type) {
            QuestionType.MULTIPLE_CHOICE -> question.options.forEachIndexed { index, option ->
                Row(Modifier.fillMaxWidth().padding(top = (1 * zoom).dp)) {
                    Text("${index + 1}) ", fontSize = fontSize, fontWeight = FontWeight.Bold)
                    NativeMathText(source = option, fontSize = fontSize, textAlign = TextAlign.Right)
                }
            }
            QuestionType.TRUE_FALSE -> Row(
                Modifier.fillMaxWidth().padding(top = (1 * zoom).dp),
                horizontalArrangement = Arrangement.spacedBy((10 * zoom).dp)
            ) {
                Text("○ صحیح", fontSize = fontSize)
                Text("○ غلط", fontSize = fontSize)
            }
            QuestionType.MATCHING -> {
                val rows = maxOf(question.matchingLeft.size, question.matchingRight.size)
                repeat(rows) { index ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = (1 * zoom).dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NativeMathText(
                            source = question.matchingRight.getOrNull(index).orEmpty(),
                            fontSize = fontSize,
                            modifier = Modifier.weight(1f)
                        )
                        Text("↔", fontSize = fontSize)
                        NativeMathText(
                            source = question.matchingLeft.getOrNull(index).orEmpty(),
                            fontSize = fontSize,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            else -> Unit
        }

        question.images.forEach { media ->
            AsyncImage(
                model = media.uri,
                contentDescription = "تصویر سؤال",
                modifier = Modifier
                    .width(WordPageLayout.mmToDp(media.widthMm.coerceIn(5f, 182f), zoom).dp)
                    .height(WordPageLayout.mmToDp(media.widthMm.coerceIn(5f, 182f) * 0.6f, zoom).dp)
                    .padding(top = WordPageLayout.mmToDp(WordPageLayout.MEDIA_GAP_MM / 2f, zoom).dp)
            )
        }

        if (question.type == QuestionType.ESSAY && question.answerLineStyle == "lined") {
            repeat(question.answerLines.coerceIn(0, 40)) {
                Spacer(Modifier.height(WordPageLayout.mmToDp(WordPageLayout.ANSWER_LINE_HEIGHT_MM, zoom).dp))
                androidx.compose.material3.HorizontalDivider(color = Color(0xFFB9C2CC))
            }
        }
    }
}

@Composable
private fun QuestionTextEditorDialog(
    question: QuestionDraft,
    row: Int,
    onDismiss: () -> Unit,
    onApply: (text: String, score: String) -> Unit
) {
    var text by remember(question.id, question.text) { mutableStateOf(question.text) }
    var score by remember(question.id, question.score) { mutableStateOf(scoreText(question.score)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش سؤال $row") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("متن سؤال") },
                    minLines = 4,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = score,
                    onValueChange = { value -> score = value.filter { it.isDigit() || it == '.' } },
                    label = { Text("بارم") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "فرمول‌های \$...\$ و توکن‌های %%FIG:...%% بدون تغییر حفظ می‌شوند. " +
                        "در پچ‌های بعدی جابه‌جایی/تغییر اندازهٔ شکل‌ها و اندازهٔ بخشی از متن هم اضافه می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { Button(onClick = { onApply(text, score) }) { Text("اعمال") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

/** بارم بدون نقطهٔ اضافی: ۲ یا ۱٫۵ */
private fun scoreText(score: Double): String =
    if (score % 1.0 == 0.0) score.toInt().toString() else score.toString()

private fun Long.toTomanText(): String = "%,d".format(java.util.Locale.US, this)
