package ir.exam.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * رشته‌های تحصیلی استاندارد دورهٔ دوم متوسطه و شاخه‌های فنی.
 * ترتیب عمداً با ترتیب رایج مدارس یکی است تا انتخاب سریع باشد.
 */
val StandardFieldsOfStudy: List<String> = listOf(
    "ریاضی فیزیک",
    "علوم تجربی",
    "ادبیات و علوم انسانی",
    "علوم و معارف اسلامی",
    "فنی و حرفه‌ای",
    "کاردانش",
    "عمومی"
)

/**
 * انتخاب‌گر رشته با همان چرخ Snapدار پایه.
 *
 * از [GradeOdometerPicker] با داده و برچسب رشته استفاده می‌کند تا رفتار،
 * گزینهٔ «سایر» و ورودی دستی دقیقاً مثل پایه باشد و کاربر دو تجربهٔ متفاوت نبیند.
 */
@Composable
fun FieldOfStudyPicker(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    availableFields: List<String> = emptyList(),
    includeStandardFields: Boolean = true,
    emptyLabel: String = "بدون رشته",
    label: String = "رشته"
) {
    GradeOdometerPicker(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        availableGrades = availableFields,
        includeStandardGrades = includeStandardFields,
        emptyLabel = emptyLabel,
        label = label,
        standardValues = StandardFieldsOfStudy,
        customLabel = "سایر رشته"
    )
}
