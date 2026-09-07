package ir.exam.app.ui.math

/**
 * هماهنگ‌کنندهٔ کاملاً Native درج محتوای غنی در کادر متن سؤال.
 *
 * کادر سؤال از BasicTextField/Compose استفاده می‌کند. تنها سطح WebView مجاز
 * برنامه FormulaHostDialog است؛ این controller هیچ fallback، bridge یا
 * reference به WebView ندارد و فقط callbackهای Native بخش سؤال را نگه می‌دارد.
 */
class QuestionEditorFieldController {
    internal var nativeInsert: ((String) -> Boolean)? = null
    internal var nativeReplace: ((String) -> Boolean)? = null
    internal var nativeOpenFormula: (() -> Boolean)? = null
    var pendingEditOccurrence: Int? = null

    /** آفست مکان‌نما پس از بازگشت از FormulaHostDialog. */
    var pendingCaretOffset: Int? = null

    /** بازکردن تنها ابزار WebView مجاز: ویرایشگر فرمول تمام‌صفحه. */
    fun openTool(name: String): Boolean =
        name == "formula" && (nativeOpenFormula?.invoke() == true)

    /** درج توکن `%%FIG:{json}%%` ساخته‌شده توسط ویرایشگرهای Native. */
    fun insertFigureJson(specJson: String): Boolean = nativeInsert?.invoke(specJson) == true

    /** جایگزینی شکل انتخاب‌شده با خروجی ویرایشگر Native. */
    fun applyEditedFigureJson(specJson: String): Boolean = nativeReplace?.invoke(specJson) == true

    /** انصراف از ویرایش توکن Native. */
    fun cancelEditFigure() {
        pendingEditOccurrence = null
    }
}
