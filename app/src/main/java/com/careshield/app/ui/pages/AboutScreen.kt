package com.careshield.app.ui.pages

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careshield.app.data.SettingsRepo
import kotlinx.coroutines.runBlocking

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    var showBackupDialog by remember { mutableStateOf(false) }
    var showUpdatePage by remember { mutableStateOf(false) }

    if (showUpdatePage) {
        AboutUpdatePage(onBack = { showUpdatePage = false })
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) { Text("🛡", fontSize = 40.sp) }
        Spacer(Modifier.height(12.dp))
        Text("CareShield 亲安盾", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text("守护家人安全上网", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("版本 1.0.1", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "为长辈打造的安全上网助手。\n\n自动跳过广告、关闭骚扰弹窗、防止误触下载、识别并拦截流氓软件。通过无障碍服务与 Shizuku 增强技术，让家人远离广告与骗局，安心使用手机。",
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
        Spacer(Modifier.height(16.dp))
    }

    // 备份选项对话框：导入 / 导出
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("备份") },
            text = {
                Column {
                    Text("导出配置：生成配置文件分享，换设备可导入沿用设置", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("导入配置：从剪贴板读取恢复", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("取消") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupDialog = false
                    exportBackup(context)
                }) { Text("导出") }
                TextButton(onClick = {
                    showBackupDialog = false
                    importBackup(context)
                }) { Text("导入") }
            }
        )
    }
}

/** 更新页：显示版本号 + 最近一次更新内容 */
@Composable
private fun AboutUpdatePage(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回") }
            Text("检查更新", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
        // 版本
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("CareShield 亲安盾", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("当前版本：v1.0.1", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("已是最新版本", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("最近更新", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        // 更新内容
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("v1.0.1 更新内容", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(UPDATE_TEXT, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}

private const val UPDATE_TEXT =
    "• 广告识别：完整覆盖倒计时跳过（数字在前/在后/纯数字/带秒），从广告出现即拦截\n" +
    "• 日志页：支持导出 / 删除拦截记录\n" +
    "• 配置备份：导出 JSON / 导入恢复，换设备沿用设置\n" +
    "• 监护人面板：新增 Shizuku 设置跳转\n" +
    "• 界面：KernelSU 风格与液态玻璃深化"

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
    val json = runBlocking { SettingsRepo(context).exportSettings() }
    val send = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, json)
    }
    runCatching { context.startActivity(Intent.createChooser(send, "导出 CareShield 配置")) }
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
