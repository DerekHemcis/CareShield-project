# 同类项目调研（2026-11-07）

目的：为 CareShield v1.6.0 之后的方向找可借鉴点。调研对象为 GitHub 上同类/相关开源项目。

## 一、核心同类

### 1. gkd-kit/gkd（44k★，GPL-3.0）
基于无障碍 + 高级选择器 + **订阅规则** 的自定义屏幕点击应用。
- 规则是声明式选择器（如 `[text*="跳过"][text.length<10][visibleToUser=true]`），按 App/界面分组，带 `matchTime`（启动 10 秒内才匹配）、`actionMaximum`（最多点 2 次防抖）等元数据。
- **应用本身不带规则**，规则靠订阅链接云端更新（官方订阅 + AIsouler/Lin-arm 等第三方订阅，合计 2 万+ 星的生态）。
- 配套 gkd-kit/inspect 快照审查工具，可在网页上审查真实界面快照再写规则。

→ 对应 CareShield 规划里的「拦截规则云端更新」。**建议**：规则数据格式直接参考/兼容 GKD 订阅格式，可坐享整个社区现成的「开屏广告」规则库；AdBlockerService 加一个从 JSON 订阅拉规则的能力即可，不需要重写引擎。

### 2. zfdang/Android-Touch-Helper（5.3k★，上架 F-Droid/Google Play）
无障碍跳开屏广告，三种点击方式：关键词 → 每App自定义控件 → 每App自定义屏幕坐标。可借鉴的具体实现：
- **复合关键词匹配**（matchesCompositeKeyword）：容器节点有 2~5 个子节点，把「跳过」和「5」这类拆散在两个子节点里的文本拼起来再匹配，且有尺寸约束（宽<70%屏、高<25%屏）。比 CareShield 现在"数字与跳过在同一按钮"的写法覆盖面更广（ CareShield 的 `scoreNode` 已有类似思路，可对照补强）。
- **点击升级链**：先 `performAction(ACTION_CLICK)`（连不可点击的节点也尝试）→ 失败找可点击祖先 → 再失败 `dispatchGesture` 坐标手势 → 记录每节点尝试次数（MAX_CLICK_ATTEMPTS 封顶）。注释里点出关键坑：**有些广告 SDK 对 ACTION_CLICK 返回 true 但实际不响应**，所以失败要重试为手势。这正是 CareShield v1.3.x 视频坐标点击踩过的坑。
- 规则匹配抽成**纯 Java 无 Android 依赖的类**（SkipAdRules），热路径每节点只取一次字符串，可 JVM 单测。
- 关键词默认只有 `["跳过"]`，极简；长度约束：节点文本最多比关键词多 6 个字符才匹配（防长句误中）。
- 安装引导文案明确写了三大件：无障碍开关 → **电池优化白名单** → 厂商后台耗电允许（VIVO 等）。与 WORKBUDDY-HANDOFF 里 ColorOS 杀后台的问题一致——这类引导值得抄进 ShizukuGuideScreen/首页。

### 3. madeye/ad-skipper（138★，本地 VLM 方案）
分层降级检测，**对 CareShield 最大的痛点（腾讯/搜狐图片式跳过按钮）最有启发**：
| 层 | 方式 | 延迟 |
|---|---|---|
| L1 | 无障碍文本匹配 | <10ms |
| L2 | ML Kit 本地中文 OCR（截屏识别） | ~100ms |
| L3 | YOLO11n 跳过按钮检测器（ncnn，~5MB） | ~20-300ms |
| L4 | 本地 VLM Grounding（llama.cpp） | 2-4s 兜底 |

→ PROJECT-RECORD 记录腾讯/搜狐失败原因是"图片/webview 按钮、无障碍扫不到"，**OCR 截屏层（L2）正好补这个洞**：无障碍树扫不到时对开屏截屏跑本地 OCR 找「跳过」二字再坐标点击，不用引入 YOLO/VLM 那么重。ML Kit OCR 是现成、离线、体积可控的。

### 4. 其他
- KonghuanSmart/SkipAds：无障碍跳广告的极简参考实现。
- BaldPhone（306★）、bjfwan/yinxing（老人桌面）、senioren-launcher：老年模式 Launcher——大图标、一键拨号、自动接听微信视频。属于另一条产品线（老人桌面），与广告拦截互补，暂不建议混进来。

## 二、可落地清单（按性价比排序）

1. **规则订阅化**：AdBlockerService 关键词/规则改为可从远程 JSON 拉取更新（格式参考 GKD 订阅），App 内规则库退化为兜底。——对应 PROJECT-RECORD 第八节规划。
2. **复合关键词 + 点击升级链**：把 TouchHelper 的两段实现思路移植进 AdBlockerService（保持 43 版基准逻辑不变的前提下加层，单分支验证）。
3. **每App自定义规则**：参考 TouchHelper 的「控件规则 + 坐标规则」界面，让监护人能针对某个 App 手动记录一次跳过位置——这正是解决腾讯/搜狐的现实路径（不追求全自动识别，靠一次人工标定）。
4. **截屏 OCR 层**：无障碍扫不到的 App 开截屏 + ML Kit OCR 找「跳过」。实验分支单点验证。
5. **保活引导补全**：把电池优化白名单、厂商后台自启的图形化引导加进 ShizukuGuideScreen（TouchHelper 文案可参考）。
6. **规则逻辑抽纯类 + 单测**：把 scoreNode/关键词匹配从 AdBlockerService 拆出为无 Android 依赖的类，加 JVM 单测（CI 已就绪，可直接跑）。
