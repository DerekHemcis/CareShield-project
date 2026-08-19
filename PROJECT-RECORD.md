# CareShield 亲安盾 — 项目开发全记录

> 本文档完整记录「亲安盾（CareShield）」App 从需求到落地的全部开发过程，供项目报告 / 答辩 / 复盘使用。
> 目标人群：面向老人的安全上网助手（自动跳过广告、拦截弹窗、防误触下载、识别拦截流氓软件）。

---

## 一、项目背景与需求来源

用户（监护人）希望为家中老人打造一款**无需老人操作、自动防护**的手机安全 App，核心诉求：

1. **广告困扰**：老人刷视频（抖音）、用微信小程序时，频繁出现开屏广告、贴片广告、诱导下载弹窗
2. **流氓软件困扰**：误触下载、诱导安装流氓应用，难以辨别
3. **操作门槛**：老人不会安装 App、不会开启权限、不会管理设置 → 需"一次配置、全自动运行"
4. **无需 root**：老人手机是普通未 root 设备，方案必须基于无障碍服务 + Shizuku 免 root 能力

---

## 二、需求澄清与关键决策（多轮沟通）

### 2.1 名字确定
- 候选：守护盾 / CareShield / 护老盾 / 安心卫士 / 家护
- **最终选定：CareShield（亲安盾）** — 兼具科技感与"护航家人"定位

### 2.2 技术方案选型
- **广告拦截方式**：用户明确选择 **关键词规则库**（识别"跳过/关闭/×"等按钮自动点击）
- **增强能力**：Shizuku（免 root 系统权限）用于拦截流氓软件安装
- **老人机 root 情况**：确认老人手机「未 root」 → Shizuku 需无线调试方式

### 2.3 功能优先级与 UI
- 核心：首页大状态卡 + 统计数字 + 4 个功能开关（自动跳过广告/弹窗自动关闭/防误触下载/流氓软件拦截）
- 底部 4 tab 导航（功能/设置/日志/关于）
- **监护人入口**（用户采纳）：密码验证后管理白名单、权限、防护设置
- **Shizuku 状态卡**：检测到未连接时首页显示警示卡 + "去开启"引导

---

## 三、UI 原型开发（HTML 阶段）

### 3.1 初始 UI（仿 LSPosed/KernelSU 管理器风格）
- 顶栏 + 状态大卡 + 三格统计 + 功能开关列表 + 底部三 tab
- Material Design 风格、卡片式布局

### 3.2 底部导航多次迭代（用户反复打磨视觉效果）

| 版本 | 需求/反馈 | 实现 |
|------|----------|------|
| v1 | 模仿 LSPosed 底栏 | 三 tab 线性图标 |
| v2 | 高级、模仿 KernelSU | Material3 风格、毛玻璃、背板高亮 |
| v3 | 液体玻璃效果 | 引入 liquid glass（毛玻璃+高光）|
| v4 | **复刻 LiquidGlass-NavBar 项目** | 逐行分析该项目源码，复刻"可拖动的玻璃指示器+吸附"机制 |
| v5 | 用户反馈"不够液态、金色换透明" | 指示器改透明玻璃 + 按住溢出 dock + 流体流动 |
| v6 | 深色/浅色主题适配导航栏 | 导航栏深/浅两套玻璃 |
| v7 | 用户重申"任何颜色都不要，纯无色透明" | 彻底去彩色，纯透明玻璃 + 中性高光 |
| v8 | 折射/色散/环境高光增强 | 多层 RGB 色散、强模糊折射、环境光斑 |

### 3.3 主题与多语言
- 主题设置：跟随系统 / 浅色 / 深色（导航栏不随主题变 → 后来按需适配）
- 语言：简体 / 繁体 / English 三语切换

### 3.4 二级页面系统
- Shizuku 开启引导页
- 监护人验证页（密码 1234）
- 监护人管理面板
- 白名单管理页

### 3.5 Bug 修复记录
- 液态玻璃开关被底部导航遮挡 → 修复 body height 锁定导致 bottom 留白失效的问题
- 最右"关于"tab 未对齐 → 修复 layoutIndicator 的 clamp 边界错误（maxCenter 算法）

