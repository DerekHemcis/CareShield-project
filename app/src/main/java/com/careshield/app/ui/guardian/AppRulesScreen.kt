package com.careshield.app.ui.guardian

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careshield.app.accessibility.AppRules
import com.careshield.app.data.SettingsRepo
import com.careshield.app.ui.locale.T
import kotlinx.coroutines.runBlocking

/**
 * 每App自定义跳过规则（监护人标定）。
 *
 * 用法：老人机装好目标 App（如腾讯视频）→ 打到开屏广告界面 → 记下"跳过"按钮
 * 大约在屏幕的什么位置（右上角约 95%/6%）→ 在此页添加：包名 + 可选关键词 + 可选比例坐标。
 * 之后 AdBlockerService 会在该 App 的开屏界面自动执行。
 *
 * 包名不知道？页内提供「已装应用选择器」，勾选即可。
 */
@Composable
fun AppRulesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = SettingsRepo(context)

    var rules by remember { mutableStateOf(AppRules.deserialize(repo.getAppRulesSync())) }
    var showPicker by remember { mutableStateOf(false) }

    // 表单状态
    var pkg by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var x by remember { mutableStateOf("") }
    var y by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }

    fun save(list: List<AppRules.Rule>) {
        rules = list
        runBlocking { repo.setAppRules(AppRules.serialize(list)) }
    }

    if (showPicker) {
        AppPickerScreen(onBack = { showPicker = false }, onPick = { chosen ->
            pkg = chosen
            showPicker = false
        })
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(T("‹ 返回"), fontSize = 16.sp) }
            Text(T("自定义跳过规则"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Text(
                T("针对某个 App 手动标定「跳过」按钮位置。开屏广告停留在哪，就量一下按钮在屏幕上的大概位置（比例 0~1）。"),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            // ---- 添加表单 ----
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = pkg, onValueChange = { pkg = it },
                        label = { Text(T("应用包名")) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(T("从已装应用选择"))
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        label = { Text(T("按钮文字（可选，如：跳过）")) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = x, onValueChange = { x = it },
                            label = { Text(T("X 比例 0~1")) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = y, onValueChange = { y = it },
                            label = { Text(T("Y 比例 0~1")) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val xf = x.toFloatOrNull()?.takeIf { it in 0f..1f }
                            val yf = y.toFloatOrNull()?.takeIf { it in 0f..1f }
                            val r = AppRules.Rule(pkg.trim(), text.trim().takeIf { it.isNotBlank() }, xf, yf)
                            if (!r.valid) {
                                msg = T("请填包名，且至少填按钮文字或完整坐标")
                            } else {
                                save(AppRules.upsert(rules, r))
                                pkg = ""; text = ""; x = ""; y = ""; msg = T("已保存（同包名会覆盖）")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(T("保存规则")) }
                    if (msg.isNotBlank()) {
                        Text(msg, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---- 已有规则列表 ----
            Text(T("已标定 ${rules.size} 个应用"), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            rules.forEach { r ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.pkg, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            val desc = buildString {
                                r.text?.let { append("文字「$it」 ") }
                                if (r.x != null && r.y != null) append("坐标 ${(r.x!! * 100).toInt()}%,${(r.y!! * 100).toInt()}%")
                            }
                            Text(desc.ifBlank { "无匹配条件" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { save(AppRules.remove(rules, r.pkg)) }) { Text(T("删除"), fontSize = 12.sp) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AppPickerScreen(onBack: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val apps = remember {
        try {
            pm.getInstalledApplications(0)
                .filter { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }
                .map { it.packageName to (pm.getApplicationLabel(it)?.toString() ?: it.packageName) }
                .sortedBy { it.second }
        } catch (_: Exception) { emptyList() }
    }
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(T("‹ 返回"), fontSize = 16.sp) }
            Text(T("选择应用"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text(T("搜索")) }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            apps.filter { it.second.contains(query, true) || it.first.contains(query, true) }
                .forEach { (p, label) ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(p) }.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                Text(p, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
        Spacer(Modifier.height(20.dp))
        }
    }
}
