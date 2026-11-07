package ir.exam.app.ui.builder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ir.exam.app.core.text.CodeBlockCodec
import ir.exam.app.ui.code.CodeEditorDialog
import ir.exam.app.ui.code.CodeLanguagePickerDialog

/**
 * V350 — درج/ویرایش بلوک کد در «هر کادر متنی»: متن سؤال، گزینه‌ها و
 * جفت‌های جورکردنی.
 *
 * رفتار مثل بقیهٔ ابزارها: برای درج تازه اول فهرست زبان‌ها باز می‌شود، بعد
 * محیط کدنویسی؛ برای ویرایش، مستقیم همان کد و زبان باز می‌شود و نتیجه در
 * «همان جای متن» می‌نشیند.
 *
 * کد اجرا نمی‌شود و جایی فرستاده نمی‌شود؛ فقط نوشته و ذخیره می‌شود.
 */
@Composable
internal fun FieldCodeEditor(
    /** occurrenceIndex = null یعنی درج تازه؛ وگرنه ویرایش همان بلوک. */
    occurrenceIndex: Int?,
    initialLang: String,
    initialCode: String,
    source: String,
    onSourceChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val editing = occurrenceIndex != null
    var lang by remember(occurrenceIndex, initialLang) { mutableStateOf(initialLang) }
    var picking by remember(occurrenceIndex, initialLang) {
        mutableStateOf(!editing && initialLang.isBlank())
    }

    if (picking) {
        CodeLanguagePickerDialog(
            title = "زبان کد را انتخاب کنید",
            onDismiss = onDismiss,
            onPick = { picked ->
                lang = picked.id
                picking = false
            }
        )
        return
    }

    CodeEditorDialog(
        initialLang = lang,
        initialCode = initialCode,
        title = if (editing) "ویرایش کد" else "کد در متن",
        doneLabel = if (editing) "ذخیرهٔ کد" else "افزودن کد",
        onDismiss = onDismiss,
        onDone = { pickedLang, code ->
            val next = if (occurrenceIndex == null) {
                CodeBlockCodec.insertAt(source, code, pickedLang, source.length)
            } else {
                val occ = CodeBlockCodec.occurrences(source).getOrNull(occurrenceIndex)
                if (occ != null) CodeBlockCodec.replace(source, occ, code, pickedLang) else source
            }
            if (next != source) onSourceChange(next)
            onDismiss()
        }
    )
}