---

## 四、从 KernelSU APK 提取液态玻璃技术资产

用户提供了 **KernelSU_v3.2.5 APK**，要求在工程中复用其液态玻璃实现。

### 4.1 反编译过程
- 工具：jadx 1.5.0（沙箱安装）+ Java 17
- 反编译 APK → 定位核心实现文件 `sources/defpackage/zr.java`（混淆后）

### 4.2 提取到的核心资产
找到 KernelSU 液态玻璃的两段 **AGSL runtime shader** 完整源码：
1. **`LiquidGlassLensDispersion`** — 折射 + **7 色棱镜色散**（红/橙/黄/绿/青/蓝/紫 各通道分离采样重组）
2. **`LiquidGlassLens`** — 基础折射透镜

**技术参数**（uniform）：
- `size` / `offset` / `cornerRadii`（四角圆角）
- `refractionHeight`（折射高度）、`refractionAmount`（折射强度）
- `depthEffect`（景深）、`chromaticAberration`（色散强度）

**算法核心**：
- SDF（有向距离场）圆角矩形 `sdRoundedRect` 判定玻璃边界
- 曲面折射映射 `circleMap(1 - -sd/refractionHeight) * refractionAmount`（越靠边界折射越强）
- 7 色分离色散重组（对照真实棱镜光学）

### 4.3 落地
- 完整 shader 写入工程 `app/src/main/res/raw/liquid_glass_dispersion.agsl` 与 `liquid_glass_base.agsl`
- AGSL 需 Android 12+ (API31+) `RenderEffect`/`RuntimeShader` 运行；低版本回退纯色胶囊

本提取成果对纯 web 是"近似"，对 **Android 原生工程可 100% 还原**。

---

## 五、正式 Android 工程开发

### 5.1 技术栈
```
Gradle 8.9 + AGP 8.5.2 + Kotlin 2.0.20 + Compose BOM 2024.09
minSdk 26 / targetSdk 34 / JDK 17
依赖：Compose Material3 + DataStore + Shizuku (rikka 13)
```

### 5.2 工程结构
```
app/src/main/java/com/careshield/app/
├── MainActivity.kt          入口（Compose setContent）
├── CareShieldApp.kt         Application（绑定 Shizuku）
├── accessibility/AdBlockerService.kt  无障碍广告拦截（关键词规则库）
├── data/SettingsRepo.kt     DataStore（设置/白名单/拦截日志）
├── shizuku/ShizukuManager.kt  Shizuku 集成
├── ui/
│   ├── Theme.kt             Material3 深浅主题
│   ├── nav/MainShell.kt     主壳 + 四页导航
│   ├── nav/GlassNavigation.kt  液态玻璃导航
│   ├── pages/               首页/设置/日志/关于
│   ├── guardian/Dialog.kt   监护人密码验证
│   └── liquidglass/LiquidGlass.kt  AGSL 接口
res/raw/*.agsl               KernelSU 提取的液态玻璃 shader
```

### 5.3 各功能实现
- **无障碍拦截**：监听窗口变化 → 遍历可点击节点 → 匹配关键词（跳过/关闭/×）→ 自动点击；带节流、白名单保护、日志
- **Shizuku**：Manifest 声明 ShizukuProvider → 版本兼容绑定 + 权限请求 + shell 执行（拦流氓安装）
- **数据持久化**：DataStore（功能开关/白名单/语言/主题/拦截日志）
- **监护人**：密码验证对话框（默认 1234）

### 5.4 编译排查（迭代修复记录）
初版 `assembleDebug` 出现错误，逐一修复：

