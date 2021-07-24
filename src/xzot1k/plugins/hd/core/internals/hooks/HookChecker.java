/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.hooks;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Subject;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.permission.flag.Flag;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
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

import java.util.Objects;

public class HookChecker {


    private HyperDrive pluginInstance;
    public final boolean factionsInstalled, factionsUUID, townyInstalled, griefPreventionInstalled, griefDefenderInstalled,
            aSkyBlockInstalled, residenceInstalled, prismaInstalled, cmiInstalled;
    private Plugin essentialsPlugin;
    private Flag HYPERDRIVE_PROTECTION;

    public HookChecker(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);

        Plugin factionsPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("Factions");
        factionsInstalled = (getPluginInstance().getServer().getPluginManager().getPlugin("Factions") != null);
        factionsUUID = (factionsPlugin != null && factionsPlugin.getDescription().getDepend().contains("MassiveCore"));

        aSkyBlockInstalled = (getPluginInstance().getServer().getPluginManager().getPlugin("ASkyBlock") != null);
        griefPreventionInstalled = (getPluginInstance().getServer().getPluginManager().getPlugin("GriefPrevention") != null);
        griefDefenderInstalled = (getPluginInstance().getServer().getPluginManager().getPlugin("GriefDefender") != null);
        townyInstalled = (getPluginInstance().getServer().getPluginManager().getPlugin("Towny") != null);
        residenceInstalled = (getPluginInstance().getServer().getPluginManager().getPlugin("Residence") != null);
        prismaInstalled = (getPluginInstance().getServer().getPluginManager().getPlugin("Prisma") != null);
        cmiInstalled = (getPluginInstance().getServer().getPluginManager().getPlugin("CMI") != null);

        essentialsPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("Essentials");
        if (essentialsPlugin == null)
            essentialsPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("EssentialsEx");


        if (griefDefenderInstalled) {
            HYPERDRIVE_PROTECTION = Flag.builder()
                    .id("hyperdrive:protection")
                    .name("hd-protect")
                    .permission("griefdefender.flag.hyperdrive.hd-protect")
                    .build();

            GriefDefender.getRegistry().getRegistryModuleFor(Flag.class).get().registerCustomType(HYPERDRIVE_PROTECTION);
        }
    }

    /**
     * Checks to see if the location is safe and doesn't collide with supported plugin's hook systems.
     *
     * @param player          The player to check.
     * @param location        The location to check safety for.
     * @param checkWorldGuard Checks if the location is in a region.
     * @return Whether it is safe.
     */
    public boolean isLocationHookSafe(Player player, Location location, boolean checkWorldGuard) {
        if (player.hasPermission("hyperdrive.admin.bypass")) return true;

        if (checkWorldGuard && getPluginInstance().getWorldGuardHandler() != null
                && !getPluginInstance().getWorldGuardHandler().passedWorldGuardHook(location))
            return false;

        final boolean ownershipCheck = getPluginInstance().getConfig().getBoolean("claim-ownership-checks");
        if (factionsInstalled) {
            if (!factionsUUID) {
                com.massivecraft.factions.entity.Faction factionAtLocation = BoardColl.get().getFactionAt(PS.valueOf(location));
                MPlayer mPlayer = MPlayer.get(player);
                if (factionAtLocation != null && (!ownershipCheck || (!factionAtLocation.getId().equalsIgnoreCase(FactionColl.get().getNone().getId())
                        && !factionAtLocation.getId().equalsIgnoreCase(mPlayer.getFaction().getId())))) {
                    return false;
                }
            } else {
                FLocation fLocation = new FLocation(location);
                com.massivecraft.factions.Faction factionAtLocation = Board.getInstance().getFactionAt(fLocation);
                FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
                if (factionAtLocation != null && (!ownershipCheck || (!factionAtLocation.isWilderness()
                        && !fPlayer.getFaction().getComparisonTag().equalsIgnoreCase(factionAtLocation.getComparisonTag())))) {
                    return false;
                }
            }
        }

        if (aSkyBlockInstalled) {
            Island island = ASkyBlockAPI.getInstance().getIslandAt(location);
            if (island != null && (!ownershipCheck || (!island.getOwner().toString().equals(player.getUniqueId().toString()) && !island.getMembers().contains(player.getUniqueId()))))
                return false;
        }

        if (griefPreventionInstalled) {
            Claim claimAtLocation = GriefPrevention.instance.dataStore.getClaimAt(location, true, null);
            if (claimAtLocation != null && (!ownershipCheck || !claimAtLocation.getOwnerName().equalsIgnoreCase(player.getName())))
                return false;
        }

        if (griefDefenderInstalled && GriefDefender.getCore().isEnabled(player.getWorld().getUID())) {
            com.griefdefender.api.claim.Claim claimAtLocation = GriefDefender.getCore().getClaimManager(Objects.requireNonNull(location.getWorld()).getUID())
                    .getClaimAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            final Subject subject = GriefDefender.getCore().getSubject(player.getUniqueId().toString());
            final Tristate result = claimAtLocation.getActiveFlagPermissionValue(HYPERDRIVE_PROTECTION, subject, null, true);

            if (!claimAtLocation.isWilderness() && !(result == Tristate.FALSE) && ((!ownershipCheck) || (!claimAtLocation.getUserTrusts().contains(player.getUniqueId()))))
                return false;
        }

        if (townyInstalled) {
            try {
                Town town = WorldCoord.parseWorldCoord(location).getTownBlock().getTown();
                if (town != null) {
                    Resident resident = TownyAPI.getInstance().getDataSource().getResident(player.getName());
                    if (resident != null && (!ownershipCheck || (!town.getResidents().contains(resident) && town.getMayor() != resident)))
                        return false;
                }
            } catch (Exception ignored) {
            }
        }

        if (residenceInstalled) {
            ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(location);
            return (res == null || (ownershipCheck && res.isOwner(player)));
        }

        return true;
    }

    // getters & setters
    public HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public Plugin getEssentialsPlugin() {
        return essentialsPlugin;
    }

}
