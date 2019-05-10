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
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class Warp
{
    private HyperDrive pluginInstance;
    private SerializableLocation warpLocation;
    private String warpName, creationDate, iconTheme, animationSet, serverIPAddress;
    private EnumContainer.Status status;
    private ChatColor displayNameColor, descriptionColor;
    private List<String> description, commands;
    private UUID owner;
    private List<UUID> whiteList, assistants;
    private double usagePrice;
    private boolean enchantedLook;

    public Warp(String warpName, OfflinePlayer player, Location location)
    {
        setPluginInstance(HyperDrive.getPluginInstance());
        setWarpName(warpName);
        setCreationDate(getPluginInstance().getManager().getSimpleDateFormat().format(new Date()));
        setOwner(player.getUniqueId());
        setWarpLocation(location);
        setServerIPAddress(getPluginInstance().getServer().getIp());

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
        setWhiteList(new ArrayList<>());
        setCommands(new ArrayList<>());
        setAssistants(new ArrayList<>());
    }

    public Warp(String warpName, Location location)
    {
        setPluginInstance(HyperDrive.getPluginInstance());
        setWarpName(warpName);
        setCreationDate(getPluginInstance().getManager().getSimpleDateFormat().format(new Date()));
        setWarpLocation(location);

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
        setWhiteList(new ArrayList<>());
        setCommands(new ArrayList<>());
        setAssistants(new ArrayList<>());
    }

    public Warp(String warpName, OfflinePlayer player, SerializableLocation serializableLocation)
    {
        setPluginInstance(HyperDrive.getPluginInstance());
        setWarpName(warpName);
        setCreationDate(getPluginInstance().getManager().getSimpleDateFormat().format(new Date()));
        setOwner(player.getUniqueId());
        setWarpLocation(serializableLocation);

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
        setWhiteList(new ArrayList<>());
        setCommands(new ArrayList<>());
        setAssistants(new ArrayList<>());
    }

    public Warp(String warpName, SerializableLocation serializableLocation)
    {
        setPluginInstance(HyperDrive.getPluginInstance());
        setWarpName(warpName);
        setCreationDate(getPluginInstance().getManager().getSimpleDateFormat().format(new Date()));
        setWarpLocation(serializableLocation);

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
        setWhiteList(new ArrayList<>());
        setCommands(new ArrayList<>());
        setAssistants(new ArrayList<>());
    }

    public void register()
    {
        getPluginInstance().getManager().getWarpMap().put(getWarpName().toLowerCase(), this);
    }

    public void unRegister()
    {
        if (!getPluginInstance().getManager().getWarpMap().isEmpty())
            getPluginInstance().getManager().getWarpMap().remove(getWarpName().toLowerCase());
    }

    public void deleteSaved(boolean async)
    {
        if (!async) delete();
        else getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), this::delete);
    }

    private void delete()
    {
        if (getPluginInstance().getConnection() == null)
        {
            File file = new File(getPluginInstance().getDataFolder(), "/warps.yml");
            if (!file.exists()) return;

            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
            yamlConfiguration.set("warps." + getWarpName(), null);
            try { yamlConfiguration.save(file); } catch (IOException e) { e.printStackTrace(); }
            return;
        }

        try
        {
            PreparedStatement preparedStatement = getPluginInstance().getConnection().prepareStatement("delete from warps where name = '" + getWarpName() + "'");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e)
        {
            e.printStackTrace();
            getPluginInstance().log(Level.WARNING, "There was an issue deleting the warp " + getWarpName() + " from the MySQL database.");
        }
    }

    // getters & setters
    public SerializableLocation getWarpLocation()
    {
        return warpLocation;
    }

    public void setWarpLocation(Location warpLocation)
    {
        this.warpLocation = new SerializableLocation(warpLocation);
    }

    public void setWarpLocation(SerializableLocation warpLocation)
    {
        this.warpLocation = warpLocation;
    }

    public String getWarpName()
    {
        return warpName;
    }

    public void setWarpName(String warpName)
    {
        this.warpName = warpName;
    }

    private HyperDrive getPluginInstance()
    {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance)
    {
        this.pluginInstance = pluginInstance;
    }

    public ChatColor getDisplayNameColor()
    {
        return displayNameColor;
    }

    public void setDisplayNameColor(ChatColor displayNameColor)
    {
        this.displayNameColor = displayNameColor;
    }

    public List<String> getDescription()
    {
        return description;
    }

    public void setDescription(List<String> description)
    {
        this.description = description;
    }

    public EnumContainer.Status getStatus()
    {
        return status;
    }

    public void setStatus(EnumContainer.Status status)
    {
        this.status = status;
    }

    public UUID getOwner()
    {
        return owner;
    }

    public void setOwner(UUID owner)
    {
        this.owner = owner;
    }

    public double getUsagePrice()
    {
        return usagePrice;
    }

    public void setUsagePrice(double usagePrice)
    {
        this.usagePrice = usagePrice;
    }

    public List<UUID> getWhiteList()
    {
        return whiteList;
    }

    public void setWhiteList(List<UUID> whiteList)
    {
        this.whiteList = whiteList;
    }

    public boolean hasIconEnchantedLook()
    {
        return enchantedLook;
    }

    public void setIconEnchantedLook(boolean enchantedLook)
    {
        this.enchantedLook = enchantedLook;
    }

    public List<String> getCommands()
    {
        return commands;
    }

    public void setCommands(List<String> commands)
    {
        this.commands = commands;
    }

    public List<UUID> getAssistants()
    {
        return assistants;
    }

    public void setAssistants(List<UUID> assistants)
    {
        this.assistants = assistants;
    }

    public String getCreationDate()
    {
        return creationDate;
    }

    public void setCreationDate(String creationDate)
    {
        this.creationDate = creationDate;
    }

    public String getIconTheme()
    {
        return iconTheme;
    }

    public void setIconTheme(String iconTheme)
    {
        this.iconTheme = iconTheme;
    }

    public String getAnimationSet()
    {
        return animationSet;
    }

    public void setAnimationSet(String animationSet)
    {
        this.animationSet = animationSet;
    }

    public String getServerIPAddress()
    {
        return serverIPAddress;
    }

    public void setServerIPAddress(String serverIPAddress)
    {
        this.serverIPAddress = serverIPAddress;
    }

    public ChatColor getDescriptionColor()
    {
        return descriptionColor;
    }

    public void setDescriptionColor(ChatColor descriptionColor)
    {
        this.descriptionColor = descriptionColor;
    }
}
