/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.hd.core.packets.jsonmsgs;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface JSONHandler {
    void sendJSONMessage(Player player, String JSONString);

    String getJSONItem(ItemStack itemStack);
}
