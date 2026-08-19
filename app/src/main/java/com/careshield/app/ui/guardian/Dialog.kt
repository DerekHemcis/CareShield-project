package com.careshield.app.ui.guardian

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.careshield.app.data.SettingsRepo
import com.careshield.app.ui.locale.T

/**
 * 监护人验证对话框：默认密码 1234，验证通过后回调 onSuccess
 */
@Composable
fun GuardianVerifyDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val repo = SettingsRepo(context)
    var password by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    // 读取真实存储的监护人密码（默认 1234，可在监护人面板修改）
    val storedPass = remember { repo.getGuardianPassSync() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(22.dp)) {
                Text("🔐", fontSize = 30.sp)
                Spacer(Modifier.height(8.dp))
                Text(T("监护人入口"), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(T("请输入监护人密码以管理设置"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; wrong = false },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    label = { Text(T("密码")) },
                    isError = wrong,
                    supportingText = if (wrong) {{ Text(T("密码错误")) }} else null,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(T("可联系监护人确认密码"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(T("返回")) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (password == storedPass) {
                            onDismiss()
                            onSuccess()
                        } else {
                            wrong = true
                            Toast.makeText(context, "密码错误", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text(T("确认")) }
                }
            }
        }
    }
}
