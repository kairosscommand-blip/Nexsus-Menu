package com.nexuscraft.nexusmenu;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Loads config.yml into plain in-memory objects. Every field is parsed defensively -- a
 * malformed category or entry is skipped (with a warning logged) rather than crashing the whole
 * plugin, same convention every other Nexus plugin's config loader follows, since a typo in a
 * 200-entry hand/AI-authored config file is far more likely than in code.
 */
final class MenuConfig {

    Material menuItemMaterial = Material.ENCHANTED_BOOK;
    String menuItemDisplayName = "&d&lNexus Codex";
    List<String> menuItemLore = new ArrayList<>();
    boolean lockInInventory = true;
    boolean keepOnDeath = true;

    boolean firstJoinGiveItem = true;
    int firstJoinDelaySeconds = 3;
    String firstJoinChatMessage = "&b&lYou've received a Nexus Codex!";

    String rootTitle = "&5&lNexus Hub";
    String categoryTitlePrefix = "&5&l";

    final List<MenuCategory> categories = new ArrayList<>();

    void load(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Logger log = plugin.getLogger();

        ConfigurationSection itemSection = config.getConfigurationSection("menu-item");
        if (itemSection != null) {
            menuItemMaterial = materialOrDefault(itemSection.getString("material"), Material.ENCHANTED_BOOK, log, "menu-item.material");
            menuItemDisplayName = itemSection.getString("display-name", menuItemDisplayName);
            menuItemLore = itemSection.getStringList("lore");
            lockInInventory = itemSection.getBoolean("lock-in-inventory", true);
            keepOnDeath = itemSection.getBoolean("keep-on-death", true);
        }

        ConfigurationSection joinSection = config.getConfigurationSection("first-join");
        if (joinSection != null) {
            firstJoinGiveItem = joinSection.getBoolean("give-item", true);
            firstJoinDelaySeconds = joinSection.getInt("delay-seconds", 3);
            firstJoinChatMessage = joinSection.getString("chat-message", firstJoinChatMessage);
        }

        ConfigurationSection titles = config.getConfigurationSection("titles");
        if (titles != null) {
            rootTitle = titles.getString("root", rootTitle);
            categoryTitlePrefix = titles.getString("category-prefix", categoryTitlePrefix);
        }

        categories.clear();
        List<?> rawCategories = config.getList("categories");
        if (rawCategories == null) {
            log.warning("config.yml has no 'categories' list -- the menu will be empty.");
            return;
        }

        int catIndex = 0;
        for (Object rawCat : rawCategories) {
            catIndex++;
            if (!(rawCat instanceof java.util.Map<?, ?> map)) {
                log.warning("categories[" + catIndex + "] is not a map -- skipped.");
                continue;
            }
            try {
                MenuCategory category = parseCategory(map, log);
                if (category != null) {
                    categories.add(category);
                }
            } catch (RuntimeException e) {
                log.warning("categories[" + catIndex + "] failed to parse (" + e.getMessage() + ") -- skipped.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private MenuCategory parseCategory(java.util.Map<?, ?> map, Logger log) {
        String id = str(map.get("id"));
        String plugin = str(map.get("plugin"));
        String installedCheck = str(map.get("installed_check"));
        if (installedCheck == null) {
            installedCheck = plugin;
        }
        String title = str(map.get("title"));
        Material icon = materialOrDefault(str(map.get("icon")), Material.BOOK, log, "category '" + id + "'.icon");
        String description = str(map.get("description"));
        if (id == null || title == null) {
            log.warning("category with plugin=" + plugin + " is missing id/title -- skipped.");
            return null;
        }

        List<MenuEntry> entries = new ArrayList<>();
        Object rawEntries = map.get("entries");
        if (rawEntries instanceof List<?> list) {
            int entryIndex = 0;
            for (Object rawEntry : list) {
                entryIndex++;
                if (!(rawEntry instanceof java.util.Map<?, ?> entryMap)) {
                    log.warning("category '" + id + "' entries[" + entryIndex + "] is not a map -- skipped.");
                    continue;
                }
                try {
                    MenuEntry entry = parseEntry((java.util.Map<?, ?>) entryMap, log, id);
                    if (entry != null) {
                        entries.add(entry);
                    }
                } catch (RuntimeException e) {
                    log.warning("category '" + id + "' entries[" + entryIndex + "] failed to parse (" + e.getMessage() + ") -- skipped.");
                }
            }
        }

        return new MenuCategory(id, plugin, installedCheck, title, icon, description, entries);
    }

    @SuppressWarnings("unchecked")
    private MenuEntry parseEntry(java.util.Map<?, ?> map, Logger log, String categoryId) {
        String label = str(map.get("label"));
        String typeRaw = str(map.get("type"));
        if (label == null || typeRaw == null) {
            log.warning("category '" + categoryId + "' has an entry missing label/type -- skipped.");
            return null;
        }
        MenuEntry.Type type;
        try {
            type = MenuEntry.Type.valueOf(typeRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warning("category '" + categoryId + "' entry '" + label + "' has unknown type '" + typeRaw + "' -- skipped.");
            return null;
        }
        Material icon = materialOrDefault(str(map.get("icon")), Material.PAPER, log,
                "category '" + categoryId + "' entry '" + label + "'.icon");
        List<String> lore = new ArrayList<>();
        Object rawLore = map.get("lore");
        if (rawLore instanceof List<?> list) {
            for (Object line : list) {
                if (line != null) {
                    lore.add(line.toString());
                }
            }
        }

        String command = str(map.get("command"));
        String commandPrefix = str(map.get("command_prefix"));
        String commandSuffix = str(map.get("command_suffix"));

        List<PromptDef> prompts = new ArrayList<>();
        Object rawPrompts = map.get("prompts");
        if (rawPrompts instanceof List<?> list) {
            for (Object rawPrompt : list) {
                if (rawPrompt instanceof java.util.Map<?, ?> promptMap) {
                    String ptitle = str(promptMap.get("title"));
                    boolean optional = Boolean.TRUE.equals(promptMap.get("optional"));
                    if (ptitle != null) {
                        prompts.add(new PromptDef(ptitle, optional));
                    }
                }
            }
        }

        if (type == MenuEntry.Type.COMMAND && command == null) {
            log.warning("category '" + categoryId + "' COMMAND entry '" + label + "' has no command -- skipped.");
            return null;
        }
        if (type != MenuEntry.Type.COMMAND && type != MenuEntry.Type.INFO && commandPrefix == null) {
            log.warning("category '" + categoryId + "' entry '" + label + "' has no command_prefix -- skipped.");
            return null;
        }
        if ((type == MenuEntry.Type.TEXT || type == MenuEntry.Type.PLAYER_TEXT) && prompts.isEmpty()) {
            log.warning("category '" + categoryId + "' entry '" + label + "' (" + type + ") has no prompts -- skipped.");
            return null;
        }
        // runPromptChain drops a blank optional value's slot entirely rather than reserving its
        // position, so an optional prompt anywhere but last would silently shift every prompt
        // after it into the wrong argument position. Only ever allow it at the end of the chain.
        for (int i = 0; i < prompts.size() - 1; i++) {
            if (prompts.get(i).optional) {
                log.warning("category '" + categoryId + "' entry '" + label + "' has an optional prompt "
                        + "before the end of its chain -- skipped (only the last prompt in a chain may be optional).");
                return null;
            }
        }

        return new MenuEntry(label, type, command, commandPrefix, commandSuffix, icon, lore, prompts);
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static Material materialOrDefault(String raw, Material fallback, Logger log, String context) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warning(context + ": unknown material '" + raw + "', using " + fallback + " instead.");
            return fallback;
        }
    }

    ItemStack buildMenuItem(Plugin plugin) {
        return MenuItemFactory.create(plugin, menuItemMaterial, menuItemDisplayName, menuItemLore);
    }
}