| # | 文件 | 错误 | 修复 |
|---|------|------|------|
| 1 | MainActivity.kt | 缺 `launch` import；挂起函数 `put` 非协程调用 | 加 `import kotlinx.coroutines.launch` |
| 2 | ShizukuManager.kt | `bindRequestService` / `AUTHORITY` 不存在 | 改为 V13 正确 API `/ 去 Provider 依赖` |
| 3 | Dialog.kt | `sp` 未解析（字号） | 加 `import androidx.compose.ui.unit.sp` |
| 4 | LiquidGlass.kt | `RenderNode` 未解析 | 删错误 import |
| 5 | GlassNavigation.kt | `luminance` 未解析 | 加 `import androidx.compose.ui.graphics.luminance` |
| 6 | HomeScreen.kt | `luminance` ×2、`SectionTitle` 重载冲突 ×2、`weight` 误调用 | 加 import / 改名 / 改传参 |
| 7 | ShizukuManager.kt | `Shizuku.bind` 已废弃 | Manifest 加 ShizukuProvider，删 bind |
| 8 | ShizukuManager.kt | `!Shizuku.checkSelfPermission()` 取反报错 | 移除 checkSelfPermission，仅调 requestPermission |

编译已从 14 个错误逐步清零。

---

## 六、发布与交付

### 6.1 本地编译路径
- 用户本机 Android Studio（Koala+）打开工程 → Gradle Sync → Build APK
- APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

### 6.2 GitHub Actions 编译（尝试过）
- 用户曾计划用 GitHub Actions 云编译 APK → 编写了 `.github/workflows/build-apk.yml`（gradle 8.9 + JDK17 + assembleDebug + upload artifact）
- 后因用户改回本地 Android Studio 编译而改用

### 6.3 安装后配置（监护人一次性操作）
1. 安装 APK
2. 开启无障碍服务（亲安盾守护服务）
3. 可选：Shizuku 增强（无 root 需无线调试激活）
4. 设置/确认监护人密码

---

## 七、技术要点总结

1. **免 root 方案**：无障碍服务（AccessibilityService）+ Shizuku（系统权限）双通道
2. **拦截核心**：关键词规则库（跳过/关闭/×） + 节流 + 白名单
3. **液态玻璃**：从 KernelSU 反编译提取 AGSL shader → 7 色棱镜色散 + 曲面折射
4. **UI**：Compose + Material3，KernelSU 风格玻璃导航，多主题多语言
5. **安全键**：监护人密码防老人误改、白名单限制可用应用

---

## 八、可改进方向（未来）

- 广告拦截从关键词规则库扩展为图像/控件特征识别（提高对混淆广告的拦截率）
- Shizuku root 方式自启（若老人机 root，可全自动）
- 拦截规则云端更新
- 使用时段限制、位置守护等更多监护人功能

---

## 九、迭代升级：策略重构 与 监护人体系（2026-08-19）

### 9.1 广告识别重构：智能置信度判定（参考 AdGuard 思路）
原始"见 x 就点"导致浏览器标签被误关。重构为三级证据 + 上下文验证：

| 级别 | 触发条件 | 处理 |
|------|---------|------|
| 强证据 | "跳过广告/关闭广告/点击跳过" | 直接拦截 |
| 中证据 | 界面有"广告"上下文时的 跳过/关闭/× | 拦截 |
| 弱证据 | 裸 ×/知道了 | 需 广告上下文 + 屏幕角落 + 小尺寸 才拦 |

新增判定：`detectAdContext`（检测"广告/Ad"标签）、`scoreNode`（评分）、`isCornerNode`（角落约束）、`findClickableAncestor`（找最近可点击父节点）。防误杀：浏览器标签 × 不满足广告上下文，不碰。

### 9.2 多策略识别
- 文字关键词（中英繁）、倒计时、contentDescription
- **信息流"广告"标签 → 自动上滑跳过**（`dispatchGesture` 手势）
- 图片假 × 防护：无文字/无描述的小角标不主动点（防误触跳广告）

### 9.3 老人 / 监护人 权限分离
- **老人可见**（主设置页「防护功能」）：自动跳过广告、关闭弹窗、滑屏跳过、拦截网页跳转、连接 Shizuku
- **监护人专属**（密码进入）：白名单保护模式、白名单应用管理、修改密码

### 9.4 监护人面板（密码验证后进入）
- **白名单保护模式**：开关 + `getWhitelistMode/setWhitelistMode` 持久化，控制 AdBlockerService 豁免逻辑
- **白名单应用管理**（WhitelistScreen）：`getInstalledApplications` 列出本机应用，Checkbox 勾选加入白名单（DataStore 存储）
- **修改监护人密码**（ChangePassScreen）：验证旧密码 → 设置新密码，GuardianVerifyDialog 读取存储密码校验（默认 1234）

