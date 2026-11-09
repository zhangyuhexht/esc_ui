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
 * 插件引导类（bootstrapper）。
 *
 * <p>它在服务端启动的「注册表构建」阶段跑，比普通插件更早，所以能往对话框注册表里塞东西、
 * 并往 ESC 菜单的标签上挂按钮。</p>
 *
 * <p>这里只注册「菜单页」一个对话框：</p>
 * <ol>
 *   <li>把 menu.yml 的 menu 段注册成对话框（按钮列表）；</li>
 *   <li>把它加进 {@code PAUSE_SCREEN_ADDITIONS} 标签 —— ESC 菜单里就会出现
 *       一个「服务器链接」按钮，点开就是这一页。</li>
 * </ol>
 *
 * <p>「服务器规则」条目点开的内容不在这里注册 —— 它是玩家点击时由
 * {@link EscUIPlugin} 现场构建的（这样改 rules 配置不用重启服务端）。</p>
 */
public final class EscUIBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        Consumer<String> warn = message -> context.getLogger().warn(message);
        MenuConfig config = MenuConfig.load(context.getDataDirectory(), warn);

        MenuDefinition menu = config.menu();
        if (menu == null) {
            warn.accept("menu.yml 里没有可用的菜单条目，ESC 菜单里不会出现按钮。");
            return;
        }

        // ① 注册「菜单页」对话框
        context.getLifecycleManager().registerEventHandler(
                RegistryEvents.DIALOG.compose().newHandler(event ->
                        event.registry().register(
                                DialogKeys.create(DialogBuilder.MENU_KEY),
                                dialogBuilder -> DialogBuilder.applyMenu(dialogBuilder, menu)
                        )
                )
        );

        // ② 把它挂到 ESC 菜单上
        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.preFlatten(RegistryKey.DIALOG).newHandler(event ->
                        event.registrar().addToTag(
                                DialogTagKeys.PAUSE_SCREEN_ADDITIONS,
                                List.of(TagEntry.valueEntry(DialogKeys.create(DialogBuilder.MENU_KEY)))
                        )
                )
        );

        context.getLogger().info("EscUI：ESC 菜单按钮「" + menu.button() + "」已注册，点开共 "
                + menu.entries().size() + " 个条目。");
    }
}
