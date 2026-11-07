# CareShield 接管与开发流程说明

> 接管日期：2026-11-07。本文记录仓库怎么来的、怎么用、接下来按什么规则开发。

## 一、仓库是怎么来的

接管前 `D:\CareShield-project` 下堆着 79 个由 AI 编码工具自动导出的工程快照文件夹
（`CareShield-project_1` … `_55`、`CareShield-project-v1.3.0` … `v1.6.0` 等，
每个 60–150 MB，含 `.gradle`、`.idea`、`build` 等构建产物，合计约 8.6 GB），
彼此仅有少量源码差异，没有任何版本控制。

现在这些快照的**源码**（不含构建产物）已按时间顺序重建成 git 历史：
每个快照一次提交，提交时间取快照导出时间，版本里程碑打 tag。
当前 `main` 的最新提交即 v1.6.0 的源码，与快照逐文件校验一致（50 个文件，哈希全等）。

原始快照 zip 保留在 `../archive/snapshots/`，万一需要考古可以直接解开对照。

## 二、目录结构

```
D:\CareShield-project\
├── careshield-app\         主工程（git 仓库，唯一开发目录）
│   ├── app\                Android 模块
│   ├── gradle\  gradlew*   Gradle Wrapper（8.9，接管时补齐，见第五节）
│   ├── docs\               交接/流程类文档
│   ├── PROJECT-RECORD.md   项目开发全记录（需求→决策→迭代→复盘）
│   ├── CHANGELOG.md        逐版本更新日志
│   └── WORKBUDDY-HANDOFF.md 一次跨工具交接留下的排查记录（历史材料）
├── archive\
│   ├── snapshots\          81 个历史快照 zip（源码，约 9 MB）
│   ├── apk\                三个历史 debug APK（v1.3.0-clean43 / snap51 / v1.4.1）
│   └── extras\             早期仓库副本、workbuddy 交接副本、DemoRogueApp 演示工程
├── materials\              项目周边材料（AI 视频、PPT、引领守护计划资料、液态玻璃参考工程）
└── _tools\                 一次性脚本（历史重建脚本等）
```

## 三、版本管理约定（从现在起）

1. **只在这一个目录开发**：`careshield-app`。不要再复制整份工程做快照，
   历史由 git 承担，`git log` / `git diff` / `git tag` 就是快照。
2. **分支**：`main` 为可编译主线。试验性改动（例如再碰视频 App 拦截）开短分支，
   验证失败直接丢弃，避免重演 v1.3.x → clean43 那次全量回退。
3. **提交**：一次提交只做一件事；说明写清「改了什么 + 为什么 + 真机结果」。
   涉及拦截逻辑的改动必须写清楚是否影响中羽/有道/咪咕（43 版基准）。
4. **发版**：能装的版本打 tag `vX.Y.Z`，同时更新 `app/build.gradle.kts` 的
   `versionCode` / `versionName`、`CHANGELOG.md`、`README.md`、`PROJECT-RECORD.md`。
   `versionCode` 沿用历史习惯（60、61…）。
5. **构建产物不入库**：`.gitignore` 已排除 `build/`、`.gradle/`、`.idea/`、
   `local.properties`。要留 APK 就放 `../archive/apk/`。

## 四、怎么构建

本机（无网环境也适用）：

```powershell
cd D:\CareShield-project\careshield-app
.\gradlew.bat assembleDebug
# 产物：app\build\outputs\apk\debug\app-debug.apk
```

Android Studio 打开 `careshield-app` 根目录（含 `settings.gradle.kts`）即可，
首次同步会下载依赖。`local.properties` 里的 `sdk.dir` 由 Android Studio 自动生成。

## 五、接管时处理过的问题

- **Gradle Wrapper 缺失**：历史快照导出时丢掉了 `gradlew`、`gradlew.bat`、
  `gradle-wrapper.jar`。已用本机 Gradle 8.9 重新生成并入库，distributionUrl 仍指向 8.9。
- **GitHub 远端不可达**：本机直连 github.com 超时、无可用代理，无法拉取
  `DerekHemcis/CareShield-project` 或 `CareShield-apk` 远端历史，也未配置 remote。
  网络恢复后如需同步，执行 `git remote add origin <url>` 再推送即可。
- **文件权限**：`D:\CareShield-project` 根目录原来缺少当前用户的写权限与所有权，
  导致工具无法建工作区；已修复（回滚脚本在 `D:\dsh-acl-recovery\`）。

## 六、当前状态与未决问题（以 v1.6.0 为准）

已实现：无障碍开屏广告跳过（文字/倒计时/坐标）、防误触下载拦截、
AdGuard 域名库 + 可选的 DNS/VPN 思路（已移出主路径）、防流氓软件本地词库 + 主动扫描 + Shizuku 卸载、
监护人密码面板/白名单、Shizuku 引导、简体/繁体/英文、深浅主题。

未决：
1. **腾讯视频 / 搜狐视频开屏广告**：广告在独立窗口、按钮为图片或 webview，
   坐标点击时机不稳，v1.4.1 尝试后仍未通过，v1.5.0 已回退到 43 版导航与拦截路径。
   再动这块请单开分支、单点验证，先在真机跑通再合回 `main`。
2. **设备侧限制**：ColorOS/realme 上无障碍服务被系统回收的问题见
   `WORKBUDDY-HANDOFF.md`，属系统保活策略，需靠电池白名单/自启动白名单缓解。
3. **规划功能**：拦截规则云端更新、使用时段限制、位置守护、签名与哈希白名单
   （见 `PROJECT-RECORD.md` 第八节）。
