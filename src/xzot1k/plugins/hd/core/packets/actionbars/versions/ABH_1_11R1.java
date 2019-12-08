/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.hd.core.packets.actionbars.versions;

import net.minecraft.server.v1_11_R1.IChatBaseComponent;
import net.minecraft.server.v1_11_R1.PacketPlayOutTitle;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xzot1k.plugins.hd.core.packets.actionbars.ActionBarHandler;

public class ABH_1_11R1 implements ActionBarHandler
{
    @Override
    public void sendActionBar(Player player, String message) {
        PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.ACTIONBAR, IChatBaseComponent.ChatSerializer
                .a("{\"text\": \"" + ChatColor.translateAlternateColorCodes('&', message) + "\"}"));
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(title);
    }

}
