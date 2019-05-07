package xzot1k.plugins.hd.core.packets.particles;

import org.bukkit.Location;

public interface ParticleHandler
{
    void displayParticle(String particleName, Location location, int offsetX, int offsetY, int offsetZ, int red, int green, int blue, int brightness, int speed, int amount);

    void displayParticle(String particleName, Location location, int offsetX, int offsetY, int offsetZ, int speed, int amount);
}
