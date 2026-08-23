package ir.exam.app.ui.builder

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ir.exam.app.ui.math.QuestionEditorFieldController
import ir.exam.app.ui.math.QuestionTextFieldWebView
import ir.exam.app.ui.math.QuestionToolIcons

/**
 * V53.1 — کادر متن سؤال WebView به‌جای کادر Native قبلی + نوار ۸ آیکن Native.
 *
 * ترتیب آیکن‌ها مطابق مرجع: فرمول، شکل، نمودار، جدول، آناتومی بدن، جدول تناوبی،
 * فیزیک، شیمی. همهٔ آیکن‌ها Native (ImageVector) هستند؛ فقط «کادر متن سؤال» و
 * «ویرایشگر فرمول» WebView می‌مانند (استثنای صریح کاربر). جدول (V53.1) و جدول
 * تناوبی (V53.2) ویرایشگر کاملاً Native دارند؛ شکل/نمودار ویرایشگرهای Native
 * موجود V45.3 را باز می‌کنند؛ آناتومی/فیزیک/شیمی تا تحویل V53.3 ابزار مرجع
 * داخل همین صفحه را باز می‌کنند.
 */
@Composable
fun QuestionTextWebSection(
    text: String,
    controller: QuestionEditorFieldController,
    onTextChanged: (String) -> Unit,
    onInsertFigure: () -> Unit,
    onInsertGraph: () -> Unit,
    onInsertTable: () -> Unit,
    onInsertPeriodic: () -> Unit,
    onInsertAnatomy: () -> Unit,
    onInsertPhysics: () -> Unit,
    onInsertChemistry: () -> Unit,
    onEditFigureToken: (String) -> Unit = {},
    onOpenFormula: (text: String, selStart: Int, selEnd: Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var overlayOpen by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }

    // V54.4 — دکمهٔ بازگشت سیستم ابتدا لایهٔ تمام‌صفحهٔ باز مرجع را می‌بندد.
    BackHandler(enabled = overlayOpen) { controller.closeOverlays() }

    // همگام‌سازی تغییرهای بیرونی (مثلاً افزودن از بانک سؤال) به WebView بدون echo.
    LaunchedEffect(text) {
        if (text != controller.lastJsValue) controller.setValue(text)
    }

    Column(modifier.animateContentSize()) {
        // V54.4 — هیچ قاب/برچسب Compose دور WebView نیست؛ برچسب «متن سؤال» و
        // قاب کادر همان markup و CSS بایت‌به‌بایت مرجع داخل خود HTML است.
        QuestionTextFieldWebView(
            controller = controller,
            initialValue = text,
            onValueChanged = onTextChanged,
            onOverlayChanged = { overlayOpen = it },
            onEditFigureToken = onEditFigureToken,
            onOpenFormula = onOpenFormula,
            onError = { loadError = true },
            modifier = Modifier
                .fillMaxWidth()
                // ابزارهای تمام‌صفحهٔ مرجع (آناتومی/تناوبی/فیزیک/شیمی) داخل همین
                // WebView باز می‌شوند؛ هنگام بازبودن، ارتفاع بیشتر می‌شود.
                .height(if (overlayOpen) 560.dp else 320.dp)
        )
        if (loadError) {
            Text(
                "ویرایشگر متن سؤال بارگیری نشد؛ دستگاه را دوباره امتحان کنید.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
        // آیکن‌های درج فقط زیر کادر متن سؤال هستند؛ نوار داخلی HTML مخفی است.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            NativeToolButton(QuestionToolIcons.Formula, "درج فرمول") { controller.openTool("formula") }
            NativeToolButton(QuestionToolIcons.Figure, "درج شکل", onInsertFigure)
            NativeToolButton(QuestionToolIcons.Graph, "درج نمودار", onInsertGraph)
            NativeToolButton(QuestionToolIcons.Table, "درج جدول", onInsertTable)
            NativeToolButton(QuestionToolIcons.Anatomy, "درج آناتومی بدن", onInsertAnatomy)
            NativeToolButton(QuestionToolIcons.Periodic, "درج جدول تناوبی", onInsertPeriodic)
            NativeToolButton(QuestionToolIcons.Physics, "درج فیزیک", onInsertPhysics)
            NativeToolButton(QuestionToolIcons.Chemistry, "درج شیمی", onInsertChemistry)
        }
    }
}

@Composable
private fun NativeToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
