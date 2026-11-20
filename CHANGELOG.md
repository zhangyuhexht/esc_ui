# Changelog

Version numbers come from `build.gradle.kts`; the jar is named after the version.

## 2.0.0 — 2026-11-01

Current release.

Moved the menu to a single registered dialog. During bootstrap the plugin registers one menu dialog and adds it to the ESC menu; opening it shows five URL buttons pointing at `zhangyuhexht.top`.

Rules used to be an in-game text dialog. They're a web page now, so the `rules` section was dropped from the default config. The `show-rules` code path is still in the plugin — add a `rules` section back and it works again, with edits taking effect on the next click.

Also in this release: dropped the old ServerLinks code, verified against Paper 26.2 and 26.3, and cleaned up the repo (editor and rule-page artifacts removed, JDK path no longer hardcoded).

## 1.1.0 and earlier — 2026-10

A lot of false starts: native ServerLinks, a Dialog page tree with inputs, a bundled web editor, a separate rules button in the ESC menu. All of it was replaced by the single-dialog design in 2.0.0. The code is gone; the reasoning behind those choices is in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Downloads: [Releases](https://github.com/zhangyuhexht/esc_ui/releases).
