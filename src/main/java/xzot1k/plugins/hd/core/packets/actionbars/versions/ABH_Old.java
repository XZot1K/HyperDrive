/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.hd.core.packets.actionbars.versions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.core.packets.actionbars.ActionBarHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ABH_Old implements ActionBarHandler {
    private Class<?> icbClass, csClass, packetClass, packetChatClass, craftPlayerClass;

    public ABH_Old() {
        try {
            icbClass = Class.forName("net.minecraft.server."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".IChatBaseComponent");

            csClass = Class.forName("net.minecraft.server."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".IChatBaseComponent$ChatSerializer");

            packetClass = Class.forName("net.minecraft.server."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".Packet");

            packetChatClass = Class.forName("net.minecraft.server."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".PacketPlayOutChat");

            craftPlayerClass = Class.forName("org.bukkit.craftbukkit."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".entity.CraftPlayer");
        } catch (ClassNotFoundException e) {e.printStackTrace();}
    }

    @Override
    public void sendActionBar(@NotNull Player player, @NotNull String message) {
        try {
            final Method method = csClass.getDeclaredMethod("a", String.class);
            final Object icbc = method.invoke(csClass, ("{\"text\": \""
                    + HyperDrive.getPluginInstance().getManager().colorText(message) + "\"}"));

            final Constructor<?> packetConstructor = packetChatClass.getConstructor(icbClass, Byte.class);
            final Object packet = packetConstructor.newInstance(icbc, (byte) 2);

            final Object cPlayer = craftPlayerClass.cast(player);
            final Object getHandle = craftPlayerClass.getDeclaredMethod("getHandle").invoke(cPlayer);
            final Object pConnection = getHandle.getClass().getDeclaredField("playerConnection").get(getHandle);
            final Method sendPacket = pConnection.getClass().getDeclaredMethod("sendPacket", packetClass);
            sendPacket.invoke(pConnection, packet);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                 | InstantiationException | NoSuchFieldException e) {e.printStackTrace();}
    }

}
