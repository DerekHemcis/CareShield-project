package com.careshield.app

import android.app.Application
import com.careshield.app.shizuku.ShizukuManager

class CareShieldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 应用启动即尝试绑定 Shizuku，并绑定无障碍状态
        ShizukuManager.bind(this)
    }
}
