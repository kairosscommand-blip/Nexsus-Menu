package com.nexuscraft.nexusmenu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds every menu this plugin shows, and carries out every button's action. Every "does
 * something in another plugin" action here works the exact same way a player typing the command
 * themselves would: it dispatches that plugin's own already-existing, already-permission-checked
 * command via Bukkit.dispatchCommand(player, ...) rather than reaching into that plugin's
 * internals. That means every button in this menu behaves identically to (and stays in sync
 * with) whatever that plugin's real command does -- no separate logic to keep in sync, no
 * reflection into private classes, and a plugin that isn't installed/enabled just doesn't get a
 * category shown for it (checked live, every time the root menu opens). Same philosophy as
 * NexusAdmin's AdminMenus, generalized here to read its whole menu tree from config.yml instead
 * of hardcoding one method per category.
 */
final class MenuService {

    private static final int PAGE_SIZE = 45; // slots 0-44 of a 54-slot inventory
    private static final int SIZE = 54;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 47;
    private static final int SLOT_PAGE_INDICATOR = 49;
    private static final int SLOT_CLOSE = 51;
    private static final int SLOT_NEXT = 53;

    private final JavaPlugin plugin;
    private final MenuConfig config;

    MenuService(JavaPlugin plugin, MenuConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    private void dispatch(Player player, String command) {
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(player, command));
    }

    private boolean installed(String pluginName) {
        return pluginName != null && Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }

    private List<MenuCategory> visibleCategories() {
        List<MenuCategory> visible = new ArrayList<>();
        for (MenuCategory category : config.categories) {
            if (installed(category.installedCheck)) {
                visible.add(category);
            }
        }
        return visible;
    }

    // ---------------------------------------------------------------- root menu

    Inventory buildRoot(Player viewer, int page) {
        List<MenuCategory> visible = visibleCategories();
        int totalPages = Math.max(1, (int) Math.ceil(visible.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        ActionMenuHolder holder = new ActionMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, SIZE, ColorCodes.translate(config.rootTitle));
        holder.setInventory(inv);

        int start = page * PAGE_SIZE;
        int end = Math.min(visible.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            MenuCategory category = visible.get(i);
            int slot = i - start;
            List<String> lore = new ArrayList<>();
            if (category.description != null && !category.description.isBlank()) {
                lore.add(category.description);
            }
            lore.add("&7" + category.entries.size() + " option(s)");
            inv.setItem(slot, MenuIcons.icon(category.icon, "&d&l" + category.title, lore));
            final MenuCategory clicked = category;
            holder.setAction(slot, () -> viewer.openInventory(buildCategory(viewer, clicked, 0)));
        }

        int finalPage = page;
        if (page > 0) {
            inv.setItem(SLOT_PREV, MenuIcons.prevPage(page));
            holder.setAction(SLOT_PREV, () -> viewer.openInventory(buildRoot(viewer, finalPage - 1)));
        }
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, MenuIcons.nextPage(page, totalPages));
            holder.setAction(SLOT_NEXT, () -> viewer.openInventory(buildRoot(viewer, finalPage + 1)));
        }
        if (totalPages > 1) {
            inv.setItem(SLOT_PAGE_INDICATOR, MenuIcons.pageIndicator(page, totalPages));
        }
        inv.setItem(SLOT_CLOSE, MenuIcons.closeButton());
        holder.setAction(SLOT_CLOSE, viewer::closeInventory);

