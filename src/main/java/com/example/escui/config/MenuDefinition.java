package com.example.escui.config;

import java.util.List;

/**
 * 「服务器链接」按钮点开后的那一页菜单（从 menu.yml 的 menu 里解析而来）。
 *
 * @param title   点开后的标题
 * @param button  ESC 菜单里按钮显示的文字
 * @param columns 按钮排几列（1~4）
 * @param entries 菜单里的条目，按顺序排
 */
public record MenuDefinition(String title, String button, int columns, List<MenuEntryDefinition> entries) {
}
