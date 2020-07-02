/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.hooks;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xzot1k.plugins.hd.HyperDrive;

public class HookChecker {

    private HyperDrive pluginInstance;
    private boolean factionsInstalled, factionsUUID, townyInstalled, griefPreventionInstalled, aSkyBlockInstalled, residenceInstalled,
            prismaInstalled;

    public HookChecker(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);

        Plugin factionsPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("Factions");
        setFactionsInstalled(factionsPlugin != null);
        setFactionsUUID(factionsPlugin != null && factionsPlugin.getDescription().getDepend().contains("MassiveCore"));

        setASkyBlockInstalled(getPluginInstance().getServer().getPluginManager().getPlugin("ASkyBlock") != null);
        setGriefPreventionInstalled(getPluginInstance().getServer().getPluginManager().getPlugin("GriefPrevention") != null);
        setTownyInstalled(getPluginInstance().getServer().getPluginManager().getPlugin("Towny") != null);
        setResidenceInstalled(getPluginInstance().getServer().getPluginManager().getPlugin("Residence") != null);
        setPrismaInstalled(getPluginInstance().getServer().getPluginManager().getPlugin("Prisma") != null);
    }

    /**
     * Checks to see if the location is safe and doesn't collide with supported plugin's hook systems.
     *
     * @param player   The player to check.
     * @param location The location to check safety for.
     * @return Whether it is safe.
     */
    public boolean isLocationHookSafe(Player player, Location location) {
        if (player.hasPermission("hyperdrive.admin.bypass")) return true;

        boolean isSafeLocation = true;
        if (getPluginInstance().getWorldGuardHandler() != null && !getPluginInstance().getWorldGuardHandler().passedWorldGuardHook(location))
            isSafeLocation = false;

        if (isFactionsInstalled()) {
            if (!isFactionsUUID()) {
                com.massivecraft.factions.entity.Faction factionAtLocation = BoardColl.get().getFactionAt(PS.valueOf(location));
                MPlayer mPlayer = MPlayer.get(player);
                if (!factionAtLocation.getId().equalsIgnoreCase(FactionColl.get().getNone().getId()) && !factionAtLocation.getId().equalsIgnoreCase(mPlayer.getFaction().getId()))
                    isSafeLocation = false;
            } else {
                FLocation fLocation = new FLocation(location);
                com.massivecraft.factions.Faction factionAtLocation = Board.getInstance().getFactionAt(fLocation);
                FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
                if (!factionAtLocation.isWilderness() && !fPlayer.getFaction().getComparisonTag().equalsIgnoreCase(factionAtLocation.getComparisonTag()))
                    isSafeLocation = false;
            }
        }

        if (isASkyBlockInstalled()) {
            Island island = ASkyBlockAPI.getInstance().getIslandAt(location);
            if (island != null && !island.getOwner().toString().equals(player.getUniqueId().toString()) && !island.getMembers().contains(player.getUniqueId()))
                isSafeLocation = false;
        }

        if (isGriefPreventionInstalled()) {
            Claim claimAtLocation = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
            if (claimAtLocation != null)
                isSafeLocation = false;
        }

        if (isTownyInstalled()) {
            try {
                Town town = WorldCoord.parseWorldCoord(location).getTownBlock().getTown();
                if (town != null) isSafeLocation = false;
            } catch (Exception ignored) {}
        }

        if (isResidenceInstalled()) {
            ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(location);
            if (res != null) isSafeLocation = false;
        }

        return isSafeLocation;
    }

    // getters & setters
    public boolean isFactionsInstalled() {
        return factionsInstalled;
    }

    private void setFactionsInstalled(boolean factionsInstalled) {
        this.factionsInstalled = factionsInstalled;
    }

    public boolean isTownyInstalled() {
        return townyInstalled;
    }

    private void setTownyInstalled(boolean townyInstalled) {
        this.townyInstalled = townyInstalled;
    }

    public boolean isGriefPreventionInstalled() {
        return griefPreventionInstalled;
    }

    private void setGriefPreventionInstalled(boolean griefPreventionInstalled) {
        this.griefPreventionInstalled = griefPreventionInstalled;
    }

    public boolean isASkyBlockInstalled() {
        return aSkyBlockInstalled;
    }

    private void setASkyBlockInstalled(boolean aSkyBlockInstalled) {
        this.aSkyBlockInstalled = aSkyBlockInstalled;
    }

    public boolean isResidenceInstalled() {
        return residenceInstalled;
    }

    private void setResidenceInstalled(boolean residenceInstalled) {
        this.residenceInstalled = residenceInstalled;
    }

    public HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public boolean isFactionsUUID() {
        return factionsUUID;
    }

    private void setFactionsUUID(boolean factionsUUID) {
        this.factionsUUID = factionsUUID;
    }

    public boolean isPrismaInstalled() {
        return prismaInstalled;
    }

    private void setPrismaInstalled(boolean prismaInstalled) {
        this.prismaInstalled = prismaInstalled;
    }
}
