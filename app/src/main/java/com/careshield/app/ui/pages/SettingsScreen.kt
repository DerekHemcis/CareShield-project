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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.careshield.app.data.SettingsRepo
import com.careshield.app.ui.guardian.GuardianVerifyDialog

@Composable
fun SettingsScreen(theme: Int, onThemeChange: (Int) -> Unit) {
    val context = LocalContext.current
    val repo = SettingsRepo(context)
    var showGuardianDialog by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)
    ) {
        // 监护人入口
        GuardianEntryCard { showGuardianDialog = true }

        SectionTitle("通用")
        // 检查更新
        SettingRow("检查更新", "3.2.5 为最新版本") {
            toast(context, "已是最新版本")
        }
        // 语言（简中转简/英循环演示）
        var langLabel by remember { mutableStateOf("简体中文") }
        SettingRow("语言", langLabel) {
            langLabel = when (langLabel) {
                "简体中文" -> "繁體中文"
                "繁體中文" -> "English"
                else -> "简体中文"
            }
        }
        // 主题
        var themeLabel by remember { mutableIntStateOf(theme) }
        val themeMap = listOf("跟随系统", "浅色", "深色")
        SettingRow("主题设置", themeMap.getOrElse(themeLabel) { "跟随系统" }) {
            themeLabel = (themeLabel + 1) % 3
            onThemeChange(themeLabel)
        }
        // 液态玻璃开关
        var liquidOn by remember { mutableStateOf(true) }
        SwitchRow("液态玻璃效果", "关闭以使用经典界面", liquidOn) { liquidOn = it }
    }

    if (showGuardianDialog) {
        GuardianVerifyDialog(
            onDismiss = { showGuardianDialog = false },
            onSuccess = { toast(context, "监护人验证通过") }
        )
    }
}

@Composable
internal fun GuardianEntryCard(onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔐", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("监护人入口", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("管理白名单 · 权限 · 防护设置", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
