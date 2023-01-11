/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.api.objects;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;

public class Warp implements Comparable<Warp> {
    private HyperDrive pluginInstance;
    private SerializableLocation warpLocation;
    private String warpName, creationDate, iconTheme, animationSet, serverIPAddress, description;
    private EnumContainer.Status status;
    private UUID owner;
    private List<UUID> playerList, assistants, voters;
    private List<String> commands;
    private int traffic, likes, dislikes;
    private double usagePrice;
    private boolean enchantedLook, whiteListMode, notify;

    public Warp(String warpName, OfflinePlayer player, Location location) {
        setPluginInstance(HyperDrive.getPluginInstance());
        setWarpName(warpName);
        setCreationDate(getPluginInstance().getManager().getSimpleDateFormat().format(new Date()));
        setOwner(player.getUniqueId());
        setWarpLocation(location);

        final boolean noServerFound = (getPluginInstance().getBungeeListener() == null || getPluginInstance().getBungeeListener().getMyServer() == null
                || getPluginInstance().getBungeeListener().getMyServer().isEmpty());
        if (noServerFound)
            setServerIPAddress(getPluginInstance().getConfig().getString("mysql-connection.default-ip"));
        else setServerIPAddress(getPluginInstance().getBungeeListener().getMyServer());

        String defaultMaterial = getPluginInstance().getConfig().getString("warp-icon-section.default-icon-material");
        setIconTheme(defaultMaterial != null ? defaultMaterial : "");
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        setAnimationSet(animationSetList.size() > 0 ? animationSetList.get(0) : "");
        setStatus(EnumContainer.Status.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-status"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setDescription(getPluginInstance().getConfig().getString("warp-icon-section.default-description"));
        setUsagePrice(0);
        setTraffic(0);
        setLikes(0);
        setDislikes(0);
        setVoters(new ArrayList<>());
        setPlayerList(new ArrayList<>());
        setCommands(new ArrayList<>());
        setAssistants(new ArrayList<>());
        setWhiteListMode(true);
        setIconEnchantedLook(false);
        setNotify(true);
    }

    public Warp(String warpName, Location location) {
        setPluginInstance(HyperDrive.getPluginInstance());
        setWarpName(warpName);
        setCreationDate(getPluginInstance().getManager().getSimpleDateFormat().format(new Date()));
        setWarpLocation(location);

        final boolean noServerFound = (getPluginInstance().getBungeeListener() == null || getPluginInstance().getBungeeListener().getMyServer() == null
                || getPluginInstance().getBungeeListener().getMyServer().isEmpty());
        if (noServerFound)
            setServerIPAddress(getPluginInstance().getConfig().getString("mysql-connection.default-ip"));
        else setServerIPAddress(getPluginInstance().getBungeeListener().getMyServer());

        String defaultMaterial = getPluginInstance().getConfig().getString("warp-icon-section.default-icon-material");
        setIconTheme(defaultMaterial != null ? defaultMaterial : "");
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        setAnimationSet(animationSetList.size() > 0 ? animationSetList.get(0) : "");
        setStatus(EnumContainer.Status.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-status"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setDescription(getPluginInstance().getConfig().getString("warp-icon-section.default-description"));
        setUsagePrice(0);
        setTraffic(0);
        setLikes(0);
        setDislikes(0);
        setVoters(new ArrayList<>());
        setPlayerList(new ArrayList<>());
        setCommands(new ArrayList<>());
        setAssistants(new ArrayList<>());
        setWhiteListMode(true);
        setIconEnchantedLook(false);
        setNotify(true);
    }

    public Warp(String warpName, OfflinePlayer player, SerializableLocation serializableLocation) {
        setPluginInstance(HyperDrive.getPluginInstance());
        setWarpName(warpName);
        setCreationDate(getPluginInstance().getManager().getSimpleDateFormat().format(new Date()));
        setOwner(player.getUniqueId());
        setWarpLocation(serializableLocation);

        final boolean noServerFound = (getPluginInstance().getBungeeListener() == null || getPluginInstance().getBungeeListener().getMyServer() == null
                || getPluginInstance().getBungeeListener().getMyServer().isEmpty());
        if (noServerFound)
            setServerIPAddress(getPluginInstance().getConfig().getString("mysql-connection.default-ip"));
        else setServerIPAddress(getPluginInstance().getBungeeListener().getMyServer());

        String defaultMaterial = getPluginInstance().getConfig().getString("warp-icon-section.default-icon-material");
        setIconTheme(defaultMaterial != null ? defaultMaterial : "");
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        setAnimationSet(animationSetList.size() > 0 ? animationSetList.get(0) : "");
        setStatus(EnumContainer.Status.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-status"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setDescription(getPluginInstance().getConfig().getString("warp-icon-section.default-description"));
        setUsagePrice(0);
        setTraffic(0);
        setLikes(0);
        setDislikes(0);
        setVoters(new ArrayList<>());
        setPlayerList(new ArrayList<>());
        setCommands(new ArrayList<>());
        setAssistants(new ArrayList<>());
        setWhiteListMode(true);
        setIconEnchantedLook(false);
        setNotify(true);
    }

    public Warp(String warpName, SerializableLocation serializableLocation) {
        setPluginInstance(HyperDrive.getPluginInstance());
        setWarpName(warpName);
        setCreationDate(getPluginInstance().getManager().getSimpleDateFormat().format(new Date()));
        setWarpLocation(serializableLocation);

        final boolean noServerFound = (getPluginInstance().getBungeeListener() == null || getPluginInstance().getBungeeListener().getMyServer() == null
                || getPluginInstance().getBungeeListener().getMyServer().isEmpty());
        if (noServerFound)
            setServerIPAddress(getPluginInstance().getConfig().getString("mysql-connection.default-ip"));
        else setServerIPAddress(getPluginInstance().getBungeeListener().getMyServer());

        String defaultMaterial = getPluginInstance().getConfig().getString("warp-icon-section.default-icon-material");
        setIconTheme(defaultMaterial != null ? defaultMaterial : "");
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        setAnimationSet(animationSetList.size() > 0 ? animationSetList.get(0) : "");
        setStatus(EnumContainer.Status.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-status"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setDescription(getPluginInstance().getConfig().getString("warp-icon-section.default-description"));
        setUsagePrice(0);
        setTraffic(0);
        setLikes(0);
        setDislikes(0);
        setVoters(new ArrayList<>());
        setPlayerList(new ArrayList<>());
        setCommands(new ArrayList<>());
        setAssistants(new ArrayList<>());
        setWhiteListMode(true);
        setIconEnchantedLook(false);
        setNotify(true);
    }

    public void register() {
        getPluginInstance().getManager().getWarpMap().put(getWarpName().toLowerCase(), this);
    }

    public void unRegister() {
        if (!getPluginInstance().getManager().getWarpMap().isEmpty())
            getPluginInstance().getManager().getWarpMap().remove(getWarpName().toLowerCase());
    }

    public String getLikeBar() {
        StringBuilder bar = new StringBuilder();
        double averageValue = Math.min(getLikes(), getDislikes());
        for (int i = -1; ++i < 12; ) {
            if (getLikes() == getDislikes()) {
                bar.append("&f").append("\u25CF");
                continue;
            } else if (getLikes() == 0 && getDislikes() != 0) {
                bar.append("&c").append("\u25CF");
                continue;
            } else if (getDislikes() == 0 && getLikes() != 0) {
                bar.append("&a").append("\u25CF");
                continue;
            }

            if (averageValue >= 0.1) {
                bar.append("&a").append("\u25CF");
                averageValue -= 0.1;
            } else bar.append("&c").append("\u25CF");
        }

        return bar.toString();
    }

    public double getLikeRatio() {return (((double) getLikes()) / ((double) getDislikes()));}

    public void deleteSaved(boolean async) {
        if (!async) delete(getWarpName());
        else
            getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> delete(getWarpName()));
    }

    public void delete(String warpName) {
        try {
            Statement statement = getPluginInstance().getDatabaseConnection().createStatement();
            statement.executeUpdate("DELETE FROM warps WHERE name = '" + warpName + "';");
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            getPluginInstance().log(Level.WARNING, "There was an issue deleting the warp " + getWarpName() + " from the database (" + e.getMessage() + ").");
        }
    }

    public void save(boolean async) {
        if (async)
            getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), (Runnable) this::save);
        else save();
    }

    private void save() {
        try {
            setWarpName(getWarpName().replace("'", "").replace("\"", ""));

            StringBuilder commands = new StringBuilder(), playerList = new StringBuilder(), assistants = new StringBuilder(), voters = new StringBuilder();
            for (int j = -1; ++j < getCommands().size(); )
                commands.append(getCommands().get(j)).append(",");
            for (int j = -1; ++j < getPlayerList().size(); )
                playerList.append(getPlayerList().get(j).toString()).append(",");
            for (int j = -1; ++j < getAssistants().size(); )
                assistants.append(getAssistants().get(j).toString()).append(",");
            for (int j = -1; ++j < getVoters().size(); )
                voters.append(getVoters().get(j).toString()).append(",");

            final String locationString = (getWarpLocation().getWorldName() + "," + getWarpLocation().getX() + "," + getWarpLocation().getY() + ","
                    + getWarpLocation().getZ() + "," + getWarpLocation().getYaw() + "," + getWarpLocation().getPitch());

            String syntax;
            if (!getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql"))
                syntax = "INSERT OR REPLACE INTO warps(name, location, status, creation_date, icon_theme, animation_set, description, commands, owner, player_list, assistants, traffic, usage_price, "
                        + "enchanted_look, server_ip, likes, dislikes, voters, white_list_mode, notify) VALUES('" + getWarpName() + "', '" + locationString + "',"
                        + " '" + getStatus().name() + "', '" + getCreationDate() + "', '" + getIconTheme() + "', '" + getAnimationSet() + "', '"
                        + getDescription().replace("'", "<hd:sq>").replace("\"", "<hd:dq>")
                        + "', '" + commands.toString().replace("'", "") + "', '" + (getOwner() != null ? getOwner().toString() : "")
                        + "', '" + playerList + "', '" + assistants + "', " + getTraffic() + ", " + getUsagePrice() + ", " + (hasIconEnchantedLook() ? 1 : 0)
                        + ", '" + getServerIPAddress() + "', " + getLikes() + ", " + getDislikes() + ", '" + voters
                        + "', " + (isWhiteListMode() ? 1 : 0) + ", " + (canNotify() ? 1 : 0) + ");";
            else
                syntax = "INSERT INTO warps(name, location, status, creation_date, icon_theme, animation_set, description, commands, owner, player_list, assistants, traffic, usage_price, "
                        + "enchanted_look, server_ip, likes, dislikes, voters, white_list_mode, notify) VALUES('" + getWarpName() + "', '" + locationString + "',"
                        + " '" + getStatus().name() + "', '" + getCreationDate() + "', '" + getIconTheme() + "', '" + getAnimationSet() + "', '" + getDescription().replace("'", "")
                        + "', '" + commands.toString().replace("'", "") + "', '" + (getOwner() != null ? getOwner().toString() : "")
                        + "', '" + playerList + "', '" + assistants + "', " + getTraffic() + ", " + getUsagePrice() + ", " + (hasIconEnchantedLook() ? 1 : 0)
                        + ", '" + getServerIPAddress() + "', " + getLikes() + ", " + getDislikes() + ", '" + voters
                        + "', " + (isWhiteListMode() ? 1 : 0) + ", " + (canNotify() ? 1 : 0) + ") ON DUPLICATE KEY UPDATE name = '" + getWarpName() + "',"
                        + " location = '" + locationString + "', status = '" + getStatus().name() + "', creation_date = '" + getCreationDate() + "', "
                        + "icon_theme = '" + getIconTheme() + "', animation_set = '" + getAnimationSet() + "', description = '" + getDescription().replace("'", "<hd:sq>").replace("\"", "<hd:dq>")
                        + "', commands = '" + commands.toString().replace("'", "").replace("\"", "") + "', owner = '" + (getOwner() != null ? getOwner().toString() : "")
                        + "', player_list = '" + playerList + "', assistants = '" + assistants + "', traffic = '" + getTraffic() + "', usage_price = '" + getUsagePrice() + "',"
                        + " enchanted_look = '" + (hasIconEnchantedLook() ? 1 : 0) + "', server_ip = '" + getServerIPAddress() + "',"
                        + " likes = '" + getLikes() + "', dislikes = '" + getDislikes() + "', voters = '" + voters + "', white_list_mode = '" + (isWhiteListMode() ? 1 : 0) + "', notify = '" + (canNotify() ? 1 : 0) + "';";

            PreparedStatement preparedStatement = getPluginInstance().getDatabaseConnection().prepareStatement(syntax);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            getPluginInstance().log(Level.WARNING, "There was an issue saving the warp '" + getWarpName() + "' to the database (" + e.getMessage() + ").");
        }
    }

    /**
     * Renames the warp to the given name (Handles Re-Registration).
     *
     * @param newName The new name (Does NOT filter).
     */
    public void rename(String newName) {
        unRegister();
        final String finalWarpName = getWarpName();
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> delete(finalWarpName));
        setWarpName(newName);
        register();
        save(true);
    }

    // getters & setters
    public SerializableLocation getWarpLocation() {
        return warpLocation;
    }

    public void setWarpLocation(Location warpLocation) {
        this.warpLocation = new SerializableLocation(warpLocation);
    }

    public void setWarpLocation(SerializableLocation warpLocation) {
        this.warpLocation = warpLocation;
    }

    public String getWarpName() {
        return warpName;
    }

    public void setWarpName(String warpName) {
        this.warpName = warpName;
    }

    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EnumContainer.Status getStatus() {
        return status;
    }

    public void setStatus(EnumContainer.Status status) {
        this.status = status;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public double getUsagePrice() {
        return usagePrice;
    }

    public void setUsagePrice(double usagePrice) {
        this.usagePrice = usagePrice;
    }

    public boolean hasIconEnchantedLook() {
        return enchantedLook;
    }

    public void setIconEnchantedLook(boolean enchantedLook) {
        this.enchantedLook = enchantedLook;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public List<UUID> getAssistants() {
        return assistants;
    }

    public void setAssistants(List<UUID> assistants) {
        this.assistants = assistants;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getIconTheme() {
        return iconTheme;
    }

    public void setIconTheme(String iconTheme) {
        this.iconTheme = iconTheme;
    }

    public String getAnimationSet() {
        return animationSet;
    }

    public void setAnimationSet(String animationSet) {
        this.animationSet = animationSet;
    }

    public String getServerIPAddress() {
        return serverIPAddress;
    }

    public void setServerIPAddress(String serverIPAddress) {
        this.serverIPAddress = serverIPAddress;
    }

    public int getTraffic() {
        return traffic;
    }

    public void setTraffic(int traffic) {
        this.traffic = traffic;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }

    public List<UUID> getVoters() {
        return voters;
    }

    public void setVoters(List<UUID> voters) {
        this.voters = voters;
    }

    public List<UUID> getPlayerList() {
        return playerList;
    }

    public void setPlayerList(List<UUID> playerList) {
        this.playerList = playerList;
    }

    public boolean isWhiteListMode() {
        return whiteListMode;
    }

    public void setWhiteListMode(boolean whiteListMode) {
        this.whiteListMode = whiteListMode;
    }

    @Override
    public int compareTo(Warp warp) {
        final int comparedNamesResult = ChatColor.stripColor(warp.getWarpName()).compareToIgnoreCase(ChatColor.stripColor(getWarpName()));
        if (comparedNamesResult != 0) return comparedNamesResult;

        final double maxRatioMax = Math.max(warp.getLikes(), getDislikes()), maxRatioMin = Math.min(warp.getLikes(), getDislikes()),
                minRatioMax = Math.max(warp.getLikes(), getDislikes()), minRatioMin = Math.min(warp.getLikes(), getDislikes());
        if (!(maxRatioMax == 0 && maxRatioMin == 0) && !(minRatioMax == 0 && minRatioMin == 0)) {
            final double maxRatio = (maxRatioMin / maxRatioMax), minRatio = (minRatioMin / minRatioMax),
                    ratioDifference = (maxRatio - minRatio);
            if (ratioDifference != 0) return (int) ratioDifference;
        }

        return 0;
    }

    public boolean canNotify() {
        return notify;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }
}