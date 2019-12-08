/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.hooks.worldguard;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import xzot1k.plugins.hd.HyperDrive;

import java.util.List;

public class WG_6
{

    public static boolean passedWorldGuardHook(Location location) {
        WorldGuardPlugin worldGuardPlugin = getWorldGuard();
        if (worldGuardPlugin != null) {
            RegionManager regionManager = worldGuardPlugin.getRegionManager(location.getWorld());
            if (regionManager != null) {
                List<String> whitelistedRegions = HyperDrive.getPluginInstance().getConfig().getStringList("hooks-section.world-guard.whitelist");
                for (ProtectedRegion protectedRegion : regionManager.getApplicableRegions(location).getRegions()) {
                    if (!isInList(whitelistedRegions, protectedRegion.getId())) return false;
                }
            }
        }

        return true;
    }

    private static boolean isInList(List<String> stringList, String string) {
        for (int i = -1; ++i < stringList.size(); ) if (stringList.get(i).equalsIgnoreCase(string)) return true;
        return false;
    }

    private static WorldGuardPlugin getWorldGuard() {
        Plugin plugin = HyperDrive.getPluginInstance().getServer().getPluginManager().getPlugin("WorldGuard");
        if (!(plugin instanceof WorldGuardPlugin)) return null;
        return (WorldGuardPlugin) plugin;
    }
}
