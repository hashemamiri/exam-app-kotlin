package ir.exam.app.ui.bank

import android.text.Html
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.exam.app.domain.bank.ReferenceBankContract
import ir.exam.app.ui.math.NativeMathText
import ir.exam.app.domain.bank.ReferenceBankQuestion
import ir.exam.app.domain.bank.ReferenceBankMedia
import ir.exam.app.ui.builder.BankQuestionOption

/**
 * V360.1 — پیش‌نمایش کاملاً بومی (به جای WebView).
 *
 * V360.5 — تصمیم کاربر: پنجرهٔ فیلتر **همه کشویی** و همه روی «همه»؛
 * نیازی به پیمودن گامها نیست. تا پیش از انتخاب پایه، درس فقط «همه»
 * دارد و فصل هم تا انتخاب درس فقط «همه»؛ ولی نوع سؤال و دشواری
 * همیشه همهٔ گزینه ها را دارند. رشته فقط برای پایهٔ ۱۰ تا ۱۲ می آید.
 * با «تأیید» شارد محتوای پایه‌ها خودکار بارگیری و فهرست کامل دیده می‌شود؛
 * آیکن فیلتر همان پنجره را برمی‌گرداند.
 */
private fun plain(html: String): String {
    if (html.isBlank()) return ""
    val raw = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        .replace('\u200c', ' ')
        .replace('\u00a0', ' ')
    return raw.split(' ', '\n', '\t', '\r')
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .trim()
}

/** V360.3 — میزبان مستقل پنجره، برای باز کردن بانک مرجع از سازندهٔ آزمون. */
@Composable
fun ReferenceBankHost(
    onAdd: (List<BankQuestionOption>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember(context.applicationContext) {
        QuestionBankViewModel(context.applicationContext)
    }
    val state by viewModel.state.collectAsState()
    ReferenceBankDialog(
        state = state,
        viewModel = viewModel,
        onAdd = onAdd,
        onDismiss = onDismiss
    )
}

@Composable
internal fun ReferenceBankDialog(
    state: QuestionBankUiState,
    viewModel: QuestionBankViewModel,
    onAdd: (List<BankQuestionOption>) -> Unit,
    onDismiss: () -> Unit
) {
    val preview = state.referencePreview
    var searchOpen by remember { mutableStateOf(false) }
    if (preview != null) {
        ReferenceBankPreview(
            question = preview,
            includeAnswer = state.referenceAnswerOverrides[preview.id] ?: state.referenceIncludeAnswer,
            onAdd = {
                onAdd(listOf(preview.toBankQuestionOption(viewModel.referenceAnswerIncluded(preview.id))))
                viewModel.dismissReferencePreview()
                onDismiss()
            },
            onBack = { viewModel.dismissReferencePreview() }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("بانک سؤال مرجع", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (state.referenceResultsOpen) {
                        IconButton(onClick = { searchOpen = !searchOpen }) {
                            Icon(Icons.Outlined.Search, contentDescription = "جست‌وجو")
                        }
                        IconButton(onClick = viewModel::openReferenceFilter) {
                            Icon(Icons.Outlined.FilterList, contentDescription = "فیلتر")
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.error)
                    }
                }

                if (state.referenceResultsOpen) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("${state.referenceFiltered.size} سؤال · ${state.referencePage + 1}/${state.referencePages} · ${state.referencePickedCount} انتخاب‌شده",
                            style = MaterialTheme.typography.labelSmall, modifier = Modifier.widthIn(min = 118.dp))
                        /* V369 — خواستهٔ کاربر: دکمهٔ تیک متنی شود. */
                        val pageAllPicked = state.referencePageRows.isNotEmpty() &&
                            state.referencePageRows.all { it.id in state.referencePicked }
                        TextButton(onClick = viewModel::toggleReferencePagePicks) {
                            Text(
                                if (pageAllPicked) "لغو انتخاب" else "انتخاب همه",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        IconButton(onClick = viewModel::prevReferencePage, enabled = state.referencePage > 0) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "قبلی")
                        }
                        IconButton(onClick = viewModel::nextReferencePage, enabled = state.referencePage < state.referencePages - 1) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "بعدی")
                        }
                        // V367 — آیکن تنها: دکمهٔ پاسخ در هدر فهرست
                        RefAnswerChip(state.referenceIncludeAnswer) {
                            viewModel.setReferenceIncludeAnswer(!state.referenceIncludeAnswer)
                        }
                        IconButton(
                            onClick = { viewModel.addReferenceMany(onAdd, onDismiss) },
                            enabled = state.referencePickedCount > 0
                        ) { Icon(Icons.Outlined.Add, contentDescription = "افزودن گروهی به آزمون", tint = MaterialTheme.colorScheme.primary) }
                        if (state.referenceDetailsLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    if (searchOpen) {
                        OutlinedTextField(
                            value = state.referenceQuery,
                            onValueChange = viewModel::setReferenceQuery,
                            placeholder = { Text("جست‌وجو در متن سؤال") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.setReferenceQuery("") }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "پاک‌کردن جست‌وجو")
                                }
                            }
                        )
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        // V367 — آیکن تنها: دکمهٔ پاسخ در هدر فیلتر
                        RefAnswerChip(state.referenceIncludeAnswer) {
                            viewModel.setReferenceIncludeAnswer(!state.referenceIncludeAnswer)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = viewModel::applyReferenceFilter,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF218838))
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("تأیید")
                        }
                    }
                }

                state.referenceError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                state.referenceDetailsError?.let { Text("برخی جزئیات بارگیری نشدند؛ اتصال را بررسی کنید.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (state.referenceLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (state.referenceResultsOpen) {
                    ReferenceBankResults(state = state, viewModel = viewModel, onAdd = onAdd, onDismiss = onDismiss)
                } else {
                    ReferenceBankFilter(state = state, viewModel = viewModel, onDismiss = onDismiss)
                }
            }
        }
    }
}

