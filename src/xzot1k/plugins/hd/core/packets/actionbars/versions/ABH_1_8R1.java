package xzot1k.plugins.hd.core.packets.actionbars.versions;

import net.minecraft.server.v1_8_R1.ChatSerializer;
import net.minecraft.server.v1_8_R1.IChatBaseComponent;
import net.minecraft.server.v1_8_R1.PacketPlayOutChat;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xzot1k.plugins.hd.core.packets.actionbars.ActionBarHandler;

public class ABH_1_8R1 implements ActionBarHandler
{
    @Override

    public void sendActionBar(Player player, String message)
    {
        IChatBaseComponent iChatBaseComponent = ChatSerializer.a("{\"text\": \""
                + ChatColor.translateAlternateColorCodes('&', message) + "\"}");
        PacketPlayOutChat bar = new PacketPlayOutChat(iChatBaseComponent, (byte) 2);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(bar);
    }

}
