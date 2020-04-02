/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.api.objects;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;

public class Warp {
    private HyperDrive pluginInstance;
    private SerializableLocation warpLocation;
    private String warpName, creationDate, iconTheme, animationSet, serverIPAddress, description;
    private EnumContainer.Status status;
    private ChatColor displayNameColor, descriptionColor;
    private UUID owner;
    private List<UUID> playerList, assistants, voters;
    private List<String> commands;
    private int traffic, likes, dislikes;
    private double usagePrice;
    private boolean enchantedLook, whiteListMode;

    public Warp(String warpName, OfflinePlayer player, Location location) {
        setPluginInstance(HyperDrive.getPluginInstance());
        setWarpName(warpName);
        setCreationDate(getPluginInstance().getManager().getSimpleDateFormat().format(new Date()));
        setOwner(player.getUniqueId());
        setWarpLocation(location);
        if (!getPluginInstance().getServer().getIp().equalsIgnoreCase(""))
            setServerIPAddress((getPluginInstance().getServer().getIp().contains("localhost") ? "127.0.0.1" : getPluginInstance().getServer().getIp())
                    + ":" + getPluginInstance().getServer().getPort());
        else
            setServerIPAddress(getPluginInstance().getConfig().getString("mysql-connection.default-ip") + ":" + getPluginInstance().getServer().getPort());

        String defaultMaterial = getPluginInstance().getConfig().getString("warp-icon-section.default-icon-material");
        setIconTheme(defaultMaterial != null ? defaultMaterial : "");
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        setAnimationSet(animationSetList.size() > 0 ? animationSetList.get(0) : "");

        setDisplayNameColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-name-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setDescriptionColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-description-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
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
    }

    public Warp(String warpName, Location location) {
        setPluginInstance(HyperDrive.getPluginInstance());
        setWarpName(warpName);
        setCreationDate(getPluginInstance().getManager().getSimpleDateFormat().format(new Date()));
        setWarpLocation(location);
        if (!getPluginInstance().getServer().getIp().equalsIgnoreCase(""))
            setServerIPAddress((getPluginInstance().getServer().getIp().contains("localhost") ? "127.0.0.1" : getPluginInstance().getServer().getIp())
                    + ":" + getPluginInstance().getServer().getPort());
        else
            setServerIPAddress(getPluginInstance().getConfig().getString("mysql-connection.default-ip") + ":" + getPluginInstance().getServer().getPort());

        String defaultMaterial = getPluginInstance().getConfig().getString("warp-icon-section.default-icon-material");
        setIconTheme(defaultMaterial != null ? defaultMaterial : "");
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        setAnimationSet(animationSetList.size() > 0 ? animationSetList.get(0) : "");

        setDisplayNameColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-name-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setDescriptionColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-description-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
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
    }

    public Warp(String warpName, OfflinePlayer player, SerializableLocation serializableLocation) {
        setPluginInstance(HyperDrive.getPluginInstance());
        setWarpName(warpName);
        setCreationDate(getPluginInstance().getManager().getSimpleDateFormat().format(new Date()));
        setOwner(player.getUniqueId());
        setWarpLocation(serializableLocation);
        if (!getPluginInstance().getServer().getIp().equalsIgnoreCase(""))
            setServerIPAddress((getPluginInstance().getServer().getIp().contains("localhost") ? "127.0.0.1" : getPluginInstance().getServer().getIp())
                    + ":" + getPluginInstance().getServer().getPort());
        else
            setServerIPAddress(getPluginInstance().getConfig().getString("mysql-connection.default-ip") + ":" + getPluginInstance().getServer().getPort());

        String defaultMaterial = getPluginInstance().getConfig().getString("warp-icon-section.default-icon-material");
        setIconTheme(defaultMaterial != null ? defaultMaterial : "");
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        setAnimationSet(animationSetList.size() > 0 ? animationSetList.get(0) : "");

        setDisplayNameColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-name-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setDescriptionColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-description-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
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
    }

    public Warp(String warpName, SerializableLocation serializableLocation) {
        setPluginInstance(HyperDrive.getPluginInstance());
        setWarpName(warpName);
        setCreationDate(getPluginInstance().getManager().getSimpleDateFormat().format(new Date()));
        setWarpLocation(serializableLocation);
        if (!getPluginInstance().getServer().getIp().equalsIgnoreCase(""))
            setServerIPAddress((getPluginInstance().getServer().getIp().contains("localhost") ? "127.0.0.1" : getPluginInstance().getServer().getIp())
                    + ":" + getPluginInstance().getServer().getPort());
        else
            setServerIPAddress(getPluginInstance().getConfig().getString("mysql-connection.default-ip") + ":" + getPluginInstance().getServer().getPort());

        String defaultMaterial = getPluginInstance().getConfig().getString("warp-icon-section.default-icon-material");
        setIconTheme(defaultMaterial != null ? defaultMaterial : "");
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        setAnimationSet(animationSetList.size() > 0 ? animationSetList.get(0) : "");

        setDisplayNameColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-name-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setDescriptionColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-description-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
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

    public void deleteSaved(boolean async) {
        if (!async) delete(getWarpName());
        else
            getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> delete(getWarpName()));
    }

