# Xiangqi with JavaFX

一个使用 JavaFX 构建的中国象棋棋盘演示项目，采用 Gradle 进行构建管理。项目包含简化的界面，仅展示棋盘与初始棋子布局，适合作为学习 JavaFX Canvas 绘制与 Gradle 配置的入门示例。

## 环境要求

- JDK 21（或兼容 JavaFX 21 的其他 JDK 版本）
- Gradle 8.5 及以上（推荐通过 `gradle wrapper` 生成本地包装器，或使用已安装的 Gradle）

## 快速开始

```powershell
# Windows PowerShell
gradle run
```

```bash
# macOS / Linux
gradle run
```

首次运行会自动下载 JavaFX 依赖。若使用的是手动安装的 Gradle，请确保版本满足要求；
如果希望使用 Gradle Wrapper，可在本项目根目录执行 `gradle wrapper` 快速生成。

## 项目结构

```text
├─ build.gradle.kts         # Gradle Kotlin DSL 构建脚本
├─ settings.gradle.kts      # 根项目设置
├─ src                      # 源码与资源目录
│  ├─ main
│  │  ├─ java
│  │  │  └─ org/example     # JavaFX 应用与棋盘逻辑
│  │  └─ resources          # 额外资源（当前为空，可放置图标等）
│  └─ test
│     └─ java               # JUnit 5 测试
└─ README.md
```

## 主要类说明

- `org.example.XiangqiApp`：JavaFX 应用入口，负责搭建舞台与场景。
- `org.example.model.ChessBoard`：封装棋盘尺寸与初始棋子列表。
- `org.example.view.ChessBoardView`：基于 `Canvas` 绘制棋盘网格、河界与棋子。

## 测试

项目使用 JUnit 5，运行所有测试：

```powershell
gradle test
```

## 常见问题

- **字体显示为方块？** 说明系统缺少中文字体，可安装常见的中文字体或在 `ChessBoardView` 中修改 `Font`。
- **JavaFX 模块错误？** 请确认 JDK 版本 >= 21，且未使用不含 JavaFX 的精简 JRE。

欢迎基于此示例继续扩展，更完整的走棋规则、音效或网络对战都可逐步加入。
