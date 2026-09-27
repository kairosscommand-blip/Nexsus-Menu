package com.nexuscraft.nexusmenu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;

/**
 * Makes the physical menu item behave like a bound/soulbound quest item rather than an ordinary
 * one, per the plugin's design: a player should never be able to drop it, give it away, feed it
 * into a container/crafting grid/anvil, or lose it to death. Every rule here is deliberately
 * simple and a little conservative rather than exhaustively covering every vanilla mechanic
 * (item frames, lecterns, and the like aren't specifically handled) -- the goal is "you can't
 * casually lose this," not "this is cryptographically un-loseable."
 *
 * <p>The core rule for clicks/drags: while the player's own inventory screen is the ONLY thing
 * open (Bukkit represents that as a top inventory of {@link InventoryType#CRAFTING}), the item
 * can be freely rearranged within the player's own main inventory/hotbar, but never placed into
 * the 2x2 crafting grid itself (slots belonging to that top inventory). The moment any other
 * inventory is open at all -- a chest, furnace, anvil, another plugin's GUI, anything -- every
 * click or drag that touches the item is blocked outright, which is simple, safe, and covers
 * every "move it into a foreign container" case without having to enumerate them individually.
 */
final class MenuItemLockListener implements Listener {

    private final JavaPlugin plugin;
    private final MenuConfig config;
    private final MenuItemClaimRegistry claims;
    private final Runnable onClaimsChanged;

    MenuItemLockListener(JavaPlugin plugin, MenuConfig config, MenuItemClaimRegistry claims, Runnable onClaimsChanged) {
        this.plugin = plugin;
        this.config = config;
        this.claims = claims;
        this.onClaimsChanged = onClaimsChanged;
    }

    private boolean isMenuItem(ItemStack item) {
        return MenuItemFactory.isMenuItem(plugin, item);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!config.lockInInventory) return;
        if (isMenuItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ColorCodes.translate("&7[NexusMenu] &fYour Nexus Codex is bound to you and can't be dropped."));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!config.lockInInventory) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        boolean involvesMenuItem = isMenuItem(event.getCurrentItem()) || isMenuItem(event.getCursor());
        if (!involvesMenuItem) {
            int hotbarSlot = event.getHotbarButton();
            if (hotbarSlot >= 0) {
                involvesMenuItem = isMenuItem(player.getInventory().getItem(hotbarSlot));
            }
        }
        // The F-key offhand swap moves whatever is currently in the offhand into the clicked
        // slot (which can be inside an open chest/anvil/villager-trade/etc. GUI) -- if that's the
        // menu item, this needs to be treated as "involves the menu item" exactly like any other
        // click, or it sails straight through the checks below untouched.
        if (!involvesMenuItem && event.getClick() == ClickType.SWAP_OFFHAND) {
            involvesMenuItem = isMenuItem(player.getInventory().getItemInOffHand());
        }
        if (!involvesMenuItem) return;

        Inventory top = event.getView().getTopInventory();
        boolean onlyOwnScreenOpen = top.getType() == InventoryType.CRAFTING;
        if (!onlyOwnScreenOpen) {
            // A real foreign inventory (chest, furnace, anvil, another plugin's GUI, ...) is
            // open. Freeze the item completely rather than trying to reason about which specific
            // click/shift-click/hotbar-swap would actually move it there.
            event.setCancelled(true);
            return;
        }
        // Only the player's own inventory screen is open -- allow rearranging within it, but
        // never let it land in the 2x2 crafting grid (part of that same top "CRAFTING"
        // inventory), and never let a click outside the window bounds (clickedInventory == null)
        // drop a cursor-held copy on the ground.
        if (event.getClickedInventory() == null || event.getClickedInventory() == top) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!config.lockInInventory) return;
        if (!isMenuItem(event.getOldCursor())) return;

        Inventory top = event.getView().getTopInventory();
        boolean onlyOwnScreenOpen = top.getType() == InventoryType.CRAFTING;
        if (!onlyOwnScreenOpen) {
            event.setCancelled(true);
            return;
        }
        int topSize = top.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (!config.keepOnDeath) return;
        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            if (isMenuItem(it.next())) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!config.keepOnDeath) return;
        Player player = event.getPlayer();
        // One tick later: handing out items during the respawn event itself is unreliable on
        // some versions (the new inventory view isn't always settled yet).
        Bukkit.getScheduler().runTask(plugin, () -> ensureCarrying(player));
    }

    /** Silently tops the player back up to exactly one menu item if they don't currently have
     *  one -- covers the death/respawn path above, and is a safe no-op the rest of the time.
     *  Also marks their first-join claim (if not already marked): a brand-new player can die and
     *  respawn during the {@code first-join.delay-seconds} window before MenuListener's own
     *  delayed first-join grant has fired, and without this, that still-pending grant would hand
     *  them a second item once its delay elapses, since it only checks the claim registry, not
     *  the player's actual inventory. */
    void ensureCarrying(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isMenuItem(stack)) {
                markClaimed(player);
                return;
            }
        }
        if (isMenuItem(player.getInventory().getItemInOffHand())) {
            markClaimed(player);
            return;
        }
        markClaimed(player);
        player.getInventory().addItem(config.buildMenuItem(plugin));
    }

    private void markClaimed(Player player) {
        if (claims.claim(player.getUniqueId())) {
            onClaimsChanged.run();
        }
    }
}
