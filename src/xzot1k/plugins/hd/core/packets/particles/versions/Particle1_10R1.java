/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.hd.core.packets.particles.versions;

import net.minecraft.server.v1_10_R1.EnumParticle;
import net.minecraft.server.v1_10_R1.PacketPlayOutWorldParticles;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xzot1k.plugins.hd.core.packets.particles.ParticleHandler;

import java.util.ArrayList;
import java.util.List;

public class Particle1_10R1 implements ParticleHandler
{
    @Override
    public void displayParticle(String particleName, Location location, int offsetX, int offsetY, int offsetZ, int red, int green, int blue, int brightness, int speed, int amount) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.valueOf(particleName), true,
                (float) location.getX(), (float) location.getY(), (float) location.getZ(),
                (float) red, (float) green, (float) blue, (float) brightness, speed, amount);
        List<Player> playerList = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (int i = -1; ++i < playerList.size(); )
            ((CraftPlayer) playerList.get(i)).getHandle().playerConnection.sendPacket(packet);
    }

    @Override
    public void displayParticle(String particleName, Location location, int offsetX, int offsetY, int offsetZ, int speed, int amount) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.valueOf(particleName), false,
                (float) location.getX(), (float) location.getY(), (float) location.getZ(), offsetX, offsetY, offsetZ, speed, amount);
        List<Player> playerList = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (int i = -1; ++i < playerList.size(); )
            ((CraftPlayer) playerList.get(i)).getHandle().playerConnection.sendPacket(packet);
    }
}
