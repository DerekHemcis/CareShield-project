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
├── MainActivity.kt          入口
├── CareShieldApp.kt         Application（绑定 Shizuku）
├── accessibility/          无障碍广告拦截服务（关键词规则库）
│   └── AdBlockerService.kt
├── data/SettingsRepo.kt    DataStore 设置/白名单/日志
├── shizuku/ShizukuManager.kt  Shizuku 集成
├── ui/
│   ├── Theme.kt            Material3 深浅主题
│   ├── nav/                MainShell + 液态玻璃导航
│   ├── pages/              首页/设置/日志/关于
│   ├── guardian/Dialog.kt  监护人密码验证
│   └── liquidglass/        液态玻璃（AGSL 入口）
app/src/main/res/raw/
├── liquid_glass_dispersion.agsl  KernelSU 提取：折射+7色色散
└── liquid_glass_base.agsl        KernelSU 提取：基础折射
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
