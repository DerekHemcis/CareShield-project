package com.careshield.app.ui.pages

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.careshield.app.data.SettingsRepo
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("🛡", fontSize = 40.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("CareShield 亲安盾", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text("守护家人安全上网", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("版本 1.0.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        // 数据备份
        Text("数据备份", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        AboutRow("导出配置备份") {
            val json = runBlocking { SettingsRepo(context).exportSettings() }
            val send = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, json)
            }
            runCatching { context.startActivity(Intent.createChooser(send, "导出 CareShield 配置")) }
        }
        AboutRow("导入配置备份") {
            toast(context, "把配置文本粘贴后，在此导入（当前版本通过导出分享文本实现恢复）")
            // 简化：尝试从剪贴板读取导入
            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
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
        Spacer(Modifier.height(16.dp))
        // 项目信息行（点击打开 GitHub）
        AboutRow("项目主页") { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(""))) } }
        AboutRow("开源许可 · MIT")
        AboutRow("帮助与反馈") { toast(context, "反馈方式待完善") }
    }
}

@Composable
private fun AboutRow(title: String, onClick: () -> Unit = {}) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("›", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun toast(context: android.content.Context, msg: String) {
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}
