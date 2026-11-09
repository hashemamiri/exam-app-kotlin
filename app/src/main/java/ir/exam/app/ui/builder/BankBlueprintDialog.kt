package ir.exam.app.ui.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.domain.bank.BankPicker
import ir.exam.app.ui.bank.toPickerItem

/**
 * V358 — نقشهٔ آزمون: «از کدام دسته، با کدام دشواری، چندتا».
 *
 * آینهٔ پنجرهٔ «انتخاب خودکار» در سایت. منطق گزینش در BankPicker
 * مشترک است، پس همان بذر در هر دو یک نتیجه می‌دهد.
 *
 * کمبود هر قاعده جدا گزارش می‌شود تا معلم بداند کدام بخش بانک خالی
 * است؛ هیچ سؤالی دو بار برداشته نمی‌شود.
 */
@Composable
internal fun BankBlueprintDialog(
    items: List<BankQuestionOption>,
    categories: List<BankCategoryOption>,
    onDismiss: () -> Unit,
    onAdd: (List<Long>) -> Unit
) {
    val fa1 = PersianDigits.convert(1)
    val fa2 = PersianDigits.convert(2)
    val fa5 = PersianDigits.convert(5)
    var text by remember {
        mutableStateOf("فصل $fa1 | آسان | $fa5\nفصل $fa2 | متوسط | $fa5\nهمه | دشوار | $fa2")
    }
    var seed by remember { mutableIntStateOf((1..999_999).random()) }

    val cats = remember(categories) {
        categories.map { BankPicker.Category(it.id.toString(), it.name, it.parentId?.toString()) }
    }
    val pickerItems = remember(items) { items.map { it.toPickerItem() } }
    val parsed = BankPicker.parseBlueprint(text, cats)
    val result = BankPicker.pick(pickerItems, cats, parsed.rules, seed)
    val pickedIds = result.picked.mapNotNull { it.id.toLongOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎯 انتخاب خودکار از بانک") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "هر خط: «دسته | دشواری | تعداد». برای همهٔ دسته‌ها «همه» و برای هر سطحی «هر» بنویسید. رقم فارسی هم پذیرفته می‌شود.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    text, { text = it },
                    label = { Text("نقشهٔ آزمون") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                parsed.errors.forEach { err ->
                    Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (parsed.rules.isNotEmpty()) {
                    Text(BankPicker.summary(result), style = MaterialTheme.typography.bodyMedium)
                    result.perRule.forEach { r ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                r.category + " · " +
                                    (if (r.difficulty < 0) "هر" else BankPicker.DIFFICULTY_LABELS[r.difficulty]),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                PersianDigits.convert(r.got) + " از " + PersianDigits.convert(r.wanted) +
                                    if (r.shortage > 0) " (" + PersianDigits.convert(r.shortage) + " کمبود)" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (r.shortage > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { seed = (1..999_999).random() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🎲 قرعهٔ تازه") }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(pickedIds) },
                enabled = pickedIds.isNotEmpty()
            ) {
                Text(
                    if (pickedIds.isEmpty()) "افزودن"
                    else "افزودن (" + PersianDigits.convert(pickedIds.size) + ")"
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
