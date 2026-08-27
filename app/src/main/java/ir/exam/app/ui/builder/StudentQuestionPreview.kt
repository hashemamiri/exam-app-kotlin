package ir.exam.app.ui.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.exam.app.ui.math.NativeMathText

/**
 * V62.7 — پیش‌نمایش دانش‌آموزی سؤال (درخواست کاربر): لمس چشم روی کارت سؤال
 * «فقط» همین پنجره را باز می‌کند؛ سؤال دقیقاً به شکلی نمایش داده می‌شود که
 * دانش‌آموز هنگام آزمون می‌بیند (گزینه‌ها، صحیح/غلط، جای خالی، عددی،
 * جورکردنی و پاسخ تشریحی) — فقط نمایشی و غیرفعال.
 */
@Composable
fun StudentQuestionPreviewDialog(question: QuestionDraft, number: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("پیش‌نمایش دانش‌آموز") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "سؤال $number (${question.score} نمره)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        NativeMathText(question.text.ifBlank { "متن سؤال" })
                        question.images.forEach { media ->
                            AsyncImage(media.uri, "تصویر سؤال", Modifier.fillMaxWidth())
                        }
                        when (question.type) {
                            QuestionType.MULTIPLE_CHOICE -> question.options.forEachIndexed { index, option ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = false, onClick = null)
                                    NativeMathText(option.ifBlank { "گزینه ${index + 1}" })
                                    question.optionImages.getOrNull(index)?.let {
                                        AsyncImage(it, "تصویر گزینه", Modifier.size(72.dp))
                                    }
                                }
                            }
                            QuestionType.TRUE_FALSE -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = false, onClick = {}, label = { Text("صحیح") })
                                FilterChip(selected = false, onClick = {}, label = { Text("غلط") })
                            }
                            QuestionType.FILL_BLANK -> OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                enabled = false,
                                label = { Text("پاسخ جای خالی") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            QuestionType.NUMERIC -> OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                enabled = false,
                                label = { Text("پاسخ عددی") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            QuestionType.MATCHING -> question.matchingLeft.forEachIndexed { leftIndex, left ->
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(9.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    NativeMathText("${leftIndex + 1}. ${left.ifBlank { "ستون راست" }}")
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        question.matchingRight.indices.forEach { rightIndex ->
                                            FilterChip(
                                                selected = false,
                                                onClick = {},
                                                label = { Text(("الف ب پ ت ث ج چ ح".split(" ")).getOrElse(rightIndex) { "${rightIndex + 1}" }) }
                                            )
                                        }
                                    }
                                }
                            }
                            QuestionType.ESSAY -> Column(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF6F6F6), RoundedCornerShape(9.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(9.dp))
                                    .padding(12.dp)
                            ) {
                                Text("محل پاسخ تشریحی دانش‌آموز", color = Color.Gray)
                            }
                        }
                        if (question.answerImageMode != "no") {
                            Text(
                                "دانش‌آموز می‌تواند تا ${question.maxAnswerImages.coerceAtLeast(1)} عکس پاسخ بفرستد.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (question.allowAnswerGraph) {
                            Text("رسم نمودار پاسخ برای دانش‌آموز فعال است.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("بستن") } }
    )
}
