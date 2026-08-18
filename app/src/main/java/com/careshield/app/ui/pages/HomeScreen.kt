package com.careshield.app.ui.pages

import android.content.Context
import android.provider.Settings
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careshield.app.data.SettingsRepo
import com.careshield.app.accessibility.AdBlockerService
import com.careshield.app.shizuku.ShizukuManager

@Composable
fun HomeScreen(
    onOpenAccessibility: () -> Unit,
    onOpenShizuku: () -> Unit,
    shizukuState: Int = 0   // 0=未连接 1=请求中 2=已连接
) {
    val context = LocalContext.current
    val repo = SettingsRepo(context)

    var a11yOn by remember { mutableStateOf(isAccessibilityEnabled(context)) }
    var counts by remember { mutableStateOf(IntArray(3)) } // [广告, 弹窗, 防装]

    LaunchedEffect(Unit) {
        a11yOn = isAccessibilityEnabled(context)
        // 读取真实拦截统计
        counts = repo.getInterceptCounts()
        // 定期刷新无障碍服务状态，保持顶部卡片实时同步
        while (true) {
            val newA11y = isAccessibilityEnabled(context)
            if (newA11y != a11yOn) a11yOn = newA11y
            kotlinx.coroutines.delay(2000)
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val heroBg = if (dark)
        Brush.linearGradient(listOf(Color(0xFF16382C), Color(0xFF0E251D)))
    else Brush.linearGradient(listOf(Color(0xFFD7F0E4), Color(0xFFE9F7F0)))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp)
    ) {
        // 状态大卡
        Box(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(heroBg)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 30.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(primary.copy(alpha = 0.35f)))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (a11yOn) "无障碍服务运行中" else "无障碍服务未开启",
                        style = MaterialTheme.typography.labelMedium,
                        color = primary.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("守护中 · 请放心", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(6.dp))
                Text("已为家人拦截广告与流氓软件打扰", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    StatCell(Modifier.weight(1f), counts[0].toString(), "今日拦截广告")
                    Spacer(Modifier.width(10.dp))
                    StatCell(Modifier.weight(1f), counts[1].toString(), "误触弹窗拦截")
                    Spacer(Modifier.width(10.dp))
                    StatCell(Modifier.weight(1f), counts[2].toString(), "防装软件")
                }
            }
        }

        // 无障碍服务引导卡（未开启时显示，带"去开启"按钮）
        if (!a11yOn) {
            AccessibilityGuideCard(onOpenAccessibility)
        }

        // Shizuku 卡片（0=未连接 1=请求中 2=已连接）
        when (shizukuState) {
            2 -> ShizukuOkCard()
            1 -> ShizukuConnectingCard()
            else -> ShizukuWarnCard(onOpenShizuku)
        }

        // 功能开关
        HomeSectionTitle("功能")
        FeatureSwitch("自动跳过广告", "开屏广告 · 贴片广告自动点「跳过」", "func_skip_ad", repo)
        FeatureSwitch("弹窗自动关闭", "识别「关闭/×」关闭骚扰弹窗", "func_close_pop", repo)
        FeatureSwitch("防误触下载", "拦截跳转应用商店下载页", "func_misclick", repo)
        FeatureSwitch("流氓软件拦截", "Shizuku 增强 · 阻止安装恶意应用", "func_rouge", repo, needsShizuku = true)
    }
}

@Composable
private fun StatCell(modifier: Modifier, n: String, label: String) {
    val s = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(s.surface.copy(alpha = if (s.background.luminance() < 0.3f) 0.6f else 0.9f))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(n, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = s.primary)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = s.onSurfaceVariant)
    }
}

@Composable
private fun AccessibilityGuideCard(onOpen: () -> Unit) {
    val s = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = s.secondaryContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("♿", fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("开启无障碍服务", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = s.onSecondaryContainer)
                Spacer(Modifier.height(2.dp))
                Text("需要在系统设置中开启「亲安盾守护服务」才能自动拦截广告", fontSize = 11.sp, color = s.onSecondaryContainer.copy(alpha = 0.7f))
            }
            Button(onClick = onOpen, colors = ButtonDefaults.buttonColors(containerColor = s.primary)) {
                Text("去开启", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ShizukuWarnCard(onOpenShizuku: () -> Unit) {    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Shizuku 未连接", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("高级拦截能力未开启，防流氓软件功能已降级", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onOpenShizuku, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE28A2A))) {
                Text("去连接", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun ShizukuConnectingCard() {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33219C6D)) // 半透明绿
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⏳", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Shizuku 连接中", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("请在系统弹窗中允许授权", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ShizukuOkCard() {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D5B)) // 正确绿
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✅", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Shizuku 已连接", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Text("高级拦截能力正常 · 防流氓软件已启用", fontSize = 11.sp, color = Color(0xFFC9E8DA))
            }
        }
    }
}

@Composable
private fun HomeSectionTitle(t: String) {
    Text(
        t, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun FeatureSwitch(title: String, desc: String, prefKey: String, repo: SettingsRepo, needsShizuku: Boolean = false) {
    var checked by remember { mutableStateOf(repo.isEnabledSync(prefKey)) }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (needsShizuku) {
                    Text("需 Shizuku 增强", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = { on ->
                    checked = on
                    kotlinx.coroutines.runBlocking { repo.setEnabled(prefKey, on) }
                }
            )
        }
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    return try {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val expected = ComponentName(context, AdBlockerService::class.java)
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        enabled.any { it.resolveInfo.serviceInfo.packageName == expected.packageName &&
                     it.resolveInfo.serviceInfo.name == expected.className }
    } catch (_: Exception) { false }
}
