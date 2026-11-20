# EscUI

A small Paper plugin that adds a **Server Links** button to the ESC menu. Clicking it opens a single in-game dialog with five links to the server's site.

Runs on Paper 26.2 and 26.3. Built with Java 25. No runtime dependencies.

## What it looks like

```
ESC → Server Links
        ├─ Website
        ├─ Docs
        ├─ Slimefun Info
        ├─ Rules
        └─ Feedback
```

Clicking a link shows the client's usual "open this link?" prompt first. Those clicks never reach the server.

## Install

Put `EscUI-2.0.0.jar` in `plugins/` and restart. On first boot the plugin writes `plugins/EscUI/menu.yml` with the default config.

Menu changes require a restart. The dialog is registered during bootstrap — that is the only window Paper gives you to add something to the ESC menu.

## Configuration

Everything lives in `plugins/EscUI/menu.yml`:

```yaml
menu:
  title: "服务器链接"        # dialog title
  button: "服务器链接"       # ESC menu button label
  columns: 1                # 1–4
  entries:
    - label: "服务器官网"
      url: "https://zhangyuhexht.top/"
    - label: "服务器文档"
      url: "https://zhangyuhexht.top/#/docs"
    - label: "粘液科技信息"
      url: "https://zhangyuhexht.top/#/slime"
    - label: "服务器规则"
      url: "https://zhangyuhexht.top/#/rules"
    - label: "反馈/提议"
      url: "https://zhangyuhexht.top/#/feedback"
```

The `label` values are the actual button text players see, so they're kept in Chinese for this server. Change them to whatever language you need. They support `&` colour codes.

Two actions per entry:

- `url` — opens a web page. Must include a scheme such as `https://`.
- `show-rules: true` — shows text from a `rules` section instead. Not used by the default config, but the code is there. Unlike the menu, `rules` is re-read on every click, so edits apply without a restart.

A bad entry is skipped with a warning rather than taking down the plugin. If every entry fails, no ESC button is registered.

## Building

```bash
./gradlew build      # Windows: .\gradlew.bat build
```

Needs JDK 25. Output: `build/libs/EscUI-2.0.0.jar`. The version in `build.gradle.kts` is injected into `paper-plugin.yml` at build time.

## Layout

```
src/main/java/com/example/escui/
  EscUIBootstrap.java     registers the dialog and the ESC button
  EscUIPlugin.java        handles show-rules clicks
  DialogBuilder.java      config → dialog
  config/                 menu.yml model and parser
src/main/resources/
  paper-plugin.yml        bootstrapper lives here, not in plugin.yml
  menu.yml                default config
scripts/                  local Paper test server helpers
docs/ARCHITECTURE.md      implementation notes
```

## Testing locally

```bash
bash scripts/setup-testserver.sh
bash scripts/run-testserver.sh
```

The scripts download Paper 26.2, set up an offline flat world on port 25599, drop in the jar, boot it, and shut it down. `run-testserver.sh` exits 0 on a clean start. Use `PAPER_VERSION=26.3` with the first script to test 26.3 instead. These are Windows Git Bash scripts; they're not part of the plugin.

## Caveats

- Dialog text should stick to common characters. Minecraft's default font renders things like ✔ and → as boxes.
- Dialog is a newer Minecraft feature. Older clients can't fall back to a different UI here.
- If all you want is a plain list of URLs, Paper's built-in ServerLinks is less work than this.

More implementation detail in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
