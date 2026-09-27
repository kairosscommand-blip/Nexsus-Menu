package com.nexuscraft.nexusmenu;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class NexusMenuPlugin extends JavaPlugin {

    private MenuConfig config;
    private MenuService menu;
    private MenuItemClaimRegistry claims;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new MenuConfig();
        config.load(this);
        this.menu = new MenuService(this, config);

        this.claims = new MenuItemClaimRegistry();
        loadClaims();

        MenuListener listener = new MenuListener(this, config, menu, claims, this::saveClaims);
        getServer().getPluginManager().registerEvents(listener, this);

        MenuItemLockListener lockListener = new MenuItemLockListener(this, config, claims, this::saveClaims);
        getServer().getPluginManager().registerEvents(lockListener, this);

        getCommand("menu").setExecutor(new MenuCommand(listener, menu, this::reload));

        int entryCount = config.categories.stream().mapToInt(c -> c.entries.size()).sum();
        getLogger().info("NexusMenu enabled -- " + config.categories.size() + " categor"
                + (config.categories.size() == 1 ? "y" : "ies") + ", " + entryCount + " menu entries loaded, "
                + claims.all().size() + " item claim(s) on file.");
    }

    @Override
    public void onDisable() {
        saveClaims();
        getLogger().info("NexusMenu disabled.");
    }

    private void reload() {
        reloadConfig();
        config.load(this);
    }

    private void loadClaims() {
        File file = new File(getDataFolder(), "item_claims.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        List<UUID> loaded = MenuItemClaimPersistence.loadFrom(data);
        claims.loadAll(loaded);
    }

    private void saveClaims() {
        getDataFolder().mkdirs();
        YamlConfiguration data = new YamlConfiguration();
        MenuItemClaimPersistence.saveTo(data, List.copyOf(claims.all()));
        try {
            data.save(new File(getDataFolder(), "item_claims.yml"));
        } catch (IOException e) {
            getLogger().warning("Failed to save item_claims.yml: " + e.getMessage());
        }
    }
}
