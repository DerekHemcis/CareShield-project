package com.careshield.app

import android.app.Application
import com.careshield.app.shizuku.ShizukuManager

class CareShieldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 启动即绑定并尝试连接 Shizuku（有服务则自动请求授权，符合"Shizuku 自动连接"）
        ShizukuManager.bind(this)
        if (ShizukuManager.isAvailable() && !ShizukuManager.isEnhanceReady()) {
            ShizukuManager.requestPermission()
        }
    }
}
