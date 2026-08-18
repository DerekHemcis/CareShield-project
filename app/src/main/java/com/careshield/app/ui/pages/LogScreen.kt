package com.careshield.app.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.careshield.app.data.SettingsRepo
import kotlinx.coroutines.runBlocking

@Composable
fun LogScreen() {
    val context = LocalContext.current
    val repo = SettingsRepo(context)
    var logText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        logText = runBlocking { repo.getLog() }.ifEmpty { "暂无拦截记录\n\n每天自动记录拦截的广告与弹窗，保护情况一目了然。" }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("拦截日志", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(logText, fontSize = 13.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
