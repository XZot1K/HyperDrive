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
import java.util.UUID;

public class GroupTemp
{
    private HyperDrive pluginInstance;
    private SerializableLocation destination;
    private boolean cancelled;
    private List<UUID> selectedPlayers, acceptedPlayers;

    public GroupTemp(HyperDrive pluginInstance, Player player, SerializableLocation destination)
    {
        setPluginInstance(pluginInstance);
        setAcceptedPlayers(new ArrayList<>());
        setCancelled(false);
        setDestination(destination);

        List<UUID> selectedPlayers = getPluginInstance().getManager().getPaging().getSelectedPlayers(player);
        setSelectedPlayers(selectedPlayers != null ? selectedPlayers : new ArrayList<>());

        createTask(player);
    }

    private void createTask(Player player)
    {
        if (getSelectedPlayers() == null || getSelectedPlayers().isEmpty())
        {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-selection-fail"), player);
            return;
        }

        GroupTeleportEvent groupTeleportEvent = new GroupTeleportEvent(getPluginInstance(), getDestination().asBukkitLocation(), player, getAcceptedPlayers());
        getPluginInstance().getServer().getPluginManager().callEvent(groupTeleportEvent);
        if (groupTeleportEvent.isCancelled()) return;
        if (groupTeleportEvent.getDestination() != null)
            setDestination(new SerializableLocation(groupTeleportEvent.getDestination()));

        new BukkitRunnable()
        {
            int time = 0;
            int duration = getPluginInstance().getConfig().getInt("teleportation-section.group-request-duration");
            String animationSet = getPluginInstance().getConfig().getString("teleportation-section.group-teleport-animation"),
                    teleportSound = getPluginInstance().getConfig().getString("general-section.global-sounds.teleport")
                            .toUpperCase().replace(" ", "_").replace("-", "_");

            @Override
            public void run()
            {
                time += 1;

                if (cancelled)
                {
                    getPluginInstance().getManager().getPaging().getPlayerSelectedMap().remove(player.getUniqueId());
                    getPluginInstance().getTeleportationHandler().clearGroupTemp(player);
                    cancel();
                    return;
                }

                if (time >= duration)
                {
                    if (!getAcceptedPlayers().isEmpty())
                    {

                        SerializableLocation serializableLocation = getDestination();
                        if (serializableLocation == null)
                        {
                            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.group-destination-teleport-fail"), player);
                            getPluginInstance().getManager().getPaging().getPlayerSelectedMap().remove(player.getUniqueId());
                            getPluginInstance().getTeleportationHandler().clearGroupTemp(player);
                            cancel();
                            return;
                        }

                        Location destinationLocation = getDestination().asBukkitLocation();
                        if (destinationLocation != null)
                        {
                            int teleportCount = 0;
                            for (int i = -1; ++i < getAcceptedPlayers().size(); )
                            {
                                UUID playerUniqueId = getAcceptedPlayers().get(i);
                                if (playerUniqueId == null) continue;

                                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(playerUniqueId);
                                if (offlinePlayer == null || !offlinePlayer.isOnline()) continue;

                                getPluginInstance().getTeleportationHandler().teleportPlayer(offlinePlayer.getPlayer(), destinationLocation);
                                if (!teleportSound.equalsIgnoreCase(""))
                                    player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound), 1, 1);
                                if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":"))
                                {
                                    String[] themeArgs = animationSet.split(":");
                                    String teleportTheme = themeArgs[2];
                                    if (teleportTheme.contains("/"))
                                    {
                                        String[] teleportThemeArgs = teleportTheme.split("/");
                                        getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
                                        getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, teleportThemeArgs[1],
                                                EnumContainer.Animation.valueOf(teleportThemeArgs[0].toUpperCase().replace(" ", "_")
                                                        .replace("-", "_")), 1);
                                    }
                                }

                                teleportCount += 1;
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.group-teleported")
                                        .replace("{player}", player.getName()), offlinePlayer.getPlayer());
                            }

                            getPluginInstance().getTeleportationHandler().teleportPlayer(player, destinationLocation);
                            if (!teleportSound.equalsIgnoreCase(""))
                                player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound), 1, 1);
                            if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":"))
                            {
                                String[] themeArgs = animationSet.split(":");
                                String teleportTheme = themeArgs[2];
                                if (teleportTheme.contains("/"))
                                {
                                    String[] teleportThemeArgs = teleportTheme.split("/");
                                    getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
                                    getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, teleportThemeArgs[1],
                                            EnumContainer.Animation.valueOf(teleportThemeArgs[0].toUpperCase().replace(" ", "_")
                                                    .replace("-", "_")), 1);
                                }
                            }

                            if (teleportCount > 0)
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.group-teleport-success"), player);
                            else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.group-teleport-fail"), player);
                        } else
                            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.destination-invalid"), player);

                    } else
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.group-teleport-fail"), player);

                    getPluginInstance().getManager().getPaging().getPlayerSelectedMap().remove(player.getUniqueId());
                    getPluginInstance().getTeleportationHandler().clearGroupTemp(player);
                    cancel();
                }
            }
        }.runTaskTimer(getPluginInstance(), 0, 20);
    }

    // getters & setters
    private HyperDrive getPluginInstance()
    {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance)
    {
        this.pluginInstance = pluginInstance;
    }

    public List<UUID> getSelectedPlayers()
    {
        return selectedPlayers;
    }

    private void setSelectedPlayers(List<UUID> selectedPlayers)
    {
        this.selectedPlayers = selectedPlayers;
    }

    public List<UUID> getAcceptedPlayers()
    {
        return acceptedPlayers;
    }

    private void setAcceptedPlayers(List<UUID> acceptedPlayers)
    {
        this.acceptedPlayers = acceptedPlayers;
    }

    public SerializableLocation getDestination()
    {
        return destination;
    }

    public void setDestination(SerializableLocation destination)
    {
        this.destination = destination;
    }

    public boolean isCancelled()
    {
        return cancelled;
    }

    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }
}
