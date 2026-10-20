package ir.exam.app.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.core.report.ClassAnalytics
import ir.exam.app.core.report.ScoreStats

/**
 * V321 — کارت «کارنامهٔ تحلیلی»: آمار توصیفی کلاس، نمودار توزیع نمره‌ها، سخت‌ترین/آسان‌ترین آزمون،
 * نمره‌های برتر و دانش‌آموزان در معرض خطر.
 *
 * همهٔ رنگ‌ها از `MaterialTheme.colorScheme` گرفته می‌شوند تا در حالت تیره/پرکنتراست هم خوانا باشد.
 */
@Composable
fun ClassAnalyticsCard(analytics: ClassAnalytics, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("کارنامهٔ تحلیلی", style = MaterialTheme.typography.titleMedium)
            Text(
                analytics.headline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (analytics.isEmpty) return@Column

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricChip("میانگین", percent(analytics.overall.mean), Modifier.weight(1f))
                MetricChip("میانه", percent(analytics.overall.median), Modifier.weight(1f))
                MetricChip("انحراف معیار", oneDecimal(analytics.overall.stdDev), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricChip("درصد قبولی", percent(analytics.overall.passRate), Modifier.weight(1f))
                MetricChip("بیشترین", percent(analytics.overall.max), Modifier.weight(1f))
                MetricChip("کمترین", percent(analytics.overall.min), Modifier.weight(1f))
            }

            Text(
                "توزیع نمره‌ها (${PersianDigits.convert(analytics.overall.count.toString())} نمره)",
                style = MaterialTheme.typography.labelLarge
            )
            DistributionBars(analytics.bins.map { it.count })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(analytics.bins.first().label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(analytics.bins.last().label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (analytics.exams.size >= 2) {
                HorizontalDivider()
                analytics.hardestExam?.let { bucket ->
                    AnalysisRow("سخت‌ترین آزمون", bucket.title, percent(bucket.stats.mean))
                }
                analytics.easiestExam?.let { bucket ->
                    AnalysisRow("آسان‌ترین آزمون", bucket.title, percent(bucket.stats.mean))
                }
            }

            if (analytics.top.isNotEmpty()) {
                HorizontalDivider()
                Text("نمره‌های برتر", style = MaterialTheme.typography.labelLarge)
                analytics.top.forEach { rank ->
                    AnalysisRow("${PersianDigits.convert(rank.rank.toString())}. ${rank.name}", "رتبه از ${PersianDigits.convert(analytics.ranking.size.toString())}", percent(rank.percent))
                }
            }

            if (analytics.atRisk.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "نیازمند پیگیری (${PersianDigits.convert(analytics.atRisk.size.toString())} نفر)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
                analytics.atRisk.take(5).forEach { rank ->
                    AnalysisRow(rank.name, "میانگین زیر ${percent(analytics.passMark)}", percent(rank.percent), danger = true)
                }
                if (analytics.atRisk.size > 5) {
                    Text(
                        "و ${PersianDigits.convert((analytics.atRisk.size - 5).toString())} نفر دیگر",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** نوارهای سادهٔ توزیع؛ بلندترین ستون با رنگ اصلی و بقیه با رنگ ملایم‌تر. */
@Composable
private fun DistributionBars(counts: List<Int>) {
    val primary = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primaryContainer
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        if (counts.isEmpty()) return@Canvas
        val slot = size.width / counts.size
        counts.forEachIndexed { index, count ->
            if (count == 0) return@forEachIndexed
            val barHeight = (count.toFloat() / max) * size.height * 0.92f
            drawRect(
                color = if (count == max) primary else soft,
                topLeft = Offset(index * slot + slot * 0.12f, size.height - barHeight),
                size = Size(slot * 0.76f, barHeight)
            )
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AnalysisRow(title: String, subtitle: String, value: String, danger: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

/** آستانهٔ قبولی و وضعیت کلی (برای استفاده در کارنامهٔ تکی). */
@Composable
fun ScoreStatsLine(stats: ScoreStats) {
    Text(
        "میانگین ${percent(stats.mean)} · میانه ${percent(stats.median)} · قبولی ${percent(stats.passRate)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun percent(value: Double): String = PersianDigits.convert(oneDecimal(value)) + "٪"

private fun oneDecimal(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded == Math.floor(rounded) && !rounded.isInfinite()) rounded.toLong().toString() else rounded.toString()
}
