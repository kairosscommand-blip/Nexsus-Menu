package com.nexuscraft.nexusmenu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * A generic clickable-button chest menu: every button is just a slot index mapped to a Runnable.
 * Used for the root category menu and every per-plugin category submenu -- none of them need
 * anything more specific than "this slot does this thing when clicked." Same shape as
 * NexusAdmin's ActionMenuHolder.
 */
final class ActionMenuHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, Runnable> actions = new HashMap<>();

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    void setAction(int slot, Runnable action) {
        actions.put(slot, action);
    }

    Runnable actionFor(int slot) {
        return actions.get(slot);
    }
}
