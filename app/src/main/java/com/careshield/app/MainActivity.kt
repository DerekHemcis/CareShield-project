package com.careshield.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.careshield.app.data.SettingsRepo
import com.careshield.app.shizuku.ShizukuManager
import com.careshield.app.ui.CareShieldTheme
import com.careshield.app.ui.nav.MainShell
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // 全局 Shizuku 状态：0=未连接 1=请求中 2=已连接
    private val shizukuState = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 应用已保存的语言（零重启即时生效）
        com.careshield.app.ui.locale.AppLocaleManager.applyLocale(this)

        // 注册 Shizuku 权限结果监听：授权成功 → 更新状态为已连接
        ShizukuManager.setPermissionResultListener { _, granted ->
            shizukuState.value = if (granted) 2 else 0
        }
        // 初始检测
        shizukuState.value = if (ShizukuManager.isEnhanceReady()) 2 else 0

        setContent {
            val scope = rememberCoroutineScope()
            var theme by remember { mutableStateOf(0) }
            val repo = SettingsRepo(this)
            LaunchedEffect(Unit) {
                theme = repo.get("theme", "0").toIntOrNull() ?: 0
            }
            CareShieldTheme(theme) {
                MainShell(
                    theme = theme,
                    onThemeChange = { t -> theme = t; scope.launch { repo.put("theme", t.toString()) } },
                    onOpenAccessibility = { openAccessibility(this@MainActivity) },
                    onOpenShizuku = { connectShizuku() },
                    shizukuState = shizukuState.value
                )
            }
        }
    }

    /** 点击"去连接"：一键连接 Shizuku */
    private fun connectShizuku() {
        when (ShizukuManager.connect()) {
            ShizukuManager.ConnectResult.READY -> {
                shizukuState.value = 2
            }
            ShizukuManager.ConnectResult.REQUESTING -> {
                shizukuState.value = 1 // 已发起授权，等待用户在系统弹窗确认
            }
            ShizukuManager.ConnectResult.NEED_START -> {
                shizukuState.value = 0
                // Shizuku 服务未运行：跳转 Shizuku 管理器引导（需用户开启）
                openShizuku(this@MainActivity)
            }
        }
    }

    private fun openAccessibility(ctx: Context) {
        runCatching { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }

    private fun openShizuku(ctx: Context) {
        runCatching { ctx.startActivity(Intent("moe.shizuku.manager.intent.action.API_V2")) }
    }
}
