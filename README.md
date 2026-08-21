# CareShield 亲安盾 — Android 工程

面向长辈的安全上网助手：无障碍广告拦截 + Shizuku 防流氓 + 白名单 + 监护人密码。

包含从 **KernelSU APK 提取的真实液态玻璃 AGSL shader**（`res/raw/liquid_glass_*.agsl`）。

---

## 一、在 Android Studio 中编译（Windows）

### 1. 打开工程
- 启动 Android Studio → **Open** → 选本文件夹（含 `settings.gradle.kts` 的根目录）
- 首次打开会自动 **Gradle Sync**（需联网下载依赖，约几分钟）

### 2. 选择构建变体
- 右下角 **Build Variants** → 选 `debug`（调试可安装）或 `release`

### 3. 编译 APK
两种方式任选：
- **菜单方式**：`Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
- **命令行方式**：在项目根目录打开终端（Android Studio 自带的 Terminal），运行：
  ```
  gradlew.bat assembleDebug
  ```
  或用 PowerShell / CMD：
  ```
  .\gradlew.bat assembleDebug
  ```

### 4. 找到 APK
```
app\build\outputs\apk\debug\app-debug.apk
```
- 把这个 APK 传到老人手机安装即可
- 手机需开启「允许安装未知来源应用」安装

---

## 二、版本要求
- **Android Studio**: Koala (2024.1) 或更新版本
- **JDK**: Android Studio 自带（配置为 17）
- **Gradle/AGP**: 工程已锁定 Gradle 8.9 + AGP 8.5.2 + Kotlin 2.0.20 + Compose BOM 2024.09
- **手机上**: Android 8.0（API 26）及以上；液态玻璃 AGSL 效果需 Android 12（API 31+），低版本自动回退为普通胶囊

---

## 三、首次安装后设置（一次性，监护人操作）

1. **安装 APK** → 打开 App
2. **开启无障碍**：App 首页点状态卡 → 系统设置 → 无障碍 → 打开「亲安盾守护服务」（授权后广告拦截才生效）
3. **可选 Shizuku 增强**：首页「Shizuku 未连接」卡 → 去开启（无 root 手机需要 Shizuku App + 无线调试激活一次）
4. **设置监护人密码**：默认 1234，可在监护人模式中修改

---

## 四、工程结构
```
app/src/main/java/com/careshield/app/
├── MainActivity.kt          入口（语言热重启、Shizuku连接）
├── CareShieldApp.kt         Application（绑定 Shizuku + 自动连）
├── accessibility/          无障碍广告拦截服务
│   └── AdBlockerService.kt  多策略：文字/倒计时/激进/防误触/信息流
├── data/SettingsRepo.kt    DataStore 设置/白名单/日志/API key
├── shizuku/ShizukuManager.kt  Shizuku 集成
├── security/
│   ├── RogueClassifier.kt   流氓软件7类判定引擎
│   ├── RogueWatchdog.kt     新装APK监听+云检+本地兜底拦截
│   ├── VirusTotalClient.kt  Virustotal 云端哈希识别
│   └── DnsVpnService.kt     DNS 过滤 VPN（源头拦广告域名）
│   └── DnsBlocker.kt        hosts 域名拦截（AdGuard规则）
├── ui/
│   ├── Theme.kt            Material3 深浅主题
│   ├── nav/MainShell.kt    主壳 + 手势切页 + 底部导航
│   ├── nav/LiquidGlassNavBar.kt workbuddy 转写的液态玻璃导航栏（可拖拽药丸指示器 + 磨砂玻璃）
│   ├── pages/              首页/设置/日志/关于
│   ├── guardian/           监护人面板/白名单/改密码
│   ├── locale/AppLocaleManager.kt + Translator.kt  多语言(简/繁/英)
│   └── shizuku/ShizukuGuideScreen.kt  图形化引导向导
app/src/main/res/
├── raw/ad_hosts.txt        3500+ 广告域名（AdGuard规则提取）
└── raw/liquid_glass*.agsl  KernelSU 提取 AGSL shader
```

---

## 五、关键词规则库（广告/弹窗识别）
`AdBlockerService.kt` 内含规则库，可按需增删：
- 跳过广告：`跳过、点击跳过、Skip...`
- 关闭弹窗：`关闭、知道了、取消、×、Close...`

规则可按需扩充，或在 `SettingsRepo` 中做云同步/配置。

---

## 六、已知说明
- 无障碍拦截为**关键词规则库**方案，能覆盖开屏广告/常见弹窗；对抗高度混淆的图片广告有限
- 若编译报错，通常是 Compose API 版本差异，按报错提示小修（SDK 会自动给出修正建议）

---

## 七、当前版本特性 (v1.2.x)

### 广告拦截（多层）
- **无障碍点跳过**：识别"跳过/倒计时"文字按钮自动点（中羽等含文字按钮可拦）
- **激进跳过（实验）**：全屏图片广告 + 角落倒计时自动点（可选项开关）
- **DNS 广告过滤（VpnService）**：把系统 DNS 指向广告过滤 DNS，**从源头拦广告域名**，视频/资讯类 App 开屏广告不加载（解决无障碍点不到的 webview 广告）

### 防流氓（智能识别）
- **Virustotal 云端**：设置页可填 API key，新装 APK 哈希查云端多引擎判定
- **本地兜底**：危险词库（90+）+ 危险 App 名单 + RogueClassifier 7 类
- **Shizuku 拦截**：命中恶意则自动卸载

### 设备保护
- 无障碍 + Shizuku 增强 + DNS VPN 三通道
- 多语言（简/繁/英）、主题切换、监护人面板、配置备份

### 依赖
- Virustotal API（可选，设置页填）；AdGuard 过滤规则提取的广告域名（内置）
- Shizuku（增强拦截）；DNS VPN（需一次授权）

