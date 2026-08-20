package com.careshield.app.security

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * DNS 投放拦截（可靠版：纯 DNS 代理，不全路由）
 * - VpnService 仅路由 VPN 网段 + 把 DNS 指到本机代理(10.0.1.1:53)
 * - App 的 DNS 查询发到 10.0.1.1:53 → 本代理收
 *   - 广告域名 → 回 0.0.0.0（广告素材不加载 → 开屏广告不显示）
 *   - 正常域名 → 经 socket 转发 114，响应原路回
 * - 不在 VPN 全路由 → 其余 App 流量走真实网络 → 不断网
 */
class DnsVpnService : VpnService() {

    companion object {
        private const val TAG = "CareShield"
        private const val CHANNEL_ID = "careshield_vpn"
        private const val LOCAL_IP = "10.0.1.1"
        const val ACTION_STOP = "com.careshield.app.STOP_VPN"
        private val UP = InetSocketAddress("114.114.114.114", 53)
    }

    private var fd: ParcelFileDescriptor? = null
    private var running = false
    // 本机 DNS 代理 socket
    private val proxySock = DatagramSocket()
    private val idMap = ConcurrentHashMap<Int, String>()  // respId -> clientKey

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        try {
            startForegroundCompat()
            val b = Builder()
            b.setSession("CareShield 广告过滤").setMtu(1400)
            b.addAddress(LOCAL_IP, 24)
            b.addDnsServer(InetAddress.getByName("114.114.114.114"))
            // 只路由 VPN 网段（内网），不路由 0.0.0.0 → App 其余流量走真实网络不断网
            b.addRoute(LOCAL_IP, 24)
            fd?.close()
            fd = b.establish()
            if (fd == null) { Log.e(TAG, "establish null"); stopSelf(); return START_NOT_STICKY }
            protect(proxySock)
            running = true
            // 绑定 10.0.1.1:53 收 App 的 DNS
            proxySock.reuseAddress = true
            proxySock.bind(InetSocketAddress(LOCAL_IP, 53))
            Thread { proxyLoop() }.start()
            Thread { upstreamLoop() }.start()
            Log.i(TAG, "DNS Proxy up @ $LOCAL_IP:53")
        } catch (e: Exception) {
            Log.e(TAG, "start ${e.message}"); runCatching { stopSelf() }
        }
        return START_STICKY
    }

    /** 本机代理：收 App DNS 查询 */
    private fun proxyLoop() {
        val buf = ByteArray(512)
        while (running) {
            try {
                val p = DatagramPacket(buf, buf.size)
                proxySock.receive(p)
                val d = ByteArray(p.length)
                System.arraycopy(buf, 0, d, 0, p.length)
                if (d.size < 12) continue
                val id = ByteBuffer.wrap(d, 0, 2).short.toInt() and 0xffff
                val qname = parseQ(d) ?: continue
                val clientKey = "${p.address.hostAddress}:${p.port}"
                Log.i(TAG, "DNS $qname @$clientKey")
                if (isAdDomain(qname)) {
                    val resp = buildResp(id.toShort(), qname, "0.0.0.0")
                    proxySock.send(DatagramPacket(resp, resp.size, p.address, p.port))
                } else {
                    idMap[id] = clientKey
                    // 原样转发上游
                    upSock(d)
                }
            } catch (_: Exception) { if (!running) break }
        }
    }

    private val upstreamSock = DatagramSocket()

    private fun upSock(data: ByteArray) {
        try { upstreamSock.send(DatagramPacket(data, data.size, UP)) }
        catch (e: Exception) { Log.e(TAG, "up ${e.message}") }
    }

    /** 上游响应回客户端 */
    private fun upstreamLoop() {
        val rb = ByteArray(512)
        while (running) {
            try {
                val p = DatagramPacket(rb, rb.size)
                upstreamSock.receive(p)
                val d = ByteArray(p.length); System.arraycopy(rb, 0, d, 0, p.length)
                val id = ByteBuffer.wrap(d, 0, 2).short.toInt() and 0xffff
                val ck = idMap[id] ?: continue
                val sp = ck.lastIndexOf(':'); val ip = ck.substring(0, sp); val port = ck.substring(sp+1).toInt()
                proxySock.send(DatagramPacket(d, d.size, InetAddress.getByName(ip), port))
                idMap.remove(id)
            } catch (_: Exception) { if (!running) break }
        }
    }

    private fun parseQ(d: ByteArray): String? {
        var i = 12; if (d.size < 13) return null
        val sb = StringBuilder()
        while (i < d.size) {
            val l = d[i].toInt() and 0xff
            if (l == 0) { i++; break }
            if (l > 63) return null
            i++; if (i+l > d.size) return null
            for (k in 0 until l) sb.append(d[i+k].toChar())
            sb.append("."); i += l
        }
        return sb.toString().trimEnd('.')
    }

    private fun buildResp(id: Short, qname: String, ip: String): ByteArray {
        try {
            val ipB = InetAddress.getByName(ip).address
            val bb = ByteBuffer.allocate(512)
            bb.putShort(id); bb.putShort(0x8180.toShort())
            bb.putShort(1); bb.putShort(1); bb.putShort(0); bb.putShort(0)
            for (lbl in qname.split(".")) { bb.put(lbl.length.toByte()); bb.put(lbl.toByteArray()) }
            bb.put(0)
            bb.putShort(1); bb.putShort(1); bb.putInt(300); bb.putShort(ipB.size.toShort()); bb.put(ipB)
            return ByteArray(bb.position()).also { System.arraycopy(bb.array(),0,it,0,it.size) }
        } catch (_: Exception) { return ByteArray(0) }
    }

    private fun isAdDomain(d: String): Boolean {
        val x = d.lowercase().trimEnd('.')
        return AD_DOMAINS.any { x == it || x.endsWith(".$it") }
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CHANNEL_ID,"DNS",NotificationManager.IMPORTANCE_LOW))
        startForeground(1, NotificationCompat.Builder(this,CHANNEL_ID)
            .setContentTitle("CareShield 广告过滤").setContentText("正在拦截广告域名")
            .setSmallIcon(android.R.drawable.ic_lock_lock).setOngoing(true).build())
    }

    override fun onDestroy() {
        running = false
        try { fd?.close() } catch (_: Exception) {}
        fd = null; runCatching { proxySock.close() }; runCatching { upstreamSock.close() }
        Log.i(TAG, "stopped")
        super.onDestroy()
    }

    private val AD_DOMAINS: Set<String> by lazy {
        val s = mutableSetOf<String>()
        try {
            s.addAll(resources.openRawResource(com.careshield.app.R.raw.ad_hosts).bufferedReader()
                .readLines().mapNotNull { it.trim().split(Regex("\\s+")).takeIf { it.size>=2 && it[0]=="0.0.0.0" }?.get(1) })
        } catch (_: Exception) {}
        s.addAll(listOf("googleads.g.doubleclick.net","doubleclick.net","googlesyndication.com","adnxs.com","adsrvr.org",
            "bidswitch.net","adservice.google.com","googletagservices.com","adsafeprotected.com","casalemedia.com",
            "mopub.com","advertising.com","unityads.unity3d.com","appsflyer.com","adjust.com","admob.com",
            "qpic.cn","mqqad.com","ads.qq.com","imasdk.googleapis.com","dldir1.qq.com",
            "yangege.cn","sogoucdn.com","sohucs.com","qqmail.com"))
        s
    }
}
