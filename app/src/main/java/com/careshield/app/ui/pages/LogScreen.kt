package com.careshield.app.ui.pages

import android.content.Context
import android.content.Intent
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

/** 拦截日志页（展示拦截记录 + 导出/删除） */
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
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("拦截日志", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("共 ${entries.size} 条", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // 操作按钮：导出 / 删除
        var showClearConfirm by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                // 导出为 .log 文件并用 FileProvider 分享
                try {
                    val logText = entries.joinToString("\n") { "${it.time} ${it.source}: 拦截 ${it.action}" }
                    val file = java.io.File(context.filesDir, "careshield.log")
                    file.writeText(logText.ifBlank { "暂无拦截记录" })
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file)
                    val send = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { context.startActivity(Intent.createChooser(send, "导出拦截日志")) }
                } catch (e: Exception) {
                    toast(context, "导出失败")
                }
            }, modifier = Modifier.weight(1f)) {
                Text("导出日志", fontSize = 13.sp)
            }
            OutlinedButton(onClick = { showClearConfirm = true }, modifier = Modifier.weight(1f)) {
                Text("删除日志", fontSize = 13.sp)
            }
        }
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("确认删除") },
                text = { Text("将清空全部拦截日志，此操作不可恢复。") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearConfirm = false
                        runBlocking { repo.clearLog() }
                        entries = emptyList()
                        toast(context, "日志已清空")
                    }) { Text("删除") }
                },
                dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("取消") } }
            )
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
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
    } catch (_: Exception) { "--" }
}

private fun toast(context: Context, msg: String) {
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}
