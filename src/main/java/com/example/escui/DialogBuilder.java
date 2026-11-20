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
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns config into in-game dialogs. Two of them:
 *
 * <ul>
 *   <li>the menu ({@link #applyMenu}), opened by the ESC button;</li>
 *   <li>the rules page ({@link #applyRules} / {@link #buildRulesDialog}), built at click time.</li>
 * </ul>
 *
 * <p>The menu is registered during bootstrap; rules are built on demand in
 * {@link #buildRulesDialog}. Both are text and buttons only, so there's no item
 * registry dependency during the early bootstrap phase.</p>
 *
 * <p>Bootstrap and the main plugin use separate class loaders, so this class gets
 * loaded twice. Don't keep static state here.</p>
 */
public final class DialogBuilder {

    /** Namespace for every dialog key and click key. */
    public static final String KEY_NAMESPACE = "escui";
    /** Prefix for "open page" click keys. */
    public static final String OPEN_PREFIX = "open/";
    /** Key of the menu dialog. */
    public static final Key MENU_KEY = Key.key(KEY_NAMESPACE, "menu");

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final int BUTTON_WIDTH = 200;

    private DialogBuilder() {
    }

    // ------------------------------------------------------------ menu

    /** Fills a registry builder with the menu dialog (bootstrap phase). */
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
            // Vanilla rejects a dialog with no actions.
            buttons.add(ActionButton.create(LEGACY.deserialize("&7关闭"), null, BUTTON_WIDTH, null));
        }
        return DialogType.multiAction(buttons, null, menu.columns());
    }

    /** One entry, one action: open a URL, or ask the server to show the rules text. */
    private static DialogAction actionOf(MenuEntryDefinition entry) {
        if (!entry.url().isEmpty()) {
            return DialogAction.staticAction(ClickEvent.openUrl(entry.url()));
        }
        // Handled by EscUIPlugin. Config is re-read on every click so rules edits are hot.
        return DialogAction.customClick(openKey("rules"), null);
    }

    // ------------------------------------------------------------ rules

    /** Build a rules dialog object at runtime. */
    public static Dialog buildRulesDialog(RulesDefinition rules) {
        return Dialog.create(factory -> applyRules(factory.empty(), rules));
    }

    /** Registry builder variant. Not currently registered; kept for future use. */
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

    // ------------------------------------------------------------ keys

    /** Click key for opening a page. */
    public static Key openKey(String page) {
        return Key.key(KEY_NAMESPACE, OPEN_PREFIX + page);
    }
}
