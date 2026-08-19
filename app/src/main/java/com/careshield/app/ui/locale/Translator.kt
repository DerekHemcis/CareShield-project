package com.careshield.app.ui.locale

import androidx.compose.runtime.*
import com.careshield.app.data.SettingsRepo

data class LangMap(val zh: String = "", val tw: String = "", val en: String = "")

object LangState {
    var current = mutableStateOf("简体中文")
}

@Composable
fun T(zh: String): String {
    val lang = LangState.current.value
    return when (lang) {
        "English" -> translations[zh]?.en?.takeIf { it.isNotEmpty() } ?: zh
        "繁體中文" -> translations[zh]?.tw?.takeIf { it.isNotEmpty() } ?: zh
        else -> zh
    }
}

fun syncLangState(context: android.content.Context) {
    val lang = runCatching {
        kotlinx.coroutines.runBlocking { SettingsRepo(context).get("lang", "简体中文") }
    }.getOrDefault("简体中文")
    LangState.current.value = lang
}

private val translations: Map<String, LangMap> = mapOf(
    // 状态/顶栏
    "无障碍服务运行中" to LangMap(tw = "無障礙服務運行中", en = "Accessibility running"),
    "无障碍服务未开启" to LangMap(tw = "無障礙服務未開啟", en = "Accessibility off"),
    "守护中 · 请放心" to LangMap(tw = "守護中 · 請放心", en = "Protected"),
    "已为家人拦截广告与流氓软件打扰" to LangMap(tw = "已為家人攔截廣告與流氓軟體打擾", en = "Ads & rogue apps blocked"),
    "今日拦截广告" to LangMap(tw = "今日攔截廣告", en = "Ads blocked"),
    "误触弹窗拦截" to LangMap(tw = "誤觸彈窗攔截", en = "Popups blocked"),
    "防装软件" to LangMap(tw = "防裝軟體", en = "Installs blocked"),
    "防护功能" to LangMap(tw = "防護功能", en = "Protection"),
    "增强能力" to LangMap(tw = "增強能力", en = "Enhancement"),
    "通用" to LangMap(tw = "通用", en = "General"),
    "监护" to LangMap(tw = "監護", en = "Guardian"),
    // Shizuku
    "Shizuku 未连接" to LangMap(en = "Shizuku not connected"),
    "高级拦截能力未开启，防流氓软件功能已降级" to LangMap(tw = "高級攔截能力未開啟，防流氓軟體功能已降級", en = "Advanced defense disabled"),
    "去开启" to LangMap(tw = "去開啟", en = "Enable"),
    "去连接" to LangMap(en = "Connect"),
    "Shizuku 已连接" to LangMap(en = "Shizuku connected"),
    "高级拦截能力正常 · 防流氓软件已启用" to LangMap(tw = "高級攔截能力正常 · 防流氓軟體已啟用", en = "Advanced defense active"),
    "Shizuku 连接中" to LangMap(en = "Connecting..."),
    "请在系统弹窗中允许授权" to LangMap(tw = "請在系統彈窗中允許授權", en = "Allow in system dialog"),
    "连接 Shizuku 增强" to LangMap(en = "Connect Shizuku"),
    "开启无障碍服务" to LangMap(tw = "開啟無障礙服務", en = "Enable accessibility"),
    // 功能开关
    "自动跳过广告" to LangMap(en = "Auto skip ads"),
    "开屏广告 · 贴片广告自动点「跳过」" to LangMap(en = "Auto click skip on ads"),
    "弹窗自动关闭" to LangMap(en = "Auto close popups"),
    "识别 关闭/× 关闭（需广告上下文）" to LangMap(en = "Detect close button"),
    "滑屏跳过信息流广告" to LangMap(en = "Swipe skip feed ads"),
    "抖音/快手信息流广告自动上滑" to LangMap(en = "Auto swipe feed ads"),
    "拦截网页广告跳转" to LangMap(en = "Block web ad redirect"),
    "需 Shizuku 增强" to LangMap(en = "Needs Shizuku"),
    // 导航
    "功能" to LangMap(en = "Home"),
    "设置" to LangMap(tw = "設定", en = "Settings"),
    "日志" to LangMap(tw = "日誌", en = "Logs"),
    "关于" to LangMap(tw = "關於", en = "About"),
    // 设置页标签
    "选择语言" to LangMap(tw = "選擇語言", en = "Select language"),
    "选择主题" to LangMap(tw = "選擇主題", en = "Select theme"),
    "恢复默认设置" to LangMap(tw = "恢復預設設定", en = "Reset to defaults"),
    "恢复默认" to LangMap(tw = "恢復預設", en = "Reset"),
    "重置" to LangMap(en = "Reset"),
    "液态玻璃效果" to LangMap(en = "Liquid Glass"),
    "关闭以使用经典界面" to LangMap(en = "Off for classic UI"),
    // 监护人
    "监护人入口" to LangMap(tw = "監護人入口", en = "Guardian"),
    "白名单 + 密码管理" to LangMap(en = "Whitelist + Password"),
    "监护人管理" to LangMap(tw = "監護人管理", en = "Guardian panel"),
    "白名单应用" to LangMap(tw = "白名單應用", en = "Whitelist apps"),
    "白名单保护模式" to LangMap(en = "Whitelist mode"),
    "开启后仅白名单应用不受拦截，其他一律拦截" to LangMap(en = "Only whitelist apps allowed"),
    "修改监护人密码" to LangMap(tw = "修改監護人密碼", en = "Change password"),
    "请输入监护人密码以管理设置" to LangMap(en = "Enter guardian password"),
    "可联系监护人确认密码" to LangMap(en = "Ask guardian for password"),
    "当前密码" to LangMap(en = "Current password"),
    "新密码" to LangMap(en = "New password"),
    "确认新密码" to LangMap(en = "Confirm password"),
    "确认修改" to LangMap(en = "Save"),
    "密码" to LangMap(en = "Password"),
    "密码错误" to LangMap(en = "Wrong password"),
    "确认" to LangMap(en = "Confirm"),
    "取消" to LangMap(en = "Cancel"),
    "返回" to LangMap(tw = "返回", en = "Back"),
    "删除" to LangMap(en = "Delete"),
    "确认删除" to LangMap(en = "Confirm delete"),
    "确认" to LangMap(en = "Confirm"),
    // 日志
    "拦截日志" to LangMap(tw = "攔截日誌", en = "Logs"),
    "导出日志" to LangMap(tw = "導出日誌", en = "Export log"),
    "删除日志" to LangMap(tw = "刪除日誌", en = "Delete log"),
    "将清空全部拦截日志，此操作不可恢复。" to LangMap(en = "Clear all logs. Cannot undo."),
    // 关于
    "CareShield 亲安盾" to LangMap(tw = "CareShield 親安盾", en = "CareShield"),
    "守护家人安全上网" to LangMap(en = "Guard family browsing"),
    "版本 1.1.1" to LangMap(en = "Version 1.1.0"),
    "当前版本：v1.1.0" to LangMap(en = "Current: v1.1.0"),
    "v1.1.0 更新内容" to LangMap(en = "v1.1.0 changelog"),
    "已是最新版本" to LangMap(en = "Up to date"),
    "检查更新" to LangMap(tw = "檢查更新", en = "Check update"),
    "最近更新" to LangMap(tw = "最近更新", en = "Recent updates"),
    "使用方法" to LangMap(en = "How to use"),
    "备份" to LangMap(en = "Backup"),
    "更多" to LangMap(en = "More"),
    "导入" to LangMap(en = "Import"),
    "导出" to LangMap(en = "Export"),
    "作者：DerekHemcis" to LangMap(en = "Author: DerekHemcis"),
    // Shizuku 引导
    "Shizuku 引导" to LangMap(en = "Shizuku guide"),
    "开启开发者模式" to LangMap(en = "Enable developer mode"),
    "开启无线调试" to LangMap(en = "Enable wireless debugging"),
    "Shizuku 已就绪" to LangMap(en = "Shizuku ready"),
    "增强能力已启用 · 防流氓软件生效" to LangMap(en = "Enhancement active"),
    "去开启开发者模式" to LangMap(en = "Enable developer mode"),
    "去开启无线调试" to LangMap(en = "Enable wireless debugging"),
)
