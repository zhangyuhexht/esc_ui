# EscUI

> 给 Paper 服务器的 ESC 暂停菜单加一个「服务器链接」按钮，点开是一页可配置的网页入口菜单。

EscUI 是一个轻量 Paper 服务端插件，基于 Minecraft 的 **Dialog（对话框）** 机制：插件在启动引导阶段注册一个菜单页对话框并挂进 `PAUSE_SCREEN_ADDITIONS` 标签，玩家按 ESC 就能看到「服务器链接」按钮。菜单里的条目由 `menu.yml` 配置，支持**跳转网页**和**显示游戏内文本**两种动作。

当前默认配置提供 5 个网页入口：

| 按钮 | 目标网址 |
| --- | --- |
| 服务器官网 | `https://zhangyuhexht.top/` |
| 服务器文档 | `https://zhangyuhexht.top/#/docs` |
| 粘液科技信息 | `https://zhangyuhexht.top/#/slime` |
| 服务器规则 | `https://zhangyuhexht.top/#/rules` |
| 反馈/提议 | `https://zhangyuhexht.top/#/feedback` |

## 运行效果

```
玩家按 ESC
   └─ 点「服务器链接」（插件注册的按钮，文字可配）
        └─ 一页游戏内菜单（Dialog）：
             ├─ 服务器官网    → 浏览器打开网页
             ├─ 服务器文档    → 浏览器打开网页
             ├─ 粘液科技信息  → 浏览器打开网页
             ├─ 服务器规则    → 浏览器打开网页
             └─ 反馈/提议     → 浏览器打开网页
```

点网址条目时客户端会先弹「是否打开此链接」确认框，确认后才用浏览器打开。

## 环境要求

| 项目 | 要求 |
| --- | --- |
| 服务端 | Paper **26.2 / 26.3**（插件 `api-version: 26.2`，两代均验证通过） |
| 客户端 | 需支持 Dialog 的客户端（Dialog 为 1.21.6+ 引入的原生特性），与服务端同版本 |
| 编译环境 | JDK **25**（`options.release = 25`） |
| 构建工具 | Gradle 9.x（Kotlin DSL，仓库自带 wrapper） |

插件没有运行时依赖：Paper API 与 PlaceholderAPI 都是 `compileOnly`，不会打进 jar，也不需要额外安装前置插件。

## 安装

