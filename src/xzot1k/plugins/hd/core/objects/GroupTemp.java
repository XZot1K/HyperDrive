/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.core.objects;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;
import xzot1k.plugins.hd.api.events.GroupTeleportEvent;
import xzot1k.plugins.hd.api.objects.SerializableLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class GroupTemp {
    private HyperDrive pluginInstance;
    private Destination destination;
    private boolean cancelled;
    private List<UUID> selectedPlayers, acceptedPlayers;

    public GroupTemp(HyperDrive pluginInstance, List<UUID> selectedPlayers, Player player, Destination destination) {
        setPluginInstance(pluginInstance);
        setAcceptedPlayers(new ArrayList<>());
        setCancelled(false);
        setDestination(destination);
        setSelectedPlayers(selectedPlayers);

        createTask(player);
    }

    private void createTask(Player player) {
        if (getSelectedPlayers() == null || getSelectedPlayers().isEmpty()) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.player-selection-fail"), player);
            return;
        }

        GroupTeleportEvent groupTeleportEvent = new GroupTeleportEvent(getDestination().getLocation().asBukkitLocation(), player, getAcceptedPlayers());
        getPluginInstance().getServer().getPluginManager().callEvent(groupTeleportEvent);
        if (groupTeleportEvent.isCancelled())
            return;
        if (groupTeleportEvent.getDestination() != null)
            setDestination(new Destination(getPluginInstance(), groupTeleportEvent.getDestination()));

        new BukkitRunnable() {
            int time = 0;
            final int duration = getPluginInstance().getConfig().getInt("teleportation-section.group-request-duration");
            final String animationSet = getPluginInstance().getConfig().getString("special-effects-section.group-teleport-animation"),
                    teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                            .toUpperCase().replace(" ", "_").replace("-", "_");
            final boolean useMySQL = getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql"),
                    useCrossWarping = getPluginInstance().getConfig().getBoolean("mysql-connection.cross-server-warping");

            @Override
            public void run() {
                time += 1;

                if (cancelled) {
                    getPluginInstance().getManager().getPaging().getPlayerSelectedMap().remove(player.getUniqueId());
                    getPluginInstance().getTeleportationHandler().clearGroupTemp(player);
                    cancel();
                    return;
                }

                if (time >= duration) {
                    if (!getAcceptedPlayers().isEmpty()) {

                        SerializableLocation serializableLocation = getDestination().getLocation();
                        if (serializableLocation == null) {
                            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
                                    .getString("language-section.group-destination-teleport-fail"), player);
                            getPluginInstance().getManager().getPaging().getPlayerSelectedMap()
                                    .remove(player.getUniqueId());
                            getPluginInstance().getTeleportationHandler().clearGroupTemp(player);
                            cancel();
                            return;
                        }

                        Location destinationLocation = getDestination().getLocation().asBukkitLocation();
                        if (destinationLocation != null) {
                            int teleportCount = 0;
                            boolean b = animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":");
                            for (int i = -1; ++i < getAcceptedPlayers().size(); ) {
                                UUID playerUniqueId = getAcceptedPlayers().get(i);
                                if (playerUniqueId == null)
                                    continue;

                                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(playerUniqueId);
                                if (!offlinePlayer.isOnline()) continue;

                                if (useCrossWarping && useMySQL && getPluginInstance().getDatabaseConnection() != null) {
                                    if (getDestination().getWarp() != null) {
                                        String warpIP = getDestination().getWarp().getServerIPAddress(),
                                                serverIP = (getPluginInstance().getServer().getIp().equalsIgnoreCase("")
                                                        || getPluginInstance().getServer().getIp().equalsIgnoreCase("0.0.0.0"))
                                                        ? getPluginInstance().getConfig().getString("mysql-connection.default-ip") + ":"
                                                        + getPluginInstance().getServer().getPort() : (getPluginInstance().getServer().getIp()
                                                        .replace("localhost", "127.0.0.1") + ":" + getPluginInstance().getServer().getPort());

                                        if (!warpIP.equalsIgnoreCase(serverIP)) {
                                            String server = getPluginInstance().getBungeeListener()
                                                    .getServerName(getDestination().getWarp().getServerIPAddress());
                                            if (server != null) {
                                                getPluginInstance().getManager().teleportCrossServer(offlinePlayer.getPlayer(), server, getDestination().getWarp().getWarpLocation());
                                                getPluginInstance().getManager().updateCooldown(offlinePlayer.getPlayer(), "warp");
                                                return;
                                            }
                                        }
                                    }

                                    getPluginInstance().getTeleportationHandler()
                                            .teleportPlayer(Objects.requireNonNull(offlinePlayer.getPlayer()), destinationLocation);
                                    getPluginInstance().getManager().updateCooldown(offlinePlayer.getPlayer(), "warp");
                                    return;
                                } else {
                                    getPluginInstance().getTeleportationHandler()
                                            .teleportPlayer(Objects.requireNonNull(offlinePlayer.getPlayer()), destinationLocation);
                                    getPluginInstance().getManager().updateCooldown(offlinePlayer.getPlayer(), "warp");
                                }

                                if (!teleportSound.equalsIgnoreCase(""))
                                    player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound), 1,
                                            1);
                                if (b) {
                                    String[] themeArgs = animationSet.split(":");
                                    getPluginInstance().getTeleportationHandler().getAnimation()
                                            .stopActiveAnimation(player);
                                    getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player,
                                            themeArgs[1], EnumContainer.Animation.valueOf(
                                                    themeArgs[0].toUpperCase().replace(" ", "_").replace("-", "_")),
                                            1);
                                }

                                teleportCount += 1;
                                getPluginInstance().getManager().sendCustomMessage(
                                        Objects.requireNonNull(getPluginInstance().getLangConfig().getString("group-teleported"))
                                                .replace("{player}", player.getName()),
                                        offlinePlayer.getPlayer());
                            }

                            if (useCrossWarping && useMySQL && getPluginInstance().getDatabaseConnection() != null) {
                                if (getDestination().getWarp() != null) {
                                    String warpIP = getDestination().getWarp().getServerIPAddress().replace("localhost", "127.0.0.1"),
                                            serverIP = (getPluginInstance().getServer().getIp().equalsIgnoreCase("")
                                                    || getPluginInstance().getServer().getIp().equalsIgnoreCase("0.0.0.0"))
                                                    ? getPluginInstance().getConfig().getString("mysql-connection.default-ip")
                                                    + ":" + getPluginInstance().getServer().getPort()
                                                    : (getPluginInstance().getServer().getIp().replace("localhost", "127.0.0.1") + ":"
                                                    + getPluginInstance().getServer().getPort());

                                    if (!warpIP.equalsIgnoreCase(serverIP)) {
                                        String server = getPluginInstance().getBungeeListener()
                                                .getServerName(getDestination().getWarp().getServerIPAddress());
                                        if (server != null) {
                                            getPluginInstance().getManager().teleportCrossServer(player, server, getDestination().getWarp().getWarpLocation());
                                            getPluginInstance().getManager().updateCooldown(player, "warp");
                                            return;
                                        }
                                    }
                                }

                                getPluginInstance().getTeleportationHandler().teleportPlayer(player,
                                        destinationLocation);
                                getPluginInstance().getManager().updateCooldown(player, "warp");
                                return;
                            } else {
                                getPluginInstance().getTeleportationHandler().teleportPlayer(player,
                                        destinationLocation);
                                getPluginInstance().getManager().updateCooldown(player, "warp");
                            }

                            if (!teleportSound.equalsIgnoreCase(""))
                                player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound), 1, 1);
                            if (b) {
                                String[] themeArgs = animationSet.split(":");
                                getPluginInstance().getTeleportationHandler().getAnimation()
                                        .stopActiveAnimation(player);
                                getPluginInstance().getTeleportationHandler().getAnimation()
                                        .playAnimation(player, themeArgs[1],
                                                EnumContainer.Animation.valueOf(
                                                        themeArgs[0].toUpperCase().replace(" ", "_").replace("-", "_")),
                                                1);
                            }

                            if (teleportCount > 0)
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig()
                                        .getString("group-teleport-success"), player);
                            else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig()
                                        .getString("group-teleport-fail"), player);
                        } else
                            getPluginInstance().getManager().sendCustomMessage(
                                    getPluginInstance().getLangConfig().getString("destination-invalid"),
                                    player);

                    } else
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("group-teleport-fail"), player);

                    getPluginInstance().getManager().getPaging().getPlayerSelectedMap().remove(player.getUniqueId());
                    getPluginInstance().getTeleportationHandler().clearGroupTemp(player);
                    cancel();
                }
            }
        }.runTaskTimer(getPluginInstance(), 0, 20);
    }

    // getters & setters
    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public List<UUID> getSelectedPlayers() {
        return selectedPlayers;
    }

    private void setSelectedPlayers(List<UUID> selectedPlayers) {
        this.selectedPlayers = selectedPlayers;
    }

    public List<UUID> getAcceptedPlayers() {
        return acceptedPlayers;
    }

    private void setAcceptedPlayers(List<UUID> acceptedPlayers) {
        this.acceptedPlayers = acceptedPlayers;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
