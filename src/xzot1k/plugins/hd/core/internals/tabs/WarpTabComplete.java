/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.tabs;

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
        boolean hasAllAccess = (commandSender.isOp() || commandSender.hasPermission("hyperdrive.admin.tab"));
        List<String> warpNames = hasAllAccess
                ? new ArrayList<>(getPluginInstance().getManager().getWarpMap().keySet()) : new ArrayList<>();

        if (!hasAllAccess && commandSender instanceof Player) {
            Player player = (Player) commandSender;
            for (Warp warp : getPluginInstance().getManager().getWarpMap().values()) {
                if (warpNames.contains(warp.getWarpName()) && warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString())
                        || warp.getAssistants().contains(player.getUniqueId()) || (warp.isWhiteListMode() && warp.getPlayerList().contains(player.getUniqueId()))
                        || warp.getStatus() == EnumContainer.Status.PUBLIC || (warp.getStatus() == EnumContainer.Status.ADMIN
                        && (player.hasPermission("hyperdrive.warps." + warp.getWarpName()) || player.hasPermission("hyperdrive.warps.*"))))
                    warpNames.add(warp.getWarpName());
            }
        }

        Collections.sort(warpNames);
        return warpNames;
    }

    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}
