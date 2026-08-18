package com.careshield.app.ui.pages

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.careshield.app.data.SettingsRepo
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 拦截记录条目 */
data class LogEntry(val time: String, val source: String, val action: String)

@Composable
fun LogScreen() {
    val context = LocalContext.current
    val repo = SettingsRepo(context)
    var entries by remember { mutableStateOf<List<LogEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        val raw = runBlocking { repo.getLog() }
        entries = parseLog(raw)
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("拦截日志", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("共 ${entries.size} 条", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("暂无拦截记录\n\n拦截广告/弹窗后，这里会显示时间与内容", fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(entries) { e ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text("🛡️", fontSize = 18.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Row {
                                    Text(e.source, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(e.time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("拦截：${e.action}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 解析日志格式 timestamp|source|action */
private fun parseLog(raw: String): List<LogEntry> {
    val out = ArrayList<LogEntry>()
    for (line in raw.lines()) {
        if (line.isBlank()) continue
        val parts = line.split("|")
        if (parts.size < 3) continue
        val ts = parts[0].toLongOrNull()
        val time = ts?.let { formatTime(it) } ?: "--"
        out.add(LogEntry(time, parts[1], parts[2]))
    }
    return out.reversed() // 新的在前
}

private fun formatTime(ms: Long): String {
    return try {
        val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        fmt.format(Date(ms))
    } catch (_: Exception) { "--" }
}
