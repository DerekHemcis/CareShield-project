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

### 11.6 DNS VPN 闪退修复（运行时）
- 编译后运行：VpnService.prepare 在 VPN 已激活时抛 SecurityException → 捕获
- isVpnRunning 重写（/sys/class/net tun 检测，避免 /proc 崩）
- DnsVpnService 全 try-catch + establish null 检测

### 11.7 AdGuard 官方域名库接入
- ad_hosts.txt 从 3500 条扩充到 20194 条（AdGuardSDNSFilter 全量域名，532KB）
- 覆盖咪咕/腾讯/搜狐/乐视等广告域名命中率提升

---

## 十二、无障碍拦截回归 + 视频 App 坐标点击修复（2026-08-21）

> 版本 v1.3.0（versionCode 33）。本轮的教训：给成熟可用的无障碍服务加功能，必须严格隔离到具体 App，绝不能改动公共拦截路径。

### 12.1 v52 失效根因定位
对比 43 / 52 两版工程，发现 52 版拦截失效三大根因：
1. **accessibility_service_config.xml 删掉了 `flagIncludeNotImportantViews` 和 `canPerformGestures`** → 看不到"不重要"的广告按钮节点、无法手势 → 拦不住（最根本）。43 版带这两个 flag 所以正常。
2. **设置页删掉了「自动跳过广告」`func_skip_ad` /「自动关闭弹窗」`func_close_pop` 开关** → 用户无法开启核心拦截。
3. **manifest 增加 DnsVpnService + MiniA11y 白板**，多余进程/干扰。

### 12.2 v30 的教训：过度改动导致回归
- 首次尝试曾**改动公共拦截路径**，导致原本能拦的有道/中羽/咪咕全部失效。
- 结论：**必须完全以 43 版为基座**，常规方法（handleAd/scoreNode/detectAdContext/isCornerNode/findClickableAncestor 等）一字不改。

### 12.3 v32 隔离修复（恢复中羽/有道/咪咕）
- 常规 App 点击路径完全回归 43 版；仅在命中视频 App 时额外处理。
- 用户实测：**中羽在线、有道词典、咪咕视频恢复正常拦截。**

### 12.4 v33 视频 App 坐标跳过（针对腾讯/搜狐）
- 新增 `VIDEO_SKIP_COORDS` 校准坐标表（key=包名，value=跳过按钮中心坐标），实测腾讯 `(982,177)`。
- `coordinateTapOnNode`：优先点「跳过」文字节点 bounds 中心，无效才用校准坐标；连续点 3 次。
- **关键升级：优先走 Shizuku root `input tap`**（ColorOS 上 `dispatchGesture` 常无法真正注入触摸，腾讯视频 dispatchGesture 实测跳过无效；root `input tap` 是系统级注入，一定生效）。Shizuku 未授权时才退回 dispatchGesture。
- 视频 App 命中判断改为 `VIDEO_SKIP_COORDS.containsKey(pkg)`，坐标点击分支记录日志（带前台应用包名，修复此前日志全是 `com.careshield.app` 的混淆）。

### 12.5 真机验证洞察
- 腾讯视频开屏「跳过」节点可 dump：`Text=跳过 clickable=true bounds=[916,135][1048,219]`（右上角）。
- 用 root `input tap 982 177` 实测：界面切换且 topResumed 变 `H5Activity` → **点到了广告跳转区进了 H5 推广页**，说明时机/位置需校准，避免误触广告。
- 结论：坐标点击可行但**必须点「跳过」文字节点中心，且选对时机**（跳过按钮出现后再点），否则会误进广告详情页（H5）。

### 12.6 日志修复
- `handleAd` 首行排除 `com.careshield.app`（避免把自家界面当广告拦截，日志不再全是 careshield）。
- 坐标点击分支输出 `前台应用 <pkg> · 跳过`，方便确认在哪个 App 拦截。

### 12.7 版本升级
- build.gradle `versionCode=33, versionName=1.3.0`；About 页全部改为 v1.3.0。

---

## 十三、液体玻璃导航栏（集成 workbuddy Compose 版，2026-08-21）

> 版本 v1.4.0（versionCode 40）。UI 层重做底部导航，拦截逻辑零改动。

### 13.1 背景
- 此前自绘的 GlassNavigation 效果一般；用户带来 workbuddy 从 Flutter `ZyadWKhedr/LiquidGlass-NavBar`
  转写的 Compose 版，要求集成。
- 要求：点击「液体玻璃效果」开关后**立即生效**（响应式），无需重启。

### 13.2 集成内容
- 引入 `LiquidGlassNavBar.kt`（workbuddy 转写，包改为 `com.careshield.app.ui.nav`）：
  - 磨砂玻璃栏：半透明白渐变 + 圆角 + 阴影 + 白色描边
  - **可水平拖拽、自动吸附最近图标的玻璃药丸指示器**（对应 Flutter 的 NavbarDraggableIndicator）
  - 跟随指示器的镜面高光（radial gradient）
  - 指示器宽度自适应 `70 * (3.5/count).coerceIn(1,1.2)`，4 项→70dp
  - 选中图标放大 + 颜色高亮
- 删除旧的自绘 `GlassNavigation.kt`
- `MainShell.kt` 改用 `LiquidGlassNavBar`，保留 AnimatedContent 淡入淡出 + 水平滑动切 tab

### 13.3 液体玻璃开关即时生效（响应式）
- `SettingsRepo` 新增 `observeEnabled(key): Flow<Boolean>`（`dataStore.data.map{...}`）
- `MainShell` 用 `repo.observeEnabled("liquid_glass").collectAsState(true)` 订阅
  → 设置页切换开关时，导航栏立即在「玻璃质感 / 纯色」间切换，无需重启
