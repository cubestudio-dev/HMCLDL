# HMCLDL - Hello Minecraft! Launcher Download Launcher

[![Java CI](https://github.com/cubestudio-dev/HMCLDL/actions/workflows/gradle.yml/badge.svg)](https://github.com/cubestudio-dev/HMCLDL/actions/workflows/gradle.yml)

**HMCLDL** 是一个专注于 **Minecraft 游戏下载** 的轻量工具，基于 [Hello Minecraft! Launcher (HMCL)](https://github.com/HMCL-dev/HMCL) 构建。

它只做一件事，并把它做好：**下载 Minecraft 游戏本体与各类加载器**。

## 功能

- **下载游戏**：浏览并下载 Minecraft 正式版 / 快照版本
- **加载器安装**：安装 Forge、NeoForge、Fabric、Quilt、OptiFine、LiteLoader 等组件
- **下载器设置**
  - 下载源设置（官方源 / 镜像源 / 自动选择）
  - 游戏下载路径设置
  - 版本隔离设置
  - 外观设置（浅色 / 深色 / 跟随系统，支持自定义主题色）
  - 缓存目录与下载线程数
  - 代理设置
- **关于**：版权声明

## 下载

前往 [Releases](https://github.com/cubestudio-dev/HMCLDL/releases) 页面获取最新版本：

| 文件 | 说明 |
| --- | --- |
| `HMCLDL-x.y.z.exe` | Windows 启动器 |
| `HMCLDL-x.y.z.jar` | 跨平台 JAR（需 Java 17+） |
| `HMCLDL-x.y.z.sh` | Linux/macOS 启动脚本 |
| `HMCLDL-x.y.z.deb` | Debian/Ubuntu 安装包 |

## 运行要求

- Java 17 或更高版本（推荐 Java 21）

## 构建

```bash
./gradlew build
```

构建产物位于 `HMCL/build/libs/`。

## 版权声明

HMCLDL 是自由软件，基于 [Hello Minecraft! Launcher](https://github.com/HMCL-dev/HMCL) 修改，遵循 [GPL-3.0](LICENSE) 许可证发布。

```
Hello Minecraft! Launcher
Copyright (C) 2013-2026 huangyuhui <huanghongxun2008@126.com> and contributors

HMCLDL
Copyright (C) 2026 HMCLDL contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

---

# HMCLDL - Hello Minecraft! Launcher Download Launcher (English)

**HMCLDL** is a lightweight tool focused on **downloading Minecraft games**, built on top of [Hello Minecraft! Launcher (HMCL)](https://github.com/HMCL-dev/HMCL).

It does one thing and does it well: **downloading Minecraft itself and its mod loaders**.

## Features

- **Download Game**: Browse and download Minecraft release / snapshot versions
- **Mod Loader Installation**: Install Forge, NeoForge, Fabric, Quilt, OptiFine, LiteLoader and more
- **Downloader Settings**: download sources, game download path, version isolation, appearance (light/dark/follow system with custom theme color), cache directory, download threads, proxy
- **About**: copyright notice

## Requirements

- Java 17 or later (Java 21 recommended)

## License

HMCLDL is free software, modified from [Hello Minecraft! Launcher](https://github.com/HMCL-dev/HMCL), licensed under [GPL-3.0](LICENSE).

## CI 说明 / CI Note

本仓库的 GitHub Actions 工作流配置位于 [`docs/gradle-workflow.yml`](docs/gradle-workflow.yml)
（因上传凭证未包含 `workflow` 权限，无法直接推送 `.github/workflows/` 文件）。

如需启用 CI：在 GitHub 网页中新建 `.github/workflows/gradle.yml`，将
`docs/gradle-workflow.yml` 的内容复制进去即可。

The GitHub Actions workflow config is kept at [`docs/gradle-workflow.yml`](docs/gradle-workflow.yml).
To enable CI, create `.github/workflows/gradle.yml` in the GitHub web editor and paste its content.
