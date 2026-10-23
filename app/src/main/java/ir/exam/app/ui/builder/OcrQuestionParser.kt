package ir.exam.app.ui.builder

import java.util.UUID

/**
 * V335.4 — استخراج خودکار سؤالات و تست‌ها از متنِ خروجیِ OCR.
 *
 * متن خوانده‌شده از عکس را به تفکیک شماره سؤال یا گزینه‌های (الف، ب، ج، د / ۱، ۲، ۳، ۴)
 * تشخیص داده و به صورت پیش‌نویس سؤالات (چندگزینه‌ای یا تشریحی) درمی‌آورد.
 */
object OcrQuestionParser {

    private val Q_START_REGEX = Regex(
        """^\s*(?:(?:سؤال|سوال)\s*([0-9۰-۹]+)[\:\.\-–—\)]|([0-9۰-۹]+)[\.\-–—\:])\s*(.*)$"""
    )

    private val PERSIAN_OPTION_REGEX = Regex(
        """^\s*(?:\(?\s*(الف|ب|ج|د)\s*[\)\.\-–—\:]|\((الف|ب|ج|د)\))\s*(.*)$"""
    )

    private val NUMERIC_OPTION_REGEX = Regex(
        """^\s*(?:\(?\s*([1-4۱-۴])\s*[\)\.\-–—\:]|\(([1-4۱-۴])\))\s*(.*)$"""
    )

    fun parse(rawText: String): List<QuestionDraft> {
        val trimmed = rawText.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (trimmed.isBlank()) return emptyList()

        val lines = trimmed.lines()
        val questionBlocks = mutableListOf<String>()
        var currentBlock = mutableListOf<String>()

        for (line in lines) {
            val match = Q_START_REGEX.find(line)
            if (match != null) {
                if (currentBlock.isNotEmpty() && currentBlock.any { it.isNotBlank() }) {
                    questionBlocks.add(currentBlock.joinToString("\n").trim())
                    currentBlock = mutableListOf()
                }
                val remainder = match.groupValues.getOrNull(3).orEmpty()
                currentBlock.add(remainder)
            } else {
                currentBlock.add(line)
            }
        }
        if (currentBlock.isNotEmpty() && currentBlock.any { it.isNotBlank() }) {
            questionBlocks.add(currentBlock.joinToString("\n").trim())
        }

        val finalBlocks = if (questionBlocks.size <= 1) {
            val doubleLineBlocks = trimmed.split(Regex("""\n\s*\n+"""))
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (doubleLineBlocks.size > 1) doubleLineBlocks else (questionBlocks.ifEmpty { listOf(trimmed) })
        } else {
            questionBlocks
        }

        return finalBlocks.mapNotNull { block ->
            parseSingleQuestion(block)
        }
    }

    private fun parseSingleQuestion(block: String): QuestionDraft? {
        val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null

        val stemLines = mutableListOf<String>()
        val options = mutableListOf<String>()
        var inOptions = false

        for (line in lines) {
            val optMatch = PERSIAN_OPTION_REGEX.find(line) ?: NUMERIC_OPTION_REGEX.find(line)
            if (optMatch != null) {
                inOptions = true
                val optText = optMatch.groupValues.lastOrNull { it.isNotBlank() } ?: line
                options.add(optText.trim())
            } else if (inOptions) {
                if (options.isNotEmpty()) {
                    options[options.lastIndex] = (options.last() + " " + line).trim()
                }
            } else {
                stemLines.add(line)
            }
        }

        val stem = stemLines.joinToString("\n").trim().ifBlank {
            if (options.isNotEmpty()) "سؤال تستی" else block.trim()
        }

        return if (options.size >= 2) {
            val finalOptions = (options.take(4) + List(maxOf(0, 4 - options.size)) { "" })
            QuestionDraft(
                id = UUID.randomUUID().toString(),
                type = QuestionType.MULTIPLE_CHOICE,
                text = stem,
                options = finalOptions,
                optionIds = List(4) { UUID.randomUUID().toString() },
                optionImages = List(4) { null }
            )
        } else {
            QuestionDraft(
                id = UUID.randomUUID().toString(),
                type = QuestionType.ESSAY,
                text = block.trim()
            )
        }
    }
}
