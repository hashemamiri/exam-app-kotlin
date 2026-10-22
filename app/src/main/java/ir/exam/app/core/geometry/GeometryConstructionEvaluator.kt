package ir.exam.app.core.geometry

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToLong
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * ارزیاب مشترک ساخت، برچسب‌گذاری زاویه و شرط‌های هندسی (هم‌فرمول با نسخهٔ وب و تابع سرور).
 * مختصات هدف و شرط‌های هندسی فقط در کلید محرمانه نگه‌داری می‌شوند و هرگز به دانش‌آموز ارسال نمی‌شوند.
 */
object GeometryConstructionEvaluator {
    const val MIN_SEGMENT_LENGTH = 0.1
    val SUPPORTED_CONSTRAINTS = listOf(
        "distance" to "فاصله / شعاع پارهخط",
        "equal_length" to "برابری طول دو پارهخط (|AB| = |CD|)",
        "angle" to "اندازهٔ زاویه (درجه)",
        "perpendicular" to "عمود بودن دو پارهخط (⊥)",
        "parallel" to "موازی بودن دو پارهخط (∥)",
        "midpoint" to "نقطهٔ وسط پارهخط",
        "collinear" to "هم‌خط بودن سه نقطه"
    )

    data class Point2D(val x: Double, val y: Double)
    data class TargetSpec(val label: String, val x: Double, val y: Double, val tolerance: Double, val score: Double)
    data class ConstraintSpec(
        val id: String = "",
        val type: String,
        val points: List<String>,
        val value: Double = 0.0,
        val tolerance: Double = 0.5,
        val score: Double = 1.0
    )

    data class ItemCheck(val label: String, val detail: String, val passed: Boolean, val earned: Double, val weight: Double)
    data class Evaluation(
        val score: Double,
        val earnedWeight: Double,
        val maxWeight: Double,
        val checks: List<ItemCheck>
    )

    data class ParsedAnswer(
        val points: Map<String, Point2D>,
        val angleLabels: List<String>,
        val recordedAngles: Map<String, Double>
    )

    fun faConstraintTitle(type: String): String =
        SUPPORTED_CONSTRAINTS.firstOrNull { it.first == type.lowercase() }?.second ?: type

    fun defaultTolerance(type: String): Double = when (type.lowercase()) {
        "angle", "perpendicular", "parallel" -> 5.0
        "collinear" -> 0.35
        else -> 0.5
    }

    fun requiredPointCount(type: String): Int = when (type.lowercase()) {
        "distance" -> 2
        "equal_length", "perpendicular", "parallel" -> 4
        else -> 3
    }

    fun splitAngleVertices(raw: String): List<String>? {
        val cleaned = raw.trim().removePrefix("∠").trim()
        if (cleaned.isEmpty()) return null
        val parts = if ('-' in cleaned || ',' in cleaned) {
            cleaned.split('-', ',').map(String::trim).filter(String::isNotBlank)
        } else if (cleaned.length == 3) {
            listOf(cleaned[0].toString(), cleaned[1].toString(), cleaned[2].toString())
        } else emptyList()
        return parts.takeIf { it.size == 3 && it[0] != it[1] && it[2] != it[1] && it[0] != it[2] }
    }

    fun canonicalAngleToken(vertices: List<String>): String? {
        if (vertices.size != 3) return null
        val trimmed = vertices.map(String::trim)
        if (trimmed.any(String::isEmpty) || trimmed.distinct().size < 3) return null
        return if (trimmed.all { it.length == 1 }) trimmed.joinToString("") else trimmed.joinToString("-")
    }

    fun normalizeAngleLabels(rawLabels: Iterable<String>): List<String> =
        rawLabels.mapNotNull { splitAngleVertices(it)?.let(::canonicalAngleToken) }.distinct().take(8)

    fun derivePublicLabels(
        targets: List<TargetSpec>,
        constraints: List<ConstraintSpec>
    ): List<String> = (targets.map { it.label.trim() } + constraints.flatMap { it.points.map(String::trim) })
        .filter(String::isNotBlank)
        .distinct()
        .take(12)

    fun derivePublicAngleLabels(
        explicitAngles: Iterable<String>,
        constraints: List<ConstraintSpec>
    ): List<String> {
        val fromConstraints = constraints
            .filter { it.type.equals("angle", ignoreCase = true) }
            .mapNotNull { canonicalAngleToken(it.points.take(3)) }
        return (normalizeAngleLabels(explicitAngles) + fromConstraints).distinct().take(8)
    }

    fun angleDegrees(a: Point2D, vertex: Point2D, c: Point2D): Double? {
        val ux = a.x - vertex.x
        val uy = a.y - vertex.y
        val vx = c.x - vertex.x
        val vy = c.y - vertex.y
        val lenU = hypot(ux, uy)
        val lenV = hypot(vx, vy)
        if (lenU < MIN_SEGMENT_LENGTH || lenV < MIN_SEGMENT_LENGTH) return null
        val cosVal = ((ux * vx + uy * vy) / (lenU * lenV)).coerceIn(-1.0, 1.0)
        return acos(cosVal) * 180.0 / PI
    }

