package com.careshield.app.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careshield.app.data.SettingsRepo
import com.careshield.app.ui.locale.T
import com.careshield.app.shizuku.ShizukuManager
import com.careshield.app.ui.guardian.GuardianVerifyDialog
import com.careshield.app.ui.guardian.GuardianPanelScreen
import kotlinx.coroutines.runBlocking

@Composable
fun SettingsScreen(theme: Int, onThemeChange: (Int) -> Unit) {
    val context = LocalContext.current
    val repo = SettingsRepo(context)
    var showGuardianDialog by remember { mutableStateOf(false) }
    var showGuardianPanel by remember { mutableStateOf(false) }

    // 语言 / 主题选择器状态
    var showLangDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    // 恢复默认后强制刷新界面（key 重建触发各开关重新读取默认值）
    var resetCounter by remember { mutableStateOf(0) }
    // 语言：从 DataStore 读取实际值，恢复默认(resetCounter变化)时重新读取
    var langLabel by remember(resetCounter) {
        mutableStateOf(runBlocking { repo.get("lang", "简体中文") })
    }

    if (showGuardianPanel) {
        GuardianPanelScreen(onBack = { showGuardianPanel = false })
        return
    }

    androidx.compose.runtime.key(resetCounter) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)
    ) {
        // 老人防护开关
        SectionTitle("防护功能")
                GuardianFeatureSwitch(T("激进跳过(实验)"), "全屏广告+角落倒计时自动点·可能误触", "func_aggressive_skip", repo, resetCounter)
                GuardianFeatureSwitch(T("滑屏跳过信息流广告"), T("抖音/快手信息流广告自动上滑"), "func_misclick", repo, resetCounter)
        GuardianFeatureSwitch(T("拦截网页广告跳转"), T("防止误触打开广告网页"), "func_block_web", repo, resetCounter)

        SectionTitle("增强能力")
        // 连接 Shizuku
        SettingRow(T("连接 Shizuku 增强"), if (ShizukuManager.isEnhanceReady()) T("已连接") else T("未连接")) {
            if (!ShizukuManager.isEnhanceReady()) {
                ShizukuManager.requestPermission()
                toast(context, "已发起 Shizuku 授权请求")
            } else {
                toast(context, "Shizuku 已连接")
            }
        }
        // DNS 广告过滤（VpnService 拦广告域名源头）
        var vpnOn by remember { mutableStateOf(isVpnRunning()) }
        val vpnLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) {
            // 授权完成，启动服务
            try {
                context.startForegroundService(Intent(context, com.careshield.app.security.DnsVpnService::class.java))
                vpnOn = true
            } catch (_: Exception) {
                context.startService(Intent(context, com.careshield.app.security.DnsVpnService::class.java))
                vpnOn = true
            }
        }
        SettingRow(if (vpnOn) "DNS 广告过滤(开)" else "DNS 广告过滤(关)", if (vpnOn) "源头拦广告域名" else "开启后拦截广告域名") {
            if (vpnOn) {
                context.stopService(Intent(context, com.careshield.app.security.DnsVpnService::class.java))
                vpnOn = false
            } else {
                try {
                    val intent = android.net.VpnService.prepare(context)
                    if (intent != null) {
                        vpnLauncher.launch(intent)
                    } else {
                        context.startService(Intent(context, com.careshield.app.security.DnsVpnService::class.java))
                        vpnOn = true
                    }
                } catch (e: Exception) {
                    toast(context, "VPN 启动失败：${e.message}")
                }
            }
        }
        // 安全 API key（Virustotal，可留空）
        var apiKeySet by remember { mutableStateOf(repo.getApiKeySync().isNotBlank()) }
        SettingRow(
            if (apiKeySet) "安全识别(已配置)" else "安全 API Key(可选)",
            if (apiKeySet) "Virustotal key 已设置" else "本地规则兜底"
        ) { showApiKeyDialog = true }
        if (showApiKeyDialog) {
            var keyInput by remember { mutableStateOf(repo.getApiKeySync()) }
            AlertDialog(
                onDismissRequest = { showApiKeyDialog = false },
                title = { Text("Virustotal API Key") },
                text = {
                    Column {
                        Text("填写后启用云端智能识别；留空则用本地关键词/名单兜底。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it },
                            placeholder = { Text("粘贴 API key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showApiKeyDialog = false
                        runBlocking { repo.setApiKey(keyInput.trim()) }
                        apiKeySet = keyInput.trim().isNotBlank()
                        toast(context, if (apiKeySet) "已启用云端识别" else "已使用本地兜底")
                    }) { Text("保存") }
                },
                dismissButton = { TextButton(onClick = { showApiKeyDialog = false }) { Text("取消") } }
            )
        }

        // 监护人入口（置于通用上方）
        SectionTitle("监护")
        GuardianEntryCard { showGuardianDialog = true }

        SectionTitle("通用")
        // 语言：点击弹选择器
        SettingRow("语言", langLabel) { showLangDialog = true }
        // 主题：点击弹选择器
        val themeMap = listOf("跟随系统", "浅色", "深色")
        SettingRow("主题设置", themeMap.getOrElse(theme) { "跟随系统" }) { showThemeDialog = true }
        // 液态玻璃开关
        var liquidOn by remember(resetCounter) { mutableStateOf(repo.isEnabledSync("liquid_glass")) }
        SwitchRow("液态玻璃效果", "关闭以使用经典界面", liquidOn) { on ->
            liquidOn = on
            runBlocking { repo.setEnabled("liquid_glass", on) }
        }

        // 恢复默认
        var showResetConfirm by remember { mutableStateOf(false) }
        Spacer(Modifier.height(8.dp))
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(Modifier.fillMaxWidth().clickable { showResetConfirm = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("↺", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(T("恢复默认设置"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(T("重置"), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text(T("恢复默认")) },
                text = { Text("将恢复出厂默认设置：\n• 全部拦截功能关闭\n• 液态玻璃关闭\n• 主题跟随系统\n• 语言简体中文\n（无障碍服务状态不受影响）") },
                confirmButton = {
                    TextButton(onClick = {
                        showResetConfirm = false
                        runBlocking { repo.resetDefaults() }
                        resetCounter++          // 强制界面重建，立即反映默认值
                        onThemeChange(0)         // 主题恢复跟随系统
                        toast(context, "已恢复默认设置")
                    }) { Text(T("确认")) }
                },
                dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text(T("取消")) } }
            )
        }
    }
    } // key(resetCounter) 结束

    // 语言选择器
    if (showLangDialog) {
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = { Text(T("选择语言")) },
            text = {
                Column {
                    LangOption("简体中文", langLabel == "简体中文") { langLabel = "简体中文"; runBlocking { com.careshield.app.ui.locale.AppLocaleManager.setLocale(context, "简体中文") }; showLangDialog = false; (context as? android.app.Activity)?.recreate() }
                    LangOption("繁體中文", langLabel == "繁體中文") { langLabel = "繁體中文"; runBlocking { com.careshield.app.ui.locale.AppLocaleManager.setLocale(context, "繁體中文") }; showLangDialog = false; (context as? android.app.Activity)?.recreate() }
                    LangOption("English", langLabel == "English") { langLabel = "English"; runBlocking { com.careshield.app.ui.locale.AppLocaleManager.setLocale(context, "English") }; showLangDialog = false; (context as? android.app.Activity)?.recreate() }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // 主题选择器
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(T("选择主题")) },
            text = {
                Column {
                    ThemeOption("跟随系统", 0, theme, onThemeChange) { showThemeDialog = false }
                    ThemeOption("浅色", 1, theme, onThemeChange) { showThemeDialog = false }
                    ThemeOption("深色", 2, theme, onThemeChange) { showThemeDialog = false }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    if (showGuardianDialog) {
        GuardianVerifyDialog(
            onDismiss = { showGuardianDialog = false },
            onSuccess = {
                showGuardianDialog = false
                showGuardianPanel = true
            }
        )
    }
}

@Composable
private fun LangOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
        if (selected) Text("✓", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ThemeOption(label: String, value: Int, current: Int, onChange: (Int) -> Unit, close: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(value); close() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, fontWeight = if (current == value) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
        if (current == value) Text("✓", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun GuardianFeatureSwitch(title: String, desc: String, key: String, repo: SettingsRepo, resetKey: Int = 0) {
    var checked by remember(resetKey) { mutableStateOf(repo.isEnabledSync(key)) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = { on ->
                checked = on
                runBlocking { repo.setEnabled(key, on) }
            })
        }
    }
}

@Composable
internal fun GuardianEntryCard(onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔐", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(T("监护人入口"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(T("白名单 + 密码管理"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", fontSize = 18.sp)
        }
    }
}

@Composable
internal fun SectionTitle(t: String) {
    Text(t, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 6.dp))
}

@Composable
internal fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("›", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    var state by remember { mutableStateOf(checked) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (desc.isNotEmpty()) Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state, onCheckedChange = { state = it; onChange(it) })
        }
    }
}

private fun toast(context: android.content.Context, msg: String) {
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}

private fun isVpnRunning(): Boolean {
    return try {
        // 检查是否有 VPN 接口
        val dir = java.io.File("/sys/class/net")
        if (!dir.exists()) return false
        dir.listFiles()?.any { it.name.startsWith("tun") || it.name.startsWith("ppp") } ?: false
    } catch (_: Exception) { false }
}
