package com.example.escui.config;

import java.util.List;

/**
 * 「服务器规则」条目点开显示的内容（从 menu.yml 的 rules 里解析而来）。
 *
 * <p>菜单里那个条目的点击由服务端处理，现场构建一个对话框，
 * 按顺序显示 {@code lines} 里的每一行文字。</p>
 *
 * @param title 对话框标题
 * @param lines 规则内容，一行一段（支持 &amp; 颜色码）
 */
public record RulesDefinition(String title, List<String> lines) {
}
