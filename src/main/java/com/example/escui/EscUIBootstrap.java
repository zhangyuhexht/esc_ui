package com.example.escui;

import com.example.escui.config.MenuConfig;
import com.example.escui.config.MenuDefinition;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import io.papermc.paper.registry.keys.tags.DialogTagKeys;
import io.papermc.paper.tag.TagEntry;

import java.util.List;
import java.util.function.Consumer;

/**
 * Plugin bootstrapper.
 *
 * <p>Runs during registry building, earlier than normal plugins, so it can put
 * things in the dialog registry and attach a button to the ESC menu.</p>
 *
 * <p>It registers exactly one dialog:</p>
 * <ol>
 *   <li>turn the {@code menu} section of menu.yml into a dialog;</li>
 *   <li>add it to {@code PAUSE_SCREEN_ADDITIONS} so it shows up in the ESC menu.</li>
 * </ol>
 *
 * <p>The rules page is not registered here. {@link EscUIPlugin} builds it when a
 * player clicks, so changes to the rules text don't need a restart.</p>
 */
public final class EscUIBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        Consumer<String> warn = message -> context.getLogger().warn(message);
        MenuConfig config = MenuConfig.load(context.getDataDirectory(), warn);

        MenuDefinition menu = config.menu();
        if (menu == null) {
            warn.accept("No usable menu entries in menu.yml; no ESC button will be added.");
            return;
        }

        // 1. Register the menu dialog.
        context.getLifecycleManager().registerEventHandler(
                RegistryEvents.DIALOG.compose().newHandler(event ->
                        event.registry().register(
                                DialogKeys.create(DialogBuilder.MENU_KEY),
                                dialogBuilder -> DialogBuilder.applyMenu(dialogBuilder, menu)
                        )
                )
        );

        // 2. Attach it to the ESC menu.
        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.preFlatten(RegistryKey.DIALOG).newHandler(event ->
                        event.registrar().addToTag(
                                DialogTagKeys.PAUSE_SCREEN_ADDITIONS,
                                List.of(TagEntry.valueEntry(DialogKeys.create(DialogBuilder.MENU_KEY)))
                        )
                )
        );

        context.getLogger().info("EscUI: ESC button '" + menu.button() + "' registered with "
                + menu.entries().size() + " entries.");
    }
}
