package ir.exam.app.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.exam.app.domain.model.EssayQuestion
import ir.exam.app.ui.code.CodeBlockView
import ir.exam.app.ui.code.CodeEditorDialog
import ir.exam.app.ui.code.CodeLanguagePickerDialog
import ir.exam.app.ui.code.CodeLanguages
import ir.exam.app.ui.code.CodeText

/**
 * V349 — پاسخ کدنویسی دانش‌آموز در اپ.
 *
 * اگر معلم فقط کد خواسته باشد: یک بخش کد.
 * اگر «هر دو» خواسته باشد: کادر توضیح متنی + بخش کد، و هر دو در یک پاسخ
 * ذخیره می‌شوند (متن، سپس بلوک کد) — دقیقاً مثل سایت.
 */
@Composable
fun StudentCodeAnswer(
    question: EssayQuestion,
    current: String,
    onAnswer: (String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var editLang by remember { mutableStateOf(question.codeLang) }
    var editCode by remember { mutableStateOf("") }

    val block = CodeText.parseFence(current)
    val textPart = if (question.codeWithText) CodeText.textPartOf(current) else ""
    val fenceText = if (block != null) CodeText.fence(block.code, block.lang) else ""

    fun start(langId: String, code: String) {
        editLang = langId
        editCode = code
        editing = true
    }

    if (picking) {
        CodeLanguagePickerDialog(
            title = "زبان پاسخ خود را انتخاب کنید",
            onDismiss = { picking = false },
            onPick = { picked ->
                picking = false
                start(picked.id, question.codeStarter)
            }
        )
    }
    if (editing) {
        CodeEditorDialog(
            initialLang = editLang,
            initialCode = editCode,
            starterCode = question.codeStarter,
            title = "پاسخ کدنویسی",
            doneLabel = "ذخیره در پاسخ",
            lockLanguage = question.codeLang.isNotBlank(),
            onDismiss = { editing = false },
            onDone = { langId, code ->
                editing = false
                onAnswer(CodeText.compose(textPart, CodeText.fence(code, langId)))
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (question.codeWithText) {
            Text("۱) توضیح متنی شما", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = textPart,
                onValueChange = { onAnswer(CodeText.compose(it, fenceText)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text("توضیح، استدلال یا نکته‌های خود را اینجا بنویسید…") }
            )
            Text("۲) پاسخ کد شما", style = MaterialTheme.typography.titleSmall)
        }
        if (block != null) {
            CodeBlockView(block.code, block.lang)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { start(block.lang, block.code) }) { Text("ویرایش کد") }
                OutlinedButton(onClick = { onAnswer(CodeText.compose(textPart, "")) }) { Text("حذف کد") }
            }
        } else {
            Text(
                if (question.codeLang.isNotBlank()) {
                    "پاسخ را در محیط کدنویسی " + CodeLanguages.byIdOr(question.codeLang).name + " بنویسید و ذخیره کنید."
                } else {
                    "روی دکمهٔ زیر بزنید، زبان را انتخاب کنید و کد خود را بنویسید."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (question.codeStarter.isNotBlank()) {
                Text(
                    "استاد کد آغازین گذاشته است؛ هنگام باز شدن ویرایشگر نمایش داده می‌شود.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = {
                if (question.codeLang.isNotBlank()) start(question.codeLang, question.codeStarter)
                else picking = true
            }) { Text("</> نوشتن کد") }
        }
    }
}
