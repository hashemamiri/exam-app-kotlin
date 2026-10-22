package ir.exam.app.data.repository

import ir.exam.app.core.geometry.GeometryConstructionEvaluator
import ir.exam.app.ui.builder.AlignSpan
import ir.exam.app.ui.builder.HotspotRect
import ir.exam.app.ui.builder.MediaDraft
import ir.exam.app.ui.builder.OptionStyle
import ir.exam.app.ui.builder.AssessmentPartDraft
import ir.exam.app.ui.builder.ConstructionConstraintDraft
import ir.exam.app.ui.builder.ConstructionTargetDraft
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.StyleSpan
import ir.exam.app.ui.builder.QuestionType
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class EncodedQuestions(
    val publicQuestions: JsonArray,
    val answerKey: JsonArray
)

internal object ExamQuestionCodec {
    // V64.5 — استایل عنصر (گزینه/سمت جورکردنی): آبجکت خالی = null (بدون استایل).
    private fun JsonElement?.decodeStyles(): List<OptionStyle?> =
        this.asArrayOrEmpty().map { element ->
            (element as? JsonObject)?.takeIf { it.isNotEmpty() }?.let { styleObj ->
                OptionStyle(
                    bold = styleObj["b"]?.asBoolean() ?: false,
                    italic = styleObj["i"]?.asBoolean() ?: false,
                    fontSizeSp = styleObj["s"]?.asDouble()?.toFloat()
                )
            }
        }

