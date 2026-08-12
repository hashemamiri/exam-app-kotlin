package ir.exam.app.ui.math

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FormulaReferenceStore(context: Context) {
    private val preferences = context.getSharedPreferences("formula_reference_history", Context.MODE_PRIVATE)
    private val json = Json

    fun favorites(): List<FormulaReferenceEntry> = readEntries("favorites")
    fun recentSymbols(): List<FormulaReferenceEntry> = readEntries("recent_symbols")
    fun recentFormulas(): List<String> = readStrings("recent_formulas")
    fun lastFormula(): String = preferences.getString("last_formula", "").orEmpty()

    fun isFavorite(entry: FormulaReferenceEntry): Boolean = favorites().any {
        it.label == entry.label && it.tex == entry.tex
    }

    fun toggleFavorite(entry: FormulaReferenceEntry): Boolean {
        val list = favorites().toMutableList()
        val index = list.indexOfFirst { it.label == entry.label && it.tex == entry.tex }
        val added = index < 0
        if (index >= 0) list.removeAt(index) else list.add(0, entry)
        writeEntries("favorites", list.take(60))
        return added
    }

    fun addRecentSymbol(entry: FormulaReferenceEntry) {
        writeEntries(
            "recent_symbols",
            (listOf(entry) + recentSymbols().filterNot {
                it.tex == entry.tex && it.label == entry.label
            }).take(24)
        )
    }

    fun addRecentFormula(tex: String) {
        val clean = tex.trim()
        if (clean.isEmpty()) return
        writeStrings("recent_formulas", (listOf(clean) + recentFormulas().filterNot { it == clean }).take(20))
        preferences.edit().putString("last_formula", clean).apply()
    }

    fun removeRecentFormula(tex: String) {
        writeStrings("recent_formulas", recentFormulas().filterNot { it == tex })
    }

    fun clearRecentFormulas() {
        writeStrings("recent_formulas", emptyList())
    }

    fun setLastFormula(tex: String) {
        preferences.edit().putString("last_formula", tex.trim()).apply()
    }

    private fun readEntries(key: String): List<FormulaReferenceEntry> = runCatching {
        val array = json.decodeFromString<List<List<String>>>(preferences.getString(key, "[]") ?: "[]")
        array.mapNotNull { if (it.size >= 2) FormulaReferenceEntry(it[0], it[1]) else null }
    }.getOrDefault(emptyList())

    private fun writeEntries(key: String, list: List<FormulaReferenceEntry>) {
        preferences.edit().putString(key, json.encodeToString(list.map { listOf(it.label, it.tex) })).apply()
    }

    private fun readStrings(key: String): List<String> = runCatching {
        json.decodeFromString<List<String>>(preferences.getString(key, "[]") ?: "[]")
    }.getOrDefault(emptyList())

    private fun writeStrings(key: String, list: List<String>) {
        preferences.edit().putString(key, json.encodeToString(list)).apply()
    }
}
