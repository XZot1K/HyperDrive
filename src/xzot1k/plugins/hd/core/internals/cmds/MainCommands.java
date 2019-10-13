package xzot1k.plugins.hd.core.internals.cmds;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;
import xzot1k.plugins.hd.api.events.MenuOpenEvent;
import xzot1k.plugins.hd.api.objects.Warp;
import xzot1k.plugins.hd.core.objects.GroupTemp;
import xzot1k.plugins.hd.core.objects.json.JSONExtra;
import xzot1k.plugins.hd.core.objects.json.JSONMessage;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainCommands implements CommandExecutor {
    private HyperDrive pluginInstance;
    private HashMap<Integer, ArrayList<String>> adminHelpPages, helpPages;

    public MainCommands(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);
        setAdminHelpPages(new HashMap<>());
        setHelpPages(new HashMap<>());

        setupAdminPages();
        setupHelpPages();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "hyperdrive":

                switch (args.length) {
                    case 1:
                        switch (args[0].toLowerCase()) {
                            case "reload":
                                runReloadCommand(commandSender);
                                return true;
                            case "info":
                                runInfoCommand(commandSender);
                                return true;
                            default:
                                if (commandSender.hasPermission("hyperdrive.admin.help"))
                                    sendAdminHelpPage(commandSender, 1);
                                else {
                                    if (commandSender instanceof Player)
                                        getPluginInstance().getManager().sendCustomMessage(
                                                getPluginInstance().getConfig().getString("language-section.no-permission"),
                                                (Player) commandSender);
                                    else
                                        commandSender.sendMessage(getPluginInstance().getManager().colorText(
                                                getPluginInstance().getConfig().getString("language-section.no-permission")));
                                }
                                return true;
                        }
                    case 2:
                        if (args[0].equalsIgnoreCase("help")) {
                            if (commandSender.hasPermission("hyperdrive.admin.help"))
                                sendAdminHelpPage(commandSender,
                                        getPluginInstance().getManager().isNumeric(args[1]) ? Integer.parseInt(args[1]) : 1);
                            else {
                                commandSender.sendMessage(getPluginInstance().getManager().colorText(
                                        getPluginInstance().getConfig().getString("language-section.no-permission")));
                            }
                            return true;
                        }

                        if (commandSender.hasPermission("hyperdrive.admin.help"))
                            sendAdminHelpPage(commandSender, 1);
                        else {
                            if (commandSender instanceof Player)
                                getPluginInstance().getManager().sendCustomMessage(
                                        getPluginInstance().getConfig().getString("language-section.no-permission"),
                                        (Player) commandSender);
                            else
                                commandSender.sendMessage(getPluginInstance().getManager().colorText(
                                        getPluginInstance().getConfig().getString("language-section.no-permission")));
                        }

                        return true;
                    case 3:
                        if (args[0].equalsIgnoreCase("updateip")) {
                            runUpdateIP(commandSender, args);
                            return true;
                        }

                        if (commandSender.hasPermission("hyperdrive.admin.help"))
                            sendAdminHelpPage(commandSender, 1);
                        else {
                            if (commandSender instanceof Player)
                                getPluginInstance().getManager().sendCustomMessage(
                                        getPluginInstance().getConfig().getString("language-section.no-permission"),
                                        (Player) commandSender);
                            else
                                commandSender.sendMessage(getPluginInstance().getManager().colorText(
                                        getPluginInstance().getConfig().getString("language-section.no-permission")));
                        }

                        return true;
                    default:
                        if (commandSender.hasPermission("hyperdrive.admin.help"))
                            sendAdminHelpPage(commandSender, 1);
                        else {
                            if (commandSender instanceof Player)
                                getPluginInstance().getManager().sendCustomMessage(
                                        getPluginInstance().getConfig().getString("language-section.no-permission"),
                                        (Player) commandSender);
                            else
                                commandSender.sendMessage(getPluginInstance().getManager().colorText(
                                        getPluginInstance().getConfig().getString("language-section.no-permission")));
                        }

                        return true;
                }

            case "warps":
                switch (args.length) {
                    case 0:
                        openListMenu(commandSender);
                        return true;
                    case 1:
                        switch (args[0].toLowerCase()) {
                            case "help":
                                sendHelpPage(commandSender, 1);
                                return true;
                            case "rtp":
                                beginRandomTeleportCommand(commandSender);
                                return true;
                            case "rtpgroup":
                            case "rtpg":
                                beginGroupRandomTeleportCommand(commandSender);
                                return true;
                            case "list":
                                runWarpListCommand(commandSender);
                                return true;
                            default:
                                beginWarpCommand(commandSender, args[0]);
                                return true;
                        }

                    case 2:

                        switch (args[0].toLowerCase()) {
                            case "accept":
                                beginAcceptCommand(commandSender, args[1]);
                                return true;
                            case "deny":
                                beginDenyCommand(commandSender, args[1]);
                                return true;
                            case "rtp":
                                beginRandomTeleportCommand(commandSender, args[1]);
                                return true;
                            case "help":
                                sendHelpPage(commandSender,
                                        getPluginInstance().getManager().isNumeric(args[1]) ? Integer.parseInt(args[1]) : 1);
                                return true;
                            case "create":
                                runWarpCreationCommand(commandSender, args[1]);
                                return true;
                            case "delete":
                                runWarpDeleteCommand(commandSender, args[1]);
                                return true;
                            case "edit":
                                runWarpEditCommand(commandSender, args[1]);
                                return true;
                            default:
                                if (runWarpAdminCommand(commandSender, args[0], args[1]))
                                    return true;
                                sendHelpPage(commandSender, 1);
                                return true;
                        }

                    case 3:
                        if (args[0].equalsIgnoreCase("rtp")) {
                            beginRandomTeleportCommand(commandSender, args[1], args[2]);
                            return true;
                        }

                        if (commandSender.hasPermission("hyperdrive.admin.help"))
                            sendAdminHelpPage(commandSender, 1);
                        else
                            sendHelpPage(commandSender, 1);
                        return true;
                    default:
                        break;
                }

                if (commandSender.hasPermission("hyperdrive.admin.help"))
                    getPluginInstance().getMainCommands().sendAdminHelpPage(commandSender, 1);
                else getPluginInstance().getMainCommands().sendHelpPage(commandSender, 1);
                return true;
            default:
                break;
        }

        return false;
    }

    private void runUpdateIP(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission("hyperdrive.updateid")) {
            String message = getPluginInstance().getConfig().getString("language-section.no-permission");
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(message, (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        String initialIP = args[1].toLowerCase().replace("current",
                getPluginInstance().getServer().getIp() + ":" + getPluginInstance().getServer().getPort()),
                setIP = args[2].toLowerCase().replace("current",
                        getPluginInstance().getServer().getIp() + ":" + getPluginInstance().getServer().getPort());

        List<Warp> foundWarps = new ArrayList<>(), warps;
        Collection<Warp> warpCollection = getPluginInstance().getManager().getWarpMap().values();
        if (warpCollection instanceof List)
            warps = (List<Warp>) warpCollection;
        else
            warps = new ArrayList<>(warpCollection);

        for (int i = -1; ++i < warps.size(); ) {
            Warp warp = warps.get(i);
            if (warp != null && warp.getServerIPAddress().equalsIgnoreCase(initialIP))
                foundWarps.add(warp);
        }

        if (foundWarps.size() <= 0) {
            String message = getPluginInstance().getConfig().getString("language-section.warp-ip-invalid")
                    .replace("{ip}", initialIP);
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(message, (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        for (int i = -1; ++i < foundWarps.size(); ) {
            Warp warp = warps.get(i);
            if (warp != null)
                warp.setServerIPAddress(setIP);
        }

        String message = getPluginInstance().getConfig().getString("language-section.warp-ip-set")
                .replace("{initial-ip}", initialIP).replace("{set-ip}", setIP)
                .replace("{count}", String.valueOf(foundWarps.size()));
        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage(message, (Player) commandSender);
        else
            commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
    }

    private void beginGroupRandomTeleportCommand(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.use.rtpgroup")) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        List<UUID> onlinePlayers = getPluginInstance().getManager().getPlayerUUIDs();
        onlinePlayers.remove(player.getUniqueId());
        if (onlinePlayers.size() <= 0) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.no-players-found"), player);
            return;
        }

        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.random-teleport-start"), player);
        getPluginInstance().getTeleportationHandler().getDestinationMap().remove(player.getUniqueId());
        getPluginInstance().getTeleportationHandler().updateDestinationWithRandomLocation(player, player.getLocation(), player.getWorld());

        Inventory inventory = getPluginInstance().getManager().buildPlayerSelectionMenu(player);
        MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.PLAYER_SELECTION, inventory, player);
        getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
        if (!menuOpenEvent.isCancelled()) {
            player.openInventory(inventory);
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.player-selection-group"), player);
        }
    }

    private void beginDenyCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.use.deny")) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer enteredPlayer = getPluginInstance().getServer().getOfflinePlayer(playerName);
        if (!enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(
                    Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-invalid"))
                            .replace("{player}", playerName),
                    player);
            return;
        }

        List<UUID> requesterList = getPluginInstance().getTeleportationHandler().getGroupRequests(player);
        if (!requesterList.contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager()
                    .sendCustomMessage(Objects
                            .requireNonNull(getPluginInstance().getConfig().getString("language-section.no-request"))
                            .replace("{player}", Objects.requireNonNull(enteredPlayer.getName())), player);
            return;
        }

        GroupTemp groupTemp = getPluginInstance().getTeleportationHandler().getGroupTemp(enteredPlayer.getUniqueId());
        if (groupTemp == null || !groupTemp.getAcceptedPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager()
                    .sendCustomMessage(Objects
                            .requireNonNull(
                                    getPluginInstance().getConfig().getString("language-section.request-deny-fail"))
                            .replace("{player}", Objects.requireNonNull(enteredPlayer.getName())), player);
            return;
        }

        groupTemp.getAcceptedPlayers().remove(player.getUniqueId());
        getPluginInstance().getManager()
                .sendCustomMessage(Objects
                        .requireNonNull(getPluginInstance().getConfig().getString("language-section.request-denied"))
                        .replace("{player}", Objects.requireNonNull(enteredPlayer.getName())), player);
    }

    private void beginAcceptCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.use.accept")) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer enteredPlayer = getPluginInstance().getServer().getOfflinePlayer(playerName);
        if (!enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage(
                    Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-invalid"))
                            .replace("{player}", playerName),
                    player);
            return;
        }

        List<UUID> requesterList = getPluginInstance().getTeleportationHandler().getGroupRequests(player);
        if (!requesterList.contains(enteredPlayer.getUniqueId())) {
            getPluginInstance().getManager()
                    .sendCustomMessage(Objects
                            .requireNonNull(getPluginInstance().getConfig().getString("language-section.no-request"))
                            .replace("{player}", Objects.requireNonNull(enteredPlayer.getName())), player);
            return;
        }

        GroupTemp groupTemp = getPluginInstance().getTeleportationHandler().getGroupTemp(enteredPlayer.getUniqueId());
        if (groupTemp == null || groupTemp.getAcceptedPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager()
                    .sendCustomMessage(Objects
                            .requireNonNull(
                                    getPluginInstance().getConfig().getString("language-section.request-accept-fail"))
                            .replace("{player}", Objects.requireNonNull(enteredPlayer.getName())), player);
            return;
        }

        groupTemp.getAcceptedPlayers().add(player.getUniqueId());
        getPluginInstance().getManager()
                .sendCustomMessage(Objects
                        .requireNonNull(getPluginInstance().getConfig().getString("language-section.request-accepted"))
                        .replace("{player}", Objects.requireNonNull(enteredPlayer.getName())), player);
    }

    private void runWarpListCommand(CommandSender commandSender) {
        if (!commandSender.hasPermission("hyperdrive.use.list")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(
                        getPluginInstance().getConfig().getString("language-section.no-permission"),
                        (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        }

        if (!(commandSender instanceof Player)) {
            String warpList = new ArrayList<>(getPluginInstance().getManager().getWarpMap().keySet()).toString()
                    .replace("[", "").replace("]", "");
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(Objects
                            .requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-list"))
                            .replace("{list}", warpList).replace("{count}",
                                    String.valueOf(getPluginInstance().getManager().getWarpMap().keySet().size()))));
            return;
        }

        Player player = (Player) commandSender;
        List<String> permittedWarpNames = getPluginInstance().getManager().getPermittedWarps(player);
        String warpList = permittedWarpNames.toString().replace("[", "").replace("]", "");
        getPluginInstance().getManager()
                .sendCustomMessage(
                        Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-list"))
                                .replace("{list}", warpList).replace("{count}",
                                String.valueOf(getPluginInstance().getManager().getWarpMap().keySet().size())),
                        player);
    }

    private void runWarpEditCommand(CommandSender commandSender, String warpName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.use.edit") || !player.hasPermission("hyperdrive.admin.edit")) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        if (!getPluginInstance().getManager().doesWarpExist(warpName)) {
            getPluginInstance().getManager()
                    .sendCustomMessage(Objects
                            .requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-invalid"))
                            .replace("{warp}", warpName), player);
            return;
        }

        Warp warp = getPluginInstance().getManager().getWarp(warpName);
        if (!player.hasPermission("hyperdrive.admin.edit")
                && (warp.getOwner().toString().equals(player.getUniqueId().toString())
                || warp.getAssistants().contains(player.getUniqueId()))) {
            getPluginInstance().getManager().sendCustomMessage(
                    Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-no-access"))
                            .replace("{warp}", warp.getWarpName()),
                    player);
            return;
        }

        Inventory inventory = getPluginInstance().getManager().buildEditMenu(warp);
        MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.EDIT, inventory,
                player);
        getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
        if (!menuOpenEvent.isCancelled()) {
            player.closeInventory();
            player.openInventory(inventory);
        }
    }

    private void runWarpDeleteCommand(CommandSender commandSender, String warpName) {
        if (!commandSender.hasPermission("hyperdrive.use.delete")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(
                        getPluginInstance().getConfig().getString("language-section.no-permission"),
                        (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        }

        if (!getPluginInstance().getManager().doesWarpExist(warpName)) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager()
                        .sendCustomMessage(Objects
                                .requireNonNull(
                                        getPluginInstance().getConfig().getString("language-section.warp-invalid"))
                                .replace("{warp}", warpName), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(Objects
                                .requireNonNull(
                                        getPluginInstance().getConfig().getString("language-section.warp-invalid"))
                                .replace("{warp}", warpName)));
            return;
        }

        Warp warp = getPluginInstance().getManager().getWarp(warpName);
        boolean useMySQL = getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql");
        if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warpName))) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager()
                        .sendCustomMessage(Objects
                                .requireNonNull(
                                        getPluginInstance().getConfig().getString("language-section.warp-invalid"))
                                .replace("{warp}", warpName), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(Objects
                                .requireNonNull(
                                        getPluginInstance().getConfig().getString("language-section.warp-invalid"))
                                .replace("{warp}", warpName)));
            return;
        }

        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            if (!player.hasPermission("hyperdrive.admin.delete")
                    && !warp.getOwner().toString().equals(player.getUniqueId().toString())) {
                getPluginInstance().getManager()
                        .sendCustomMessage(Objects
                                .requireNonNull(
                                        getPluginInstance().getConfig().getString("language-section.delete-not-owner"))
                                .replace("{warp}", warpName), (Player) commandSender);
                return;
            }
        }

        warp.unRegister();
        warp.deleteSaved(true);

        if (commandSender instanceof Player)
            getPluginInstance().getManager()
                    .sendCustomMessage(Objects
                            .requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-deleted"))
                            .replace("{warp}", warpName), (Player) commandSender);
        else
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(Objects
                            .requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-deleted"))
                            .replace("{warp}", warpName)));
    }

    private void runWarpCreationCommand(CommandSender commandSender, String warpName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.use.create")) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        if (!getPluginInstance().getTeleportationHandler().isLocationHookSafe(player, player.getLocation())) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.not-hook-safe"), player);
            return;
        }

        if (getPluginInstance().getManager().hasMetWarpLimit(player)) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.warp-limit-met"), player);
            return;
        }

        List<String> globalFilterStrings = getPluginInstance().getConfig()
                .getStringList("filter-section.global-filter");
        for (int i = -1; ++i < globalFilterStrings.size(); ) {
            String filterString = globalFilterStrings.get(i);
            warpName = warpName.replace(filterString, "");
        }

        warpName = ChatColor.stripColor(getPluginInstance().getManager().colorText(warpName));
        if (getPluginInstance().getManager().doesWarpExist(warpName)) {
            getPluginInstance().getManager()
                    .sendCustomMessage(Objects
                            .requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-exists"))
                            .replace("{warp}", warpName), player);
            return;
        }

        Warp warp = new Warp(warpName, player, player.getLocation());

        boolean useMySQL = getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql");
        if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warpName))) {
            getPluginInstance().getManager()
                    .sendCustomMessage(Objects
                            .requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-exists"))
                            .replace("{warp}", warpName), player);
        } else {
            warp.register();
            warp.save(true, getPluginInstance().getConnection() != null);
            getPluginInstance().getManager()
                    .sendCustomMessage(Objects
                            .requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-created"))
                            .replace("{warp}", warpName), player);
        }
    }

    private boolean runWarpAdminCommand(CommandSender commandSender, String warpName, String playerName) {
        if (!commandSender.hasPermission("hyperdrive.admin.warp")) {
            beginWarpCommand(commandSender, warpName);
            return true;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline())
            return false;

        if (!getPluginInstance().getManager().doesWarpExist(warpName))
            return false;

        Warp warp = getPluginInstance().getManager().getWarp(warpName);

        if ((getPluginInstance().getConnection() != null
                && getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql"))) {
            String warpIP = warp.getServerIPAddress().replace("localhost", "127.0.0.1"),
                    serverIP = (getPluginInstance().getServer().getIp().equalsIgnoreCase("")
                            || getPluginInstance().getServer().getIp().equalsIgnoreCase("0.0.0.0"))
                            ? getPluginInstance().getConfig().getString("mysql-connection.default-ip") + ":"
                            + getPluginInstance().getServer().getPort()
                            : (getPluginInstance().getServer().getIp().replace("localhost", "127.0.0.1") + ":"
                            + getPluginInstance().getServer().getPort());

            if (!warpIP.equalsIgnoreCase(serverIP)) {
                String server = getPluginInstance().getBungeeListener().getServerName(warp.getServerIPAddress());
                if (server == null) {
                    if (commandSender instanceof Player)
                        getPluginInstance().getManager().sendCustomMessage(
                                Objects.requireNonNull(
                                        getPluginInstance().getConfig().getString("language-section.ip-ping-fail"))
                                        .replace("{warp}", warp.getWarpName())
                                        .replace("{ip}", warp.getServerIPAddress()),
                                (Player) commandSender);
                    else
                        commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects
                                .requireNonNull(
                                        getPluginInstance().getConfig().getString("language-section.ip-ping-fail"))
                                .replace("{warp}", warp.getWarpName()).replace("{ip}", warp.getServerIPAddress())));
                    return true;
                }
            }
        }

        getPluginInstance().getTeleportationHandler().updateTeleportTemp(enteredPlayer, "warp", warp.getWarpName(), 0);
        return true;
    }

    private void beginRandomTeleportCommand(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.rtp")) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        if (!player.hasPermission("hyperdrive.rtpbypass")) {
            long cooldownDurationLeft = getPluginInstance().getManager().getCooldownDuration(player, "rtp", getPluginInstance().getConfig().getInt("random-teleport-section.cooldown"));
            if (cooldownDurationLeft > 0) {
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.random-teleport-cooldown"))
                        .replace("{duration}", String.valueOf(cooldownDurationLeft)), player);
                return;
            }
        }

        if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.already-teleporting"), player);
            return;
        }

        for (String worldName : getPluginInstance().getConfig()
                .getStringList("random-teleport-section.forbidden-worlds")) {
            if (worldName.equalsIgnoreCase(player.getWorld().getName())) {
                getPluginInstance().getManager().sendCustomMessage(
                        getPluginInstance().getConfig().getString("language-section.forbidden-world"), player);
                return;
            }
        }

        getPluginInstance().getManager().updateCooldown(player, "rtp");
        int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance().getConfig().getInt("random-teleport-section.delay-duration") : 0;
        getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "rtp", player.getWorld().getName(), duration);

        String title = getPluginInstance().getConfig().getString("random-teleport-section.start-title"), subTitle = getPluginInstance().getConfig().getString("random-teleport-section.start-sub-title");
        if (title != null && subTitle != null)
            getPluginInstance().getManager().sendTitle(player, title.replace("{duration}", String.valueOf(duration)),
                    subTitle.replace("{duration}", String.valueOf(duration)), 0, 5, 0);
        getPluginInstance().getManager().sendActionBar(player, Objects.requireNonNull(getPluginInstance().getConfig().getString("random-teleport-section.start-bar-message"))
                .replace("{duration}", String.valueOf(duration)));
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.random-teleport-begin")).replace("{duration}", String.valueOf(duration)), player);
    }

    private void beginRandomTeleportCommand(CommandSender commandSender, String playerName) {
        if (!commandSender.hasPermission("hyperdrive.admin.rtp") && !commandSender.hasPermission("hyperdrive.rtp")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.no-permission"), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        } else if (!commandSender.hasPermission("hyperdrive.admin.rtp") && commandSender.hasPermission("hyperdrive.rtp") && commandSender instanceof Player) {
            Player player = (Player) commandSender;
            World world = getPluginInstance().getServer().getWorld(playerName);
            if (world == null) {
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig()
                        .getString("language-section.world-invalid")).replace("{world}", playerName)));
                return;
            }

            if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.already-teleporting"), player);
                return;
            }

            for (String worldName : getPluginInstance().getConfig().getStringList("random-teleport-section.forbidden-worlds")) {
                if (worldName.equalsIgnoreCase(player.getWorld().getName())) {
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig()
                            .getString("language-section.random-teleport-admin-forbidden")).replace("{world}", playerName)));
                    return;
                }
            }

            int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance().getConfig().getInt("teleportation-section.warp-delay-duration") : 0;
            getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "rtp", player.getWorld().getName(), duration);

            String title = getPluginInstance().getConfig().getString("random-teleport-section.start-title"), subTitle = getPluginInstance().getConfig().getString("random-teleport-section.start-sub-title");
            if (title != null && subTitle != null)
                getPluginInstance().getManager().sendTitle(player, title.replace("{duration}", String.valueOf(duration)),
                        subTitle.replace("{duration}", String.valueOf(duration)), 0, 5, 0);
            getPluginInstance().getManager().sendActionBar(player, Objects.requireNonNull(getPluginInstance().getConfig().getString("random-teleport-section.start-bar-message"))
                    .replace("{duration}", String.valueOf(duration)));
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.random-teleport-begin")).replace("{duration}", String.valueOf(duration)), player);

            getPluginInstance().getManager().updateCooldown(player, "rtp");
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(
                    Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-invalid"))
                            .replace("{player}", playerName)));
            return;
        }

        if (!enteredPlayer.hasPermission("hyperdrive.rtpbypass")) {
            long cooldownDurationLeft = getPluginInstance().getManager().getCooldownDuration(enteredPlayer, "rtp", getPluginInstance().getConfig().getInt("random-teleport-section.cooldown"));
            if (cooldownDurationLeft > 0) {
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.random-teleport-cooldown"))
                        .replace("{duration}", String.valueOf(cooldownDurationLeft)), enteredPlayer);
                return;
            }
        }

        if (getPluginInstance().getTeleportationHandler().isTeleporting(enteredPlayer)) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-already-teleporting")).replace("{player}", enteredPlayer.getName()), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-already-teleporting")).replace("{player}", enteredPlayer.getName())));
            return;
        }

        for (String worldName : getPluginInstance().getConfig().getStringList("random-teleport-section.forbidden-worlds")) {
            if (worldName.equalsIgnoreCase(enteredPlayer.getWorld().getName())) {
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(Objects
                                .requireNonNull(getPluginInstance().getConfig()
                                        .getString("language-section.random-teleport-admin-forbidden"))
                                .replace("{player}", enteredPlayer.getName()).replace("{world}",
                                        Objects.requireNonNull(enteredPlayer.getLocation().getWorld()).getName())));
                return;
            }
        }

        int duration = !enteredPlayer.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance().getConfig().getInt("random-teleport-section.delay-duration") : 0;
        getPluginInstance().getTeleportationHandler().updateTeleportTemp(enteredPlayer, "rtp", enteredPlayer.getWorld().getName(), duration);

        String title = getPluginInstance().getConfig().getString("random-teleport-section.start-title"), subTitle = getPluginInstance().getConfig().getString("random-teleport-section.start-sub-title");
        if (title != null && subTitle != null)
            getPluginInstance().getManager().sendTitle(enteredPlayer, title.replace("{duration}", String.valueOf(duration)),
                    subTitle.replace("{duration}", String.valueOf(duration)), 0, 5, 0);
        getPluginInstance().getManager().sendActionBar(enteredPlayer, Objects.requireNonNull(getPluginInstance().getConfig().getString("random-teleport-section.start-bar-message"))
                .replace("{duration}", String.valueOf(duration)));
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.random-teleport-begin")).replace("{duration}", String.valueOf(duration)), enteredPlayer);

        if (!commandSender.getName().equalsIgnoreCase(enteredPlayer.getName()))
            commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects
                    .requireNonNull(getPluginInstance().getConfig().getString("language-section.random-teleport-admin"))
                    .replace("{player}", enteredPlayer.getName())
                    .replace("{world}", Objects.requireNonNull(enteredPlayer.getLocation().getWorld()).getName())));
    }

    private void beginRandomTeleportCommand(CommandSender commandSender, String playerName, String worldName) {
        if (!commandSender.hasPermission("hyperdrive.admin.rtp")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(
                        getPluginInstance().getConfig().getString("language-section.no-permission"),
                        (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(playerName);
        if (enteredPlayer == null || !enteredPlayer.isOnline()) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(
                    Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-invalid"))
                            .replace("{player}", playerName)));
            return;
        }

        if (!enteredPlayer.hasPermission("hyperdrive.rtpbypass")) {
            long cooldownDurationLeft = getPluginInstance().getManager().getCooldownDuration(enteredPlayer, "rtp", getPluginInstance().getConfig().getInt("random-teleport-section.cooldown"));
            if (cooldownDurationLeft > 0) {
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.random-teleport-cooldown"))
                        .replace("{duration}", String.valueOf(cooldownDurationLeft)), enteredPlayer);
                return;
            }
        }

        if (getPluginInstance().getTeleportationHandler().isTeleporting(enteredPlayer)) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-already-teleporting")).replace("{player}", enteredPlayer.getName()), (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.player-already-teleporting")).replace("{player}", enteredPlayer.getName())));
            return;
        }

        World world = getPluginInstance().getServer().getWorld(worldName);
        if (world == null) {
            commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.world-invalid"))
                    .replace("{world}", worldName)));
            return;
        }

        for (String wn : getPluginInstance().getConfig().getStringList("random-teleport-section.forbidden-worlds")) {
            if (wn.equalsIgnoreCase(world.getName())) {
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(Objects
                                .requireNonNull(getPluginInstance().getConfig()
                                        .getString("language-section.random-teleport-admin-forbidden"))
                                .replace("{player}", enteredPlayer.getName()).replace("{world}", world.getName())));
                return;
            }
        }

        int duration = !enteredPlayer.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance().getConfig().getInt("random-teleport-section.delay-duration") : 0;
        getPluginInstance().getTeleportationHandler().updateTeleportTemp(enteredPlayer, "rtp", world.getName(), duration);

        String title = getPluginInstance().getConfig().getString("random-teleport-section.start-title"), subTitle = getPluginInstance().getConfig().getString("random-teleport-section.start-sub-title");
        if (title != null && subTitle != null)
            getPluginInstance().getManager().sendTitle(enteredPlayer, title.replace("{duration}", String.valueOf(duration)),
                    subTitle.replace("{duration}", String.valueOf(duration)), 0, 5, 0);
        getPluginInstance().getManager().sendActionBar(enteredPlayer, Objects.requireNonNull(getPluginInstance().getConfig().getString("random-teleport-section.start-bar-message"))
                .replace("{duration}", String.valueOf(duration)));
        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.random-teleport-begin")).replace("{duration}", String.valueOf(duration)), enteredPlayer);

        commandSender.sendMessage(getPluginInstance().getManager().colorText(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.random-teleport-admin"))
                .replace("{player}", enteredPlayer.getName()).replace("{world}", world.getName())));
    }

    private void runInfoCommand(CommandSender commandSender) {
        if (!commandSender.hasPermission("hyperdrive.info")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(
                        getPluginInstance().getConfig().getString("language-section.no-permission"),
                        (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        }

        String[] infoLines = {"&e&m-------------------------", "", "&7Plugin Name: &dHyperDrive",
                "&7Version: &a" + getPluginInstance().getDescription().getVersion(), "&7Author(s): &bXZot1K", "",
                "&7Testing Accommodation(s): &cSikatsu&7, &6JarFiles&7, &dHRZNzero", "",
                "&e&m-------------------------"};
        for (int i = -1; ++i < infoLines.length; ) {
            String infoLine = infoLines[i];
            commandSender.sendMessage(getPluginInstance().getManager().colorText(infoLine));
        }
    }

    private void runReloadCommand(CommandSender commandSender) {
        if (!commandSender.hasPermission("hyperdrive.reload")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage(
                        getPluginInstance().getConfig().getString("language-section.no-permission"),
                        (Player) commandSender);
            else
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(getPluginInstance().getConfig().getString("language-section.no-permission")));
            return;
        }

        getPluginInstance().stopTasks();
        getPluginInstance().reloadConfig();
        getPluginInstance().getManager().setSimpleDateFormat(new SimpleDateFormat(
                Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.date-format"))));
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
            getPluginInstance().saveWarps(getPluginInstance().getConnection() != null);
            getPluginInstance().getManager().getWarpMap().clear();
            getPluginInstance().loadWarps(getPluginInstance().getConnection() != null);
            getPluginInstance().startTasks();
        });

        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getConfig().getString("language-section.reload")));
        } else
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.reload"), (Player) commandSender);
    }

    private void beginWarpCommand(CommandSender commandSender, String warpName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!getPluginInstance().getManager().doesWarpExist(warpName)) {
            getPluginInstance().getManager()
                    .sendCustomMessage(Objects
                            .requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-invalid"))
                            .replace("{warp}", warpName), player);
            return;
        }

        Warp warp = getPluginInstance().getManager().getWarp(warpName);
        if (warp.getStatus() != EnumContainer.Status.PUBLIC
                && !warp.getOwner().toString().equals(player.getUniqueId().toString())
                && !warp.getAssistants().contains(player.getUniqueId())
                && !warp.getWhiteList().contains(player.getUniqueId())
                && !(player.hasPermission("hyperdrive.warps." + warp.getWarpName())
                || player.hasPermission("hyperdrive.warps.*"))) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        if ((getPluginInstance().getConnection() != null
                && getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql"))) {
            String warpIP = warp.getServerIPAddress().replace("localhost", "127.0.0.1"),
                    serverIP = (getPluginInstance().getServer().getIp().equalsIgnoreCase("")
                            || getPluginInstance().getServer().getIp().equalsIgnoreCase("0.0.0.0"))
                            ? getPluginInstance().getConfig().getString("mysql-connection.default-ip") + ":"
                            + getPluginInstance().getServer().getPort()
                            : (getPluginInstance().getServer().getIp().replace("localhost", "127.0.0.1") + ":"
                            + getPluginInstance().getServer().getPort());

            if (!warpIP.equalsIgnoreCase(serverIP)) {
                String server = getPluginInstance().getBungeeListener().getServerName(warp.getServerIPAddress());
                if (server == null) {
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.ip-ping-fail"))
                            .replace("{warp}", warp.getWarpName()).replace("{ip}", warp.getServerIPAddress()), (Player) commandSender);
                    return;
                }
            }
        }

        long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp",
                getPluginInstance().getConfig().getInt("teleportation-section.cooldown-duration"));
        if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-cooldown"))
                    .replace("{duration}", String.valueOf(currentCooldown)), player);
            return;
        }

        if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig().getString("language-section.already-teleporting"), player);
            return;
        }

        if (getPluginInstance().getConfig().getBoolean("general-section.use-vault")
                && !player.hasPermission("hyperdrive.economybypass")
                && !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())
                && !warp.getAssistants().contains(player.getUniqueId())
                && (warp.getWhiteList().contains(player.getUniqueId()))) {
            EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(player,
                    warp.getUsagePrice());
            if (!economyResponse.transactionSuccess()) {
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
                        .replace("{amount}", String.valueOf(warp.getUsagePrice())), player);
                return;
            } else
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.transaction-success"))
                        .replace("{amount}", String.valueOf(warp.getUsagePrice())), player);
        }

        int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance().getConfig().getInt("teleportation-section.warp-delay-duration") : 0;
        if (warp.getAnimationSet().contains(":")) {
            String[] themeArgs = warp.getAnimationSet().split(":");
            String delayTheme = themeArgs[1];
            if (delayTheme.contains("/")) {
                String[] delayThemeArgs = delayTheme.split("/");
                getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
                getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, delayThemeArgs[1],
                        EnumContainer.Animation.valueOf(
                                delayThemeArgs[0].toUpperCase().replace(" ", "_").replace("-", "_")),
                        duration);
            }
        }

        String title = getPluginInstance().getConfig().getString("teleportation-section.start-title"),
                subTitle = getPluginInstance().getConfig().getString("teleportation-section.start-sub-title");
        if (title != null && subTitle != null)
            getPluginInstance().getManager().sendTitle(player,
                    title.replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()),
                    subTitle.replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()), 0,
                    5, 0);

        getPluginInstance().getManager().sendActionBar(player, Objects
                .requireNonNull(getPluginInstance().getConfig().getString("teleportation-section.start-bar-message"))
                .replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()));

        getPluginInstance().getManager().sendCustomMessage(Objects
                .requireNonNull(getPluginInstance().getConfig().getString("language-section.teleportation-start"))
                .replace("{warp}", warp.getWarpName()).replace("{duration}", String.valueOf(duration)), player);

        getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "warp", warp.getWarpName(), duration);
    }

    private void openListMenu(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getConfig().getString("language-section.must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.use")) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.no-permission"), player);
            return;
        }

        Inventory inventory = getPluginInstance().getManager().buildListMenu(player);
        MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.LIST, inventory,
                player);
        getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
        if (!menuOpenEvent.isCancelled())
            player.openInventory(inventory);
    }

    // page methods
    private void setupAdminPages() {
        ArrayList<String> page1 = new ArrayList<>(), page2 = new ArrayList<>(), page3 = new ArrayList<>(),
                page4 = new ArrayList<>(), page5 = new ArrayList<>();
        page1.add("");
        page1.add("&e<&m-----------&r&e( &d&lCommands &e[&dPage &a1&e] &e)&m-----------&r&e>");
        page1.add("");
        page1.add(
                "&7&l*&r &e/hyperdrive help <page> &7- &aopens a help page or the main page, if the page is not defined.");
        page1.add("&7&l*&r &e/hyperdrive reload &7- &are-loads all packets, tasks, warps, and configurations.");
        page1.add("&7&l*&r &e/hyperdrive info &7- &adisplays information about the current build of the plugin.");
        page1.add("");
        getAdminHelpPages().put(1, page1);

        page2.add("");
        page2.add("&e<&m-----------&r&e( &d&lCommands &e[&dPage &a2&e] &e)&m-----------&r&e>");
        page2.add("");
        page2.add("&7&l*&r &e/warps &7- &aopens the warp list menu.");
        page2.add("&7&l*&r &e/warps <name> &7- &aattempts to teleport to the entered warp.");
        page2.add(
                "&7&l*&r &e/warps <name> <player> &7- &aattempts to teleport the entered player to the entered warp.");
        page2.add("&7&l*&r &e/warps rtp &7- &abegins the random teleportation process on the sender.");
        page2.add("&7&l*&r &e/warps rtp <player> &7- &abegins the random teleportation process on the entered player.");
        page2.add(
                "&7&l*&r &e/warps rtp <player> <world> &7- &abegins the random teleportation process on the entered player to the entered world.");
        page2.add("");
        getAdminHelpPages().put(2, page2);

        page3.add("");
        page3.add("&e<&m-----------&r&e( &d&lCommands &e[&dPage &a3&e] &e)&m-----------&r&e>");
        page3.add("");
        page3.add("&7&l*&r &e/warps create <name> &7- &aattempts to create a warp with the entered name.");
        page3.add("&7&l*&r &e/warps delete <name> &7- &aattempts to delete a warp with the entered name.");
        page3.add("&7&l*&r &e/tp <player> &7- &ateleports the sender to the entered player.");
        page3.add("&7&l*&r &e/tp <player1> <player2> &7- &ateleports player 1 to player 2.");
        page3.add("&7&l*&r &e/tpo <player> &7- &ateleports to the player unnoticed and overriding teleport toggle.");
        page3.add("");
        getAdminHelpPages().put(3, page3);

        page4.add("");
        page4.add("&e<&m-----------&r&e( &d&lCommands &e[&dPage &a4&e] &e)&m-----------&r&e>");
        page4.add("");
        page4.add(
                "&7&l*&r &e/tpohere <player> &7- &ateleports the player to the sender's location unnoticed and overriding teleport toggle.");
        page4.add("&7&l*&r &e/tphere <player> &7- &ateleports the player to the sender's location.");
        page4.add(
                "&7&l*&r &e/tppos <x> <y> <z> <world> &7- &ateleports the sender to the defined coordinates in the defined world.");
        page4.add(
                "&7&l*&r &e/tppos <player> <x> <y> <z> <world> &7- &ateleports the entered player to the defined coordinates in the defined world.");
        page4.add(
                "&7&l*&r &e/tppos <x> <y> <z> &7- &athe sender will be teleported to the defined coordinates in the current world.");
        page4.add(
                "&7&l*&r &e/back <player> &7- &aattempts to teleport the entered player to their last teleport location.");
        page4.add("");
        getAdminHelpPages().put(4, page4);

        page5.add("");
        page5.add("&e<&m-----------&r&e( &d&lCommands &e[&dPage &a4&e] &e)&m-----------&r&e>");
        page5.add("");
        page5.add(
                "&7&l*&r &e/crossserver <player> <server> <world> <x> <y> <z> &7- &aattempts to teleport the defined player to the server at the defined coordinates.");
        page5.add(
                "&7&l*&r &e/crossserver <player> <server> <world> <x> <y> <z> <yaw> <pitch> &7- &aattempts to teleport the defined player to the server at the defined coordinates.");
        page5.add(
                "&7&l*&r &e/hyperdrive updateip <initial-ip> <new-ip> &7- &asets all IP Addresses of warps with the initial server ip to the new IP Address, Use 'current' for current server IP.");
        page5.add("");
        getAdminHelpPages().put(5, page5);
    }

    private void setupHelpPages() {
        ArrayList<String> page1 = new ArrayList<>(), page2 = new ArrayList<>(), page3 = new ArrayList<>();
        page1.add("");
        page1.add("&e<&m------------&r&e( &d&lCommands &e[&dPage &a1&e] &e)&m-----------&r&e>");
        page1.add("");
        page1.add("&7&l*&r &e/warps help <page> &7- &aopens a help page or the main page, if the page is not defined.");
        page1.add("&7&l*&r &e/warps &7- &aopens the warp list menu.");
        page1.add("&7&l*&r &e/warps <name> &7- &aattempts to teleport to the entered warp.");
        page1.add("&7&l*&r &e/warps create <name> &7- &aattempts to create a warp with the entered name.");
        page1.add("&7&l*&r &e/warps delete <name> &7- &aattempts to delete a warp with the entered name.");
        page1.add("");
        getHelpPages().put(1, page1);

        page2.add("");
        page2.add("&e<&m------------&r&e( &d&lCommands &e[&dPage &a2&e] &e)&m-----------&r&e>");
        page2.add("");
        page2.add("&7&l*&r &e/tpa <player> &7- &asends a request to teleport to the entered player.");
        page2.add("&7&l*&r &e/tpaccept <player> &7- &aaccepts the found teleport request and teleports the requester to the acceptor.");
        page2.add("&7&l*&r &e/tpdeny <player> &7- &adenies the found teleport request.");
        page2.add("&7&l*&r &e/tpaccept &7- &aaccepts the first found teleport request and teleports the requester to the acceptor.");
        page2.add("&7&l*&r &e/tpdeny &7- &adenies the first found teleport request.");
        page2.add("");
        getHelpPages().put(2, page2);

        page3.add("");
        page3.add("&e<&m------------&r&e( &d&lCommands &e[&dPage &a3&e] &e)&m-----------&r&e>");
        page3.add("");
        page3.add("&7&l*&r &e/tptoggle &7- &atoggles teleportation such as TPA requests and forceful teleportation commands.");
        page3.add("&7&l*&r &e/back &7- &aattempts to teleport the sender to their last teleport location.");
        page3.add("&7&l*&r &e/tpahere <player> &7- &asends a request for the player to teleport to you.");
        page3.add("&7&l*&r &e/warps rtp &7- &abegins the random teleportation process on the sender.");
        page3.add("&7&l*&r &e/warps rtp <world> &7- &abegins the random teleportation process on the sender to the specified world.");
        page3.add("");
        getHelpPages().put(3, page3);
    }

    public void sendAdminHelpPage(CommandSender commandSender, int page) {
        if (!(commandSender instanceof Player)) {
            if (!getAdminHelpPages().containsKey(page)) {
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(getPluginInstance().getConfig().getString("language-section.invalid-help-page")));
                return;
            }

            List<String> lines = getAdminHelpPages().get(page);
            for (int i = -1; ++i < lines.size(); )
                commandSender.sendMessage(getPluginInstance().getManager().colorText(lines.get(i)));
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText("&e<&m-------------------------------------------------------&r&e>"));
            return;
        }

        Player player = (Player) commandSender;
        if (!getAdminHelpPages().containsKey(page)) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.invalid-help-page"), player);
            return;
        }

        List<String> lines = getAdminHelpPages().get(page);
        for (int i = -1; ++i < lines.size(); )
            player.sendMessage(getPluginInstance().getManager().colorText(lines.get(i)));

        if (getAdminHelpPages().containsKey(page + 1) && getAdminHelpPages().containsKey(page - 1)) {
            JSONMessage jsonMessage = new JSONMessage(getPluginInstance(), "&e<&m------&r&e(");
            JSONExtra jsonExtra1 = new JSONExtra(getPluginInstance(), " &d[Previous Page] ");
            jsonExtra1.setClickEvent(EnumContainer.JSONClickAction.RUN_COMMAND, "/hyperdrive help " + (page - 1));
            jsonExtra1.setHoverEvent(EnumContainer.JSONHoverAction.SHOW_TEXT,
                    "&aOpens the previous administrator help page.");
            jsonMessage.addExtra(jsonExtra1);

            JSONExtra jsonExtra2 = new JSONExtra(getPluginInstance(), "&e|");
            jsonMessage.addExtra(jsonExtra2);

            JSONExtra jsonExtra3 = new JSONExtra(getPluginInstance(), " &d[Next Page] ");
            jsonExtra3.setClickEvent(EnumContainer.JSONClickAction.RUN_COMMAND, "/hyperdrive help " + (page + 1));
            jsonExtra3.setHoverEvent(EnumContainer.JSONHoverAction.SHOW_TEXT,
                    "&aOpens the next administrator help page.");
            jsonMessage.addExtra(jsonExtra3);

            JSONExtra jsonExtra4 = new JSONExtra(getPluginInstance(), "&e)&m-------&r&e>");
            jsonMessage.addExtra(jsonExtra4);
            jsonMessage.sendJSONToPlayer(player);
        } else if (getAdminHelpPages().containsKey(page + 1)) {
            JSONMessage jsonMessage = new JSONMessage(getPluginInstance(), "&e<&m--------------&r&e(");
            JSONExtra jsonExtra1 = new JSONExtra(getPluginInstance(), " &d[Next Page] ");
            jsonExtra1.setClickEvent(EnumContainer.JSONClickAction.RUN_COMMAND, "/hyperdrive help " + (page + 1));
            jsonExtra1.setHoverEvent(EnumContainer.JSONHoverAction.SHOW_TEXT,
                    "&aOpens the next administrator help page.");
            jsonMessage.addExtra(jsonExtra1);
            JSONExtra jsonExtra2 = new JSONExtra(getPluginInstance(), "&e)&m---------------&r&e>");
            jsonMessage.addExtra(jsonExtra2);
            jsonMessage.sendJSONToPlayer(player);
        } else if (getAdminHelpPages().containsKey(page - 1)) {
            JSONMessage jsonMessage = new JSONMessage(getPluginInstance(), "&e<&m------------&r&e(");
            JSONExtra jsonExtra1 = new JSONExtra(getPluginInstance(), " &d[Previous Page] ");
            jsonExtra1.setClickEvent(EnumContainer.JSONClickAction.RUN_COMMAND, "/hyperdrive help " + (page - 1));
            jsonExtra1.setHoverEvent(EnumContainer.JSONHoverAction.SHOW_TEXT,
                    "&aOpens the previous administrator help page.");
            jsonMessage.addExtra(jsonExtra1);
            JSONExtra jsonExtra2 = new JSONExtra(getPluginInstance(), "&e)&m-------------&r&e>");
            jsonMessage.addExtra(jsonExtra2);
            jsonMessage.sendJSONToPlayer(player);
        } else
            player.sendMessage(getPluginInstance().getManager()
                    .colorText("&e<&m-------------------------------------------------------&r&e>"));
    }

    public void sendHelpPage(CommandSender commandSender, int page) {
        if (!(commandSender instanceof Player)) {
            if (!getHelpPages().containsKey(page)) {
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(getPluginInstance().getConfig().getString("language-section.invalid-help-page")));
                return;
            }

            List<String> lines = getHelpPages().get(page);
            for (int i = -1; ++i < lines.size(); )
                commandSender.sendMessage(getPluginInstance().getManager().colorText(lines.get(i)));
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText("&e<&m-------------------------------------------------------&r&e>"));
            return;
        }

        Player player = (Player) commandSender;
        if (!getHelpPages().containsKey(page)) {
            getPluginInstance().getManager().sendCustomMessage(
                    getPluginInstance().getConfig().getString("language-section.invalid-help-page"), player);
            return;
        }

        // getPluginInstance().getManager().clearChat(player);
        List<String> lines = getHelpPages().get(page);
        for (int i = -1; ++i < lines.size(); )
            player.sendMessage(getPluginInstance().getManager().colorText(lines.get(i)));

        if (getHelpPages().containsKey(page + 1) && getHelpPages().containsKey(page - 1)) {
            JSONMessage jsonMessage = new JSONMessage(getPluginInstance(), "&e<&m------&r&e(");
            JSONExtra jsonExtra1 = new JSONExtra(getPluginInstance(), " &d[Previous Page] ");
            jsonExtra1.setClickEvent(EnumContainer.JSONClickAction.RUN_COMMAND, "/hyperdrive help " + (page - 1));
            jsonExtra1.setHoverEvent(EnumContainer.JSONHoverAction.SHOW_TEXT,
                    "&aOpens the previous administrator help page.");
            jsonMessage.addExtra(jsonExtra1);

            JSONExtra jsonExtra2 = new JSONExtra(getPluginInstance(), "&e|");
            jsonMessage.addExtra(jsonExtra2);

            JSONExtra jsonExtra3 = new JSONExtra(getPluginInstance(), " &d[Next Page] ");
            jsonExtra3.setClickEvent(EnumContainer.JSONClickAction.RUN_COMMAND, "/hyperdrive help " + (page + 1));
            jsonExtra3.setHoverEvent(EnumContainer.JSONHoverAction.SHOW_TEXT,
                    "&aOpens the next administrator help page.");
            jsonMessage.addExtra(jsonExtra3);

            JSONExtra jsonExtra4 = new JSONExtra(getPluginInstance(), "&e)&m-------&r&e>");
            jsonMessage.addExtra(jsonExtra4);
            jsonMessage.sendJSONToPlayer(player);
        } else if (getHelpPages().containsKey(page + 1)) {
            JSONMessage jsonMessage = new JSONMessage(getPluginInstance(), "&e<&m--------------&r&e(");
            JSONExtra jsonExtra1 = new JSONExtra(getPluginInstance(), " &d[Next Page] ");
            jsonExtra1.setClickEvent(EnumContainer.JSONClickAction.RUN_COMMAND, "/hyperdrive help " + (page + 1));
            jsonExtra1.setHoverEvent(EnumContainer.JSONHoverAction.SHOW_TEXT, "&aOpens the next help page.");
            jsonMessage.addExtra(jsonExtra1);
            JSONExtra jsonExtra2 = new JSONExtra(getPluginInstance(), "&e)&m---------------&r&e>");
            jsonMessage.addExtra(jsonExtra2);
            jsonMessage.sendJSONToPlayer(player);
        } else if (getHelpPages().containsKey(page - 1)) {
            JSONMessage jsonMessage = new JSONMessage(getPluginInstance(), "&e<&m------------&r&e(");
            JSONExtra jsonExtra1 = new JSONExtra(getPluginInstance(), " &d[Previous Page] ");
            jsonExtra1.setClickEvent(EnumContainer.JSONClickAction.RUN_COMMAND, "/hyperdrive help " + (page - 1));
            jsonExtra1.setHoverEvent(EnumContainer.JSONHoverAction.SHOW_TEXT, "&aOpens the previous help page.");
            jsonMessage.addExtra(jsonExtra1);
            JSONExtra jsonExtra2 = new JSONExtra(getPluginInstance(), "&e)&m-------------&r&e>");
            jsonMessage.addExtra(jsonExtra2);
            jsonMessage.sendJSONToPlayer(player);
        } else
            player.sendMessage(getPluginInstance().getManager()
                    .colorText("&e<&m-------------------------------------------------------&r&e>"));
    }

    // getters & setters
    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    private HashMap<Integer, ArrayList<String>> getHelpPages() {
        return helpPages;
    }

    private void setHelpPages(HashMap<Integer, ArrayList<String>> helpPages) {
        this.helpPages = helpPages;
    }

    private HashMap<Integer, ArrayList<String>> getAdminHelpPages() {
        return adminHelpPages;
    }

    private void setAdminHelpPages(HashMap<Integer, ArrayList<String>> adminHelpPages) {
        this.adminHelpPages = adminHelpPages;
    }
}
