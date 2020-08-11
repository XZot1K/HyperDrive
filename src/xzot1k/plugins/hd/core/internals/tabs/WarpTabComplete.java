/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.tabs;

import net.md_5.bungee.api.ChatColor;
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

        if (command.getName().equalsIgnoreCase("warps") && args.length == 1) {
            boolean hasAllAccess = (commandSender.isOp() || commandSender.hasPermission("hyperdrive.admin.tab"));
            final List<String> warpNames = new ArrayList<>();

            if (hasAllAccess) {
                for (Warp warp : getPluginInstance().getManager().getWarpMap().values())
                    if (warp.getWarpName().toLowerCase().contains(args[0].toLowerCase()))
                        warpNames.add(ChatColor.stripColor(warp.getWarpName()));
            } else if (commandSender instanceof Player) {
                Player player = (Player) commandSender;
                for (Warp warp : getPluginInstance().getManager().getWarpMap().values()) {
                    if (warp.getWarpName().toLowerCase().contains(args[0].toLowerCase()) && !warpNames.contains(warp.getWarpName())
                            && ((warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString()))
                            || warp.getAssistants().contains(player.getUniqueId()) || (warp.isWhiteListMode() && warp.getPlayerList().contains(player.getUniqueId()))
                            || warp.getStatus() == EnumContainer.Status.PUBLIC || (warp.getStatus() == EnumContainer.Status.ADMIN
                            && ((player.hasPermission("hyperdrive.warps." + warp.getWarpName()) || player.hasPermission("hyperdrive.warps.*"))))))
                        warpNames.add(ChatColor.stripColor(warp.getWarpName()));
                }
            }

            Collections.sort(warpNames);
            return warpNames;
        }

        if (args.length >= 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : getPluginInstance().getServer().getOnlinePlayers())
                playerNames.add(player.getName());
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
