package com.nexuscraft.nexusmenu;

import org.bukkit.Material;

import java.util.List;

/** One plugin's whole tab in the root menu -- e.g. "Great Houses" (NexusHouses), "Economy"
 *  (NexusEconomy). Only shown in the root menu when {@link #installedCheck} is currently an
 *  enabled plugin (checked live on every open, not just at startup). */
final class MenuCategory {

    final String id;
    final String plugin;
    final String installedCheck;
    final String title;
    final Material icon;
    final String description;
    final List<MenuEntry> entries;

    MenuCategory(String id, String plugin, String installedCheck, String title, Material icon,
                 String description, List<MenuEntry> entries) {
        this.id = id;
        this.plugin = plugin;
        this.installedCheck = installedCheck;
        this.title = title;
        this.icon = icon;
        this.description = description;
        this.entries = entries;
    }
}
