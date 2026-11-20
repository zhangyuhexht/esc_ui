# EscUI: implementation notes

A short map of how this plugin fits together. Written against 2.0.0. When this file disagrees with the code, the code wins.

## What it does

One button in the ESC menu, one dialog behind it. The default config puts five URL buttons in that dialog:

```
ESC → Server Links
        ├─ Website    → zhangyuhexht.top/
        ├─ Docs       → zhangyuhexht.top/#/docs
        ├─ Slimefun   → zhangyuhexht.top/#/slime
        ├─ Rules      → zhangyuhexht.top/#/rules
        └─ Feedback   → zhangyuhexht.top/#/feedback
```

An entry can also use `show-rules: true` to open a text dialog built on the server. That was how rules worked before they moved to a web page. The default config doesn't use it, but the code is intact.

## Why Dialog and not ServerLinks

Paper's built-in ServerLinks only stores name + URL pairs. There's no way to put arbitrary text in there, and no way to build a menu with custom body content. Dialog gives you both.

The catch: a dialog has to be registered during the bootstrap phase to appear in the ESC menu. That's why menu edits need a restart, while `rules` text (built on demand) is hot.

## Boot sequence

```
bootstrap phase
  EscUIBootstrap.bootstrap()
    MenuConfig.load()                 read menu.yml, release default from jar if missing
    RegistryEvents.DIALOG            register menu dialog as escui:menu
    LifecycleEvents.TAGS.preFlatten  add it to DialogTagKeys.PAUSE_SCREEN_ADDITIONS

onEnable
  register PlayerCustomClickEvent listener
  build a rules dialog once as a sanity check

runtime
  URL entry  → client-side ClickEvent.openUrl, no server round trip
  rules entry → customClick(escui:open/rules) → EscUIPlugin re-reads menu.yml
                → Dialog.create() → player.showDialog()
```

The exact API calls:

```java
// register
RegistryEvents.DIALOG.compose().newHandler(event ->
    event.registry().register(DialogKeys.create(key), builder -> ...));

// hang it off the pause menu
LifecycleEvents.TAGS.preFlatten(RegistryKey.DIALOG).newHandler(event ->
    event.registrar().addToTag(DialogTagKeys.PAUSE_SCREEN_ADDITIONS,
        List.of(TagEntry.valueEntry(DialogKeys.create(key)))));

// build on click
Dialog.create(factory -> ...);
player.showDialog(dialog);
```

Bootstrap and the main plugin run in different class loaders, so `DialogBuilder` gets loaded twice. Keep it stateless.

## The classes

| Class | Job |
| --- | --- |
| `EscUIBootstrap` | Registers the menu dialog and adds it to the ESC tag. No valid entries → warning, no button. |
| `EscUIPlugin` | Handles `escui:open/rules` clicks only. Re-reads config on each click, which is what makes rules edits hot. |
| `DialogBuilder` | Turns config into dialogs. `applyMenu()` for the registered one, `buildRulesDialog()` for the runtime one. |
| `MenuConfig` | Parses `menu` and `rules`. Releases the bundled `menu.yml` on first boot. Returns an empty object on failure instead of throwing. |
| `MenuDefinition`, `MenuEntryDefinition`, `RulesDefinition` | Plain records. |

Two details worth knowing:

- `DialogType.multiAction` refuses an empty action list, so `DialogBuilder` adds a close button if no entries survived parsing.
- The dialogs are text and buttons only. No items are involved, which avoids the bootstrap-phase issue where the item registry isn't bound yet.

## config format

```yaml
menu:
  title: "服务器链接"       # dialog title, defaults to the same value
  button: "服务器链接"      # ESC button text, defaults to the same value
  columns: 1               # 1–4, clamped
  entries:
    - label: "服务器官网"    # required
      url: "https://zhangyuhexht.top/"

# optionally:
# rules:
#   title: "服务器规则"
#   lines:
#     - "one line per paragraph"
```

`url` takes precedence if an entry has both fields. A URL without a scheme is rejected. `rules.lines` empty means `rules()` returns null, so a `show-rules` entry will only log a warning when clicked.

Button labels are Chinese because that's what players on this server read. They're plain strings; swap them if you need another language.

## Build and test

```bash
./gradlew build
bash scripts/setup-testserver.sh
bash scripts/run-testserver.sh
```

`processResources` expands `${version}` into `paper-plugin.yml`. Compile target is Java 25 with UTF-8 and `-Xlint:deprecation`.

The test scripts are Windows Git Bash helpers. They download Paper, write an offline flat world on port 25599, install the jar, boot it, wait for `Done (`, then stop it. Exit 0 means clean. `PAPER_VERSION=26.3` switches versions.

Two things that bite: Paper refuses to start if two jars with the same plugin name are in `plugins/` (setup clears the old one), and you must redeploy after rebuilding or you'll test a stale jar.

Verified on Paper 26.3: EscUI 2.0.0 loads, registers five entries, `Done (8.576s)`, no errors.

## Known rough edges

- Menu structure needs a restart. Rules text doesn't.
- `build.gradle.kts` still pulls in PlaceholderAPI as `compileOnly`, but nothing uses it. Either implement placeholders or drop the dependency.
- No client-side test has been done here — only server logs. Actual rendering and link clicks need a real client.
- Minecraft's default font misses a lot of glyphs. Avoid ✔, ✘, → in dialog text.
- `paper-plugin.yml` still describes the plugin as having an in-game rules text. That's leftover wording from before rules moved to a web page; it's cosmetic.

## If you're extending it

- More page types: follow the `show-rules` pattern. `openKey(page)` already accepts any page name; add a branch in `EscUIPlugin.onCustomClick`.
- Hot reload for the menu: the registry can't be changed at runtime, so you'd need to register a minimal shell and build the real dialog on click. Costs an extra click.
- Hardcoding the config: replace `MenuConfig` with a constructed `MenuDefinition`. Small change if the admin panel isn't needed.
- Config validation: check for `show-rules` entries without a `rules` section at startup instead of warning on click.
