package com.example.escui.config;

/**
 * One button in the menu, parsed from menu.yml.
 *
 * <p>Each entry does exactly one thing:</p>
 * <ul>
 *   <li>non-empty {@code url} — open that URL in a browser;</li>
 *   <li>{@code showRules} true — show the rules text in-game.</li>
 * </ul>
 *
 * @param label     button text, may contain &amp; colour codes
 * @param url       URL to open (required when showRules is false)
 * @param showRules whether to show the rules text instead
 */
public record MenuEntryDefinition(String label, String url, boolean showRules) {
}
