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
│   ├── RogueWatchdog.kt     新装APK监听(受限)
│   ├── RogueScanner.kt      一键扫描已装App，识别高风险并卸除
│   └── VirusTotalClient.kt  Virustotal 云端哈希识别
├── ui/
│   ├── Theme.kt            Material3 深浅主题
│   ├── nav/MainShell.kt    主壳 + 手势切页 + 底部导航
│   ├── nav/GlassNavigation.kt Material3 底部导航（玻璃/经典）
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

## 七、当前版本特性 (v1.7.0)

### 广告拦截（无障碍，实测）
- **无障碍点跳过**：识别"跳过/倒计时"文字按钮自动点。
  实测可拦：**有道词典 / 中羽在线 / 咪咕视频** 的开屏广告。
- 视频类 App（腾讯/搜狐）开屏广告在独立窗口，无障碍扫不到，暂未稳定跳过（见 PROJECT-RECORD 第十四章）。

### 防流氓软件（主动扫描，可靠）
- **一键扫描手机（推荐）**：首页「一键扫描手机」→ 主动扫描已装 App，`RogueClassifier` 本地
  危险词库（色情/赌博/贷款/私彩等）判定 → 列出高风险 → 逐个 **Shizuku `pm uninstall` 一键移除**。
  不依赖后台广播，只要点击就必能发现问题 App。
- **Virustotal 云端**（可选）：设置页填 API key，哈希查云端多引擎判定。
- 说明：原先"装完自动卸载"依赖 `PACKAGE_ADDED` 广播，安卓 8+ 后台收不到，已不作为主路径。

### 设备保护
- 无障碍 + Shizuku 增强 双通道；多语言（简/繁/英）、主题切换、监护人面板、日志导出。

### 依赖
- Virustotal API（可选）；AdGuard 广告域名库（备用）；Shizuku（防流氓卸载 / 增强拦截）。

### 已知边界
- DNS/VpnService 拦截不可行（hosts 只读、ndc 不被识别），已移除。
- 腾讯视频 / 搜狐视频 开屏广告暂不支持自动跳过。

