package com.example.escui.config;

import java.util.List;

/**
 * The page behind the ESC "Server Links" button, parsed from menu.yml.
 *
 * @param title   dialog title
 * @param button  ESC menu button label
 * @param columns number of button columns (1–4)
 * @param entries buttons, in display order
 */
public record MenuDefinition(String title, String button, int columns, List<MenuEntryDefinition> entries) {
}
