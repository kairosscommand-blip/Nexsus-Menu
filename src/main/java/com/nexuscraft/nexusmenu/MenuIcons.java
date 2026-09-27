package com.nexuscraft.nexusmenu;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/** Small shared helper for building the ItemStacks every menu in this plugin uses as buttons. */
final class MenuIcons {

    private static final int WRAP_WIDTH = 45;

    private MenuIcons() {
    }

    static ItemStack icon(Material material, String coloredName, List<String> loreLines) {
        ItemStack stack = new ItemStack(material == null ? Material.PAPER : material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ColorCodes.translate(coloredName));
        List<String> wrapped = new ArrayList<>();
        for (String raw : loreLines) {
            for (String piece : wrap(ColorCodes.translate(raw))) {
                wrapped.add(ChatColor.GRAY + piece);
            }
        }
        if (!wrapped.isEmpty()) {
            meta.setLore(wrapped);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    static ItemStack plain(Material material, String coloredName, String... lore) {
        List<String> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(line);
        }
        return icon(material, coloredName, lines);
    }

    static ItemStack backButton() {
        return plain(Material.ARROW, "&e&l← Back");
    }

    static ItemStack closeButton() {
        return plain(Material.BARRIER, "&c&lClose");
    }

    static ItemStack nextPage(int currentPage, int totalPages) {
        return plain(Material.LIME_DYE, "&a&lNext Page →", "Page " + (currentPage + 2) + " of " + totalPages);
    }

    static ItemStack prevPage(int currentPage) {
        return plain(Material.LIME_DYE, "&a&l← Previous Page", "Page " + currentPage + " of the menu");
    }

    static ItemStack pageIndicator(int currentPage, int totalPages) {
        return plain(Material.PAPER, "&7Page " + (currentPage + 1) + " / " + totalPages);
    }

    static ItemStack playerHead(OfflinePlayer target, String... lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(target);
            meta = skullMeta;
        }
        meta.setDisplayName(ChatColor.YELLOW + String.valueOf(target.getName()));
        List<String> loreLines = new ArrayList<>();
        for (String line : lore) {
            loreLines.add(ChatColor.GRAY + line);
        }
        if (!loreLines.isEmpty()) {
            meta.setLore(loreLines);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    /** Splits a single (possibly long) line of text into several lore-width lines, breaking only
     *  at word boundaries -- config.yml's lore text is written as prose, not pre-wrapped, so this
     *  keeps every tooltip readable regardless of how long an individual config line is. */
    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() > 0 && current.length() + 1 + word.length() > WRAP_WIDTH) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }
}
