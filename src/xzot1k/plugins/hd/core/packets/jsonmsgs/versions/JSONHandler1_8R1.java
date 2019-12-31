/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.hd.core.packets.jsonmsgs.versions;

import net.minecraft.server.v1_8_R1.ChatSerializer;
import net.minecraft.server.v1_8_R1.IChatBaseComponent;
import net.minecraft.server.v1_8_R1.NBTTagCompound;
import net.minecraft.server.v1_8_R1.PacketPlayOutChat;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xzot1k.plugins.hd.core.packets.jsonmsgs.JSONHandler;

public class JSONHandler1_8R1 implements JSONHandler {
    @Override
    public void sendJSONMessage(Player player, String JSONString) {
        IChatBaseComponent comp = ChatSerializer.a(JSONString);
        PacketPlayOutChat packetPlayOutChat = new PacketPlayOutChat(comp);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetPlayOutChat);
    }

    @Override
    public String getJSONItem(ItemStack itemStack) {
        net.minecraft.server.v1_8_R1.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound compound = new NBTTagCompound();
        compound = nmsItemStack.save(compound);
        return compound.toString();
    }
}
