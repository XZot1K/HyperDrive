/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.api;

import io.papermc.lib.PaperLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.events.RandomTeleportEvent;
import xzot1k.plugins.hd.core.internals.hooks.HookChecker;
import xzot1k.plugins.hd.core.objects.Destination;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RandomTeleportation implements Runnable {

    private final static HyperDrive pluginInstance = HyperDrive.getPluginInstance();
    private final static LinkedHashMap<UUID, RandomTeleportation> rtpMap = new LinkedHashMap<>();
    private final int taskId, maxAttempts;
    private final String teleportSound;
    private final List<String> biomeBlackList;
    private final Player player;
    private final World world;
    private final boolean onlyUpdateDestination;
    private boolean foundLocation = false;
    private int attempts;
    private long boundsRadius;
    private Location baseLocation = null;
    private CompletableFuture<Chunk> chunkCompleteFuture;

    public RandomTeleportation(@NotNull Player player, @NotNull World world, boolean onlyUpdateDestination) {
        attempts = 0;
        boundsRadius = 0;
        this.player = player;
        this.world = world;
        this.onlyUpdateDestination = onlyUpdateDestination;
        maxAttempts = pluginInstance.getConfig().getInt("random-teleport-section.max-tries");
        teleportSound = Objects.requireNonNull(pluginInstance.getConfig().getString("general-section.global-sounds.teleport"))
                .toUpperCase().replace(" ", "_").replace("-", "_");
        biomeBlackList = pluginInstance.getConfig().getStringList("random-teleport-section.biome-blacklist");

        final String customBorderString = getCustomBorderString(world);
        if (customBorderString != null && customBorderString.contains(":")) {
            final String[] borderArgs = customBorderString.split(":");
            if (borderArgs.length >= 3) {
                final String size = borderArgs[1], center = borderArgs[2];
                if (!pluginInstance.getManager().isNotNumeric(size) && center.contains(",")) {
                    final String[] centerArgs = center.split(",");
                    final long foundSize = Long.parseLong(size);
                    if (!pluginInstance.getManager().isNotNumeric(centerArgs[0]) && !pluginInstance.getManager().isNotNumeric(centerArgs[1])) {
                        final int x = Integer.parseInt(centerArgs[0]), z = Integer.parseInt(centerArgs[1]);
                        boundsRadius = foundSize;
                        baseLocation = new Location(world, x, 0, z);
                    }
                }
            }
        }

        final WorldBorder worldBorder = world.getWorldBorder();

        if (boundsRadius <= 0) {
            boundsRadius = (long) worldBorder.getSize();
            if ((worldBorder.getSize() / 2) < boundsRadius) boundsRadius = (long) (worldBorder.getSize() / 2);
        }

        if (baseLocation == null) baseLocation = worldBorder.getCenter();
        if (boundsRadius >= 29999984) boundsRadius = 5000;

        taskId = pluginInstance.getServer().getScheduler().runTaskTimerAsynchronously(pluginInstance, this, 0, 20).getTaskId();
    }

    public static RandomTeleportation getRTPInstance(UUID playerUniqueId) {
        RandomTeleportation randomTeleportation = rtpMap.getOrDefault(playerUniqueId, null);
        if (randomTeleportation != null) {
            pluginInstance.getServer().getScheduler().cancelTask(randomTeleportation.getTaskId());
            rtpMap.remove(playerUniqueId);
            return randomTeleportation;
        }
        return null;
    }

    public static void clearRTPInstance(UUID playerUniqueId) {
        RandomTeleportation randomTeleportation = rtpMap.getOrDefault(playerUniqueId, null);
        if (randomTeleportation != null) pluginInstance.getServer().getScheduler().cancelTask(randomTeleportation.getTaskId());
        rtpMap.remove(playerUniqueId);
    }

    public static LinkedHashMap<UUID, RandomTeleportation> getRtpMap() {
        return rtpMap;
    }

    @Override
    public void run() {
        if (chunkCompleteFuture != null && !chunkCompleteFuture.isDone() && !chunkCompleteFuture.isCompletedExceptionally()) return;

        if (foundLocation) {
            pluginInstance.getServer().getScheduler().cancelTask(getTaskId());
            clearRTPInstance(player.getUniqueId());
            return;
        }

        if (attempts >= maxAttempts) {
            pluginInstance.getServer().getScheduler().cancelTask(getTaskId());
            clearRTPInstance(player.getUniqueId());
            pluginInstance.getManager().sendCustomMessage("random-teleport-fail", player, ("{tries}:" + attempts));
            pluginInstance.getManager().clearCooldown(player, "rtp");
            return;
        } else attempts += 1;

        int xCalcOne = (int) (baseLocation.getBlockX() - boundsRadius), xCalcTwo = (int) (baseLocation.getBlockX() + boundsRadius),
                zCalcOne = (int) (baseLocation.getBlockZ() - boundsRadius), zCalcTwo = (int) (baseLocation.getBlockZ() + boundsRadius);

        int minX = Math.min(xCalcOne, xCalcTwo), maxX = Math.max(xCalcOne, xCalcTwo),
                minZ = Math.min(zCalcOne, zCalcTwo), maxZ = Math.max(zCalcOne, zCalcTwo),
                xAddition = pluginInstance.getTeleportationHandler().getRandomInRange(minX, maxX),
                zAddition = pluginInstance.getTeleportationHandler().getRandomInRange(minZ, maxZ),
                x = (int) (baseLocation.getX() + xAddition), z = (int) (baseLocation.getZ() + zAddition);

        if (xAddition >= boundsRadius || zAddition >= boundsRadius) return;

        if (pluginInstance.getServerVersion().startsWith("v1_8_") || pluginInstance.getServerVersion().startsWith("v1_9_")
                || pluginInstance.getServerVersion().startsWith("v1_10_") || pluginInstance.getServerVersion().startsWith("v1_11_")
                || pluginInstance.getServerVersion().startsWith("v1_12_")) {
            chunkCompleteFuture = PaperLib.getChunkAtAsync(world, x >> 16, z >> 16, true);
        } else {
            try {
                chunkCompleteFuture = PaperLib.getChunkAtAsyncUrgently(world, x >> 16, z >> 16, true);
            } catch (IllegalStateException | NoSuchMethodError ignored) {
                chunkCompleteFuture = PaperLib.getChunkAtAsync(world, x >> 16, z >> 16, true);
            }
        }

        chunkCompleteFuture.whenComplete((chunk, exception) ->
                pluginInstance.getServer().getScheduler().runTask(pluginInstance, () -> {handleAction(chunk, x, z);}));
    }

    private void handleAction(@NotNull Chunk chunk, int x, int z) {
        final int highestY = getHighestY(world, x, z);
        if (world.getEnvironment() == World.Environment.NETHER && highestY >= 120) {
            chunkCompleteFuture = null;
            return;
        }

        final Block originalBlock = world.getBlockAt(x, highestY, z),
                block = originalBlock.getRelative(BlockFace.DOWN);

        boolean isBlockedBiome = false;
        if (!biomeBlackList.isEmpty()) for (String biomeName : biomeBlackList)
            if (block.getBiome().name().contains(biomeName.toUpperCase().replace(" ", "_").replace("-", "_"))) {
                isBlockedBiome = true;
                break;
            }

        final boolean failedSafeCheck = pluginInstance.getHookChecker().isNotSafe(player, block.getLocation(), HookChecker.CheckType.RTP),
                isForbiddenMaterial = pluginInstance.getManager().isForbiddenMaterial(block.getType(), block.getData());
        if (isBlockedBiome || failedSafeCheck || isForbiddenMaterial) {
            chunkCompleteFuture = null;
            return;
        }

        foundLocation = true;
        pluginInstance.getServer().getScheduler().cancelTask(getTaskId());
        clearRTPInstance(player.getUniqueId());

        Location location = originalBlock.getLocation().clone().add(0.5, 1, 0.5);
        location.setYaw(player.getLocation().getYaw());
        location.setPitch(player.getLocation().getPitch());

        RandomTeleportEvent randomTeleportEvent = new RandomTeleportEvent(location, player);
        pluginInstance.getServer().getPluginManager().callEvent(randomTeleportEvent);
        if (randomTeleportEvent.isCancelled()) return;

        if (onlyUpdateDestination) {
            pluginInstance.getTeleportationHandler().updateDestination(player, new Destination(pluginInstance, location));
            return;
        }

        pluginInstance.getTeleportationHandler().teleportPlayer(player, location);
        if (!player.hasPermission("hyperdrive.rtpbypass")) pluginInstance.getManager().updateCooldown(player, "rtp");

        String animationLine = pluginInstance.getConfig().getString("special-effects-section.random-teleport-animation");
        if (animationLine != null && animationLine.contains(":")) {
            String[] animationArgs = animationLine.split(":");
            if (animationArgs.length >= 2) {
                EnumContainer.Animation animation = EnumContainer.Animation.valueOf(animationArgs[0].toUpperCase().replace(" ", "_").replace("-",
                        "_"));
                pluginInstance.getTeleportationHandler().getAnimation().playAnimation(player,
                        animationArgs[1].toUpperCase().replace(" ", "_").replace("-", "_"), animation, 1);
            }
        }

        if (!teleportSound.equalsIgnoreCase("")) Objects.requireNonNull(location.getWorld()).playSound(location, Sound.valueOf(teleportSound), 1, 1);
        pluginInstance.getManager().sendCustomMessage("random-teleported", player, "{tries}:" + attempts, "{x}:" + location.getBlockX(),
                "{y}:" + location.getBlockY(), "{z}:" + location.getBlockZ(), "{world}:" + Objects.requireNonNull(location.getWorld()).getName());
    }

    private String getCustomBorderString(World world) {
        for (String line : pluginInstance.getConfig().getStringList("random-teleport-section.custom-borders"))
            if (line.toLowerCase().startsWith(world.getName().toLowerCase())) return line;
        return null;
    }

    private int getHighestY(@NotNull World world, double x, double z) {
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

    public int getTaskId() {
        return taskId;
    }

}