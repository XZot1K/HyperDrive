package xzot1k.plugins.hd.api.objects;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;

public class Warp {
    private HyperDrive pluginInstance;
    private SerializableLocation warpLocation;
    private String warpName, creationDate, iconTheme, animationSet, serverIPAddress;
    private EnumContainer.Status status;
    private ChatColor displayNameColor, descriptionColor;
    private List<String> description, commands;
    private UUID owner;
    private List<UUID> whiteList, assistants;
    private int traffic;
    private double usagePrice;
    private boolean enchantedLook;

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

        List<String> iconThemeList = getPluginInstance().getConfig().getStringList("warp-icon-section.icon-theme-list");
        setIconTheme(iconThemeList.size() > 0 ? iconThemeList.get(0) : "");
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        setAnimationSet(animationSetList.size() > 0 ? animationSetList.get(0) : "");

        setDisplayNameColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-name-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setDescriptionColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-description-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setStatus(EnumContainer.Status.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-status"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        List<String> defaultDescription = getPluginInstance().getConfig().getStringList("warp-icon-section.default-description");
        List<String> newLore = new ArrayList<>();
        for (int i = -1; ++i < defaultDescription.size(); )
            newLore.add(getPluginInstance().getManager().colorText(defaultDescription.get(i)));
        setDescription(newLore);
        setUsagePrice(0);
        setTraffic(0);
        setWhiteList(new ArrayList<>());
        setCommands(new ArrayList<>());
        setAssistants(new ArrayList<>());
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

        List<String> iconThemeList = getPluginInstance().getConfig().getStringList("warp-icon-section.icon-theme-list");
        setIconTheme(iconThemeList.size() > 0 ? iconThemeList.get(0) : "");
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        setAnimationSet(animationSetList.size() > 0 ? animationSetList.get(0) : "");

        setDisplayNameColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-name-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setDescriptionColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-description-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setStatus(EnumContainer.Status.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-status"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        List<String> defaultDescription = getPluginInstance().getConfig().getStringList("warp-icon-section.default-description");
        List<String> newLore = new ArrayList<>();
        for (int i = -1; ++i < defaultDescription.size(); )
            newLore.add(getPluginInstance().getManager().colorText(defaultDescription.get(i)));
        setDescription(newLore);
        setUsagePrice(0);
        setTraffic(0);
        setWhiteList(new ArrayList<>());
        setCommands(new ArrayList<>());
        setAssistants(new ArrayList<>());
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

        List<String> iconThemeList = getPluginInstance().getConfig().getStringList("warp-icon-section.icon-theme-list");
        setIconTheme(iconThemeList.size() > 0 ? iconThemeList.get(0) : "");
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        setAnimationSet(animationSetList.size() > 0 ? animationSetList.get(0) : "");

        setDisplayNameColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-name-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setDescriptionColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-description-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setStatus(EnumContainer.Status.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-status"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        List<String> defaultDescription = getPluginInstance().getConfig().getStringList("warp-icon-section.default-description");
        List<String> newLore = new ArrayList<>();
        for (int i = -1; ++i < defaultDescription.size(); )
            newLore.add(getPluginInstance().getManager().colorText(defaultDescription.get(i)));
        setDescription(newLore);
        setUsagePrice(0);
        setTraffic(0);
        setWhiteList(new ArrayList<>());
        setCommands(new ArrayList<>());
        setAssistants(new ArrayList<>());
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

        List<String> iconThemeList = getPluginInstance().getConfig().getStringList("warp-icon-section.icon-theme-list");
        setIconTheme(iconThemeList.size() > 0 ? iconThemeList.get(0) : "");
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        setAnimationSet(animationSetList.size() > 0 ? animationSetList.get(0) : "");

        setDisplayNameColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-name-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setDescriptionColor(ChatColor.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-description-color"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        setStatus(EnumContainer.Status.valueOf(Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.default-status"))
                .toUpperCase().replace(" ", "_").replace("-", "_")));
        List<String> defaultDescription = getPluginInstance().getConfig().getStringList("warp-icon-section.default-description");
        List<String> newLore = new ArrayList<>();
        for (int i = -1; ++i < defaultDescription.size(); )
            newLore.add(getPluginInstance().getManager().colorText(defaultDescription.get(i)));
        setDescription(newLore);
        setUsagePrice(0);
        setTraffic(0);
        setWhiteList(new ArrayList<>());
        setCommands(new ArrayList<>());
        setAssistants(new ArrayList<>());
    }

    public void register() {
        getPluginInstance().getManager().getWarpMap().put(getWarpName().toLowerCase(), this);
    }

    public void unRegister() {
        if (!getPluginInstance().getManager().getWarpMap().isEmpty())
            getPluginInstance().getManager().getWarpMap().remove(getWarpName().toLowerCase());
    }

    public void deleteSaved(boolean async) {
        if (!async) delete();
        else getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), this::delete);
    }

    private void delete() {
        if (getPluginInstance().getConnection() == null) {
            File file = new File(getPluginInstance().getDataFolder(), "/warps.yml");
            if (!file.exists()) return;

            try {
                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
                yamlConfiguration.set(getWarpName(), null);
                yamlConfiguration.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            PreparedStatement preparedStatement = getPluginInstance().getConnection().prepareStatement(
                    "delete from warps where name = '" + getWarpName() + "'");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            getPluginInstance().log(Level.WARNING, "There was an issue deleting the warp " + getWarpName() + " from the MySQL database.");
        }
    }

    public void save(boolean async, boolean useMySQL) {
        if (async)
            getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> save(useMySQL));
        else save(useMySQL);
    }

    private void save(boolean useMySQL) {
        File file = new File(getPluginInstance().getDataFolder(), "/warps.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        try {
            setWarpName(getWarpName().replace("§", "").replaceAll("[.,?:;\'\"\\\\|`~!@#$%^&*()+=/<>]", ""));
            if (!useMySQL || getPluginInstance().getConnection() == null) {
                yaml.set(getWarpName() + ".location.world", getWarpLocation().getWorldName());
                yaml.set(getWarpName() + ".location.x", getWarpLocation().getX());
                yaml.set(getWarpName() + ".location.y", getWarpLocation().getY());
                yaml.set(getWarpName() + ".location.z", getWarpLocation().getZ());
                yaml.set(getWarpName() + ".location.yaw", getWarpLocation().getYaw());
                yaml.set(getWarpName() + ".location.pitch", getWarpLocation().getPitch());

                try {
                    List<String> whiteList = new ArrayList<>();
                    for (int j = -1; ++j < getWhiteList().size(); ) {
                        UUID uuid = getWhiteList().get(j);
                        whiteList.add(uuid.toString());
                    }

                    List<String> assistants = new ArrayList<>();
                    for (int j = -1; ++j < getAssistants().size(); ) {
                        UUID uuid = getAssistants().get(j);
                        assistants.add(uuid.toString());
                    }

                    yaml.set(getWarpName() + ".traffic", getTraffic());
                    yaml.set(getWarpName() + ".status", getStatus().toString());
                    yaml.set(getWarpName() + ".creation-date", getCreationDate());
                    yaml.set(getWarpName() + ".server-ip",
                            getServerIPAddress().replace("localhost", "127.0.0.1"));
                    yaml.set(getWarpName() + ".owner", getOwner().toString());
                    yaml.set(getWarpName() + ".assistants", assistants);
                    yaml.set(getWarpName() + ".whitelist", whiteList);
                    yaml.set(getWarpName() + ".commands", getCommands());
                    yaml.set(getWarpName() + ".animation-set", getAnimationSet());

                    yaml.set(getWarpName() + ".icon.theme", getIconTheme());
                    yaml.set(getWarpName() + ".icon.description-color", getDescriptionColor().name());
                    yaml.set(getWarpName() + ".icon.name-color", getDisplayNameColor().name());
                    yaml.set(getWarpName() + ".icon.description", getDescription());
                    yaml.set(getWarpName() + ".icon.use-enchanted-look", hasIconEnchantedLook());
                    yaml.set(getWarpName() + ".icon.prices.usage", getUsagePrice());
                } catch (Exception e) {
                    e.printStackTrace();
                    getPluginInstance().log(Level.INFO, "There was an issue saving the warp " + getWarpName()
                            + "'s data aside it's location.");
                }

                return;
            }

            StringBuilder description = new StringBuilder(), commands = new StringBuilder(),
                    whitelist = new StringBuilder(), assistants = new StringBuilder();
            for (int j = -1; ++j < getDescription().size(); )
                description.append(getDescription().get(j)).append(",");
            for (int j = -1; ++j < getCommands().size(); )
                commands.append(getCommands().get(j)).append(",");
            for (int j = -1; ++j < getWhiteList().size(); )
                whitelist.append(getWhiteList().get(j).toString()).append(",");
            for (int j = -1; ++j < getAssistants().size(); )
                assistants.append(getAssistants().get(j).toString()).append(",");

            Statement statement = getPluginInstance().getConnection().createStatement();
            ResultSet rs = statement.executeQuery("select * from warps where name='" + getWarpName() + "'");
            if (rs.next()) {
                statement.executeUpdate("update warps set location = '"
                        + (getWarpLocation().getWorldName() + "," + getWarpLocation().getX() + ","
                        + getWarpLocation().getY() + "," + getWarpLocation().getZ() + ","
                        + getWarpLocation().getYaw() + "," + getWarpLocation().getPitch())
                        + "', status = '" + getStatus().name() + "', creation_date = '"
                        + getCreationDate() + "', icon_theme = '" + getIconTheme()
                        + "', animation_set = '" + getAnimationSet() + "'," + " name_color = '"
                        + getDisplayNameColor().name() + "', description = '" + description.toString()
                        + "', commands = '" + commands.toString() + "'," + " owner = '" + getOwner().toString()
                        + "', white_list = '" + whitelist.toString() + "', assistants = '" + assistants.toString()
                        + "'," + " usage_price = '" + getUsagePrice() + "', enchanted_look = '"
                        + (hasIconEnchantedLook() ? 1 : 0) + "', server_ip = '" + getServerIPAddress()
                        + "' where name = '" + getWarpName() + "';");

                rs.close();
                statement.close();
                return;
            }

            PreparedStatement preparedStatement = getPluginInstance().getConnection().prepareStatement(
                    "insert into warps (name, location, status, creation_date, icon_theme, animation_set, " +
                            "description_color, name_color, description, commands, owner, white_list, assistants, " +
                            "traffic, usage_price, enchanted_look, server_ip) "
                            + "values ('" + getWarpName() + "', '"
                            + (getWarpLocation().getWorldName() + "," + getWarpLocation().getX() + ","
                            + getWarpLocation().getY() + "," + getWarpLocation().getZ() + ","
                            + getWarpLocation().getYaw() + "," + getWarpLocation().getPitch())
                            + "', '" + getStatus().name() + "', '" + getCreationDate() + "', '"
                            + getIconTheme() + "', '" + getAnimationSet() + "', '"
                            + getDescriptionColor().name() + "', '" + getDisplayNameColor().name()
                            + "', ?, ?, '" + getOwner().toString() + "', ?, ?, " + getTraffic() + ", "
                            + getUsagePrice() + ", " + hasIconEnchantedLook() + ", '"
                            + getServerIPAddress().replace("localhost", "127.0.0.1") + "');");

            preparedStatement.setString(1, description.toString());
            preparedStatement.setString(2, commands.toString());
            preparedStatement.setString(3, whitelist.toString());
            preparedStatement.setString(4, assistants.toString());

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * '
     * Renames the warp to the given name (Handles Re-Registration).
     *
     * @param newName The new name (Does NOT filter).
     * @return Whether the rename was successful (Checks if the newName already exists).
     */
    public boolean rename(String newName) {
        if (!getPluginInstance().getManager().doesWarpExist(newName)) {
            unRegister();
            setWarpName(newName);
            register();
            return true;
        }

        return false;
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

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
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

    public List<UUID> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(List<UUID> whiteList) {
        this.whiteList = whiteList;
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
        return serverIPAddress.replace("localhost", "127.0.0.1")
                .replace("0.0.0.0", getPluginInstance().getConfig().getString("mysql-connection.default-ip"));
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

}
