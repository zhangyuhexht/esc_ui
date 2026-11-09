package com.example.escui;

import com.example.escui.config.MenuConfig;
import com.example.escui.config.RulesDefinition;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

/**
 * EscUI 主类。
 *
 * <p>「菜单页」是在引导阶段注册好的（见 {@link EscUIBootstrap}），ESC 菜单里
 * 「服务器链接」按钮点开就是它。运行期这里做两件事：</p>
 * <ol>
 *   <li>启动时做一次「运行期构建对话框」自检；</li>
 *   <li>处理「服务器规则」条目的点击：<b>重新读一次 menu.yml 的 rules 段</b>，
 *       现场构建规则文本对话框再显示 —— 所以改 rules 配置不用重启服务端。</li>
 * </ol>
 *
 * <p>注意：菜单页本身的条目增减/改名是启动时注册的，那类改动要重启服务端。</p>
 */
public final class EscUIPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // 运行期自检：确认这个阶段能构建对话框（引导阶段注册的不算）
        try {
            Path dir = getDataFolder().toPath();
            RulesDefinition rules = MenuConfig.load(dir, getLogger()::warning).rules();
            if (rules != null) {
                DialogBuilder.buildRulesDialog(rules);
                getLogger().info("运行期构建规则对话框自检通过（共 " + rules.lines().size() + " 行）。");
            }
        } catch (Throwable ex) {
            getLogger().warning("运行期构建对话框自检失败：" + ex);
        }

        getLogger().info("EscUI 已启用。菜单条目改动需要重启服务端。");
    }

    @Override
    public void onDisable() {
        getLogger().info("EscUI 已卸载。");
    }

    /**
     * 处理菜单里「服务器规则」条目的点击。
     *
     * <p>其它动作（开网址）都是客户端自己完成的，不会走到这里。</p>
     */
    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent event) {
        Key identifier = event.getIdentifier();
        if (!DialogBuilder.KEY_NAMESPACE.equals(identifier.namespace())) {
            return;
        }
        String value = identifier.value();
        if (!value.startsWith(DialogBuilder.OPEN_PREFIX)) {
            return;
        }
        String page = value.substring(DialogBuilder.OPEN_PREFIX.length());
        if (!"rules".equals(page)) {
            getLogger().warning("点击要打开的页面「" + page + "」不存在，请检查 menu.yml");
            return;
        }

        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
            return;
        }
        Player player = connection.getPlayer();

        // 每次都重新读配置：这样改完 rules 段直接就能生效，不用重启服务端
        MenuConfig config = MenuConfig.load(getDataFolder().toPath(), getLogger()::warning);
        RulesDefinition rules = config.rules();
        if (rules == null) {
            getLogger().warning("menu.yml 里没有 rules 段，打不开服务器规则。");
            return;
        }

        try {
            Dialog dialog = DialogBuilder.buildRulesDialog(rules);
            player.showDialog(dialog);
        } catch (Throwable ex) {
            getLogger().warning("打开服务器规则失败：" + ex);
        }
    }
}
