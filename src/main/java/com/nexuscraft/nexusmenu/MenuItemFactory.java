package com.nexuscraft.nexusmenu;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * The physical "right-click to open the hub" item every player can carry -- tagged with a
 * PersistentDataContainer marker rather than fingerprinted by its display name, exactly like
 * NexusStarter's GuideItemFactory. Deliberately a plain configurable vanilla item (ENCHANTED_BOOK
 * by default), never a WRITTEN_BOOK: NexusStarter's own guide item avoids relying on the
 * client-side book-reading gesture so the trigger behaves identically on Java and on
 * Bedrock/Xbox via Geyser -- a plain right-click on an ordinary item is the one interaction both
 * clients handle the same way.
 */
final class MenuItemFactory {

    private static final String MARKER_KEY = "menu_item";

    private MenuItemFactory() {
    }

    static ItemStack create(Plugin plugin, Material material, String displayName, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorCodes.translate(displayName));

        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ColorCodes.translate(line));
        }
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, "true");
        item.setItemMeta(meta);
        return item;
    }

    static boolean isMenuItem(Plugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return "true".equals(item.getItemMeta().getPersistentDataContainer().get(key(plugin), PersistentDataType.STRING));
    }

    private static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, MARKER_KEY);
    }
}
