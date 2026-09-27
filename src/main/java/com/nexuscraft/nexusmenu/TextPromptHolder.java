package com.nexuscraft.nexusmenu;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * A virtual anvil used purely as a text-input box: a disposable "seed" item sits in the input
 * slot, the player types into the rename field, and clicking the (renamed) output item confirms
 * it. See MenuListener for the PrepareAnvilEvent/InventoryClickEvent handling that makes this
 * work. Same shape and trick as NexusAdmin's TextPromptHolder.
 */
final class TextPromptHolder implements InventoryHolder {

    private Inventory inventory;
    private final String placeholder;
    private final Consumer<String> onConfirm;
    private final boolean allowBlank;

    TextPromptHolder(String placeholder, boolean allowBlank, Consumer<String> onConfirm) {
        this.placeholder = placeholder;
        this.allowBlank = allowBlank;
        this.onConfirm = onConfirm;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    String placeholder() {
        return placeholder;
    }

    boolean allowBlank() {
        return allowBlank;
    }

    Consumer<String> onConfirm() {
        return onConfirm;
    }

    static NamespacedKey seedTag(Plugin plugin) {
        return new NamespacedKey(plugin, "menu_prompt_seed");
    }
}
