/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.api;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.events.BasicTeleportationEvent;
import xzot1k.plugins.hd.api.events.EconomyChargeEvent;
import xzot1k.plugins.hd.api.events.WarpEvent;
import xzot1k.plugins.hd.api.objects.SerializableLocation;
import xzot1k.plugins.hd.api.objects.Warp;
import xzot1k.plugins.hd.core.internals.Animation;
import xzot1k.plugins.hd.core.internals.hooks.HookChecker;
import xzot1k.plugins.hd.core.objects.Destination;
import xzot1k.plugins.hd.core.objects.GroupTemp;
import xzot1k.plugins.hd.core.objects.TeleportTemp;

import java.util.*;

public class TeleportationHandler implements Runnable {
    private HyperDrive pluginInstance;
    private Random random;

    private Animation animation;
    private HashMap<UUID, GroupTemp> groupTempMap;
    private HashMap<UUID, TeleportTemp> teleportTempMap;
    private HashMap<UUID, Destination> destinationMap;
    private List<UUID> randomTeleportingPlayers;
    private final String teleportSound, teleportTitle, teleportSubTitle, delayTitle, delaySubTitle, randomTeleportDelayTitle, randomTeleportSubDelayTitle;

    public TeleportationHandler(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);
        setRandom(new Random());
        this.teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport")).toUpperCase().replace(" ", "_").replace("-", "_");
        this.teleportTitle = getPluginInstance().getConfig().getString("teleportation-section.teleport-title");
        this.teleportSubTitle = getPluginInstance().getConfig().getString("teleportation-section.teleport-sub-title");
        this.delayTitle = getPluginInstance().getConfig().getString("teleportation-section.delay-title");
        this.delaySubTitle = getPluginInstance().getConfig().getString("teleportation-section.delay-sub-title");
        this.randomTeleportDelayTitle = getPluginInstance().getConfig().getString("random-teleport-section.delay-title");
        this.randomTeleportSubDelayTitle = getPluginInstance().getConfig().getString("random-teleport-section.delay-sub-title");
        setPluginInstance(pluginInstance);
        setAnimation(new Animation(pluginInstance));
        setGroupTempMap(new HashMap<>());
        setTeleportTempMap(new HashMap<>());
        setDestinationMap(new HashMap<>());
        setRandomTeleportingPlayers(new ArrayList<>());
    }

    @Override
    public void run() {
        List<UUID> playerUniqueIds = new ArrayList<>(getTeleportTempMap().keySet());
        for (UUID playerUniqueId : playerUniqueIds) {
            Player player = getPluginInstance().getServer().getPlayer(playerUniqueId);
            if (player != null && player.isOnline()) {
                final TeleportTemp teleportTemp = getTeleportTempMap().get(playerUniqueId);
                if (teleportTemp != null) {
                    Warp warp = null;
                    if (teleportTemp.getSeconds() > 0) {
                        teleportTemp.setSeconds(teleportTemp.getSeconds() - 1);
                        if (teleportTemp.getSeconds() <= 5 && teleportTemp.getSeconds() > 0) {
                            if (teleportTemp.getTeleportValue() != null && teleportTemp.getTeleportTypeId().equalsIgnoreCase("warp")) {
                                warp = getPluginInstance().getManager().getWarp(teleportTemp.getTeleportValue());
                                if (warp != null) {
                                    if (delayTitle != null && delaySubTitle != null)
                                        getPluginInstance().getManager().sendTitle(player, delayTitle.replace("{warp}", warp.getWarpName()).replace("{duration}", String.valueOf(teleportTemp.getSeconds())),
                                                delaySubTitle.replace("{warp}", warp.getWarpName()).replace("{duration}", String.valueOf(teleportTemp.getSeconds())), 0, 5, 0);

                                    int delayDuration = getPluginInstance().getConfig().getInt("teleportation-section.warp-delay-duration");

                                    String actionMessage = getPluginInstance().getConfig().getString("teleportation-section.delay-bar-message");
                                    if (actionMessage != null && !actionMessage.isEmpty())
                                        getPluginInstance().getManager().sendActionBar(player, actionMessage
                                                .replace("{progress}", getPluginInstance().getManager().getProgressionBar(teleportTemp.getSeconds(), delayDuration, 10))
                                                .replace("{duration}", String.valueOf(delayDuration)).replace("{duration-left}", String.valueOf(teleportTemp.getSeconds()))
                                                .replace("{warp}", warp.getWarpName()));

                                    getPluginInstance().getManager().sendCustomMessage("teleportation-delay", player, "{warp}:" + warp.getWarpName(), "{duration}:" + teleportTemp.getSeconds());
                                }
                            } else if (teleportTemp.getTeleportTypeId().equalsIgnoreCase("rtp")) {
                                if (randomTeleportDelayTitle != null && randomTeleportSubDelayTitle != null)
                                    getPluginInstance().getManager().sendTitle(player, randomTeleportDelayTitle.replace("{duration}", String.valueOf(teleportTemp.getSeconds())),
                                            randomTeleportSubDelayTitle.replace("{duration}", String.valueOf(teleportTemp.getSeconds())), 0, 5, 0);

                                int delayDuration = getPluginInstance().getConfig().getInt("teleportation-section.warp-delay-duration");

                                String actionMessage = getPluginInstance().getConfig().getString("random-teleport-section.delay-bar-message");
                                if (actionMessage != null && !actionMessage.isEmpty())
                                    getPluginInstance().getManager().sendActionBar(player, actionMessage
                                            .replace("{progress}", getPluginInstance().getManager().getProgressionBar(teleportTemp.getSeconds(), delayDuration, 10))
                                            .replace("{duration}", String.valueOf(delayDuration))
                                            .replace("{duration-left}", String.valueOf(teleportTemp.getSeconds())));

                                getPluginInstance().getManager().sendCustomMessage("random-teleport-delay", player, "{duration}:" + teleportTemp.getSeconds());
                            }
                        }
                    } else {
                        boolean isVanished = getPluginInstance().getManager().isVanished(player);
                        switch (teleportTemp.getTeleportTypeId().toLowerCase()) {
                            case "warp":
                                if (teleportTemp.getTeleportValue() != null) {
                                    warp = getPluginInstance().getManager().getWarp(teleportTemp.getTeleportValue());
                                    if (warp != null && warp.getWarpLocation() != null) {
                                        final Location warpLocation = warp.getWarpLocation().asBukkitLocation();
                                        WarpEvent warpEvent = new WarpEvent(warpLocation, player);
                                        getPluginInstance().getServer().getPluginManager().callEvent(warpEvent);
                                        if (warpEvent.isCancelled()) {
                                            getAnimation().stopActiveAnimation(player);
                                            getTeleportTempMap().remove(playerUniqueId);
                                            return;
                                        }

                                        if (!getPluginInstance().getHookChecker().isLocationHookSafe(player, warpLocation, HookChecker.CheckType.WARP)) {
                                            getAnimation().stopActiveAnimation(player);
                                            getTeleportTempMap().remove(playerUniqueId);
                                            getPluginInstance().getManager().sendCustomMessage("not-hook-safe", player, ("{warp}:" + warp.getWarpName()));
                                            return;
                                        }

                                        if (getPluginInstance().getConfig().getBoolean("general-section.use-vault") && !player.hasPermission("hyperdrive.economybypass")
                                                && !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString()) && !warp.getAssistants().contains(player.getUniqueId())) {
                                            EconomyChargeEvent economyChargeEvent = new EconomyChargeEvent(player, warp.getUsagePrice());
                                            getPluginInstance().getServer().getPluginManager().callEvent(economyChargeEvent);
                                            if (!economyChargeEvent.isCancelled()) {
                                                if (!getPluginInstance().getVaultHandler().getEconomy().has(player, economyChargeEvent.getAmount())) {
                                                    getAnimation().stopActiveAnimation(player);
                                                    getTeleportTempMap().remove(playerUniqueId);
                                                    getPluginInstance().getManager().sendCustomMessage("insufficient-funds", player, "{amount}:" + economyChargeEvent.getAmount());
                                                    return;
                                                }

                                                getPluginInstance().getVaultHandler().getEconomy().withdrawPlayer(player, warp.getUsagePrice());
                                                if (warp.getOwner() != null) {
                                                    OfflinePlayer owner = getPluginInstance().getServer().getOfflinePlayer(warp.getOwner());
                                                    getPluginInstance().getVaultHandler().getEconomy().depositPlayer(owner, warp.getUsagePrice());
                                                }

                                                getPluginInstance().getManager().sendCustomMessage("transaction-success", player, "{amount}:" + economyChargeEvent.getAmount());
                                            }
                                        }

                                        if (warp.getOwner() != null)
                                            if (!warp.getOwner().toString().equalsIgnoreCase(player.getUniqueId().toString())
                                                    && !warp.getAssistants().contains(player.getUniqueId()))
                                                warp.setTraffic(warp.getTraffic() + 1);

                                        boolean useMySQL = (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")
                                                && getPluginInstance().getConfig().getBoolean("mysql-connection.cross-server"));
                                        if (useMySQL && getPluginInstance().getDatabaseConnection() != null && getPluginInstance().getBungeeListener() != null) {
                                            String serverIP = (warp.getServerIPAddress().contains(".") ? getPluginInstance().getBungeeListener().getServerAddressMap().get(
                                                    getPluginInstance().getBungeeListener().getMyServer()) : getPluginInstance().getBungeeListener().getMyServer());

                                            if (!warp.getServerIPAddress().equalsIgnoreCase(serverIP)) {
                                                for (String command : warp.getCommands()) {
                                                    if (command.toUpperCase().endsWith(":PLAYER"))
                                                        getPluginInstance().getServer().dispatchCommand(player, command.replaceAll("(?i):PLAYER", "")
                                                                .replaceAll("(?i):CONSOLE", "").replace("{player}", player.getName()));
                                                    else if (command.toUpperCase().endsWith(":CONSOLE"))
                                                        getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(),
                                                                command.replaceAll("(?i):PLAYER", "").replaceAll("(?i):CONSOLE", "")
                                                                        .replace("{player}", player.getName()));
                                                    else
                                                        getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(),
                                                                command.replace("{player}", player.getName()));
                                                }

                                                getTeleportTempMap().remove(playerUniqueId);
                                                getPluginInstance().getManager().teleportCrossServer(player, (warp.getServerIPAddress().contains(".")
                                                        ? getPluginInstance().getBungeeListener().getServerName(warp.getServerIPAddress()) : warp.getServerIPAddress()), warp.getWarpLocation());
                                                getPluginInstance().getManager().updateCooldown(player, "warp");
                                            }

                                            if (warpLocation == null || warpLocation.getWorld() == null) {
                                                getPluginInstance().getManager().sendActionBar(player, Objects.requireNonNull(getPluginInstance().getLangConfig()
                                                        .getString("teleport-fail-message")).replace("{warp}", warp.getWarpName()));
                                                getTeleportTempMap().remove(playerUniqueId);
                                                return;
                                            }

                                            for (String command : warp.getCommands()) {
                                                if (command.toUpperCase().endsWith(":PLAYER"))
                                                    getPluginInstance().getServer().dispatchCommand(player, command
                                                            .replaceAll("(?i):PLAYER", "").replaceAll("(?i):CONSOLE", "").replace("{player}", player.getName()));
                                                else if (command.toUpperCase().endsWith(":CONSOLE"))
                                                    getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(),
                                                            command.replaceAll("(?i):PLAYER", "").replaceAll("(?i):CONSOLE", "").replace("{player}", player.getName()));
                                                else
                                                    getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(), command.replace("{player}", player.getName()));
                                            }

                                        } else {
                                            for (String command : warp.getCommands()) {
                                                if (command.toUpperCase().endsWith(":PLAYER"))
                                                    getPluginInstance().getServer().dispatchCommand(player, command.replaceAll("(?i):PLAYER", "").replaceAll("(?i):CONSOLE", "").replace("{player}", player.getName()));
                                                else if (command.toUpperCase().endsWith(":CONSOLE"))
                                                    getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(),
                                                            command.replaceAll("(?i):PLAYER", "").replaceAll("(?i):CONSOLE", "").replace("{player}", player.getName()));
                                                else
                                                    getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(), command.replace("{player}", player.getName()));
                                            }
                                        }
                                        getTeleportTempMap().remove(playerUniqueId);
                                        teleportPlayer(player, warpLocation);
                                        getPluginInstance().getManager().updateCooldown(player, "warp");

                                        // Warp teleport animation
                                        if (!isVanished) {
                                            if (warp.getAnimationSet() != null && warp.getAnimationSet().contains(":")) {
                                                String[] themeArgs = warp.getAnimationSet().split(":");
                                                String teleportTheme = themeArgs[2];
                                                if (teleportTheme.contains("/")) {
                                                    String[] teleportThemeArgs = teleportTheme.split("/");
                                                    getAnimation().stopActiveAnimation(player);
                                                    getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, teleportThemeArgs[1], EnumContainer.Animation.valueOf(teleportThemeArgs[0]
                                                            .toUpperCase().replace(" ", "_").replace("-", "_")), 1);
                                                }
                                            }

                                            if (!teleportSound.equalsIgnoreCase("") && warpLocation.getWorld() != null)
                                                warpLocation.getWorld().playSound(warpLocation, Sound.valueOf(teleportSound), 1, 1);
                                        }

                                        if (warp.canNotify() && warp.getOwner() != null && !isVanished) {
                                            Player owner = getPluginInstance().getServer().getPlayer(warp.getOwner());
                                            if (owner != null && owner.isOnline())
                                                getPluginInstance().getManager().sendCustomMessage("warp-notify", owner, "{player}:" + player.getName(), "{warp}:" + warp.getWarpName());
                                        }

                                        if (warp.canNotify() && !warp.getAssistants().isEmpty() && !isVanished)
                                            for (UUID id : warp.getAssistants()) {
                                                Player assistant = getPluginInstance().getServer().getPlayer(id);
                                                if (assistant != null && assistant.isOnline())
                                                    getPluginInstance().getManager().sendCustomMessage("warp-notify", assistant, "{player}:" + player.getName(), "{warp}:" + warp.getWarpName());
                                            }

                                        if (teleportTitle != null && teleportSubTitle != null)
                                            getPluginInstance().getManager().sendTitle(player,
                                                    teleportTitle.replace("{warp}", warp.getWarpName()),
                                                    teleportSubTitle.replace("{warp}", warp.getWarpName()), 0, 5, 0);

                                        String actionMessage = getPluginInstance().getConfig().getString("teleportation-section.teleport-bar-message");
                                        if (actionMessage != null && !actionMessage.isEmpty())
                                            getPluginInstance().getManager().sendActionBar(player, actionMessage.replace("{warp}", warp.getWarpName()));

                                        getPluginInstance().getManager().sendCustomMessage("teleportation-engaged", player, "{warp}:" + warp.getWarpName(), "{duration}:" + teleportTemp.getSeconds());
                                    }
                                }
                                break;
                            case "rtp":
                                if (teleportTemp.getTeleportValue() != null) {
                                    getTeleportTempMap().remove(playerUniqueId);
                                    World world = getPluginInstance().getServer().getWorld(teleportTemp.getTeleportValue());
                                    randomlyTeleportPlayer(player, (world == null || world.getName().equalsIgnoreCase("")) ? player.getWorld() : world);

                                    String randomTeleportDelayAnimation = getPluginInstance().getConfig().getString("special-effects-section.random-teleport-delay-animation");
                                    if (!isVanished && randomTeleportDelayAnimation != null && randomTeleportDelayAnimation.contains(":")) {
                                        String[] themeArgs = randomTeleportDelayAnimation.split(":");
                                        String teleportTheme = themeArgs[1];
                                        if (teleportTheme.contains("/")) {
                                            String[] teleportThemeArgs = teleportTheme.split("/");
                                            getAnimation().stopActiveAnimation(player);
                                            getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, teleportThemeArgs[0],
                                                    EnumContainer.Animation.valueOf(teleportThemeArgs[0].toUpperCase().replace(" ", "_").replace("-", "_")), 1);
                                        }
                                    }
                                }
                                break;
                            case "tp":
                                operateTeleportation(teleportTemp, teleportTemp.getTeleportTypeId().toLowerCase(), player, playerUniqueId);
                                break;
                            default:
                                break;
                        }
                    }
                }
            } else getTeleportTempMap().remove(playerUniqueId);
        }
    }

    /**
     * Gets a random int between two values.
     *
     * @param min The minimum.
     * @param max The maximum.
     * @return The found value.
     */
    public int getRandomInRange(int min, int max) {
        return getRandom().nextInt((max - min) + 1) + min;
    }

    private void operateTeleportation(TeleportTemp teleportTemp, String teleportId, Player player, UUID playerUniqueId) {
        if (teleportTemp.getTeleportValue() != null) {
            final Location toLocation = new SerializableLocation(teleportTemp.getTeleportValue()).asBukkitLocation();
            if (toLocation == null) return;

            BasicTeleportationEvent basicTeleportationEvent = new BasicTeleportationEvent(toLocation, player);
            getPluginInstance().getServer().getPluginManager().callEvent(basicTeleportationEvent);
            if (basicTeleportationEvent.isCancelled()) {
                getAnimation().stopActiveAnimation(player);
                getTeleportTempMap().remove(playerUniqueId);
                return;
            }

            getTeleportTempMap().remove(playerUniqueId);
            teleportPlayer(player, toLocation);
            getPluginInstance().getManager().updateCooldown(player, teleportId);

            String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.standalone-teleport"))
                    .toUpperCase().replace(" ", "_").replace("-", "_"),
                    animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
            if (!teleportSound.equalsIgnoreCase(""))
                player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound), 1, 1);

            boolean isVanished = getPluginInstance().getManager().isVanished(player);
            if (!isVanished && animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
                String[] themeArgs = animationSet.split(":");
                getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
                getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, themeArgs[1],
                        EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                                .replace("-", "_")), 1);
            }

            if (getPluginInstance().getTeleportationCommands() != null && getPluginInstance().getTeleportationCommands().getSpawnLocation() != null)
                if (!getPluginInstance().getTeleportationCommands().getSpawnLocation().getWorldName().equals(toLocation.getWorld().getName())
                        && !(getPluginInstance().getTeleportationCommands().getSpawnLocation().getX() != toLocation.getX()
                        || getPluginInstance().getTeleportationCommands().getSpawnLocation().getY() != toLocation.getY()
                        || getPluginInstance().getTeleportationCommands().getSpawnLocation().getZ() != toLocation.getZ()))
                    getPluginInstance().getManager().sendCustomMessage("basic-teleportation-engaged", player,
                            "{world}:" + toLocation.getWorld().getName(), "{x}:" + toLocation.getBlockX(), "{y}:" + toLocation.getBlockY(),
                            "{z}:" + toLocation.getBlockZ(), "{duration}:" + teleportTemp.getSeconds());
        }
    }

    // teleportation temp stuff
    public void updateTeleportTemp(Player player, String teleportTypeId, String teleportValue, int seconds) {
        getTeleportTempMap().put(player.getUniqueId(),
                new TeleportTemp(getPluginInstance(), teleportTypeId, teleportValue, seconds));
    }

    public boolean isTeleporting(Player player) {
        return (!getTeleportTempMap().isEmpty() && getTeleportTempMap().containsKey(player.getUniqueId()))
                || getRandomTeleportingPlayers().contains(player.getUniqueId());
    }

    public boolean isRandomTeleporting(Player player) {
        return getRandomTeleportingPlayers().contains(player.getUniqueId());
    }

    public int getRemainingTime(Player player) {
        if (!getTeleportTempMap().isEmpty() && getTeleportTempMap().containsKey(player.getUniqueId())) {
            TeleportTemp teleportTemp = getTeleportTempMap().get(player.getUniqueId());
            if (teleportTemp != null)
                return teleportTemp.getSeconds();
        }

        return 0;
    }

    public void removeTeleportTemp(Player player) {
        getTeleportTempMap().remove(player.getUniqueId());
    }

    /**
     * Warps the player to the specified warp
     *
     * @param player    The player to warp
     * @param warp      The warp to teleport the player to
     * @param warpDelay The delay before teleportation takes place (Seconds)
     */
    public void warpPlayer(Player player, Warp warp, int warpDelay) {
        updateTeleportTemp(player, "warp", warp.getWarpName(), warpDelay);
    }

    // teleport methods
    public void teleportPlayer(Player player, Location location) {
        if (player == null || !player.isOnline() || location == null || location.getWorld() == null)
            return;

        player.setVelocity(new Vector(0, 0, 0));
        getPluginInstance().getTeleportationCommands().updateLastLocation(player, player.getLocation());

        int invulnerabilityDuration = getPluginInstance().getConfig().getInt("teleportation-section.invulnerability-duration");
        if (invulnerabilityDuration > 0)
            getPluginInstance().getManager().updateCooldown(player, "invulnerability");

        boolean teleportVehicle = getPluginInstance().getConfig().getBoolean("teleportation-section.teleport-vehicles");
        if (player.getVehicle() != null && teleportVehicle) {
            Entity entity = player.getVehicle();
            if (!(getPluginInstance().getServerVersion().startsWith("v1_10") || getPluginInstance().getServerVersion().startsWith("v1_9")
                    || getPluginInstance().getServerVersion().startsWith("v1_8")))
                entity.removePassenger(player);

            if (entity.getPassengers().contains(player))
                entity.eject();

            player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
            new BukkitRunnable() {
                @Override
                public void run() {
                    entity.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                    entity.addPassenger(player);
                }
            }.runTaskLater(getPluginInstance(), 1);
        } else
            player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    public void randomlyTeleportPlayer(Player player, World world) {
        if (player == null || world == null) return;
        getPluginInstance().getManager().sendCustomMessage("random-teleport-start", player);
        getRandomTeleportingPlayers().add(player.getUniqueId());
        world.getWorldBorder();
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
                new RandomTeleportation(getPluginInstance(), world, player, false));
    }

    public void updateDestinationWithRandomLocation(Player player, World world) {
        getRandomTeleportingPlayers().add(player.getUniqueId());
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
                new RandomTeleportation(getPluginInstance(), world, player, true));
    }

    // group stuff

    /**
     * Returns the GroupTemp the passed player id has accepted.
     *
     * @param playerUniqueId The passed player id.
     * @return The found GroupTemp the player accepted.
     */
    public GroupTemp getAcceptedGroupTemp(UUID playerUniqueId) {
        List<UUID> groupLeaderList = new ArrayList<>(getGroupTempMap().keySet());
        for (int i = -1; ++i < groupLeaderList.size(); ) {
            UUID groupLeaderId = groupLeaderList.get(i);
            if (groupLeaderId != null) {
                GroupTemp groupTemp = getGroupTempMap().get(groupLeaderId);
                if (groupTemp.getAcceptedPlayers().contains(playerUniqueId))
                    return groupTemp;
            }
        }

        return null;
    }

    /**
     * Returns the leader of the group the player accepted.
     *
     * @param playerUniqueId The passed player id.
     * @return The id of the group leader.
     */
    public UUID getGroupLeader(UUID playerUniqueId) {
        List<UUID> groupLeaderList = new ArrayList<>(getGroupTempMap().keySet());
        for (int i = -1; ++i < groupLeaderList.size(); ) {
            UUID groupLeaderId = groupLeaderList.get(i);
            if (groupLeaderId != null) {
                GroupTemp groupTemp = getGroupTempMap().get(groupLeaderId);
                if (groupTemp.getAcceptedPlayers().contains(playerUniqueId))
                    return groupLeaderId;
            }
        }

        return null;
    }

    /**
     * Returns the GroupTemp of the passed group leader id.
     *
     * @param playerUniqueId The passed group leader id.
     * @return The found GroupTemp the group leader.
     */
    public GroupTemp getGroupTemp(UUID playerUniqueId) {
        if (!getGroupTempMap().isEmpty() && getGroupTempMap().containsKey(playerUniqueId))
            return getGroupTempMap().get(playerUniqueId);
        return null;
    }

    public void clearGroupTemp(OfflinePlayer player) {
        getGroupTempMap().remove(player.getUniqueId());
    }

    public List<UUID> getGroupRequests(Player player) {
        List<UUID> uuidList = new ArrayList<>(), groupTempList = new ArrayList<>(getGroupTempMap().keySet());
        for (int i = -1; ++i < groupTempList.size(); ) {
            UUID requesterId = groupTempList.get(i);
            if (requesterId == null) continue;

            GroupTemp groupTemp = getGroupTemp(requesterId);
            if (groupTemp == null) continue;

            if (groupTemp.getSelectedPlayers().contains(player.getUniqueId()))
                uuidList.add(requesterId);
        }

        return uuidList;
    }

    public void createGroupTemp(Player player, List<UUID> selectedPlayers, Destination destination) {

        if (destination == null) {
            getPluginInstance().getManager().sendCustomMessage("group-destination-invalid", player);
            return;
        }

        final GroupTemp groupTemp = new GroupTemp(getPluginInstance(), selectedPlayers, player, destination);
        getGroupTempMap().put(player.getUniqueId(), groupTemp);
    }

    public Destination getDestination(Player player) {
        if (!getDestinationMap().isEmpty() && getDestinationMap().containsKey(player.getUniqueId()))
            return getDestinationMap().get(player.getUniqueId());
        return null;
    }

    public void updateDestination(Player player, Destination destination) {
        getDestinationMap().put(player.getUniqueId(), destination);
    }

    // getters and setters
    public HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    private HashMap<UUID, TeleportTemp> getTeleportTempMap() {
        return teleportTempMap;
    }

    private void setTeleportTempMap(HashMap<UUID, TeleportTemp> teleportTempMap) {
        this.teleportTempMap = teleportTempMap;
    }

    public Animation getAnimation() {
        return animation;
    }

    private void setAnimation(Animation animation) {
        this.animation = animation;
    }

    public List<UUID> getRandomTeleportingPlayers() {
        return randomTeleportingPlayers;
    }

    private void setRandomTeleportingPlayers(List<UUID> randomTeleportingPlayers) {
        this.randomTeleportingPlayers = randomTeleportingPlayers;
    }

    private HashMap<UUID, GroupTemp> getGroupTempMap() {
        return groupTempMap;
    }

    private void setGroupTempMap(HashMap<UUID, GroupTemp> groupTempMap) {
        this.groupTempMap = groupTempMap;
    }

    public HashMap<UUID, Destination> getDestinationMap() {
        return destinationMap;
    }

    private void setDestinationMap(HashMap<UUID, Destination> destinationMap) {
        this.destinationMap = destinationMap;
    }

    public Random getRandom() {
        return random;
    }

    private void setRandom(Random random) {
        this.random = random;
    }
}
