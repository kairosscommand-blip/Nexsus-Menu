package com.nexuscraft.nexusmenu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** {@code /menu} (aliases {@code /nexusmenu}, {@code /hub}) -- the typed fallback for a player
 *  who lost their menu item, plus the one admin-only reload command. Unrestricted by default:
 *  the whole point of this plugin is never needing a command, but the fallback itself shouldn't
 *  need a permission node either. */
public final class MenuCommand implements CommandExecutor {

    private final MenuListener listener;
    private final MenuService menu;
    private final Runnable onReload;

    public MenuCommand(MenuListener listener, MenuService menu, Runnable onReload) {
        this.listener = listener;
        this.menu = menu;
        this.onReload = onReload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "open";
        return switch (sub) {
            case "open", "" -> open(sender);
            case "item" -> item(sender);
            case "reload" -> reload(sender);
            default -> {
                sender.sendMessage(usage());
                yield true;
            }
        };
    }

    private boolean open(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Run this in-game to open the Nexus Hub.");
            return true;
        }
        player.openInventory(menu.buildRoot(player, 0));
        return true;
    }

    private boolean item(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Run this in-game to receive the menu item.");
            return true;
        }
        if (listener.claimItem(player)) {
            player.sendMessage(ColorCodes.translate("&7[NexusMenu] &fYour Nexus Codex has been added to your inventory."));
        } else {
            player.sendMessage(ColorCodes.translate("&7[NexusMenu] &fYou've already claimed your Nexus Codex. "
                    + "Lost it? Ask an admin, or just run &f/menu&7 to open the hub without it."));
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("nexusmenu.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        onReload.run();
        sender.sendMessage("§7[NexusMenu] §fconfig.yml reloaded.");
        return true;
    }

    private static String usage() {
        return "§7Usage: §f/menu <open|item|reload>";
    }
}
