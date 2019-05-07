package xzot1k.plugins.hd.core.packets.titles.versions;

import net.minecraft.server.v1_8_R1.ChatSerializer;
import net.minecraft.server.v1_8_R1.EnumTitleAction;
import net.minecraft.server.v1_8_R1.PacketPlayOutTitle;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.core.packets.titles.TitleHandler;

public class Titles1_8R1 implements TitleHandler
{
    private HyperDrive pluginInstance;

    public Titles1_8R1(HyperDrive pluginInstance)
    {
        setPluginInstance(pluginInstance);
    }

    @Override
    public void sendTitle(Player player, String text, int fadeIn, int displayTime, int fadeout)
    {
        PacketPlayOutTitle title = new PacketPlayOutTitle(EnumTitleAction.TITLE, ChatSerializer.a("{\"text\":\""
                + getPluginInstance().getManager().colorText(text) + "\"}"), fadeIn * 20, displayTime * 20, fadeout * 20);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(title);
    }

    @Override
    public void sendSubTitle(Player player, String text, int fadeIn, int displayTime, int fadeout)
    {
        PacketPlayOutTitle title = new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, ChatSerializer.a("{\"text\":\""
                + getPluginInstance().getManager().colorText(text) + "\"}"), fadeIn * 20, displayTime * 20, fadeout * 20);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(title);
    }

    @Override
    public void sendTitle(Player player, String title, String subTitle, int fadeIn, int displayTime, int fadeOut)
    {
        sendTitle(player, title, fadeIn * 20, displayTime * 20, fadeOut * 20);
        sendSubTitle(player, subTitle, fadeIn * 20, displayTime * 20, fadeOut * 20);
    }

    private HyperDrive getPluginInstance()
    {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance)
    {
        this.pluginInstance = pluginInstance;
    }
}
