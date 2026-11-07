package ir.exam.app.ui.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.exam.app.domain.model.QuestionType
import ir.exam.app.ui.code.CodeEditorDialog
import ir.exam.app.ui.code.CodeLanguagePickerDialog
import ir.exam.app.ui.code.CodeLanguages

/**
 * V349 — بخش «شکل پاسخ» برای سؤال تشریحی در اپ (همهٔ آزمون‌ها، نه فقط ابتدایی).
 *
 * دو کلید مستقل: «متنی» و «کدنویسی». می‌توان هر دو را با هم روشن کرد؛
 * در آن حالت دانش‌آموز هم کادر توضیح می‌بیند و هم محیط کد.
 * دست‌کم یکی همیشه روشن می‌ماند.
 */
@Composable
internal fun BuilderCodeSection(question: QuestionDraft, viewModel: ExamBuilderViewModel) {
    if (question.type != QuestionType.ESSAY) return
    val codeOn = question.mode == "code"
    val textOn = !codeOn || question.codeWithText
    var picking by remember { mutableStateOf(false) }
    var editingStarter by remember { mutableStateOf(false) }

    if (picking) {
        CodeLanguagePickerDialog(
            title = "زبان پاسخ را تعیین کنید",
            onDismiss = { picking = false },
            onPick = { picked ->
                viewModel.setQuestionCodeLang(question.id, picked.id)
                picking = false
            }
        )
    }
    if (editingStarter) {
        CodeEditorDialog(
            initialLang = question.codeLang,
            initialCode = question.codeStarter,
            title = "کد آغازین برای دانش‌آموز",
            doneLabel = "ذخیرهٔ کد آغازین",
            onDismiss = { editingStarter = false },
            onDone = { _, code ->
                viewModel.setQuestionCodeStarter(question.id, code)
                editingStarter = false
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("شکل پاسخ:", style = MaterialTheme.typography.titleSmall)
                if (textOn) {
                    Button(onClick = {
                        if (codeOn) viewModel.setQuestionCodeWithText(question.id, false)
                    }) { Text("متنی", fontSize = 12.sp) }
                } else {
                    OutlinedButton(onClick = {
                        viewModel.setQuestionCodeWithText(question.id, true)
                    }) { Text("متنی", fontSize = 12.sp) }
                }
                if (codeOn) {
                    Button(onClick = {
                        if (textOn) viewModel.setQuestionMode(question.id, "")
                    }) { Text("</> کدنویسی", fontSize = 12.sp) }
                } else {
                    OutlinedButton(onClick = {
                        viewModel.setQuestionMode(question.id, "code")
                        if (textOn) viewModel.setQuestionCodeWithText(question.id, true)
                    }) { Text("</> کدنویسی", fontSize = 12.sp) }
                }
            }
            if (codeOn && textOn) {
                Text(
                    "هر دو: دانش‌آموز هم توضیح می‌نویسد و هم کد.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (codeOn) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("زبان دانش‌آموز:", fontSize = 12.sp)
                    OutlinedButton(onClick = { picking = true }) {
                        Text(
                            if (question.codeLang.isNotBlank()) CodeLanguages.byIdOr(question.codeLang).name + " (ثابت)"
                            else "آزاد — خودش انتخاب می‌کند",
                            fontSize = 12.sp
                        )
                    }
                    if (question.codeLang.isNotBlank()) {
                        OutlinedButton(onClick = { viewModel.setQuestionCodeLang(question.id, "") }) {
                            Text("برداشتن", fontSize = 12.sp)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { editingStarter = true }) {
                        Text(if (question.codeStarter.isBlank()) "کد آغازین (اختیاری)" else "ویرایش کد آغازین", fontSize = 12.sp)
                    }
                    if (question.codeStarter.isNotBlank()) {
                        OutlinedButton(onClick = { viewModel.setQuestionCodeStarter(question.id, "") }) {
                            Text("حذف", fontSize = 12.sp)
                        }
                    }
                }
                Text(
                    "کد اجرا نمی‌شود؛ تصحیح مثل سؤال تشریحی دستی است.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
