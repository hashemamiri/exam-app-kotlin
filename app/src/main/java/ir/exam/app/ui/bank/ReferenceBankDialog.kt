package ir.exam.app.ui.bank

import android.text.Html
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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

@Composable
internal fun ReferenceBankDialog(
    state: QuestionBankUiState,
    viewModel: QuestionBankViewModel,
    onAdd: (BankQuestionOption) -> Unit,
    onDismiss: () -> Unit
) {
    val preview = state.referencePreview
    if (preview != null) {
        ReferenceBankPreview(
            question = preview,
            onAdd = {
                onAdd(preview.toBankQuestionOption())
                viewModel.dismissReferencePreview()
                onDismiss()
            },
            onBack = { viewModel.dismissReferencePreview() }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        },
        title = { Text("بانک سؤال مرجع") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(
                    "سؤالها و تصویرها در فضای ابریاند؛ فقط نتیجهٔ پالایششده دانلود میشود.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (state.referenceLoading) {
                    CircularProgressIndicator(Modifier.padding(12.dp))
                    return@Column
                }
                state.referenceError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Text("پایه", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items((ReferenceBankContract.MIN_GRADE..ReferenceBankContract.MAX_GRADE).toList()) { g ->
                        FilterChip(
                            selected = state.referenceGrade == g,
                            onClick = { viewModel.setReferenceGrade(g) },
                            label = { Text(g.toString()) }
                        )
                    }
                }

                Text("رشته", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("", "عمومی", "ریاضی فیزیک", "علوم تجربی", "انسانی", "هنرستان")) { t ->
                        FilterChip(
                            selected = state.referenceTrack == t,
                            onClick = { viewModel.setReferenceTrack(t) },
                            label = { Text(t.ifBlank { "همه" }) }
                        )
                    }
                }

                Text("درس", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("") + state.referenceSubjects) { s ->
                        FilterChip(
                            selected = state.referenceSubject == s,
                            onClick = { viewModel.setReferenceSubject(s) },
                            label = { Text(s.ifBlank { "همه" }) }
                        )
                    }
                }

                Text("فصل", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("") + state.referenceChapters) { c ->
                        FilterChip(
                            selected = state.referenceChapter == c,
                            onClick = { viewModel.setReferenceChapter(c) },
                            label = { Text(c.ifBlank { "همه" }) }
                        )
                    }
                }

                Text("نوع سؤال", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("") + state.referenceTypes) { t ->
                        FilterChip(
                            selected = state.referenceType == t,
                            onClick = { viewModel.setReferenceType(t) },
                            label = { Text(t.ifBlank { "همه" }) }
                        )
                    }
                }

                Text("دشواری", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(null) + (0..4).toList()) { d ->
                        FilterChip(
                            selected = state.referenceDifficulty == d,
                            onClick = { viewModel.setReferenceDifficulty(d) },
                            label = { Text(d?.let { ReferenceBankContract.DIFFICULTY_LABELS[it] } ?: "همه") }
                        )
                    }
                }

                OutlinedTextField(
                    value = state.referenceQuery,
                    onValueChange = viewModel::setReferenceQuery,
                    label = { Text("جستوجو در متن سؤال") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${state.referenceFiltered.size} سؤال · صفحه ${state.referencePage + 1}/${state.referencePages}")
                    Row {
                        TextButton(onClick = viewModel::prevReferencePage) { Text("قبلی") }
                        TextButton(onClick = viewModel::nextReferencePage) { Text("بعدی") }
                    }
                }

                if (state.referencePageRows.isEmpty()) {
                    Text("سؤالی با این فیلترها پیدا نشد.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(state.referencePageRows, key = { it.id }) { row ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text(
                                    listOf(row.subject, row.chapter, row.typeFa, row.difficultyFa)
                                        .filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(row.text.take(180), style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { viewModel.previewReference(row) }) { Text("پیشنمایش") }
                                    Button(onClick = {
                                        viewModel.addReference(row, onAdd)
                                        onDismiss()
                                    }) { Text("افزودن") }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ReferenceBankPreview(
    question: ReferenceBankQuestion,
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
                question.imageUrls.take(8).forEach { url ->
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
                val answer = plain(question.answerHtml).ifBlank { question.answerText }
                if (answer.isNotBlank()) {
                    Text("پاسخ / راهنما", style = MaterialTheme.typography.labelMedium)
                    Text(answer, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}
