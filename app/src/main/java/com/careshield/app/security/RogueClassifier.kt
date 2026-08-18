package com.careshield.app.security

/**
 * 流氓软件判定引擎（纯规则、可解释）
 * 判定分类：1 色情 2 赌博 3 非法贷款 4 私彩 5 病毒 6 传统流氓 7 不明来源
 */
object RogueClassifier {

    /** 判定动作 */
    enum class Action { BLOCK, WARN, SKIP }

    /** 判定结果 */
    data class Verdict(val action: Action, val category: Int, val reason: String)

    /** 可信白名单包名（官方持牌机构、正规应用，放行不拦），可扩充 */
    private val TRUSTED_PKGS = setOf(
        // 银行 / 消费金融官方
        "com.icbc", "com.ccb.longrui", "com.abchina.android", "com.cmbchina.ccd.pluto.cmbActivity",
        "com.micbank.boc", "com.citicbank.mobilebank", "com.spdbccc.app", "com.unionpay",
        // 官方购彩 / 应用商店
        "com.tencent.tbs", "com.coolapk.market", "com.tencent.mm", "com.ss.android.ugc.aweme"
    )

    /** 色情类关键词 */
    private val SEXUAL = listOf("色情","约炮","裸聊","撸","成人","色图","黄片","陪聊","招嫖","摸鱼","福利姬","深夜聊天")

    /** 赌博类关键词 */
    private val GAMBLE = listOf("棋牌","博彩","真人视讯","百家乐","龙虎","时时彩","澳门娱乐","网络赌场","现金棋牌","德扑","扎金花")

    /** 非法贷款类关键词 */
    private val LOAN = listOf("秒批","极速放款","网贷","无抵押贷","高利贷","砍头息","714","套路贷","黑户贷款","不看征信")

    /** 私彩类关键词 */
    private val LOTTERY = listOf("六合彩","私彩","境外彩票","彩票预测","港彩","内幕码")

    /**
     * 判定是否拦截。按类别依次检查，命中即返回对应动作。
     * @param pkg 包名
     * @param label 应用名（可能为空）
     * @param hasMedia 是否含关键高风险权限
     */
    fun classify(pkg: String, label: String, hasDeviceAdmin: Boolean,
                 hasAccessibility: Boolean, hasSms: Boolean): Verdict {
        // 0. 白名单豁免
        if (TRUSTED_PKGS.contains(pkg)) return Verdict(Action.SKIP, 0, "可信白名单")

        val text = "$pkg $label"

        // 类别1 色情
        SEXUAL.forEach { if (text.contains(it)) return Verdict(Action.BLOCK, 1, "色情:含'$it'") }
        // 类别2 赌博
        GAMBLE.forEach { if (text.contains(it)) return Verdict(Action.BLOCK, 2, "赌博:含'$it'") }
        // 类别3 非法贷款
        LOAN.forEach { if (text.contains(it)) return Verdict(Action.BLOCK, 3, "贷款:含'$it'") }
        // 类别4 私彩
        LOTTERY.forEach { if (text.contains(it)) return Verdict(Action.BLOCK, 4, "私彩:含'$it'") }

        // 类别5 病毒/木马：高风险权限组合
        if (hasDeviceAdmin && hasSms) return Verdict(Action.BLOCK, 5, "管理+短信高风险权限")
        if (hasAccessibility && !TRUSTED_PKGS.contains(pkg)) {
            // 无障碍权限常被恶意利用
        }

        // 类别7 不明来源：无签名/签名无效由调用方判断
        // 默认放行（需更多信息）
        return Verdict(Action.SKIP, 0, "未匹配明确风险")
    }

    /** 关键词总表（供设置页展示） */
    val CATEGORY_LABELS = listOf(
        "色情", "赌博", "非法贷款", "私彩", "病毒木马", "传统流氓", "不明来源"
    )
}
