/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.hooks;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xzot1k.plugins.hd.HyperDrive;

import java.util.Optional;

public class HookChecker {

    public final boolean townyInstalled, griefPreventionInstalled, griefDefenderInstalled,
            aSkyBlockInstalled, residenceInstalled, prismaInstalled, cmiInstalled, landsInstalled;
    private HyperDrive pluginInstance;
    private Plugin essentialsPlugin;
    private com.griefdefender.api.permission.flag.Flag HYPERDRIVE_PROTECTION;
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
        landsInstalled = (getPluginInstance().getServer().getPluginManager().getPlugin("Lands") != null);

        essentialsPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("Essentials");
        if (essentialsPlugin == null)
            essentialsPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("EssentialsEx");

        if (griefDefenderInstalled) {
            HYPERDRIVE_PROTECTION = com.griefdefender.api.permission.flag.Flag.builder().id("hyperdrive:protection").name("hd-protect")
                    .permission("griefdefender.flag.hyperdrive.hd-protect").build();
            Optional<com.griefdefender.api.registry.CatalogRegistryModule<com.griefdefender.api.permission.flag.Flag>>
                    catalogRegistryModule = com.griefdefender.api.GriefDefender.getRegistry()
                    .getRegistryModuleFor(com.griefdefender.api.permission.flag.Flag.class);
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
                    com.massivecraft.factions.Faction factionAtLocation = com.massivecraft.factions.Board
                            .getInstance().getFactionAt(new com.massivecraft.factions.FLocation(location));
                    com.massivecraft.factions.FPlayer saberPlayer = com.massivecraft.factions.FPlayers.getInstance().getByPlayer(player);
                    if (factionAtLocation != null && (!ownershipCheck || (!factionAtLocation.isWilderness()
                            && !factionAtLocation.getId().equalsIgnoreCase(saberPlayer.getFaction().getId()))))
                        return true;
                    break;
                case UUID:
                    com.massivecraft.factions.Faction uuidFaction = com.massivecraft.factions.Board.getInstance()
                            .getFactionAt(new com.massivecraft.factions.FLocation(location));
                    com.massivecraft.factions.FPlayer uuidPlayer = com.massivecraft.factions.FPlayers.getInstance().getByPlayer(player);
                    if (uuidFaction != null && (!ownershipCheck || (!uuidFaction.getId()
                            .equalsIgnoreCase(com.massivecraft.factions.Factions.getInstance().getWilderness().getId())
                            && !uuidFaction.getId().equalsIgnoreCase(uuidPlayer.getFaction().getId()))))
                        return true;
                    break;
                case MASSIVE:
                    com.massivecraft.factions.entity.Faction mFaction = com.massivecraft.factions.entity.BoardColl
                            .get().getFactionAt(com.massivecraft.massivecore.ps.PS.valueOf(location));
                    com.massivecraft.factions.entity.MPlayer fPlayer = com.massivecraft.factions.entity.MPlayer.get(player);
                    if (mFaction != null && (!ownershipCheck || (mFaction.getComparisonName()
                            .equalsIgnoreCase(com.massivecraft.factions.entity.FactionColl.get().getNone().getComparisonName())
                            && !fPlayer.getFaction().getComparisonName().equalsIgnoreCase(mFaction.getComparisonName())))) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }

        if (aSkyBlockInstalled && checkType != CheckType.WARP) {
            com.wasteofplastic.askyblock.Island island = com.wasteofplastic.askyblock.ASkyBlockAPI.getInstance().getIslandAt(location);
            if (island != null && (!ownershipCheck || (!island.getOwner().toString().equals(player.getUniqueId().toString())
                    && !island.getMembers().contains(player.getUniqueId())))) return true;
        }

        if (griefPreventionInstalled && checkType != CheckType.WARP) {
            me.ryanhamshire.GriefPrevention.Claim claimAtLocation = me.ryanhamshire.GriefPrevention.GriefPrevention
                    .instance.dataStore.getClaimAt(location, true, null);
            if (claimAtLocation != null && (!ownershipCheck || !claimAtLocation.getOwnerName().equalsIgnoreCase(player.getName())
                    || !claimAtLocation.getOwnerID().toString().equals(player.getUniqueId().toString())))
                return true;
        }

        final World world = player.getWorld();
        if (griefDefenderInstalled && checkType != CheckType.WARP
                && com.griefdefender.api.GriefDefender.getCore().isEnabled(world.getUID()) && location.getWorld() != null) {
            com.griefdefender.api.claim.Claim claimAtLocation = com.griefdefender.api.GriefDefender.getCore().getClaimAt(location);
            if (claimAtLocation != null && !claimAtLocation.isWilderness()) {

                if (claimAtLocation.getParent() != null && !claimAtLocation.getParent().isWilderness()
                        && claimAtLocation.getParent().getOwnerUniqueId().toString().equals(player.getUniqueId().toString()))
                    return true;

                if (!claimAtLocation.getOwnerUniqueId().toString().equals(player.getUniqueId().toString())) return true;
            }
        }

        if (townyInstalled && checkType != CheckType.WARP) {
            try {
                com.palmergames.bukkit.towny.object.Town town = com.palmergames.bukkit.towny.object.WorldCoord
                        .parseWorldCoord(location).getTownBlock().getTown();
                if (town != null) {
                    final com.palmergames.bukkit.towny.object.Resident resident = com.palmergames.bukkit
                            .towny.TownyAPI.getInstance().getResident(player.getName());
                    if (resident != null && (!ownershipCheck || (!town.getResidents().contains(resident) && town.getMayor() != resident)))
                        return true;
                }
            } catch (Exception ignored) {
            }
        }

        if (residenceInstalled && checkType != CheckType.WARP) {
            com.bekvon.bukkit.residence.protection.ClaimedResidence res = com.bekvon.bukkit.residence
                    .Residence.getInstance().getResidenceManager().getByLoc(location);
            return (res != null && (!ownershipCheck || !res.isOwner(player))); // If false is returned, the hook failed and teleportation is blocked.
        }

        if (landsInstalled && checkType != CheckType.WARP) {
            me.angeschossen.lands.api.LandsIntegration api = me.angeschossen.lands.api.LandsIntegration.of(pluginInstance);
            me.angeschossen.lands.api.land.LandWorld landWorld = api.getWorld(world);
            if (landWorld != null) {
                me.angeschossen.lands.api.land.Area area = landWorld.getArea(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                if (area != null) {
                    if (checkType == CheckType.RTP) return false;
                    if (!area.getOwnerUID().toString().equals(player.getUniqueId().toString())) {
                        // me.angeschossen.lands.api.player.LandPlayer landPlayer = api.getLandPlayer(player.getUniqueId());
                        // if(landPlayer != null && !landWorld.hasFlag(landPlayer, location, null,  me.angeschossen.lands.api.flags.Flags., false))
                        return false;
                    }
                }
            }
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