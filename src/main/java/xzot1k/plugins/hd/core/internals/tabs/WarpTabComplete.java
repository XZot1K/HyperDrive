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

        if (command.getName().equalsIgnoreCase("rtp")) {
            if (args.length == 1) return new ArrayList<String>() {{
                for (Player player : getPluginInstance().getServer().getOnlinePlayers())
                    if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())
                            && !getPluginInstance().getTeleportationCommands().getToggledPlayers().contains(player.getUniqueId()))
                        add(player.getName());
            }};
            else if (args.length == 2) return new ArrayList<String>() {{
                for (World world : getPluginInstance().getServer().getWorlds())
                    if (world.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                        add(world.getName());
            }};
        } else if (command.getName().equalsIgnoreCase("rtpadmin")) {
            if (args.length == 1) return new ArrayList<String>() {{
                for (Player player : getPluginInstance().getServer().getOnlinePlayers())
                    if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())
                            && !getPluginInstance().getTeleportationCommands().getToggledPlayers().contains(player.getUniqueId()))
                        add(player.getName());
            }};
            else if (args.length == 2) return new ArrayList<String>() {{
                for (World world : getPluginInstance().getServer().getWorlds())
                    if (world.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                        add(world.getName());
            }};
        } else if (command.getName().equalsIgnoreCase("tppos") && args.length >= 3)
            return new ArrayList<String>() {{
                add("~");
            }};
        else if (command.getName().equalsIgnoreCase("tpa") || command.getName().equalsIgnoreCase("tpahere")
                || command.getName().equalsIgnoreCase("tpohere") || command.getName().equalsIgnoreCase("tppos")) {
            if (args.length == 1) return new ArrayList<String>() {{
                for (Player player : getPluginInstance().getServer().getOnlinePlayers())
                    if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())
                            && !getPluginInstance().getTeleportationCommands().getToggledPlayers().contains(player.getUniqueId()))
                        add(player.getName());
            }};
        } else if (command.getName().equalsIgnoreCase("spawn")) {
            if (args.length == 1) return new ArrayList<String>() {{
                for (Player player : getPluginInstance().getServer().getOnlinePlayers())
                    if (player.getName().toLowerCase().startsWith(args[0].toLowerCase()) && (commandSender.hasPermission("hyperdrive.admin.tab")
                            || !getPluginInstance().getTeleportationCommands().getToggledPlayers().contains(player.getUniqueId())))
                        add(player.getName());
            }};
        } else if (command.getName().equalsIgnoreCase("tpaccept") || command.getName().equalsIgnoreCase("tpdeny")) {
            if (args.length == 1) return new ArrayList<String>() {{
                for (Player player : getPluginInstance().getServer().getOnlinePlayers())
                    if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())
                            && getPluginInstance().getTeleportationCommands().hasRequestPending((Player) commandSender, player)
                            && !getPluginInstance().getTeleportationCommands().getToggledPlayers().contains(player.getUniqueId()))
                        add(player.getName());
            }};
        } else if (command.getName().equalsIgnoreCase("warps") && args.length == 1) {
            boolean hasAllAccess = (commandSender.isOp() || commandSender.hasPermission("hyperdrive.admin.tab"));
            final List<String> list = new ArrayList<>();

            if ("list".startsWith(args[0].toLowerCase())) list.add("list");
            else if ("assistants".startsWith(args[0].toLowerCase())) list.add("assistants");
            else if ("create".startsWith(args[0].toLowerCase())) list.add("create");
            else if ("delete".startsWith(args[0].toLowerCase())) list.add("delete");
            else if ("clear".startsWith(args[0].toLowerCase())) list.add("clear");
            else if ("visits".startsWith(args[0].toLowerCase())) list.add("visits");
            else if ("help".startsWith(args[0].toLowerCase())) list.add("help");
            else if ("setstatus".startsWith(args[0].toLowerCase())) list.add("setstatus");
            else if ("edit".startsWith(args[0].toLowerCase())) list.add("edit");
            else if ("accept".startsWith(args[0].toLowerCase())) list.add("accept");
            else if ("deny".startsWith(args[0].toLowerCase())) list.add("deny");

            if (hasAllAccess) {
                for (Warp warp : getPluginInstance().getManager().getWarpMap().values()) {
                    final String warpName = ChatColor.stripColor(warp.getWarpName());
                    if (warpName.toLowerCase().startsWith(args[0].toLowerCase()))
                        list.add(warpName);
                }
            } else if (commandSender instanceof Player) {
                Player player = (Player) commandSender;
                for (Warp warp : getPluginInstance().getManager().getWarpMap().values()) {
                    final String warpName = ChatColor.stripColor(warp.getWarpName());
                    if (warpName.toLowerCase().startsWith(args[0].toLowerCase()) && !list.contains(warpName)
                            && ((warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString()))
                            || warp.getAssistants().contains(player.getUniqueId()) || (warp.isWhiteListMode() && warp.getPlayerList().contains(player.getUniqueId()))
                            || (!warp.isWhiteListMode() && !warp.getPlayerList().contains(player.getUniqueId()))
                            || warp.getStatus() == EnumContainer.Status.PUBLIC || (warp.getStatus() == EnumContainer.Status.ADMIN
                            && ((player.hasPermission("hyperdrive.warps." + warpName) || player.hasPermission("hyperdrive.warps.*"))))))
                        if (warpName.toLowerCase().startsWith(args[0].toLowerCase()))
                            list.add(ChatColor.stripColor(warp.getWarpName()));
                }
            }

            Collections.sort(list);
            return list;
        } else if (command.getName().equalsIgnoreCase("warps") && args.length == 2 && args[0].equalsIgnoreCase("visits")) {
            final List<String> list = new ArrayList<>();

            if (commandSender.hasPermission("hyperdrive.admin.visits")) {
                if ("add".startsWith(args[1].toLowerCase())) list.add("add");
                else if ("remove".startsWith(args[1].toLowerCase())) list.add("remove");
                else if ("set".startsWith(args[1].toLowerCase())) list.add("set");

                Collections.sort(list);
            }

            return list;
        } else if (command.getName().equalsIgnoreCase("warps") && args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            final List<String> list = new ArrayList<>();

            if (commandSender.hasPermission("hyperdrive.clear")) {
                for (World world : getPluginInstance().getServer().getWorlds())
                    if (world.getName().toLowerCase().startsWith(args[1].toLowerCase())) list.add(world.getName());

                Collections.sort(list);
            }

            return list;
        } else if (command.getName().equalsIgnoreCase("warps") && args.length == 2 && args[0].equalsIgnoreCase("delete")
                && commandSender instanceof Player) {
            final List<String> list = new ArrayList<>();

            if (commandSender.hasPermission("hyperdrive.use.delete")) {
                for (String warp : getPluginInstance().getManager().getPermittedWarps((Player) commandSender))
                    if (warp.toLowerCase().startsWith(args[1].toLowerCase())) list.add(warp);
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
