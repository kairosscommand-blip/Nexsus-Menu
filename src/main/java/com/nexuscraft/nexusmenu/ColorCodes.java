package com.nexuscraft.nexusmenu;

/** Translates the '&amp;' color-code shorthand config.yml uses (e.g. "&amp;b&amp;lTitle") into
 *  the real section-sign codes Minecraft actually uses. Same convention as every other plugin in
 *  this family -- see NexusStarter's identical ColorCodes class. */
final class ColorCodes {

    private ColorCodes() {
    }

    static String translate(String raw) {
        return raw == null ? null : raw.replace('&', '§');
    }
}
