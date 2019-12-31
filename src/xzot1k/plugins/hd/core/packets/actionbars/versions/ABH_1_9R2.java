/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.hd.core.packets.actionbars.versions;

import net.minecraft.server.v1_9_R2.IChatBaseComponent;
import net.minecraft.server.v1_9_R2.PacketPlayOutChat;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xzot1k.plugins.hd.core.packets.actionbars.ActionBarHandler;

public class ABH_1_9R2 implements ActionBarHandler {
    @Override
    public void sendActionBar(Player player, String message) {
        IChatBaseComponent iChatBaseComponent = IChatBaseComponent.ChatSerializer.a("{\"text\": \""
                + ChatColor.translateAlternateColorCodes('&', message) + "\"}");
        PacketPlayOutChat bar = new PacketPlayOutChat(iChatBaseComponent, (byte) 2);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(bar);
    }

}
