/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.core.packets.actionbars.versions;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.core.packets.actionbars.ActionBarHandler;

public class ABH_Latest implements ActionBarHandler {
    @Override
    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(HyperDrive.getPluginInstance().getManager().colorText(message)));
    }

}
