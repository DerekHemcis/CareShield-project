package com.careshield.app.ui.guardian

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.careshield.app.data.SettingsRepo
import com.careshield.app.ui.locale.T
import kotlinx.coroutines.runBlocking

/**
 * 白名单应用管理：列出本机已安装应用，勾选进白名单。
 */
@Composable
fun WhitelistScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = SettingsRepo(context)
    val apps = remember { loadInstalledApps(context) }
    val whitelist = remember { mutableStateOf(runBlocking { repo.getWhitelist() }.toMutableSet()) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(T("‹ 返回"), fontSize = 16.sp) }
            Text(T("白名单应用"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Text(
            "勾选允许老人使用的应用（微信、抖音等）。白名单模式开启后，仅白名单内应用不被拦截。",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(Modifier.fillMaxSize().padding(bottom = 16.dp)) {
            items(apps) { app ->
                val pm = context.packageManager
                val label = app.loadLabel(pm).toString()
                val pkg = app.packageName
                val icon = runCatching { app.loadIcon(pm).toBitmap() }.getOrNull()
                val checked = whitelist.value.contains(pkg)
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        // 应用图标
                        if (icon != null) {
                            androidx.compose.foundation.Image(
                                bitmap = icon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Box(Modifier.size(36.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label, fontSize = 14.sp)
                            Text(pkg, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { on ->
                                val set = whitelist.value.toMutableSet()
                                if (on) set.add(pkg) else set.remove(pkg)
                                whitelist.value = set
                                runBlocking { if (on) repo.addWhitelist(pkg) else repo.removeWhitelist(pkg) }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun loadInstalledApps(context: Context): List<ApplicationInfo> {
    val pm = context.packageManager
    return try {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // 排除系统应用
            .sortedBy { it.loadLabel(pm).toString() }
    } catch (_: Exception) { emptyList() }
}
