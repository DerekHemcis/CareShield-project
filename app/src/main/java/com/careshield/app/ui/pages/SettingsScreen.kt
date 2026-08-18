package com.careshield.app.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careshield.app.data.SettingsRepo
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
    var langLabel by remember { mutableStateOf("简体中文") }

    if (showGuardianPanel) {
        GuardianPanelScreen(onBack = { showGuardianPanel = false })
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)
    ) {
        // 老人防护开关
        SectionTitle("防护功能")
        GuardianFeatureSwitch("自动跳过广告", "识别开屏广告并自动点击跳过", "func_skip_ad", repo)
        GuardianFeatureSwitch("自动关闭广告弹窗", "识别 关闭/× 关闭（需广告上下文）", "func_close_pop", repo)
        GuardianFeatureSwitch("滑屏跳过信息流广告", "抖音/快手信息流广告自动上滑", "func_misclick", repo)
        GuardianFeatureSwitch("拦截网页广告跳转", "防止误触打开广告网页", "func_block_web", repo)

        SectionTitle("增强能力")
        // 连接 Shizuku
        SettingRow("连接 Shizuku 增强", if (ShizukuManager.isEnhanceReady()) "已连接" else "未连接") {
            if (!ShizukuManager.isEnhanceReady()) {
                ShizukuManager.requestPermission()
                toast(context, "已发起 Shizuku 授权请求")
            } else {
                toast(context, "Shizuku 已连接")
            }
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
        var liquidOn by remember { mutableStateOf(repo.isEnabledSync("liquid_glass")) }
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Row(Modifier.fillMaxWidth().clickable { showResetConfirm = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("恢复默认设置", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.weight(1f))
                Text("重置", fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("恢复默认") },
                text = { Text("将恢复出厂默认设置：\n• 全部拦截功能关闭\n• 液态玻璃关闭\n• 主题跟随系统\n• 语言简体中文\n（无障碍服务状态不受影响）") },
                confirmButton = {
                    TextButton(onClick = {
                        showResetConfirm = false
                        runBlocking { repo.resetDefaults() }
                        toast(context, "已恢复默认设置")
                    }) { Text("确认") }
                },
                dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("取消") } }
            )
        }
    }

    // 语言选择器
    if (showLangDialog) {
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = { Text("选择语言") },
            text = {
                Column {
                    LangOption("简体中文", langLabel == "简体中文") { langLabel = "简体中文"; showLangDialog = false }
                    LangOption("繁體中文", langLabel == "繁體中文") { langLabel = "繁體中文"; showLangDialog = false }
                    LangOption("English", langLabel == "English") { langLabel = "English"; showLangDialog = false }
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
            title = { Text("选择主题") },
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
private fun GuardianFeatureSwitch(title: String, desc: String, key: String, repo: SettingsRepo) {
    var checked by remember { mutableStateOf(repo.isEnabledSync(key)) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
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
                Text("监护人入口", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("白名单 + 密码管理", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
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
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (desc.isNotEmpty()) Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state, onCheckedChange = { state = it; onChange(it) })
        }
    }
}

private fun toast(context: android.content.Context, msg: String) {
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}
