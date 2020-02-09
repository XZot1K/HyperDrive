/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.cmds;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;
import xzot1k.plugins.hd.api.events.MenuOpenEvent;
import xzot1k.plugins.hd.api.objects.SerializableLocation;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class TeleportationCommands implements CommandExecutor {
    private HyperDrive pluginInstance;
    private List<UUID> toggledPlayers, tpaHereSentPlayers;
    private HashMap<UUID, UUID> tpaSentMap;
    private HashMap<UUID, SerializableLocation> lastLocationMap;
    private SerializableLocation spawnLocation, firstJoinLocation;

    public TeleportationCommands(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);
        setToggledPlayers(new ArrayList<>());
        setLastLocationMap(new HashMap<>());
        setTpaHereSentPlayers(new ArrayList<>());
        setTpaSentMap(new HashMap<>());

        File file = new File(getPluginInstance().getDataFolder(), "/data.yml");
        if (!file.exists()) {
            getPluginInstance().log(Level.INFO, "The 'data.yml' file does not exist. Skipping extra data values.");
            return;
        }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection cs = yaml.getConfigurationSection("");
        if (cs == null) return;

        Collection<String> keys = cs.getKeys(false);
        if (keys.isEmpty()) return;

        if (keys.contains("spawn"))
            setSpawnLocation(new SerializableLocation(yaml.getString("spawn.world"), yaml.getDouble("spawn.x"), yaml.getDouble("spawn.y"),
                    yaml.getDouble("spawn.z"), (float) yaml.getDouble("spawn.yaw"), (float) yaml.getDouble("spawn.pitch")));
        if (keys.contains("first-join-spawn"))
            setFirstJoinLocation(new SerializableLocation(yaml.getString("first-join-spawn.world"), yaml.getDouble("first-join-spawn.x"), yaml.getDouble("first-join-spawn.y"),
                    yaml.getDouble("first-join-spawn.z"), (float) yaml.getDouble("first-join-spawn.yaw"), (float) yaml.getDouble("first-join-spawn.pitch")));
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {

        switch (command.getName().toLowerCase()) {
            case "grouprandomteleport":
                runGroupRTPCommand(commandSender);
                return true;
            case "randomteleport":

                switch (args.length) {
                    case 0:
                        runRTPCommand(commandSender, null, null);
                        return true;
                    case 1:
                        runRTPCommand(commandSender, args[0], null);
                        return true;
                    case 2:
                        runRTPCommand(commandSender, args[0], args[1]);
                        return true;
                }

                if (commandSender.hasPermission("hyperdrive.admin.help"))
                    getPluginInstance().getMainCommands().sendAdminHelpPage(commandSender, 1);
                else getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;
            case "spawn":
                switch (args.length) {
                    case 0:
                        runSpawnCommand(commandSender, null);
                        return true;
                    case 1:
                        runSpawnCommand(commandSender, args[0]);
                        return true;
                }

                if (commandSender.hasPermission("hyperdrive.admin.help"))
                    getPluginInstance().getMainCommands().sendAdminHelpPage(commandSender, 1);
                else getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;
            case "crossserver":

                switch (args.length) {
                    case 8:
                        runCrossServer(commandSender, args);
                        return true;
                    case 6:
                        runCrossServerShortened(commandSender, args);
                        return true;
                }

                if (commandSender.hasPermission("hyperdrive.admin.help"))
                    getPluginInstance().getMainCommands().sendAdminHelpPage(commandSender, 1);
                else getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;
            case "teleport":

                switch (args.length) {
                    case 1:
                        runTeleportCommand(commandSender, args[0]);
                        return true;
                    case 2:
                        runTeleportCommand(commandSender, args[0], args[1]);
                        return true;
                }

                if (commandSender.hasPermission("hyperdrive.admin.help"))
                    getPluginInstance().getMainCommands().sendAdminHelpPage(commandSender, 1);
                else getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;
            case "teleporthere":

                if (args.length >= 1) {
                    runTeleportHereCommand(commandSender, args[0]);
                    return true;
                }

                if (commandSender.hasPermission("hyperdrive.admin.help"))
                    getPluginInstance().getMainCommands().sendAdminHelpPage(commandSender, 1);
                else getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;
            case "teleportoverride":

                if (args.length >= 1) {
                    runTeleportOverrideCommand(commandSender, args[0]);
                    return true;
                }

                if (commandSender.hasPermission("hyperdrive.admin.help"))
                    getPluginInstance().getMainCommands().sendAdminHelpPage(commandSender, 1);
                else getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;
            case "teleportoverridehere":

                if (args.length >= 1) {
                    runTeleportOverrideHereCommand(commandSender, args[0]);
                    return true;
                }

                if (commandSender.hasPermission("hyperdrive.admin.help"))
                    getPluginInstance().getMainCommands().sendAdminHelpPage(commandSender, 1);
                else getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;
            case "teleportposition":

                if (args.length >= 5) {
                    runTeleportPosCommand(commandSender, args[0], args[1], args[2], args[3], args[4]);
                    return true;
                } else if (args.length >= 4) {
                    runTeleportPosCommand(commandSender, args[0], args[1], args[2], args[3]);
                    return true;
                } else if (args.length >= 3) {
                    runTeleportPosCommand(commandSender, args[0], args[1], args[2]);
                    return true;
                }

                if (commandSender.hasPermission("hyperdrive.admin.help"))
                    getPluginInstance().getMainCommands().sendAdminHelpPage(commandSender, 1);
                else getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;
            case "teleportaskhere":

                if (args.length >= 1) {
                    runTeleportAskHere(commandSender, args[0]);
                    return true;
                }

                getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;

            case "teleportask":

                if (args.length >= 1) {
                    runTeleportAsk(commandSender, args[0]);
                    return true;
                }

                getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;

            case "teleportaccept":
                if (args.length >= 1) {
                    runTeleportAskAccept(commandSender, args[0]);
                } else {
                    runTeleportAskAccept(commandSender);
                }
                return true;
            case "teleportdeny":
                if (args.length >= 1) {
                    runTeleportAskDeny(commandSender, args[0]);
                } else {
                    runTeleportAskDeny(commandSender);
                }
                return true;
            case "teleporttoggle":

                runTeleportToggle(commandSender);
                return true;
            case "back":

                if (args.length >= 1)
                    runBack(commandSender, args[0]);
                else runBack(commandSender);
                return true;
        }

        return false;
    }

    private void runGroupRTPCommand(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getLangConfig().getString("no-permission")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.use.rtpgroup")) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        List<UUID> onlinePlayers = getPluginInstance().getManager().getPlayerUUIDs();
        onlinePlayers.remove(player.getUniqueId());
        if (onlinePlayers.size() <= 0) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getLangConfig().getString("no-players-found"), player);
            return;
        }

        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("random-teleport-start"), player);
        getPluginInstance().getTeleportationHandler().getDestinationMap().remove(player.getUniqueId());
        getPluginInstance().getTeleportationHandler().updateDestinationWithRandomLocation(player, player.getLocation(), player.getWorld());

        Inventory inventory = getPluginInstance().getManager().buildPlayerSelectionMenu(player);
        MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.PLAYER_SELECTION, inventory, player);
        getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
        if (!menuOpenEvent.isCancelled()) {
            player.openInventory(inventory);
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getLangConfig().getString("player-selection-group"), player);
        }
    }

    private void runRTPCommand(CommandSender commandSender, String playerName, String worldName) {

        final List<String> forbiddenWorlds = getPluginInstance().getConfig().getStringList("random-teleport-section.forbidden-worlds");
        final String title = getPluginInstance().getConfig().getString("random-teleport-section.start-title"),
                subTitle = getPluginInstance().getConfig().getString("random-teleport-section.start-sub-title");
        final int rtpDelay = getPluginInstance().getConfig().getInt("random-teleport-section.delay-duration");

        if (playerName == null && worldName == null) {

            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
                return;
            }

            Player player = (Player) commandSender;
            if (!player.hasPermission("hyperdrive.rtp")) {
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
                return;
            }

            if (!player.hasPermission("hyperdrive.rtpbypass")) {
                long cooldownDurationLeft = getPluginInstance().getManager().getCooldownDuration(player, "rtp", getPluginInstance().getConfig().getInt("random-teleport-section.cooldown"));
                if (cooldownDurationLeft > 0) {
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("random-teleport-cooldown"))
                            .replace("{duration}", String.valueOf(cooldownDurationLeft)), player);
                    return;
                }
            }

            if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("already-teleporting"), player);
                return;
            }

            for (String world : forbiddenWorlds) {
                if (world.equalsIgnoreCase(player.getWorld().getName())) {
                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("forbidden-world"), player);
                    return;
                }
            }

            getPluginInstance().getManager().updateCooldown(player, "rtp");
            int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? rtpDelay : 0;
            getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "rtp", player.getWorld().getName(), duration);

            if (title != null && subTitle != null)
                getPluginInstance().getManager().sendTitle(player, title.replace("{duration}", String.valueOf(duration)),
                        subTitle.replace("{duration}", String.valueOf(duration)), 0, 5, 0);
            getPluginInstance().getManager().sendActionBar(player, Objects.requireNonNull(getPluginInstance().getConfig().getString("random-teleport-section.start-bar-message"))
                    .replace("{duration}", String.valueOf(duration)));
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("random-teleport-begin"))
                    .replace("{duration}", String.valueOf(duration)), player);
            return;
        }

        if (playerName != null && worldName == null) {
            if (!commandSender.hasPermission("hyperdrive.admin.rtp") && !commandSender.hasPermission("hyperdrive.rtp")) {
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), (Player) commandSender);
                else
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-permission")));
                return;
            } else if (!commandSender.hasPermission("hyperdrive.admin.rtp") && commandSender.hasPermission("hyperdrive.rtp") && commandSender instanceof Player) {
                Player player = (Player) commandSender;
                World world = getPluginInstance().getServer().getWorld(playerName);
                if (world == null) {
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig()
                            .getString("world-invalid")).replace("{world}", playerName)));
                    return;
                }

                if (!player.hasPermission("hyperdrive.rtpbypass")) {
                    long cooldownDurationLeft = getPluginInstance().getManager().getCooldownDuration(player, "rtp", getPluginInstance().getConfig().getInt("random-teleport-section.cooldown"));
                    if (cooldownDurationLeft > 0) {
                        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("random-teleport-cooldown"))
                                .replace("{duration}", String.valueOf(cooldownDurationLeft)), player);
                        return;
                    }
                }

                if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("already-teleporting"), player);
                    return;
                }

                for (String wName : getPluginInstance().getConfig().getStringList("random-teleport-section.forbidden-worlds")) {
                    if (wName.equalsIgnoreCase(player.getWorld().getName())) {
                        commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig()
                                .getString("random-teleport-admin-forbidden")).replace("{world}", playerName)));
                        return;
                    }
                }

                int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? rtpDelay : 0;
                getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "rtp", player.getWorld().getName(), duration);

                if (title != null && subTitle != null)
                    getPluginInstance().getManager().sendTitle(player, title.replace("{duration}", String.valueOf(duration)),
                            subTitle.replace("{duration}", String.valueOf(duration)), 0, 5, 0);
                getPluginInstance().getManager().sendActionBar(player, Objects.requireNonNull(getPluginInstance().getConfig().getString("random-teleport-section.start-bar-message"))
                        .replace("{duration}", String.valueOf(duration)));
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("random-teleport-begin"))
                        .replace("{duration}", String.valueOf(duration)), player);

                getPluginInstance().getManager().updateCooldown(player, "rtp");
                return;
            }

            Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
            if (enteredPlayer == null || !enteredPlayer.isOnline()) {
                commandSender.sendMessage(getPluginInstance().getManager().colorText(
                        Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                                .replace("{player}", playerName)));
                return;
            }

            if (!enteredPlayer.hasPermission("hyperdrive.rtpbypass")) {
                long cooldownDurationLeft = getPluginInstance().getManager().getCooldownDuration(enteredPlayer, "rtp", getPluginInstance().getConfig().getInt("random-teleport-section.cooldown"));
                if (cooldownDurationLeft > 0) {
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("random-teleport-cooldown"))
                            .replace("{duration}", String.valueOf(cooldownDurationLeft)), enteredPlayer);
                    return;
                }
            }

            if (getPluginInstance().getTeleportationHandler().isTeleporting(enteredPlayer)) {
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-already-teleporting")).replace("{player}", enteredPlayer.getName()), (Player) commandSender);
                else
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-already-teleporting")).replace("{player}", enteredPlayer.getName())));
                return;
            }

            for (String world : forbiddenWorlds) {
                if (world.equalsIgnoreCase(enteredPlayer.getWorld().getName())) {
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("random-teleport-admin-forbidden"))
                            .replace("{player}", enteredPlayer.getName()).replace("{world}", Objects.requireNonNull(enteredPlayer.getLocation().getWorld()).getName())));
                    return;
                }
            }

            int duration = !enteredPlayer.hasPermission("hyperdrive.tpdelaybypass") ? rtpDelay : 0;
            getPluginInstance().getTeleportationHandler().updateTeleportTemp(enteredPlayer, "rtp", enteredPlayer.getWorld().getName(), duration);

            if (title != null && subTitle != null)
                getPluginInstance().getManager().sendTitle(enteredPlayer, title.replace("{duration}", String.valueOf(duration)),
                        subTitle.replace("{duration}", String.valueOf(duration)), 0, 5, 0);
            getPluginInstance().getManager().sendActionBar(enteredPlayer, Objects.requireNonNull(getPluginInstance().getConfig().getString("random-teleport-section.start-bar-message"))
                    .replace("{duration}", String.valueOf(duration)));
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("random-teleport-begin")).replace("{duration}", String.valueOf(duration)), enteredPlayer);

            if (!commandSender.getName().equalsIgnoreCase(enteredPlayer.getName()))
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects
                        .requireNonNull(getPluginInstance().getLangConfig().getString("random-teleport-admin"))
                        .replace("{player}", enteredPlayer.getName())
                        .replace("{world}", Objects.requireNonNull(enteredPlayer.getLocation().getWorld()).getName())));
            return;
        }

        if (playerName != null) {
            if (!commandSender.hasPermission("hyperdrive.admin.rtp")) {
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendCustomMessage(
                            getPluginInstance().getLangConfig().getString("no-permission"),
                            (Player) commandSender);
                else
                    commandSender.sendMessage(getPluginInstance().getManager()
                            .colorText(getPluginInstance().getLangConfig().getString("no-permission")));
                return;
            }

            Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
            if (enteredPlayer == null || !enteredPlayer.isOnline()) {
                commandSender.sendMessage(getPluginInstance().getManager().colorText(
                        Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                                .replace("{player}", playerName)));
                return;
            }

            if (!enteredPlayer.hasPermission("hyperdrive.rtpbypass")) {
                long cooldownDurationLeft = getPluginInstance().getManager().getCooldownDuration(enteredPlayer, "rtp", getPluginInstance().getConfig().getInt("random-teleport-section.cooldown"));
                if (cooldownDurationLeft > 0) {
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("random-teleport-cooldown"))
                            .replace("{duration}", String.valueOf(cooldownDurationLeft)), enteredPlayer);
                    return;
                }
            }

            if (getPluginInstance().getTeleportationHandler().isTeleporting(enteredPlayer)) {
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-already-teleporting")).replace("{player}", enteredPlayer.getName()), (Player) commandSender);
                else
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-already-teleporting")).replace("{player}", enteredPlayer.getName())));
                return;
            }

            World world = getPluginInstance().getServer().getWorld(worldName);
            if (world == null) {
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("world-invalid"))
                        .replace("{world}", worldName)));
                return;
            }

            for (String wn : getPluginInstance().getConfig().getStringList("random-teleport-section.forbidden-worlds")) {
                if (wn.equalsIgnoreCase(world.getName())) {
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("random-teleport-admin-forbidden"))
                            .replace("{player}", enteredPlayer.getName()).replace("{world}", world.getName())));
                    return;
                }
            }

            int duration = !enteredPlayer.hasPermission("hyperdrive.tpdelaybypass") ? rtpDelay : 0;
            getPluginInstance().getTeleportationHandler().updateTeleportTemp(enteredPlayer, "rtp", world.getName(), duration);

            if (title != null && subTitle != null)
                getPluginInstance().getManager().sendTitle(enteredPlayer, title.replace("{duration}", String.valueOf(duration)),
                        subTitle.replace("{duration}", String.valueOf(duration)), 0, 5, 0);
            getPluginInstance().getManager().sendActionBar(enteredPlayer, Objects.requireNonNull(getPluginInstance().getConfig().getString("random-teleport-section.start-bar-message"))
                    .replace("{duration}", String.valueOf(duration)));
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("random-teleport-begin")).replace("{duration}", String.valueOf(duration)), enteredPlayer);

            commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("random-teleport-admin"))
                    .replace("{player}", enteredPlayer.getName()).replace("{world}", world.getName())));
        }
    }

    private void runSpawnCommand(CommandSender commandSender, String firstArg) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.spawn")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        if (!player.hasPermission("hyperdrive.admin.spawn") || (firstArg == null || firstArg.equalsIgnoreCase(""))) {
            if (getSpawnLocation() == null) {
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("spawn-invalid"), player);
                return;
            }

            Location spawnLocation = getSpawnLocation().asBukkitLocation();
            player.setVelocity(new Vector(0, 0, 0));
            player.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.COMMAND);

            String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                    .toUpperCase().replace(" ", "_").replace("-", "_"),
                    animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
            if (!teleportSound.equalsIgnoreCase(""))
                player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound), 1, 1);
            if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
                String[] themeArgs = animationSet.split(":");
                getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
                getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, themeArgs[1],
                        EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                                .replace("-", "_")), 1);
            }

            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-spawn"), player);
            return;
        }

        if (firstArg.equalsIgnoreCase("set")) {
            setSpawnLocation(new SerializableLocation(player.getLocation()));
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("spawn-set"), player);
        } else if (firstArg.equalsIgnoreCase("setfirstjoin") || firstArg.equalsIgnoreCase("sfj")) {
            setFirstJoinLocation(new SerializableLocation(player.getLocation()));
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("spawn-set-first-join"), player);
        } else if (firstArg.equalsIgnoreCase("clear")) {
            setFirstJoinLocation(null);
            setSpawnLocation(null);
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("spawns-cleared"), player);
        }
    }

    private void runCrossServerShortened(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission("hyperdrive.admin.crossserver")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-permission")));
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(args[0]);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                        .replace("{player}", args[0]), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                        .replace("{player}", args[0])));
            return;
        }

        if (getPluginInstance().getConfig().getBoolean("mysql-connection-section.use-mysql") && getPluginInstance().getDatabaseConnection() == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance()
                        .getLangConfig().getString("mysql-disabled")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance()
                        .getLangConfig().getString("mysql-disabled"))));
            return;
        }

        String serverName = args[1];
        World world = getPluginInstance().getServer().getWorld(args[2]);
        if (world == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("world-invalid"))
                        .replace("{world}", args[1]), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("world-invalid"))
                        .replace("{world}", args[1])));
            return;
        }

        // X coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[3])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid"))));
            return;
        }

        // Y coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[4])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid"))));
            return;
        }

        // Z coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[5])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid"))));
            return;
        }

        Location location = new Location(world, Double.parseDouble(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), enteredPlayer.getLocation().getYaw(),
                enteredPlayer.getLocation().getPitch());

        getPluginInstance().getManager().teleportCrossServer(enteredPlayer, getPluginInstance().getBungeeListener().getIPFromMap(serverName), serverName, new SerializableLocation(location));
        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("cross-server"))
                    .replace("{player}", enteredPlayer.getName()).replace("{server}", serverName)), (Player) commandSender);
        else
            commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("cross-server"))
                    .replace("{player}", enteredPlayer.getName()).replace("{server}", serverName))));
    }

    private void runCrossServer(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission("hyperdrive.admin.crossserver")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-permission")));
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(args[0]);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                        .replace("{player}", args[0]), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                        .replace("{player}", args[0])));
            return;
        }

        if (getPluginInstance().getConfig().getBoolean("mysql-connection-section.use-mysql") && getPluginInstance().getDatabaseConnection() == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance()
                        .getLangConfig().getString("mysql-disabled")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance()
                        .getLangConfig().getString("mysql-disabled"))));
            return;
        }

        String serverName = args[1];
        World world = getPluginInstance().getServer().getWorld(args[2]);
        if (world == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("world-invalid"))
                        .replace("{world}", args[1]), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("world-invalid"))
                        .replace("{world}", args[1])));
            return;
        }

        // X coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[3])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid"))));
            return;
        }

        // Y coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[4])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid"))));
            return;
        }

        // Z coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[5])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid"))));
            return;
        }

        // Yaw coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[6])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid"))));
            return;
        }

        // Pitch coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[7])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid"))));
            return;
        }

        Location location = new Location(world, Double.parseDouble(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Float.parseFloat(args[6]), Float.parseFloat(args[7]));
        getPluginInstance().getManager().teleportCrossServer(enteredPlayer, getPluginInstance().getBungeeListener().getIPFromMap(serverName), serverName, new SerializableLocation(location));
        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("cross-server"))
                    .replace("{player}", enteredPlayer.getName()).replace("{server}", serverName)), (Player) commandSender);
        else
            commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("cross-server"))
                    .replace("{player}", enteredPlayer.getName()).replace("{server}", serverName))));
    }

    private void runBack(CommandSender commandSender, String playerName) {
        if (!commandSender.hasPermission("hyperdrive.admin.back")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-permission")));
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                        .replace("{player}", playerName), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                        .replace("{player}", playerName)));
            return;
        }

        Location lastLocation = getLastLocation(enteredPlayer);
        if (lastLocation == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-last-location"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-last-location")));
            return;
        }

        enteredPlayer.setVelocity(new Vector(0, 0, 0));
        enteredPlayer.teleport(lastLocation, PlayerTeleportEvent.TeleportCause.COMMAND);
        if (commandSender instanceof Player && !((Player) commandSender).getUniqueId().toString().equals(enteredPlayer.getUniqueId().toString()))
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("teleported-last-location"))
                    .replace("{player}", enteredPlayer.getName()), (Player) commandSender);
        else
            commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("teleported-last-location"))
                    .replace("{player}", enteredPlayer.getName())));
        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-last-location"), enteredPlayer);
    }

    private void runBack(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.back")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("self-teleportation-toggled"), player);
            return;
        }

        Location lastLocation = getLastLocation(player);
        if (lastLocation == null) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-last-location"), player);
            return;
        }

        player.setVelocity(new Vector(0, 0, 0));
        player.teleport(lastLocation, PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                .toUpperCase().replace(" ", "_").replace("-", "_"),
                animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
        if (!teleportSound.equalsIgnoreCase(""))
            player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound), 1, 1);
        if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
            String[] themeArgs = animationSet.split(":");
            getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
            getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, themeArgs[1],
                    EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                            .replace("-", "_")), 1);
        }

        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-last-location"), player);
    }

    private void runTeleportToggle(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpt")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getToggledPlayers().remove(player.getUniqueId());
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-toggled-on"), player);
        } else {
            getToggledPlayers().add(player.getUniqueId());
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-toggled-off"), player);
        }
    }

    private void runTeleportAskDeny(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("self-teleportation-toggled"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-self"), player);
            return;
        }

        if (getTpaSentMap().containsKey(enteredPlayer.getUniqueId())) {
            UUID playerUniqueId = getTpaSentMap().get(enteredPlayer.getUniqueId());
            if (playerUniqueId == null || player.getUniqueId().toString().equals(playerUniqueId.toString())) {
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-invalid"))
                        .replace("{player}", enteredPlayer.getName()), player);
                return;
            }
        } else if (getTpaSentMap().isEmpty() || !getTpaSentMap().containsKey(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-invalid"))
                    .replace("{player}", enteredPlayer.getName()), player);
            return;
        }

        getTpaSentMap().remove(enteredPlayer.getUniqueId());
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-deny"))
                .replace("{player}", enteredPlayer.getName()), player);
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-denied"))
                .replace("{player}", player.getName()), enteredPlayer);
    }

    private void runTeleportAskAccept(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("self-teleportation-toggled"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-self"), player);
            return;
        }

        if (!getTpaSentMap().isEmpty() && getTpaSentMap().containsKey(enteredPlayer.getUniqueId())) {
            UUID playerUniqueId = getTpaSentMap().get(enteredPlayer.getUniqueId());
            if (playerUniqueId == null) {
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-invalid"))
                        .replace("{player}", enteredPlayer.getName()), player);
                return;
            }
        } else if (getTpaSentMap().isEmpty() || !getTpaSentMap().containsKey(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-invalid"))
                    .replace("{player}", enteredPlayer.getName()), player);
            return;
        }

        getTpaSentMap().remove(enteredPlayer.getUniqueId());
        if (getTpaHereSentPlayers().contains(enteredPlayer.getUniqueId())) {
            getTpaHereSentPlayers().remove(enteredPlayer.getUniqueId());
            player.teleport(enteredPlayer.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        } else enteredPlayer.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                .toUpperCase().replace(" ", "_").replace("-", "_"),
                animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
        if (!teleportSound.equalsIgnoreCase(""))
            player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound), 1, 1);
        if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
            String[] themeArgs = animationSet.split(":");
            getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
            getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, themeArgs[1],
                    EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                            .replace("-", "_")), 1);
        }

        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-accept"))
                .replace("{player}", enteredPlayer.getName()), player);
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-accepted"))
                .replace("{player}", player.getName()), enteredPlayer);
    }

    private void runTeleportAskAccept(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("self-teleportation-toggled"), player);
            return;
        }

        Player foundPlayer = null;
        for (UUID senderId : getTpaSentMap().keySet()) {
            UUID requestedPlayer = getTpaSentMap().get(senderId);
            if (requestedPlayer.toString().equals(player.getUniqueId().toString())) {
                foundPlayer = getPluginInstance().getServer().getPlayer(senderId);
                if (foundPlayer == null || !foundPlayer.isOnline()) {
                    getTpaSentMap().remove(requestedPlayer);
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                            .replace("{player}", foundPlayer != null ? foundPlayer.getName() : ""), player);
                    return;
                }

                UUID playerUniqueId = getTpaSentMap().get(foundPlayer.getUniqueId());
                if (playerUniqueId == null) {
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-empty"))
                            .replace("{player}", foundPlayer.getName()), player);
                    return;
                }

                getTpaSentMap().remove(foundPlayer.getUniqueId());
                if (getTpaHereSentPlayers().contains(foundPlayer.getUniqueId())) {
                    getTpaHereSentPlayers().remove(foundPlayer.getUniqueId());
                    player.teleport(foundPlayer.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
                } else foundPlayer.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
                break;
            }
        }

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                .toUpperCase().replace(" ", "_").replace("-", "_"),
                animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
        if (!teleportSound.equalsIgnoreCase(""))
            player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound), 1, 1);
        if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
            String[] themeArgs = animationSet.split(":");
            getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
            getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, themeArgs[1],
                    EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                            .replace("-", "_")), 1);
        }

        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-accept"))
                .replace("{player}", foundPlayer != null ? foundPlayer.getName() : ""), player);

        if (foundPlayer != null)
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-accepted"))
                    .replace("{player}", player.getName()), foundPlayer);
    }

    private void runTeleportAskDeny(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("self-teleportation-toggled"), player);
            return;
        }

        if (getTpaSentMap().isEmpty() || !getTpaSentMap().containsValue(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("player-tpa-empty"), player);
            return;
        }

        Player foundPlayer = null;
        for (UUID senderId : getTpaSentMap().keySet()) {
            UUID requestedPlayer = getTpaSentMap().get(senderId);
            if (requestedPlayer.toString().equals(player.getUniqueId().toString())) {
                foundPlayer = getPluginInstance().getServer().getPlayer(senderId);
                if (foundPlayer == null || !foundPlayer.isOnline()) {
                    getTpaSentMap().remove(requestedPlayer);
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                            .replace("{player}", foundPlayer != null ? foundPlayer.getName() : ""), player);
                    return;
                }

                getTpaSentMap().remove(requestedPlayer);
                break;
            }
        }

        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-deny"))
                .replace("{player}", foundPlayer != null ? foundPlayer.getName() : ""), player);

        if (foundPlayer != null)
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-denied"))
                    .replace("{player}", player.getName()), foundPlayer);
    }

    private void runTeleportAskHere(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("self-teleportation-toggled"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-here-self"), player);
            return;
        }

        if (getToggledPlayers().contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-teleportation-toggled"))
                    .replace("{player}", enteredPlayer.getName()), player);
            return;
        }

        updateTeleportAskRequest(player, enteredPlayer);
        if (!getTpaHereSentPlayers().contains(player.getUniqueId()))
            getTpaHereSentPlayers().add(player.getUniqueId());
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-sent"))
                .replace("{player}", enteredPlayer.getName()), player);
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-received"))
                .replace("{player}", player.getName()), enteredPlayer);

        new BukkitRunnable() {
            @Override
            public void run() {
                getTpaSentMap().remove(player.getUniqueId());
            }
        }.runTaskLaterAsynchronously(getPluginInstance(), 20 * getPluginInstance().getConfig().getInt("teleportation-section.teleport-ask-duration"));
    }

    private void runTeleportAsk(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("self-teleportation-toggled"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-self"), player);
            return;
        }

        if (getToggledPlayers().contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-teleportation-toggled"))
                    .replace("{player}", enteredPlayer.getName()), player);
            return;
        }

        updateTeleportAskRequest(player, enteredPlayer);
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-sent"))
                .replace("{player}", enteredPlayer.getName()), player);
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-tpa-received"))
                .replace("{player}", player.getName()), enteredPlayer);

        new BukkitRunnable() {
            @Override
            public void run() {
                getTpaSentMap().remove(player.getUniqueId());
            }
        }.runTaskLaterAsynchronously(getPluginInstance(), 20 * getPluginInstance().getConfig().getInt("teleportation-section.teleport-ask-duration"));
    }

    private void runTeleportPosCommand(CommandSender commandSender, String xEntry, String yEntry, String zEntry) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.admin.tppos")) {
            player.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-permission")));
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("self-teleportation-toggled"), player);
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(xEntry) || getPluginInstance().getManager().isNotNumeric(yEntry) || getPluginInstance().getManager().isNotNumeric(zEntry)) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("coordinate-invalid"), (Player) commandSender);
            return;
        }

        player.teleport(new Location(player.getWorld(), Double.parseDouble(xEntry), Double.parseDouble(yEntry), Double.parseDouble(zEntry),
                player.getLocation().getYaw(), player.getLocation().getPitch()), PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                .toUpperCase().replace(" ", "_").replace("-", "_"),
                animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
        if (!teleportSound.equalsIgnoreCase(""))
            player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound), 1, 1);
        if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
            String[] themeArgs = animationSet.split(":");
            getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
            getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, themeArgs[1],
                    EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                            .replace("-", "_")), 1);
        }

        commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("teleported-pos"))
                .replace("{world}", player.getWorld().getName()).replace("{x}", xEntry).replace("{y}", yEntry).replace("{z}", zEntry));
    }

    private void runTeleportPosCommand(CommandSender commandSender, String xEntry, String yEntry, String zEntry, String worldName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.admin.tppos")) {
            player.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-permission")));
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("self-teleportation-toggled"), player);
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(xEntry) || getPluginInstance().getManager().isNotNumeric(yEntry) || getPluginInstance().getManager().isNotNumeric(zEntry)) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("coordinate-invalid"), (Player) commandSender);
            return;
        }

        World world = getPluginInstance().getServer().getWorld(worldName);
        if (world == null) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("world-invalid")).replace("{world}", worldName), (Player) commandSender);
            return;
        }

        player.teleport(new Location(world, Double.parseDouble(xEntry), Double.parseDouble(yEntry), Double.parseDouble(zEntry),
                player.getLocation().getYaw(), player.getLocation().getPitch()), PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                .toUpperCase().replace(" ", "_").replace("-", "_"),
                animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
        if (!teleportSound.equalsIgnoreCase(""))
            player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound), 1, 1);
        if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
            String[] themeArgs = animationSet.split(":");
            getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
            getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, themeArgs[1],
                    EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                            .replace("-", "_")), 1);
        }

        commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("teleported-pos"))
                .replace("{world}", player.getWorld().getName()).replace("{x}", xEntry).replace("{y}", yEntry).replace("{z}", zEntry));
    }

    private void runTeleportPosCommand(CommandSender commandSender, String playerName, String xEntry, String yEntry, String zEntry, String worldName) {
        if (!commandSender.hasPermission("hyperdrive.admin.tppos")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-permission")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-permission")));
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                        .replace("{player}", playerName), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("player-invalid"))
                        .replace("{player}", playerName));
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(xEntry) || getPluginInstance().getManager().isNotNumeric(yEntry) || getPluginInstance().getManager().isNotNumeric(zEntry)) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("coordinate-invalid"))
                        .replace("{player}", playerName), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("coordinate-invalid")));
            return;
        }

        World world = getPluginInstance().getServer().getWorld(worldName);
        if (world == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("world-invalid")).replace("{world}", worldName)
                        .replace("{player}", playerName), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("world-invalid")).replace("{world}", worldName)
                        .replace("{world}", worldName));
            return;
        }

        enteredPlayer.teleport(new Location(world, Double.parseDouble(xEntry), Double.parseDouble(yEntry), Double.parseDouble(zEntry),
                enteredPlayer.getLocation().getYaw(), enteredPlayer.getLocation().getPitch()), PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                .toUpperCase().replace(" ", "_").replace("-", "_"),
                animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
        if (!teleportSound.equalsIgnoreCase(""))
            enteredPlayer.getWorld().playSound(enteredPlayer.getLocation(), Sound.valueOf(teleportSound), 1, 1);
        if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
            String[] themeArgs = animationSet.split(":");
            getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(enteredPlayer);
            getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(enteredPlayer, themeArgs[1],
                    EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                            .replace("-", "_")), 1);
        }

        if (commandSender instanceof Player && !((Player) commandSender).getUniqueId().toString().equals(enteredPlayer.getUniqueId().toString()))
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("teleport-pos"))
                    .replace("{player}", enteredPlayer.getName()), (Player) commandSender);
        else
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("teleport-pos"))
                    .replace("{player}", enteredPlayer.getName()));
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("teleported-pos"))
                .replace("{world}", world.getName()).replace("{x}", xEntry).replace("{y}", yEntry).replace("{z}", zEntry), enteredPlayer);
    }

    private void runTeleportOverrideHereCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.admin.tpohere")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-self"), player);
            return;
        }

        enteredPlayer.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("tp-receiver"))
                .replace("{player}", enteredPlayer.getName()), player);
    }

    private void runTeleportOverrideCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.admin.tpo")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-self"), player);
            return;
        }

        player.teleport(enteredPlayer.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("tp-victim"))
                .replace("{player}", enteredPlayer.getName()), player);
    }

    private void runTeleportHereCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.admin.tphere")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("self-teleportation-toggled"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-self"), player);
            return;
        }

        if (getToggledPlayers().contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("toggled-tp-player"))
                    .replace("{player}", enteredPlayer.getName()), player);
            return;
        }

        enteredPlayer.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                .toUpperCase().replace(" ", "_").replace("-", "_"),
                animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
        if (!teleportSound.equalsIgnoreCase(""))
            enteredPlayer.getWorld().playSound(enteredPlayer.getLocation(), Sound.valueOf(teleportSound), 1, 1);
        if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
            String[] themeArgs = animationSet.split(":");
            getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
            getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, themeArgs[1],
                    EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                            .replace("-", "_")), 1);
        }

        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("tp-receiver"))
                .replace("{player}", enteredPlayer.getName()), player);
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("tp-victim"))
                .replace("{player}", player.getName()), enteredPlayer);
    }

    private void runTeleportCommand(CommandSender commandSender, String playerName1, String playerName2) {
        if (!commandSender.hasPermission("hyperdrive.admin.tp")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-permission")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-permission")));
            return;
        }

        Player enteredPlayer1 = getPluginInstance().getServer().getPlayer(playerName1), enteredPlayer2 = getPluginInstance().getServer().getPlayer(playerName2);
        if (enteredPlayer1 == null || !enteredPlayer1.isOnline() || enteredPlayer2 == null || !enteredPlayer2.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("players-invalid"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("players-invalid")));
            return;
        }

        if (enteredPlayer1.getUniqueId().toString().equals(enteredPlayer2.getUniqueId().toString())) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-same"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("teleport-same")));
            return;
        }

        if (commandSender instanceof Player && ((Player) commandSender).getUniqueId().toString().equals(enteredPlayer1.getUniqueId().toString())
                && ((Player) commandSender).getUniqueId().toString().equals(enteredPlayer2.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-self"), (Player) commandSender);
            return;
        }

        enteredPlayer1.teleport(enteredPlayer2, PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                .toUpperCase().replace(" ", "_").replace("-", "_"),
                animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
        if (!teleportSound.equalsIgnoreCase(""))
            enteredPlayer1.getWorld().playSound(enteredPlayer1.getLocation(), Sound.valueOf(teleportSound), 1, 1);
        if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
            String[] themeArgs = animationSet.split(":");
            getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(enteredPlayer1);
            getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(enteredPlayer1, themeArgs[1],
                    EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                            .replace("-", "_")), 1);
        }

        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("tp-receiver"))
                .replace("{player}", enteredPlayer1.getName()), enteredPlayer2);
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("tp-victim"))
                .replace("{player}", enteredPlayer2.getName()), enteredPlayer1);
    }

    private void runTeleportCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.admin.tp")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("self-teleportation-toggled"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleport-self"), player);
            return;
        }

        if (getToggledPlayers().contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("toggled-tp-player"))
                    .replace("{player}", enteredPlayer.getName()), player);
            return;
        }

        player.teleport(enteredPlayer.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                .toUpperCase().replace(" ", "_").replace("-", "_"),
                animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
        if (!teleportSound.equalsIgnoreCase(""))
            player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound), 1, 1);
        if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
            String[] themeArgs = animationSet.split(":");
            getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
            getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, themeArgs[1],
                    EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                            .replace("-", "_")), 1);
        }

        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("tp-victim"))
                .replace("{player}", enteredPlayer.getName()), player);
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("tp-receiver"))
                .replace("{player}", player.getName()), enteredPlayer);
    }

    // helping methods
    public boolean hasRequestPending(Player player) {
        if (!getTpaSentMap().isEmpty() && getTpaSentMap().containsKey(player.getUniqueId())) {
            UUID playerUniqueId = getTpaSentMap().get(player.getUniqueId());
            return playerUniqueId != null;
        }

        return false;
    }

    private void updateTeleportAskRequest(Player sender, Player receiver) {
        getTpaSentMap().put(sender.getUniqueId(), receiver.getUniqueId());
    }

    public void updateLastLocation(Player player, Location location) {
        getLastLocationMap().put(player.getUniqueId(), new SerializableLocation(location));
    }

    public Location getLastLocation(Player player) {
        if (!getLastLocationMap().isEmpty() && getLastLocationMap().containsKey(player.getUniqueId())) {
            SerializableLocation serializableLocation = getLastLocationMap().get(player.getUniqueId());
            if (serializableLocation != null) return serializableLocation.asBukkitLocation();
        }

        return null;
    }

    // getters & setters
    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public List<UUID> getToggledPlayers() {
        return toggledPlayers;
    }

    private void setToggledPlayers(List<UUID> toggledPlayers) {
        this.toggledPlayers = toggledPlayers;
    }

    public HashMap<UUID, UUID> getTpaSentMap() {
        return tpaSentMap;
    }

    private void setTpaSentMap(HashMap<UUID, UUID> tpaSentMap) {
        this.tpaSentMap = tpaSentMap;
    }

    public HashMap<UUID, SerializableLocation> getLastLocationMap() {
        return lastLocationMap;
    }

    private void setLastLocationMap(HashMap<UUID, SerializableLocation> lastLocationMap) {
        this.lastLocationMap = lastLocationMap;
    }

    public List<UUID> getTpaHereSentPlayers() {
        return tpaHereSentPlayers;
    }

    private void setTpaHereSentPlayers(List<UUID> tpaHereSentPlayers) {
        this.tpaHereSentPlayers = tpaHereSentPlayers;
    }

    public SerializableLocation getSpawnLocation() {
        return spawnLocation;
    }

    private void setSpawnLocation(SerializableLocation spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public SerializableLocation getFirstJoinLocation() {
        return firstJoinLocation;
    }

    private void setFirstJoinLocation(SerializableLocation firstJoinLocation) {
        this.firstJoinLocation = firstJoinLocation;
    }
}