    fun segmentAngleDegrees(a: Point2D, b: Point2D, c: Point2D, d: Point2D): Double? {
        val ux = b.x - a.x
        val uy = b.y - a.y
        val vx = d.x - c.x
        val vy = d.y - c.y
        val lenU = hypot(ux, uy)
        val lenV = hypot(vx, vy)
        if (lenU < MIN_SEGMENT_LENGTH || lenV < MIN_SEGMENT_LENGTH) return null
        val cosVal = ((ux * vx + uy * vy) / (lenU * lenV)).coerceIn(-1.0, 1.0)
        return acos(cosVal) * 180.0 / PI
    }

    fun parseResponse(raw: String?): ParsedAnswer {
        if (raw.isNullOrBlank()) return ParsedAnswer(emptyMap(), emptyList(), emptyMap())
        return runCatching {
            val element: JsonElement = Json.parseToJsonElement(raw)
            val root = element as? JsonObject ?: return ParsedAnswer(emptyMap(), emptyList(), emptyMap())
            val points = linkedMapOf<String, Point2D>()
            val recordedAngles = linkedMapOf<String, Double>()
            root.forEach { (key, value) ->
                if (key == "_angles") {
                    when (value) {
                        is JsonObject -> value.forEach { (angleKey, angleVal) ->
                            val canonical = splitAngleVertices(angleKey)?.let(::canonicalAngleToken) ?: return@forEach
                            val deg = (angleVal as? JsonPrimitive)?.doubleOrNull ?: Double.NaN
                            recordedAngles[canonical] = deg
                        }
                        is kotlinx.serialization.json.JsonArray -> value.forEach { el ->
                            val rawTok = (el as? JsonPrimitive)?.content ?: return@forEach
                            val canonical = splitAngleVertices(rawTok)?.let(::canonicalAngleToken) ?: return@forEach
                            recordedAngles[canonical] = Double.NaN
                        }
                        else -> Unit
                    }
                } else if (!key.startsWith("_")) {
                    val obj = value as? JsonObject ?: return@forEach
                    val x = (obj["x"] as? JsonPrimitive)?.doubleOrNull ?: return@forEach
                    val y = (obj["y"] as? JsonPrimitive)?.doubleOrNull ?: return@forEach
                    if (x.isFinite() && y.isFinite()) {
                        points[key] = Point2D(x.coerceIn(-5.0, 5.0), y.coerceIn(-5.0, 5.0))
                    }
                }
            }
            ParsedAnswer(points, recordedAngles.keys.toList(), recordedAngles)
        }.getOrElse { ParsedAnswer(emptyMap(), emptyList(), emptyMap()) }
    }

    fun encodeResponse(points: Map<String, Point2D>, angleLabels: Iterable<String> = emptyList()): String {
        val cleanAngles = normalizeAngleLabels(angleLabels)
        return buildJsonObject {
            points.forEach { (label, pt) ->
                put(label, buildJsonObject {
                    put("x", JsonPrimitive(roundCoord(pt.x)))
                    put("y", JsonPrimitive(roundCoord(pt.y)))
                })
            }
            if (cleanAngles.isNotEmpty()) {
                put("_angles", buildJsonObject {
                    cleanAngles.forEach { token ->
                        val verts = splitAngleVertices(token)
                        val deg = if (verts != null) {
                            val p0 = points[verts[0]]
                            val p1 = points[verts[1]]
                            val p2 = points[verts[2]]
                            if (p0 != null && p1 != null && p2 != null) angleDegrees(p0, p1, p2) else null
                        } else null
                        put(token, JsonPrimitive(deg?.let(::roundAngle) ?: -1.0))
                    }
                })
            }
        }.toString()
    }

    fun formatResponseSummary(raw: String?, expectedLabels: List<String> = emptyList()): String {
        val parsed = parseResponse(raw)
        if (parsed.points.isEmpty() && parsed.angleLabels.isEmpty()) return "بدون پاسخ"
        val orderedKeys = (expectedLabels + parsed.points.keys).distinct()
        val pointParts = orderedKeys.mapNotNull { label ->
            val p = parsed.points[label]
            if (p != null) "$label: (${"%.2f".format(java.util.Locale.US, p.x)}, ${"%.2f".format(java.util.Locale.US, p.y)})"
            else if (label in expectedLabels) "$label: ثبت‌نشده" else null
        }
        val angleParts = parsed.angleLabels.map { token ->
            val verts = splitAngleVertices(token)
            val measured = verts?.let { v ->
                val p0 = parsed.points[v[0]]
                val p1 = parsed.points[v[1]]
                val p2 = parsed.points[v[2]]
                if (p0 != null && p1 != null && p2 != null) angleDegrees(p0, p1, p2) else null
            } ?: parsed.recordedAngles[token]?.takeIf { it >= 0.0 }
            "∠$token: " + (measured?.let { "${"%.1f".format(java.util.Locale.US, it)}°" } ?: "ناتمام")
        }
        return (pointParts + angleParts).joinToString(" · ").ifBlank { "بدون پاسخ" }
    }

