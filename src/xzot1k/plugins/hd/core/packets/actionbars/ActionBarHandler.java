/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.hd.core.packets.actionbars;

import org.bukkit.entity.Player;

public interface ActionBarHandler {
    void sendActionBar(Player player, String message);
}
