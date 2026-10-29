# FuckHyperOTG (小米澎湃常驻OTG)

[![Build FuckHyperOTG](https://github.com/YourUsername/FuckHyperOTG/actions/workflows/build.yml/badge.svg)](https://github.com/YourUsername/FuckHyperOTG/actions/workflows/build.yml)
[![LSPosed](https://img.shields.io/badge/LSPosed-Supported-brightgreen.svg)](https://github.com/LSPosed/LSPosed)
[![HyperOS](https://img.shields.io/badge/Xiaomi-HyperOS%201.0%20%7C%202.0-blue.svg)](https://www.mi.com)

**FuckHyperOTG** 是一个专门针对 **小米澎湃系统（Xiaomi HyperOS 1.0 / 2.0 及 MIUI 14）** 开发的现代 LSPosed / Xposed 模块。

该项目灵感来源于针对真我 ColorOS 系统的开源项目 [`com.w2016.fuckRealmeOTG`](https://github.com/Xposed-Modules-Repo/com.w2016.fuckRealmeOTG)。在小米澎湃系统中，系统为了省电引入了严格的 OTG 限制机制：**开启 OTG 后若在 10 分钟内无高频数据交互，系统会自动关闭 OTG 供电和连接**，同时部分机型还存在插入外设后开关置灰、隐藏 OTG 开关等问题。

**FuckHyperOTG** 通过多层系统级 Hook，彻底解除这些限制，实现 **常驻 OTG 供电与外设数据连接**！

---

## 💡 解决的核心痛点

1. **阻止 10 分钟自动关闭**：
   - 彻底拦截并废除 HyperOS 的 10 分钟空闲定时器（`600,000ms` 超时任务与 `AlarmManager` 唤醒任务）。
   - 外接声卡、解码耳扩（小尾巴）、游戏手柄、散热背夹、USB 采集卡、随身 SSD 硬盘不会再因“短时间无数据传输”被系统强行断电。
2. **强制显示 OTG 设置开关**：
   - 在部分平板或特定 HyperOS 机型中，OTG 开关被系统默认隐藏；
   - 某些情况下插上外接线后开关会呈现**置灰无法点击**状态。本模块强制激活开关并解除置灰。
3. **保持 Type-C 接口 USB Host VBUS 供电常开**：
   - 阻止底层电源管理把 USB 角色降级或关闭对外输出供电。
4. **控制中心快捷磁贴 (Quick Settings Tile)**：
   - 提供可在下拉控制中心直接添加的“常驻OTG”磁贴，无需每次繁琐翻找“设置 -> 更多设置 -> OTG”。
5. **Root 极速应急通道 (可选)**：
   - 如果手机已获取 Root 权限（Magisk / KernelSU / APatch），可在 App 内直接一键强制写入底层的 Linux sysfs OTG 节点。

---

## 🛠️ Hook 架构与原理

本模块采用系统级多层拦截策略，全面兼容 HyperOS 1.0 (Android 14) 与 HyperOS 2.0 (Android 15)：

| 目标作用域 (Package) | Hook 目标类 / 方法 | 作用说明 |
| :--- | :--- | :--- |
| **`com.android.settings`** (系统设置) | `*OtgPreferenceController*`, `OtherPersonalSettings`, `postDelayed` | 强制返回 `AVAILABLE`，阻止超时 Runnable 执行 `setChecked(false)`。 |
| **`android`** (系统框架 / system_server) | `UsbDeviceManager`, `UsbPortManager`, `AlarmManagerService`, `MiuiUsbService` | 拦截系统的 OTG 超时定时报警广播，保持 USB Host 供电与通信角色。 |
| **`com.android.providers.settings`** | `Settings.Global.putInt`, `Settings.System.putInt` | 拦截将 `otg_switch` 写入为 `0` 的超时写操作。 |
| **`com.fuckhyperotg`** (模块本体) | `MainActivity.isModuleActivated()` | 模块自检与激活状态回传。 |

---

## 📱 安装与使用说明

### 1. 前置条件
- 已解锁 Bootloader 并获取 Root 权限（推荐 Magisk、KernelSU 或 APatch）。
- 已安装并正常激活 **LSPosed** 框架。

### 2. 激活步骤
1. 在 [Releases](../../releases) 页面下载最新版 `FuckHyperOTG-release.apk` 并安装。
2. 打开 **LSPosed 管理器**，进入“模块”列表，找到并勾选启用 **FuckHyperOTG**。
3. 确保模块推荐的**作用域**已正确勾选：
   - ✅ **系统界面 / 系统框架 (`android`)**
   - ✅ **系统设置 (`com.android.settings`)**
   - ✅ **系统界面 (`com.android.systemui`)**
4. 重启设备（或软重启）。
5. 打开 FuckHyperOTG 应用，界面顶部显示绿色“模块已激活”，即代表常驻 OTG 已生效！

---

## 🚀 GitHub Actions 自动编译指南

本项目已完整配置好 **GitHub Actions CI/CD** 工作流 (`.github/workflows/build.yml`)。

### 方式一：Fork 并直接在 GitHub 网页触发
1. 将本项目 Fork 到你的 GitHub 个人仓库。
2. 进入仓库页面的 **Actions** 标签栏。
3. 选择 **Build FuckHyperOTG** 工作流。
4. 点击右侧 **Run workflow** 按钮。
5. 等待 2~3 分钟构建完成，在构建日志底部的 **Artifacts** 区域即可直接下载编译生成的 `FuckHyperOTG-release.apk` 与 `FuckHyperOTG-debug.apk`！

### 方式二：发布 Release
当你为仓库推送新的版本 Tag 时（例如 `git tag v1.0.0 && git push origin v1.0.0`），GitHub Actions 会自动触发编译，并将生成的 APK 直接附加到 GitHub 官方 Releases 页面供所有人下载。

---

## 💻 本地编译

如果你想在本地通过 Android Studio 或命令行编译：

```bash
# 克隆仓库
git clone https://github.com/YourUsername/FuckHyperOTG.git
cd FuckHyperOTG

# 编译 Debug APK
./gradlew assembleDebug

# 编译 Release APK
./gradlew assembleRelease
```
编译产物位于 `app/build/outputs/apk/` 目录下。

---

## 📄 开源许可与致敬

- 参考项目：[com.w2016.fuckRealmeOTG](https://github.com/Xposed-Modules-Repo/com.w2016.fuckRealmeOTG) by [w2016561536](https://github.com/w2016561536)
- 协议：[Apache License 2.0](LICENSE)
