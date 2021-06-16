/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.tabs;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;
import xzot1k.plugins.hd.api.objects.Warp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WarpTabComplete implements TabCompleter {

    private HyperDrive pluginInstance;

    public WarpTabComplete(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("teleportposition") && args.length >= 3)
            return new ArrayList<String>() {{
                add("~");
            }};

        if (command.getName().equalsIgnoreCase("warps") && args.length == 1) {
            boolean hasAllAccess = (commandSender.isOp() || commandSender.hasPermission("hyperdrive.admin.tab"));
            final List<String> list = new ArrayList<>();

            if ("list".contains(args[0].toLowerCase())) list.add("list");
            if ("assistants".contains(args[0].toLowerCase())) list.add("assistants");
            if ("create".contains(args[0].toLowerCase())) list.add("create");
            if ("delete".contains(args[0].toLowerCase())) list.add("delete");
            if ("clear".contains(args[0].toLowerCase())) list.add("clear");
            if ("visits".contains(args[0].toLowerCase())) list.add("visits");
            if ("help".contains(args[0].toLowerCase())) list.add("help");

            if (hasAllAccess) {
                for (Warp warp : getPluginInstance().getManager().getWarpMap().values())
                    if (warp.getWarpName().toLowerCase().contains(args[0].toLowerCase()))
                        list.add(ChatColor.stripColor(warp.getWarpName()));
            } else if (commandSender instanceof Player) {
                Player player = (Player) commandSender;
                for (Warp warp : getPluginInstance().getManager().getWarpMap().values()) {
                    if (warp.getWarpName().toLowerCase().contains(args[0].toLowerCase()) && !list.contains(warp.getWarpName())
                            && ((warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString()))
                            || warp.getAssistants().contains(player.getUniqueId()) || (warp.isWhiteListMode() && warp.getPlayerList().contains(player.getUniqueId()))
                            || (!warp.isWhiteListMode() && !warp.getPlayerList().contains(player.getUniqueId()))
                            || warp.getStatus() == EnumContainer.Status.PUBLIC || (warp.getStatus() == EnumContainer.Status.ADMIN
                            && ((player.hasPermission("hyperdrive.warps." + net.md_5.bungee.api.ChatColor.stripColor(warp.getWarpName())) || player.hasPermission("hyperdrive.warps.*"))))))
                        list.add(ChatColor.stripColor(warp.getWarpName()));
                }
            }

            Collections.sort(list);
            return list;
        } else if (command.getName().equalsIgnoreCase("warps") && args.length == 2 && args[0].equalsIgnoreCase("visits")) {
            final List<String> list = new ArrayList<>();

            if (commandSender.hasPermission("hyperdrive.admin.visits")) {
                list.add("add");
                list.add("remove");
                list.add("set");

                Collections.sort(list);
            }

            return list;
        } else if (command.getName().equalsIgnoreCase("warps") && args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            final List<String> list = new ArrayList<>();

            if (commandSender.hasPermission("hyperdrive.clear")) {
                for (World world : getPluginInstance().getServer().getWorlds())
                    list.add(world.getName());

                Collections.sort(list);
            }

            return list;
        } else if (command.getName().equalsIgnoreCase("warps") && args.length == 2 && args[0].equalsIgnoreCase("delete")
                && commandSender instanceof Player) {
            final List<String> list = new ArrayList<>();

            if (commandSender.hasPermission("hyperdrive.use.delete")) {
                list.addAll(getPluginInstance().getManager().getPermittedWarps((Player) commandSender));
                Collections.sort(list);
            }

            return list;
        }

        if (args.length >= 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : getPluginInstance().getServer().getOnlinePlayers())
                if (!getPluginInstance().getManager().isVanished(player)) playerNames.add(player.getName());
            Collections.sort(playerNames);
            return playerNames;
        }

        return null;
    }

    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}