    // V68 — استایل تکه‌ای متن: آرایهٔ {s,e,b,i}؛ خالی/غایب = بدون استایل.
    private fun JsonElement?.decodeSpans(): List<StyleSpan> =
        this.asArrayOrEmpty().mapNotNull { element ->
            (element as? JsonObject)?.let { o ->
                val s = o["s"]?.asInt() ?: return@let null
                val e = o["e"]?.asInt() ?: return@let null
                if (e > s) StyleSpan(
                    s, e, o["b"]?.asBoolean() ?: false, o["i"]?.asBoolean() ?: false,
                    underline = o["u"]?.asBoolean() ?: false,
                    color = (o["c"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.matches(Regex("#[0-9a-fA-F]{6}")) },
                    size = o["z"]?.asInt()?.takeIf { it in 1..100 }, // V129
                    font = (o["f"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }?.take(30)
                ) else null
            }
        }

    private fun encodeSpans(spans: List<StyleSpan>): JsonArray? =
        if (spans.isEmpty()) null else JsonArray(spans.map { span ->
            JsonObject(buildMap {
                put("s", JsonPrimitive(span.start))
                put("e", JsonPrimitive(span.end))
                if (span.bold) put("b", JsonPrimitive(true))
                if (span.italic) put("i", JsonPrimitive(true))
                if (span.underline) put("u", JsonPrimitive(true))
                span.color?.let { put("c", JsonPrimitive(it)) }
                span.size?.let { put("z", JsonPrimitive(it)) }
                span.font?.let { put("f", JsonPrimitive(it)) }
            })
        })

    // V121 — تراز پاراگرافیِ تکه‌ای: آرایهٔ {s,e,a}؛ خالی/غایب = بدون بازهٔ تراز.
    private fun JsonElement?.decodeAlignSpans(): List<AlignSpan> =
        this.asArrayOrEmpty().mapNotNull { element ->
            (element as? JsonObject)?.let { o ->
                val s = o["s"]?.asInt() ?: return@let null
                val e = o["e"]?.asInt() ?: return@let null
                val align = (o["a"] as? JsonPrimitive)?.contentOrNull
                    ?.takeIf { it in setOf("right", "center", "left", "justify") } ?: return@let null
                if (e > s) AlignSpan(s, e, align) else null
            }
        }

    private fun encodeAlignSpans(spans: List<AlignSpan>): JsonArray? =
        if (spans.isEmpty()) null else JsonArray(spans.map { span ->
            JsonObject(mapOf(
                "s" to JsonPrimitive(span.start),
                "e" to JsonPrimitive(span.end),
                "a" to JsonPrimitive(span.align)
            ))
        })

    private fun encodeStyles(styles: List<OptionStyle?>, size: Int): JsonArray? =
        if (styles.none { it != null }) null else JsonArray(List(size) { index ->
            styles.getOrNull(index)?.let { style ->
                JsonObject(buildMap {
                    put("b", JsonPrimitive(style.bold))
                    put("i", JsonPrimitive(style.italic))
                    style.fontSizeSp?.let { put("s", JsonPrimitive(it)) }
                })
            } ?: JsonObject(emptyMap())
        })

    fun decode(publicQuestions: JsonElement, answerKey: JsonElement?): List<QuestionDraft> {
        val keys = answerKey.asArrayOrEmpty().mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            (obj["i"]?.asInt() ?: index) to obj
        }.toMap()

        return publicQuestions.asArrayOrEmpty().mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            val key = keys[index] ?: JsonObject(emptyMap())
            val type = parseType(obj["type"]?.asString())
            val imageUrls = obj["images"].asArrayOrEmpty().mapNotNull { it.asString() }.toMutableList()
            obj["image"]?.asString()?.takeIf(String::isNotBlank)?.let { if (it !in imageUrls) imageUrls.add(0, it) }
            val positions = obj["imgFreePositions"].asArrayOrEmpty()

            QuestionDraft(
                id = obj["id"]?.asString() ?: UUID.randomUUID().toString(),
                type = type,
                text = obj["text"]?.asString().orEmpty(),
                // V327 — passage مشترك برای سؤال‌های درک مطلب؛ برای دادهٔ قدیمی غایب است.
                passage = obj["passage"]?.asString().orEmpty(),
                sectionTitle = obj["sectionTitle"]?.asString().orEmpty(),
                topic = obj["topic"]?.asString().orEmpty(),
                difficulty = obj["difficulty"]?.asInt()?.coerceIn(1, 3) ?: 0,
                learningObjective = obj["learningObjective"]?.asString().orEmpty(),
                proofAssumptions = obj["proofAssumptions"]?.asString().orEmpty(),
                proofGoal = obj["proofGoal"]?.asString().orEmpty(),
                constructionPrompt = obj["constructionPrompt"]?.asString().orEmpty(),
                constructionAngleLabels = obj["constructionAngleLabels"].asArrayOrEmpty().mapNotNull { it.asString()?.trim() }.filter(String::isNotBlank),
                constructionTargets = key["constructionTargets"].asArrayOrEmpty().mapIndexedNotNull { targetIndex, rawTarget ->
                    val target = rawTarget as? JsonObject ?: return@mapIndexedNotNull null
                    ConstructionTargetDraft(
                        id = target["id"]?.asString() ?: "${obj["id"]?.asString().orEmpty()}-target-$targetIndex",
                        label = target["label"]?.asString() ?: "${('A'.code + targetIndex).toChar()}",
                        x = target["x"]?.asDouble() ?: 0.0,
                        y = target["y"]?.asDouble() ?: 0.0,
                        tolerance = target["tolerance"]?.asDouble() ?: 0.5,
                        score = target["score"]?.asDouble() ?: 1.0
                    )
                },
                constructionConstraints = key["constructionConstraints"].asArrayOrEmpty().mapIndexedNotNull { cIndex, rawConstraint ->
                    val c = rawConstraint as? JsonObject ?: return@mapIndexedNotNull null
                    val cType = c["type"]?.asString()?.lowercase() ?: "distance"
                    ConstructionConstraintDraft(
                        id = c["id"]?.asString() ?: "${obj["id"]?.asString().orEmpty()}-constraint-$cIndex",
                        type = cType,
                        points = c["points"].asArrayOrEmpty().mapNotNull { it.asString()?.trim() }.filter(String::isNotBlank),
                        value = c["value"]?.asDouble() ?: c["targetValue"]?.asDouble() ?: 0.0,
                        tolerance = c["tolerance"]?.asDouble() ?: GeometryConstructionEvaluator.defaultTolerance(cType),
                        score = c["score"]?.asDouble() ?: 1.0
                    )
                },
                rubric = key["rubric"]?.asString().orEmpty(),
                solutionSteps = obj["solutionSteps"].asArrayOrEmpty().mapNotNull { it.asString() },
                assessmentParts = obj["assessmentParts"].asArrayOrEmpty().mapIndexedNotNull { partIndex, rawPart ->
                    val part = rawPart as? JsonObject ?: return@mapIndexedNotNull null
                    AssessmentPartDraft(
                        id = part["id"]?.asString() ?: "${obj["id"]?.asString().orEmpty()}-part-$partIndex",
                        label = part["label"]?.asString().orEmpty(),
                        prompt = part["prompt"]?.asString().orEmpty(),
                        score = part["score"]?.asDouble() ?: 0.0,
                        rubric = key["partRubrics"].asArrayOrEmpty().getOrNull(partIndex)?.asString().orEmpty(),
                        carriesForward = part["carriesForward"]?.asBoolean() ?: false,
                        reasonPrompt = part["reasonPrompt"]?.asString().orEmpty(),
                        requiresReason = part["requiresReason"]?.asBoolean() ?: false
                    )
                },
                score = obj["score"]?.asDouble() ?: 1.0,
                grade = obj["grade"]?.asInt()?.coerceIn(0, 6) ?: 0,
                subject = obj["subject"]?.asString().orEmpty(),
                voiceMaxSeconds = obj["voice_max_seconds"]?.asInt()?.coerceIn(10, 300) ?: 0,
                options = obj["options"].asArrayOrEmpty().map { it.asString().orEmpty() },
                optionIds = List(obj["options"].asArrayOrEmpty().size) { UUID.randomUUID().toString() },
                optionImages = obj["optionImages"].asArrayOrEmpty().map { it.asString()?.takeIf(String::isNotBlank) },
                // V64.4 — استایل هر گزینه؛ JSON قدیمی بدون optionStyles = همه null.
                optionStyles = obj["optionStyles"].decodeStyles(),
                matchingLeftStyles = obj["leftStyles"].decodeStyles(),
                matchingRightStyles = obj["rightStyles"].decodeStyles(),
                textSpans = obj["spans"].decodeSpans(),
                // V121 — تراز پاراگرافیِ تکه‌ای؛ غایب = بدون بازهٔ تراز (fallback به textAlign کلی).
                alignSpans = obj["alignSpans"].decodeAlignSpans(),
                correctIndex = key["correctOption"]?.asInt() ?: obj["correctIndex"]?.asInt(),
                expectedText = when (type) {
                    QuestionType.TRUE_FALSE -> (key["correctAnswer"]?.asBoolean() ?: false).toString()
                    QuestionType.FILL_BLANK -> key["accept"].asArrayOrEmpty().mapNotNull(JsonElement::asString).joinToString("|")
                    else -> key["correctAnswer"]?.asString() ?: obj["expectedText"]?.asString().orEmpty()
                },
                expectedNumber = key["answer"]?.asString() ?: obj["expectedNumber"]?.asString().orEmpty(),
                tolerance = key["tolerance"]?.asString() ?: obj["tolerance"]?.asString() ?: "0",
                caseSensitive = key["caseSensitive"]?.asBoolean() ?: obj["caseSensitive"]?.asBoolean() ?: false,
                matchingLeft = obj["leftItems"].asArrayOrEmpty().map { it.asString().orEmpty() },
                matchingLeftIds = List(obj["leftItems"].asArrayOrEmpty().size) { UUID.randomUUID().toString() },
                matchingRight = obj["rightItems"].asArrayOrEmpty().map { it.asString().orEmpty() },
                matchingRightIds = List(obj["rightItems"].asArrayOrEmpty().size) { UUID.randomUUID().toString() },
                matchingPairs = (key["matchAnswer"] as? JsonObject)?.mapNotNull { (left, right) ->
                    val leftIndex = left.toIntOrNull()
                    val rightIndex = right.asInt()
                    if (leftIndex != null && rightIndex != null) leftIndex to rightIndex else null
                }?.toMap().orEmpty(),
                matchingLeftImages = obj["leftImages"].asArrayOrEmpty().map { it.asString()?.takeIf(String::isNotBlank) },
                matchingRightImages = obj["rightImages"].asArrayOrEmpty().map { it.asString()?.takeIf(String::isNotBlank) },
                answerImageMode = obj["allowImages"]?.asString() ?: "no",
                allowAnswerGraph = obj["allowAnswerGraph"]?.asBoolean() ?: false,
                maxAnswerImages = obj["maxImages"]?.asInt() ?: 0,
                images = imageUrls.mapIndexed { imageIndex, url ->
                    val pos = positions.getOrNull(imageIndex) as? JsonObject
                    MediaDraft(
                        uri = url,
                        xMm = pos?.get("x")?.asFloat() ?: obj["imgX"]?.asFloat() ?: 20f,
                        yMm = pos?.get("y")?.asFloat() ?: obj["imgY"]?.asFloat() ?: 30f,
                        widthMm = pos?.get("w")?.asFloat() ?: obj["imgW"]?.asFloat() ?: 55f
                    )
                },
                textAlign = obj["align"]?.asString()?.takeIf { it in setOf("right", "center", "left", "justify") } ?: "right",
                imagePosition = obj["imgPos"]?.asString()?.takeIf { it in setOf("above", "below", "right", "left", "free") } ?: "below",
                fontFamily = obj["font"]?.asString().orEmpty().ifBlank { "default" },
                fontSizeSp = (obj["fontSize"]?.asFloat() ?: 16f).coerceIn(8f, 40f),
                bold = obj["bold"]?.asBoolean() ?: false,
                italic = obj["italic"]?.asBoolean() ?: false,
                answerLines = (obj["answerLines"]?.asInt() ?: if (type == QuestionType.ESSAY) 5 else 2).coerceIn(0, 12),
                answerLineStyle = obj["answerLineStyle"]?.asString()?.takeIf { it in setOf("lined", "blank", "grid") } ?: "lined",
                answerLineSpacingCm = ((obj["answerLineSpacingCm"] as? JsonPrimitive)?.floatOrNull ?: 1.0f).coerceIn(0.5f, 2.0f),
                optionsLayout = obj["optionsLayout"]?.asString()?.takeIf { it in setOf("1row", "2rows", "4rows") } ?: "",
                // V135 — صوت سؤال
                audioUri = obj["audio"]?.asString()?.takeIf(String::isNotBlank),
                audioBytes = (obj["audioBytes"] as? JsonPrimitive)?.longOrNull ?: 0L,
                audioMs = (obj["audioMs"] as? JsonPrimitive)?.longOrNull ?: 0L,
                // V305 / V314.0 — حالت ویژهٔ ابتدایی (مرتب‌سازی / انتخاب روی تصویر / بخش‌کردن / سبد)
                mode = obj["mode"]?.asString()?.takeIf { it in setOf("order", "hotspot", "draw", "dictation", "voice", "syllable", "bucket", "paint", "steps", "proof", "construct") }.orEmpty(),
                // V327 — تنظیم‌های رنگ‌آمیزی و املا
                paintRows = (obj["paintRows"]?.asInt() ?: 4).coerceIn(1, 8),
                paintCols = (obj["paintCols"]?.asInt() ?: 4).coerceIn(1, 8),
                paintGoal = (obj["paintGoal"]?.asInt() ?: 1).coerceAtLeast(1),
                dictationGap = (obj["dict_gap"]?.asInt() ?: 6).coerceIn(3, 20),
                dictationRepeats = (obj["dict_repeat"]?.asInt() ?: 2).coerceIn(1, 3),
                dictationWords = obj["speak"]?.asString().orEmpty(),
                // ترتیب درست فقط در کلید پاسخ (accept[0]) است؛ public items بُرخورده است.
                orderItems = if (obj["mode"]?.asString() != "order") emptyList() else key["accept"].asArrayOrEmpty().firstOrNull()?.asString()
                    ?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() }
                    ?: obj["items"].asArrayOrEmpty().map { it.asString().orEmpty() },
                hotspots = obj["hotspots"].asArrayOrEmpty().mapNotNull { h ->
                    (h as? JsonObject)?.let { o ->
                        HotspotRect(o["x"]?.asFloat() ?: return@let null, o["y"]?.asFloat() ?: return@let null, o["w"]?.asFloat() ?: return@let null, o["h"]?.asFloat() ?: return@let null)
                    }
                },
                rawPublic = obj,
                rawAnswer = key
            )
        }
    }

    fun encode(questions: List<QuestionDraft>): EncodedQuestions {
        val publicRows = questions.map { question ->
            val values = question.rawPublic.toMutableMap()
            ANSWER_FIELDS.forEach(values::remove)
            values["id"] = JsonPrimitive(question.id)
            values["type"] = JsonPrimitive(typeValue(question.type))
            values["text"] = JsonPrimitive(question.text.trim())
            values["score"] = JsonPrimitive(question.score)
            // V327 — متن روخوانی مشترک (درک مطلب); مقدار خالی کلید را حذف می‌کند.
            if (question.passage.isNotBlank()) values["passage"] = JsonPrimitive(question.passage) else values.remove("passage")
            if (question.sectionTitle.isNotBlank()) values["sectionTitle"] = JsonPrimitive(question.sectionTitle) else values.remove("sectionTitle")
            if (question.topic.isNotBlank()) values["topic"] = JsonPrimitive(question.topic) else values.remove("topic")
            if (question.difficulty in 1..3) values["difficulty"] = JsonPrimitive(question.difficulty) else values.remove("difficulty")
            if (question.learningObjective.isNotBlank()) values["learningObjective"] = JsonPrimitive(question.learningObjective.trim()) else values.remove("learningObjective")
            values.remove("rubric")
            if (question.solutionSteps.isNotEmpty()) values["solutionSteps"] = JsonArray(question.solutionSteps.map(::JsonPrimitive)) else values.remove("solutionSteps")
            if (question.assessmentParts.isNotEmpty()) values["assessmentParts"] = JsonArray(question.assessmentParts.map { part ->
                JsonObject(mapOf("id" to JsonPrimitive(part.id), "label" to JsonPrimitive(part.label), "prompt" to JsonPrimitive(part.prompt), "score" to JsonPrimitive(part.score), "carriesForward" to JsonPrimitive(part.carriesForward), "reasonPrompt" to JsonPrimitive(part.reasonPrompt), "requiresReason" to JsonPrimitive(part.requiresReason)))
            }) else values.remove("assessmentParts")
            if (question.proofAssumptions.isNotBlank()) values["proofAssumptions"] = JsonPrimitive(question.proofAssumptions) else values.remove("proofAssumptions")
            if (question.proofGoal.isNotBlank()) values["proofGoal"] = JsonPrimitive(question.proofGoal) else values.remove("proofGoal")
            if (question.constructionPrompt.isNotBlank()) values["constructionPrompt"] = JsonPrimitive(question.constructionPrompt) else values.remove("constructionPrompt")
            val derivedLabels = GeometryConstructionEvaluator.derivePublicLabels(
                question.constructionTargets.map { GeometryConstructionEvaluator.TargetSpec(it.label, it.x, it.y, it.tolerance, it.score) },
                question.constructionConstraints.map { GeometryConstructionEvaluator.ConstraintSpec(it.id, it.type, it.points, it.value, it.tolerance, it.score) }
            )
            val derivedAngleLabels = GeometryConstructionEvaluator.derivePublicAngleLabels(
                question.constructionAngleLabels,
                question.constructionConstraints.map { GeometryConstructionEvaluator.ConstraintSpec(it.id, it.type, it.points, it.value, it.tolerance, it.score) }
            )
            if (derivedLabels.isNotEmpty()) values["constructionLabels"] = JsonArray(derivedLabels.map(::JsonPrimitive)) else values.remove("constructionLabels")
            if (derivedAngleLabels.isNotEmpty()) values["constructionAngleLabels"] = JsonArray(derivedAngleLabels.map(::JsonPrimitive)) else values.remove("constructionAngleLabels")
            values.remove("constructionTargets")
            values.remove("constructionConstraints")
            // V308 / ۱۴ — برچسب پایه/درس فقط وقتی پر است نوشته می‌شود
            if (question.grade in 1..6) values["grade"] = JsonPrimitive(question.grade)
            if (question.subject.isNotBlank()) values["subject"] = JsonPrimitive(question.subject.trim())
            values["images"] = JsonArray(question.images.map { JsonPrimitive(it.uri) })
            values["image"] = question.images.firstOrNull()?.let { JsonPrimitive(it.uri) } ?: JsonNull
            values["imgFreePositions"] = JsonArray(question.images.map { image ->
                JsonObject(mapOf(
                    "x" to JsonPrimitive(image.xMm),
                    "y" to JsonPrimitive(image.yMm),
                    "w" to JsonPrimitive(image.widthMm)
                ))
            })
            // V135 — صوت سؤال (URL + حجم فشرده + مدت)؛ بدون صوت، کلیدها حذف می‌شوند.
            if (!question.audioUri.isNullOrBlank()) {
                values["audio"] = JsonPrimitive(question.audioUri)
                values["audioBytes"] = JsonPrimitive(question.audioBytes)
                values["audioMs"] = JsonPrimitive(question.audioMs)
            } else {
                values.remove("audio"); values.remove("audioBytes"); values.remove("audioMs")
            }
            values["allowImages"] = JsonPrimitive(question.answerImageMode)
            values["allowAnswerGraph"] = JsonPrimitive(question.allowAnswerGraph)
            values["maxImages"] = JsonPrimitive(if (question.answerImageMode == "no") 0 else question.maxAnswerImages.coerceIn(1, 10))
            values["align"] = JsonPrimitive(question.textAlign)
            values["imgPos"] = JsonPrimitive(question.imagePosition)
            values["font"] = JsonPrimitive(question.fontFamily)
            values["fontSize"] = JsonPrimitive(question.fontSizeSp.coerceIn(8f, 40f))
            values["bold"] = JsonPrimitive(question.bold)
            values["italic"] = JsonPrimitive(question.italic)
            values["answerLines"] = JsonPrimitive(question.answerLines.coerceIn(0, 12))
            values["answerLineStyle"] = JsonPrimitive(question.answerLineStyle)
            values["answerLineSpacingCm"] = JsonPrimitive(question.answerLineSpacingCm.coerceIn(0.5f, 2.0f))
            // V208 — فقط وقتی سؤال چیدمان خودش را دارد نوشته می‌شود (خالی = پیش‌فرض آزمون)
            if (question.optionsLayout.isNotBlank()) values["optionsLayout"] = JsonPrimitive(question.optionsLayout)
            // V68.3.1 — استایل تکه‌ای متن برای «هر نوع سؤال» نوشته می‌شود (در
            // V68.0 اشتباهاً داخل شاخهٔ MATCHING بود و roundtrip شکست می‌خورد).
            encodeSpans(question.textSpans)?.let { values["spans"] = it }
            // V121 — تراز پاراگرافیِ تکه‌ای؛ فقط وقتی بازه‌ای هست نوشته می‌شود تا
            // JSON قدیمیِ سؤال‌های بدونِ این ویژگی دست‌نخورده بماند.
            encodeAlignSpans(question.alignSpans)?.let { values["alignSpans"] = it }
            if (question.type == QuestionType.MULTIPLE_CHOICE) {
                values["options"] = JsonArray(question.options.map(::JsonPrimitive))
                values["optionImages"] = JsonArray(question.options.indices.map { index ->
                    question.optionImages.getOrNull(index)?.let(::JsonPrimitive) ?: JsonPrimitive("")
                })
                // V64.4 — فقط وقتی استایلی هست بنویس تا JSON قدیمی دست‌نخورده بماند.
                encodeStyles(question.optionStyles, question.options.size)?.let {
                    values["optionStyles"] = it
                }
            }
            // V305 / V314.0 — حالت ویژه: mode/items/hotspots/buckets/syllableCount فقط وقتی فعال است نوشته می‌شود
            values.remove("mode"); values.remove("items"); values.remove("hotspots"); values.remove("speak"); values.remove("buckets"); values.remove("syllableCount")
            values.remove("paintRows"); values.remove("paintCols"); values.remove("paintGoal"); values.remove("dict_gap"); values.remove("dict_repeat")
            // V306 — پاسخ با تخته: فقط نشانهٔ حالت؛ پاسخ دانش‌آموز تصویر تخته است.
            if (question.type == QuestionType.ESSAY && question.mode == "draw") values["mode"] = JsonPrimitive("draw")
            // V309 — پاسخ صوتی: حالت و سقف ضبط (ثانیه) نوشته می‌شود
            if (question.type == QuestionType.ESSAY && question.mode == "voice") {
                values["mode"] = JsonPrimitive("voice")
                values["voice_max_seconds"] = JsonPrimitive(question.voiceMaxSeconds.takeIf { it > 0 }?.coerceIn(10, 300) ?: 60)
            }
            if (question.type == QuestionType.ESSAY && question.mode in setOf("steps", "proof", "construct")) {
                values["mode"] = JsonPrimitive(question.mode)
            }
            // V306 — املا: صدای معلم اولویت دارد؛ اگر صدا نباشد، متن املا برای «بخوان» (TTS دستگاه) در سؤال عمومی می‌آید.
            if (question.type == QuestionType.FILL_BLANK && question.mode == "dictation") {
                values["mode"] = JsonPrimitive("dictation")
                values["dict_gap"] = JsonPrimitive(question.dictationGap.coerceIn(3, 20))
                values["dict_repeat"] = JsonPrimitive(question.dictationRepeats.coerceIn(1, 3))
                if (question.audioUri.isNullOrBlank()) values["speak"] = JsonPrimitive(question.dictationWords.ifBlank { question.expectedText.split('|').first().trim() })
            }
            // V327 — رنگ‌آمیزی خانه‌های شبکه: ابعاد و هدف در دادهٔ عمومی؛ پاسخ درست فقط در کلید.
            if (question.type == QuestionType.FILL_BLANK && question.mode == "paint") {
                values["mode"] = JsonPrimitive("paint")
                values["paintRows"] = JsonPrimitive(question.paintRows.coerceIn(1, 8))
                values["paintCols"] = JsonPrimitive(question.paintCols.coerceIn(1, 8))
                values["paintGoal"] = JsonPrimitive(question.paintGoal.coerceIn(1, question.paintRows.coerceIn(1, 8) * question.paintCols.coerceIn(1, 8)))
            }
            if (question.type == QuestionType.FILL_BLANK && question.mode == "order") {
                values["mode"] = JsonPrimitive("order")
                // V305 — امنیت: موارد عمومی همیشه بُرخورده‌اند تا ترتیب درست در پاسخ عمومی لو نرود.
                values["items"] = JsonArray(shuffledOrderPublic(question.orderItems).map { JsonPrimitive(it) })
            }
            // V314.0 — بخش‌کردن و صداکشی: تعداد بخش‌ها در دادهٔ عمومی قرار می‌گیرد
            if (question.type == QuestionType.FILL_BLANK && question.mode == "syllable") {
                values["mode"] = JsonPrimitive("syllable")
                val cnt = question.expectedText.split('|').firstOrNull().orEmpty().split('-').map { it.trim() }.count { it.isNotEmpty() }.coerceIn(2, 10)
                values["syllableCount"] = JsonPrimitive(cnt)
            }
            // V314.0 — دسته‌بندی در سبد: نام سبدها و موارد بُرخورده در دادهٔ عمومی قرار می‌گیرد
            if (question.type == QuestionType.FILL_BLANK && question.mode == "bucket") {
                val parsed = parseBucketSpec(question.expectedText)
                if (parsed.isNotEmpty()) {
                    values["mode"] = JsonPrimitive("bucket")
                    values["buckets"] = JsonArray(parsed.map { JsonPrimitive(it.first) })
                    values["items"] = JsonArray(shuffledOrderPublic(parsed.flatMap { it.second }).map { JsonPrimitive(it) })
                }
            }
            if (question.type == QuestionType.MULTIPLE_CHOICE && question.mode == "hotspot") {
                values["mode"] = JsonPrimitive("hotspot")
                values["hotspots"] = JsonArray(question.options.indices.map { i ->
                    val r = question.hotspots.getOrNull(i)
                    JsonObject(mapOf("x" to JsonPrimitive(r?.x ?: 0f), "y" to JsonPrimitive(r?.y ?: 0f), "w" to JsonPrimitive(r?.w ?: 0f), "h" to JsonPrimitive(r?.h ?: 0f)))
                })
            }
            if (question.type == QuestionType.MATCHING) {
                values["leftItems"] = JsonArray(question.matchingLeft.map(::JsonPrimitive))
                values["rightItems"] = JsonArray(question.matchingRight.map(::JsonPrimitive))
                // V64.5 — استایل مستقل سمت‌ها؛ فقط وقتی استایلی هست.
                encodeStyles(question.matchingLeftStyles, question.matchingLeft.size)?.let {
                    values["leftStyles"] = it
                }
                encodeStyles(question.matchingRightStyles, question.matchingRight.size)?.let {
                    values["rightStyles"] = it
                }
                values["leftImages"] = JsonArray(question.matchingLeft.indices.map { index ->
                    question.matchingLeftImages.getOrNull(index)?.let(::JsonPrimitive) ?: JsonPrimitive("")
                })
                values["rightImages"] = JsonArray(question.matchingRight.indices.map { index ->
                    question.matchingRightImages.getOrNull(index)?.let(::JsonPrimitive) ?: JsonPrimitive("")
                })
            }
            JsonObject(values)
        }

        val answerRows = questions.mapIndexed { index, question ->
            val values = question.rawAnswer.toMutableMap()
            values["i"] = JsonPrimitive(index)
            if (question.rubric.isNotBlank()) values["rubric"] = JsonPrimitive(question.rubric) else values.remove("rubric")
            if (question.assessmentParts.isNotEmpty()) values["partRubrics"] = JsonArray(question.assessmentParts.map { JsonPrimitive(it.rubric) }) else values.remove("partRubrics")
            if (question.constructionTargets.isNotEmpty()) values["constructionTargets"] = JsonArray(question.constructionTargets.map { target -> JsonObject(mapOf("id" to JsonPrimitive(target.id), "label" to JsonPrimitive(target.label), "x" to JsonPrimitive(target.x), "y" to JsonPrimitive(target.y), "tolerance" to JsonPrimitive(target.tolerance), "score" to JsonPrimitive(target.score))) }) else values.remove("constructionTargets")
            if (question.constructionConstraints.isNotEmpty()) values["constructionConstraints"] = JsonArray(question.constructionConstraints.map { c ->
                JsonObject(mapOf(
                    "id" to JsonPrimitive(c.id),
                    "type" to JsonPrimitive(c.type.lowercase()),
                    "points" to JsonArray(c.points.map { JsonPrimitive(it.trim()) }),
                    "value" to JsonPrimitive(c.value),
                    "tolerance" to JsonPrimitive(c.tolerance),
                    "score" to JsonPrimitive(c.score)
                ))
            }) else values.remove("constructionConstraints")
            when (question.type) {
                QuestionType.MULTIPLE_CHOICE -> values["correctOption"] = JsonPrimitive(question.correctIndex ?: 0)
                QuestionType.TRUE_FALSE -> values["correctAnswer"] = JsonPrimitive(question.expectedText.toBooleanStrictOrNull() ?: false)
                QuestionType.FILL_BLANK -> {
                    // V305 / V314.0 — مرتب‌سازی، بخش‌کردن و دسته‌بندی سبدی
                    values["accept"] = when (question.mode) {
                        "order" -> JsonArray(listOf(JsonPrimitive(orderAnswerOf(question.orderItems))))
                        "syllable" -> JsonArray(
                            question.expectedText.split('|').map { variant ->
                                variant.split('-').map { it.trim() }.filter { it.isNotEmpty() }.joinToString("-")
                            }.filter(String::isNotBlank).map(::JsonPrimitive)
                        )
                        "bucket" -> {
                            val canon = bucketCanonicalAnswer(parseBucketSpec(question.expectedText))
                            JsonArray(listOf(JsonPrimitive(canon.ifBlank { question.expectedText.trim() })))
                        }
                        else -> JsonArray(
                            question.expectedText.split('|').map(String::trim).filter(String::isNotBlank).map(::JsonPrimitive)
                        )
                    }
                    values["caseSensitive"] = JsonPrimitive(question.caseSensitive)
                }
                QuestionType.NUMERIC -> {
                    values["answer"] = question.expectedNumber.toDoubleOrNull()?.let(::JsonPrimitive) ?: JsonNull
                    values["tolerance"] = JsonPrimitive(question.tolerance.toDoubleOrNull() ?: 0.0)
                }
                QuestionType.MATCHING -> {
                    values["matchAnswer"] = JsonObject(question.matchingPairs.mapKeys { it.key.toString() }
                        .mapValues { JsonPrimitive(it.value) })
                }
                else -> Unit
            }
            JsonObject(values)
        }
        return EncodedQuestions(JsonArray(publicRows), JsonArray(answerRows))
    }

    /** V305 — بُرخوردن موارد برای نمایش عمومی (با تلاش برای این‌که با ترتیب درست فرق داشته باشد). */
    fun shuffledOrderPublic(items: List<String>): List<String> {
        val clean = items.map { it.trim() }.filter { it.isNotBlank() }
        if (clean.size < 2) return clean
        var out = clean.shuffled()
        var tries = 0
        while (out == clean && tries < 20) { out = clean.shuffled(); tries++ }
        if (out == clean) out = clean.drop(1) + clean.first()
        return out
    }

    /** V305 — رشتهٔ پاسخ مرتب‌سازی: موارد با «,» (سرور «،» را هم به «,» نرمال می‌کند؛ فاصله‌های اطراف حذف). */
    fun orderAnswerOf(items: List<String>): String = items.map { it.trim().replace("،", ",") }.filter { it.isNotBlank() }.joinToString(",")

    /** V314.0 — تجزیهٔ فرمت دسته‌بندی در سبد: «سبد ۱: الف، ب | سبد ۲: ج، د» */
    fun parseBucketSpec(spec: String): List<Pair<String, List<String>>> =
        spec.split('|').mapNotNull { seg ->
            val idx = seg.indexOf(':')
            if (idx <= 0) null
            else {
                val bName = seg.substring(0, idx).trim()
                val bItems = seg.substring(idx + 1).split(',', '،', '+').map { it.trim() }.filter { it.isNotBlank() }
                if (bName.isBlank() || bItems.isEmpty()) null else bName to bItems
            }
        }

    /** V314.0 — رشتهٔ استاندارد پاسخ دسته‌بندی سبدی (موارد داخل هر سبد مرتب می‌شوند تا ترتیب مهم نباشد). */
    fun bucketCanonicalAnswer(buckets: List<Pair<String, List<String>>>): String =
        buckets.joinToString("|") { (name, list) -> "${name.trim()}:${list.map { it.trim() }.filter { it.isNotBlank() }.sorted().joinToString("+")}" }

    private fun parseType(value: String?): QuestionType = when (value?.lowercase()) {
        "multiple", "multiple_choice", "multiplechoice" -> QuestionType.MULTIPLE_CHOICE
        "truefalse", "true_false" -> QuestionType.TRUE_FALSE
        "fill", "fill_blank" -> QuestionType.FILL_BLANK
        "numeric", "number" -> QuestionType.NUMERIC
        "matching", "match" -> QuestionType.MATCHING
        else -> QuestionType.ESSAY
    }

    private fun typeValue(type: QuestionType): String = when (type) {
        QuestionType.ESSAY -> "essay"
        QuestionType.MULTIPLE_CHOICE -> "multiple"
        QuestionType.TRUE_FALSE -> "truefalse"
        QuestionType.FILL_BLANK -> "fill"
        QuestionType.NUMERIC -> "numeric"
        QuestionType.MATCHING -> "matching"
    }

    private val ANSWER_FIELDS = setOf(
        "correctOption", "correctAnswer", "accept", "answer", "tolerance", "caseSensitive",
        "matchAnswer", "pairs", "rubric", "partRubrics", "constructionTargets", "constructionConstraints"
    )
}

private fun JsonElement?.asArrayOrEmpty(): JsonArray = this as? JsonArray ?: JsonArray(emptyList())
private fun JsonElement?.asString(): String? = (this as? JsonPrimitive)?.contentOrNull
private fun JsonElement?.asInt(): Int? = (this as? JsonPrimitive)?.intOrNull
private fun JsonElement?.asDouble(): Double? = (this as? JsonPrimitive)?.doubleOrNull
private fun JsonElement?.asFloat(): Float? = (this as? JsonPrimitive)?.floatOrNull
private fun JsonElement?.asBoolean(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull
