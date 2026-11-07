package ir.exam.app.ui.code

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * V349 — نمایش بلوک کد (پاسخ دانش‌آموز یا کد داخل سؤال) به‌صورت رنگی و فقط‌خواندنی.
 * در صفحهٔ آزمون و در تصحیح دستی استفاده می‌شود.
 */

/** رنگ‌آمیزی متن کد؛ طول متن تغییر نمی‌کند. */
fun codeAnnotated(code: String, lang: CodeLanguage): AnnotatedString {
    val builder = AnnotatedString.Builder(code)
    for (span in CodeTokens.spans(code, lang)) {
        if (span.start < span.end && span.end <= code.length) {
            val color = when (span.kind) {
                CodeTokens.Kind.KEYWORD -> Color(0xFFC792EA)
                CodeTokens.Kind.COMMENT -> Color(0xFF6B7A99)
                CodeTokens.Kind.STRING -> Color(0xFF9ECE6A)
                CodeTokens.Kind.NUMBER -> Color(0xFFF78C6C)
                CodeTokens.Kind.TYPE -> Color(0xFF7DCFFF)
                CodeTokens.Kind.FUNCTION -> Color(0xFF82AAFF)
            }
            builder.addStyle(SpanStyle(color = color), span.start, span.end)
        }
    }
    return builder.toAnnotatedString()
}

@Composable
fun CodeBlockView(code: String, langId: String?, modifier: Modifier = Modifier) {
    val lang = CodeLanguages.byIdOr(langId)
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 19.sp)
    Column(
        modifier.fillMaxWidth()
            .background(Color(0xFF1B2035), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF2B3350), RoundedCornerShape(10.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = RoundedCornerShape(6.dp), color = Color(lang.colorArgb)) {
                Text(lang.short, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color.White)
            }
            Text(lang.name, fontSize = 11.sp, color = Color(0xFFB9C2DE))
            Text(
                CodeTokens.lineNumbers(code).count { it == '\n' }.plus(1).toString() + " خط",
                fontSize = 10.sp,
                color = Color(0xFF6B7A99)
            )
        }
        Row(
            Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState())
                .padding(vertical = 6.dp)
        ) {
            Text(
                CodeTokens.lineNumbers(code),
                style = mono,
                color = Color(0xFF5B668F),
                modifier = Modifier.padding(start = 10.dp, end = 8.dp)
            )
            Text(
                codeAnnotated(code, lang),
                style = mono,
                color = Color(0xFFE6E9F5),
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(end = 10.dp)
            )
        }
    }
}
