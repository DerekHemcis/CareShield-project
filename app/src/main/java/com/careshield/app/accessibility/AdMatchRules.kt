package com.careshield.app.accessibility

/**
 * 广告匹配纯逻辑（无 Android 依赖，可 JVM 单测）。
 *
 * 借鉴 zfdang/Android-Touch-Helper (SkipAdRules) 的思路：
 *  - 复合关键词：把"跳过"和倒计时数字拆散在不同子节点里的情况拼起来匹配
 *    （如容器内两个子节点分别是 "跳过" 和 "5" → 等效 "跳过 5"）。
 *  - 长度约束：节点文本最多比关键词多 KEYWORD_MAX_EXTRA_LENGTH 个字符，
 *    防止长句里偶然包含关键词被误中。
 *
 * 设计约束：不改 43 稳定版的 scoreNode 语义，这里只做"补充层"，
 * 由 AdBlockerService 在原评分未命中时调用。
 */
object AdMatchRules {

    /** 节点文本最多比关键词多出这么多字符，才认为是一次真正的关键词命中 */
    const val KEYWORD_MAX_EXTRA_LENGTH = 6

    /** 复合容器允许的子节点数量范围（TouchHelper: 2~5） */
    const val COMPOSITE_MIN_CHILDREN = 2
    const val COMPOSITE_MAX_CHILDREN = 5

    /** 复合容器尺寸约束（占屏比例上限），防止把整个页面当按钮 */
    const val COMPOSITE_MAX_WIDTH_RATIO = 0.7f
    const val COMPOSITE_MAX_HEIGHT_RATIO = 0.25f

    /** 复合匹配里数字子节点的最大长度（"10" 可以，"12345678" 不行） */
    const val COMPOSITE_MAX_DIGIT_LEN = 3

    /**
     * 复合关键词判定：给一组子节点文本（按界面顺序），判断是否等效命中
     * "跳过 + 倒计时数字" 组合。
     *
     * 命中条件（全部满足）：
     *  - 至少一个子节点含"跳过/Skip"（短文本，见 isShortSkipText）
     *  - 至少一个子节点是 1~COMPOSITE_MAX_DIGIT_LEN 位的纯数字（倒计时）
     *
     * @return 命中时返回组合出的等效文本（如 "跳过 5"），未命中返回 null
     */
    fun matchCompositeSkip(childTexts: List<String>): String? {
        if (childTexts.size < COMPOSITE_MIN_CHILDREN) return null
        var skipText: String? = null
        var digitText: String? = null
        for (raw in childTexts) {
            val t = raw.trim()
            if (t.isEmpty()) continue
            if (skipText == null && isShortSkipText(t)) skipText = t
            else if (digitText == null && isShortCountdown(t)) digitText = t
        }
        if (skipText != null && digitText != null) return "$skipText $digitText"
        return null
    }

    /** 单节点短文本是否含跳过语义（长度受约束，防长句误中） */
    fun isShortSkipText(t: String): Boolean {
        val s = t.trim()
        if (s.isEmpty()) return false
        val keywords = listOf("跳过", "跳過", "Skip", "skip")
        return keywords.any { kw ->
            s.contains(kw) && s.length <= kw.length + KEYWORD_MAX_EXTRA_LENGTH
        }
    }

    /** 纯数字倒计时片段（1~3 位） */
    fun isShortCountdown(t: String): Boolean {
        val s = t.trim()
        return s.isNotEmpty() && s.length <= COMPOSITE_MAX_DIGIT_LEN && s.all { it.isDigit() }
    }

    /**
     * 容器复合匹配的尺寸预检（数值由调用方量好传入，保持本类无 Android 依赖）。
     * 宽或高超过限制 → 不算广告按钮。
     */
    fun compositeSizeOk(width: Int, height: Int, screenW: Int, screenH: Int): Boolean {
        if (screenW <= 0 || screenH <= 0 || width <= 0 || height <= 0) return false
        return width <= screenW * COMPOSITE_MAX_WIDTH_RATIO &&
            height <= screenH * COMPOSITE_MAX_HEIGHT_RATIO
    }
}
