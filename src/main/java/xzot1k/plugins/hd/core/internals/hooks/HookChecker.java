/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.hooks;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.registry.CatalogRegistryModule;
import com.massivecraft.factions.*;
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
import net.prosavage.factionsx.manager.GridManager;
import net.prosavage.factionsx.manager.PlayerManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xzot1k.plugins.hd.HyperDrive;

import java.util.Objects;
import java.util.Optional;

public class HookChecker {

    public final boolean townyInstalled, griefPreventionInstalled, griefDefenderInstalled,
            aSkyBlockInstalled, residenceInstalled, prismaInstalled, cmiInstalled;
    private HyperDrive pluginInstance;
    private Plugin essentialsPlugin;
    private Flag HYPERDRIVE_PROTECTION;
    private FactionsType factionsType;

    public HookChecker(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);


        factionsType = null;
        Plugin factionsPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("Factions");
        if (factionsPlugin != null) {
            if (factionsPlugin.getDescription().getDepend().contains("MassiveCore"))
                factionsType = FactionsType.MASSIVE;
            else if (factionsPlugin.getDescription().getAuthors().contains("CmdrKittens"))
                factionsType = FactionsType.UUID;
            else if (factionsPlugin.getDescription().getAuthors().contains("DroppingAnvil"))
                factionsType = FactionsType.SABER;
        } else {
            factionsPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("FactionsX");
            if (factionsPlugin != null) factionsType = FactionsType.X;
        }

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
            HYPERDRIVE_PROTECTION = Flag.builder().id("hyperdrive:protection").name("hd-protect")
                    .permission("griefdefender.flag.hyperdrive.hd-protect").build();
            Optional<CatalogRegistryModule<Flag>> catalogRegistryModule = GriefDefender.getRegistry().getRegistryModuleFor(Flag.class);
            catalogRegistryModule.ifPresent(flagCatalogRegistryModule -> flagCatalogRegistryModule.registerCustomType(HYPERDRIVE_PROTECTION));
        }
    }

    /**
     * Checks to see if the location is safe and doesn't collide with supported plugin's hook systems.
     *
     * @param player    The player to check.
     * @param location  The location to check safety for.
     * @param checkType Tells he functions what to handle checks for.
     * @return Whether it is safe.
     */
    public boolean isNotSafe(Player player, Location location, CheckType checkType) {
        if (player.hasPermission("hyperdrive.admin.bypass")) return false;

        final boolean ownershipCheck = getPluginInstance().getConfig().getBoolean("general-section.claim-ownership-checks");
        if (checkType != CheckType.WARP && getPluginInstance().getWorldGuardHandler() != null
                && !getPluginInstance().getWorldGuardHandler().passedWorldGuardHook(location, ownershipCheck, player))
            return true;

        if (getFactionsType() != null && checkType != CheckType.WARP) {
            switch (getFactionsType()) {
                case SABER:
                    Faction factionAtLocation = Board.getInstance().getFactionAt(new FLocation(location));
                    FPlayer saberPlayer = FPlayers.getInstance().getByPlayer(player);
                    if (factionAtLocation != null && (!ownershipCheck || (!factionAtLocation.isWilderness()
                            && !factionAtLocation.getId().equalsIgnoreCase(saberPlayer.getFaction().getId()))))
                        return true;
                    break;
                case X:
                    net.prosavage.factionsx.core.Faction xfaction = GridManager.INSTANCE.getFactionAt(new net.prosavage
                            .factionsx.persist.data.FLocation(location.getBlockX(), location.getBlockZ(), Objects.requireNonNull(location.getWorld()).getName()));
                    if (xfaction != null) {
                        net.prosavage.factionsx.core.FPlayer xFPlayer = PlayerManager.INSTANCE.getFPlayer(player);
                        if (!ownershipCheck || (xfaction.getId() != xFPlayer.getFaction().getId() && !xfaction.isWilderness()))
                            return true;
                    }
                    break;
                case UUID:
                    com.massivecraft.factions.entity.Faction uuidFaction = BoardColl.get().getFactionAt(PS.valueOf(location));
                    FPlayer uuidPlayer = FPlayers.getInstance().getByPlayer(player);
                    if (uuidFaction != null && (!ownershipCheck || (!uuidFaction.getId().equalsIgnoreCase(FactionColl.get().getNone().getId())
                            && !uuidFaction.getId().equalsIgnoreCase(uuidPlayer.getFaction().getId()))))
                        return true;
                    break;
                case MASSIVE:
                    com.massivecraft.factions.entity.Faction mFaction = BoardColl.get().getFactionAt(PS.valueOf(location));
                    MPlayer fPlayer = MPlayer.get(player);
                    if (mFaction != null && (!ownershipCheck || (mFaction.getComparisonName().equalsIgnoreCase(FactionColl.get().getNone().getComparisonName())
                            && !fPlayer.getFaction().getComparisonName().equalsIgnoreCase(mFaction.getComparisonName())))) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }

        if (aSkyBlockInstalled && checkType != CheckType.WARP) {
            Island island = ASkyBlockAPI.getInstance().getIslandAt(location);
            if (island != null && (!ownershipCheck || (!island.getOwner().toString().equals(player.getUniqueId().toString())
                    && !island.getMembers().contains(player.getUniqueId())))) return true;
        }

        if (griefPreventionInstalled && checkType != CheckType.WARP) {
            Claim claimAtLocation = GriefPrevention.instance.dataStore.getClaimAt(location, true, null);
            if (claimAtLocation != null && (!ownershipCheck || !claimAtLocation.getOwnerName().equalsIgnoreCase(player.getName())))
                return true;
        }

        final World world = player.getWorld();
        if (griefDefenderInstalled && checkType != CheckType.WARP && GriefDefender.getCore().isEnabled(world.getUID())) {
            com.griefdefender.api.claim.Claim claimAtLocation = GriefDefender.getCore().getClaimManager(Objects.requireNonNull(location.getWorld()).getUID())
                    .getClaimAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            if (claimAtLocation != null && !claimAtLocation.isWilderness() && (!ownershipCheck || (claimAtLocation.getOwnerUniqueId().toString()
                    .equals(player.getUniqueId().toString()) || claimAtLocation.getUserTrusts().contains(player.getUniqueId())))) return true;
        }

        if (townyInstalled && checkType != CheckType.WARP) {
            try {
                Town town = WorldCoord.parseWorldCoord(location).getTownBlock().getTown();
                if (town != null) {
                    Resident resident = TownyAPI.getInstance().getDataSource().getResident(player.getName());
                    if (resident != null && (!ownershipCheck || (!town.getResidents().contains(resident) && town.getMayor() != resident)))
                        return true;
                }
            } catch (Exception ignored) {
            }
        }

        if (residenceInstalled && checkType != CheckType.WARP) {
            ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(location);
            return (res != null && (!ownershipCheck || !res.isOwner(player))); // If false is returned, the hook failed and teleportation is blocked.
        }

        return false;
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

    public FactionsType getFactionsType() {
        return factionsType;
    }

    public enum CheckType {
        CREATION, WARP, RTP
    }

    public enum FactionsType {
        UUID, X, MASSIVE, SABER
    }

}
