package xzot1k.plugins.hd.core.internals.cmds;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;
import xzot1k.plugins.hd.api.objects.SerializableLocation;

import java.util.*;

public class TeleportationCommands implements CommandExecutor {
    private HyperDrive pluginInstance;
    private List<UUID> toggledPlayers;
    private HashMap<UUID, UUID> tpaSentMap;
    private HashMap<UUID, SerializableLocation> lastLocationMap;

    public TeleportationCommands(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);
        setToggledPlayers(new ArrayList<>());
        setLastLocationMap(new HashMap<>());
        setTpaSentMap(new HashMap<>());
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {

        switch (command.getName().toLowerCase()) {

            case "crossserver":

                switch (args.length) {
                    case 8:
                        runCrossServer(commandSender, args);
                        return true;

                    case 6:
                        runCrossServerShortened(commandSender, args);
                        return true;

                    default:
                        if (commandSender.hasPermission("hyperdrive.admin.help"))
                            getPluginInstance().getMainCommands().sendAdminHelpPage(commandSender, 1);
                        else getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);

                        return true;
                }

            case "teleport":

                switch (args.length) {
                    case 1:
                        runTeleportCommand(commandSender, args[0]);
                        return true;

                    case 2:
                        runTeleportCommand(commandSender, args[0], args[1]);
                        return true;

                    default:

                        if (commandSender.hasPermission("hyperdrive.admin.help"))
                            getPluginInstance().getMainCommands().sendAdminHelpPage(commandSender, 1);
                        else getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);

                        return true;
                }

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
                }

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
                    return true;
                }

                getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;
            case "teleportdeny":

                if (args.length >= 1) {
                    runTeleportAskDeny(commandSender, args[0]);
                    return true;
                }

                getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
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

    private void runCrossServerShortened(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission("hyperdrive.admin.crossserver")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(args[0]);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-invalid"))
                        .replace("{player}", args[0]), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-invalid"))
                        .replace("{player}", args[0])));
            return;
        }

        if (getPluginInstance().getConnection() == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance()
                        .getConfig().getString("language-section.mysql-disabled")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance()
                        .getConfig().getString("language-section.mysql-disabled"))));
            return;
        }

        String serverName = args[1];
        World world = getPluginInstance().getServer().getWorld(args[2]);
        if (world == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.world-invalid"))
                        .replace("{world}", args[1]), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.world-invalid"))
                        .replace("{world}", args[1])));
            return;
        }

        // X coordinate
        if (!getPluginInstance().getManager().isNumeric(args[3])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid"))));
            return;
        }

        // Y coordinate
        if (!getPluginInstance().getManager().isNumeric(args[4])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid"))));
            return;
        }

        // Z coordinate
        if (!getPluginInstance().getManager().isNumeric(args[5])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid"))));
            return;
        }

        Location location = new Location(world, Double.parseDouble(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), enteredPlayer.getLocation().getYaw(),
                enteredPlayer.getLocation().getPitch());

        getPluginInstance().getManager().teleportCrossServer(enteredPlayer, getPluginInstance().getBungeeListener().getIPFromMap(serverName), serverName, location);
        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.cross-server"))
                    .replace("{player}", enteredPlayer.getName()).replace("{server}", serverName)), (Player) commandSender);
        else
            commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.cross-server"))
                    .replace("{player}", enteredPlayer.getName()).replace("{server}", serverName))));
    }

    private void runCrossServer(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission("hyperdrive.admin.crossserver")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(args[0]);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-invalid"))
                        .replace("{player}", args[0]), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-invalid"))
                        .replace("{player}", args[0])));
            return;
        }

        if (getPluginInstance().getConnection() == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance()
                        .getConfig().getString("language-section.mysql-disabled")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance()
                        .getConfig().getString("language-section.mysql-disabled"))));
            return;
        }

        String serverName = args[1];
        World world = getPluginInstance().getServer().getWorld(args[2]);
        if (world == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.world-invalid"))
                        .replace("{world}", args[1]), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.world-invalid"))
                        .replace("{world}", args[1])));
            return;
        }

        // X coordinate
        if (!getPluginInstance().getManager().isNumeric(args[3])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid"))));
            return;
        }

        // Y coordinate
        if (!getPluginInstance().getManager().isNumeric(args[4])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid"))));
            return;
        }

        // Z coordinate
        if (!getPluginInstance().getManager().isNumeric(args[5])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid"))));
            return;
        }

        // Yaw coordinate
        if (!getPluginInstance().getManager().isNumeric(args[6])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid"))));
            return;
        }

        // Pitch coordinate
        if (!getPluginInstance().getManager().isNumeric(args[7])) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.coordinate-invalid"))));
            return;
        }

        Location location = new Location(world, Double.parseDouble(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Float.parseFloat(args[6]), Float.parseFloat(args[7]));
        getPluginInstance().getManager().teleportCrossServer(enteredPlayer, getPluginInstance().getBungeeListener().getIPFromMap(serverName), serverName, location);
        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.cross-server"))
                    .replace("{player}", enteredPlayer.getName()).replace("{server}", serverName)), (Player) commandSender);
        else
            commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.cross-server"))
                    .replace("{player}", enteredPlayer.getName()).replace("{server}", serverName))));
    }

    private void runBack(CommandSender commandSender, String playerName) {
        if (!commandSender.hasPermission("hyperdrive.admin.back")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-invalid"))
                        .replace("{player}", playerName), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-invalid"))
                        .replace("{player}", playerName)));
            return;
        }

        Location lastLocation = getLastLocation(enteredPlayer);
        if (lastLocation == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-last-location"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.no-last-location")));
            return;
        }

        enteredPlayer.teleport(lastLocation);
        if (commandSender instanceof Player && !((Player) commandSender).getUniqueId().toString().equals(enteredPlayer.getUniqueId().toString()))
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.teleported-last-location"))
                    .replace("{player}", enteredPlayer.getName()), (Player) commandSender);
        else
            commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.teleported-last-location"))
                    .replace("{player}", enteredPlayer.getName())));
        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-last-location"), enteredPlayer);
    }

    private void runBack(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.back")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.self-teleportation-toggled"), player);
            return;
        }

        Location lastLocation = getLastLocation(player);
        if (lastLocation == null) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-last-location"), player);
            return;
        }

        player.teleport(lastLocation);

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

        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-last-location"), player);
    }

    private void runTeleportToggle(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpt")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getToggledPlayers().remove(player.getUniqueId());
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-toggled-on"), player);
        } else {
            getToggledPlayers().add(player.getUniqueId());
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-toggled-off"), player);
        }
    }

    private void runTeleportAskDeny(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.self-teleportation-toggled"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-invalid")
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-self"), player);
            return;
        }

        if (getTpaSentMap().containsKey(enteredPlayer.getUniqueId())) {
            UUID playerUniqueId = getTpaSentMap().get(enteredPlayer.getUniqueId());
            if (playerUniqueId == null || player.getUniqueId().toString().equals(playerUniqueId.toString())) {
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-tpa-invalid")
                        .replace("{player}", enteredPlayer.getName()), player);
                return;
            }
        } else if (getTpaSentMap().isEmpty() || !getTpaSentMap().containsKey(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-tpa-invalid")
                    .replace("{player}", enteredPlayer.getName()), player);
            return;
        }

        getTpaSentMap().remove(enteredPlayer.getUniqueId());
        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-tpa-deny")
                .replace("{player}", enteredPlayer.getName()), player);
        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-tpa-denied")
                .replace("{player}", player.getName()), enteredPlayer);
    }

    private void runTeleportAskAccept(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.self-teleportation-toggled"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-invalid")
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-self"), player);
            return;
        }

        if (!getTpaSentMap().isEmpty() && getTpaSentMap().containsKey(enteredPlayer.getUniqueId())) {
            UUID playerUniqueId = getTpaSentMap().get(enteredPlayer.getUniqueId());
            if (playerUniqueId == null) {
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-tpa-invalid")
                        .replace("{player}", enteredPlayer.getName()), player);
                return;
            }
        } else if (getTpaSentMap().isEmpty() || !getTpaSentMap().containsKey(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-tpa-invalid")
                    .replace("{player}", enteredPlayer.getName()), player);
            return;
        }

        getTpaSentMap().remove(enteredPlayer.getUniqueId());
        enteredPlayer.teleport(player.getLocation());

        String teleportSound = getPluginInstance().getConfig().getString("general-section.global-sounds.teleport")
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

        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-tpa-accept")
                .replace("{player}", enteredPlayer.getName()), player);
        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-tpa-accepted")
                .replace("{player}", player.getName()), enteredPlayer);
    }

    private void runTeleportAsk(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.tpa")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.self-teleportation-toggled"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-invalid")
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-self"), player);
            return;
        }

        if (getToggledPlayers().contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-teleportation-toggled")
                    .replace("{player}", enteredPlayer.getName()), player);
            return;
        }

        updateTeleportAskRequest(player, enteredPlayer);
        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-tpa-sent")
                .replace("{player}", enteredPlayer.getName()), player);
        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-tpa-received")
                .replace("{player}", player.getName()), enteredPlayer);

        new BukkitRunnable() {
            @Override
            public void run() {
                getTpaSentMap().remove(player.getUniqueId());
            }
        }.runTaskLaterAsynchronously(getPluginInstance(), 20 * getPluginInstance().getConfig().getInt("teleportation-section.teleport-ask-duration"));
    }

    private void runTeleportPosCommand(CommandSender commandSender, String xEntry, String yEntry, String zEntry, String worldName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.admin.tppos")) {
            player.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.self-teleportation-toggled"), player);
            return;
        }

        if (!getPluginInstance().getManager().isNumeric(xEntry) || !getPluginInstance().getManager().isNumeric(yEntry) || !getPluginInstance().getManager().isNumeric(zEntry)) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.coordinate-invalid"), (Player) commandSender);
            return;
        }

        World world = getPluginInstance().getServer().getWorld(worldName);
        if (world == null) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.world-invalid"), (Player) commandSender);
            return;
        }

        player.teleport(new Location(world, Double.parseDouble(xEntry), Double.parseDouble(yEntry), Double.parseDouble(zEntry),
                player.getLocation().getYaw(), player.getLocation().getPitch()));

        String teleportSound = getPluginInstance().getConfig().getString("general-section.global-sounds.teleport")
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

        commandSender.sendMessage(getPluginInstance().getManager().colorText("language-section.teleported-pos")
                .replace("{world}", world.getName()).replace("{x}", xEntry).replace("{y}", yEntry).replace("{z}", zEntry));
    }

    private void runTeleportPosCommand(CommandSender commandSender, String playerName, String xEntry, String yEntry, String zEntry, String worldName) {
        if (!commandSender.hasPermission("hyperdrive.admin.tppos")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.no-permission")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-invalid")
                        .replace("{player}", playerName), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText("language-section.player-invalid").replace("{player}", playerName));
            return;
        }

        if (!getPluginInstance().getManager().isNumeric(xEntry) || !getPluginInstance().getManager().isNumeric(yEntry) || !getPluginInstance().getManager().isNumeric(zEntry)) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.coordinate-invalid")
                        .replace("{player}", playerName), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText("language-section.coordinate-invalid"));
            return;
        }

        World world = getPluginInstance().getServer().getWorld(worldName);
        if (world == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.world-invalid")
                        .replace("{player}", playerName), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText("language-section.world-invalid").replace("{world}", worldName));
            return;
        }

        enteredPlayer.teleport(new Location(world, Double.parseDouble(xEntry), Double.parseDouble(yEntry), Double.parseDouble(zEntry),
                enteredPlayer.getLocation().getYaw(), enteredPlayer.getLocation().getPitch()));

        String teleportSound = getPluginInstance().getConfig().getString("general-section.global-sounds.teleport")
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
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-pos")
                    .replace("{player}", enteredPlayer.getName()), (Player) commandSender);
        else
            commandSender.sendMessage(getPluginInstance().getManager().colorText("language-section.teleport-pos").replace("{player}", enteredPlayer.getName()));
        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleported-pos")
                .replace("{world}", world.getName()).replace("{x}", xEntry).replace("{y}", yEntry).replace("{z}", zEntry), enteredPlayer);
    }

    private void runTeleportOverrideHereCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.admin.tpohere")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-invalid")
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-self"), player);
            return;
        }

        enteredPlayer.teleport(player.getLocation());
        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.tp-receiver")
                .replace("{player}", enteredPlayer.getName()), player);
    }

    private void runTeleportOverrideCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.admin.tpo")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-invalid")
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-self"), player);
            return;
        }

        player.teleport(enteredPlayer.getLocation());
        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.tp-victim")
                .replace("{player}", enteredPlayer.getName()), player);
    }

    private void runTeleportHereCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.admin.tphere")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.self-teleportation-toggled"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-invalid")
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-self"), player);
            return;
        }

        if (getToggledPlayers().contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.toggled-tp-player")
                    .replace("{player}", enteredPlayer.getName()), player);
            return;
        }

        enteredPlayer.teleport(player.getLocation());

        String teleportSound = getPluginInstance().getConfig().getString("general-section.global-sounds.teleport")
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

        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.tp-receiver")
                .replace("{player}", enteredPlayer.getName()), player);
        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.tp-victim")
                .replace("{player}", player.getName()), enteredPlayer);
    }

    private void runTeleportCommand(CommandSender commandSender, String playerName1, String playerName2) {
        if (!commandSender.hasPermission("hyperdrive.admin.tp")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.no-permission")), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        }

        Player enteredPlayer1 = getPluginInstance().getServer().getPlayer(playerName1), enteredPlayer2 = getPluginInstance().getServer().getPlayer(playerName2);
        if (enteredPlayer1 == null || !enteredPlayer1.isOnline() || enteredPlayer2 == null || !enteredPlayer2.isOnline()) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.players-invalid"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.players-invalid")));
            return;
        }

        if (enteredPlayer1.getUniqueId().toString().equals(enteredPlayer2.getUniqueId().toString())) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-same"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.teleport-same")));
            return;
        }

        if (commandSender instanceof Player && ((Player) commandSender).getUniqueId().toString().equals(enteredPlayer1.getUniqueId().toString())
                && ((Player) commandSender).getUniqueId().toString().equals(enteredPlayer2.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-self"), (Player) commandSender);
            return;
        }

        enteredPlayer1.teleport(enteredPlayer2);

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

        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.tp-receiver"))
                .replace("{player}", enteredPlayer1.getName()), enteredPlayer2);
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.tp-victim"))
                .replace("{player}", enteredPlayer2.getName()), enteredPlayer1);
    }

    private void runTeleportCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.admin.tp")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        if (getToggledPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.self-teleportation-toggled"), player);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.player-invalid")
                    .replace("{player}", playerName), player);
            return;
        }

        if (enteredPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.teleport-self"), player);
            return;
        }

        if (getToggledPlayers().contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.toggled-tp-player")
                    .replace("{player}", enteredPlayer.getName()), player);
            return;
        }

        player.teleport(enteredPlayer.getLocation());

        String teleportSound = getPluginInstance().getConfig().getString("general-section.global-sounds.teleport")
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

        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.tp-victim"))
                .replace("{player}", enteredPlayer.getName()), player);
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.tp-receiver"))
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

    private HashMap<UUID, UUID> getTpaSentMap() {
        return tpaSentMap;
    }

    private void setTpaSentMap(HashMap<UUID, UUID> tpaSentMap) {
        this.tpaSentMap = tpaSentMap;
    }

    private HashMap<UUID, SerializableLocation> getLastLocationMap() {
        return lastLocationMap;
    }

    private void setLastLocationMap(HashMap<UUID, SerializableLocation> lastLocationMap) {
        this.lastLocationMap = lastLocationMap;
    }
}