/** V360.5 — یک کشوی ساده و بومی، بدون وابستگی آزمایشی. */
@Composable
private fun RefDropdown(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(options.firstOrNull { it.first == value }?.second ?: "همه", maxLines = 1)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (v, text) ->
                    DropdownMenuItem(
                        text = { Text(text, maxLines = 1) },
                        onClick = { onSelect(v); expanded = false }
                    )
                }
            }
        }
    }
}

/** V369 — دکمهٔ «افزودن پاسخ» مثل سایت متنی است، نه آیکن تنها. */
@Composable
private fun RefAnswerChip(on: Boolean, onToggle: () -> Unit) {
    val tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    TextButton(onClick = onToggle) {
        Icon(
            if (on) Icons.Outlined.Check else Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        Spacer(Modifier.width(4.dp))
        Text("افزودن پاسخ", style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
private fun ReferenceBankFilter(
    state: QuestionBankUiState,
    viewModel: QuestionBankViewModel,
    onDismiss: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val gradeOptions = listOf("0" to "همهٔ پایه‌ها") +
            (ReferenceBankContract.MIN_GRADE..ReferenceBankContract.MAX_GRADE).map { it.toString() to "پایهٔ $it" }
        RefDropdown("پایه", state.referenceGrade.toString(), gradeOptions) { value ->
            viewModel.setReferenceGrade(value.toIntOrNull() ?: 0)
        }
        if (state.referenceTracksVisible) {
            val trackOptions = listOf("" to "همهٔ رشته‌ها") +
                state.referenceTrackOptions.filter { it.isNotBlank() }.map { it to it }
            RefDropdown("رشته", state.referenceTrack, trackOptions, viewModel::setReferenceTrack)
        }
        val subjectOptions = listOf("" to "همهٔ درس‌ها") + state.referenceSubjectOptions.map { it to it }
        RefDropdown("درس", state.referenceSubject, subjectOptions, viewModel::setReferenceSubject)
        val chapterOptions = listOf("" to "همهٔ فصل‌ها") + state.referenceChapterOptions.map { it to it }
        RefDropdown("فصل", state.referenceChapter, chapterOptions, viewModel::setReferenceChapter)
        val typeOptions = listOf("" to "همهٔ نوع‌ها") + state.referenceTypeOptions.map { it to it }
        RefDropdown("نوع سؤال", state.referenceType, typeOptions, viewModel::setReferenceType)
        val difficultyOptions = listOf("" to "همهٔ سطح‌ها") +
            ReferenceBankContract.DIFFICULTY_LABELS.mapIndexed { index, label -> index.toString() to label }
        RefDropdown("دشواری", state.referenceDifficulty?.toString() ?: "", difficultyOptions) { value ->
            viewModel.setReferenceDifficulty(value.toIntOrNull())
        }
        // V369 — خواستهٔ کاربر: تصویردار/بدون تصویر
        RefDropdown(
            "تصویر", state.referenceImageFilter,
            listOf("" to "همه", "1" to "با تصویر", "0" to "بدون تصویر")
        ) { value -> viewModel.setReferenceImageFilter(value) }
        // V369 — خواستهٔ کاربر: تعداد نمایش در صفحه
        RefDropdown(
            "تعداد در صفحه", state.referencePageSize.toString(),
            listOf("25" to "۲۵ سؤال", "50" to "۵۰ سؤال", "75" to "۷۵ سؤال", "100" to "۱۰۰ سؤال")
        ) { value -> viewModel.setReferencePageSize(value.toIntOrNull() ?: 25) }
    }
}

@Composable
private fun ReferenceBankResults(
    state: QuestionBankUiState,
    viewModel: QuestionBankViewModel,
    onAdd: (List<BankQuestionOption>) -> Unit,
    onDismiss: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        if (state.referencePageRows.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (state.referenceCatalogMissing) "فهرست این پایه هنوز بارگذاری نشده است."
                    else "سؤالی با این فیلترها پیدا نشد.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            items(state.referencePageRows, key = { it.id }) { row ->
                val detail = state.referenceDetails[row.id]
                val includeAnswer = state.referenceAnswerOverrides[row.id] ?: state.referenceIncludeAnswer
                androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = row.id in state.referencePicked,
                                onCheckedChange = { viewModel.toggleReferencePick(row.id) }
                            )
                            val meta = listOf(
                                "پایه ${row.grade}", row.subject, row.chapter, row.typeFa,
                                row.difficultyFa, row.area.takeIf { it.isNotBlank() }
                            ).filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")
                            Text(meta, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        }
                        if (detail != null) {
                            NativeMathText(source = detail.displayText, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
                            detail.questionImageUrls.take(8).forEach { url ->
                                RefBankImage(
                                    url = url, contentDescription = "تصویر سؤال",
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            detail.options.forEachIndexed { index, option ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    NativeMathText(source = "${index + 1}) ${option.ifBlank { "گزینه ${index + 1}" }}",
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize, modifier = Modifier.weight(1f))
                                    detail.optionImageList.getOrNull(index)?.takeIf { !it.isNullOrBlank() }?.let { url ->
                                        RefBankImage(
                                            url = url, contentDescription = "تصویر گزینه ${index + 1}",
                                            modifier = Modifier.widthIn(max = 180.dp).heightIn(max = 110.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        } else if (state.referenceDetailsLoading) {
                            // V370 — سه نوار اسکلتون به‌جای اسپینر تک‌خانه.
                            ReferenceBankSkeleton()
                        } else if (row.text.isNotBlank()) {
                            NativeMathText(
                                // V363 — برش امن: نه وسط فرمول، نه نصفه‌کلمه.
                                source = ReferenceBankMedia.displayText(row.text, "", 180),
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize
                            )
                        } else {
                            // V363 — سطر بی‌متن: راهنما، نه کارت سفید.
                            Text(
                                "متن این سؤال در فهرست این پایه نیست؛ دکمهٔ «پیش‌نمایش» متن کامل را نشان می‌دهد.",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // V367 — تراشهٔ پاسخ روی کارت، آیکن تنها
                            IconButton(onClick = { viewModel.toggleReferenceAnswer(row.id) }) {
                                Icon(
                                    if (includeAnswer) Icons.Outlined.Check else Icons.Outlined.Add,
                                    contentDescription = "افزودن پاسخ",
                                    tint = if (includeAnswer) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.previewReference(row) }) {
                                Icon(Icons.Outlined.Visibility, contentDescription = "پیش‌نمایش")
                            }
                            IconButton(onClick = { viewModel.addReference(row, onAdd, onDismiss) }) {
                                Icon(Icons.Outlined.Add, contentDescription = "افزودن به آزمون", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ReferenceBankPreview(
    question: ReferenceBankQuestion,
    includeAnswer: Boolean,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onBack,
        confirmButton = { Button(onClick = onAdd) { Text("➕ افزودن به آزمون") } },
        dismissButton = { TextButton(onClick = onBack) { Text("بازگشت") } },
        title = { Text("پیش‌نمایش سؤال") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    listOf(question.subject, question.chapter, question.typeFa, question.difficultyFa)
                        .filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall
                )
                // V360.9 — تشخیص قابل دیدن شارد کهنه.
                if (question.shardIsStale) {
                    Text(
                        "هشدار: شارد قدیمی است؛ فیلد تصویر گزینه ها وجود ندارد. پنجره را ببندید و دوباره باز کنید.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    val n = question.optionImageList.count { !it.isNullOrBlank() }
                    if (n > 0) {
                        Text("تصویر گزینه ها: $n", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                NativeMathText(
                    source = question.displayText,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                )
                question.questionImageUrls.take(8).forEach { url ->
                    RefBankImage(
                        url = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).padding(vertical = 6.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                if (question.options.isNotEmpty()) {
                    Text("گزینه ها", style = MaterialTheme.typography.labelMedium)
                    question.options.forEachIndexed { i, o ->
                        NativeMathText(
                            source = "${i + 1}) $o",
                            fontSize = MaterialTheme.typography.bodySmall.fontSize
                        )
                        // V360.8 — تصویر گزینه کنار همان گزینه، نه زیر متن سؤال.
                        question.optionImageList.getOrNull(i)?.let { url ->
                            RefBankImage(
                                url = url,
                                contentDescription = "تصویر گزینه ${i + 1}",
                                modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).padding(vertical = 4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                if (includeAnswer) {
                    val answer = plain(question.answerHtml).ifBlank { question.answerText }
                    if (answer.isNotBlank()) {
                        Text("پاسخ / راهنما", style = MaterialTheme.typography.labelMedium)
                        NativeMathText(source = answer, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    }
                    question.answerImageUrls.take(8).forEach { url ->
                        RefBankImage(
                            url = url,
                            contentDescription = "تصویر پاسخ",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).padding(vertical = 6.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    )
}

/**
 * V370 — اسکلتون سطر: سه نوار که ساختار فهرست را تا رسیدن متن نگه می‌دارند
 * (جای دایرهٔ چرخان؛ چشم زودتر فهرست را «آماده» می‌بیند).
 */
@Composable
private fun ReferenceBankSkeleton() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0.92f, 0.74f, 0.47f).forEach { fraction ->
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            )
        }
    }
}
