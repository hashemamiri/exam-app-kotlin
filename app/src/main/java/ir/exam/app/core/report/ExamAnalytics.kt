package ir.exam.app.core.report

/**
 * V321 — کارنامهٔ تحلیلی.
 *
 * آمار توصیفیِ نمرهٔ کلاس و تحلیل تک‌تک آزمون‌ها فقط از همان سطرهایی حساب می‌شود که صفحهٔ
 * «کارنامه» نمایش می‌دهد؛ بنابراین «میانگین» و «درصد قبولی» همیشه با جدولِ روی صفحه یکی است.
 * این فایل عمداً **بدون وابستگی اندروید** نوشته شده تا با تست واحد بررسی شود.
 */
data class ScoreRow(
    val studentId: String,
    val name: String,
    /** شناسهٔ آزمون → درصد (۰ تا ۱۰۰)؛ null = در آن آزمون نمره‌ای ثبت نشده است. */
    val scores: Map<String, Double?>
)

/** آمار توصیفی یک مجموعه نمره (درصدی). */
data class ScoreStats(
    val count: Int,
    val mean: Double,
    val median: Double,
    val stdDev: Double,
    val min: Double,
    val max: Double,
    /** درصد نمره‌های ≥ آستانهٔ قبولی. */
    val passRate: Double,
    val passMark: Double
) {
    val spread: Double get() = max - min
}

/** یک بازهٔ نمودار توزیع (مثلاً ۵۰ تا ۶۰). */
data class DistributionBin(val from: Int, val to: Int, val count: Int) {
    val label: String get() = "$from–$to"
}

/** تحلیل یک آزمون در محدودهٔ انتخاب‌شده. */
data class ExamBucket(
    val examId: String,
    val title: String,
    val stats: ScoreStats
)

/** جایگاه یک دانش‌آموز. */
data class StudentRank(
    val studentId: String,
    val name: String,
    val percent: Double,
    /** ۱ = بالاترین نمره. */
    val rank: Int,
    /** درصدِ نفراتی که نمرهٔ کمتر یا مساوی او دارند (۰ تا ۱۰۰). */
    val percentile: Double
)

data class ClassAnalytics(
    val overall: ScoreStats,
    val bins: List<DistributionBin>,
    val exams: List<ExamBucket>,
    val ranking: List<StudentRank>,
    /** دانش‌آموزانی که میانگینشان کمتر از آستانهٔ هشدار است یا افت محسوس داشته‌اند. */
    val atRisk: List<StudentRank>,
    /** دانش‌آموزان سه رتبهٔ اول. */
    val top: List<StudentRank>,
    /** آزمونی با کمترین و بیشترین میانگین (برای جملهٔ خلاصه). */
    val hardestExam: ExamBucket?,
    val easiestExam: ExamBucket?,
    val passMark: Double
) {
    val isEmpty: Boolean get() = overall.count == 0

    /** جملهٔ یک‌خطی آمادهٔ نمایش در کارت تحلیل. */
    val headline: String
        get() = when {
            isEmpty -> "برای نمایش تحلیل، ابتدا کلاس و آزمون را انتخاب کنید."
            overall.mean >= 80 -> "عملکرد کلاس عالی است: میانگین ${oneDecimal(overall.mean)}٪"
            overall.mean >= 60 -> "عملکرد کلاس قابل قبول است: میانگین ${oneDecimal(overall.mean)}٪"
            overall.mean >= 50 -> "میانگین کلاس ${oneDecimal(overall.mean)}٪ — مرز قبولی؛ سؤال‌های سخت را بازبینی کنید."
            else -> "میانگین کلاس ${oneDecimal(overall.mean)}٪ — زیر آستانهٔ قبولی؛ آموزش مجدد لازم است."
        }

    private fun oneDecimal(v: Double): String {
        val r = Math.round(v * 10.0) / 10.0
        return if (r == Math.floor(r)) r.toLong().toString() else r.toString()
    }
}

object ExamAnalytics {

    /** آستانهٔ قبولی پیش‌فرض (۵۰٪) و آستانهٔ هشدار برای دانش‌آموزان در معرض خطر. */
    const val DEFAULT_PASS_MARK = 50.0
    const val RISK_MARK = 50.0
    /** سقف نمرهٔ کسی که «افت محسوس» او هشدار به حساب می‌آید. */
    const val RISK_TREND_MARK = 70.0
    /** افت/بهبود بیشتر از این مقدار (درصد) «محسوس» شمرده می‌شود. */
    const val TREND_MARK = 10.0