    fun formatConstraintSummary(constraint: ConstraintSpec): String {
        val pts = constraint.points
        return when (constraint.type.lowercase()) {
            "distance" -> "|${pts.getOrElse(0) { "?" }}${pts.getOrElse(1) { "?" }}| = ${constraint.value} ± ${constraint.tolerance} (بارم ${constraint.score})"
            "equal_length" -> "|${pts.getOrElse(0) { "?" }}${pts.getOrElse(1) { "?" }}| = |${pts.getOrElse(2) { "?" }}${pts.getOrElse(3) { "?" }}| ± ${constraint.tolerance} (بارم ${constraint.score})"
            "angle" -> "∠${pts.take(3).joinToString("")} = ${constraint.value}° ± ${constraint.tolerance}° (بارم ${constraint.score})"
            "perpendicular" -> "${pts.getOrElse(0) { "?" }}${pts.getOrElse(1) { "?" }} ⊥ ${pts.getOrElse(2) { "?" }}${pts.getOrElse(3) { "?" }} (±${constraint.tolerance}° · بارم ${constraint.score})"
            "parallel" -> "${pts.getOrElse(0) { "?" }}${pts.getOrElse(1) { "?" }} ∥ ${pts.getOrElse(2) { "?" }}${pts.getOrElse(3) { "?" }} (±${constraint.tolerance}° · بارم ${constraint.score})"
            "midpoint" -> "${pts.getOrElse(0) { "?" }} وسط ${pts.getOrElse(1) { "?" }}${pts.getOrElse(2) { "?" }} (±${constraint.tolerance} · بارم ${constraint.score})"
            "collinear" -> "هم‌خطی ${pts.take(3).joinToString("، ")} (±${constraint.tolerance} · بارم ${constraint.score})"
            else -> "${constraint.type} (${pts.joinToString(",")})"
        }
    }

