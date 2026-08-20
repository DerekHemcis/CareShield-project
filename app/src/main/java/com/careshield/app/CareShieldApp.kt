package com.careshield.app

import android.app.Application
import android.util.Log
import com.careshield.app.shizuku.ShizukuManager

class CareShieldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 关键：Application.onCreate 绝不能崩溃（否则整个进程起不来，无障碍服务 disconnected）
        // Shizuku 初始化全部 try-catch，且不在 Application 阶段请求权限（避免崩）
        try {
            ShizukuManager.bind(this)
        } catch (e: Throwable) {
            Log.w("CareShield", "bind shizuku: ${e.message}")
        }
    }
}
