package com.careshield.app.ui.shizuku

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.careshield.app.shizuku.ShizukuManager

/**
 * Shizuku 图形化引导向导（方案 B：老人机无 root）
 * 步骤1：开启开发者模式 → 步骤2：开启无线调试 → 完成后通过无线调试配对
 */
@Composable
fun ShizukuGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // 实时检测状态
    var shizukuOk by remember { mutableStateOf(ShizukuManager.isAvailable()) }
    var devModeOn by remember { mutableStateOf(isDevModeOn(context)) }

    // 定期刷新
    LaunchedEffect(Unit) {
        while (true) {
            shizukuOk = ShizukuManager.isAvailable()
            devModeOn = isDevModeOn(context)
            kotlinx.coroutines.delay(1500)
        }
    }

    // 当前步骤
    val step = when {
        shizukuOk -> 3   // 已完成
        !devModeOn -> 1  // 需开开发者模式
        else -> 2        // 需开无线调试
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回") }
            Text("Shizuku 引导", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(10.dp))

        // 步骤指示器（动画：当前步骤高亮脉动）
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            StepDot(1, step >= 1, step == 1, "开发者模式", active = step == 1)
            StepDot(2, step >= 2, step == 2, "无线调试", active = step == 2)
            StepDot(3, step >= 3, step == 3, "完成", active = step == 3)
        }
        Spacer(Modifier.height(20.dp))

        when (step) {
            3 -> {
                Text("✅", fontSize = 48.sp)
                Spacer(Modifier.height(10.dp))
                Text("Shizuku 已就绪", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("增强能力已启用 · 防流氓软件生效", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            1 -> {
                Text("📱", fontSize = 48.sp)
                Spacer(Modifier.height(10.dp))
                Text("开启开发者模式", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                GuideCard {
                    Text("1. 点击下方【去开启】进入开发者选项设置", fontSize = 13.sp)
                    Text("2. 打开「开发者选项」开关", fontSize = 13.sp)
                    Text("3. 回到本页，会自动进入下一步", fontSize = 13.sp)
                }
                Spacer(Modifier.height(14.dp))
                Button(onClick = { openDevSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text("去开启开发者模式")
                }
            }
            else -> {
                Text("📡", fontSize = 48.sp)
                Spacer(Modifier.height(10.dp))
                Text("开启无线调试", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    GuideCard {
                        Text("1. 点击下方【去开启】进入无线调试", fontSize = 13.sp)
                        Text("2. 打开「无线调试」开关", fontSize = 13.sp)
                        Text("3. 点击「使用配对码配对」，配对 Shizuku", fontSize = 13.sp)
                        Text("4. 让电脑/监护人用 adb 配对即可", fontSize = 13.sp)
                    }
                } else {
                    GuideCard {
                        Text("Android 10 及以下：开启「USB 调试」后用电脑连接配对", fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(onClick = { openWirelessDebug(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text("去开启无线调试")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "提示：完成一次配对后，手机不重启即可持续使用。重启后需重新配对。",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StepDot(num: Int, done: Boolean, current: Boolean, label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(40.dp)
                .background(
                    if (done) MaterialTheme.colorScheme.primary
                    else if (current) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (done) "✓" else "$num",
                color = if (done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun GuideCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
        }
    }
}

private fun isDevModeOn(context: Context): Boolean {
    val v = Settings.Global.getInt(
        context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
    return v != 0
}

private fun openDevSettings(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
}

private fun openWirelessDebug(context: Context) {
    // Android 无独立"无线调试"跳转常量，回退到开发者选项（无线调试开关在其中）
    runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
}
