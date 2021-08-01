/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.api;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.events.RandomTeleportEvent;
import xzot1k.plugins.hd.api.objects.SerializableLocation;
import xzot1k.plugins.hd.core.internals.hooks.HookChecker;
import xzot1k.plugins.hd.core.objects.Destination;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class RandomTeleportation implements Runnable {

    private HyperDrive pluginInstance;
    private List<String> biomeBlackList;
    private int attempts, maxAttempts;
    private long boundsRadius;
    private boolean onlyUpdateDestination;
    private String teleportSound;

    private Player player;
    private Location baseLocation;

    public RandomTeleportation(HyperDrive pluginInstance, World world, Player player, boolean onlyUpdateDestination) {
        setPluginInstance(pluginInstance);
        setPlayer(player);
        setAttempts(0);
        setOnlyUpdateDestination(onlyUpdateDestination);
        setBiomeBlackList(getPluginInstance().getConfig().getStringList("random-teleport-section.biome-blacklist"));
        setMaxAttempts(getPluginInstance().getConfig().getInt("random-teleport-section.max-tries"));
        setTeleportSound(Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                .toUpperCase().replace(" ", "_").replace("-", "_"));
        setAttempts(0);

        final String customBorderString = getCustomBorderString(world);
        if (customBorderString != null && customBorderString.contains(":")) {
            final String[] borderArgs = customBorderString.split(":");
            if (borderArgs.length >= 3) {
                final String size = borderArgs[1], center = borderArgs[2];
                if (!getPluginInstance().getManager().isNotNumeric(size) && center.contains(",")) {
                    final String[] centerArgs = center.split(",");
                    final long foundSize = Long.parseLong(size);
                    if (!getPluginInstance().getManager().isNotNumeric(centerArgs[0])
                            && !getPluginInstance().getManager().isNotNumeric(centerArgs[1])) {
                        final int x = Integer.parseInt(centerArgs[0]), z = Integer.parseInt(centerArgs[1]);
                        setBoundsRadius(foundSize);
                        setBaseLocation(new Location(world, x, 0, z));
                        return;
                    }
                }
            }
        }

        WorldBorder worldBorder = world.getWorldBorder();
        setBoundsRadius((long) worldBorder.getSize());
        if ((worldBorder.getSize() / 2) < getBoundsRadius()) setBoundsRadius((int) (worldBorder.getSize() / 2));
        setBaseLocation(worldBorder.getCenter());
    }

    @Override
    public void run() {

        Location newLocation = null;
        boolean foundLocation = false;

        while ((getMaxAttempts() >= 0 && getAttempts() < getMaxAttempts())) {
            setAttempts(getAttempts() + 1);

            int minX = (int) (getBaseLocation().getBlockX() - getBoundsRadius()), maxX = (int) (getBaseLocation().getBlockX() + getBoundsRadius()),
                    minZ = (int) (getBaseLocation().getBlockZ() - getBoundsRadius()), maxZ = (int) (getBaseLocation().getBlockZ() + getBoundsRadius()),
                    xAddition = getPluginInstance().getTeleportationHandler().getRandomInRange(minX, maxX),
                    zAddition = getPluginInstance().getTeleportationHandler().getRandomInRange(minZ, maxZ),
                    x = (int) (getBaseLocation().getX() + xAddition), z = (int) (getBaseLocation().getZ() + zAddition);

            if (x >= getBoundsRadius() || z >= getBoundsRadius() || x <= -getBoundsRadius() || z <= -getBoundsRadius()
                    || new SerializableLocation(getBaseLocation()).distance(x, 0, z) < (getBoundsRadius() * 0.1))
                continue;

            Chunk chunk;
            try {
                chunk = getPluginInstance().getManager().getChunk(getBaseLocation().getWorld(), x, z).get();
            } catch (Exception e) {
                getPluginInstance().log(Level.WARNING, "The random teleportation task was unable to obtain a chunk."
                        + " Don't worry, I caught it before it caused further issues! Gonna try again...");
                continue;
            }

            if (chunk == null) continue;
            int highestY = getHighestY(Objects.requireNonNull(getBaseLocation().getWorld()), x, z);
            if (highestY <= 0) continue;

            final Block foundBlock = getBaseLocation().getWorld().getBlockAt(x, highestY, z);
            boolean isBlockedBiome = false;
            if (!getBiomeBlackList().isEmpty()) for (String biomeName : getBiomeBlackList())
                if (foundBlock.getBiome().name().contains(biomeName.toUpperCase().replace(" ", "_").replace("-", "_"))) {
                    isBlockedBiome = true;
                    break;
                }

            if (isBlockedBiome || !getPluginInstance().getHookChecker().isLocationHookSafe(getPlayer(), foundBlock.getLocation().clone(), HookChecker.CheckType.RTP)
                    || getPluginInstance().getManager().isForbiddenMaterial(foundBlock.getType(), foundBlock.getData()))
                continue;

            Block relativeUp = foundBlock.getRelative(BlockFace.UP);
            if (!relativeUp.getType().name().contains("AIR") || !relativeUp.getRelative(BlockFace.UP).getType().name().contains("AIR"))
                continue;

            newLocation = new Location(getBaseLocation().getWorld(), x + 0.5, highestY + 2, z + 0.5,
                    getPlayer().getLocation().getYaw(), getPlayer().getLocation().getPitch());
            foundLocation = true;
            break;
        }

        if (foundLocation) {
            final Location finalNewLocation = newLocation;
            getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () -> {
                getPluginInstance().getTeleportationHandler().getRandomTeleportingPlayers().remove(getPlayer().getUniqueId());
                getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(getPlayer());

                RandomTeleportEvent randomTeleportEvent = new RandomTeleportEvent(finalNewLocation, getPlayer());
                getPluginInstance().getServer().getPluginManager().callEvent(randomTeleportEvent);
                if (randomTeleportEvent.isCancelled()) return;

                if (isOnlyUpdateDestination()) {
                    getPluginInstance().getTeleportationHandler().updateDestination(player, new Destination(getPluginInstance(), finalNewLocation));
                    return;
                }

                getPluginInstance().getTeleportationHandler().teleportPlayer(getPlayer(), finalNewLocation);

                if (!getPlayer().hasPermission("hyperdrive.rtpbypass"))
                    getPluginInstance().getManager().updateCooldown(getPlayer(), "rtp");

                String animationLine = getPluginInstance().getConfig().getString("special-effects-section.random-teleport-animation");
                if (animationLine != null && animationLine.contains(":")) {
                    String[] animationArgs = animationLine.split(":");
                    if (animationArgs.length >= 2) {
                        EnumContainer.Animation animation = EnumContainer.Animation.valueOf(animationArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"));
                        getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(getPlayer(), animationArgs[1].toUpperCase().replace(" ", "_").replace("-", "_"), animation, 1);
                    }
                }

                if (getTeleportSound() != null && !getTeleportSound().equalsIgnoreCase(""))
                    Objects.requireNonNull(finalNewLocation.getWorld()).playSound(finalNewLocation, Sound.valueOf(getTeleportSound()), 1, 1);

                getPluginInstance().getManager().sendCustomMessage("random-teleported", getPlayer(), "{tries}:" + getAttempts(),
                        "{x}:" + finalNewLocation.getBlockX(), "{y}:" + finalNewLocation.getBlockY(), "{z}:" + finalNewLocation.getBlockZ(),
                        "{world}:" + Objects.requireNonNull(finalNewLocation.getWorld()).getName());
            });
        } else {
            getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () ->
                    getPluginInstance().getManager().sendCustomMessage("random-teleport-fail", getPlayer(), "{tries}:" + getAttempts()));
            getPluginInstance().getManager().clearCooldown(getPlayer(), "rtp");
            getPluginInstance().getTeleportationHandler().getRandomTeleportingPlayers().remove(getPlayer().getUniqueId());
        }
    }

    private String getCustomBorderString(World world) {
        for (String line : getPluginInstance().getConfig().getStringList("random-teleport-section.custom-borders"))
            if (line.toLowerCase().startsWith(world.getName().toLowerCase())) return line;
        return null;
    }

    private int getHighestY(World world, double x, double z) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            for (int i = 120; --i > 0; ) {
                final Block block = world.getBlockAt((int) x, i, (int) z);
                if (block.getType().name().contains("AIR")) continue;

                final Block blockOneAbove = world.getBlockAt((int) x, (i + 1), (int) z);
                if (!blockOneAbove.getType().name().contains("AIR")) continue;

                final Block blockTwoAbove = world.getBlockAt((int) x, (i + 2), (int) z);
                if (!blockTwoAbove.getType().name().contains("AIR")) continue;

                return i;
            }
            return -1;
        }

        return world.getHighestBlockYAt((int) x, (int) z);
    }

    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public List<String> getBiomeBlackList() {
        return biomeBlackList;
    }

    private void setBiomeBlackList(List<String> biomeBlackList) {
        this.biomeBlackList = biomeBlackList;
    }

    public int getAttempts() {
        return attempts;
    }

    private void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    private void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    private String getTeleportSound() {
        return teleportSound;
    }

    private void setTeleportSound(String teleportSound) {
        this.teleportSound = teleportSound;
    }

    public long getBoundsRadius() {
        return boundsRadius;
    }

    private void setBoundsRadius(long boundsRadius) {
        this.boundsRadius = boundsRadius;
    }

    public Player getPlayer() {
        return player;
    }

    private void setPlayer(Player player) {
        this.player = player;
    }

    public boolean isOnlyUpdateDestination() {
        return onlyUpdateDestination;
    }

    public void setOnlyUpdateDestination(boolean onlyUpdateDestination) {
        this.onlyUpdateDestination = onlyUpdateDestination;
    }

    public Location getBaseLocation() {
        return baseLocation;
    }

    public void setBaseLocation(Location baseLocation) {
        this.baseLocation = baseLocation;
    }
}
