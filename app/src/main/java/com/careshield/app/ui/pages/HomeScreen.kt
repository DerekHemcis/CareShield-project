package com.careshield.app.ui.pages

import android.content.Context
import android.provider.Settings
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careshield.app.data.SettingsRepo
import com.careshield.app.R
import com.careshield.app.ui.locale.T
import com.careshield.app.accessibility.AdBlockerService
import com.careshield.app.security.RogueScanner
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
                        if (a11yOn) T("无障碍服务运行中") else T("无障碍服务未开启"),
                        style = MaterialTheme.typography.labelMedium,
                        color = primary.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(T("守护中 · 请放心"), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(6.dp))
                Text(T("已为家人拦截广告与流氓软件打扰"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    StatCell(Modifier.weight(1f), counts[0].toString(), T("今日拦截广告"))
                    Spacer(Modifier.width(10.dp))
                    StatCell(Modifier.weight(1f), counts[1].toString(), T("误触弹窗拦截"))
                    Spacer(Modifier.width(10.dp))
                    StatCell(Modifier.weight(1f), counts[2].toString(), T("防装软件"))
                }
            }
        }

        // 无障碍服务引导卡（未开启时显示，带T("去开启")按钮）
        if (!a11yOn) {
            AccessibilityGuideCard(onOpenAccessibility)
        }

        // Shizuku 卡片（0=未连接 1=请求中 2=已连接）
        when (shizukuState) {
            2 -> ShizukuOkCard()
            1 -> ShizukuConnectingCard()
            else -> ShizukuWarnCard(onOpenShizuku)
        }

        // 防护功能开关（功能页，放 Column 内）
        HomeSectionTitle("防护功能")
        FeatureSwitch(T("自动跳过广告"), T("识别开屏广告并自动点击跳过"), "func_skip_ad", repo)
        FeatureSwitch(T("自动关闭广告弹窗"), T("识别 关闭/× 关闭（需广告上下文）"), "func_close_pop", repo)
        FeatureSwitch(T("滑屏跳过信息流广告"), T("抖音/快手信息流广告自动上滑"), "func_misclick", repo)
        FeatureSwitch(T("拦截网页广告跳转"), T("防止误触打开广告网页"), "func_block_web", repo)
        FeatureSwitch(T("流氓软件拦截"), T("Shizuku 增强 · 阻止安装恶意应用"), "func_rouge", repo, needsShizuku = true)
        RogueScanCard()
    }
}

/**
 * 一键扫描手机：遍历已安装 App，识别高风险(赌博/贷款/色情等)并可选 Shizuku 一键卸载。
 * 不依赖 PACKAGE_ADDED 广播（安卓8+ 后台收不到），主动扫描，可靠可控。
 */
@Composable
private fun RogueScanCard() {
    val context = LocalContext.current
    var scanning by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<List<RogueScanner.ScanItem>?>(null) }   // null=未扫过
    var scanningError by remember { mutableStateOf<String?>(null) }
    val s = MaterialTheme.colorScheme

    fun doScan() {
        scanning = true
        scanningError = null
        found = null
        Thread {
            try {
                val items = RogueScanner.scan(context)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    scanning = false
                    found = items
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    scanning = false
                    scanningError = e.message ?: "扫描失败"
                }
            }
        }.start()
    }

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔍", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(T("一键扫描手机"), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = s.onSurface)
                    Text(T("主动识别装好的高风险 App（赌博/贷款/色情等）"), fontSize = 11.sp, color = s.onSurfaceVariant)
                }
                Button(
                    onClick = { doScan() },
                    enabled = !scanning,
                    colors = ButtonDefaults.buttonColors(containerColor = if (scanning) s.onSurface.copy(alpha=.3f) else s.primary)
                ) { Text(if (scanning) T("扫描中…") else T("开始扫描")) }
            }

            // 扫描结果
            val list = found
            if (list != null) {
                Spacer(Modifier.height(8.dp))
                if (list.isEmpty()) {
                    Text(T("✓ 未发现高风险 App"), fontSize = 13.sp, color = s.primary)
                } else {
                    Text(T("发现 ${list.size} 个高风险 App，点击移除："), fontSize = 13.sp, color = s.onSurfaceVariant)
                    list.forEach { item ->
                        RogueItemRow(item)
                    }
                }
            }
            if (scanningError != null) {
                Spacer(Modifier.height(8.dp))
                Text(T("扫描失败：${scanningError}"), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun RogueItemRow(item: RogueScanner.ScanItem) {
    val context = LocalContext.current
    val s = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth().padding(top = 6.dp).clickable {
            // 卸载该 App
            val ok = com.careshield.app.shizuku.ShizukuManager.execShell("pm uninstall --user 0 ${item.packageName}")
            android.widget.Toast.makeText(
                context,
                "尝试移除 ${item.label}（${ok?.trim() ?: "未授权，需先连接 Shizuku"}）",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = s.errorContainer)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = s.onErrorContainer)
                Text("${item.packageName} · ${item.reason}", fontSize = 11.sp, color = s.onErrorContainer.copy(alpha = .7f))
            }
            Text("移除", color = s.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
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
                Text(T("开启无障碍服务"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = s.onSecondaryContainer)
                Spacer(Modifier.height(2.dp))
                Text(T("需要在系统设置中开启「亲安盾守护服务」才能自动拦截广告"), fontSize = 11.sp, color = s.onSecondaryContainer.copy(alpha = 0.7f))
            }
            Button(onClick = onOpen, colors = ButtonDefaults.buttonColors(containerColor = s.primary)) {
                Text(T("去开启"), color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
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
                Text(T("Shizuku 未连接"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(T("高级拦截能力未开启，防流氓软件功能已降级"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onOpenShizuku, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE28A2A))) {
                Text(T("去连接"), fontWeight = FontWeight.Bold, color = Color.White)
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
                Text(T("Shizuku 连接中"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(T("请在系统弹窗中允许授权"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(T("Shizuku 已连接"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Text(T("高级拦截能力正常 · 防流氓软件已启用"), fontSize = 11.sp, color = Color(0xFFC9E8DA))
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
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (needsShizuku) {
                    Text(T("需 Shizuku 增强"), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
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
        // 方式1：读系统已启用服务字符串（最直接可靠）
        val expected = "com.careshield.app/com.careshield.app.accessibility.AdBlockerService"
        val enabledStr = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        if (enabledStr.contains(expected)) return true
        // 方式2：AccessibilityManager 列表比对（兜底）
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val cmp = ComponentName(context, AdBlockerService::class.java)
        am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
            it.resolveInfo.serviceInfo.packageName == cmp.packageName &&
            it.resolveInfo.serviceInfo.name == cmp.className
        }
    } catch (_: Exception) { false }
}
