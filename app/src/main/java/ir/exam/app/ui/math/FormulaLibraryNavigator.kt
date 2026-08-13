package ir.exam.app.ui.math

/** مسیر واحد و تست‌پذیر برای بازکردن همهٔ کتابخانه‌های فرمول. */
object FormulaLibraryNavigator {
    fun entries(
        data: FormulaReferenceData,
        categoryId: String,
        favorites: List<FormulaReferenceEntry> = emptyList(),
        recent: List<FormulaReferenceEntry> = emptyList(),
        uppercase: Boolean = false
    ): List<FormulaReferenceEntry> {
        val source = when (categoryId) {
            "__all" -> data.allItems
            "__favorites" -> favorites
            "__recent_symbols" -> recent
            "letters" -> (if (uppercase) 'A'..'Z' else 'a'..'z').map {
                FormulaReferenceEntry("حرف $it", it.toString())
            }
            else -> data.categoryById[categoryId]?.items.orEmpty()
        }
        return source.distinctBy { it.label + "¦" + it.tex }
    }

    fun search(data: FormulaReferenceData, query: String): List<FormulaReferenceEntry> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()
        return (data.allItems + data.categoryById["unicode"]?.items.orEmpty())
            .asSequence()
            .filter {
                it.label.lowercase().contains(normalized) ||
                    it.tex.lowercase().contains(normalized)
            }
            .distinctBy { it.label + "¦" + it.tex }
            .toList()
    }

    fun categoryTitle(data: FormulaReferenceData, id: String): String =
        data.categoryById[id]?.label ?: when (id) {
            "__all" -> "همهٔ نمادها"
            "__favorites" -> "علاقه‌مندی"
            "__recent_symbols" -> "نمادهای اخیر"
            "letters" -> "حروف انگلیسی"
            else -> "کتابخانه فرمول"
        }
}
