package com.careshield.app.ui.pages

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import com.careshield.app.data.SettingsRepo
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 统计页：广告拦截统计 + 应用列表 + 时段筛选 + 导出/清除日志（适配主题） */
@Composable
fun LogScreen() {
    val context = LocalContext.current
    val repo = SettingsRepo(context)
    val s = MaterialTheme.colorScheme
    val dark = s.background.luminance() < 0.3f

    // 时段筛选：0=全部 1=24h 2=7天 3=30天
    var period by remember { mutableStateOf(0) }
    var showPeriod by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // 真实拦截统计（广告/流氓防装/总请求）
    var adCount by remember { mutableIntStateOf(0) }
    var racketCount by remember { mutableIntStateOf(0) }
    var requestCount by remember { mutableIntStateOf(0) }
    // 按 App 分组的拦截
    var appStats by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    LaunchedEffect(period) {
        val counts = runBlocking { repo.getInterceptCounts() }
        adCount = counts[0]
        racketCount = counts[2]
        requestCount = counts[0] + counts[1] + counts[2]
        appStats = runBlocking { repo.getAppInterceptStats() }
    }

    val total = requestCount
    val adPct = if (total > 0) adCount.toFloat() / total else 0f
    val racketPct = if (total > 0) racketCount.toFloat() / total else 0f

    Column(Modifier.fillMaxSize().background(s.background)) {
        // 顶部标题 + 按钮
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text("统计", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = s.onBackground,
                modifier = Modifier.align(Alignment.CenterStart))
            Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("📅", fontSize = 18.sp, modifier = Modifier.clickable { showPeriod = true })
                Text("⋮", fontSize = 18.sp, modifier = Modifier.clickable { showMenu = true })
            }
        }

        // 请求模块
        Text("请求", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = s.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp))
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatNum("$adCount", "广告", 0xFFFF7A2F)
            StatNum("$racketCount", "捆绑软件", 0xFFFFC120)
            StatNum("$requestCount", "请求", if (dark) 0xFFE0E0E0L else 0xFF333333L)
        }
        Spacer(Modifier.height(8.dp))
        // 分段进度条
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
            horizontalArrangement = Arrangement.Start) {
            Box(Modifier.width((adPct * 360).dp).fillMaxHeight().background(androidx.compose.ui.graphics.Color(0xFFFF7A2F)))
            Box(Modifier.width((racketPct * 360).dp).fillMaxHeight().background(androidx.compose.ui.graphics.Color(0xFFFFC120)))
            Box(Modifier.weight(1f).fillMaxHeight().background(if (dark) androidx.compose.ui.graphics.Color(0xFF333333) else androidx.compose.ui.graphics.Color(0xFFE0E0E0)))
        }
        Spacer(Modifier.height(24.dp))

        // 应用程序列表
        Text("应用程序", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = s.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(appStats.take(10)) { (pkg, count) ->
                AppRow(context, pkg, count)
            }
            if (appStats.size > 10) {
                item {
                    Text("查看所有应用（共 ${appStats.size} 个）…", fontSize = 13.sp,
                        color = s.onSurfaceVariant, modifier = Modifier.padding(20.dp))
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    // 时段弹窗
    if (showPeriod) {
        AlertDialog(
            onDismissRequest = { showPeriod = false },
            title = { Text("时段") },
            text = {
                Column {
                    PeriodOption("全时段", period == 0) { period = 0; showPeriod = false }
                    PeriodOption("24 小时", period == 1) { period = 1; showPeriod = false }
                    PeriodOption("7 天", period == 2) { period = 2; showPeriod = false }
                    PeriodOption("30 天", period == 3) { period = 3; showPeriod = false }
                }
            },
            confirmButton = {}
        )
    }

    // 三点菜单：清除统计 / 导出日志
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("操作") },
            text = {
                Column {
                    TextButton(onClick = {
                        showMenu = false
                        runBlocking { repo.clearLog() }
                        adCount = 0; racketCount = 0; requestCount = 0; appStats = emptyList()
                    }) { Text("清除统计数据", color = androidx.compose.ui.graphics.Color(0xFFFF7A2F)) }
                    TextButton(onClick = {
                        showMenu = false
                        exportLogAsFile(context)
                    }) { Text("导出日志") }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun StatNum(num: String, label: String, color: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(color))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PeriodOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).clip(CircleShape)
            .background(if (selected) androidx.compose.ui.graphics.Color(0xFF4CD964) else MaterialTheme.colorScheme.surfaceVariant)
            .then(if (selected) Modifier else Modifier))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 15.sp)
    }
}

@Composable
private fun AppRow(context: Context, pkg: String, count: Int) {
    val icon = runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap() }.getOrNull()
    val label = runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            androidx.compose.foundation.Image(
                bitmap = icon.asImageBitmap(), contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        } else {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("$count", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun exportLogAsFile(context: Context) {
    try {
        val repo = SettingsRepo(context)
        val logText = runBlocking { repo.getLog() }.ifBlank { "暂无拦截日志" }
        val file = java.io.File(context.filesDir, "careshield.log")
        file.writeText(logText)
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(send, "导出日志")) }
    } catch (_: Exception) { }
}
