package com.nexuscraft.nexusmenu;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Tracks which players have already received the physical menu item -- a plain in-memory set,
 *  persisted to {@code item_claims.yml}. Same one-time-per-player-ever shape as NexusStarter's
 *  GuideItemClaimRegistry: a brand-new player claims it automatically on first join, and an
 *  existing player (who joined before this plugin was installed) claims it automatically the
 *  very next time they join, or immediately via {@code /menu item} if already online. */
final class MenuItemClaimRegistry {

    private final Set<UUID> claimed = new HashSet<>();

    boolean claim(UUID uuid) {
        return claimed.add(uuid);
    }

    boolean hasClaimed(UUID uuid) {
        return claimed.contains(uuid);
    }

    boolean reset(UUID uuid) {
        return claimed.remove(uuid);
    }

    Set<UUID> all() {
        return Set.copyOf(claimed);
    }

    void loadAll(Iterable<UUID> uuids) {
        claimed.clear();
        for (UUID uuid : uuids) {
            claimed.add(uuid);
        }
    }
}