1. 到 [Releases](https://github.com/zhangyuhexht/esc_ui/releases) 下载 `EscUI-2.0.0.jar`；
2. 放进服务端的 `plugins/` 目录；
3. 重启服务端（首次启动会释放一份默认配置到 `plugins/EscUI/menu.yml`）；
4. 玩家按 ESC，即可看到「服务器链接」按钮。

## 配置

配置文件位置：`plugins/EscUI/menu.yml`（仓库里的 `src/main/resources/menu.yml` 是默认模板）。

```yaml
menu:
  title: "服务器链接"          # 点开后对话框的标题
  button: "服务器链接"         # ESC 菜单里按钮显示的文字
  columns: 1                  # 按钮排几列（1~4）
  entries:                    # 菜单条目，按此顺序排列
    - label: "服务器官网"       # 按钮文字，支持 & 颜色码
      url: "https://zhangyuhexht.top/"        # 动作一：用浏览器打开网址
    - label: "服务器文档"
      url: "https://zhangyuhexht.top/#/docs"
    - label: "粘液科技信息"
      url: "https://zhangyuhexht.top/#/slime"
    - label: "服务器规则"
      url: "https://zhangyuhexht.top/#/rules"
    - label: "反馈/提议"
      url: "https://zhangyuhexht.top/#/feedback"
```

| 字段 | 说明 |
| --- | --- |
| `menu.title` / `menu.button` | 对话框标题 / ESC 按钮文字，缺省均为「服务器链接」 |
| `menu.columns` | 按钮列数，1~4，缺省 1 |
| `entries[].label` | 必填，按钮文字，支持 `&a` `&c` `&6` `&f` `&l` 等格式码 |
| `entries[].url` | 动作一：打开网页，必须带 `https://` 等协议头 |
| `entries[].show-rules` | 动作二：填 `true` 时显示 `rules` 段里的游戏内文本（与 `url` 二选一，同时存在时 `url` 优先） |
| `rules.title` / `rules.lines` | 可选。规则文本对话框的标题与内容，一行一段，`""` 表示空行 |

> 当前默认配置里 5 个条目**全部使用 `url`**，`rules` 段未启用；但代码完整支持 `show-rules`，需要游戏内文本时把它加回来即可。单条配置写错只会被跳过并打一条警告，不会导致插件启动失败。

### 配置生效时机

| 改了什么 | 怎么生效 |
| --- | --- |
| `menu`（条目增减、改名、网址、列数、按钮文字） | **重启服务端**（菜单页在启动引导阶段注册进注册表） |
| `rules`（游戏内文本，如启用） | **不用重启**，玩家每次点击时重新读配置并构建对话框 |

## 从源码构建

```bash
# 需要 JDK 25，建议用 JAVA_HOME 指到 JDK 25
./gradlew build          # Windows: .\gradlew.bat build
```

产物：`build/libs/EscUI-2.0.0.jar`。构建脚本会把 `build.gradle.kts` 里的版本号注入 `paper-plugin.yml` 的 `${version}` 占位符。如果 Gradle 找不到 JDK 25，可在 `gradle.properties` 里取消注释并填写本机路径，或让 Gradle 自动探测/下载。

## 项目结构

```
esc_ui/
├── src/main/java/com/example/escui/
│   ├── EscUIBootstrap.java       # 引导阶段：注册菜单页对话框 + 挂 ESC 按钮
│   ├── EscUIPlugin.java          # 运行期：处理 show-rules 点击 + 构建自检
│   ├── DialogBuilder.java        # 配置 → 对话框（菜单页 / 规则页）
│   └── config/                   # menu.yml 的数据模型与解析
├── src/main/resources/
│   ├── paper-plugin.yml          # 插件描述（bootstrapper 必须写在这里）
│   └── menu.yml                  # 默认菜单配置
├── scripts/
│   ├── setup-testserver.sh       # 一键搭建本地 Paper 测试服并部署 jar
│   └── run-testserver.sh         # 启动 → 验证加载 → 自动关闭（PASS/FAIL）
├── docs/项目总览与迭代指南.md     # 架构与开发指南
└── CHANGELOG.md                  # 版本记录
```

## 本地测试

仓库附带两个脚本，可以起一个一次性 Paper 服验证插件能否干净加载（在 Windows Git Bash 下验证过）：

```bash
bash scripts/setup-testserver.sh   # 下载 Paper、写 eula/配置、放入构建产物
bash scripts/run-testserver.sh     # 启动 → 等 Done → 自动 stop → 输出 PASS/FAIL
```

默认 Paper 26.2，可用 `PAPER_VERSION=26.3 bash scripts/setup-testserver.sh` 切换；测试服默认端口 `25599`、离线模式、flat 世界，目录 `.tools/testserver` 不入库。

## 已知限制

- 菜单页结构改动需要重启服务端；只有 `rules` 文本是热生效的。
- 对话框文本避免使用生僻 Unicode 符号（如 ✔ ✘ →），Minecraft 默认字体可能显示成方框；用【】、· 等常见字符。
- Dialog 是较新版本的原生机制，旧版本客户端无法回退显示，需与服务端保持同版本。
- 原生「服务器链接」列表只能放「名字 + 网址」，因此本项目才自行注册 Dialog；若只需要纯网址列表，原生 ServerLinks 是更简单的方案。

更细的架构说明、API 调用链与迭代方向见 [docs/项目总览与迭代指南.md](docs/项目总览与迭代指南.md)。
