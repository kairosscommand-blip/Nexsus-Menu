package com.nexuscraft.nexusmenu;

/** One step of a (possibly chained) free-text anvil prompt -- see {@link MenuEntry}. */
final class PromptDef {
    final String title;
    final boolean optional;

    PromptDef(String title, boolean optional) {
        this.title = title;
        this.optional = optional;
    }
}
