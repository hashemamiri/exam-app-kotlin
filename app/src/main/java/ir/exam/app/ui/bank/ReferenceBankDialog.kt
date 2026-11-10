package ir.exam.app.ui.bank

import android.text.Html
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import ir.exam.app.domain.bank.ReferenceBankContract
import ir.exam.app.domain.bank.ReferenceBankQuestion
import ir.exam.app.ui.builder.BankQuestionOption

/**
 * V360.1 — پیشنمایش کاملاً بومی.
 *
 * نسخهٔ نخست V360 از WebView برای نمایش HTML سؤال استفاده میکرد؛
 * پین قدیمی Neumorphic69IntegrationTest بهدرستی جلوی ورود WebView به
 * runtime بومی را گرفت و CI شکست. بهجای شل کردن آن پین، اینجا متن با
 * Html.fromHtml ساده و تصویرها با AsyncImage نمایش داده میشوند.
 *
 * V360.3 — پنجرهٔ فیلتر مرحلهای (پایه ← رشته ← درس ← فصل ← نوع ← دشواری)
 * با تأیید نهایی، فهرست سؤالها و افزودن گروهی. رشته فقط برای پایههای ۱۰ تا ۱۲
 * میآید. پاسخ و تصویر پاسخ فقط با تیک کاربر و فقط در بخش پاسخ کپی میشوند.
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

/** V360.3 — میزبان مستقل پنجره، برای باز کردن بانک مرجع از دکمهٔ + و سازندهٔ آزمون. */
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
    if (preview != null) {
        ReferenceBankPreview(
            question = preview,
            includeAnswer = state.referenceIncludeAnswer,
            onAdd = {
                onAdd(listOf(preview.toBankQuestionOption(state.referenceIncludeAnswer)))
                viewModel.dismissReferencePreview()
                onDismiss()
            },
            onBack = { viewModel.dismissReferencePreview() }
        )
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("بانک سؤال مرجع", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (state.referenceResultsOpen) {
                        IconButton(onClick = viewModel::openReferenceFilter) {
                            Icon(Icons.Outlined.FilterList, contentDescription = "فیلتر")
                        }
                    }
                    TextButton(onClick = onDismiss) { Text("بستن") }
                }
                Text(
                    "سؤالها و تصویرها در فضای ابریاند؛ فقط نتیجهٔ پالایششده دانلود میشود.",
                    style = MaterialTheme.typography.bodySmall
                )
                state.referenceError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (state.referenceLoading) {
                    CircularProgressIndicator(Modifier.padding(12.dp))
                } else if (state.referenceResultsOpen) {
                    ReferenceBankResults(state = state, viewModel = viewModel, onAdd = onAdd, onDismiss = onDismiss)
                } else {
                    ReferenceBankWizard(state = state, viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReferenceBankWizard(state: QuestionBankUiState, viewModel: QuestionBankViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "فیلتر مرحلهای: پایه ← رشته (فقط پایهٔ ۱۰ تا ۱۲) ← درس ← فصل ← نوع سؤال ← دشواری",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.referenceCatalogMissing) {
            Text(
                "فهرست این پایه هنوز روی فضای ابری بارگذاری نشده است؛ فیلترها قابل دیدناند ولی سؤالی نمایش داده نمیشود.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (state.referenceStage >= ReferenceBankContract.STEP_GRADE) {
            Text("۱) پایه را تعیین کنید", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (ReferenceBankContract.MIN_GRADE..ReferenceBankContract.MAX_GRADE).forEach { g ->
                    FilterChip(
                        selected = state.referenceGradeChosen && state.referenceGrade == g,
                        onClick = { viewModel.setReferenceGrade(g) },
                        label = { Text(g.toString(), maxLines = 1) }
                    )
                }
            }
        }

        if (state.referenceStage >= ReferenceBankContract.STEP_TRACK && state.referenceTracksVisible) {
            Text("۲) رشته را انتخاب کنید", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.referenceTrackOptions.forEach { t ->
                    FilterChip(
                        selected = state.referenceTrackChosen && state.referenceTrack == t,
                        onClick = { viewModel.setReferenceTrack(t) },
                        label = { Text(t.ifBlank { "همهٔ رشتهها" }, maxLines = 1) }
                    )
                }
            }
        }

        if (state.referenceStage >= ReferenceBankContract.STEP_SUBJECT) {
            Text("۳) درس", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("") + state.referenceSubjectOptions).forEach { s ->
                    FilterChip(
                        selected = state.referenceSubjectChosen && state.referenceSubject == s,
                        onClick = { viewModel.setReferenceSubject(s) },
                        label = { Text(s.ifBlank { "همهٔ درسها" }, maxLines = 1) }
                    )
                }
            }
        }

        if (state.referenceStage >= ReferenceBankContract.STEP_CHAPTER) {
            Text("۴) فصل", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("") + state.referenceChapterOptions).forEach { c ->
                    FilterChip(
                        selected = state.referenceChapter == c,
                        onClick = { viewModel.setReferenceChapter(c) },
                        label = { Text(c.ifBlank { "همهٔ فصلها" }, maxLines = 1) }
                    )
                }
            }
        }

        if (state.referenceStage >= ReferenceBankContract.STEP_TYPE) {
            Text("۵) نوع سؤال", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("") + state.referenceTypeOptions).forEach { t ->
                    FilterChip(
                        selected = state.referenceType == t,
                        onClick = { viewModel.setReferenceType(t) },
                        label = { Text(t.ifBlank { "همهٔ انواع" }, maxLines = 1) }
                    )
                }
            }
        }

        if (state.referenceStage >= ReferenceBankContract.STEP_DIFFICULTY) {
            Text("۶) دشواری", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf<Int?>(null) + (0..4).toList()).forEach { d ->
                    FilterChip(
                        selected = state.referenceDifficulty == d,
                        onClick = { viewModel.setReferenceDifficulty(d) },
                        label = { Text(d?.let { ReferenceBankContract.DIFFICULTY_LABELS[it] } ?: "همهٔ سطحها", maxLines = 1) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.referenceQuery,
            onValueChange = viewModel::setReferenceQuery,
            label = { Text("جستوجو در متن سؤال (اختیاری)") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.referenceIncludeAnswer,
                onCheckedChange = viewModel::setReferenceIncludeAnswer
            )
            Text("افزودن پاسخ و تصویر پاسخ — فقط در بخش پاسخ", style = MaterialTheme.typography.bodySmall)
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (state.referenceStage > ReferenceBankContract.STEP_GRADE) {
                TextButton(onClick = viewModel::prevReferenceStage) { Text("گام قبل") }
            }
            Spacer(Modifier.weight(1f))
            val last = state.referenceStage == ReferenceBankContract.STEP_DIFFICULTY
            Button(
                onClick = { if (last) viewModel.applyReferenceFilter() else viewModel.nextReferenceStage() },
                enabled = state.referenceStepReady()
            ) { Text(if (last) "تأیید فیلتر و نمایش سؤالها" else "بعدی") }
        }
    }
}

@Composable
private fun ReferenceBankResults(
    state: QuestionBankUiState,
    viewModel: QuestionBankViewModel,
    onAdd: (List<BankQuestionOption>) -> Unit,
    onDismiss: () -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${state.referenceFiltered.size} سؤال · صفحه ${state.referencePage + 1}/${state.referencePages}")
            Spacer(Modifier.weight(1f))
            TextButton(onClick = viewModel::prevReferencePage) { Text("قبلی") }
            TextButton(onClick = viewModel::nextReferencePage) { Text("بعدی") }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = viewModel::toggleReferencePagePicks) { Text("انتخاب/برداشتن صفحه") }
            Spacer(Modifier.weight(1f))
            Text("${state.referencePickedCount} انتخابشده", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = viewModel::clearReferencePicks) { Text("پاک کردن") }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.referenceIncludeAnswer,
                onCheckedChange = viewModel::setReferenceIncludeAnswer
            )
            Text("پاسخ و تصویر پاسخ فقط در بخش پاسخ", style = MaterialTheme.typography.bodySmall)
        }

        if (state.referencePageRows.isEmpty()) {
            Text(
                if (state.referenceCatalogMissing) "فهرست این پایه هنوز بارگذاری نشده است."
                else "سؤالی با این فیلترها پیدا نشد؛ دکمهٔ فیلتر را بزنید.",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.referencePageRows, key = { it.id }) { row ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = row.id in state.referencePicked,
                                onCheckedChange = { viewModel.toggleReferencePick(row.id) }
                            )
                            Text(
                                listOf(row.subject, row.chapter, row.typeFa, row.difficultyFa)
                                    .filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(row.text.take(180), style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { viewModel.previewReference(row) }) { Text("پیشنمایش") }
                            OutlinedButton(onClick = {
                                viewModel.addReference(row, onAdd, onDismiss)
                            }) { Text("افزودن") }
                        }
                    }
                }
            }
        }
        Button(
            onClick = { viewModel.addReferenceMany(onAdd, onDismiss) },
            enabled = state.referencePickedCount > 0,
            modifier = Modifier.fillMaxWidth()
        ) { Text("افزودن گروهی (${state.referencePickedCount}) به آزمون") }
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
        title = { Text("پیشنمایش سؤال") },
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
                Text(plain(question.qHtml).ifBlank { question.text }, style = MaterialTheme.typography.bodyMedium)
                question.questionImageUrls.take(8).forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).padding(vertical = 6.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                if (question.options.isNotEmpty()) {
                    Text("گزینهها", style = MaterialTheme.typography.labelMedium)
                    question.options.forEachIndexed { i, o ->
                        Text("${i + 1}) $o", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (includeAnswer) {
                    val answer = plain(question.answerHtml).ifBlank { question.answerText }
                    if (answer.isNotBlank()) {
                        Text("پاسخ / راهنما", style = MaterialTheme.typography.labelMedium)
                        Text(answer, style = MaterialTheme.typography.bodySmall)
                    }
                    question.answerImageUrls.take(8).forEach { url ->
                        AsyncImage(
                            model = url,
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