- `LiquidGlassNavBar` 增加 `useGlass` 参数：true 玻璃渐变背景 / false 纯 surface

### 13.4 兼容保证
- 拦截逻辑完全未动（AdBlockerService md5 与 43 一致），中羽/有道/咪咕不受影响

---

## 十四、视频 App 拦截尝试 与 导航回退（2026-08-21，v1.4.1 / v1.5.0）

> 这是针对"老人常用视频 App（腾讯/搜狐）也能自动跳过广告"这条难路的探索与阶段性收束。

### 14.1 v1.4.1：视频 App 开屏广告拦截尝试
- **目标**：有道/中羽不是老人常刷的；老人更常看 腾讯视频、搜狐视频 等。
- **新增 `handleVideoAdIfApplicable`**（完全隔离，不影响常规拦截）：
  - 仅当 packageName ∈ 视频 App 名单（腾讯 qqlive / 搜狐 sohuvideo / 爱奇艺 qiyi /
    优酷 youku / 乐视 letv）时触发。
  - **遍历所有交互窗口（`windows`）** 找"跳过/倒计时"广告节点——因为这些开屏广告渲染在
    **独立窗口**（SplashHomeActivity 叠加层），`rootInActiveWindow` 扫不到，这是无障碍拦不住
    腾讯/搜狐的直接原因。
  - 命中后用 **Shizuku root `input tap` 真实坐标点击** 跳过按钮（ColorOS 上 `dispatchGesture`
    注入不可靠）；Shizuku 未授权时退回手势。
- **实测结果**：腾讯视频、搜狐视频仍然没有跳过。

### 14.2 结论：腾讯/搜狐开屏广告为何难拦
- 大厂视频 App 开屏广告多为 **App 内私有渲染 + 图片/webview"跳过"按钮**，无障碍节点不稳定，
  且**坐标点击受广告出现时机/真实点击区域**影响大。
- **DNS 拦截也走不通**：`/system hosts` 只读、`ndc 改 DNS` 不被该系统识别，且开屏广告素材
  不走可过滤的公共广告域名 → 腾讯/搜狐无法从 DNS 源头拦截。
- **阶段结论**：在 ColorOS 上，腾讯/搜狐开屏广告暂无法稳定自动跳过。产品如实呈现"能拦
  中羽/有道/咪咕"，腾讯/搜狐暂不支持，不建议再盲目攻坚。

### 14.3 v1.5.0：导航栏回退到 43 稳定版 + 移除液体玻璃开关
- **导航栏回退**：移除 workbuddy 转写的 `LiquidGlassNavBar.kt`（可拖拽药丸 + 镜面高光），
  恢复 43clean 的 `GlassNavigation.kt`（Material3 标准导航栏）。
- **删除液体玻璃开关**：
  - 设置页移除「液态玻璃效果」开关；
  - `MainShell` 固定玻璃样式（`liquidGlass = true`），不再读任何开关；
  - `SettingsRepo.observeEnabled`（临时响应式封装）一并移除，清掉无用 `map` import。
  - 残留：`SettingsRepo.resetDefaults` 里 `liquid_glass` 键仍在（无害兜底）；`LiquidGlass.kt`
    及 AGSL shader 资源保留在工程（技术资产，未启用）。
- **README 同步**：工程结构里 `LiquidGlassNavBar.kt` 引用改回 `GlassNavigation.kt`。
- **隔离保证**：`AdBlockerService` 的 `handleAd` 及全部常规方法与 43 完全一致，
  中羽/有道/咪咕不受影响。

### 14.4 版本
- v1.4.1（versionCode 41）→ v1.5.0（versionCode 50）；About 页同步；CHANGELOG 已更新至 v1.5.0。

---

## 十五、防流氓软件：一键扫描手机（2026-08-21，v1.6.0）

> 方案 A。把"被动等广播"改成"主动扫描"，可靠解决防装。

### 15.1 为什么原防装不生效（实测结论）
- `RogueWatchdog` 靠监听 `ACTION_PACKAGE_ADDED` 广播触发 → 安卓 8.0+ 后台收不到该隐式广播
  （除非进程在前台/被点开）→ 装流氓软件时几乎不触发。
- 实测日志：装赌博软件时只有"防误触: 拦截 下载页面"（按返回），**没有任何"流氓"拦截记录** →
  证实不是规则没命中，而是广播根本没到。
- 顺带发现自干扰 bug：`handleAd` 未排除自己 → 日志大量 "拦截 com.careshield.app · 跳过广告"。

### 15.2 新增 RogueScanner（主动扫描，方案 A）
- 遍历 `getInstalledApplications`，用 `RogueClassifier.classify(pkg, label)` 判定，
  命中 `BLOCK`（色情/赌博/贷款/私彩等）则列为"高风险"。
- 跳过 CareShield 自身、Shizuku、系统关键应用（防误卸）。
- **不依赖后台广播**，用户点击才扫描，可靠、可控、可演示。

### 15.3 首页新增「一键扫描手机」
- 按钮点击 → 后台扫描 → 列出高风险 App（名字 + 包名 + 命中原因）
- 每个条目可点"移除" → `ShizukuManager.execShell("pm uninstall --user 0 <pkg>")` 一键卸载
- 这是现场演示防装的最佳方式（装一个"极速放款"样机 → 扫到 → 移除）。

### 15.4 修复自干扰
- `handleAd` 首行 `if (pkg == "com.careshield.app") return`，不再自拦自记。

### 15.5 版本
- v1.6.0（versionCode 60）；About/CHANGELOG/README 同步。

---

*文档更新时间：2026-08-21*