### 9.5 UI 交互改进
- **语言/主题改为弹选择器**：点击弹出 AlertDialog 选项单选（不再是循环轮换）
- **日志新增拦截时间**：LogScreen 解析 `timestamp|source|action`，显示格式化时间（MM-dd HH:mm）+ 来源 + 拦截内容，最新在前

### 9.6 诊断探针
AdBlockerService 收到窗口事件即打印 `CareShield: PROBE ...` 日志，便于真机确认服务监听与识别链路（解决"碰运气遇广告"难以验证的问题）。

### 9.7 真机排查结论
- 确认无 root 设备无法静默开启无障碍（Android 安全边界）；适配为"监护人配一次、系统记忆、老人零操作"
- 拦截日志存于应用私有 DataStore，**本机本地、跨设备隔离**

---

## 十、AdGuard 学习 + DNS 拦截 + 统计页（2026-08-20）

### 10.1 AdGuard 解析与 DNS 拦截（B 路线）
- 解析 AdGuard v4.13.1 APK：提取其过滤规则库（Adblock Plus 语法 `||域名^`），提取 19334 个广告域名
- 实现 `DnsBlocker`：内置 3500 个广告域名（res/raw/ad_hosts.txt），经 Shizuku 写入 /etc/hosts → DNS 层拦截广告域名
- 带标记段（# CareShield-AD-BLOCK-START/END）、备份恢复、enable/disable 开关

### 10.2 日志页 → 统计页（照 HTML + 主题适配）
- 3 项统计（广告/捆绑软件/请求）+ 分段进度条（橙/黄/灰）
- 时段弹窗（全时段/24h/7天/30天）
- 三点菜单：清除统计 / 导出日志(.log)
- 应用程序列表（图标 + 名称 + 拦截数 + 查看所有）
- 主题适配：跟随深浅色（非固定黑）
- 真实数据：getInterceptCounts + 新增 getAppInterceptStats（按 App 分组）

### 10.3 应用列表图标
PackageManager 加载各应用图标显示。

### 10.4 开关布局调整（用户要求）
- 首页（功能页）保留完整防护开关
- 设置页只保留：激进跳过 / 滑屏跳过信息流 / 拦截网页广告跳转

### 10.5 激进跳过（可选项）
- func_aggressive_skip 开关：全屏广告 + 角落倒计时自动点，默认关，避免误触

---

## 十一、DNS 过滤 VPN + 自干扰修复（2026-08-20）

### 11.1 视频 App 广告拦截挑战
测试发现腾讯/咪咕/乐视等 open 屏广告登录：
- 广告跳过按钮多为 webview/纯图片，**无障碍读不到** → 点跳过无效
- /system hosts 只读无法写；ndc 改 DNS 不被识别

### 11.2 解决方案：DnsVpnService（本机 DNS 过滤 VPN）
- VpnService 建立本地 VPN，DNS 指向广告过滤 DNS（AdGuard 94.140.14.14/15.15）
- 所有 App DNS 解析走广告过滤 → 广告域名解析 0.0.0.0 → 广告不加载（源头条拦）
- 无需 root、前台服务常驻、可逆开关
- 设置页「DNS 广告过滤」开关接入

### 11.3 自干扰 bug 修复（关键）
- 发现 CareShield 曾在 handleAd 里没排除自己，反复拦截/点击自家界面（日志全是 com.careshield.app）
- 修复：handleAd 首行排除 `com.careshield.app`

### 11.4 版本管理
- 之后每次打包：更新 版本号 + About 更新内容 + CHANGELOG + README + PROJECT-RECORD

---

*文档更新时间：2026-08-20*

### 11.5 DNS VPN 闪退修复
- Android 14: VpnService 前台服务需 android:foregroundServiceType="vpn" + startForeground 传 FOREGROUND_SERVICE_TYPE_VPN，否则闪退
- Manifest 加 foregroundServiceType; DnsVpnService 按 API 传 type
