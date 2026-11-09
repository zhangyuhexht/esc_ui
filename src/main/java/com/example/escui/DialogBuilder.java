package com.example.escui;

import com.example.escui.config.MenuDefinition;
import com.example.escui.config.MenuEntryDefinition;
import com.example.escui.config.RulesDefinition;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * 把配置翻译成游戏内对话框。管两种对话框：
 *
 * <ul>
 *   <li><b>菜单页</b>（{@link #applyMenu}）：ESC 按钮点开的那一页，里面是一排按钮；</li>
 *   <li><b>规则页</b>（{@link #applyRules} / {@link #buildRulesDialog}）：
 *       「服务器规则」条目点开显示的文本，运行期现场构建。</li>
 * </ul>
 *
 * <p>两个阶段用法不同：菜单页在引导阶段注册进注册表（{@link #applyMenu}）；
 * 规则页在玩家点击时现场构建（{@link #buildRulesDialog} → {@code player.showDialog}）。
 * 两者都只含文字和按钮、不碰物品，所以引导阶段也没有「物品注册表未绑定」的问题。</p>
 *
 * <p>注意：引导类和主类跑在不同的类加载器里，这个类会被各自加载一份，
 * 所以它不能依赖任何静态共享状态。</p>
 */
public final class DialogBuilder {

    /** 所有对话框 key / 点击 key 的命名空间。 */
    public static final String KEY_NAMESPACE = "escui";
    /** 「打开页面」点击 key 的前缀。 */
    public static final String OPEN_PREFIX = "open/";
    /** 菜单页面对应的对话框 key。 */
    public static final Key MENU_KEY = Key.key(KEY_NAMESPACE, "menu");

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final int BUTTON_WIDTH = 200;

    private DialogBuilder() {
    }

    // ------------------------------------------------------------ 菜单页

    /** 填进注册表的 builder（引导阶段注册菜单页用）。 */
    public static void applyMenu(DialogRegistryEntry.Builder builder, MenuDefinition menu) {
        builder.base(buildMenuBase(menu)).type(buildMenuType(menu));
    }

    private static DialogBase buildMenuBase(MenuDefinition menu) {
        return DialogBase.builder(LEGACY.deserialize(menu.title()))
                .externalTitle(LEGACY.deserialize(menu.button()))
                .canCloseWithEscape(true)
                .body(List.of())
                .build();
    }

    private static DialogType buildMenuType(MenuDefinition menu) {
        List<ActionButton> buttons = new ArrayList<>();
        for (MenuEntryDefinition entry : menu.entries()) {
            buttons.add(ActionButton.create(
                    LEGACY.deserialize(entry.label()),
                    null,
                    BUTTON_WIDTH,
                    actionOf(entry)));
        }
        if (buttons.isEmpty()) {
            // Minecraft 要求对话框至少有一个按钮（"actions cannot be empty"）
            buttons.add(ActionButton.create(LEGACY.deserialize("&7关闭"), null, BUTTON_WIDTH, null));
        }
        return DialogType.multiAction(buttons, null, menu.columns());
    }

    /** 一个条目只做一件事：有 url 就开网址，否则（show-rules）交给服务端显示规则文本。 */
    private static DialogAction actionOf(MenuEntryDefinition entry) {
        if (!entry.url().isEmpty()) {
            return DialogAction.staticAction(ClickEvent.openUrl(entry.url()));
        }
        // 「显示规则」交给服务端处理（见 EscUIPlugin 的点击监听）：
        // 规则文本每次点开重新读，这样改 rules 配置不用重启服务端。
        return DialogAction.customClick(openKey("rules"), null);
    }

    // ------------------------------------------------------------ 规则页

    /** 现场构建一个规则对话框对象（运行期 showDialog 用）。 */
    public static Dialog buildRulesDialog(RulesDefinition rules) {
        return Dialog.create(factory -> applyRules(factory.empty(), rules));
    }

    /** 填进注册表的 builder（规则页不用注册，保留此入口以备不时之需）。 */
    public static void applyRules(DialogRegistryEntry.Builder builder, RulesDefinition rules) {
        builder.base(buildRulesBase(rules)).type(buildRulesType());
    }

    private static DialogBase buildRulesBase(RulesDefinition rules) {
        List<DialogBody> body = new ArrayList<>();
        for (String line : rules.lines()) {
            body.add(DialogBody.plainMessage(LEGACY.deserialize(line)));
        }
        return DialogBase.builder(LEGACY.deserialize(rules.title()))
                .canCloseWithEscape(true)
                .body(body)
                .build();
    }

    private static DialogType buildRulesType() {
        ActionButton close = ActionButton.create(
                LEGACY.deserialize("&7关闭"), null, BUTTON_WIDTH, null);
        return DialogType.multiAction(List.of(close), null, 1);
    }

    // ------------------------------------------------------------ key

    /** 「打开某个页面」用的点击 key。 */
    public static Key openKey(String page) {
        return Key.key(KEY_NAMESPACE, OPEN_PREFIX + page);
    }
}
