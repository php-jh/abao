package com.abao.speaking.logic

import com.abao.speaking.data.AppData
import com.abao.speaking.model.ScoreResult
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 与网页 script.js 保持一致的工具与业务逻辑。
 */
object WebScript {
    fun audioSlug(text: String): String {
        var slug = text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        if (slug.length > 80) {
            slug = slug.take(80).trimEnd('-')
        }
        return slug
    }

    fun normalize(text: String) =
        text.lowercase().replace(Regex("[^a-z\\s']"), " ").replace(Regex("\\s+"), " ").trim()

    fun clamp(value: Double, minValue: Double, maxValue: Double) =
        max(minValue, min(maxValue, value))

    fun speedNeedleDegrees(wpm: Int): Float {
        val deg = clamp((wpm / 220.0) * 130 - 65, -65.0, 65.0)
        return deg.toFloat()
    }

    fun scoreAnswer(answer: String, target: String, elapsedSeconds: Double): ScoreResult {
        val answerWords = normalize(answer).split(" ").filter { it.isNotEmpty() }
        val targetWords = normalize(target).split(" ").filter { it.isNotEmpty() }
        val overlap = answerWords.count { targetWords.contains(it) }
        val coverage = if (targetWords.isEmpty()) 0.0 else overlap.toDouble() / targetWords.size
        val lengthRatio = min(1.0, answerWords.size.toDouble() / max(1, targetWords.size))
        val hasVerb = Regex(
            "\\b(am|is|are|was|were|be|used|struggle|improve|joined|became|started|make|finish|feel|share|practice|help|learn|grow|overcome)\\b",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(answer)
        val hasConnector = Regex("\\b(and|because|with|for|to|as|if|when)\\b", RegexOption.IGNORE_CASE)
            .containsMatchIn(answer)
        val wpm = ((answerWords.size / max(5.0, elapsedSeconds)) * 60).roundToInt()
        val speedScore = clamp(100 - kotlin.math.abs(wpm - 115) * 0.8, 55.0, 100.0).roundToInt()

        val values = linkedMapOf(
            "语句词汇搭配" to clamp(coverage * 84 + if (hasConnector) 12 else 0, 45.0, 100.0).roundToInt(),
            "用词准确度" to clamp(coverage * 90 + 8, 48.0, 100.0).roundToInt(),
            "句子结构" to clamp((if (hasVerb) 64 else 38) + (if (hasConnector) 22 else 8) + lengthRatio * 14, 42.0, 100.0).roundToInt(),
            "发音" to clamp(coverage * 74 + speedScore * 0.22, 50.0, 100.0).roundToInt(),
            "语速" to speedScore,
            "流利度" to clamp(lengthRatio * 68 + speedScore * 0.26, 48.0, 100.0).roundToInt(),
            "完整度" to clamp(lengthRatio * 92 + 6, 40.0, 100.0).roundToInt()
        )
        val total = values.values.sum() / AppData.metrics.size
        return ScoreResult(values, total, wpm, coverage)
    }

    fun buildAnalysis(result: ScoreResult): String {
        val weak = result.values.entries.sortedBy { it.value }.take(2).joinToString("、") { it.key }
        return "本次回答综合分 ${result.total}。主要优势是能覆盖情景关键词；建议重点提升 $weak。回答时可补充问题、行动和积极结果，让表达更完整。"
    }

    fun suggestCorrection(answer: String, target: String): String {
        val normalized = normalize(answer)
        if (!Regex(
                "\\b(am|is|are|would|need|work|join|look|make|share|ask|used|struggle|improve|became|started|finish|feel|practice)\\b",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(answer)
        ) {
            return "请补充清晰谓语结构。可参考：$target"
        }
        if (!normalized.contains("as a result")) {
            return "建议补充结果句。可参考：$target"
        }
        if (answer.split(Regex("\\s+")).size < 7) {
            return "回答偏短，可以扩展为：$target"
        }
        return target
    }
}
