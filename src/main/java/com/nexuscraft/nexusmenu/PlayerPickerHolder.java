package com.nexuscraft.nexusmenu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * "Pick an online player" sub-menu -- one player head per online player, click one to run
 * onSelect against them. Used by every PLAYER/PLAYER_TEXT entry instead of asking the player to
 * type a name. Same shape as NexusAdmin's PlayerPickerHolder.
 */
final class PlayerPickerHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, UUID> slotPlayers = new HashMap<>();
    private Consumer<Player> onSelect;
    private Runnable onBack;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    void putPlayer(int slot, UUID playerId) {
        slotPlayers.put(slot, playerId);
    }

    UUID playerAt(int slot) {
        return slotPlayers.get(slot);
    }

    void setOnSelect(Consumer<Player> onSelect) {
        this.onSelect = onSelect;
    }

    Consumer<Player> onSelect() {
        return onSelect;
    }

    void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    Runnable onBack() {
        return onBack;
    }
}
