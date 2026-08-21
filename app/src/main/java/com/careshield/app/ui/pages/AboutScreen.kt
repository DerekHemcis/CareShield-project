package com.careshield.app.ui.pages

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careshield.app.data.SettingsRepo
import com.careshield.app.ui.locale.T
import kotlinx.coroutines.runBlocking

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    var showBackupDialog by remember { mutableStateOf(false) }
    var showUpdatePage by remember { mutableStateOf(false) }
    var showUsagePage by remember { mutableStateOf(false) }

    if (showUpdatePage) {
        AboutUpdatePage(onBack = { showUpdatePage = false })
        return
    }
    if (showUsagePage) {
        UsageGuidePage(onBack = { showUsagePage = false })
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) { Text("🛡", fontSize = 40.sp) }
        Spacer(Modifier.height(12.dp))
        Text(T("CareShield 亲安盾"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(T("守护家人安全上网"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        // 使用方法入口 ⓘ
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(Modifier.fillMaxWidth().clickable { showUsagePage = true }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Text("?", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(T("使用方法"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(T("开启守护 · Shizuku · 无障碍说明"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    T("为长辈打造的安全上网助手。自动跳过广告，关闭骚扰弹窗，防止误触下载，识别并拦截流氓软件。通过无障碍服务与 Shizuku 增强技术，让家人远离广告与骗局，安心使用手机。"),
                    fontSize = 13.sp, lineHeight = 22.sp, textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        AboutSectionTitle("更多")
        // 检查更新（进入更新页，显示版本 + 更新内容）
        AboutRow("检查更新") { showUpdatePage = true }
        // 备份（单按钮 → 弹导入/导出选项）
        AboutRow("备份") { showBackupDialog = true }
        Spacer(Modifier.height(24.dp))
        // 底部：版本 + 版权信息
        Text(T("版本 1.6.0"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(T("© 2026 DerekHemcis · CareShield 亲安盾"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(T("作者：DerekHemcis"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }

    // 备份选项对话框：导入 / 导出
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(T("备份")) },
            text = {
                Column {
                    Text(T("导出配置：生成配置文件分享，换设备可导入沿用设置"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(T("导入配置：从剪贴板读取恢复"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text(T("取消")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupDialog = false
                    exportBackup(context)
                }) { Text(T("导出")) }
                TextButton(onClick = {
                    showBackupDialog = false
                    importBackup(context)
                }) { Text(T("导入")) }
            }
        )
    }
}

/** 更新页：显示版本号 + 最近一次更新内容 */
@Composable
private fun AboutUpdatePage(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(T("‹ 返回")) }
            Text(T("检查更新"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
        // 版本
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(T("CareShield 亲安盾"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(T("当前版本：v1.6.0"), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(T("已是最新版本"), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(T("最近更新"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        // 更新内容
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(T("v1.6.0 更新内容"), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(UPDATE_TEXT, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}

private const val UPDATE_TEXT =
    "• 广告拦截：实测拦截 有道词典 / 中羽在线 / 咪咕视频 的开屏广告\n" +
    "• 智能防流氓：Virustotal 云端识别 + 本地危险词库（色情/赌博/贷款/私彩）\n" +
    "• 一键扫描手机：主动扫描已装 App，识别高风险软件，一键 Shizuku 卸载\n" +
    "• 无障碍 config 回归 43 稳定版；导航固定 Material3（无液体玻璃开关）\n" +
    "• 多语言(简/繁/英)、主题切换、监护人面板、日志导出"

@Composable
private fun AboutSectionTitle(t: String) {
    Text(t, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}

@Composable
private fun AboutRow(title: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("›", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun exportBackup(context: Context) {
    try {
        val json = runBlocking { SettingsRepo(context).exportSettings() }
        // 写入私有文件目录，生成 careshield_config.json
        val file = java.io.File(context.filesDir, "careshield_config.json")
        file.writeText(json)
        android.util.Log.i("CareShield", "备份写入 ${file.absolutePath}")
        toast(context, "配置已导出到 careshield_config.json")
        // 分享文件（FileProvider）
        reportShare(context, file)
    } catch (e: Exception) {
        toast(context, "导出失败：${e.message}")
    }
}

private fun reportShare(context: Context, file: java.io.File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(send, "导出配置")) }
}

private fun importBackup(context: Context) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = cm.primaryClip
    if (clip != null && clip.itemCount > 0) {
        val text = clip.getItemAt(0).coerceToText(context).toString()
        if (text.contains("func_") || text.contains("whitelist")) {
            runBlocking { SettingsRepo(context).importSettings(text) }
            toast(context, "已从剪贴板导入配置")
        } else {
            toast(context, "剪贴板内容不是配置备份")
        }
    } else {
        toast(context, "剪贴板为空，请先复制配置备份文本")
    }
}

private fun toast(context: Context, msg: String) {
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}

/** 使用方法页：方案 A（root）/ 方案 B（老人机无 root）+ 无障碍开启说明 */
@Composable
private fun UsageGuidePage(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(T("‹ 返回")) }
            Text(T("使用方法"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))

        // 无障碍（必须）
        UsageSection("① 开启无障碍（必须）", onClick = {
            runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }) {
            Text(T("设置 → 无障碍 → 已安装的服务 → 开启「亲安盾守护服务」"), fontSize = 13.sp)
            Text(T("开启后广告自动拦截才生效"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // 方案 B（老人机无 root）
        UsageSection("② 开启 Shizuku 增强") {
            Text(T("方案B（无 Root 老人机）："), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(T("1. 进入设置 → 关于手机 → 连点版本号 7 次，开启开发者模式"), fontSize = 12.sp)
            Text(T("2. 开发者选项 → 开启「无线调试」"), fontSize = 12.sp)
            Text(T("3. 用「使用配对码配对」+ 电脑 adb 配对一次"), fontSize = 12.sp)
            Text(T("4. 之后到 更多 → 使用方法 → Shizuku 引导 里可图形化协助"), fontSize = 12.sp)
        }

        // 方案 A（root 手机）
        UsageSection("③ 方案A（Root 手机）：Shizuku root 自启") {
            Text(T("已 Root（如 KernelSU/Magisk）：在 Shizuku 中选择「通过Root启动」，开机自动运行，无需无线调试"), fontSize = 12.sp)
        }

        // 备份
        UsageSection("④ 换设备沿用设置") {
            Text(T("关于 → 备份 → 导出 JSON；新设备导入即可"), fontSize = 12.sp)
        }
    }
}

@Composable
private fun UsageSection(title: String, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(
            Modifier.padding(14.dp).then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        ) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
        }
    }
}
