package com.careshield.app.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AdMatchRules 纯逻辑单测（无 Android 依赖，本地/CI 都能跑）。
 */
class AdMatchRulesTest {

    // ---- 复合关键词：跳过 + 数字 拆在两个子节点 ----

    @Test
    fun `复合 - 跳过和数字分属两个子节点可命中`() {
        val r = AdMatchRules.matchCompositeSkip(listOf("跳过", "5"))
        assertEquals("跳过 5", r)
    }

    @Test
    fun `复合 - 顺序无所谓 数字在前也命中`() {
        assertEquals("跳过 3", AdMatchRules.matchCompositeSkip(listOf("3", "跳过")))
    }

    @Test
    fun `复合 - 繁体与英文跳过也命中`() {
        assertEquals("跳過 10", AdMatchRules.matchCompositeSkip(listOf("跳過", "10")))
        assertEquals("Skip 2", AdMatchRules.matchCompositeSkip(listOf("Skip", "2")))
    }

    @Test
    fun `复合 - 只有跳过没有数字不命中`() {
        assertNull(AdMatchRules.matchCompositeSkip(listOf("跳过", "广告")))
    }

    @Test
    fun `复合 - 只有数字没有跳过不命中`() {
        assertNull(AdMatchRules.matchCompositeSkip(listOf("5", "6")))
    }

    @Test
    fun `复合 - 子节点不足两个不命中`() {
        assertNull(AdMatchRules.matchCompositeSkip(listOf("跳过")))
        assertNull(AdMatchRules.matchCompositeSkip(emptyList()))
    }

    @Test
    fun `复合 - 过长数字串不算倒计时`() {
        // 8 位数字更像时间/号码，不该当倒计时
        assertNull(AdMatchRules.matchCompositeSkip(listOf("跳过", "12345678")))
    }

    // ---- 短文本跳过判定（长度约束）----

    @Test
    fun `短文本 - 跳过加少量后缀仍命中`() {
        // "跳过广告" 4 字 = 关键词 2 字 + 2 字符，≤ 2+6
        assertTrue(AdMatchRules.isShortSkipText("跳过广告"))
        assertTrue(AdMatchRules.isShortSkipText("3s后可跳过"))
    }

    @Test
    fun `短文本 - 长句里含跳过不命中`() {
        val long = "这是一段很长的说明文字里面只是碰巧出现了跳过两个字请不要误点"
        assertFalse(AdMatchRules.isShortSkipText(long))
    }

    @Test
    fun `短文本 - 空串不命中`() {
        assertFalse(AdMatchRules.isShortSkipText(""))
        assertFalse(AdMatchRules.isShortSkipText("   "))
    }

    // ---- 纯数字倒计时 ----

    @Test
    fun `倒计时 - 1到3位数字命中`() {
        assertTrue(AdMatchRules.isShortCountdown("5"))
        assertTrue(AdMatchRules.isShortCountdown("10"))
        assertTrue(AdMatchRules.isShortCountdown(" 99 "))
    }

    @Test
    fun `倒计时 - 非纯数字或过长不命中`() {
        assertFalse(AdMatchRules.isShortCountdown("12a"))
        assertFalse(AdMatchRules.isShortCountdown("1000"))
        assertFalse(AdMatchRules.isShortCountdown(""))
    }

    // ---- 复合容器尺寸预检 ----

    @Test
    fun `尺寸 - 正常按钮通过`() {
        assertTrue(AdMatchRules.compositeSizeOk(300, 120, 1080, 2400))
    }

    @Test
    fun `尺寸 - 超过屏宽70%拒绝`() {
        assertFalse(AdMatchRules.compositeSizeOk(800, 120, 1080, 2400))
    }

    @Test
    fun `尺寸 - 超过屏高25%拒绝`() {
        assertFalse(AdMatchRules.compositeSizeOk(300, 700, 1080, 2400))
    }

    @Test
    fun `尺寸 - 非法输入拒绝`() {
        assertFalse(AdMatchRules.compositeSizeOk(0, 100, 1080, 2400))
        assertFalse(AdMatchRules.compositeSizeOk(100, 100, 0, 2400))
    }
}
