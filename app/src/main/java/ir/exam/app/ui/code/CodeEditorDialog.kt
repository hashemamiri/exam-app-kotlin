package ir.exam.app.ui.code

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * V349 — محیط کدنویسی اپ: ۳۲ زبان، رنگی‌شدن دستورها، شمارهٔ خط،
 * نوار علائم، قطعه‌کدهای آمادهٔ هر زبان و کامل‌کردن خودکار کلیدواژه‌ها.
 *
 * کد اجرا نمی‌شود؛ فقط نوشته و ذخیره می‌شود و تصحیح آن دستی است.
 */

private val EditorBg = Color(0xFF1B2035)
private val EditorLine = Color(0xFF2B3350)
private val EditorInk = Color(0xFFE6E9F5)
private val EditorGutter = Color(0xFF5B668F)

private fun tokenColor(kind: CodeTokens.Kind): Color = when (kind) {
    CodeTokens.Kind.KEYWORD -> Color(0xFFC792EA)
    CodeTokens.Kind.COMMENT -> Color(0xFF6B7A99)
    CodeTokens.Kind.STRING -> Color(0xFF9ECE6A)
    CodeTokens.Kind.NUMBER -> Color(0xFFF78C6C)
    CodeTokens.Kind.TYPE -> Color(0xFF7DCFFF)
    CodeTokens.Kind.FUNCTION -> Color(0xFF82AAFF)
}

/** فقط رنگ می‌افزاید؛ طول متن و جای نشانگر تغییر نمی‌کند. */
private class CodeVisualTransformation(private val lang: CodeLanguage) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val src = text.text
        val builder = AnnotatedString.Builder(src)
        for (span in CodeTokens.spans(src, lang)) {
            if (span.start < span.end && span.end <= src.length) {
                builder.addStyle(SpanStyle(color = tokenColor(span.kind)), span.start, span.end)
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
fun CodeEditorDialog(
    initialLang: String?,
    initialCode: String,
    starterCode: String = "",
    title: String = "محیط کدنویسی",
    doneLabel: String = "ذخیرهٔ کد",
    lockLanguage: Boolean = false,
    onDismiss: () -> Unit,
    onDone: (langId: String, code: String) -> Unit
) {
    var lang by remember { mutableStateOf(CodeLanguages.byIdOr(initialLang)) }
    var picking by remember { mutableStateOf(false) }
    val firstText = when {
        initialCode.isNotEmpty() -> initialCode
        starterCode.isNotEmpty() -> starterCode
        else -> lang.sample
    }
    var field by remember { mutableStateOf(TextFieldValue(firstText, TextRange(firstText.length))) }

    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp, color = EditorInk)
    val caret = field.selection.start.coerceIn(0, field.text.length)
    val suggestions = remember(field.text, caret, lang.id) {
        if (field.selection.start == field.selection.end) CodeText.suggestions(lang.id, field.text, caret)
        else emptyList()
    }
    val lineNo = field.text.substring(0, caret).count { it == '\n' } + 1
    val colNo = caret - (field.text.lastIndexOf('\n', (caret - 1).coerceAtLeast(0)).let { if (caret == 0) -1 else it })

    fun put(piece: String) {
        val (next, at) = CodeText.insert(field.text, field.selection.start, field.selection.end, piece)
        if (next.length <= CodeText.MAX) field = TextFieldValue(next, TextRange(at))
    }

    fun acceptSuggestion(word: String) {
        val (next, at) = CodeText.accept(field.text, field.selection.start, word)
        field = TextFieldValue(next, TextRange(at))
    }

    if (picking) {
        CodeLanguagePickerDialog(
            title = "زبان محیط کدنویسی",
            onDismiss = { picking = false },
            onPick = { picked ->
                val wasSample = field.text == lang.sample
                lang = picked
                if (wasSample) field = TextFieldValue(picked.sample, TextRange(picked.sample.length))
                picking = false
            }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(lang.colorArgb)) {
                        Text(lang.short, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 12.sp, color = Color.White)
                    }
                    Text(lang.name, fontSize = 13.sp)
                    Text("Ln $lineNo, Col $colNo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!lockLanguage) {
                        TextButton(onClick = { picking = true }) { Text("تغییر زبان", fontSize = 12.sp) }
                    }
                }

                val vScroll = rememberScrollState()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 300.dp)
                        .background(EditorBg, RoundedCornerShape(10.dp))
                        .border(1.dp, EditorLine, RoundedCornerShape(10.dp))
                        .verticalScroll(vScroll)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        CodeTokens.lineNumbers(field.text),
                        style = mono,
                        color = EditorGutter,
                        modifier = Modifier.padding(start = 10.dp, end = 8.dp)
                    )
                    Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                        BasicTextField(
                            value = field,
                            onValueChange = { next ->
                                if (next.text.length <= CodeText.MAX) {
                                    field = next
                                }
                            },
                            textStyle = mono,
                            cursorBrush = SolidColor(Color(0xFF7EE787)),
                            visualTransformation = CodeVisualTransformation(lang),
                            modifier = Modifier.widthIn(min = 520.dp).padding(end = 10.dp)
                        )
                    }
                }

                if (suggestions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suggestions) { word ->
                            OutlinedButton(onClick = { acceptSuggestion(word) }) {
                                Text(word, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Text("علائم", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(lang.symbols) { sym ->
                        OutlinedButton(onClick = { put(sym) }, modifier = Modifier.widthIn(min = 40.dp)) {
                            Text(sym, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                    }
                }

                Text("آمادهٔ ${lang.name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(lang.snippets) { snip ->
                        OutlinedButton(onClick = { put(snip.code) }) { Text(snip.label, fontSize = 12.sp) }
                    }
                }

                Text(
                    "کد اجرا نمی‌شود؛ فقط نوشته و ذخیره می‌شود. تصحیح دستی است.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDone(lang.id, field.text) }) { Text(doneLabel) }
                    OutlinedButton(onClick = onDismiss) { Text("انصراف") }
                }
            }
        }
    }
}
