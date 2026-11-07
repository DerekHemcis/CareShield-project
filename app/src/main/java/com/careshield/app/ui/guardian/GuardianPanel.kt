package com.careshield.app.ui.guardian

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careshield.app.data.SettingsRepo
import com.careshield.app.ui.locale.T
import kotlinx.coroutines.runBlocking

/**
 * 监护人管理面板：仅包含监护人专属能力。
 *  - 白名单保护模式开关
 *  - 白名单应用管理（进入 WhitelistScreen）
 *  - 修改监护人密码
 */
@Composable
fun GuardianPanelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = SettingsRepo(context)
    var whitelistMode by remember { mutableStateOf(repo.getWhitelistModeSync()) }

    // 二级页状态
    var showWhitelist by remember { mutableStateOf(false) }
    var showChangePass by remember { mutableStateOf(false) }
    var showShizukuGuide by remember { mutableStateOf(false) }
    var showAppRules by remember { mutableStateOf(false) }

    if (showWhitelist) { WhitelistScreen(onBack = { showWhitelist = false }); return }
    if (showChangePass) { ChangePassScreen(onBack = { showChangePass = false }); return }
    if (showShizukuGuide) { com.careshield.app.ui.shizuku.ShizukuGuideScreen(onBack = { showShizukuGuide = false }); return }
    if (showAppRules) { AppRulesScreen(onBack = { showAppRules = false }); return }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(T("‹ 返回"), fontSize = 16.sp) }
            Text(T("监护人管理"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            GuardianHint("监护人专属设置，请勿让老人修改。")

            // 白名单保护模式
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(T("白名单保护模式"), fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(2.dp))
                        Text(T("开启后仅白名单应用不受拦截，其他一律拦截"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = whitelistMode, onCheckedChange = { on ->
                        whitelistMode = on
                        runBlocking { repo.setWhitelistMode(on) }
                    })
                }
            }

            // 白名单应用管理
            GuardianNavRow("白名单应用管理", "选择允许老人使用的应用", { showWhitelist = true })
            // 修改监护人密码
            GuardianNavRow("修改监护人密码", "默认 1234，可自定义", { showChangePass = true })
            // Shizuku 设置（打开图形引导向导）
            GuardianNavRow("Shizuku 设置", "引导开启 · 图形化步骤", { showShizukuGuide = true })
            // 每App自定义跳过规则（针对腾讯视频等图片式开屏广告）
            GuardianNavRow("自定义跳过规则", "给个别App标定「跳过」按钮位置", { showAppRules = true })
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GuardianHint(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun GuardianNavRow(title: String, desc: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.clickable { onClick() }.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun toast(context: Context, msg: String) {
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}
