package com.careshield.app.ui.guardian

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careshield.app.data.SettingsRepo
import com.careshield.app.ui.locale.T
import android.widget.Toast
import kotlinx.coroutines.runBlocking

/**
 * 修改监护人密码
 */
@Composable
fun ChangePassScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = SettingsRepo(context)
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var newPass2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            TextButton(onClick = onBack) { Text(T("‹ 返回")) }
        }
        Spacer(Modifier.height(20.dp))
        Text("🔑", fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text(T("修改监护人密码"), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = oldPass, onValueChange = { oldPass = it; error = "" },
            label = { Text(T("当前密码")) }, visualTransformation = PasswordVisualTransformation(),
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = newPass, onValueChange = { newPass = it; error = "" },
            label = { Text(T("新密码")) }, visualTransformation = PasswordVisualTransformation(),
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = newPass2, onValueChange = { newPass2 = it; error = "" },
            label = { Text(T("确认新密码")) }, visualTransformation = PasswordVisualTransformation(),
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val current = repo.getGuardianPassSync()
            when {
                oldPass != current -> error = "当前密码错误"
                newPass.length < 4 -> error = "新密码至少 4 位"
                newPass != newPass2 -> error = "两次输入的新密码不一致"
                else -> {
                    runBlocking { repo.setGuardianPass(newPass) }
                    Toast.makeText(context, "密码已修改", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(T("确认修改"))
        }
    }
}