        return inv;
    }

    // ---------------------------------------------------------------- category menu

    Inventory buildCategory(Player viewer, MenuCategory category, int page) {
        List<MenuEntry> entries = category.entries;
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        ActionMenuHolder holder = new ActionMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                ColorCodes.translate(config.categoryTitlePrefix + category.title));
        holder.setInventory(inv);

        int finalPage = page;
        int start = page * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            MenuEntry entry = entries.get(i);
            int slot = i - start;
            inv.setItem(slot, MenuIcons.icon(entry.icon, "&b" + entry.label, entry.lore));
            final MenuEntry clicked = entry;
            holder.setAction(slot, () -> activate(viewer, category, clicked, finalPage));
        }

        if (page > 0) {
            inv.setItem(SLOT_PREV, MenuIcons.prevPage(page));
            holder.setAction(SLOT_PREV, () -> viewer.openInventory(buildCategory(viewer, category, finalPage - 1)));
        }
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, MenuIcons.nextPage(page, totalPages));
            holder.setAction(SLOT_NEXT, () -> viewer.openInventory(buildCategory(viewer, category, finalPage + 1)));
        }
        if (totalPages > 1) {
            inv.setItem(SLOT_PAGE_INDICATOR, MenuIcons.pageIndicator(page, totalPages));
        }
        inv.setItem(SLOT_BACK, MenuIcons.backButton());
        holder.setAction(SLOT_BACK, () -> viewer.openInventory(buildRoot(viewer, 0)));
        inv.setItem(SLOT_CLOSE, MenuIcons.closeButton());
        holder.setAction(SLOT_CLOSE, viewer::closeInventory);

        return inv;
    }

    // ---------------------------------------------------------------- activating an entry

    private void activate(Player viewer, MenuCategory category, MenuEntry entry, int returnPage) {
        switch (entry.type) {
            case INFO -> {
                // Nothing to dispatch -- the lore already says everything. Just leave the menu
                // open so the player can keep reading it.
            }
            case COMMAND -> dispatch(viewer, entry.command);
            case TEXT -> runPromptChain(viewer, category, entry, returnPage, List.of(), 0);
            case PLAYER -> viewer.openInventory(buildPlayerPicker(viewer,
                    "&d&lWho? — " + entry.label,
                    target -> {
                        List<String> tokens = new ArrayList<>();
                        tokens.add(entry.commandPrefix);
                        tokens.add(target.getName());
                        if (entry.commandSuffix != null) {
                            tokens.add(entry.commandSuffix);
                        }
                        dispatch(viewer, String.join(" ", tokens));
                    },
                    () -> viewer.openInventory(buildCategory(viewer, category, returnPage))));
            case PLAYER_TEXT -> viewer.openInventory(buildPlayerPicker(viewer,
                    "&d&lWho? — " + entry.label,
                    target -> runPromptChain(viewer, category, entry, returnPage, List.of(target.getName()), 0),
                    () -> viewer.openInventory(buildCategory(viewer, category, returnPage))));
        }
    }

    /** Chains one or more anvil text prompts in order, accumulating each typed value, then
     *  dispatches the final assembled command once every prompt (in {@code entry.prompts}) has
     *  been answered. {@code priorTokens} carries anything that comes before the prompts
     *  themselves (the command prefix, and a picked player's name for PLAYER_TEXT entries). */
    private void runPromptChain(Player viewer, MenuCategory category, MenuEntry entry, int returnPage,
                                 List<String> priorTokens, int promptIndex) {
        if (promptIndex >= entry.prompts.size()) {
            List<String> tokens = new ArrayList<>();
            tokens.add(entry.commandPrefix);
            tokens.addAll(priorTokens);
            if (entry.commandSuffix != null) {
                tokens.add(entry.commandSuffix);
            }
            dispatch(viewer, String.join(" ", tokens));
            return;
        }
        PromptDef prompt = entry.prompts.get(promptIndex);
        viewer.openInventory(buildTextPrompt(prompt.title, prompt.optional, typed -> {
            List<String> next = new ArrayList<>(priorTokens);
            if (!typed.isBlank()) {
                next.add(typed);
            }
            runPromptChain(viewer, category, entry, returnPage, next, promptIndex + 1);
        }));
    }

    // ---------------------------------------------------------------- shared: player picker

    private Inventory buildPlayerPicker(Player viewer, String title, java.util.function.Consumer<Player> onSelect, Runnable onBack) {
        PlayerPickerHolder holder = new PlayerPickerHolder();
        Inventory inv = Bukkit.createInventory(holder, SIZE, ColorCodes.translate(title));
        holder.setInventory(inv);
        holder.setOnSelect(onSelect);
        holder.setOnBack(onBack);

        List<? extends Player> online = List.copyOf(Bukkit.getOnlinePlayers());
        int slot = 0;
        for (Player candidate : online) {
            if (slot >= PAGE_SIZE) break; // more than 45 concurrent players -- a paginated
            // picker is a reasonable future improvement once a server regularly exceeds that.
            inv.setItem(slot, MenuIcons.playerHead(candidate));
            holder.putPlayer(slot, candidate.getUniqueId());
            slot++;
        }

        inv.setItem(SLOT_BACK, MenuIcons.backButton());
        return inv;
    }

    // ---------------------------------------------------------------- shared: text prompt anvil

    private Inventory buildTextPrompt(String title, boolean allowBlank, java.util.function.Consumer<String> onConfirm) {
        TextPromptHolder holder = new TextPromptHolder("Type here...", allowBlank, onConfirm);
        Inventory inv = Bukkit.createInventory(holder, InventoryType.ANVIL, ColorCodes.translate("&3" + title));
        holder.setInventory(inv);

        ItemStack seed = new ItemStack(Material.PAPER);
        var meta = seed.getItemMeta();
        meta.setDisplayName(org.bukkit.ChatColor.RESET + holder.placeholder());
        meta.getPersistentDataContainer().set(TextPromptHolder.seedTag(plugin), PersistentDataType.BOOLEAN, true);
        seed.setItemMeta(meta);
        inv.setItem(0, seed);

        return inv;
    }
}
