package xzot1k.plugins.hd.core.internals.hooks.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import xzot1k.plugins.hd.HyperDrive;

import java.util.ArrayList;
import java.util.List;

public class WG_7
{

    public static boolean passedWorldGuardHook(Location location)
    {
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        com.sk89q.worldedit.util.Location worldEditLocation = BukkitAdapter.adapt(location);


        ApplicableRegionSet regionsAtLocation = query.getApplicableRegions(worldEditLocation);
        List<ProtectedRegion> regionList = new ArrayList<>(regionsAtLocation.getRegions());
        List<String> whitelistedRegions = HyperDrive.getPluginInstance().getConfig().getStringList("hooks-section.world-guard.whitelist");

        for (int i = -1; ++i < regionList.size(); )
        {
            ProtectedRegion protectedRegion = regionList.get(i);
            if (!isInList(whitelistedRegions, protectedRegion.getId())) return false;
        }

        return true;
    }

    private static boolean isInList(List<String> stringList, String string)
    {
        for (int i = -1; ++i < stringList.size(); ) if (stringList.get(i).equalsIgnoreCase(string)) return true;
        return false;
    }
}
