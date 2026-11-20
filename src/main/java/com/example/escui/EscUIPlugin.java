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
 * Main plugin class.
 *
 * <p>The menu dialog is registered during bootstrap (see {@link EscUIBootstrap}).
 * At runtime this class does two things:</p>
 * <ol>
 *   <li>a one-off sanity check that dialogs can be built outside bootstrap;</li>
 *   <li>handle clicks on a show-rules entry by re-reading menu.yml and building
 *       the rules dialog on the spot — so text edits take effect without a restart.</li>
 * </ol>
 *
 * <p>Menu entries themselves are baked in at startup; adding, removing or
 * renaming them still needs a restart.</p>
 */
public final class EscUIPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // Sanity check: the bootstrap-phase registration doesn't prove this works.
        try {
            Path dir = getDataFolder().toPath();
            RulesDefinition rules = MenuConfig.load(dir, getLogger()::warning).rules();
            if (rules != null) {
                DialogBuilder.buildRulesDialog(rules);
                getLogger().info("Runtime rules dialog check passed (" + rules.lines().size() + " lines).");
            }
        } catch (Throwable ex) {
            getLogger().warning("Runtime dialog check failed: " + ex);
        }

        getLogger().info("EscUI enabled. Menu entry changes require a server restart.");
    }

    @Override
    public void onDisable() {
        getLogger().info("EscUI disabled.");
    }

    /**
     * Handles clicks on the rules entry. URL entries never reach this method —
     * the client performs those itself.
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
            getLogger().warning("Unknown page in click key: " + page + ". Check menu.yml.");
            return;
        }

        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
            return;
        }
        Player player = connection.getPlayer();

        // Re-read every time so rules edits are live.
        MenuConfig config = MenuConfig.load(getDataFolder().toPath(), getLogger()::warning);
        RulesDefinition rules = config.rules();
        if (rules == null) {
            getLogger().warning("No rules section in menu.yml; cannot open the rules page.");
            return;
        }

        try {
            Dialog dialog = DialogBuilder.buildRulesDialog(rules);
            player.showDialog(dialog);
        } catch (Throwable ex) {
            getLogger().warning("Failed to open rules: " + ex);
        }
    }
}
