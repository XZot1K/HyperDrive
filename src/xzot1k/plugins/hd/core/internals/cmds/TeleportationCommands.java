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
                        runRTPCommand(commandSender, false);
                        return true;
                    case 2:
                        runRTPCommand(commandSender, args[0], args[1], false);
                        return true;
                }

                if (commandSender.hasPermission("hyperdrive.admin.help"))
                    getPluginInstance().getMainCommands().sendAdminHelpPage(commandSender, 1);
                else getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;
            case "randomteleportadmin":

                switch (args.length) {
                    case 0:
                        runRTPCommand(commandSender, true);
                        return true;
                    case 2:
                        runRTPCommand(commandSender, args[0], args[1], true);
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
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.use.rtpgroup")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        for (String forbiddenWorld : getPluginInstance().getConfig().getStringList("random-teleport-section.forbidden-worlds")) {
            if (player.getWorld().getName().equalsIgnoreCase(forbiddenWorld)) {
                getPluginInstance().getManager().sendCustomMessage("forbidden-world", player);
                return;
            }
        }

        List<UUID> onlinePlayers = getPluginInstance().getManager().getPlayerUUIDs();
        onlinePlayers.remove(player.getUniqueId());
        if (onlinePlayers.size() <= 0) {
            getPluginInstance().getManager().sendCustomMessage("no-players-found", player);
            return;
        }

        getPluginInstance().getManager().sendCustomMessage("random-teleport-start", player);
        getPluginInstance().getTeleportationHandler().getDestinationMap().remove(player.getUniqueId());
        getPluginInstance().getTeleportationHandler().updateDestinationWithRandomLocation(player, player.getLocation(), player.getWorld());

        Inventory inventory = getPluginInstance().getManager().buildPlayerSelectionMenu(player);
        MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.PLAYER_SELECTION, inventory, player);
        getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
        if (!menuOpenEvent.isCancelled()) {
            player.openInventory(inventory);
            getPluginInstance().getManager().sendCustomMessage("player-selection-group", player);
        }
    }

    private void runRTPCommand(CommandSender commandSender, String playerName, String worldName, boolean bypassLimits) {
        if (!commandSender.hasPermission("hyperdrive.admin.rtp")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            String message = getPluginInstance().getLangConfig().getString("player-invalid");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{player}", playerName)));
            return;
        }

        if (!bypassLimits && !enteredPlayer.hasPermission("hyperdrive.rtpbypass")) {
            long cooldownDurationLeft = getPluginInstance().getManager().getCooldownDuration(enteredPlayer, "rtp", getPluginInstance().getConfig().getInt("random-teleport-section.cooldown"));
            if (cooldownDurationLeft > 0) {
                getPluginInstance().getManager().sendCustomMessage("random-teleport-cooldown", enteredPlayer, "{duration}:" + cooldownDurationLeft);
                return;
            }
        }

        if (getPluginInstance().getTeleportationHandler().isTeleporting(enteredPlayer)) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("player-already-teleporting", (Player) commandSender, "{player}:" + enteredPlayer.getName());
            else {
                String message = getPluginInstance().getLangConfig().getString("player-already-teleporting");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{player}", enteredPlayer.getName())));
            }
            return;
        }

        World world = getPluginInstance().getServer().getWorld(worldName);
        if (world == null) {
            String message = getPluginInstance().getLangConfig().getString("world-invalid");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{world}", worldName)));
            return;
        }

        for (String forbiddenWorld : getPluginInstance().getConfig().getStringList("random-teleport-section.forbidden-worlds")) {
            if (world.getName().equalsIgnoreCase(forbiddenWorld)) {
                String message = getPluginInstance().getLangConfig().getString("random-teleport-admin-forbidden");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{player}", enteredPlayer.getName()).replace("{world}", world.getName())));
                return;
            }
        }

        final String title = getPluginInstance().getConfig().getString("random-teleport-section.start-title"),
                subTitle = getPluginInstance().getConfig().getString("random-teleport-section.start-sub-title");
        final int rtpDelay = getPluginInstance().getConfig().getInt("random-teleport-section.delay-duration");
        int duration = (!bypassLimits && !enteredPlayer.hasPermission("hyperdrive.tpdelaybypass")) ? rtpDelay : 0;
        if (duration > 0) {
            getPluginInstance().getTeleportationHandler().updateTeleportTemp(enteredPlayer, "rtp", world.getName(), duration);
            if (title != null && subTitle != null)
                getPluginInstance().getManager().sendTitle(enteredPlayer, title.replace("{duration}", String.valueOf(duration)),
                        subTitle.replace("{duration}", String.valueOf(duration)), 0, 5, 0);

            String actionMessage = getPluginInstance().getConfig().getString("random-teleport-section.start-bar-message");
            if (actionMessage != null && !actionMessage.isEmpty())
                getPluginInstance().getManager().sendActionBar(enteredPlayer, actionMessage.replace("{duration}", String.valueOf(duration)));
            getPluginInstance().getManager().sendCustomMessage("random-teleport-begin", enteredPlayer, "{duration}:" + duration);
        } else getPluginInstance().getTeleportationHandler().randomlyTeleportPlayer(enteredPlayer, world);

        if (!commandSender.getName().equalsIgnoreCase(enteredPlayer.getName())) {
            String message = getPluginInstance().getLangConfig().getString("random-teleport-admin");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{player}", enteredPlayer.getName()).replace("{world}", world.getName())));
        }
    }

    private void runRTPCommand(CommandSender commandSender, boolean bypassLimits) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission(bypassLimits ? "hyperdrive.admin.rtp" : "hyperdrive.rtp")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (!bypassLimits && !player.hasPermission("hyperdrive.rtpbypass")) {
            long cooldownDurationLeft = getPluginInstance().getManager().getCooldownDuration(player, "rtp", getPluginInstance().getConfig().getInt("random-teleport-section.cooldown"));
            if (cooldownDurationLeft > 0) {
                getPluginInstance().getManager().sendCustomMessage("random-teleport-cooldown", player, "{duration}:" + cooldownDurationLeft);
                return;
            }
        }

        if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
            getPluginInstance().getManager().sendCustomMessage("already-teleporting", player);
            return;
        }

        for (String forbiddenWorld : getPluginInstance().getConfig().getStringList("random-teleport-section.forbidden-worlds")) {
            if (player.getWorld().getName().equalsIgnoreCase(forbiddenWorld)) {
                getPluginInstance().getManager().sendCustomMessage("forbidden-world", player);
                return;
            }
        }

        final String title = getPluginInstance().getConfig().getString("random-teleport-section.start-title"),
                subTitle = getPluginInstance().getConfig().getString("random-teleport-section.start-sub-title");
        final int rtpDelay = getPluginInstance().getConfig().getInt("random-teleport-section.delay-duration");
        int duration = (!bypassLimits && !player.hasPermission("hyperdrive.tpdelaybypass")) ? rtpDelay : 0;
        if (duration > 0) {
            getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "rtp", player.getWorld().getName(), duration);
            if (title != null && subTitle != null)
                getPluginInstance().getManager().sendTitle(player, title.replace("{duration}", String.valueOf(duration)),
                        subTitle.replace("{duration}", String.valueOf(duration)), 0, 5, 0);

            String actionMessage = getPluginInstance().getConfig().getString("random-teleport-section.start-bar-message");
            if (actionMessage != null && !actionMessage.isEmpty())
                getPluginInstance().getManager().sendActionBar(player, actionMessage.replace("{duration}", String.valueOf(duration)));

            getPluginInstance().getManager().sendCustomMessage("random-teleport-begin", player, "{duration}:" + duration);
        } else getPluginInstance().getTeleportationHandler().randomlyTeleportPlayer(player, player.getWorld());
    }

    private void runSpawnCommand(CommandSender commandSender, String firstArg) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.spawn")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (!player.hasPermission("hyperdrive.admin.spawn") || (firstArg == null || firstArg.equalsIgnoreCase(""))) {
            if (getSpawnLocation() == null) {
                getPluginInstance().getManager().sendCustomMessage("spawn-invalid", player);
                return;
            }

            int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance().getConfig().getInt("teleportation-section.standalone-delay-duration") : 0;
            getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "tp", getSpawnLocation().toString(), duration);
            getPluginInstance().getManager().sendCustomMessage("teleport-spawn", player, "{duration}:" + duration);
            return;
        }

        if (firstArg.equalsIgnoreCase("set")) {
            setSpawnLocation(new SerializableLocation(player.getLocation()));
            getPluginInstance().getManager().sendCustomMessage("spawn-set", player);
        } else if (firstArg.equalsIgnoreCase("setfirstjoin") || firstArg.equalsIgnoreCase("sfj")) {
            setFirstJoinLocation(new SerializableLocation(player.getLocation()));
            getPluginInstance().getManager().sendCustomMessage("spawn-set-first-join", player);
        } else if (firstArg.equalsIgnoreCase("clear")) {
            setFirstJoinLocation(null);
            setSpawnLocation(null);
            getPluginInstance().getManager().sendCustomMessage("spawns-cleared", player);
        }
    }

    private void runCrossServerShortened(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission("hyperdrive.admin.crossserver")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(args[0]);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("player-invalid", (Player) commandSender, "{player}:" + args[0]);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                        .replace("{player}", args[0])));
            return;
        }

        if (getPluginInstance().getConfig().getBoolean("mysql-connection-section.use-mysql") && getPluginInstance().getDatabaseConnection() == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("mysql-disabled", (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance()
                        .getLangConfig().getString("mysql-disabled"))));
            return;
        }

        String serverName = args[1];
        World world = getPluginInstance().getServer().getWorld(args[2]);
        if (world == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("world-invalid", (Player) commandSender, "{world}:" + args[1]);
            else {
                String message = getPluginInstance().getLangConfig().getString("world-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{world}", args[1])));
            }
            return;
        }

        // X coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[3])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("coordinate-invalid", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("coordinate-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        // Y coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[4])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("coordinate-invalid", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("coordinate-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        // Z coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[5])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("coordinate-invalid", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("coordinate-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        Location location = new Location(world, Double.parseDouble(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), enteredPlayer.getLocation().getYaw(),
                enteredPlayer.getLocation().getPitch());

        getPluginInstance().getManager().teleportCrossServer(enteredPlayer, serverName, new SerializableLocation(location));
        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage("cross-server", (Player) commandSender, "{player}:" + enteredPlayer.getName(), "{server}:" + serverName);
        else {
            String message = getPluginInstance().getLangConfig().getString("cross-server");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{player}", enteredPlayer.getName()).replace("{server}", serverName)));
        }
    }

    private void runCrossServer(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission("hyperdrive.admin.crossserver")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(args[0]);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("player-invalid", (Player) commandSender, "{player}:" + args[0]);
            else {
                String message = getPluginInstance().getLangConfig().getString("player-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{player}", args[0])));
            }
            return;
        }

        if (getPluginInstance().getConfig().getBoolean("mysql-connection-section.use-mysql") && getPluginInstance().getDatabaseConnection() == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("mysql-disabled", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("mysql-disabled");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        String serverName = args[1];
        World world = getPluginInstance().getServer().getWorld(args[2]);
        if (world == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("world-invalid", (Player) commandSender, "{world}:" + args[1]);
            else {
                String message = getPluginInstance().getLangConfig().getString("world-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{world}", args[1])));
            }
            return;
        }

        // X coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[3])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("coordinate-invalid", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("coordinate-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        // Y coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[4])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("coordinate-invalid", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("coordinate-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        // Z coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[5])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("coordinate-invalid", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("coordinate-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        // Yaw coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[6])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("coordinate-invalid", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("coordinate-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        // Pitch coordinate
        if (getPluginInstance().getManager().isNotNumeric(args[7])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("coordinate-invalid", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("coordinate-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        Location location = new Location(world, Double.parseDouble(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Float.parseFloat(args[6]), Float.parseFloat(args[7]));
        getPluginInstance().getManager().teleportCrossServer(enteredPlayer, serverName, new SerializableLocation(location));
        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage("cross-server", (Player) commandSender, "{player}:" + enteredPlayer.getName(), "{server}:" + serverName);
        else {
            String message = getPluginInstance().getLangConfig().getString("cross-server");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{player}", enteredPlayer.getName()).replace("{server}", serverName)));
        }
    }

    private void runBack(CommandSender commandSender, String playerName) {
        if (!commandSender.hasPermission("hyperdrive.admin.back")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("player-invalid", (Player) commandSender, "{player}:" + playerName);
            else {
                String message = getPluginInstance().getLangConfig().getString("player-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{player}", playerName)));
            }
            return;
        }

        Location lastLocation = getLastLocation(enteredPlayer);
        if (lastLocation == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-last-location", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-last-location");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        enteredPlayer.setVelocity(new Vector(0, 0, 0));
        enteredPlayer.teleport(lastLocation);
        getPluginInstance().getTeleportationCommands().updateLastLocation(enteredPlayer, lastLocation);
        if (commandSender instanceof Player && !((Player) commandSender).getUniqueId().toString().equals(enteredPlayer.getUniqueId().toString()))
            getPluginInstance().getManager().sendCustomMessage("teleported-last-location", (Player) commandSender, "{player}:" + enteredPlayer.getName());
        else {
            String message = getPluginInstance().getLangConfig().getString("teleported-last-location");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{player}", enteredPlayer.getName())));
        }
        getPluginInstance().getManager().sendCustomMessage("teleport-last-location", enteredPlayer);
    }

    private void runBack(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.back")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("self-teleportation-toggled", player);
            return;
        }

        Location lastLocation = getLastLocation(player);
        if (lastLocation == null) {
            getPluginInstance().getManager().sendCustomMessage("no-last-location", player);
            return;
        }

        int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance().getConfig().getInt("teleportation-section.standalone-delay-duration") : 0;
        getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "tp", new SerializableLocation(lastLocation).toString(), duration);

        player.setVelocity(new Vector(0, 0, 0));
        player.teleport(lastLocation);
        getPluginInstance().getTeleportationCommands().updateLastLocation(player, lastLocation);
        getPluginInstance().getManager().sendCustomMessage("teleport-last-location", player);
    }

    private void runTeleportToggle(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpt")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getToggledPlayers().remove(player.getUniqueId());
            getPluginInstance().getManager().sendCustomMessage("teleport-toggled-on", player);
        } else {
            getToggledPlayers().add(player.getUniqueId());
            getPluginInstance().getManager().sendCustomMessage("teleport-toggled-off", player);
        }
    }

    private void runTeleportAskDeny(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("self-teleportation-toggled", player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage("player-invalid", player, "{player}:" + playerName);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage("teleport-self", player);
            return;
        }

        if (getTpaSentMap().containsKey(enteredPlayer.getUniqueId())) {
            UUID playerUniqueId = getTpaSentMap().get(enteredPlayer.getUniqueId());
            if (playerUniqueId == null || player.getUniqueId().toString().equals(playerUniqueId.toString())) {
                getPluginInstance().getManager().sendCustomMessage("player-tpa-invalid", player, "{player}:" + enteredPlayer.getName());
                return;
            }
        } else if (getTpaSentMap().isEmpty() || !getTpaSentMap().containsKey(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("player-tpa-invalid", player, "{player}:" + enteredPlayer.getName());
            return;
        }

        getTpaSentMap().remove(enteredPlayer.getUniqueId());
        getPluginInstance().getManager().sendCustomMessage("player-tpa-deny", player, "{player}:" + enteredPlayer.getName());
        getPluginInstance().getManager().sendCustomMessage("player-tpa-denied", enteredPlayer, "{player}:" + player.getName());
    }

    private void runTeleportAskAccept(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("self-teleportation-toggled", player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage("player-invalid", player, "{player}:" + playerName);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage("teleport-self", player);
            return;
        }

        if (!getTpaSentMap().isEmpty() && getTpaSentMap().containsKey(enteredPlayer.getUniqueId())) {
            UUID playerUniqueId = getTpaSentMap().get(enteredPlayer.getUniqueId());
            if (playerUniqueId == null) {
                getPluginInstance().getManager().sendCustomMessage("player-tpa-invalid", player, "{player}:" + enteredPlayer.getName());
                return;
            }
        } else if (getTpaSentMap().isEmpty() || !getTpaSentMap().containsKey(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("player-tpa-invalid", player, "{player}:" + enteredPlayer.getName());
            return;
        }

        getTpaSentMap().remove(enteredPlayer.getUniqueId());
        if (getTpaHereSentPlayers().contains(enteredPlayer.getUniqueId())) {
            getTpaHereSentPlayers().remove(enteredPlayer.getUniqueId());
            player.teleport(enteredPlayer.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        } else enteredPlayer.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        getPluginInstance().getManager().updateCooldown(player, "tpa");

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.standalone-teleport"))
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

        getPluginInstance().getManager().sendCustomMessage("player-tpa-accept", player, "{player}:" + enteredPlayer.getName());
        getPluginInstance().getManager().sendCustomMessage("player-tpa-accepted", enteredPlayer, "{player}:" + player.getName());
    }

    private void runTeleportAskAccept(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("self-teleportation-toggled", player);
            return;
        }

        Player foundPlayer = null;
        for (UUID senderId : getTpaSentMap().keySet()) {
            UUID requestedPlayer = getTpaSentMap().get(senderId);
            if (requestedPlayer.toString().equals(player.getUniqueId().toString())) {
                foundPlayer = getPluginInstance().getServer().getPlayer(senderId);
                if (foundPlayer == null || !foundPlayer.isOnline()) {
                    getTpaSentMap().remove(requestedPlayer);
                    getPluginInstance().getManager().sendCustomMessage("player-invalid", player, "{player}:" + (foundPlayer != null ? foundPlayer.getName() : ""));
                    return;
                }

                UUID playerUniqueId = getTpaSentMap().get(foundPlayer.getUniqueId());
                if (playerUniqueId == null) {
                    getPluginInstance().getManager().sendCustomMessage("player-tpa-empty", player, "{player}:" + foundPlayer.getName());
                    return;
                }

                getTpaSentMap().remove(foundPlayer.getUniqueId());
                if (getTpaHereSentPlayers().contains(foundPlayer.getUniqueId())) {
                    getTpaHereSentPlayers().remove(foundPlayer.getUniqueId());
                    player.teleport(foundPlayer.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
                } else foundPlayer.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
                getPluginInstance().getManager().updateCooldown(player, "tpa");
                break;
            }
        }

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.standalone-teleport"))
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

        getPluginInstance().getManager().sendCustomMessage("player-tpa-accept", player, "{player}:" + (foundPlayer != null ? foundPlayer.getName() : ""));

        if (foundPlayer != null)
            getPluginInstance().getManager().sendCustomMessage("player-tpa-accepted", foundPlayer, "{player}:" + player.getName());
    }

    private void runTeleportAskDeny(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("self-teleportation-toggled", player);
            return;
        }

        if (getTpaSentMap().isEmpty() || !getTpaSentMap().containsValue(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("player-tpa-empty", player);
            return;
        }

        Player foundPlayer = null;
        for (UUID senderId : getTpaSentMap().keySet()) {
            UUID requestedPlayer = getTpaSentMap().get(senderId);
            if (requestedPlayer.toString().equals(player.getUniqueId().toString())) {
                foundPlayer = getPluginInstance().getServer().getPlayer(senderId);
                if (foundPlayer == null || !foundPlayer.isOnline()) {
                    getTpaSentMap().remove(requestedPlayer);
                    getPluginInstance().getManager().sendCustomMessage("player-invalid", player, "{player}:" + (foundPlayer != null ? foundPlayer.getName() : ""));
                    return;
                }

                getTpaSentMap().remove(requestedPlayer);
                break;
            }
        }

        getPluginInstance().getManager().sendCustomMessage("player-tpa-deny", player, "{player}:" + (foundPlayer != null ? foundPlayer.getName() : ""));

        if (foundPlayer != null)
            getPluginInstance().getManager().sendCustomMessage("player-tpa-denied", foundPlayer, "{player}:" + player.getName());
    }

    private void runTeleportAskHere(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("self-teleportation-toggled", player);
            return;
        }

        int cooldown = getPluginInstance().getConfig().getInt("teleportation-section.tpa-cooldown");
        long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "tpa", cooldown);
        if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
            getPluginInstance().getManager().sendCustomMessage("tpa-cooldown", player, "{duration}:" + currentCooldown);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage("player-invalid", player, "{player}:" + playerName);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage("teleport-here-self", player);
            return;
        }

        if (getToggledPlayers().contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("player-teleportation-toggled", player, "{player}:" + enteredPlayer.getName());
            return;
        }

        updateTeleportAskRequest(player, enteredPlayer);
        if (!getTpaHereSentPlayers().contains(player.getUniqueId()))
            getTpaHereSentPlayers().add(player.getUniqueId());
        getPluginInstance().getManager().sendCustomMessage("player-tpahere-sent", player, "{player}:" + enteredPlayer.getName());
        getPluginInstance().getManager().sendCustomMessage("player-tpahere-received", enteredPlayer, "{player}:" + player.getName());

        new BukkitRunnable() {
            @Override
            public void run() {
                getTpaSentMap().remove(player.getUniqueId());
            }
        }.runTaskLaterAsynchronously(getPluginInstance(), 20 * getPluginInstance().getConfig().getInt("teleportation-section.teleport-ask-duration"));
    }

    private void runTeleportAsk(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("self-teleportation-toggled", player);
            return;
        }

        int cooldown = getPluginInstance().getConfig().getInt("teleportation-section.tpa-cooldown");
        long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "tpa", cooldown);
        if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
            getPluginInstance().getManager().sendCustomMessage("tpa-cooldown", player, "{duration}:" + currentCooldown);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage("player-invalid", player, "{player}:" + playerName);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage("teleport-self", player);
            return;
        }

        if (getToggledPlayers().contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("player-teleportation-toggled", player, "{player}:" + enteredPlayer.getName());
            return;
        }

        updateTeleportAskRequest(player, enteredPlayer);
        getPluginInstance().getManager().sendCustomMessage("player-tpa-sent", player, "{player}:" + enteredPlayer.getName());
        getPluginInstance().getManager().sendCustomMessage("player-tpa-received", enteredPlayer, "{player}:" + player.getName());

        new BukkitRunnable() {
            @Override
            public void run() {
                getTpaSentMap().remove(player.getUniqueId());
            }
        }.runTaskLaterAsynchronously(getPluginInstance(), 20 * getPluginInstance().getConfig().getInt("teleportation-section.teleport-ask-duration"));
    }

    private void runTeleportPosCommand(CommandSender commandSender, String xEntry, String yEntry, String zEntry) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.admin.tppos")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("self-teleportation-toggled", player);
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(xEntry) || getPluginInstance().getManager().isNotNumeric(yEntry) || getPluginInstance().getManager().isNotNumeric(zEntry)) {
            getPluginInstance().getManager().sendCustomMessage("coordinate-invalid", (Player) commandSender);
            return;
        }

        player.teleport(new Location(player.getWorld(), Double.parseDouble(xEntry), Double.parseDouble(yEntry), Double.parseDouble(zEntry),
                player.getLocation().getYaw(), player.getLocation().getPitch()), PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.standalone-teleport"))
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

        String message = getPluginInstance().getLangConfig().getString("teleported-pos");
        if (message != null && !message.isEmpty())
            commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{world}", player.getWorld().getName())
                    .replace("{x}", xEntry).replace("{y}", yEntry).replace("{z}", zEntry)));
    }

    private void runTeleportPosCommand(CommandSender commandSender, String xEntry, String yEntry, String zEntry, String worldName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.admin.tppos")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("self-teleportation-toggled", player);
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(xEntry) || getPluginInstance().getManager().isNotNumeric(yEntry) || getPluginInstance().getManager().isNotNumeric(zEntry)) {
            getPluginInstance().getManager().sendCustomMessage("coordinate-invalid", (Player) commandSender);
            return;
        }

        World world = getPluginInstance().getServer().getWorld(worldName);
        if (world == null) {
            getPluginInstance().getManager().sendCustomMessage("world-invalid", (Player) commandSender, "{world}:" + worldName);
            return;
        }

        player.teleport(new Location(world, Double.parseDouble(xEntry), Double.parseDouble(yEntry), Double.parseDouble(zEntry),
                player.getLocation().getYaw(), player.getLocation().getPitch()), PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.standalone-teleport"))
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

        String message = getPluginInstance().getLangConfig().getString("teleported-pos");
        if (message != null && !message.isEmpty())
            commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{world}", player.getWorld().getName())
                    .replace("{x}", xEntry).replace("{y}", yEntry).replace("{z}", zEntry)));
    }

    private void runTeleportPosCommand(CommandSender commandSender, String playerName, String xEntry, String yEntry, String zEntry, String worldName) {
        if (!commandSender.hasPermission("hyperdrive.admin.tppos")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("player-invalid", (Player) commandSender, "{player}:" + playerName);
            else {
                String message = getPluginInstance().getLangConfig().getString("player-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{player}", playerName)));
            }
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(xEntry) || getPluginInstance().getManager().isNotNumeric(yEntry) || getPluginInstance().getManager().isNotNumeric(zEntry)) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("coordinate-invalid", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("coordinate-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        World world = getPluginInstance().getServer().getWorld(worldName);
        if (world == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("world-invalid", (Player) commandSender, "{world}:" + worldName, "{player}:" + playerName);
            else {
                String message = getPluginInstance().getLangConfig().getString("world-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{world}", worldName)
                            .replace("{world}", worldName)));
            }
            return;
        }

        enteredPlayer.teleport(new Location(world, Double.parseDouble(xEntry), Double.parseDouble(yEntry), Double.parseDouble(zEntry),
                enteredPlayer.getLocation().getYaw(), enteredPlayer.getLocation().getPitch()), PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.standalone-teleport"))
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
            getPluginInstance().getManager().sendCustomMessage("teleport-pos", (Player) commandSender, "{player}:" + enteredPlayer.getName());
        else {
            String message = getPluginInstance().getLangConfig().getString("teleport-pos");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{player}", enteredPlayer.getName())));
        }
        getPluginInstance().getManager().sendCustomMessage("teleported-pos", enteredPlayer, "{world}" + world.getName(), "{x}" + xEntry, "{y}" + yEntry, "{z}" + zEntry);
    }

    private void runTeleportOverrideHereCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.admin.tpohere")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage("player-invalid", player, "{player}:" + playerName);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage("teleport-self", player);
            return;
        }

        enteredPlayer.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        getPluginInstance().getManager().sendCustomMessage("tp-receiver", player, "{player}:" + enteredPlayer.getName());
    }

    private void runTeleportOverrideCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.admin.tpo")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage("player-invalid", player, "{player}:" + playerName);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage("teleport-self", player);
            return;
        }

        player.teleport(enteredPlayer.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        getPluginInstance().getManager().sendCustomMessage("tp-victim", player, "{player}:" + enteredPlayer.getName());
    }

    private void runTeleportHereCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.admin.tphere")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("self-teleportation-toggled", player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage("player-invalid", player, "{player}:" + playerName);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage("teleport-self", player);
            return;
        }

        if (getToggledPlayers().contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("toggled-tp-player", player, "{player}:" + enteredPlayer.getName());
            return;
        }

        enteredPlayer.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.standalone-teleport"))
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

        getPluginInstance().getManager().sendCustomMessage("tp-receiver", player, "{player}:" + enteredPlayer.getName());
        getPluginInstance().getManager().sendCustomMessage("tp-victim", enteredPlayer, "{player}:" + player.getName());
    }

    private void runTeleportCommand(CommandSender commandSender, String playerName1, String playerName2) {
        if (!commandSender.hasPermission("hyperdrive.admin.tp")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        Player enteredPlayer1 = getPluginInstance().getServer().getPlayer(playerName1), enteredPlayer2 = getPluginInstance().getServer().getPlayer(playerName2);
        if (enteredPlayer1 == null || !enteredPlayer1.isOnline() || enteredPlayer2 == null || !enteredPlayer2.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("players-invalid", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("player-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        if (enteredPlayer1.getUniqueId().toString().equals(enteredPlayer2.getUniqueId().toString())) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("teleport-same", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("teleport-same");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        if (commandSender instanceof Player && ((Player) commandSender).getUniqueId().toString().equals(enteredPlayer1.getUniqueId().toString())
                && ((Player) commandSender).getUniqueId().toString().equals(enteredPlayer2.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage("teleport-self", (Player) commandSender);
            return;
        }

        enteredPlayer1.teleport(enteredPlayer2, PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.standalone-teleport"))
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

        getPluginInstance().getManager().sendCustomMessage("tp-receiver", enteredPlayer2, "{player}:" + enteredPlayer1.getName());
        getPluginInstance().getManager().sendCustomMessage("tp-victim", enteredPlayer1, "{player}:" + enteredPlayer2.getName());
    }

    private void runTeleportCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.admin.tp")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("self-teleportation-toggled", player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage("player-invalid", player, "{player}:" + playerName);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage("teleport-self", player);
            return;
        }

        if (getToggledPlayers().contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("toggled-tp-player", player, "{player}:" + enteredPlayer.getName());
            return;
        }

        player.teleport(enteredPlayer.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);

        String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.standalone-teleport"))
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

        getPluginInstance().getManager().sendCustomMessage("tp-victim", player, "{player}:" + enteredPlayer.getName());
        getPluginInstance().getManager().sendCustomMessage("tp-receiver", enteredPlayer, "{player}:" + player.getName());
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
