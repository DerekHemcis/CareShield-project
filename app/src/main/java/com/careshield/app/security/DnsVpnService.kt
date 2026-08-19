package com.careshield.app.security

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.careshield.app.R

/**
 * 本机 DNS 过滤 VPN（AdGuard 简化版）：
 * 建立本地 VPN，将系统 DNS 指向广告过滤 DNS（AdGuard/114 等），从源头拦截广告域名。
 * 一旦激活，所有 App 的 DNS 解析都走广告过滤 DNS，视频/资讯类 App 的开屏广告域名被解析为 0.0.0.0 → 广告不加载。
 */
class DnsVpnService : VpnService() {

    private var fd: ParcelFileDescriptor? = null

    companion object {
        // 广告过滤 DNS（AdGuard DNS：拦截广告/跟踪）
        private val AD_DNS = arrayOf(
            "94.140.14.14",  // dns.adguard.com
            "94.140.15.15"
        )
        private const val CHANNEL_ID = "careshield_vpn"
        const val ACTION_STOP = "com.careshield.app.STOP_VPN"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 停止指令
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundCompat()
        val builder = Builder()
        builder.setSession("CareShield DNS 过滤")
        builder.setMtu(1400)
        // 分配本地地址（仅 DNS，可极简 /30）
        builder.addAddress("10.0.1.1", 32)
        // 关键：把 DNS 指向广告过滤 DNS，路由所有流量（DNS 走我们）
        builder.addRoute("0.0.0.0", 0)
        // 设置 DNS（循环 add，兼容 Builder API）
        AD_DNS.forEach { builder.addDnsServer(it) }
        try {
            fd?.close()
            fd = builder.establish()
            Log.i("CareShield", "DNS VPN established, DNS=${AD_DNS.joinToString()}")
        } catch (e: Exception) {
            Log.e("CareShield", "VPN establish failed: ${e.message}")
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        try { fd?.close() } catch (_: Exception) {}
        fd = null
        Log.i("CareShield", "DNS VPN stopped")
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "CareShield 防护", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CareShield 防护运行中")
            .setContentText("正在过滤广告域名（DNS）")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }
}
