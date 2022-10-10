package xzot1k.plugins.hd.core.packets.particles.versions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.core.packets.particles.ParticleHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PH_Old implements ParticleHandler {
    private Class<?> particlePacketClass, enumParticleClass, cpClass, packetClass;

    public PH_Old() {
        try {
            packetClass = Class.forName("net.minecraft.server."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".Packet");

            particlePacketClass = Class.forName("net.minecraft.server."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".PacketPlayOutWorldParticles");

            enumParticleClass = Class.forName("net.minecraft.server."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".EnumParticle");

            cpClass = Class.forName("org.bukkit.craftbukkit."
                    + HyperDrive.getPluginInstance().getServerVersion() + ".entity.CraftPlayer");
        } catch (NoClassDefFoundError | ClassNotFoundException e) {e.printStackTrace();}
    }

    @Override
    public void displayParticle(@NotNull String particleName, @NotNull Location location, int offsetX, int offsetY,
                                int offsetZ, int red, int green, int blue, int brightness, int speed, int amount) {
        try {
            final Method valueOf = enumParticleClass.getDeclaredMethod("valueOf", String.class);
            final Object particle = valueOf.invoke(enumParticleClass, particleName);

            for (Constructor<?> cons : particlePacketClass.getDeclaredConstructors()) {
                if (cons.getParameterCount() >= 9) {
                    final Object packet = cons.newInstance(particle, false, (float) location.getX(), (float) location.getY(),
                            (float) location.getZ(), offsetX, offsetY, offsetZ, (float) speed, amount, null);
                    send(packet);
                    break;
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                 | InstantiationException e) {e.printStackTrace();}
    }

    @Override
    public void displayParticle(@NotNull String particleName, @NotNull Location location, int offsetX,
                                int offsetY, int offsetZ, int speed, int amount) {
        try {
            final Method valueOf = enumParticleClass.getDeclaredMethod("valueOf", String.class);
            final Object particle = valueOf.invoke(enumParticleClass, particleName);

            for (Constructor<?> cons : particlePacketClass.getDeclaredConstructors()) {
                if (cons.getParameterCount() >= 9) {
                    final Object packet = cons.newInstance(particle, false, (float) location.getX(), (float) location.getY(),
                            (float) location.getZ(), offsetX, offsetY, offsetZ, (float) speed, amount, null);
                    send(packet);
                    break;
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                 | InstantiationException e) {e.printStackTrace();}
    }

    private void send(@NotNull Object packet) {
        try {
            for (Player player : HyperDrive.getPluginInstance().getServer().getOnlinePlayers()) {
                final Object cPlayer = cpClass.cast(player);
                final Object getHandle = cpClass.getDeclaredMethod("getHandle").invoke(cPlayer);
                final Object pConnection = getHandle.getClass().getDeclaredField("playerConnection").get(getHandle);
                final Method sendPacket = pConnection.getClass().getDeclaredMethod("sendPacket", packetClass);
                sendPacket.invoke(pConnection, packet);
            }
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException
                 | NoSuchFieldException e) {e.printStackTrace();}
    }

}
