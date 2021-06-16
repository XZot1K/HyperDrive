/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WorldGuardHandler {

    private final WorldGuardPlugin worldGuardPlugin;

    public WorldGuardHandler() {
        worldGuardPlugin = WorldGuardPlugin.inst();
    }

    /**
     * Checks if the location is within a region that also has correct flag setup.
     *
     * @param location The location.
     * @return Whether the check passed.
     */
    public boolean passedWorldGuardHook(Location location) {
        if (worldGuardPlugin == null) return true;

        if (worldGuardPlugin.getDescription().getVersion().startsWith("6")) {
            try {
                Method method = worldGuardPlugin.getClass().getMethod("getRegionManager", World.class);
                method.setAccessible(true);

                RegionManager regionManager = (RegionManager) method.invoke(worldGuardPlugin, location.getWorld());
                if (regionManager == null) return true;

                Class<? extends RegionManager> rmClass = regionManager.getClass();
                Method applicableRegionsMethod = rmClass.getMethod("getApplicableRegions", Location.class);
                applicableRegionsMethod.setAccessible(true);
                return (((ApplicableRegionSet) applicableRegionsMethod.invoke(regionManager, location)).size() <= 0);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            ApplicableRegionSet regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
                    .getApplicableRegions(BukkitAdapter.adapt(location));
            return (regionContainer.size() <= 0);
        }

        return true;
    }

}
