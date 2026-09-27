package com.nexuscraft.nexusmenu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Every way a player actually reaches (and uses) the menu: the one-time first-join handoff, the
 * one-time item claim (also covers a player who joined before this plugin existed -- see {@link
 * #claimItem}), right-clicking the physical menu item, and every click inside any of {@link
 * MenuService}'s inventories (including the anvil text-prompt trick). One shared listener, same
 * shape as NexusAdmin's AdminMenuListener plus NexusStarter's GuideMenuListener combined -- there
 * is exactly one of each event handler in this whole plugin.
 */
final class MenuListener implements Listener {

    private final JavaPlugin plugin;
    private final MenuConfig config;
    private final MenuService menu;
    private final MenuItemClaimRegistry claims;
    private final Runnable onClaimsChanged;

    MenuListener(JavaPlugin plugin, MenuConfig config, MenuService menu, MenuItemClaimRegistry claims, Runnable onClaimsChanged) {
        this.plugin = plugin;
        this.config = config;
        this.menu = menu;
        this.claims = claims;
        this.onClaimsChanged = onClaimsChanged;
    }

    // ---------------------------------------------------------------- first join / claim

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        long delayTicks = Math.max(0, config.firstJoinDelaySeconds) * 20L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> onJoinDelayed(player), delayTicks);
    }

    private void onJoinDelayed(Player player) {
        if (!player.isOnline()) {
            return;
        }
        if (config.firstJoinGiveItem && claimItem(player) && config.firstJoinChatMessage != null) {
            player.sendMessage(ColorCodes.translate(config.firstJoinChatMessage));
        }
    }

    /** The one shared path every way of getting the physical menu item goes through -- automatic
     *  on join, or on demand via {@code /menu item} -- so they can never drift out of sync with
     *  each other. Returns true only if this call is the one that actually granted it. */
    boolean claimItem(Player player) {
        if (!claims.claim(player.getUniqueId())) {
            return false;
        }
        player.getInventory().addItem(config.buildMenuItem(plugin));
        onClaimsChanged.run();
        return true;
    }

    boolean hasClaimedItem(UUID uuid) {
        return claims.hasClaimed(uuid);
    }

    void resetItemClaim(UUID uuid) {
        if (claims.reset(uuid)) {
            onClaimsChanged.run();
        }
    }

    // ---------------------------------------------------------------- opening the menu

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!MenuItemFactory.isMenuItem(plugin, item)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().openInventory(menu.buildRoot(event.getPlayer(), 0));
    }

    // ---------------------------------------------------------------- inventory clicks

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        Object holder = top.getHolder();

        if (holder instanceof ActionMenuHolder actionMenu) {
            event.setCancelled(true);
            if (event.getClickedInventory() != top) return;
            Runnable action = actionMenu.actionFor(event.getSlot());
            if (action != null) {
                playClick(event.getWhoClicked());
                action.run();
            }
            return;
        }

        if (holder instanceof PlayerPickerHolder picker) {
            event.setCancelled(true);
            if (event.getClickedInventory() != top) return;
            if (!(event.getWhoClicked() instanceof Player viewer)) return;

            UUID targetId = picker.playerAt(event.getSlot());
            if (targetId != null) {
                Player target = Bukkit.getPlayer(targetId);
                if (target == null || !target.isOnline()) {
                    viewer.sendMessage(ChatColor.RED + "That player just went offline -- try again.");
                    return;
                }
                playClick(viewer);
                if (picker.onSelect() != null) {
                    picker.onSelect().accept(target);
                }
                return;
            }
            if (picker.onBack() != null) {
                playClick(viewer);
                picker.onBack().run();
            }
            return;
        }

        if (holder instanceof TextPromptHolder prompt) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null || event.getClickedInventory() != top) return;
            if (event.getSlot() != 2) return; // only the anvil's output slot confirms
            if (!(event.getWhoClicked() instanceof Player viewer)) return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            String typed = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            boolean blank = typed == null || typed.isBlank() || typed.equals(prompt.placeholder());
            if (blank && !prompt.allowBlank()) {
                viewer.sendMessage(ChatColor.RED + "Type something first, then click it to confirm.");
                return;
            }
            playClick(viewer);
            viewer.closeInventory();
            if (prompt.onConfirm() != null) {
                prompt.onConfirm().accept(blank ? "" : typed);
            }
        }
    }

    /** The drag/paint gesture bypasses InventoryClickEvent entirely, so without this a player
     *  could drop a real item into one of these throwaway virtual menus and have it silently
     *  discarded the next time the menu is rebuilt/reopened. Every menu inventory this plugin
     *  ever creates is one of these three holder types, so cancelling the drag whenever the top
     *  inventory is one of them is always correct here. */
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Object holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof ActionMenuHolder || holder instanceof PlayerPickerHolder || holder instanceof TextPromptHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getInventory().getHolder() instanceof TextPromptHolder prompt)) return;

        AnvilInventory anvil = event.getInventory();
        anvil.setRepairCost(0);

        ItemStack seed = anvil.getItem(0);
        if (seed == null) {
            event.setResult(null);
            return;
        }

        String typed = anvil.getRenameText();
        ItemStack result = seed.clone();
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + (typed == null || typed.isBlank() ? prompt.placeholder() : typed));
        result.setItemMeta(meta);
        event.setResult(result);
    }

    /** Without this, closing a text-prompt anvil without confirming would leave the player
     *  holding a stray "Type here..." paper -- so any left in a player's own inventory get swept
     *  up one tick after any of our anvils close. */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TextPromptHolder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack stack = contents[i];
                if (stack == null || !stack.hasItemMeta()) continue;
                Boolean isSeed = stack.getItemMeta().getPersistentDataContainer()
                        .get(TextPromptHolder.seedTag(plugin), PersistentDataType.BOOLEAN);
                if (Boolean.TRUE.equals(isSeed)) {
                    player.getInventory().setItem(i, null);
                }
            }
        });
    }

    private void playClick(org.bukkit.entity.HumanEntity who) {
        if (!(who instanceof Player player)) return;
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
        } catch (Throwable ignored) {
            // Purely a nice-to-have; never let a missing/renamed sound break navigation.
        }
    }
}