    fun evaluate(
        questionScore: Double,
        targets: List<TargetSpec>,
        constraints: List<ConstraintSpec>,
        rawResponse: String?
    ): Evaluation {
        val parsed = parseResponse(rawResponse)
        val checks = mutableListOf<ItemCheck>()
        var maxWeight = 0.0
        var earnedWeight = 0.0

        targets.forEach { target ->
            val weight = target.score.coerceAtLeast(0.0)
            val tol = target.tolerance.coerceAtLeast(0.0)
            if (target.label.isBlank() || target.x !in -5.0..5.0 || target.y !in -5.0..5.0 || tol <= 0.0 || tol > 5.0) {
                return Evaluation(0.0, 0.0, maxWeight, checks)
            }
            maxWeight += weight
            val actual = parsed.points[target.label]
            val dist = actual?.let { hypot(it.x - target.x, it.y - target.y) }
            val ok = dist != null && dist <= tol
            val earned = if (ok) weight else 0.0
            earnedWeight += earned
            checks += ItemCheck(
                label = "نقطه ${target.label}",
                detail = if (dist != null) "خطا ${"%.2f".format(java.util.Locale.US, dist)} (مجاز ${target.tolerance})" else "ثبت‌نشده",
                passed = ok,
                earned = earned,
                weight = weight
            )
        }

        constraints.forEach { c ->
            val weight = c.score.coerceAtLeast(0.0)
            val tol = c.tolerance.coerceAtLeast(0.0)
            if (tol <= 0.0) return@forEach
            maxWeight += weight
            val pts = c.points.map { parsed.points[it.trim()] }
            val result: Pair<Boolean, String> = when (c.type.lowercase()) {
                "distance" -> {
                    val p0 = pts.getOrNull(0); val p1 = pts.getOrNull(1)
                    if (p0 == null || p1 == null) false to "نقاط ناقص"
                    else {
                        val d = hypot(p1.x - p0.x, p1.y - p0.y)
                        (abs(d - c.value) <= tol && d >= MIN_SEGMENT_LENGTH) to "|${c.points.take(2).joinToString("")}| = ${"%.2f".format(java.util.Locale.US, d)}"
                    }
                }
                "equal_length" -> {
                    val p0 = pts.getOrNull(0); val p1 = pts.getOrNull(1); val p2 = pts.getOrNull(2); val p3 = pts.getOrNull(3)
                    if (p0 == null || p1 == null || p2 == null || p3 == null) false to "نقاط ناقص"
                    else {
                        val d1 = hypot(p1.x - p0.x, p1.y - p0.y)
                        val d2 = hypot(p3.x - p2.x, p3.y - p2.y)
                        (d1 >= MIN_SEGMENT_LENGTH && d2 >= MIN_SEGMENT_LENGTH && abs(d1 - d2) <= tol) to "Δ = ${"%.2f".format(java.util.Locale.US, abs(d1 - d2))}"
                    }
                }
                "angle" -> {
                    val p0 = pts.getOrNull(0); val p1 = pts.getOrNull(1); val p2 = pts.getOrNull(2)
                    val deg = if (p0 != null && p1 != null && p2 != null) angleDegrees(p0, p1, p2) else null
                    if (deg == null) false to "زاویه تعریف‌نشده"
                    else (abs(deg - c.value) <= tol) to "∠${c.points.take(3).joinToString("")} = ${"%.1f".format(java.util.Locale.US, deg)}°"
                }
                "perpendicular" -> {
                    val p0 = pts.getOrNull(0); val p1 = pts.getOrNull(1); val p2 = pts.getOrNull(2); val p3 = pts.getOrNull(3)
                    val deg = if (p0 != null && p1 != null && p2 != null && p3 != null) segmentAngleDegrees(p0, p1, p2, p3) else null
                    if (deg == null) false to "پارهخط ناقص"
                    else (abs(deg - 90.0) <= tol) to "زاویه = ${"%.1f".format(java.util.Locale.US, deg)}°"
                }
                "parallel" -> {
                    val p0 = pts.getOrNull(0); val p1 = pts.getOrNull(1); val p2 = pts.getOrNull(2); val p3 = pts.getOrNull(3)
                    val deg = if (p0 != null && p1 != null && p2 != null && p3 != null) segmentAngleDegrees(p0, p1, p2, p3) else null
                    val distinct = p0 != null && p2 != null && (hypot(p0.x - p2.x, p0.y - p2.y) >= MIN_SEGMENT_LENGTH || (p1 != null && p3 != null && hypot(p1.x - p3.x, p1.y - p3.y) >= MIN_SEGMENT_LENGTH))
                    if (deg == null || !distinct) false to "پارهخط ناقص یا منطبق"
                    else (min(deg, 180.0 - deg) <= tol) to "انحراف = ${"%.1f".format(java.util.Locale.US, min(deg, 180.0 - deg))}°"
                }
                "midpoint" -> {
                    val pm = pts.getOrNull(0); val pa = pts.getOrNull(1); val pb = pts.getOrNull(2)
                    if (pm == null || pa == null || pb == null || hypot(pb.x - pa.x, pb.y - pa.y) < MIN_SEGMENT_LENGTH) false to "نقاط ناقص"
                    else {
                        val err = hypot(pm.x - (pa.x + pb.x) / 2.0, pm.y - (pa.y + pb.y) / 2.0)
                        (err <= tol) to "فاصله از وسط = ${"%.2f".format(java.util.Locale.US, err)}"
                    }
                }
                "collinear" -> {
                    val pa = pts.getOrNull(0); val pb = pts.getOrNull(1); val pc = pts.getOrNull(2)
                    if (pa == null || pb == null || pc == null) false to "نقاط ناقص"
                    else {
                        val ab = hypot(pb.x - pa.x, pb.y - pa.y)
                        val ac = hypot(pc.x - pa.x, pc.y - pa.y)
                        val bc = hypot(pc.x - pb.x, pc.y - pb.y)
                        if (ab < MIN_SEGMENT_LENGTH || ac < MIN_SEGMENT_LENGTH || bc < MIN_SEGMENT_LENGTH) false to "نقاط منطبق"
                        else {
                            val distLine = abs((pb.x - pa.x) * (pc.y - pa.y) - (pb.y - pa.y) * (pc.x - pa.x)) / ab
                            (distLine <= tol) to "فاصله از خط = ${"%.2f".format(java.util.Locale.US, distLine)}"
                        }
                    }
                }
                else -> false to "نوع ناشناخته"
            }
            val earned = if (result.first) weight else 0.0
            earnedWeight += earned
            checks += ItemCheck(
                label = faConstraintTitle(c.type),
                detail = result.second,
                passed = result.first,
                earned = earned,
                weight = weight
            )
        }

        val finalScore = if (maxWeight <= 0.0 || questionScore <= 0.0) 0.0
        else ((questionScore * earnedWeight / maxWeight) * 100.0).roundToLong() / 100.0
        return Evaluation(finalScore, earnedWeight, maxWeight, checks)
    }

    private fun roundCoord(v: Double): Double = ((v * 100.0).roundToLong()) / 100.0
    private fun roundAngle(v: Double): Double = ((v * 10.0).roundToLong()) / 10.0
}