    private void delete(String warpName) {
        try {
            PreparedStatement preparedStatement = getPluginInstance().getDatabaseConnection().prepareStatement("delete from warps where name = '" + getWarpName() + "'");
            preparedStatement.executeUpdate();
            preparedStatement.close();
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
            setWarpName(getWarpName().replace("§", "").replaceAll("[.,?:;'\"\\\\|`~!@#$%^&*()+=/<>]", ""));

            StringBuilder commands = new StringBuilder(), playerList = new StringBuilder(), assistants = new StringBuilder(), voters = new StringBuilder();
            for (int j = -1; ++j < getCommands().size(); )
                commands.append(getCommands().get(j)).append(",");
            for (int j = -1; ++j < getPlayerList().size(); )
                playerList.append(getPlayerList().get(j).toString()).append(",");
            for (int j = -1; ++j < getAssistants().size(); )
                assistants.append(getAssistants().get(j).toString()).append(",");
            for (int j = -1; ++j < getVoters().size(); )
                voters.append(getVoters().get(j).toString()).append(",");

            final String locationString = (getWarpLocation().getWorldName() + "," + getWarpLocation().getX() + "," + getWarpLocation().getY() + "," + getWarpLocation().getZ() + ","
                    + getWarpLocation().getYaw() + "," + getWarpLocation().getPitch()), owner = (getOwner() != null ? getOwner().toString() : "");

            Statement statement = getPluginInstance().getDatabaseConnection().createStatement();
            ResultSet rs = statement.executeQuery("select * from warps where name = '" + getWarpName() + "';");

            final boolean warpExists = rs.next();
            rs.close();
            statement.close();

            final String syntax = warpExists ? "update warps set (name, location, status, creation_date, icon_theme, animation_set, description_color, name_color, description, commands, owner, player_list, "
                    + "assistants, traffic, usage_price, enchanted_look, server_ip, likes, dislikes, voters, white_list_mode) = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) where name = '" + getWarpName() + "';"
                    : "insert into warps (name, location, status, creation_date, icon_theme, animation_set, description_color, name_color, description, commands, owner, player_list, assistants, traffic, "
                    + "usage_price, enchanted_look, server_ip, likes, dislikes, voters, white_list_mode) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
            PreparedStatement preparedStatement = getPluginInstance().getDatabaseConnection().prepareStatement(syntax);

            preparedStatement.setString(1, getWarpName());
            preparedStatement.setString(2, locationString);
            preparedStatement.setString(3, getStatus().name());
            preparedStatement.setString(4, getCreationDate());
            preparedStatement.setString(5, getIconTheme());
            preparedStatement.setString(6, getAnimationSet());
            preparedStatement.setString(7, getDescriptionColor().name());
            preparedStatement.setString(8, getDisplayNameColor().name());
            preparedStatement.setString(9, getDescription());
            preparedStatement.setString(10, commands.toString());
            preparedStatement.setString(11, owner);
            preparedStatement.setString(12, playerList.toString());
            preparedStatement.setString(13, assistants.toString());
            preparedStatement.setInt(14, getTraffic());
            preparedStatement.setDouble(15, getUsagePrice());
            preparedStatement.setInt(16, (hasIconEnchantedLook() ? 1 : 0));
            preparedStatement.setString(17, getServerIPAddress().replace("localhost", "127.0.0.1"));
            preparedStatement.setInt(18, getLikes());
            preparedStatement.setInt(19, getDislikes());
            preparedStatement.setString(20, voters.toString());
            preparedStatement.setInt(21, (isWhiteListMode() ? 1 : 0));

            preparedStatement.executeUpdate();
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
        if (!getPluginInstance().getManager().doesWarpExist(newName)) return;

        unRegister();
        final String finalWarpName = getWarpName();
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> delete(finalWarpName));
        setWarpName(newName);
        save(true);
        register();
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

    public ChatColor getDisplayNameColor() {
        return displayNameColor;
    }

    public void setDisplayNameColor(ChatColor displayNameColor) {
        this.displayNameColor = displayNameColor;
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
        String ipAddress = serverIPAddress;

        if (serverIPAddress != null) {
            final String defaultIp = getPluginInstance().getConfig().getString("mysql-connection.default-ip");
            if (defaultIp != null) ipAddress = ipAddress.replace("localhost", "127.0.0.1")
                    .replace("0.0.0.0", defaultIp);
        }

        return ipAddress;
    }

    public void setServerIPAddress(String serverIPAddress) {
        this.serverIPAddress = serverIPAddress;
    }

    public ChatColor getDescriptionColor() {
        return descriptionColor;
    }

    public void setDescriptionColor(ChatColor descriptionColor) {
        this.descriptionColor = descriptionColor;
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
}
