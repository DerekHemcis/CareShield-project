package com.careshield.app.security

import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.net.HttpURLConnection
import java.net.URL

/**
 * Virustotal 客户端：通过 APK 哈希查询云端多引擎判定。
 * API key 由运行时从设置读取（不硬编码），无 key 时降级为本地规则。
 */
object VirusTotalClient {

    private const val BASE = "https://www.virustotal.com/api/v3/files"
    private const val TIMEOUT = 15000

    /** 检测结果 */
    data class Result(
        val harmless: Int = 0,
        val malicious: Int = 0,
        val suspicious: Int = 0,
        val unknown: Int = 0
    ) {
        val isMalicious: Boolean
            get() = malicious > 0 || suspicious > 0

        val riskScore: Int get() = malicious * 2 + suspicious  // 恶意权重高
    }

    /** 计算文件 SHA256 */
    fun sha256(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) { null }
    }

    /**
     * 通过哈希查询 Virustotal
     * @param apiKey API key（由设置传入，为空返回 null）
     * @param sha256 文件哈希
     */
    fun queryHash(apiKey: String, sha256: String): Result? {
        if (apiKey.isBlank()) return null
        return try {
            val url = URL("$BASE/$sha256")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("x-apikey", apiKey)
            conn.connectTimeout = TIMEOUT
            conn.readTimeout = TIMEOUT
            val code = conn.responseCode
            if (code != 200) return null   // 404=未知哈希等
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            parse(body)
        } catch (_: Exception) { null }
    }

    /** 从 Virustotal JSON 解析引擎统计 */
    internal fun parse(body: String): Result {
        return try {
            val stats = JSONObject(body)
                .getJSONObject("data")
                .getJSONObject("attributes")
                .getJSONObject("last_analysis_stats")
            Result(
                harmless = stats.optInt("harmless"),
                malicious = stats.optInt("malicious"),
                suspicious = stats.optInt("suspicious"),
                unknown = stats.optInt("unknown")
            )
        } catch (_: Exception) { Result() }
    }

    /** 本地兜底：危险词/来源判定（无 key 或 API 失败时用） */
    fun localClassify(label: String, pkg: String): Boolean {
        val text = "$pkg $label"
        return DANGER_WORDS.any { text.contains(it) } ||
            DANGER_APPS.any { label.contains(it) || pkg.contains(normalizePkg(it)) }
    }

    private fun normalizePkg(name: String): String = name

    /** 危险关键词（出现即视为高危，阻止下载/安装） */
    private val DANGER_WORDS = listOf(
        "清理大师","极速清理","手机加速","省电大师","垃圾清理","一键省电",
        "金币赚钱","看视频赚钱","走路赚钱","睡觉赚钱","红包提现","免费领",
        "限时领取","立即领取","高额收益","高息理财","保本高收益",
        "补肾","特效药","偏方","根治","降糖神药","降压神药","男科","妇科神药","免费问诊","一对一诊疗",
        "私彩","六合彩","彩票预测","博彩","线上赌场","棋牌赌博","充值上分","金币兑现",
        "套路贷","714高炮","砍头息","无抵押秒放款","低息网贷",
        "色情直播","成人交友","同城约","招嫖","色情资源",
        "破解版","内购破解","去广告破解","游戏外挂","作弊器","自动点击","挂机脚本",
        "弹窗锁屏","劫持主页","自动安装","静默下载","复活残留","一键解锁","root工具","刷机包",
        "私密视频","美女短视频","同城美女","少妇交友",
        "养老理财","养老投资","原始股","私募理财",
        "数字货币挖矿","云挖矿","挂机挖矿","流量劫持",
        "话费充值返利","免费领手机","免费领电动车","免费领手表","扫码领现金","填写信息领红包",
        "代开证明","代办证件","征信修复","洗白征信","逾期消除",
        "短视频带货神药","老偏方","祖传秘方"
    )

    /** 危险 App 名名单（出现即高危） */
    private val DANGER_APPS = listOf(
        "红果短剧","河马短剧","番茄短剧","七猫短剧","星图短剧","多多短剧",
        "番茄畅听","喜马拉雅极速版","百度大字版","搜狐极速版","趣头条","中青看点","惠头条",
        "欢乐斗地主极速版","途游斗地主","同城游",
        "酷狗大字版","酷我音乐极速版","多多视频",
        "淘特极速版","京东极速版","今日头条极速版","抖音极速版","快手极速版"
    )
}
