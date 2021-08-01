/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.cmds;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.WordUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;
import xzot1k.plugins.hd.api.events.EconomyChargeEvent;
import xzot1k.plugins.hd.api.events.MenuOpenEvent;
import xzot1k.plugins.hd.api.objects.Warp;
import xzot1k.plugins.hd.core.internals.hooks.HookChecker;
import xzot1k.plugins.hd.core.objects.GroupTemp;

import java.util.*;

public class MainCommands implements CommandExecutor {
    private HyperDrive pluginInstance;
    private HashMap<Integer, ArrayList<String>> adminHelpPages, helpPages;
    private HashMap<UUID, String> warpConfirmationMap;

    public MainCommands(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);
        setAdminHelpPages(new HashMap<>());
        setHelpPages(new HashMap<>());
        setWarpConfirmationMap(new HashMap<>());

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
                                        getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
                                    else {
                                        String message = getPluginInstance().getLangConfig().getString("no-permission");
                                        if (message != null && !message.isEmpty())
                                            commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
                                    }
                                }
                                return true;
                        }
                    case 2:
                        if (args[0].equalsIgnoreCase("help")) {
                            if (commandSender.hasPermission("hyperdrive.admin.help"))
                                sendAdminHelpPage(commandSender, !getPluginInstance().getManager().isNotNumeric(args[1]) ? Integer.parseInt(args[1]) : 1);
                            else
                                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-permission")));
                            return true;
                        } else if (args[0].equalsIgnoreCase("clear")) {
                            if (commandSender.hasPermission("hyperdrive.clear"))
                                sendClear(commandSender, args[1]);
                            else
                                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("no-permission")));
                            return true;
                        }

                        if (commandSender.hasPermission("hyperdrive.admin.help"))
                            sendAdminHelpPage(commandSender, 1);
                        else {
                            if (commandSender instanceof Player)
                                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
                            else {
                                String message = getPluginInstance().getLangConfig().getString("no-permission");
                                if (message != null && !message.isEmpty())
                                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
                            }
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
                                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
                            else {
                                String message = getPluginInstance().getLangConfig().getString("no-permission");
                                if (message != null && !message.isEmpty())
                                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
                            }
                        }

                        return true;
                    default:
                        if (commandSender.hasPermission("hyperdrive.admin.help"))
                            sendAdminHelpPage(commandSender, 1);
                        else {
                            if (commandSender instanceof Player)
                                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
                            else {
                                String message = getPluginInstance().getLangConfig().getString("no-permission");
                                if (message != null && !message.isEmpty())
                                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
                            }
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
                            case "list":
                                runWarpListCommand(commandSender);
                                return true;
                            default:
                                break;
                        }

                    case 2:

                        switch (args[0].toLowerCase()) {
                            case "accept":
                                beginAcceptCommand(commandSender, args[1]);
                                return true;
                            case "deny":
                                beginDenyCommand(commandSender, args[1]);
                                return true;
                            case "help":
                                sendHelpPage(commandSender, !getPluginInstance().getManager().isNotNumeric(args[1]) ? Integer.parseInt(args[1]) : 1);
                                return true;
                            default:
                                break;
                        }

                    default:
                        break;
                }

                if (args.length >= 4 && args[0].equalsIgnoreCase("visits")) {
                    StringBuilder warpNameBuilder = new StringBuilder();
                    for (int i = -1; ++i < (args.length - 2); ) {
                        warpNameBuilder.append(args[i]);
                        if (i < (args.length - 1)) warpNameBuilder.append(" ");
                    }

                    initiateVisitsModify(commandSender, warpNameBuilder.toString().trim(), args[args.length - 2], args[args.length - 1]);
                    return true;
                }

                if (args.length >= 3 && args[0].equalsIgnoreCase("setstatus")) {
                    StringBuilder warpNameBuilder = new StringBuilder();
                    for (int i = -1; ++i < (args.length - 1); ) {
                        warpNameBuilder.append(args[i]);
                        if (i < (args.length - 1)) warpNameBuilder.append(" ");
                    }

                    beginStatusSetCommand(commandSender, warpNameBuilder.toString().trim(), args[args.length - 1]);
                    return true;
                }

                if (args.length >= 2) {

                    if (args[0].equalsIgnoreCase("create")) {
                        StringBuilder warpNameBuilder = new StringBuilder();
                        for (int i = 0; ++i < args.length; ) {
                            warpNameBuilder.append(args[i]);
                            if (i < (args.length - 1)) warpNameBuilder.append(" ");
                        }

                        runWarpCreationCommand(commandSender, warpNameBuilder.toString().trim());
                        return true;
                    } else if (args[0].equalsIgnoreCase("delete")) {
                        StringBuilder warpNameBuilder = new StringBuilder();
                        for (int i = 0; ++i < args.length; ) {
                            warpNameBuilder.append(args[i]);
                            if (i < (args.length - 1)) warpNameBuilder.append(" ");
                        }

                        runWarpDeleteCommand(commandSender, warpNameBuilder.toString().trim());
                        return true;
                    } else if (args[0].equalsIgnoreCase("edit")) {
                        StringBuilder warpNameBuilder = new StringBuilder();
                        for (int i = 0; ++i < args.length; ) {
                            warpNameBuilder.append(args[i]);
                            if (i < (args.length - 1)) warpNameBuilder.append(" ");
                        }

                        runWarpEditCommand(commandSender, warpNameBuilder.toString());
                        return true;
                    } else if (args[0].equalsIgnoreCase("list")) {
                        StringBuilder warpNameBuilder = new StringBuilder();
                        for (int i = 0; ++i < args.length; ) {
                            warpNameBuilder.append(args[i]);
                            if (i < (args.length - 1)) warpNameBuilder.append(" ");
                        }

                        runListCommand(commandSender, warpNameBuilder.toString().trim());
                        return true;
                    } else if (args[0].equalsIgnoreCase("assistants")) {
                        StringBuilder warpNameBuilder = new StringBuilder();
                        for (int i = 0; ++i < args.length; ) {
                            warpNameBuilder.append(args[i]);
                            if (i < (args.length - 1)) warpNameBuilder.append(" ");
                        }

                        runAssistantsCommand(commandSender, warpNameBuilder.toString().trim());
                        return true;
                    }

                    Player player = getPluginInstance().getServer().getPlayer(args[args.length - 1]);
                    if (player != null) {
                        StringBuilder warpNameBuilder = new StringBuilder();
                        for (int i = -1; ++i < (args.length - 1); ) {
                            warpNameBuilder.append(args[i]);
                            if (warpNameBuilder.length() > 0) warpNameBuilder.append(" ");
                        }

                        runWarpAdminCommand(commandSender, warpNameBuilder.toString().trim(), player.getName());
                        return true;
                    }
                }

                StringBuilder warpNameBuilder = new StringBuilder();
                for (int i = -1; ++i < args.length; ) {
                    warpNameBuilder.append(args[i]);
                    if (warpNameBuilder.length() > 0) warpNameBuilder.append(" ");
                }

                beginWarpCommand(commandSender, warpNameBuilder.toString().trim());
                return true;
            default:
                break;
        }

        return false;
    }

    private void sendClear(CommandSender commandSender, String worldName) {
        if (!commandSender.hasPermission("hyperdrive.clear")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        World world = getPluginInstance().getServer().getWorld(worldName);
        if (world != null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("world-invalid", (Player) commandSender, "{world}:" + worldName);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{world}", worldName)));
            }
            return;
        }

        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
            Set<Map.Entry<String, Warp>> warpEntries = getPluginInstance().getManager().getWarpMap().entrySet();
            for (Map.Entry<String, Warp> entry : warpEntries) {
                if (entry.getValue() == null || entry.getValue().getWarpLocation() == null || !entry.getValue().getWarpLocation().getWorldName().equalsIgnoreCase(worldName))
                    continue;
                entry.getValue().unRegister();
                entry.getValue().deleteSaved(false);
            }
        });

        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage("warps-cleared", (Player) commandSender, "{world}:" + worldName);
        else {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{world}", worldName)));
        }
    }

    private void initiateVisitsModify(CommandSender commandSender, String commandType, String warpName, String value) {
        if (!commandSender.hasPermission("hyperdrive.admin.visits")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        Warp warp = getPluginInstance().getManager().getWarp(warpName);
        boolean useMySQL = (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")
                && getPluginInstance().getConfig().getBoolean("mysql-connection.cross-server"));
        if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warpName))) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("warp-invalid", (Player) commandSender, "{warp}:" + warpName);
            else {
                String message = getPluginInstance().getLangConfig().getString("warp-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{warp}", warpName)));
            }
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(value)) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("invalid-visit-amount", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("invalid-visit-amount");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        final int foundValue = (int) Double.parseDouble(value);
        switch (commandType.toLowerCase()) {
            case "add":

                warp.setTraffic(warp.getTraffic() + foundValue);
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendCustomMessage("visits-modified", (Player) commandSender,
                            "{amount}:" + foundValue, "{warp}:" + warp.getWarpName(), "{total}:" + warp.getTraffic());
                else {
                    String message = getPluginInstance().getLangConfig().getString("visits-modified");
                    if (message != null && !message.isEmpty())
                        commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{amount}", String.valueOf(foundValue))
                                .replace("{warp}", warp.getWarpName()).replace("{total}", String.valueOf(warp.getTraffic()))));
                }

                return;

            case "remove":

                warp.setTraffic(Math.max(0, (warp.getTraffic() - foundValue)));
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendCustomMessage("visits-modified", (Player) commandSender,
                            "{warp}:" + warp.getWarpName(), "{total}:" + warp.getTraffic(), "{amount}:" + foundValue);
                else {
                    String message = getPluginInstance().getLangConfig().getString("visits-modified");
                    if (message != null && !message.isEmpty())
                        commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{amount}", String.valueOf(foundValue))
                                .replace("{warp}", warp.getWarpName()).replace("{total}", String.valueOf(warp.getTraffic()))));
                }

                return;

            case "set":

                warp.setTraffic(foundValue);
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendCustomMessage("visits-modified", (Player) commandSender,
                            "{warp}:" + warp.getWarpName(), "{total}:" + warp.getTraffic(), "{amount}:" + foundValue);
                else {
                    String message = getPluginInstance().getLangConfig().getString("visits-modified");
                    if (message != null && !message.isEmpty())
                        commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{amount}", String.valueOf(foundValue))
                                .replace("{warp}", warp.getWarpName()).replace("{total}", String.valueOf(warp.getTraffic()))));
                }

                return;

            default:
                break;
        }

        if (commandSender.hasPermission("hyperdrive.admin.help"))
            sendAdminHelpPage(commandSender, 1);
        else
            sendHelpPage(commandSender, 1);
    }

    private void runAssistantsCommand(CommandSender commandSender, String warpName) {
        if (!commandSender.hasPermission("hyperdrive.use.list") && !commandSender.hasPermission("hyperdrive.admin.list")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        Warp warp = getPluginInstance().getManager().getWarp(warpName);
        boolean useMySQL = (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")
                && getPluginInstance().getConfig().getBoolean("mysql-connection.cross-server"));
        if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warpName))) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("warp-invalid", (Player) commandSender, "{warp}:" + warp.getWarpName());
            else {
                String message = getPluginInstance().getLangConfig().getString("warp-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{warp}", warpName)));
            }
            return;
        }

        if (commandSender instanceof Player && !commandSender.hasPermission("hyperdrive.admin.list")) {
            Player player = (Player) commandSender;
            if ((warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString())) || warp.getAssistants().contains(player.getUniqueId())) {
                String message = getPluginInstance().getLangConfig().getString("warp-no-access");
                if (message != null && !message.isEmpty())
                    getPluginInstance().getManager().sendCustomMessage("warp-no-access", player, "{warp}:" + warp.getWarpName());
            }
        }

        List<String> playerNames = new ArrayList<>();
        for (UUID playerUniqueId : warp.getAssistants()) {
            OfflinePlayer offlinePlayer = getPluginInstance().getServer().getPlayer(playerUniqueId);
            if (offlinePlayer == null) continue;
            playerNames.add(offlinePlayer.getName());
        }

        Collections.sort(playerNames);
        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage("assistants-list", (Player) commandSender, "{warp}:" + warp.getWarpName(), "{list}:" + playerNames);
        else {
            String message = getPluginInstance().getLangConfig().getString("assistants-list");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{warp}", warp.getWarpName()).replace("{list}", playerNames.toString())));
        }
    }

    private void runListCommand(CommandSender commandSender, String warpName) {
        if (!commandSender.hasPermission("hyperdrive.use.list") && !commandSender.hasPermission("hyperdrive.admin.list")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        Warp warp = getPluginInstance().getManager().getWarp(warpName);
        boolean useMySQL = (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")
                && getPluginInstance().getConfig().getBoolean("mysql-connection.cross-server"));
        if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warpName))) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("warp-invalid", (Player) commandSender, "{warp}:" + warpName);
            else {
                String message = getPluginInstance().getLangConfig().getString("warp-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{warp}", warpName)));
            }
            return;
        }

        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            if (!commandSender.hasPermission("hyperdrive.admin.list") && (warp.getOwner() != null && !warp.getOwner().toString().equals(player.getUniqueId().toString()))
                    && !warp.getAssistants().contains(player.getUniqueId())) {
                getPluginInstance().getManager().sendCustomMessage("warp-no-access", player, "{warp}:" + warp.getWarpName());
                return;
            }
        }

        List<String> playerNames = new ArrayList<String>() {{
            for (UUID playerUniqueId : warp.getPlayerList())
                add(getPluginInstance().getServer().getOfflinePlayer(playerUniqueId).getName());
        }};

        Collections.sort(playerNames);
        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage("listed-list", (Player) commandSender,
                    "{warp}:" + warp.getWarpName(), "{list}:" + playerNames);
        else {
            String message = getPluginInstance().getLangConfig().getString("listed-list");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{warp}", warp.getWarpName()).replace("{list}", playerNames.toString())));
        }
    }

    private void beginStatusSetCommand(CommandSender commandSender, String warpName, String status) {
        if (!commandSender.hasPermission("hyperdrive.admin.status")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        Warp warp = getPluginInstance().getManager().getWarp(warpName);
        boolean useMySQL = (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")
                && getPluginInstance().getConfig().getBoolean("mysql-connection.cross-server"));
        if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warpName))) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("warp-invalid", (Player) commandSender, "{warp}:" + warpName);
            else {
                String message = getPluginInstance().getLangConfig().getString("warp-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{warp}", warpName)));
            }
            return;
        }

        EnumContainer.Status enteredStatus = null;
        for (EnumContainer.Status stat : EnumContainer.Status.values())
            if (stat.name().equalsIgnoreCase(status)) {
                enteredStatus = stat;
                break;
            }

        if (enteredStatus == null) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("invalid-status", (Player) commandSender,
                        "{statues}:" + Arrays.toString(EnumContainer.Status.values()));
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{statuses}", Arrays.toString(EnumContainer.Status.values()))));
            }
            return;
        }

        warp.setStatus(enteredStatus);
        warp.save(true);
        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage("status-set", (Player) commandSender,
                    "{status}:" + WordUtils.capitalize(enteredStatus.name().toLowerCase()), "{warp}:" + warp.getWarpName());
        else {
            String message = getPluginInstance().getLangConfig().getString("status-set");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message
                        .replace("{warp}", warp.getWarpName())
                        .replace("{status}", WordUtils.capitalize(enteredStatus.name().toLowerCase()))));
        }
    }

    private void runUpdateIP(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission("hyperdrive.updateid")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
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
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("warp-ip-invalid", (Player) commandSender, "{ip}:" + initialIP);
            else {
                String message = getPluginInstance().getLangConfig().getString("warp-ip-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{ip}", initialIP)));
            }
            return;
        }

        for (int i = -1; ++i < foundWarps.size(); ) {
            Warp warp = warps.get(i);
            if (warp != null)
                warp.setServerIPAddress(setIP);
        }

        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage("warp-ip-set", (Player) commandSender,
                    "{initial-ip}:" + initialIP, "{set-ip}:" + setIP, "{count}:" + foundWarps.size());
        else {
            String message = getPluginInstance().getLangConfig().getString("warp-ip-set");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{initial-ip}", initialIP).replace("{set-ip}", setIP)
                        .replace("{count}", String.valueOf(foundWarps.size()))));
        }
    }

    private void beginDenyCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.use.deny")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer enteredPlayer = getPluginInstance().getServer().getOfflinePlayer(playerName);
        if (!enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage("player-invalid", player, "{player}:" + playerName);
            return;
        }

        GroupTemp groupTemp = getPluginInstance().getTeleportationHandler().getGroupTemp(enteredPlayer.getUniqueId());
        if (groupTemp == null || !groupTemp.getAcceptedPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("request-deny-fail", player, "{player}:" + enteredPlayer.getName());
            return;
        }

        groupTemp.getAcceptedPlayers().remove(player.getUniqueId());
        getPluginInstance().getManager().sendCustomMessage("request-denied", player, "{player}:" + enteredPlayer.getName());
    }

    private void beginAcceptCommand(CommandSender commandSender, String playerName) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!commandSender.hasPermission("hyperdrive.use.accept")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer enteredPlayer = getPluginInstance().getServer().getOfflinePlayer(playerName);
        if (!enteredPlayer.isOnline()) {
            getPluginInstance().getManager().sendCustomMessage("player-invalid", player, "{player}:" + playerName);
            return;
        }

        GroupTemp groupTemp = getPluginInstance().getTeleportationHandler().getGroupTemp(enteredPlayer.getUniqueId());
        if (groupTemp == null || groupTemp.getAcceptedPlayers().contains(player.getUniqueId())) {
            getPluginInstance().getManager().sendCustomMessage("request-accept-fail", player, "{player}:" + enteredPlayer.getName());
            return;
        }

        groupTemp.getAcceptedPlayers().add(player.getUniqueId());
        getPluginInstance().getManager().sendCustomMessage("request-accepted", player, "{player}:" + enteredPlayer.getName());
    }

    private void runWarpListCommand(CommandSender commandSender) {
        if (!commandSender.hasPermission("hyperdrive.use.list")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        if (!(commandSender instanceof Player)) {
            String warpList = getPluginInstance().getManager().getWarpMap().keySet().isEmpty() ? "[]" : new ArrayList<>(getPluginInstance().getManager().getWarpMap().keySet()).toString()
                    .replace("[", "").replace("]", "");
            String message = getPluginInstance().getLangConfig().getString("warp-list");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{list}", warpList).replace("{count}",
                        String.valueOf(getPluginInstance().getManager().getWarpMap().keySet().size()))));
            return;
        }

        Player player = (Player) commandSender;
        List<String> permittedWarpNames = getPluginInstance().getManager().getPermittedWarps(player);
        String warpList = permittedWarpNames.isEmpty() ? "[]" : permittedWarpNames.toString().replace("[", "").replace("]", "");
        getPluginInstance().getManager().sendCustomMessage("warp-list", player, "{list}:" + warpList,
                "{count}:" + permittedWarpNames.size());
    }

    private void runWarpEditCommand(CommandSender commandSender, String warpName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.use.edit") || !player.hasPermission("hyperdrive.admin.edit")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (!getPluginInstance().getManager().doesWarpExist(warpName)) {
            getPluginInstance().getManager().sendCustomMessage("warp-invalid", player, "{warp}:" + warpName);
            return;
        }

        Warp warp = getPluginInstance().getManager().getWarp(warpName);
        if (!getPluginInstance().getManager().canEditWarp(player, warp)) {
            getPluginInstance().getManager().sendCustomMessage("warp-no-access", player, "{warp}:" + warp.getWarpName());
            return;
        }

        Inventory inventory = getPluginInstance().getManager().buildEditMenu(player, warp);
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
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        if (!getPluginInstance().getManager().doesWarpExist(warpName)) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("warp-invalid", (Player) commandSender, "{warp}:" + warpName);
            else {
                String message = getPluginInstance().getLangConfig().getString("warp-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{warp}", warpName)));
            }
            return;
        }

        Warp warp = getPluginInstance().getManager().getWarp(warpName);
        boolean useMySQL = (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")
                && getPluginInstance().getConfig().getBoolean("mysql-connection.cross-server"));
        if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warpName))) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("warp-invalid", (Player) commandSender, "{warp}:" + warp.getWarpName());
            else {
                String message = getPluginInstance().getLangConfig().getString("warp-invalid");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{warp}", warp.getWarpName())));
            }
            return;
        }

        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            if (!player.hasPermission("hyperdrive.admin.delete") && !warp.getOwner().toString().equals(player.getUniqueId().toString())) {
                getPluginInstance().getManager().sendCustomMessage("delete-not-owner", (Player) commandSender, "{warp}:" + warp.getWarpName());
                return;
            }
        }

        warp.unRegister();
        warp.deleteSaved(true);

        if (commandSender instanceof Player)
            getPluginInstance().getManager().sendCustomMessage("warp-deleted", (Player) commandSender, "{warp}:" + warp.getWarpName());
        else {
            String message = getPluginInstance().getLangConfig().getString("warp-deleted");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message.replace("{warp}", warp.getWarpName())));
        }
    }

    private void runWarpCreationCommand(CommandSender commandSender, String warpName) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.use.create")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        if (getPluginInstance().getManager().isBlockedWorld(player.getWorld())) {
            getPluginInstance().getManager().sendCustomMessage("blocked-world", player);
            return;
        }

        if (!getPluginInstance().getHookChecker().isLocationHookSafe(player, player.getLocation(), HookChecker.CheckType.CREATION)) {
            getPluginInstance().getManager().sendCustomMessage("not-hook-safe", player);
            return;
        }

        if (getPluginInstance().getManager().hasMetWarpLimit(player)) {
            getPluginInstance().getManager().sendCustomMessage("warp-limit-met", player);
            return;
        }

        List<String> globalFilterStrings = getPluginInstance().getConfig()
                .getStringList("filter-section.global-filter");
        for (int i = -1; ++i < globalFilterStrings.size(); ) {
            String filterString = globalFilterStrings.get(i);
            warpName = warpName.replace(filterString, "");
        }

        warpName = getPluginInstance().getManager().colorText(ChatColor.WHITE + warpName);
        final String strippedName = ChatColor.stripColor(warpName);
        if (strippedName.equalsIgnoreCase("") || strippedName.isEmpty()) {
            getPluginInstance().getManager().sendCustomMessage("invalid-warp-name", player);
            return;
        }

        if (getPluginInstance().getManager().doesWarpExist(warpName)) {
            getPluginInstance().getManager().sendCustomMessage("warp-exists", player, "{warp}:" + warpName);
            return;
        }

        warpName = warpName.replace("'", "").replace("\"", "");
        Warp warp = new Warp(warpName, player, player.getLocation());
        boolean useMySQL = (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")
                && getPluginInstance().getConfig().getBoolean("mysql-connection.cross-server"));
        if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warpName))) {
            getPluginInstance().getManager().sendCustomMessage("warp-exists", player, "{warp}:" + warp.getWarpName());
        } else {
            warp.register();
            warp.save(true);
            getPluginInstance().getManager().sendCustomMessage("warp-created", player, "{warp}:" + warp.getWarpName());
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
        getPluginInstance().getTeleportationHandler().updateTeleportTemp(enteredPlayer, "warp", warp.getWarpName(), 0);
        return true;
    }

    private void runInfoCommand(CommandSender commandSender) {
        if (!commandSender.hasPermission("hyperdrive.info")) {
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
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
                getPluginInstance().getManager().sendCustomMessage("no-permission", (Player) commandSender);
            else {
                String message = getPluginInstance().getLangConfig().getString("no-permission");
                if (message != null && !message.isEmpty())
                    commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            }
            return;
        }

        getPluginInstance().stopTasks(getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql"));
        getPluginInstance().reloadConfigs();
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
            getPluginInstance().saveWarps(false);
            getPluginInstance().getManager().getWarpMap().clear();
            getPluginInstance().loadWarps();
            getPluginInstance().startTasks();
        });

        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("reload");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
        } else
            getPluginInstance().getManager().sendCustomMessage("reload", (Player) commandSender);
    }

    private void beginWarpCommand(CommandSender commandSender, String warpName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getLangConfig().getString("must-be-player")));
            return;
        }

        Player player = (Player) commandSender;
        Warp warp = getPluginInstance().getManager().getWarp(warpName);
        if (warp == null) {
            getPluginInstance().getManager().sendCustomMessage("warp-invalid", player, "{warp}:" + warpName);
            return;
        }

        if (!getPluginInstance().getManager().canUseWarp(player, warp)) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp", getPluginInstance().getConfig().getInt("teleportation-section.cooldown-duration"));
        if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
            getPluginInstance().getManager().sendCustomMessage("warp-cooldown", player, "{duration}:" + currentCooldown);
            return;
        }

        if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
            getPluginInstance().getManager().sendCustomMessage("already-teleporting", player);
            return;
        }

        if (!getPluginInstance().getHookChecker().isLocationHookSafe(player, warp.getWarpLocation().asBukkitLocation(), HookChecker.CheckType.WARP)) {
            getPluginInstance().getManager().sendCustomMessage("not-hook-safe", player, ("{warp}:" + warp.getWarpName()));
            return;
        }

        if (getPluginInstance().getConfig().getBoolean("general-section.use-vault") && !player.hasPermission("hyperdrive.economybypass")
                && (warp.getOwner() != null && !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())) && !warp.getAssistants().contains(player.getUniqueId())
                && (!warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode())) {

            if (warp.getUsagePrice() > 0 && !isConfirming(player, warp)) {
                updateConfirmation(player, warp);
                getPluginInstance().getManager().sendCustomMessage("warp-confirm", player, "{warp}:" + warp.getWarpName(),
                        "{price}:" + warp.getUsagePrice());
                return;
            }

            clearConfirmation(player);
            EconomyChargeEvent economyChargeEvent = new EconomyChargeEvent(player, warp.getUsagePrice());
            getPluginInstance().getServer().getPluginManager().callEvent(economyChargeEvent);
            if (!economyChargeEvent.isCancelled()) {
                if (!getPluginInstance().getVaultHandler().getEconomy().has(player, economyChargeEvent.getAmount())) {
                    getPluginInstance().getManager().sendCustomMessage("insufficient-funds", player, "{amount}:" + economyChargeEvent.getAmount());
                    return;
                }

                getPluginInstance().getVaultHandler().getEconomy().withdrawPlayer(player, economyChargeEvent.getAmount());

                if (warp.getOwner() != null) {
                    OfflinePlayer owner = getPluginInstance().getServer().getOfflinePlayer(warp.getOwner());
                    getPluginInstance().getVaultHandler().getEconomy().depositPlayer(owner, economyChargeEvent.getAmount());
                }

                getPluginInstance().getManager().sendCustomMessage("transaction-success", player, "{amount}:" + economyChargeEvent.getAmount());
            }
        }

        int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance().getConfig().getInt("teleportation-section.warp-delay-duration") : 0;
        boolean isVanished = getPluginInstance().getManager().isVanished(player);
        if (!isVanished && warp.getAnimationSet().contains(":")) {
            String[] themeArgs = warp.getAnimationSet().split(":");
            String delayTheme = themeArgs[1];
            if (delayTheme.contains("/")) {
                String[] delayThemeArgs = delayTheme.split("/");
                getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
                getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, delayThemeArgs[1], EnumContainer.Animation.valueOf(delayThemeArgs[0].toUpperCase().replace(" ", "_")
                        .replace("-", "_")), duration);
            }
        }

        String title = getPluginInstance().getConfig().getString("teleportation-section.start-title"),
                subTitle = getPluginInstance().getConfig().getString("teleportation-section.start-sub-title");
        if (title != null && subTitle != null)
            getPluginInstance().getManager().sendTitle(player, title.replace("{duration}", String.valueOf(duration))
                    .replace("{warp}", warp.getWarpName()), subTitle.replace("{duration}", String.valueOf(duration))
                    .replace("{warp}", warp.getWarpName()), 0, 5, 0);

        String actionMessage = getPluginInstance().getConfig().getString("teleportation-section.start-bar-message");
        if (actionMessage != null && !actionMessage.isEmpty())
            getPluginInstance().getManager().sendActionBar(player, actionMessage.replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()));

        getPluginInstance().getManager().sendCustomMessage("teleportation-start", player, "{warp}:" + warp.getWarpName(), "{duration}:" + duration);
        getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "warp", warp.getWarpName(), duration);
    }

    private void openListMenu(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.isEmpty())
                commandSender.sendMessage(getPluginInstance().getManager().colorText(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("hyperdrive.use")) {
            getPluginInstance().getManager().sendCustomMessage("no-permission", player);
            return;
        }

        Inventory inventory = getPluginInstance().getManager().buildListMenu(player);
        MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.LIST, inventory, player);
        getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
        if (!menuOpenEvent.isCancelled())
            player.openInventory(inventory);
    }

    // page methods
    private void setupAdminPages() {
        final String pageHeader = getPluginInstance().getLangConfig().getString("help-header"),
                cmdPrefix = getPluginInstance().getLangConfig().getString("command-prefix"),
                descriptionPrefix = getPluginInstance().getLangConfig().getString("command-description-prefix");
        ArrayList<String> page1 = new ArrayList<>(), page2 = new ArrayList<>(), page3 = new ArrayList<>(),
                page4 = new ArrayList<>(), page5 = new ArrayList<>(), page6 = new ArrayList<>();
        page1.add("");
        page1.add(Objects.requireNonNull(pageHeader).replace("{page}", String.valueOf(1)));
        page1.add("");
        page1.add(cmdPrefix + "/hyperdrive help <page> " + descriptionPrefix + "Opens a help page or the main page, if the page is not defined.");
        page1.add(cmdPrefix + "/hyperdrive reload " + descriptionPrefix + "Re-loads all packets, tasks, warps, and configurations.");
        page1.add(cmdPrefix + "/hyperdrive info " + descriptionPrefix + "Displays information about the current build of the plugin.");
        page1.add(cmdPrefix + "/hyperdrive updateip <initial-ip> <new-ip> " + descriptionPrefix + "Sets all IP Addresses of warps with the initial server " +
                "ip to the new IP Address, Use 'current' for current server IP.");
        page1.add("");
        getAdminHelpPages().put(1, page1);

        page2.add("");
        page2.add(Objects.requireNonNull(pageHeader).replace("{page}", String.valueOf(2)));
        page2.add("");
        page2.add(cmdPrefix + "/warps " + descriptionPrefix + "Opens the warp list menu.");
        page2.add(cmdPrefix + "/warps <name> " + descriptionPrefix + "Attempts to teleport to the entered warp.");
        page2.add(cmdPrefix + "/warps <name> <player> " + descriptionPrefix + "Attempts to teleport the entered player to the entered warp.");
        page2.add(cmdPrefix + "/warps assistants <warp> " + descriptionPrefix + "Displays a list of all current assistants attached to the defined warp.");
        page2.add(cmdPrefix + "/warps list <warp> " + descriptionPrefix + "Displays a list of all current whitelisted/blacklisted players attached to the defined warp.");
        page2.add("");
        getAdminHelpPages().put(2, page2);

        page3.add("");
        page3.add(Objects.requireNonNull(pageHeader).replace("{page}", String.valueOf(3)));
        page3.add("");
        page3.add(cmdPrefix + "/warps create <name> " + descriptionPrefix + "Attempts to create a warp with the entered name.");
        page3.add(cmdPrefix + "/warps delete <name> " + descriptionPrefix + "Attempts to delete a warp with the entered name.");
        page3.add(cmdPrefix + "/tp <player> " + descriptionPrefix + "Teleports the sender to the entered player.");
        page3.add(cmdPrefix + "/tp <player1> <player2> " + descriptionPrefix + "Teleports player 1 to player 2.");
        page3.add(cmdPrefix + "/tpo <player> " + descriptionPrefix + "Teleports to the player unnoticed and overriding teleport toggle.");
        page3.add("");
        getAdminHelpPages().put(3, page3);

        page4.add("");
        page4.add(Objects.requireNonNull(pageHeader).replace("{page}", String.valueOf(4)));
        page4.add("");
        page4.add(cmdPrefix + "/tpohere <player> " + descriptionPrefix + "Teleports the player to the sender's location unnoticed and overriding teleport toggle.");
        page4.add(cmdPrefix + "/tphere <player> " + descriptionPrefix + "Teleports the player to the sender's location.");
        page4.add(cmdPrefix + "/tppos <x> <y> <z> <world> " + descriptionPrefix + "Teleports the sender to the defined coordinates in the defined world.");
        page4.add(cmdPrefix + "/tppos <player> <x> <y> <z> <world> " + descriptionPrefix + "Teleports the entered player to the defined coordinates in the defined world.");
        page4.add(cmdPrefix + "/tppos <x> <y> <z> " + descriptionPrefix + "The sender will be teleported to the defined coordinates in the current world.");
        page4.add(cmdPrefix + "/back <player> " + descriptionPrefix + "Attempts to teleport the entered player to their last teleport location.");
        page4.add("");
        getAdminHelpPages().put(4, page4);

        page5.add("");
        page5.add(Objects.requireNonNull(pageHeader).replace("{page}", String.valueOf(5)));
        page5.add("");
        page5.add(cmdPrefix + "/rtp " + descriptionPrefix + "begins the random teleportation process on the sender.");
        page5.add(cmdPrefix + "/rtp <player> <world> " + descriptionPrefix + "begins the random teleportation process on the entered player to the entered world.");
        page5.add(cmdPrefix + "/spawn " + descriptionPrefix + "teleports the sender to the normal spawn, if it exists.");
        page5.add(cmdPrefix + "/spawn <set/setfirstjoin/clear> " + descriptionPrefix + "sets the first join spawn, normal spawn, or clears both spawns based on the entered argument.");
        page5.add(cmdPrefix + "/crossserver <player> <server> <world> <x> <y> <z> " + descriptionPrefix + "attempts to teleport the defined player " +
                "to the server at the defined coordinates.");
        page5.add(cmdPrefix + "/crossserver <player> <server> <world> <x> <y> <z> <yaw> <pitch> " + descriptionPrefix + "attempts to teleport the " +
                "defined player to the server at the defined coordinates.");
        page5.add("");
        getAdminHelpPages().put(5, page5);

        page6.add("");
        page6.add(Objects.requireNonNull(pageHeader).replace("{page}", String.valueOf(6)));
        page6.add("");
        page6.add(cmdPrefix + "/warps visits add <warp> <amount> " + descriptionPrefix + "Adds the defined amount to the warp's total visit count.");
        page6.add(cmdPrefix + "/warps visits remove <warp> <amount> " + descriptionPrefix + "Removes the defined amount from the warp's total visit count.");
        page6.add(cmdPrefix + "/warps visits set <warp> <amount> " + descriptionPrefix + "Sets the defined amount as the warp's total visit count.");
        page6.add(cmdPrefix + "/warps clear <world> " + descriptionPrefix + "Deletes all warps from the defined world.");
        page6.add("");
        getAdminHelpPages().put(6, page6);
    }

    private void setupHelpPages() {
        final String pageHeader = getPluginInstance().getLangConfig().getString("help-header"),
                cmdPrefix = getPluginInstance().getLangConfig().getString("command-prefix"),
                descriptionPrefix = getPluginInstance().getLangConfig().getString("command-description-prefix");
        ArrayList<String> page1 = new ArrayList<>(), page2 = new ArrayList<>(), page3 = new ArrayList<>(), page4 = new ArrayList<>();
        page1.add("");
        page1.add(Objects.requireNonNull(pageHeader).replace("{page}", String.valueOf(1)));
        page1.add("");
        page1.add(cmdPrefix + "/warps help <page> " + descriptionPrefix + "opens a help page or the main page, if the page is not defined.");
        page1.add(cmdPrefix + "/warps " + descriptionPrefix + "opens the warp list menu.");
        page1.add(cmdPrefix + "/warps <name> " + descriptionPrefix + "attempts to teleport to the entered warp.");
        page1.add(cmdPrefix + "/warps create <name> " + descriptionPrefix + "attempts to create a warp with the entered name.");
        page1.add(cmdPrefix + "/warps delete <name> " + descriptionPrefix + "attempts to delete a warp with the entered name.");
        page1.add("");
        getHelpPages().put(1, page1);

        page2.add("");
        page2.add(Objects.requireNonNull(pageHeader).replace("{page}", String.valueOf(2)));
        page2.add("");
        page2.add(cmdPrefix + "/warps assistants <warp> " + descriptionPrefix + "displays a list of all current assistants attached to the defined warp.");
        page2.add(cmdPrefix + "/warps list <warp> " + descriptionPrefix + "displays a list of all current whitelisted/blacklisted players attached to the defined warp.");
        page2.add(cmdPrefix + "/tpa <player> " + descriptionPrefix + "sends a request to teleport to the entered player.");
        page2.add(cmdPrefix + "/tpaccept <player> " + descriptionPrefix + "accepts the found teleport request and teleports the requester to the acceptor.");
        page2.add(cmdPrefix + "/tpdeny <player> " + descriptionPrefix + "denies the found teleport request.");
        page2.add("");
        getHelpPages().put(2, page2);

        page3.add("");
        page3.add(Objects.requireNonNull(pageHeader).replace("{page}", String.valueOf(3)));
        page3.add("");
        page2.add(cmdPrefix + "/tpaccept " + descriptionPrefix + "accepts the first found teleport request and teleports the requester to the acceptor.");
        page2.add(cmdPrefix + "/tpdeny " + descriptionPrefix + "denies the first found teleport request.");
        page3.add(cmdPrefix + "/tptoggle " + descriptionPrefix + "toggles teleportation such as TPA requests and forceful teleportation commands.");
        page3.add(cmdPrefix + "/back " + descriptionPrefix + "attempts to teleport to the last teleport location.");
        page3.add(cmdPrefix + "/tpahere <player> " + descriptionPrefix + "sends a request for the player to teleport to you.");
        page3.add("");
        getHelpPages().put(3, page3);

        page4.add("");
        page4.add(Objects.requireNonNull(pageHeader).replace("{page}", String.valueOf(4)));
        page4.add("");
        page3.add(cmdPrefix + "/rtp " + descriptionPrefix + "begins the random teleportation process.");
        page3.add(cmdPrefix + "/spawn " + descriptionPrefix + "teleports to the normal spawn, if it exists.");
        page4.add("");
        getHelpPages().put(4, page4);
    }

    public void sendJSONLine(Player player, int page) {
        final String previousCommand = ("/hyperdrive help " + (page - 1)), nextCommand = ("/hyperdrive help " + (page + 1));

        TextComponent footerLeft = new TextComponent(TextComponent.fromLegacyText(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("help-footer-left")))),
                footerRight = new TextComponent(TextComponent.fromLegacyText(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("help-footer-right")))),
                separator = new TextComponent(TextComponent.fromLegacyText(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("help-footer-separator")))),
                previousMessage = new TextComponent(TextComponent.fromLegacyText(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("help-footer-previous")))),
                nextMessage = new TextComponent(TextComponent.fromLegacyText((getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("help-footer-next")))));
        previousMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, previousCommand));
        nextMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, nextCommand));

        if (getAdminHelpPages().containsKey(page + 1) && getAdminHelpPages().containsKey(page - 1)) {
            footerLeft.addExtra(previousMessage);
            footerLeft.addExtra(separator);
            footerLeft.addExtra(footerRight);
            player.spigot().sendMessage(footerLeft);
        } else if (getAdminHelpPages().containsKey(page + 1)) {
            footerLeft.addExtra(nextMessage);
            footerLeft.addExtra(footerRight);
            player.spigot().sendMessage(footerLeft);
        } else if (getAdminHelpPages().containsKey(page - 1)) {
            footerLeft.addExtra(previousMessage);
            footerLeft.addExtra(footerRight);
            player.spigot().sendMessage(footerLeft);
        } else
            player.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("help-footer-static")));
    }

    public void sendAdminHelpPage(CommandSender commandSender, int page) {
        if (!(commandSender instanceof Player)) {
            if (!getAdminHelpPages().containsKey(page)) {
                commandSender.sendMessage(getPluginInstance().getManager().colorText(getPluginInstance().getLangConfig().getString("invalid-help-page")));
                return;
            }

            List<String> lines = getAdminHelpPages().get(page);
            for (int i = -1; ++i < lines.size(); )
                commandSender.sendMessage(getPluginInstance().getManager().colorText(lines.get(i)));
            commandSender.sendMessage(getPluginInstance().getManager().colorText("&e<&m-------------------------------------------------------&r&e>"));
            return;
        }

        Player player = (Player) commandSender;
        if (!getAdminHelpPages().containsKey(page)) {
            getPluginInstance().getManager().sendCustomMessage("invalid-help-page", player);
            return;
        }

        List<String> lines = getAdminHelpPages().get(page);
        for (int i = -1; ++i < lines.size(); )
            player.sendMessage(getPluginInstance().getManager().colorText(lines.get(i)));

        sendJSONLine(player, page);
    }

    public void sendHelpPage(CommandSender commandSender, int page) {
        if (!(commandSender instanceof Player)) {
            if (!getHelpPages().containsKey(page)) {
                commandSender.sendMessage(getPluginInstance().getManager()
                        .colorText(getPluginInstance().getLangConfig().getString("invalid-help-page")));
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
            getPluginInstance().getManager().sendCustomMessage("invalid-help-page", player);
            return;
        }

        // getPluginInstance().getManager().clearChat(player);
        List<String> lines = getHelpPages().get(page);
        for (int i = -1; ++i < lines.size(); )
            player.sendMessage(getPluginInstance().getManager().colorText(lines.get(i)));

        sendJSONLine(player, page);
    }

    // confirmation methods

    private boolean isConfirming(Player player, Warp warp) {
        return !getWarpConfirmationMap().isEmpty() && getWarpConfirmationMap().containsKey(player.getUniqueId())
                && (!getWarpConfirmationMap().containsKey(player.getUniqueId()) || getWarpConfirmationMap().get(player.getUniqueId()).equalsIgnoreCase(warp.getWarpName()));
    }

    private void updateConfirmation(Player player, Warp warp) {
        getWarpConfirmationMap().put(player.getUniqueId(), warp.getWarpName());
    }

    public void clearConfirmation(Player player) {
        getWarpConfirmationMap().remove(player.getUniqueId());
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

    private HashMap<UUID, String> getWarpConfirmationMap() {
        return warpConfirmationMap;
    }

    private void setWarpConfirmationMap(HashMap<UUID, String> warpConfirmationMap) {
        this.warpConfirmationMap = warpConfirmationMap;
    }
}
