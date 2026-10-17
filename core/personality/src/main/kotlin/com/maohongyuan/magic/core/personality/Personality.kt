package com.maohongyuan.magic.core.personality

import com.maohongyuan.magic.core.model.DivinationChart
import com.maohongyuan.magic.core.model.DivinationSystem
import com.maohongyuan.magic.core.resource.JsonResource
import kotlinx.serialization.Serializable

/**
 * 四个维度。
 *
 * [first] 与 [second] 是两极字母。代码里大量用「第一极 = 正分」的约定，
 * 所以顺序不能随意调换。
 */
enum class Dimension(val first: Char, val second: Char, val label: String) {
    EI('E', 'I', "外向 — 内向"),
    SN('S', 'N', "实感 — 直觉"),
    TF('T', 'F', "思考 — 情感"),
    JP('J', 'P', "判断 — 知觉"),
    ;

    fun letterOf(score: Int): Char = if (score >= 0) first else second
}

/**
 * 一道题。
 *
 * 两端是**对立的具体描述**，中间五点。不用「非常同意/不同意」那种 Likert 措辞 ——
 * 那种问法在中文里读着生硬，也容易让人顺着社会期待作答。
 *
 * [left] 标明左侧代表哪个字母。有些题是反向的（左 = I 而非 E），
 * 这样能抵消一味往一边选的倾向。
 */
@Serializable
data class Question(
    val id: Int,
    val dim: String,
    val left: String,
    val l: String,
    val r: String,
) {
    val dimension: Dimension get() = Dimension.valueOf(dim)
}

/** 一种类型的描述 */
@Serializable
data class PersonalityType(
    val code: String,
    val label: String,
    val summary: String,
    val strengths: List<String> = emptyList(),
    val cautions: List<String> = emptyList(),
    val fit: String = "",
)

/**
 * 题库与类型库。
 *
 * `init` 自检：60 题、四维各 15 题、16 型、类型代码覆盖全部组合。
 * 少一题会让该维度分数偏一边；少一型会让人测出空白结果 —— 都必须在加载时炸出来。
 */
object PersonalityData {

    val questions: List<Question> by lazy {
        JsonResource.load<QuestionFile>("/personality/questions.json").questions
    }

    val types: List<PersonalityType> by lazy {
        JsonResource.load<TypeFile>("/personality/types.json").types
    }

    init {
        check(questions.size == QUESTION_COUNT) { "题库必须 $QUESTION_COUNT 题，实际 ${questions.size}" }
        Dimension.entries.forEach { d ->
            val n = questions.count { it.dimension == d }
            check(n == PER_DIMENSION) { "${d.label} 必须 $PER_DIMENSION 题，实际 $n" }
        }
        check(questions.map { it.id }.toSet().size == QUESTION_COUNT) { "题号有重复" }
        check(questions.all { it.left == it.dimension.first.toString() || it.left == it.dimension.second.toString() }) {
            "有题目的 left 字段不是本维度的字母"
        }
        // 反向题必须存在，否则一律往左选就能拿满分
        Dimension.entries.forEach { d ->
            check(questions.any { it.dimension == d && it.left[0] == d.first }) { "${d.label} 缺正向题" }
            check(questions.any { it.dimension == d && it.left[0] == d.second }) { "${d.label} 缺反向题" }
        }

        check(types.size == 16) { "类型必须 16 种，实际 ${types.size}" }
        val expected = buildList {
            listOf("I", "E").forEach { a -> listOf("S", "N").forEach { b ->
                listOf("T", "F").forEach { c -> listOf("J", "P").forEach { d -> add("$a$b$c$d") } }
            } }
        }
        check(types.map { it.code }.toSet() == expected.toSet()) {
            "类型代码未覆盖全部 16 种组合，缺失：${expected.toSet() - types.map { it.code }.toSet()}"
        }
    }

    const val QUESTION_COUNT = 60
    const val PER_DIMENSION = 15

    /** 单题最大贡献：choice 1 或 5 时距中点 2 */
    const val MAX_PER_QUESTION = 2

    fun typeOf(code: String): PersonalityType? = types.firstOrNull { it.code == code }

    @Serializable private data class QuestionFile(val questions: List<Question> = emptyList())
    @Serializable private data class TypeFile(val types: List<PersonalityType> = emptyList())
}

/** 某维度的得分 */
data class DimensionScore(
    val dimension: Dimension,
    /** 正数偏第一极，负数偏第二极 */
    val raw: Int,
    /** 该极的强度百分比（0..100），绝对值 */
    val percent: Int,
) {
    val letter: Char get() = dimension.letterOf(raw)
}

/** 测评结果盘 */
data class PersonalityChart(
    val answers: Map<Int, Int>,
    val scores: List<DimensionScore>,
    val code: String,
    val type: PersonalityType?,
) : DivinationChart {

    override val system: DivinationSystem = DivinationSystem.MBTI

    /** 是否有维度平局 —— 平局时结果稳定性差，UI 应提示 */
    val hasTie: Boolean get() = scores.any { it.raw == 0 }
}

/**
 * 计分。
 *
 * ── 平局规则（显式定义，不是随手写的）──
 * `raw == 0` 时取**第一极**（E / S / T / J）。
 * 不取「随机」也不取「上一题」—— 那样同样答案会得出不同类型，无法复现。
 *
 * ── 为什么用 ±(3 - choice) ──
 * choice 1..5，3 是居中。选 1 给 +2、选 5 给 -2、选 3 给 0，
 * 线性且对称。反向题的 left 是第二极，符号取反即可抵消。
 */
object PersonalityScoring {

    fun score(answers: Map<Int, Int>): PersonalityChart {
        val scores = Dimension.entries.map { dim ->
            var raw = 0
            PersonalityData.questions.filter { it.dimension == dim }.forEach { q ->
                val choice = answers[q.id] ?: 3      // 未答按居中处理
                val magnitude = 3 - choice.coerceIn(1, 5)
                // left 是第一极 → 正分；left 是第二极（反向题）→ 取反
                val sign = if (q.left[0] == dim.first) 1 else -1
                raw += sign * magnitude
            }
            val max = PersonalityData.PER_DIMENSION * PersonalityData.MAX_PER_QUESTION
            DimensionScore(
                dimension = dim,
                raw = raw,
                percent = (kotlin.math.abs(raw) * 100 / max).coerceIn(0, 100),
            )
        }
        val code = scores.joinToString("") { it.letter.toString() }
        return PersonalityChart(
            answers = answers,
            scores = scores,
            code = code,
            type = PersonalityData.typeOf(code),
        )
    }

    /** 是否答完 */
    fun isComplete(answers: Map<Int, Int>): Boolean =
        PersonalityData.questions.all { answers.containsKey(it.id) }
}
