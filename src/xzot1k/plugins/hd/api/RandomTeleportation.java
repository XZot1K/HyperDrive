/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.api;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.events.RandomTeleportEvent;
import xzot1k.plugins.hd.api.objects.SerializableLocation;
import xzot1k.plugins.hd.core.objects.Destination;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class RandomTeleportation implements Runnable {

    private HyperDrive pluginInstance;
    private List<String> forbiddenMaterialList, biomeBlackList;
    private int attempts, maxAttempts, boundsRadius, smartLimit;
    private boolean onlyUpdateDestination;
    private String teleportSound;

    private World baseLocationWorld;
    private Player player;
    private SerializableLocation baseLocation;

    public RandomTeleportation(HyperDrive pluginInstance, Location baseLocation, Player player, boolean onlyUpdateDestination) {
        setPluginInstance(pluginInstance);
        setPlayer(player);
        setAttempts(0);
        setSmartLimit(0);
        setOnlyUpdateDestination(onlyUpdateDestination);
        setForbiddenMaterialList(getPluginInstance().getConfig().getStringList("random-teleport-section.forbidden-materials"));
        setBiomeBlackList(getPluginInstance().getConfig().getStringList("random-teleport-section.biome-blacklist"));
        setMaxAttempts(getPluginInstance().getConfig().getInt("random-teleport-section.max-tries"));
        setTeleportSound(Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                .toUpperCase().replace(" ", "_").replace("-", "_"));
        setAttempts(0);
        setBaseLocationWorld(baseLocation.getWorld());
        setBaseLocation(new SerializableLocation(baseLocation));
        setBoundsRadius(getPluginInstance().getManager().getBounds(baseLocation.getWorld()));
    }

    @Override
    public void run() {

        Location newLocation = null;
        boolean foundLocation = false;

        while ((getMaxAttempts() >= 0 && getAttempts() < getMaxAttempts())) {
            setAttempts(getAttempts() + 1);

            int smartBounds = (getBoundsRadius() - getSmartLimit()),
                    xAddition = getPluginInstance().getTeleportationHandler().getRandomInRange(-smartBounds, smartBounds),
                    zAddition = getPluginInstance().getTeleportationHandler().getRandomInRange(-smartBounds, smartBounds),
                    x = (int) (getBaseLocation().getX() + xAddition),
                    z = (int) (getBaseLocation().getZ() + zAddition);

            if (x >= getBoundsRadius() || z >= getBoundsRadius() || getBaseLocation().distance(x, 0, z) < (getBoundsRadius() * 0.1))
                continue;
            if (getSmartLimit() < getBoundsRadius())
                setSmartLimit((int) (getSmartLimit() + (getBoundsRadius() * 0.005)));

            Chunk chunk;
            try {
                chunk = getPluginInstance().getManager().getChunk(getBaseLocationWorld(), x, z).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                getPluginInstance().log(Level.WARNING, "The random teleportation task was unable to obtain a chunk. Don't worry, I caught it before it caused further issues! Gonna try again...");
                return;
            }

            if (chunk == null) continue;
            int highestY = getHighestY(getBaseLocationWorld(), x, z);
            if (highestY <= 0) continue;

            final Block foundBlock = getBaseLocationWorld().getBlockAt(x, highestY, z);

            boolean isBlockedBiome = false;
            if (!getBiomeBlackList().isEmpty()) for (String biomeName : getBiomeBlackList())
                if (foundBlock.getBiome().name().contains(biomeName.toUpperCase().replace(" ", "_").replace("-", "_"))) {
                    isBlockedBiome = true;
                    break;
                }

            if (isBlockedBiome || !getPluginInstance().getHookChecker().isLocationHookSafe(getPlayer(), foundBlock.getLocation()) || isForbidden(foundBlock))
                continue;

            Block relativeUp = foundBlock.getRelative(BlockFace.UP);
            if (!relativeUp.getType().name().contains("AIR") || !relativeUp.getRelative(BlockFace.UP).getType().name().contains("AIR"))
                continue;

            newLocation = new Location(getBaseLocationWorld(), x + 0.5, highestY + 2, z + 0.5, getPlayer().getLocation().getYaw(), getPlayer().getLocation().getPitch());
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

                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString(".random-teleported"))
                        .replace("{tries}", String.valueOf(getAttempts())).replace("{x}", String.valueOf(finalNewLocation.getBlockX()))
                        .replace("{y}", String.valueOf(finalNewLocation.getBlockY())).replace("{z}", String.valueOf(finalNewLocation.getBlockZ()))
                        .replace("{world}", Objects.requireNonNull(finalNewLocation.getWorld()).getName()), getPlayer());
            });
        } else {
            getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () -> getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString(".random-teleport-fail"))
                    .replace("{tries}", String.valueOf(getAttempts())), getPlayer()));
            getPluginInstance().getManager().clearCooldown(getPlayer(), "rtp");
            getPluginInstance().getTeleportationHandler().getRandomTeleportingPlayers().remove(getPlayer().getUniqueId());
        }
    }

    private int getHighestY(World world, double x, double z) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            for (int i = 127; --i > 0; ) {
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

    private boolean isForbidden(Block foundBlock) {
        for (String materialLine : getForbiddenMaterialList()) {
            if (materialLine == null || materialLine.isEmpty()) continue;

            if (materialLine.contains(":")) {
                String[] materialArgs = materialLine.split(":");
                int durability = 0;

                if (!getPluginInstance().getManager().isNotNumeric(materialArgs[1]))
                    durability = Integer.parseInt(materialArgs[1]);

                if (foundBlock.getType().name().contains(materialArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"))
                        && (durability == -1 || foundBlock.getData() == durability))
                    return true;
            } else if (foundBlock.getType().name().contains(materialLine.toUpperCase().replace(" ", "_").replace("-", "_")))
                return true;
        }

        return false;
    }

    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public List<String> getForbiddenMaterialList() {
        return forbiddenMaterialList;
    }

    private void setForbiddenMaterialList(List<String> forbiddenMaterialList) {
        this.forbiddenMaterialList = forbiddenMaterialList;
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

    public SerializableLocation getBaseLocation() {
        return baseLocation;
    }

    private void setBaseLocation(SerializableLocation baseLocation) {
        this.baseLocation = baseLocation;
    }

    public int getBoundsRadius() {
        return boundsRadius;
    }

    private void setBoundsRadius(int boundsRadius) {
        this.boundsRadius = boundsRadius;
    }

    private int getSmartLimit() {
        return smartLimit;
    }

    private void setSmartLimit(int smartLimit) {
        this.smartLimit = smartLimit;
    }

    public World getBaseLocationWorld() {
        return baseLocationWorld;
    }

    private void setBaseLocationWorld(World baseLocationWorld) {
        this.baseLocationWorld = baseLocationWorld;
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
}
