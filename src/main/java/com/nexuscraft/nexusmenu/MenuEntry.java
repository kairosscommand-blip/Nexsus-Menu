package com.nexuscraft.nexusmenu;

import org.bukkit.Material;

import java.util.List;

/**
 * One button inside a category's menu. Every type ends the same way -- a single
 * {@code Bukkit.dispatchCommand(player, finalCommand)} call, built up out of the command prefix
 * plus whatever the player picked/typed -- except {@link Type#INFO}, which never dispatches
 * anything and exists purely to show lore (used for the one honest exception in this whole menu:
 * NexusSurvival's disease cure has no player-facing command at all).
 */
final class MenuEntry {

    enum Type {
        /** No further input needed -- dispatches {@link #command} as-is. */
        COMMAND,
        /** One or more chained free-text anvil prompts, appended in order after {@link #commandPrefix}. */
        TEXT,
        /** An online-player picker; the picked name is appended after {@link #commandPrefix}
         *  (and before {@link #commandSuffix}, if set). */
        PLAYER,
        /** A player picker followed by one or more chained free-text prompts. */
        PLAYER_TEXT,
        /** Pure information -- clicking it does nothing but play a sound; no command exists. */
        INFO
    }

    final String label;
    final Type type;
    final String command;
    final String commandPrefix;
    final String commandSuffix;
    final Material icon;
    final List<String> lore;
    final List<PromptDef> prompts;

    MenuEntry(String label, Type type, String command, String commandPrefix, String commandSuffix,
              Material icon, List<String> lore, List<PromptDef> prompts) {
        this.label = label;
        this.type = type;
        this.command = command;
        this.commandPrefix = commandPrefix;
        this.commandSuffix = commandSuffix;
        this.icon = icon;
        this.lore = lore;
        this.prompts = prompts;
    }
}
