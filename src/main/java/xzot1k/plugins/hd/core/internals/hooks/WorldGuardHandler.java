/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

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
     * @param location       The location.
     * @param checkOwnerShip Determines if ownership should be checked.
     * @param player         The owner to check for.
     * @return Whether the check passed.
     */
    public boolean passedWorldGuardHook(Location location, boolean checkOwnerShip, Player player) {
        if (worldGuardPlugin == null) return true;

        LocalPlayer localPlayer = (player != null ? WorldGuardPlugin.inst().wrapPlayer(player) : null);
        if (worldGuardPlugin.getDescription().getVersion().startsWith("6")) {
            try {
                Method method = worldGuardPlugin.getClass().getMethod("getRegionManager", World.class);
                method.setAccessible(true);

                RegionManager regionManager = (RegionManager) method.invoke(worldGuardPlugin, location.getWorld());
                if (regionManager == null) return true;

                Class<? extends RegionManager> rmClass = regionManager.getClass();
                Method applicableRegionsMethod = rmClass.getDeclaredMethod("getApplicableRegions", Location.class);
                applicableRegionsMethod.setAccessible(true);

                ApplicableRegionSet regionSet = ((ApplicableRegionSet) applicableRegionsMethod.invoke(regionManager, location));
                if (checkOwnerShip && localPlayer != null) {
                    for (ProtectedRegion region : regionSet) {
                        if (region.isOwner(localPlayer)) continue;
                        return false;
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            ApplicableRegionSet set = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().getApplicableRegions(BukkitAdapter.adapt(location));
            if (set == null) return true;
            return set.testState(localPlayer, Flags.BUILD);
        }

        return true;
    }

}
