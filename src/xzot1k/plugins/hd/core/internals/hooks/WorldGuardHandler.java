/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.hooks;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class WorldGuardHandler {

    private static StateFlag HD_ALLOW;
    private final WorldGuardPlugin worldGuardPlugin;

    public WorldGuardHandler() {
        FlagRegistry registry = null;
        worldGuardPlugin = WorldGuardPlugin.inst();
        if (worldGuardPlugin.getDescription().getVersion().startsWith("6")) {
            try {
                Method method = worldGuardPlugin.getClass().getMethod("getFlagRegistry");
                registry = (FlagRegistry) method.invoke(worldGuardPlugin);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else registry = com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry();

        if (registry == null) return;
        try {
            StateFlag flag = new StateFlag("hd-allow", false);
            registry.register(flag);
            HD_ALLOW = flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("hd-allow");
            if (existing instanceof StateFlag) HD_ALLOW = (StateFlag) existing;
        }
    }

    public boolean passedWorldGuardHook(Location location) {
        if (worldGuardPlugin == null) return true;

        ApplicableRegionSet applicableRegionSet = null;
        if (worldGuardPlugin.getDescription().getVersion().startsWith("6")) {
            try {
                Method method = worldGuardPlugin.getClass().getMethod("getRegionManager", World.class);

                RegionManager regionManager = (RegionManager) method.invoke(worldGuardPlugin, location.getWorld());
                if (regionManager == null) return true;

                Method applicableRegionsMethod = worldGuardPlugin.getClass().getMethod("getApplicableRegions", Location.class);
                applicableRegionSet = (ApplicableRegionSet) applicableRegionsMethod.invoke(regionManager, location);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            RegionQuery query = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location worldEditLocation = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location);
            applicableRegionSet = query.getApplicableRegions(worldEditLocation);
        }

        if (applicableRegionSet == null) return true;
        Set<ProtectedRegion> regions = applicableRegionSet.getRegions();
        if (regions.isEmpty()) return true;

        for (ProtectedRegion protectedRegion : regions)
            if (!protectedRegion.getFlags().containsKey(HD_ALLOW) && (protectedRegion.getFlags().get(HD_ALLOW) instanceof Boolean
                    && !((boolean) protectedRegion.getFlags().get(HD_ALLOW)))) return false;
        return true;
    }
}
