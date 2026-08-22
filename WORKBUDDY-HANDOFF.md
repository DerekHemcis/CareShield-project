# WorkBuddy 交接文档 — CareShield 亲安盾

## 一、项目是什么
面向老人的 Android 安全上网 App：**自动跳过开屏广告** + 拦流氓软件 + 防误触下载。
- 包名 `com.careshield.app`，minSdk 26 / targetSdk 34
- 技术栈：Compose + DataStore + Shizuku + Kotlin 2.0.20 + AGP 8.5.2
- 编译：用户本机 Android Studio，Debug APK（无法云编译）

## 二、当前核心 BUG（重点）
**无障碍服务 Enabled 但无法真正运行，跳过广告功能完全无效。**

现象（真机 realme RMX6699 / ColorOS / Android16，KernelSU root）：
```
dumpsys accessibility:
  Enabled services: {com.careshield.app/minitest.MiniA11yService},
                    {com.careshield.app/accessibility.AdBlockerService}
  Crashed services: {MiniA11yService}, {AdBlockerService}   ← 关键
```
日志关键行：
```
AccessibilityServiceConnection: [binderDied] applicationInfo = com.careshield.app
AccessibilityUserState: [serviceDisconnectedLocked] add mCrashedServices (两个服务)
process=com.careshield.app reason=USER REQUESTED (force-stop 触发的，可忽略)
pss=0 rss=166MB   ← 进程常驻内存异常高
```

**已排除的假设：**
1. ❌ Manifest 声明问题 —— 已反编译 APK 确认两个 service 的 meta-data、BIND_ACCESSIBILITY_SERVICE、exported、intent-filter 全部正确
2. ❌ 资源丢失 —— accessibility_service_config.xml 和 mini_accessibility_config.xml 都打进了 APK
3. ❌ Application.onCreate 崩溃 —— 已 try-catch 包裹 Shizuku 初始化并去掉 requestPermission，**仍无效**；且 logcat 无任何 AndroidRuntime/FATAL 崩溃堆栈

**当前最可能方向（请验证）：**
- 无任何 crash 堆栈 + binderDied + rss=166MB → **进程是被系统杀掉的，不是代码崩溃**
- 强烈怀疑是 **ColorOS(realme 定制)激进的杀后台/防自启策略**：无障碍服务常驻进程被系统冻结/杀死 → binderDied → 服务进 Crashed → 系统不再 bind
- 白板 MiniA11yService（什么都不做）也被 Crashed，正好证明是**进程级被杀**，与业务逻辑无关

**建议排查顺序：**
1. `adb logcat -b crash -d` 确认到底有没有崩溃堆栈（用户环境已被 force-stop 干扰过，需干净复现）
2. 确认进程被杀的 evidence：`logcat | grep -E 'lowmemorykiller|lmkd|kill|Cached|reason=killed'`
3. ColorOS 对无障碍服务的处理：检查 App 是否被加入 ColorOS 电池优化白名单 / 自启动白名单
4. 排查为何 rss 高达 166MB（DataStore/Shizuku 初始化是否过重）
5. 尝试加前台服务保活或 `android:foregroundServiceType="mediaPlayback|specialUse"` 看是否缓解

## 三、工程结构
```
app/src/main/java/com/careshield/app/
├── accessibility/AdBlockerService.kt   无障碍广告拦截（多策略：文字/倒计时/激进跳过）
├── minitest/MiniA11yService.kt         白板无障碍测试服务（用于区分进程级 vs 逻辑级问题）
├── security/                           防流氓（RogueClassifier/RogueWatchdog/VirusTotalClient/DnsVpnService）
├── data/SettingsRepo.kt                DataStore 配置/白名单/日志
├── shizuku/ShizukuManager.kt           Shizuku (rikka) 集成
├── ui/                                 Compose 界面（MainShell/pages/locale/guardian）
└── res/raw/ad_hosts.txt                AdGuard 全量广告域名(2万条)
```

## 四、参考开源
- NetGuard (M66B)、RethinkDNS、AdGuardSDNSFilter 的 DNS/VpnService 拦截
- 自动跳过广告参考：LSP 框架 + ACCESSIBILITY 自动点击

## 五、用户测试环境
- realme RMX6699 (ColorOS) + vivo（用户实测两台都不行 → 普遍性，非设备）
- KernelSU root + LSPosed + Shizuku root 授权
- 已装：无障碍服务已开启（Enabled 列表能看到）

## 六、最终目标
1. **先让无障碍服务能真正 Running 并收到事件**（解决 binderDied→Crashed）
2. 自动跳过：咪咕视频/腾讯/搜狐/乐视/中羽/有道 的开屏广告（都含"跳过"文字或倒计时按钮）
3. 尽力实现完整 DnsVpnService（VpnService 全路由+流量转发）从 DNS 源头拦广告

## 七、开发历史关键点
- 从 KernelSU_v3.2.5 反编译提取 AGSL 液态玻璃 shader 用于 UI（非 bug 相关）
- 曾多次改 UI/主题/多语言，与当前 bug 无关
- AdBlockerService 业务逻辑（找"跳过/广告"文字、找倒计时、findAndClick）在逻辑上测试通过，只是服务根本没跑起来
