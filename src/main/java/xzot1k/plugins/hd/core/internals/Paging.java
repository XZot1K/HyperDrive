/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;
import xzot1k.plugins.hd.api.objects.Warp;

import java.util.*;

public class Paging {
    private HyperDrive pluginInstance;
    private Map<UUID, Map<Integer, List<UUID>>> playerSelectionPageMap;
    private Map<UUID, List<UUID>> playerSelectedMap;
    private Map<UUID, Map<Integer, List<Warp>>> warpPageMap;
    private Map<UUID, Integer> currentPageMap;
    private Random random;

    public Paging(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);
        setWarpPageMap(new HashMap<>());
        setCurrentPageMap(new HashMap<>());
        setPlayerSelectionPageMap(new HashMap<>());
        setPlayerSelectedMap(new HashMap<>());
        setRandom(new Random());
    }

    public int getCurrentPage(OfflinePlayer player) {
        if (!getCurrentPageMap().isEmpty() && getCurrentPageMap().containsKey(player.getUniqueId()))
            return getCurrentPageMap().get(player.getUniqueId());
        return 1;
    }

    // player selection pages
    public Map<Integer, List<UUID>> getCurrentPlayerSelectionPages(OfflinePlayer player) {
        if (!getPlayerSelectionPageMap().isEmpty() && getPlayerSelectionPageMap().containsKey(player.getUniqueId()))
            return getPlayerSelectionPageMap().get(player.getUniqueId());
        return null;
    }

    public void resetPlayerSelectionPages(OfflinePlayer player) {
        if (!getPlayerSelectionPageMap().isEmpty())
            getWarpPageMap().remove(player.getUniqueId());
        if (!getCurrentPageMap().isEmpty())
            getCurrentPageMap().remove(player.getUniqueId());
    }

    public boolean hasNextPlayerSelectionPage(OfflinePlayer player) {
        if (!getPlayerSelectionPageMap().isEmpty() && getPlayerSelectionPageMap().containsKey(player.getUniqueId()))
            return getPlayerSelectionPageMap().get(player.getUniqueId()).containsKey(getCurrentPage(player) + 1);
        return false;
    }

    public boolean hasPreviousPlayerSelectionPage(OfflinePlayer player) {
        if (!getPlayerSelectionPageMap().isEmpty() && getPlayerSelectionPageMap().containsKey(player.getUniqueId()))
            return getPlayerSelectionPageMap().get(player.getUniqueId()).containsKey(getCurrentPage(player) - 1);
        return false;
    }

    public Map<Integer, List<UUID>> getPlayerSelectionPages(OfflinePlayer player) {
        int slotCount = getPluginInstance().getMenusConfig().getIntegerList("ps-menu-section.player-slots").size();
        List<UUID> playerList = getPluginInstance().getManager().getPlayerUUIDs();
        playerList.remove(player.getUniqueId());
        playerSelectionSort(playerList);

        Map<Integer, List<UUID>> finalMap = new HashMap<>();
        int currentPage = 1;
        List<UUID> currentPlayerList = new ArrayList<>();
        for (int i = -1; ++i < playerList.size(); ) {
            UUID playerUniqueId = playerList.get(i);
            if (playerUniqueId != null) {
                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(playerUniqueId);
                if (offlinePlayer.isOnline()) {
                    int cooldown = getPluginInstance().getConfig().getInt("teleportation-section.cooldown-duration");
                    long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp",
                            cooldown);
                    if (currentCooldown <= 0 || Objects.requireNonNull(offlinePlayer.getPlayer())
                            .hasPermission("hyperdrive.tpcooldown")) {
                        if (currentPlayerList.size() < slotCount)
                            currentPlayerList.add(playerUniqueId);
                        else {
                            finalMap.put(currentPage, currentPlayerList);
                            currentPlayerList = new ArrayList<>();
                            currentPage += 1;
                        }
                    }
                }
            }
        }

        if (!currentPlayerList.isEmpty()) finalMap.put(currentPage, currentPlayerList);
        return finalMap;
    }

    private void playerSelectionSort(List<UUID> playerList) {
        playerSelectionSort(playerList, 0, playerList.size() - 1);
    }

    private void playerSelectionSort(List<UUID> playerList, int low, int high) {
        if (low < (high + 1)) {
            int p = playerSelectionPartition(playerList, low, high);
            playerSelectionSort(playerList, low, (p - 1));
            playerSelectionSort(playerList, (p + 1), high);
        }
    }

    private int playerSelectionPartition(List<UUID> playerList, int low, int high) {
        playerSwapIndex(playerList, low, (getRandom().nextInt((high - low) + 1) + low));
        int border = (low + 1);
        for (int i = border - 1; ++i <= high; ) {
            UUID playerAtHigh = playerList.get(i), playerAtLow = playerList.get(low);
            OfflinePlayer p1 = getPluginInstance().getServer().getOfflinePlayer(playerAtHigh),
                    p2 = getPluginInstance().getServer().getOfflinePlayer(playerAtLow);
            if (p1.isOnline() && p2.isOnline()
                    && Objects.requireNonNull(p1.getName()).compareTo(Objects.requireNonNull(p2.getName())) < 0)
                playerSwapIndex(playerList, i, border++);

            if (!p1.isOnline())
                playerList.remove(playerAtHigh);
            if (!p2.isOnline())
                playerList.remove(playerAtLow);
        }

        playerSwapIndex(playerList, low, border - 1);
        return border - 1;
    }

    private void playerSwapIndex(List<UUID> playerList, int index1, int index2) {
        UUID temp = playerList.get(index1);
        playerList.set(index1, playerList.get(index2));
        playerList.set(index2, temp);
    }

    public List<UUID> getSelectedPlayers(OfflinePlayer player) {
        if (!getPlayerSelectedMap().isEmpty() && getPlayerSelectedMap().containsKey(player.getUniqueId()))
            return getPlayerSelectedMap().get(player.getUniqueId());
        else
            return Collections.emptyList();
    }

    public void updateSelectedPlayers(OfflinePlayer player, UUID playerUniqueId, boolean isRemoval) {
        if (!getPlayerSelectedMap().isEmpty() && getPlayerSelectedMap().containsKey(player.getUniqueId())) {
            List<UUID> playerList = getPlayerSelectedMap().get(player.getUniqueId());
            if (playerList != null) {
                if (isRemoval)
                    playerList.remove(playerUniqueId);
                else
                    playerList.add(playerUniqueId);
                return;
            }
        }

        List<UUID> playerList = new ArrayList<>();
        playerList.add(playerUniqueId);
        getPlayerSelectedMap().put(player.getUniqueId(), playerList);
    }

    public void updateCurrentPlayerSelectionPage(OfflinePlayer player, boolean isNext) {
        int currentPage = getCurrentPage(player);
        Map<Integer, List<UUID>> currentPages = getCurrentPlayerSelectionPages(player);
        if (currentPages != null)
            if (!currentPages.isEmpty() && currentPages.containsKey(isNext ? currentPage + 1 : currentPage - 1))
                getCurrentPageMap().put(player.getUniqueId(), isNext ? currentPage + 1 : currentPage - 1);
            else
                getCurrentPageMap().put(player.getUniqueId(), 1);
    }

    // warp paging
    public void resetWarpPages(OfflinePlayer player) {
        getWarpPageMap().remove(player.getUniqueId());
        getCurrentPageMap().remove(player.getUniqueId());
    }

    public void updateCurrentWarpPage(OfflinePlayer player, boolean isNext) {
        int currentPage = getCurrentPage(player);
        Map<Integer, List<Warp>> currentPages = getCurrentWarpPages(player);
        if (currentPages != null)
            if (!currentPages.isEmpty() && currentPages.containsKey(isNext ? currentPage + 1 : currentPage - 1))
                getCurrentPageMap().put(player.getUniqueId(), isNext ? currentPage + 1 : currentPage - 1);
            else
                getCurrentPageMap().put(player.getUniqueId(), 1);
    }

    public Map<Integer, List<Warp>> getCurrentWarpPages(OfflinePlayer player) {
        if (!getWarpPageMap().isEmpty() && getWarpPageMap().containsKey(player.getUniqueId()))
            return getWarpPageMap().get(player.getUniqueId());
        return null;
    }

    public boolean hasNextWarpPage(OfflinePlayer player) {
        if (!getWarpPageMap().isEmpty() && getWarpPageMap().containsKey(player.getUniqueId()))
            return getWarpPageMap().get(player.getUniqueId()).containsKey(getCurrentPage(player) + 1);
        return false;
    }

    public boolean hasPreviousWarpPage(OfflinePlayer player) {
        if (!getWarpPageMap().isEmpty() && getWarpPageMap().containsKey(player.getUniqueId()))
            return getWarpPageMap().get(player.getUniqueId()).containsKey(getCurrentPage(player) - 1);
        return false;
    }

    public Map<Integer, List<Warp>> getWarpPages(OfflinePlayer player, String menuPath, EnumContainer.Filter filter, @Nullable String filterValue) {

        final int slotCount = getPluginInstance().getMenusConfig().getIntegerList(menuPath + ".warp-slots").size();
        List<Warp> warpList = new ArrayList<>(getPluginInstance().getManager().getWarpMap().values());

        final ConfigurationSection customMenus = getPluginInstance().getMenusConfig().getConfigurationSection("custom-menus-section");
        if (customMenus != null) {
            for (String menuId : customMenus.getKeys(false)) {
                if (!customMenus.contains(menuId + ".items")) continue;
                final ConfigurationSection cs = customMenus.getConfigurationSection(menuId + ".items");
                if (cs != null) {
                    for (String itemId : cs.getKeys(false)) {
                        final String clickAction = cs.getString(itemId + ".click-action");
                        if (clickAction != null) {
                            warpList.removeIf(warp -> clickAction.toLowerCase().contains(ChatColor
                                    .stripColor(warp.getWarpName().toLowerCase())));
                        }
                    }
                }
            }
        }

        final boolean isFeatured = (filter == EnumContainer.Filter.FEATURED);
        if (isFeatured) warpList.sort(new Warp.TrafficSort());
        else warpList.sort(new Warp.LikesSort());

        // warpList.sort(Warp::compareTo);
        //Collections.reverse(warpList);

        //if (!isFeatured) warpList.sort(Warp::compareTo);
        //else warpList.sort(new Warp.TrafficSort());

        //warpList.sort(Comparator.reverseOrder());

        //warpSort(warpList, false, false);
        //if (filter == EnumContainer.Filter.FEATURED) warpSort(warpList, true, false);
        // else warpSort(warpList, false, true);

        Map<Integer, List<Warp>> finalMap = new HashMap<>();
        int currentPage = 1, trafficThreshold = getPluginInstance().getMenusConfig().getInt(menuPath + ".traffic-threshold");
        List<Warp> currentWarpList = new ArrayList<>();
        for (int i = -1; ++i < warpList.size(); ) {
            final Warp warp = warpList.get(i);
            if (warp == null || !getPluginInstance().getManager().getWarpMap().containsKey(warp.getWarpName().toLowerCase()))
                continue;

            if (filter == EnumContainer.Filter.SEARCH && filterValue != null && !filterValue.isEmpty()) {

                OfflinePlayer op = getPluginInstance().getServer().getOfflinePlayer(warp.getOwner());
                if ((op.hasPlayedBefore() && op.getName() != null && op.getName().equalsIgnoreCase(filterValue))
                        || ChatColor.stripColor(warp.getWarpName()).contains(filterValue)) {
                    if (currentWarpList.size() < slotCount)
                        currentWarpList.add(warp);
                    else {
                        finalMap.put(currentPage, new ArrayList<>(currentWarpList));
                        currentWarpList.clear();
                        currentWarpList.add(warp);
                        currentPage += 1;
                    }
                }

                continue;
            } else if (filter == EnumContainer.Filter.FEATURED) {
                if (warp.getTraffic() >= trafficThreshold)
                    if (currentWarpList.size() < slotCount)
                        currentWarpList.add(warp);
                    else {
                        finalMap.put(currentPage, new ArrayList<>(currentWarpList));
                        currentWarpList.clear();
                        currentWarpList.add(warp);
                        currentPage += 1;
                    }

                continue;
            } else if (filter == EnumContainer.Filter.OWNED) {
                if (warp.getOwner().toString().equalsIgnoreCase(player.getUniqueId().toString())
                        || warp.getAssistants().contains(player.getUniqueId()))
                    if (currentWarpList.size() < slotCount)
                        currentWarpList.add(warp);
                    else {
                        finalMap.put(currentPage, new ArrayList<>(currentWarpList));
                        currentWarpList.clear();
                        currentWarpList.add(warp);
                        currentPage += 1;
                    }

                continue;
            } else if (filter == EnumContainer.Filter.PUBLIC) {
                if (warp.getStatus() == EnumContainer.Status.PUBLIC)
                    if (currentWarpList.size() < slotCount)
                        currentWarpList.add(warp);
                    else {
                        finalMap.put(currentPage, new ArrayList<>(currentWarpList));
                        currentWarpList.clear();
                        currentWarpList.add(warp);
                        currentPage += 1;
                    }

                continue;
            } else if (filter == EnumContainer.Filter.PRIVATE) {
                if (warp.getStatus() == EnumContainer.Status.PRIVATE)
                    if (currentWarpList.size() < slotCount)
                        currentWarpList.add(warp);
                    else {
                        finalMap.put(currentPage, new ArrayList<>(currentWarpList));
                        currentWarpList.clear();
                        currentWarpList.add(warp);
                        currentPage += 1;
                    }

                continue;
            } else if (filter == EnumContainer.Filter.ADMIN) {
                if (warp.getStatus() == EnumContainer.Status.ADMIN)
                    if (currentWarpList.size() < slotCount)
                        currentWarpList.add(warp);
                    else {
                        finalMap.put(currentPage, new ArrayList<>(currentWarpList));
                        currentWarpList.clear();
                        currentWarpList.add(warp);
                        currentPage += 1;
                    }

                continue;
            }

            if (currentWarpList.size() < slotCount)
                currentWarpList.add(warp);
            else {
                finalMap.put(currentPage, new ArrayList<>(currentWarpList));
                currentWarpList.clear();
                currentWarpList.add(warp);
                currentPage += 1;
            }
        }

        if (!currentWarpList.isEmpty()) {
            if (currentWarpList.size() > slotCount) {
                finalMap.put(currentPage, new ArrayList<>(currentWarpList));
                currentWarpList.clear();
                currentPage += 1;
            }

            finalMap.put(currentPage, currentWarpList);
        }

        return finalMap;
    }

    // getters & setters
    public Map<UUID, Map<Integer, List<Warp>>> getWarpPageMap() {
        return warpPageMap;
    }

    private void setWarpPageMap(Map<UUID, Map<Integer, List<Warp>>> warpPageMap) {
        this.warpPageMap = warpPageMap;
    }

    public Map<UUID, Integer> getCurrentPageMap() {
        return currentPageMap;
    }

    private void setCurrentPageMap(Map<UUID, Integer> currentPageMap) {
        this.currentPageMap = currentPageMap;
    }

    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    private Random getRandom() {
        return random;
    }

    private void setRandom(Random random) {
        this.random = random;
    }

    public Map<UUID, Map<Integer, List<UUID>>> getPlayerSelectionPageMap() {
        return playerSelectionPageMap;
    }

    private void setPlayerSelectionPageMap(Map<UUID, Map<Integer, List<UUID>>> playerSelectionPageMap) {
        this.playerSelectionPageMap = playerSelectionPageMap;
    }

    public Map<UUID, List<UUID>> getPlayerSelectedMap() {
        return playerSelectedMap;
    }

    private void setPlayerSelectedMap(Map<UUID, List<UUID>> playerSelectedMap) {
        this.playerSelectedMap = playerSelectedMap;
    }
}