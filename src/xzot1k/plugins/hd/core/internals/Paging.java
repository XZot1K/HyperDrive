package xzot1k.plugins.hd.core.internals;

import org.bukkit.OfflinePlayer;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;
import xzot1k.plugins.hd.api.objects.Warp;

import java.util.*;

public class Paging {
    private HyperDrive pluginInstance;
    private HashMap<UUID, HashMap<Integer, List<UUID>>> playerSelectionPageMap;
    private HashMap<UUID, List<UUID>> playerSelectedMap;
    private HashMap<UUID, HashMap<Integer, List<Warp>>> warpPageMap;
    private HashMap<UUID, Integer> currentPageMap;
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
    public HashMap<Integer, List<UUID>> getCurrentPlayerSelectionPages(OfflinePlayer player) {
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

    public HashMap<Integer, List<UUID>> getPlayerSelectionPages(OfflinePlayer player) {
        int slotCount = getPluginInstance().getConfig().getIntegerList("ps-menu-section.player-slots").size();
        List<UUID> playerList = getPluginInstance().getManager().getPlayerUUIDs();
        playerList.remove(player.getUniqueId());
        playerSelectionSort(playerList);

        HashMap<Integer, List<UUID>> finalMap = new HashMap<>();
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

        if (currentPlayerList.size() > 0)
            finalMap.put(currentPage, currentPlayerList);
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
            return null;
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
        HashMap<Integer, List<UUID>> currentPages = getCurrentPlayerSelectionPages(player);
        if (currentPages != null)
            if (!currentPages.isEmpty() && currentPages.containsKey(isNext ? currentPage + 1 : currentPage - 1))
                getCurrentPageMap().put(player.getUniqueId(), isNext ? currentPage + 1 : currentPage - 1);
            else
                getCurrentPageMap().put(player.getUniqueId(), 1);
    }

    // warp paging
    public void resetWarpPages(OfflinePlayer player) {
        if (!getWarpPageMap().isEmpty())
            getWarpPageMap().remove(player.getUniqueId());
        if (!getCurrentPageMap().isEmpty())
            getCurrentPageMap().remove(player.getUniqueId());
    }

    public void updateCurrentWarpPage(OfflinePlayer player, boolean isNext) {
        int currentPage = getCurrentPage(player);
        HashMap<Integer, List<Warp>> currentPages = getCurrentWarpPages(player);
        if (currentPages != null)
            if (!currentPages.isEmpty() && currentPages.containsKey(isNext ? currentPage + 1 : currentPage - 1))
                getCurrentPageMap().put(player.getUniqueId(), isNext ? currentPage + 1 : currentPage - 1);
            else
                getCurrentPageMap().put(player.getUniqueId(), 1);
    }

    public HashMap<Integer, List<Warp>> getCurrentWarpPages(OfflinePlayer player) {
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

    public HashMap<Integer, List<Warp>> getWarpPages(OfflinePlayer player, String menuPath, String status) {
        String ownedFormat = getPluginInstance().getConfig().getString("list-menu-section.own-status-format"),
                publicFormat = getPluginInstance().getConfig().getString("list-menu-section.public-status-format"),
                privateFormat = getPluginInstance().getConfig().getString("list-menu-section.private-status-format"),
                adminFormat = getPluginInstance().getConfig().getString("list-menu-section.admin-status-format"),
                featuredFormat = getPluginInstance().getConfig().getString("list-menu-section.featured-status-format");

        switch (status.toLowerCase()) {
            case "public":
                status = publicFormat;
                break;
            case "private":
                status = privateFormat;
                break;
            case "admin":
                status = adminFormat;
                break;
            case "featured":
                status = featuredFormat;
                break;
            default:
                break;
        }

        int slotCount = getPluginInstance().getConfig().getIntegerList(menuPath + ".warp-slots").size();
        List<Warp> warpList = !getPluginInstance().getManager().getWarpMap().isEmpty()
                ? new ArrayList<>(getPluginInstance().getManager().getWarpMap().values())
                : new ArrayList<>();

        if (status != null)
            warpSort(warpList, status.equals(featuredFormat));

        HashMap<Integer, List<Warp>> finalMap = new HashMap<>();
        int currentPage = 1,
                trafficThreshold = getPluginInstance().getConfig().getInt("list-menu-section.traffic-threshold");
        List<Warp> currentWarpList = new ArrayList<>();
        for (int i = -1; ++i < warpList.size(); ) {
            Warp warp = warpList.get(i);
            if (warp != null && status != null) {
                if (status.equalsIgnoreCase(featuredFormat)) {
                    if (warp.getTraffic() >= trafficThreshold)
                        if (currentWarpList.size() < slotCount)
                            currentWarpList.add(warp);
                        else {
                            finalMap.put(currentPage, currentWarpList);
                            currentWarpList = new ArrayList<>();
                            currentPage += 1;
                        }

                    continue;
                } else if (status.equalsIgnoreCase(ownedFormat)) {
                    if (warp.getOwner().toString().equalsIgnoreCase(player.getUniqueId().toString())
                            || warp.getAssistants().contains(player.getUniqueId()))
                        if (currentWarpList.size() < slotCount)
                            currentWarpList.add(warp);
                        else {
                            finalMap.put(currentPage, currentWarpList);
                            currentWarpList = new ArrayList<>();
                            currentPage += 1;
                        }

                    continue;
                } else if (status.equalsIgnoreCase(publicFormat)) {
                    if (warp.getStatus() == EnumContainer.Status.PUBLIC)
                        if (currentWarpList.size() < slotCount)
                            currentWarpList.add(warp);
                        else {
                            finalMap.put(currentPage, currentWarpList);
                            currentWarpList = new ArrayList<>();
                            currentPage += 1;
                        }

                    continue;
                } else if (status.equalsIgnoreCase(privateFormat)) {
                    if (warp.getStatus() == EnumContainer.Status.PRIVATE)
                        if (currentWarpList.size() < slotCount)
                            currentWarpList.add(warp);
                        else {
                            finalMap.put(currentPage, currentWarpList);
                            currentWarpList = new ArrayList<>();
                            currentPage += 1;
                        }

                    continue;
                } else if (status.equalsIgnoreCase(adminFormat)) {
                    if (warp.getStatus() == EnumContainer.Status.ADMIN)
                        if (currentWarpList.size() < slotCount)
                            currentWarpList.add(warp);
                        else {
                            finalMap.put(currentPage, currentWarpList);
                            currentWarpList = new ArrayList<>();
                            currentPage += 1;
                        }

                    continue;
                }

                if (currentWarpList.size() < slotCount)
                    currentWarpList.add(warp);
                else {
                    finalMap.put(currentPage, currentWarpList);
                    currentWarpList = new ArrayList<>();
                    currentPage += 1;
                }
            }
        }

        if (currentWarpList.size() > 0)
            finalMap.put(currentPage, currentWarpList);
        return finalMap;
    }

    private void warpSort(List<Warp> warpList, boolean sortAsFeatured) {
        warpSort(warpList, 0, warpList.size() - 1, sortAsFeatured);
    }

    private void warpSort(List<Warp> warpList, int low, int high, boolean sortAsFeatured) {
        if (low < (high + 1)) {
            int p = warpPartition(warpList, low, high, sortAsFeatured);
            warpSort(warpList, low, (p - 1), sortAsFeatured);
            warpSort(warpList, (p + 1), high, sortAsFeatured);
        }
    }

    private int warpPartition(List<Warp> warpList, int low, int high, boolean sortAsFeatured) {
        warpSwapIndex(warpList, low, (getRandom().nextInt((high - low) + 1) + low));
        int border = (low + 1);
        for (int i = border - 1; ++i <= high; ) {
            Warp warpAtHigh = warpList.get(i), warpAtLow = warpList.get(low);
            int compareValue = (sortAsFeatured ? ((warpAtHigh.getTraffic() > warpAtLow.getTraffic()) ? 0 : 1)
                    : (warpAtHigh.getLikes() > warpAtLow.getLikes() ? 0 : 1));
            if (compareValue == 0)
                warpSwapIndex(warpList, i, border++);
        }

        warpSwapIndex(warpList, low, border - 1);
        return border - 1;
    }

    private void warpSwapIndex(List<Warp> warpList, int index1, int index2) {
        Warp temp = warpList.get(index1);
        warpList.set(index1, warpList.get(index2));
        warpList.set(index2, temp);
    }

    // getters & setters
    public HashMap<UUID, HashMap<Integer, List<Warp>>> getWarpPageMap() {
        return warpPageMap;
    }

    private void setWarpPageMap(HashMap<UUID, HashMap<Integer, List<Warp>>> warpPageMap) {
        this.warpPageMap = warpPageMap;
    }

    public HashMap<UUID, Integer> getCurrentPageMap() {
        return currentPageMap;
    }

    private void setCurrentPageMap(HashMap<UUID, Integer> currentPageMap) {
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

    public HashMap<UUID, HashMap<Integer, List<UUID>>> getPlayerSelectionPageMap() {
        return playerSelectionPageMap;
    }

    private void setPlayerSelectionPageMap(HashMap<UUID, HashMap<Integer, List<UUID>>> playerSelectionPageMap) {
        this.playerSelectionPageMap = playerSelectionPageMap;
    }

    public HashMap<UUID, List<UUID>> getPlayerSelectedMap() {
        return playerSelectedMap;
    }

    private void setPlayerSelectedMap(HashMap<UUID, List<UUID>> playerSelectedMap) {
        this.playerSelectedMap = playerSelectedMap;
    }
}
