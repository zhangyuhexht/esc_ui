package com.example.escui.config;

import java.util.List;

/**
 * Text shown by a show-rules entry, parsed from menu.yml.
 *
 * <p>The click is handled server-side: a dialog is built on the spot, with one
 * line of {@code lines} per paragraph.</p>
 *
 * @param title dialog title
 * @param lines text lines, may contain &amp; colour codes
 */
public record RulesDefinition(String title, List<String> lines) {
}