    fun of(
        rows: List<ScoreRow>,
        examTitles: Map<String, String>,
        passMark: Double = DEFAULT_PASS_MARK,
        binCount: Int = 10
    ): ClassAnalytics {
        val allValues = rows.flatMap { row -> row.scores.values.filterNotNull() }
        val overall = statsOf(allValues, passMark)
        val bins = binsOf(allValues, binCount)

        val examIds = rows.flatMap { it.scores.keys }.distinct().sortedBy { examTitles[it] ?: it }
        val exams = examIds.map { id ->
            ExamBucket(id, examTitles[id] ?: id, statsOf(rows.mapNotNull { it.scores[id] }, passMark))
        }.filter { it.stats.count > 0 }

        val ranking = rows
            .mapNotNull { row ->
                val values = row.scores.values.filterNotNull()
                if (values.isEmpty()) null
                else StudentRank(row.studentId, row.name, values.average(), 0, 0.0)
            }
            .sortedByDescending { it.percent }
            .mapIndexed { index, rank ->
                rank.copy(rank = index + 1, percentile = percentileOf(index, rows.size))
            }

        val top = ranking.take(3)
        // «در معرض خطر» = زیر آستانهٔ قبولی، یا افتِ محسوس در حالی که هنوز نمرهٔ بالایی ندارد.
        // (دانش‌آموز ممتاز با افت ۱۰ درصدی هشدار نمی‌گیرد تا فهرست هشدارها واقعی بماند.)
        val atRisk = ranking.filter { rank ->
            rank.percent < RISK_MARK ||
                (rank.percent < RISK_TREND_MARK && declined(rows.first { r -> r.studentId == rank.studentId }))
        }

        return ClassAnalytics(
            overall = overall,
            bins = bins,
            exams = exams,
            ranking = ranking,
            atRisk = atRisk,
            top = top,
            hardestExam = exams.minByOrNull { it.stats.mean },
            easiestExam = exams.maxByOrNull { it.stats.mean },
            passMark = passMark
        )
    }

    /** آمار توصیفی؛ انحراف معیار «جامعه» (÷n) محاسبه می‌شود. */
    fun statsOf(values: List<Double>, passMark: Double = DEFAULT_PASS_MARK): ScoreStats {
        val clean = values.filter { !it.isNaN() }
        if (clean.isEmpty()) return ScoreStats(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, passMark)
        val mean = clean.average()
        val sorted = clean.sorted()
        val median = if (sorted.size % 2 == 1) sorted[sorted.size / 2]
        else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        val variance = clean.sumOf { (it - mean) * (it - mean) } / clean.size
        return ScoreStats(
            count = clean.size,
            mean = mean,
            median = median,
            stdDev = Math.sqrt(variance),
            min = sorted.first(),
            max = sorted.last(),
            passRate = clean.count { it >= passMark } * 100.0 / clean.size,
            passMark = passMark
        )
    }

    /** توزیع نمره‌ها در بازه‌های مساوی ۰ تا ۱۰۰ (بازهٔ آخر شامل ۱۰۰ است). */
    fun binsOf(values: List<Double>, binCount: Int = 10): List<DistributionBin> {
        val bins = binCount.coerceIn(2, 20)
        val width = 100.0 / bins
        val counts = IntArray(bins)
        values.filter { !it.isNaN() }.forEach { v ->
            val clamped = v.coerceIn(0.0, 100.0)
            val index = ((clamped / width).toInt()).coerceIn(0, bins - 1)
            counts[index]++
        }
        return (0 until bins).map { i ->
            DistributionBin(
                from = Math.round(i * width).toInt(),
                to = Math.round((i + 1) * width).toInt(),
                count = counts[i]
            )
        }
    }

    /** درصد رتبه‌ها که پایین‌تر یا مساوی این رتبه‌اند. */
    fun percentileOf(index: Int, total: Int): Double {
        if (total <= 0) return 0.0
        return (total - index) * 100.0 / total
    }

    /** افت محسوس: نیمهٔ اول آزمون‌ها بهتر از نیمهٔ دوم باشد. */
    fun declined(row: ScoreRow): Boolean {
        val values = row.scores.values.filterNotNull()
        if (values.size < 2) return false
        val half = values.size / 2
        if (half == 0) return false
        val first = values.take(half).average()
        val second = values.drop(half).average()
        return first - second >= TREND_MARK
    }

    /** آیا این دانش‌آموز روند صعودی محسوس دارد؟ */
    fun improved(row: ScoreRow): Boolean {
        val values = row.scores.values.filterNotNull()
        if (values.size < 2) return false
        val half = values.size / 2
        if (half == 0) return false
        return values.drop(half).average() - values.take(half).average() >= TREND_MARK
    }
}
