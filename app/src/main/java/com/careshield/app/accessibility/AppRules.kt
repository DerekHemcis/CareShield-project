package com.careshield.app.accessibility

import org.json.JSONArray
import org.json.JSONObject

/**
 * 每App自定义跳过规则（监护人手动标定，参考 Android-Touch-Helper 的
 * per-app widget/position 规则）。
 *
 * 存储格式（SettingsRepo "app_rules" 一个 JSON 字符串）：
 * [{"pkg":"com.tencent.qqlive","text":"跳过","x":0.955,"y":0.06}]
 *  - pkg  必填，应用包名
 *  - text 可选，控件匹配关键词（无障碍扫到含该词的短节点就点）
 *  - x/y  可选，0.0~1.0 屏幕比例坐标（无障碍扫不到的图片按钮用坐标兜底）
 *
 * 纯逻辑无 Android UI 依赖，可 JVM 单测。
 */
object AppRules {

    data class Rule(
        val pkg: String,
        val text: String? = null,
        val x: Float? = null,
        val y: Float? = null
    ) {
        val valid: Boolean get() = pkg.isNotBlank() && (!text.isNullOrBlank() || (x != null && y != null))
    }

    // ---- 序列化 ----

    fun serialize(rules: List<Rule>): String {
        val arr = JSONArray()
        rules.filter { it.valid }.forEach { r ->
            val o = JSONObject()
            o.put("pkg", r.pkg)
            r.text?.let { o.put("text", it) }
            r.x?.let { o.put("x", it.toDouble()) }
            r.y?.let { o.put("y", it.toDouble()) }
            arr.put(o)
        }
        return arr.toString()
    }

    fun deserialize(json: String): List<Rule> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val pkg = o.optString("pkg", "")
                val text = o.optString("text", "").takeIf { it.isNotBlank() }
                val x = if (o.has("x")) o.optDouble("x", -1.0).toFloat().takeIf { it in 0f..1f } else null
                val y = if (o.has("y")) o.optDouble("y", -1.0).toFloat().takeIf { it in 0f..1f } else null
                Rule(pkg, text, x, y).takeIf { it.valid }
            }
        } catch (_: Exception) { emptyList() }
    }

    // ---- 查询 ----

    fun ruleFor(rules: List<Rule>, pkg: String): Rule? =
        rules.firstOrNull { it.pkg == pkg }

    // ---- 增删 ----

    /** 添加或替换同一 pkg 的规则（每 App 一条） */
    fun upsert(rules: List<Rule>, rule: Rule): List<Rule> {
        if (!rule.valid) return rules
        return rules.filter { it.pkg != rule.pkg } + rule
    }

    fun remove(rules: List<Rule>, pkg: String): List<Rule> =
        rules.filter { it.pkg != pkg }

    // ---- 匹配（纯逻辑部分）----

    /**
     * 控件文本是否命中规则关键词：短节点 + 含关键词（长度约束同 AdMatchRules）。
     */
    fun textMatches(rule: Rule, nodeText: String?, nodeDesc: String?): Boolean {
        val kw = rule.text?.trim().takeUnless { it.isNullOrBlank() } ?: return false
        val t = (nodeText ?: "").trim()
        val d = (nodeDesc ?: "").trim()
        return AdMatchRules.isShortSkipText(t) && t.contains(kw) ||
            AdMatchRules.isShortSkipText(d) && d.contains(kw) ||
            t == kw || d == kw
    }
}
