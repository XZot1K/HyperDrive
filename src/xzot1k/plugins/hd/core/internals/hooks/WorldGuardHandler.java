/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

import java.util.Collection;

public class WorldGuardHandler {

    private static StateFlag HD_ALLOW;

    public WorldGuardHandler() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
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
        Collection<ProtectedRegion> regions = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().getApplicableRegions(BukkitAdapter.adapt(location)).getRegions();
        if (regions.isEmpty()) return true;

        for (ProtectedRegion protectedRegion : regions)
            if (protectedRegion.getFlags().containsKey(HD_ALLOW) && (protectedRegion.getFlags().get(HD_ALLOW) instanceof Boolean
                    && ((boolean) protectedRegion.getFlags().get(HD_ALLOW))))
                return true;
        return false;
    }
}
