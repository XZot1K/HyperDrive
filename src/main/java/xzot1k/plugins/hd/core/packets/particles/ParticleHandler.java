/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.hd.core.packets.particles;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface ParticleHandler {
    void displayParticle(@NotNull String particleName, @NotNull Location location, int offsetX, int offsetY,
                         int offsetZ, int red, int green, int blue, int brightness, int speed, int amount);

    void displayParticle(@NotNull String particleName, @NotNull Location location, int offsetX,
                         int offsetY, int offsetZ, int speed, int amount);
}
