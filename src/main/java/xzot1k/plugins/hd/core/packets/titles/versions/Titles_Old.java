/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.hd.core.packets.titles.versions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.core.packets.titles.TitleHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Titles_Old implements TitleHandler {

    private Class<?> cbcClass, csClass, titlePacketClass, etaClass, cpClass, packetClass;

    public Titles_Old() {
        try {
            cbcClass = Class.forName("net.minecraft.server."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".IChatBaseComponent");

            csClass = Class.forName("net.minecraft.server."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".IChatBaseComponent$ChatSerializer");

            packetClass = Class.forName("net.minecraft.server."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".Packet");

            titlePacketClass = Class.forName("net.minecraft.server."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".PacketPlayOutTitle");

            etaClass = Class.forName("net.minecraft.server."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".PacketPlayOutTitle$EnumTitleAction");

            cpClass = Class.forName("org.bukkit.craftbukkit."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".entity.CraftPlayer");
        } catch (NoClassDefFoundError | ClassNotFoundException e) {e.printStackTrace();}
    }

    @Override
    public void sendTitle(Player player, String text, int fadeIn, int displayTime, int fadeout) {
        send(player, "TITLE", text, fadeIn, displayTime, fadeout);
    }

    @Override
    public void sendSubTitle(Player player, String text, int fadeIn, int displayTime, int fadeout) {
        send(player, "SUBTITLE", text, fadeIn, displayTime, fadeout);
    }

    private void send(@NotNull Player player, @NotNull String action, @NotNull String text, int fadeIn, int displayTime, int fadeOut) {
        try {
            final Object titleAction = etaClass.getDeclaredField(action).get(null);

            final Method aMethod = csClass.getDeclaredMethod("a", String.class);
            final Object textField = aMethod.invoke(csClass, "{\"text\":\""
                    + HyperDrive.getPluginInstance().getManager().colorText(text) + "\"}");


            Constructor<?> pConst = null;
            for (Constructor<?> con : titlePacketClass.getConstructors()) {

                if (con.getParameterTypes().length != 5) continue;

                if (con.getParameterTypes()[0] != etaClass || con.getParameterTypes()[1] != cbcClass
                        && con.getParameterTypes()[2] != int.class
                        && con.getParameterTypes()[3] != int.class
                        && con.getParameterTypes()[4] != int.class) continue;

                pConst = con;
                break;
            }

            if (pConst == null) {
                for (Constructor<?> con : titlePacketClass.getConstructors()) {

                    if (con.getParameterTypes().length != 5) continue;

                    if (con.getParameterTypes()[0] != etaClass || con.getParameterTypes()[1] != String.class
                            && con.getParameterTypes()[2] != int.class
                            && con.getParameterTypes()[3] != int.class
                            && con.getParameterTypes()[4] != int.class) continue;

                    pConst = con;
                    break;
                }
            }

            if (pConst == null) return;

            final Object packet = pConst.newInstance(titleAction, textField, (fadeIn * 20), (displayTime * 20), (fadeOut * 20));

            final Object cPlayer = cpClass.cast(player);
            final Object getHandle = cpClass.getDeclaredMethod("getHandle").invoke(cPlayer);
            final Object pConnection = getHandle.getClass().getDeclaredField("playerConnection").get(getHandle);
            final Method sendPacket = pConnection.getClass().getDeclaredMethod("sendPacket", packetClass);
            sendPacket.invoke(pConnection, packet);
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | InstantiationException e) {e.printStackTrace();}
    }

    @Override
    public void sendTitle(Player player, String title, String subTitle, int fadeIn, int displayTime, int fadeOut) {
        sendTitle(player, title, fadeIn * 20, displayTime * 20, fadeOut * 20);
        sendSubTitle(player, subTitle, fadeIn * 20, displayTime * 20, fadeOut * 20);
    }

}
