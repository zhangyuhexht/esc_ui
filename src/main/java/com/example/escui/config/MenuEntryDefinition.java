package com.example.escui.config;

/**
 * 菜单里的一个条目（从 menu.yml 的 menu.entries 里解析而来）。
 *
 * <p>一个条目只做一件事：</p>
 * <ul>
 *   <li>{@code url} 非空 —— 点了用浏览器打开这个网址；</li>
 *   <li>{@code showRules} 为 true —— 点了在游戏内显示规则文本。</li>
 * </ul>
 *
 * @param label     按钮上显示的文字（可含 &amp; 颜色码）
 * @param url        要打开的网址（showRules 为 false 时必须有）
 * @param showRules 是否显示规则文本
 */
public record MenuEntryDefinition(String label, String url, boolean showRules) {
}
