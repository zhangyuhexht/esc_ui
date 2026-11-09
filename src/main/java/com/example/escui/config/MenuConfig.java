package com.example.escui.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * menu.yml 的读取与解析。
 *
 * <p>一个文件里两部分内容：</p>
 * <ul>
 *   <li>{@code menu} —— ESC 菜单里「服务器链接」按钮点开后的那一页（按钮列表）；</li>
 *   <li>{@code rules} —— 「服务器规则」条目点开显示的文本。</li>
 * </ul>
 *
 * <p>文件不存在时会从插件 jar 里释放一份默认的出来。</p>
 */
public final class MenuConfig {

    private static final String DEFAULT_TITLE = "服务器链接";
    private static final String DEFAULT_BUTTON = "服务器链接";
    private static final String DEFAULT_RULES_TITLE = "服务器规则";

    private final MenuDefinition menu;
    private final RulesDefinition rules;

    private MenuConfig(MenuDefinition menu, RulesDefinition rules) {
        this.menu = menu;
        this.rules = rules;
    }

    /**
     * 读取配置。
     *
     * @param dataDir 插件数据目录
     * @param warn    出问题时往哪打警告
     * @return 解析结果（即使失败也会返回一个对象，可能是空的）
     */
    public static MenuConfig load(Path dataDir, Consumer<String> warn) {
        MenuDefinition menu = null;
        RulesDefinition rules = null;
        try {
            Files.createDirectories(dataDir);
            Path file = dataDir.resolve("menu.yml");
            if (!Files.exists(file)) {
                try (InputStream in = MenuConfig.class.getResourceAsStream("/menu.yml")) {
                    if (in != null) {
                        Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file.toFile());
            menu = parseMenu(cfg, warn);
            rules = parseRules(cfg);
        } catch (Exception ex) {
            warn.accept("读取 menu.yml 失败：" + ex.getMessage());
        }
        return new MenuConfig(menu, rules);
    }

    // ------------------------------------------------------------ menu

    /** menu 段没有配（或一个条目都没有）时返回 null —— 调用方据此跳过注册。 */
    private static MenuDefinition parseMenu(YamlConfiguration cfg, Consumer<String> warn) {
        List<MenuEntryDefinition> entries = new ArrayList<>();
        for (Map<?, ?> raw : cfg.getMapList("menu.entries")) {
            try {
                entries.add(parseEntry(raw));
            } catch (Exception ex) {
                warn.accept("有一个菜单条目解析失败，已跳过：" + ex.getMessage());
            }
        }
        if (entries.isEmpty()) {
            return null;
        }
        String title = cfg.getString("menu.title", DEFAULT_TITLE);
        String button = cfg.getString("menu.button", DEFAULT_BUTTON);
        int columns = Math.max(1, Math.min(4, cfg.getInt("menu.columns", 1)));
        return new MenuDefinition(title, button, columns, entries);
    }

    /** 一个条目：label 必须有；url 和 show-rules 二选一（都有的话 url 优先）。 */
    private static MenuEntryDefinition parseEntry(Map<?, ?> raw) {
        String label = str(raw, "label", "").trim();
        if (label.isEmpty()) {
            throw new IllegalArgumentException("缺少 label（按钮上显示的文字）");
        }
        String url = str(raw, "url", "").trim();
        boolean showRules = Boolean.parseBoolean(str(raw, "show-rules", "false").trim());
        if (url.isEmpty() && !showRules) {
            throw new IllegalArgumentException("条目「" + label + "」既没有 url 也没有 show-rules，不知道点了做什么");
        }
        if (!url.isEmpty()) {
            URI uri = URI.create(url); // 格式非法会在这里抛 IllegalArgumentException
            if (uri.getScheme() == null) {
                throw new IllegalArgumentException("网址「" + url + "」没有协议头（比如 https://）");
            }
        }
        return new MenuEntryDefinition(label, url, showRules);
    }

    // ------------------------------------------------------------ rules

    /** rules 段没有配（或 lines 为空）时返回 null。 */
    private static RulesDefinition parseRules(YamlConfiguration cfg) {
        List<String> lines = cfg.getStringList("rules.lines");
        if (lines.isEmpty()) {
            return null;
        }
        return new RulesDefinition(cfg.getString("rules.title", DEFAULT_RULES_TITLE), lines);
    }

    // ------------------------------------------------------------ 查询

    /** @return 菜单页；没有可用条目时返回 null */
    public MenuDefinition menu() {
        return menu;
    }

    /** @return 规则文本；没有配置时返回 null */
    public RulesDefinition rules() {
        return rules;
    }

    private static String str(Map<?, ?> raw, String key, String fallback) {
        Object value = raw.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}
