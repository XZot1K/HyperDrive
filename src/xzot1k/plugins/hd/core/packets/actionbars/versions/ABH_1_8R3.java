/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.core.packets.actionbars.versions;

import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.core.packets.actionbars.ActionBarHandler;

public class ABH_1_8R3 implements ActionBarHandler {
    @Override
    public void sendActionBar(Player player, String message) {
        IChatBaseComponent iChatBaseComponent = IChatBaseComponent.ChatSerializer.a("{\"text\": \""
                + HyperDrive.getPluginInstance().getManager().colorText(message) + "\"}");
        PacketPlayOutChat bar = new PacketPlayOutChat(iChatBaseComponent, (byte) 2);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(bar);
    }

}
