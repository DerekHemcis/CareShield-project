package com.careshield.app.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRulesTest {

    // ---- 序列化/反序列化 ----

    @Test
    fun `序列化往返一致`() {
        val rules = listOf(
            AppRules.Rule("com.tencent.qqlive", text = "跳过", x = 0.955f, y = 0.06f),
            AppRules.Rule("com.sohu.sohuvideo", x = 0.9f, y = 0.1f),
            AppRules.Rule("com.qiyi.video", text = "关闭")
        )
        val json = AppRules.serialize(rules)
        val back = AppRules.deserialize(json)
        assertEquals(3, back.size)
        assertEquals("com.tencent.qqlive", back[0].pkg)
        assertEquals("跳过", back[0].text)
        assertEquals(0.955f, back[0].x!!, 1e-4f)
        assertNull(back[1].text)
        assertEquals(0.9f, back[1].x!!, 1e-4f)
    }

    @Test
    fun `反序列化 - 空串与坏JSON返回空表`() {
        assertTrue(AppRules.deserialize("").isEmpty())
        assertTrue(AppRules.deserialize("not json").isEmpty())
        assertTrue(AppRules.deserialize("[]").isEmpty())
    }

    @Test
    fun `反序列化 - 非法坐标被剔除`() {
        val json = """[{"pkg":"a","x":1.5,"y":0.1},{"pkg":"b","x":0.5,"y":-0.2},{"pkg":"c"}]"""
        val rules = AppRules.deserialize(json)
        // a 的 x 越界→无效整条剔除；b 的 y 越界→剔除；c 无 text 无坐标→剔除
        assertTrue(rules.isEmpty())
    }

    @Test
    fun `反序列化 - 坐标边界值0和1合法`() {
        val json = """[{"pkg":"a","x":0.0,"y":1.0}]"""
        assertEquals(1, AppRules.deserialize(json).size)
    }

    // ---- upsert/remove ----

    @Test
    fun `upsert - 同pkg替换不重复`() {
        var rules = listOf(AppRules.Rule("a", text = "跳过"))
        rules = AppRules.upsert(rules, AppRules.Rule("a", x = 0.9f, y = 0.1f))
        assertEquals(1, rules.size)
        assertNull(rules[0].text)
        rules = AppRules.upsert(rules, AppRules.Rule("b", text = "关闭"))
        assertEquals(2, rules.size)
    }

    @Test
    fun `upsert - 无效规则不加入`() {
        val rules = AppRules.upsert(emptyList(), AppRules.Rule("a"))
        assertTrue(rules.isEmpty())
    }

    @Test
    fun `remove - 按pkg删除`() {
        var rules = listOf(AppRules.Rule("a", text = "x"), AppRules.Rule("b", text = "y"))
        rules = AppRules.remove(rules, "a")
        assertEquals(1, rules.size)
        assertEquals("b", rules[0].pkg)
    }

    // ---- 文本匹配 ----

    @Test
    fun `匹配 - 短节点含关键词命中`() {
        val rule = AppRules.Rule("a", text = "跳过")
        assertTrue(AppRules.textMatches(rule, "跳过", null))
        assertTrue(AppRules.textMatches(rule, "3s跳过", null))
        assertTrue(AppRules.textMatches(rule, null, "点击跳过"))
    }

    @Test
    fun `匹配 - 长句不命中`() {
        val rule = AppRules.Rule("a", text = "跳过")
        assertFalse(AppRules.textMatches(rule, "这是很长的一句话里面恰好有跳过两个字但绝对不是广告按钮", null))
    }

    @Test
    fun `匹配 - 完全等于关键词即使较长也命中`() {
        val rule = AppRules.Rule("a", text = "关闭广告")
        assertTrue(AppRules.textMatches(rule, "关闭广告", null))
    }

    @Test
    fun `匹配 - 无text规则的控件匹配不命中`() {
        val rule = AppRules.Rule("a", x = 0.5f, y = 0.5f)
        assertFalse(AppRules.textMatches(rule, "跳过", null))
    }
}
