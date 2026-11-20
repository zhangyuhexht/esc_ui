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
 * Reads menu.yml.
 *
 * <p>The file has two sections:</p>
 * <ul>
 *   <li>{@code menu} — the button list behind the ESC button;</li>
 *   <li>{@code rules} — text shown by a show-rules entry.</li>
 * </ul>
 *
 * <p>If the file doesn't exist, a default copy is released from the jar.</p>
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
     * Loads the config.
     *
     * @param dataDir plugin data folder
     * @param warn    where to send warnings
     * @return parsed result; never null, but either field may be null
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
            warn.accept("Failed to read menu.yml: " + ex.getMessage());
        }
        return new MenuConfig(menu, rules);
    }

    // ------------------------------------------------------------ menu

    /** Returns null when there are no valid entries; callers skip registration. */
    private static MenuDefinition parseMenu(YamlConfiguration cfg, Consumer<String> warn) {
        List<MenuEntryDefinition> entries = new ArrayList<>();
        for (Map<?, ?> raw : cfg.getMapList("menu.entries")) {
            try {
                entries.add(parseEntry(raw));
            } catch (Exception ex) {
                warn.accept("Skipping a malformed menu entry: " + ex.getMessage());
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

    /** label is required; url and show-rules are mutually exclusive (url wins). */
    private static MenuEntryDefinition parseEntry(Map<?, ?> raw) {
        String label = str(raw, "label", "").trim();
        if (label.isEmpty()) {
            throw new IllegalArgumentException("missing label (button text)");
        }
        String url = str(raw, "url", "").trim();
        boolean showRules = Boolean.parseBoolean(str(raw, "show-rules", "false").trim());
        if (url.isEmpty() && !showRules) {
            throw new IllegalArgumentException("entry '" + label + "' has neither url nor show-rules");
        }
        if (!url.isEmpty()) {
            URI uri = URI.create(url); // throws on malformed URLs
            if (uri.getScheme() == null) {
                throw new IllegalArgumentException("URL '" + url + "' has no scheme (e.g. https://)");
            }
        }
        return new MenuEntryDefinition(label, url, showRules);
    }

    // ------------------------------------------------------------ rules

    /** Returns null if the section is missing or has no lines. */
    private static RulesDefinition parseRules(YamlConfiguration cfg) {
        List<String> lines = cfg.getStringList("rules.lines");
        if (lines.isEmpty()) {
            return null;
        }
        return new RulesDefinition(cfg.getString("rules.title", DEFAULT_RULES_TITLE), lines);
    }

    // ------------------------------------------------------------ accessors

    /** @return menu definition, or null if no valid entries */
    public MenuDefinition menu() {
        return menu;
    }

    /** @return rules definition, or null if not configured */
    public RulesDefinition rules() {
        return rules;
    }

    private static String str(Map<?, ?> raw, String key, String fallback) {
        Object value = raw.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}
