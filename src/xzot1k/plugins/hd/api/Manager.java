package xzot1k.plugins.hd.api;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.objects.Warp;
import xzot1k.plugins.hd.core.internals.Paging;
import xzot1k.plugins.hd.core.objects.json.JSONExtra;
import xzot1k.plugins.hd.core.objects.json.JSONMessage;
import xzot1k.plugins.hd.core.packets.actionbars.ActionBarHandler;
import xzot1k.plugins.hd.core.packets.actionbars.versions.*;
import xzot1k.plugins.hd.core.packets.jsonmsgs.JSONHandler;
import xzot1k.plugins.hd.core.packets.jsonmsgs.versions.*;
import xzot1k.plugins.hd.core.packets.particles.ParticleHandler;
import xzot1k.plugins.hd.core.packets.particles.versions.*;
import xzot1k.plugins.hd.core.packets.titles.TitleHandler;
import xzot1k.plugins.hd.core.packets.titles.versions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

public class Manager
{
    private HyperDrive pluginInstance;
    private Paging paging;
    private SimpleDateFormat simpleDateFormat;

    private ParticleHandler particleHandler;
    private JSONHandler jsonHandler;
    private TitleHandler titleHandler;
    private ActionBarHandler actionBarHandler;

    private HashMap<String, Warp> warpMap;
    private HashMap<UUID, Double> lastTransactionMap;
    private HashMap<UUID, HashMap<String, Long>> cooldownMap;
    private HashMap<UUID, HashMap<String, String>> chatInteractionMap;
    private HashMap<UUID, List<UUID>> groupMap;

    public Manager(HyperDrive pluginInstance)
    {
        setPluginInstance(pluginInstance);
        setSimpleDateFormat(new SimpleDateFormat(Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.date-format"))));
        setPaging(new Paging(getPluginInstance()));
        setCooldownMap(new HashMap<>());
        setWarpMap(new HashMap<>());
        setChatInteractionMap(new HashMap<>());
        setLastTransactionMap(new HashMap<>());
        setGroupMap(new HashMap<>());

        setupPackets();
    }

    // general stuff
    private void setupPackets()
    {
        boolean succeeded = true;
        long startTime = System.currentTimeMillis();
        switch (getPluginInstance().getServerVersion())
        {
            case "v1_14_R1":
                setParticleHandler(new Particle_Latest());
                setJsonHandler(new JSONHandler1_14R1());
                setTitleHandler(new Titles_Latest(getPluginInstance()));
                setActionBarHandler(new ABH_1_14R1());
                break;
            case "v1_13_R2":
                setParticleHandler(new Particle_Latest());
                setJsonHandler(new JSONHandler1_13R2());
                setTitleHandler(new Titles_Latest(getPluginInstance()));
                setActionBarHandler(new ABH_1_13R2());
                break;
            case "v1_13_R1":
                setParticleHandler(new Particle_Latest());
                setJsonHandler(new JSONHandler1_13R1());
                setTitleHandler(new Titles_Latest(getPluginInstance()));
                setActionBarHandler(new ABH_1_13R1());
                break;
            case "v1_12_R1":
                setParticleHandler(new Particle1_12R1());
                setJsonHandler(new JSONHandler1_12R1());
                setTitleHandler(new Titles1_12R1(getPluginInstance()));
                setActionBarHandler(new ABH_1_12R1());
                break;
            case "v1_11_R1":
                setParticleHandler(new Particle1_11R1());
                setJsonHandler(new JSONHandler1_11R1());
                setTitleHandler(new Titles1_11R1(getPluginInstance()));
                setActionBarHandler(new ABH_1_11R1());
                break;
            case "v1_10_R1":
                setParticleHandler(new Particle1_10R1());
                setJsonHandler(new JSONHandler1_10R1());
                setTitleHandler(new Titles1_10R1(getPluginInstance()));
                setActionBarHandler(new ABH_1_10R1());
                break;
            case "v1_9_R2":
                setParticleHandler(new Particle1_9R2());
                setJsonHandler(new JSONHandler1_9R2());
                setTitleHandler(new Titles1_9R2(getPluginInstance()));
                setActionBarHandler(new ABH_1_9R2());
                break;
            case "v1_9_R1":
                setParticleHandler(new Particle1_9R1());
                setJsonHandler(new JSONHandler1_9R1());
                setTitleHandler(new Titles1_9R1(getPluginInstance()));
                setActionBarHandler(new ABH_1_9R1());
                break;
            case "v1_8_R3":
                setParticleHandler(new Particle1_8R3());
                setJsonHandler(new JSONHandler1_8R3());
                setTitleHandler(new Titles1_8R3(getPluginInstance()));
                setActionBarHandler(new ABH_1_8R3());
                break;
            case "v1_8_R2":
                setParticleHandler(new Particle1_8R2());
                setJsonHandler(new JSONHandler1_8R2());
                setTitleHandler(new Titles1_8R2(getPluginInstance()));
                setActionBarHandler(new ABH_1_8R2());
                break;
            case "v1_8_R1":
                setParticleHandler(new Particle1_8R1());
                setJsonHandler(new JSONHandler1_8R1());
                setTitleHandler(new Titles1_8R1(getPluginInstance()));
                setActionBarHandler(new ABH_1_8R1());
                break;
            default:
                succeeded = false;
                break;
        }

        if (succeeded)
            getPluginInstance().log(Level.INFO, getPluginInstance().getServerVersion() + " packets were successfully setup! (Took " + (System.currentTimeMillis() - startTime) + "ms)");
        else
            getPluginInstance().log(Level.WARNING, "Your version is not supported by HyperDrive's packets. Expect errors when attempting to use anything packet " +
                    "related. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    public boolean isNumeric(String string)
    {
        return string.matches("-?\\d+(\\.\\d+)?");
    }

    public List<String> wrapString(String text, int lineSize)
    {
        List<String> result = new ArrayList<>();
        char[] chars = text.toCharArray();
        int counter = 0;
        StringBuilder tempLine = new StringBuilder();

        for (int i = -1; ++i < chars.length; )
        {
            if (counter <= lineSize) tempLine.append(chars[i]);
            else
            {
                tempLine.append(chars[i]);
                result.add(tempLine.toString());
                tempLine.setLength(0);
                counter = 0;
                continue;
            }

            counter += 1;
        }

        if (tempLine.length() > 0) result.add(tempLine.toString());
        return result;
    }

    public String colorText(String text)
    {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public boolean isChatColor(String text)
    {
        for (int i = -1; ++i < ChatColor.values().length; )
            if (ChatColor.values()[i].name().equalsIgnoreCase(text)) return true;
        return false;
    }

    public void sendCustomMessage(String message, Player player)
    {
        if (message != null && !message.equalsIgnoreCase(""))
        {
            String prefix = getPluginInstance().getConfig().getString("language-section.prefix");
            if (message.contains("<") && message.contains(">"))
            {
                message = prefix + message;
                String jsonFormat = StringUtils.substringBetween(message, "<", ">");
                String[] jsonFormatArgs = jsonFormat.split(":");

                String splitMessage = message.replace("<" + jsonFormat + ">", "_.SPLIT._");
                String[] splitMessageArgs = splitMessage.split("_.SPLIT._");
                JSONMessage jm1 = new JSONMessage(getPluginInstance(), prefix + splitMessageArgs[0]);
                JSONExtra je1 = new JSONExtra(getPluginInstance(), jsonFormatArgs[0]);
                if (jsonFormatArgs.length >= 2)
                    je1.setClickEvent(EnumContainer.JSONClickAction.RUN_COMMAND, jsonFormatArgs[1]);
                if (jsonFormatArgs.length >= 3)
                    je1.setHoverEvent(EnumContainer.JSONHoverAction.SHOW_TEXT, jsonFormatArgs[2]);
                jm1.addExtra(je1);

                if (splitMessageArgs.length >= 2)
                {
                    JSONExtra je2 = new JSONExtra(getPluginInstance(), splitMessageArgs[1]);
                    jm1.addExtra(je2);
                }

                jm1.sendJSONToPlayer(player);
                return;
            }

            player.sendMessage(colorText(prefix + message));
        }
    }

    public void sendActionBar(Player player, String message)
    {
        if (message != null && !message.equalsIgnoreCase(""))
            getActionBarHandler().sendActionBar(player, message);
    }

    public void sendTitle(Player player, String title, String subTitle, int fadeIn, int displayTime, int fadeOut)
    {
        getTitleHandler().sendTitle(player, colorText(title), colorText(subTitle), fadeIn, displayTime, fadeOut);
    }

    public void displayParticle(Location location, String particleEffect)
    {
        getParticleHandler().displayParticle(particleEffect, location, 0, 0, 0, 0, 1);
    }

    public List<String> getColorNames()
    {
        List<String> colorNames = new ArrayList<>();
        for (int i = -1; ++i < ChatColor.values().length; )
        {
            ChatColor color = ChatColor.values()[i];
            if (color == ChatColor.BOLD || color == ChatColor.ITALIC || color == ChatColor.UNDERLINE || color == ChatColor.STRIKETHROUGH
                    || color == ChatColor.MAGIC || color == ChatColor.RESET)
                continue;
            colorNames.add(WordUtils.capitalize(color.name().toLowerCase().replace("_", " ").replace("-", " ")));
        }

        return colorNames;
    }

    public List<UUID> getPlayerUUIDs()
    {
        List<UUID> tempList = new ArrayList<>();
        List<Player> playerList = new ArrayList<>(getPluginInstance().getServer().getOnlinePlayers());
        for (int i = -1; ++i < playerList.size(); )
        {
            Player player = playerList.get(i);
            tempList.add(player.getUniqueId());
        }

        return tempList;
    }

    public double getLastTransactionAmount(OfflinePlayer player)
    {
        if (!getLastTransactionMap().isEmpty() && getLastTransactionMap().containsKey(player.getUniqueId()))
            return getLastTransactionMap().get(player.getUniqueId());
        else return 0;
    }

    public void updateLastTransactionAmount(OfflinePlayer player, double amount)
    {
        if (!getLastTransactionMap().isEmpty() && getLastTransactionMap().containsKey(player.getUniqueId()))
            getLastTransactionMap().put(player.getUniqueId(), getLastTransactionMap().get(player.getUniqueId()) + amount);
        else getLastTransactionMap().put(player.getUniqueId(), amount);
    }

    public void returnLastTransactionAmount(OfflinePlayer player)
    {
        if (getPluginInstance().getConfig().getBoolean("general-section.use-vault"))
        {
            double lastAmount = getLastTransactionAmount(player);
            if (lastAmount > 0)
            {
                getPluginInstance().getVaultEconomy().depositPlayer(player, lastAmount);
                if (player.isOnline())
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.last-transaction-return"))
                            .replace("{amount}", String.valueOf(lastAmount)), player.getPlayer());
            }
        }
    }

    public String getProgressionBar(int f1, int f2, int segments)
    {
        StringBuilder bar = new StringBuilder();
        int fractionValue = (int) (((double) f1) / ((double) f2) * segments);

        for (int i = -1; ++i < segments; )
        {
            if (fractionValue > 0)
            {
                bar.append("&a█");
                fractionValue -= 1;
            } else bar.append("&c█");
        }

        return bar.toString();
    }

    // cross-server stuff
    public void teleportCrossServer(Player player, String serverName, Location location)
    {
        if (getPluginInstance().getConnection() == null) return;

        try
        {
            getPluginInstance().getServer().getMessenger().registerOutgoingPluginChannel(pluginInstance, "BungeeCord");
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteArray);
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(pluginInstance, "BungeeCord", byteArray.toByteArray());
        } catch (Exception ex)
        {
            ex.printStackTrace();
            getPluginInstance().log(Level.WARNING, "There seems to have been an issue when switching the player to the '" + serverName + "' server.");
            return;
        }

        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () ->
        {
            try
            {
                Statement statement = getPluginInstance().getConnection().createStatement();

                ResultSet rs = statement.executeQuery("select * from transfer where player_uuid = '" + player.getUniqueId().toString() + "'");
                if (rs.next())
                {
                    statement.executeUpdate("update transfer set location = '" + (Objects.requireNonNull(location.getWorld()).getName() + "," + location.getX() + ","
                            + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch()) + "' where player_uuid = '"
                            + player.getUniqueId().toString() + "';");
                    rs.close();
                    statement.close();
                    getPluginInstance().log(Level.WARNING, "Cross-Server transfer initiated (" + player.getName() + "_" + player.getUniqueId().toString() + " / " + serverName + " / World: "
                            + location.getWorld().getName() + ", X: " + location.getBlockX() + ", Y: " + location.getBlockY() + ", Z: " + location.getBlockZ() + ")");
                    return;
                }

                PreparedStatement preparedStatement = getPluginInstance().getConnection().prepareStatement("insert into transfer (player_uuid, location) values ('"
                        + player.getUniqueId().toString() + "', '" + (Objects.requireNonNull(location.getWorld()).getName() + "," + location.getX() + "," + location.getY() + ","
                        + location.getZ() + "," + location.getYaw() + "," + location.getPitch()) + "');");
                preparedStatement.executeUpdate();
                preparedStatement.close();
                getPluginInstance().log(Level.WARNING, "Cross-Server transfer initiated (" + player.getName() + "_" + player.getUniqueId().toString() + " / " + serverName + " / World: "
                        + location.getWorld().getName() + ", X: " + location.getBlockX() + ", Y: " + location.getBlockY() + ", Z: " + location.getBlockZ() + ")");
            } catch (SQLException e)
            {
                e.printStackTrace();
                getPluginInstance().log(Level.WARNING, "There seems to have been an issue communicating with the MySQL database.");
                getPluginInstance().log(Level.WARNING, "Cross-Server initiation failed for the player '" + player.getName() + "_" + player.getUniqueId().toString() + "'. They have been sent to " +
                        "the '" + serverName + "' server, but the location was unable to be processed.");
            }
        });
    }

    // cooldown stuff
    public void updateCooldown(OfflinePlayer player, String cooldownId)
    {
        if (!getCooldownMap().isEmpty() && getCooldownMap().containsKey(player.getUniqueId()))
        {
            HashMap<String, Long> playerCooldownMap = getCooldownMap().get(player.getUniqueId());
            if (playerCooldownMap != null)
                playerCooldownMap.put(cooldownId.toUpperCase(), System.currentTimeMillis());
        }

        getCooldownMap().put(player.getUniqueId(), new HashMap<>());
        getCooldownMap().get(player.getUniqueId()).put(cooldownId, System.currentTimeMillis());
    }

    public long getCooldownDuration(OfflinePlayer player, String cooldownId, int cooldownDuration)
    {
        if (!getCooldownMap().isEmpty() && getCooldownMap().containsKey(player.getUniqueId()))
        {
            HashMap<String, Long> playerCooldownMap = getCooldownMap().get(player.getUniqueId());
            if (playerCooldownMap != null && !playerCooldownMap.isEmpty() && playerCooldownMap.containsKey(cooldownId))
                return ((playerCooldownMap.get(cooldownId) / 1000) + cooldownDuration)
                        - (System.currentTimeMillis() / 1000);
        }

        return 0;
    }

    // inventory stuff
    public String getNextIconTheme(Warp warp)
    {
        List<String> themeList = getPluginInstance().getConfig().getStringList("warp-icon-section.icon-theme-list");
        int currentIndex = 0;
        for (int i = -1; ++i < themeList.size(); )
            if (themeList.get(i).equalsIgnoreCase(warp.getIconTheme()))
            {
                currentIndex = i;
                break;
            }

        int nextTheme = (currentIndex + 1);
        return nextTheme < themeList.size() ? themeList.get(nextTheme) : themeList.get(0);
    }

    @SuppressWarnings("deprecation")
    private ItemStack buildItem(Material material, int durability, String displayName, List<String> lore, int amount)
    {
        ItemStack itemStack = new ItemStack(material, amount);
        itemStack.setDurability((short) durability);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null)
        {
            itemMeta.setDisplayName(colorText(displayName));
            itemMeta.setLore(lore);
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    @SuppressWarnings("deprecation")
    private ItemStack getPlayerHead(String playerName, String displayName, List<String> lore, int amount)
    {
        boolean isNew = getPluginInstance().getServerVersion().startsWith("v1_13") || getPluginInstance().getServerVersion().startsWith("v1_14");
        ItemStack itemStack;

        if (isNew)
        {
            itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial("PLAYER_HEAD")), amount);
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            if (skullMeta != null)
            {
                skullMeta.setOwningPlayer(getPluginInstance().getServer().getOfflinePlayer(playerName));
                skullMeta.setDisplayName(colorText(displayName));
                skullMeta.setLore(lore);
                itemStack.setItemMeta(skullMeta);
            }
        } else
        {
            itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial("SKULL_ITEM")), amount, (short) org.bukkit.SkullType.PLAYER.ordinal());
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            if (skullMeta != null)
            {
                skullMeta.setOwner(playerName);
                skullMeta.setDisplayName(colorText(displayName));
                skullMeta.setLore(lore);
                itemStack.setItemMeta(skullMeta);
            }
        }

        return itemStack;
    }

    public ItemStack getPlayerSelectionHead(OfflinePlayer player, boolean isSelected)
    {
        boolean isNew = getPluginInstance().getServerVersion().startsWith("v1_13") || getPluginInstance().getServerVersion().startsWith("v1_14");
        ItemStack itemStack;

        if (isNew)
        {
            itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial("PLAYER_HEAD")), 1);
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            if (skullMeta != null)
            {
                skullMeta.setOwningPlayer(player);
                skullMeta.setDisplayName(colorText(isSelected ? getPluginInstance().getConfig().getString("ps-menu-section.selected-player-head.display-name") + player.getName()
                        : getPluginInstance().getConfig().getString("ps-menu-section.unselected-player-head.display-name") + player.getName()));

                List<String> lore = isSelected ? getPluginInstance().getConfig().getStringList("ps-menu-section.selected-player-head.lore")
                        : getPluginInstance().getConfig().getStringList("ps-menu-section.unselected-player-head.lore"), newLore = new ArrayList<>();
                for (int i = -1; ++i < lore.size(); )
                    newLore.add(getPluginInstance().getManager().colorText(lore.get(i)));

                skullMeta.setLore(newLore);
                itemStack.setItemMeta(skullMeta);
            }
        } else
        {
            itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial("SKULL_ITEM")), 1, (short) 3);
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            if (skullMeta != null)
            {
                skullMeta.setOwner(player.getName());
                skullMeta.setDisplayName(colorText(isSelected ? getPluginInstance().getConfig().getString("ps-menu-section.selected-player-head.display-name") + player.getName()
                        : getPluginInstance().getConfig().getString("ps-menu-section.unselected-player-head.display-name") + player.getName()));

                List<String> lore = isSelected ? getPluginInstance().getConfig().getStringList("ps-menu-section.selected-player-head.lore")
                        : getPluginInstance().getConfig().getStringList("ps-menu-section.unselected-player-head.lore"), newLore = new ArrayList<>();
                for (int i = -1; ++i < lore.size(); )
                    newLore.add(getPluginInstance().getManager().colorText(lore.get(i)));

                skullMeta.setLore(newLore);
                itemStack.setItemMeta(skullMeta);
            }
        }

        return itemStack;
    }

    public String getMenuId(String inventoryName)
    {
        ConfigurationSection configurationSection = getPluginInstance().getConfig().getConfigurationSection("custom-menus-section");
        if (configurationSection != null)
        {
            List<String> menuIds = new ArrayList<>(configurationSection.getKeys(false));
            for (int i = -1; ++i < menuIds.size(); )
            {
                String menuId = menuIds.get(i), inventoryTitle = colorText(getPluginInstance().getConfig().getString("custom-menus-section." + menuId + ".title"));
                if (inventoryTitle.equalsIgnoreCase(inventoryName)) return menuId;
            }
        }

        return null;
    }

    public String getIdFromSlot(String menuPath, int slot)
    {
        List<String> configurationSection = new ArrayList<>(
                Objects.requireNonNull(getPluginInstance().getConfig().getConfigurationSection(menuPath + ".items")).getKeys(false));
        for (int i = -1; ++i < configurationSection.size(); )
        {
            String itemId = configurationSection.get(i);
            int itemSlot = getPluginInstance().getConfig().getInt(menuPath + ".items." + itemId + ".slot");
            if (itemSlot == slot)
                return itemId;
        }

        return null;
    }

    public String getFilterStatusFromItem(ItemStack itemStack, String menuPath, String itemId)
    {
        if (itemStack != null && itemStack.hasItemMeta() && Objects.requireNonNull(itemStack.getItemMeta()).hasDisplayName())
        {
            String displayName = getPluginInstance().getConfig().getString(menuPath + ".items." + itemId + ".display-name");
            if ((displayName != null) && displayName.toLowerCase().contains("{current-status}"))
            {
                String[] displayNameArgs = displayName.replace("{current-status}", "%%/split_point/%%")
                        .split("%%/split_point/%%");
                String pulledStatus;
                switch (displayNameArgs.length)
                {
                    case 1:
                        pulledStatus = itemStack.getItemMeta().getDisplayName().replace(colorText(displayNameArgs[0]), "");
                        break;
                    case 2:
                        pulledStatus = itemStack.getItemMeta().getDisplayName().replace(colorText(displayNameArgs[0]), "")
                                .replace(colorText(displayNameArgs[1]), "");
                        break;
                    default:
                        pulledStatus = itemStack.getItemMeta().getDisplayName();
                        break;
                }

                if (pulledStatus.equals(getPluginInstance().getConfig().getString("list-menu-section.public-status-format")))
                    return EnumContainer.Status.PUBLIC.name();
                else if (pulledStatus.equals(getPluginInstance().getConfig().getString("list-menu-section.private-status-format")))
                    return EnumContainer.Status.PRIVATE.name();
                else if (pulledStatus.equals(getPluginInstance().getConfig().getString("list-menu-section.admin-status-format")))
                    return EnumContainer.Status.ADMIN.name();
                else return ChatColor.stripColor(pulledStatus);
            }
        }

        return null;
    }

    public String getCurrentFilterStatus(String menuPath, Inventory inventory)
    {
        List<String> itemIds = new ArrayList<>(Objects.requireNonNull(getPluginInstance().getConfig().getConfigurationSection(menuPath + ".items")).getKeys(false));
        for (int i = -1; ++i < itemIds.size(); )
        {
            String itemId = itemIds.get(i), clickAction = getPluginInstance().getConfig().getString(menuPath + ".items." + itemId + ".click-action");
            if (clickAction != null && clickAction.equalsIgnoreCase("filter-switch"))
            {
                ItemStack itemStack = inventory.getItem(getPluginInstance().getConfig().getInt(menuPath + ".items." + itemId + ".slot"));
                if (itemStack != null && itemStack.hasItemMeta() && Objects.requireNonNull(itemStack.getItemMeta()).hasDisplayName())
                {
                    String displayName = getPluginInstance().getConfig().getString(menuPath + ".items." + itemId + ".display-name");
                    if (displayName != null && displayName.toLowerCase().contains("{current-status}"))
                    {
                        String[] displayNameArgs = displayName.replace("{current-status}", "%%/split_point/%%").split("%%/split_point/%%");
                        String pulledStatus;
                        switch (displayNameArgs.length)
                        {
                            case 1:
                                pulledStatus = itemStack.getItemMeta().getDisplayName().replace(colorText(displayNameArgs[0]), "");
                                break;
                            case 2:
                                pulledStatus = itemStack.getItemMeta().getDisplayName().replace(colorText(displayNameArgs[0]), "").replace(colorText(displayNameArgs[1]), "");
                                break;
                            default:
                                pulledStatus = itemStack.getItemMeta().getDisplayName();
                                break;
                        }

                        return ChatColor.stripColor(pulledStatus);
                    }
                }
            }
        }

        return EnumContainer.Status.PUBLIC.name();
    }

    public ItemStack buildWarpIcon(OfflinePlayer player, Warp warp)
    {
        String publicFormat = getPluginInstance().getConfig().getString("list-menu-section.public-status-format"),
                privateFormat = getPluginInstance().getConfig().getString("list-menu-section.private-status-format"),
                adminFormat = getPluginInstance().getConfig().getString("list-menu-section.admin-status-format");

        String[] eventPlaceholders = {"{is-owner}", "{has-access}", "{no-access}", "{can-edit}", "{is-private}", "{is-public}", "{is-admin}"};
        List<String> iconLoreFormat = getPluginInstance().getConfig().getStringList("warp-icon-section.list-lore-format"),
                newLore = new ArrayList<>(), warpDescription = warp.getDescription();

        for (int i = -1; ++i < iconLoreFormat.size(); )
        {
            String formatLine = iconLoreFormat.get(i), foundEventPlaceholder = null;

            if (formatLine.equalsIgnoreCase("{description}"))
            {
                if (warpDescription != null && !warpDescription.isEmpty())
                {
                    for (int j = -1; ++j < warpDescription.size(); )
                        newLore.add(warp.getDescriptionColor() + ChatColor.stripColor(warpDescription.get(j)));
                }

                continue;
            }

            for (int j = -1; ++j < eventPlaceholders.length; )
            {
                String eventPlaceholder = eventPlaceholders[j];
                if (formatLine.toLowerCase().contains(eventPlaceholder.toLowerCase()))
                {
                    foundEventPlaceholder = eventPlaceholder;
                    formatLine = formatLine.replace(eventPlaceholder, "");
                    break;
                }
            }

            String statusName;

            switch (warp.getStatus())
            {
                case PRIVATE:
                    statusName = privateFormat;
                    break;
                case ADMIN:
                    statusName = adminFormat;
                    break;
                default:
                    statusName = publicFormat;
                    break;
            }

            String furtherFormattedLine;
            OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(warp.getOwner());

            furtherFormattedLine = formatLine.replace("{creation-date}", warp.getCreationDate()).replace("{assistant-count}", String.valueOf(warp.getAssistants().size()))
                    .replace("{usage-price}", String.valueOf(warp.getUsagePrice())).replace("{whitelist-count}", String.valueOf(warp.getWhiteList().size()))
                    .replace("{status}", Objects.requireNonNull(statusName))
                    .replace("{theme}", warp.getIconTheme() != null && warp.getIconTheme().contains(":") ? warp.getIconTheme().split(":")[0] : "")
                    .replace("{animation-set}", warp.getAnimationSet() != null && warp.getAnimationSet().contains(":") ? warp.getAnimationSet().split(":")[0] : "")
                    .replace("{player}", Objects.requireNonNull(player.getName()))
                    .replace("{traffic}", String.valueOf(warp.getTraffic()))
                    .replace("{owner}", offlinePlayer.getName() == null ? Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.invalid-retrieval"))
                            : Objects.requireNonNull(offlinePlayer.getName()));

            if (foundEventPlaceholder != null)
            {
                switch (foundEventPlaceholder)
                {
                    case "{is-owner}":
                        if (warp.getOwner().toString().equals(player.getUniqueId().toString()))
                            newLore.add(colorText(furtherFormattedLine));
                        break;
                    case "{can-edit}":
                        if (warp.getOwner().toString().equals(player.getUniqueId().toString()) || warp.getAssistants().contains(player.getUniqueId()))
                            newLore.add(colorText(furtherFormattedLine));
                        break;
                    case "{has-access}":
                        if (warp.getStatus() == EnumContainer.Status.PUBLIC || (warp.getOwner().toString().equals(player.getUniqueId().toString())
                                || warp.getAssistants().contains(player.getUniqueId()) || warp.getWhiteList().contains(player.getUniqueId()))
                                || (Objects.requireNonNull(player.getPlayer()).hasPermission("hyperdrive.warps." + warp.getWarpName())
                                || player.getPlayer().hasPermission("hyperdrive.warps.*")))
                            newLore.add(colorText(furtherFormattedLine));
                        break;
                    case "{no-access}":
                        if (warp.getStatus() != EnumContainer.Status.PUBLIC && !warp.getOwner().toString().equals(player.getUniqueId().toString()) && !warp.getAssistants().contains(player.getUniqueId())
                                && !warp.getWhiteList().contains(player.getUniqueId()) && (!Objects.requireNonNull(player.getPlayer()).hasPermission("hyperdrive.warps." + warp.getWarpName())
                                && !player.getPlayer().hasPermission("hyperdrive.warps.*")))
                            newLore.add(colorText(furtherFormattedLine));
                        break;
                    case "{is-private}":
                        if (warp.getStatus() == EnumContainer.Status.PRIVATE)
                            newLore.add(colorText(furtherFormattedLine));
                        break;
                    case "{is-public}":
                        if (warp.getStatus() == EnumContainer.Status.PUBLIC)
                            newLore.add(colorText(furtherFormattedLine));
                        break;
                    case "{is-admin}":
                        if (warp.getStatus() == EnumContainer.Status.ADMIN)
                            newLore.add(colorText(furtherFormattedLine));
                        break;
                    default:
                        break;
                }
            } else newLore.add(colorText(furtherFormattedLine));
        }

        if (warp.getIconTheme() != null && warp.getIconTheme().contains(":"))
        {
            String[] themeArgs = warp.getIconTheme().split(":");
            Material material = Material.getMaterial(themeArgs[1].toUpperCase().replace(" ", "_").replace("-", "_"));
            int durability = Integer.parseInt(themeArgs[2]), amount = Integer.parseInt(themeArgs[3]);

            if (material != null && (material.name().equalsIgnoreCase("SKULL_ITEM") || material.name().equalsIgnoreCase("PLAYER_HEAD")))
            {
                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(warp.getOwner());
                ItemStack item = getPlayerHead(offlinePlayer.getName(), warp.getDisplayNameColor() + warp.getWarpName(), newLore, amount);
                ItemMeta itemMeta = item.getItemMeta();
                if (warp.hasIconEnchantedLook() && itemMeta != null)
                {
                    itemMeta.addEnchant(Enchantment.DURABILITY, 10, true);
                    itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(itemMeta);
                }
                return item;
            } else
            {
                ItemStack item = buildItem(material, durability, warp.getDisplayNameColor() + warp.getWarpName(), newLore, amount);
                ItemMeta itemMeta = item.getItemMeta();
                if (warp.hasIconEnchantedLook() && itemMeta != null)
                {
                    itemMeta.addEnchant(Enchantment.DURABILITY, 10, true);
                    itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(itemMeta);
                }

                return item;
            }
        } else
        {
            OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(warp.getOwner());
            ItemStack item = getPlayerHead(offlinePlayer.getName(), warp.getDisplayNameColor() + warp.getWarpName(), newLore, 1);
            ItemMeta itemMeta = item.getItemMeta();
            if (warp.hasIconEnchantedLook() && itemMeta != null)
            {
                itemMeta.addEnchant(Enchantment.DURABILITY, 10, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(itemMeta);
            }

            return item;
        }
    }

    public ItemStack buildItemFromId(OfflinePlayer player, String currentFilterStatus, String menuPath, String itemId)
    {
        boolean hasPreviousPage = getPaging().hasPreviousWarpPage(player), hasNextPage = getPaging().hasNextWarpPage(player);
        int currentPage = getPaging().getCurrentPage(player);
        List<String> itemIds = new ArrayList<>(Objects.requireNonNull(getPluginInstance().getConfig().getConfigurationSection(menuPath + ".items")).getKeys(false));

        String ownFormat = getPluginInstance().getConfig().getString("list-menu-section.own-status-format"),
                everythingFormat = getPluginInstance().getConfig().getString("list-menu-section.everything-status-format"),
                publicFormat = getPluginInstance().getConfig().getString("list-menu-section.public-status-format"),
                privateFormat = getPluginInstance().getConfig().getString("list-menu-section.private-status-format"),
                adminFormat = getPluginInstance().getConfig().getString("list-menu-section.admin-status-format");

        int index = 0;
        if (currentFilterStatus.replace("-", " ").replace("_", " ").equalsIgnoreCase(privateFormat)
                || currentFilterStatus.equalsIgnoreCase(EnumContainer.Status.PRIVATE.name()))
            index = 1;
        else if (currentFilterStatus.replace("-", " ").replace("_", " ").equalsIgnoreCase(adminFormat)
                || currentFilterStatus.equalsIgnoreCase(EnumContainer.Status.ADMIN.name()))
            index = 2;
        else if (currentFilterStatus.replace("-", " ").replace("_", " ").equalsIgnoreCase(ownFormat))
            index = 3;
        else if (currentFilterStatus.replace("-", " ").replace("_", " ").equalsIgnoreCase(everythingFormat))
            index = 4;

        String statusFormat;
        switch (index)
        {
            case 1:
                statusFormat = privateFormat;
                break;
            case 2:
                statusFormat = adminFormat;
                break;
            case 3:
                statusFormat = ownFormat;
                break;
            case 4:
                statusFormat = everythingFormat;
                break;
            default:
                statusFormat = publicFormat;
                break;
        }

        for (int i = -1; ++i < itemIds.size(); )
        {
            if (itemIds.get(i).equalsIgnoreCase(itemId))
            {
                boolean usePlayerHead = getPluginInstance().getConfig().getBoolean(menuPath + ".items." + itemId + ".use-player-head");
                if (usePlayerHead)
                {
                    String displayName = Objects.requireNonNull(getPluginInstance().getConfig().getString(menuPath + ".items." + itemId + ".display-name"))
                            .replace("{current-page}", String.valueOf(currentPage))
                            .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                            .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")
                            .replace("{next-status}", Objects.requireNonNull(statusFormat))
                            .replace("{current-status}", statusFormat);

                    List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getConfig().getStringList(menuPath + ".items." + itemId + ".lore");
                    for (int j = -1; ++j < lore.size(); )
                        newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                                .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                                .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")
                                .replace("{next-status}", statusFormat)
                                .replace("{current-status}", statusFormat)));
                    return getPlayerHead(getPluginInstance().getConfig().getString(menuPath + ".items." + itemId + ".player-head-name"),
                            displayName, newLore, getPluginInstance().getConfig().getInt(menuPath + ".items." + itemId + ".amount"));
                } else
                {
                    String displayName = Objects.requireNonNull(getPluginInstance().getConfig().getString(menuPath + ".items." + itemId + ".display-name"))
                            .replace("{current-page}", String.valueOf(currentPage))
                            .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                            .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")
                            .replace("{next-status}", Objects.requireNonNull(statusFormat))
                            .replace("{current-status}", statusFormat);
                    List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getConfig().getStringList(menuPath + ".items." + itemId + ".lore");
                    for (int j = -1; ++j < lore.size(); )
                        newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                                .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                                .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")
                                .replace("{next-status}", statusFormat)
                                .replace("{current-status}", statusFormat)));
                    Material material = Material.getMaterial(Objects.requireNonNull(getPluginInstance().getConfig().getString(menuPath + ".items." + itemId + ".material"))
                            .toUpperCase().replace(" ", "_").replace("-", "_"));
                    return buildItem(material, getPluginInstance().getConfig().getInt(menuPath + ".items." + itemId + ".durability"),
                            displayName, newLore, getPluginInstance().getConfig().getInt(menuPath + ".items." + itemId + ".amount"));
                }
            }
        }

        return null;
    }

    public Inventory buildListMenu(OfflinePlayer player)
    {
        Inventory inventory = getPluginInstance().getServer().createInventory(null, getPluginInstance().getConfig().getInt("list-menu-section.size"),
                colorText(getPluginInstance().getConfig().getString("list-menu-section.title")));

        List<Integer> warpSlots = getPluginInstance().getConfig().getIntegerList("list-menu-section.warp-slots");
        ItemStack emptySlotFiller = null;

        int defaultFilterIndex = getPluginInstance().getConfig().getInt("list-menu-section.default-filter-index");

        String currentStatus, ownFormat = getPluginInstance().getConfig().getString("list-menu-section.own-status-format"),
                publicFormat = getPluginInstance().getConfig().getString("list-menu-section.public-status-format"),
                privateFormat = getPluginInstance().getConfig().getString("list-menu-section.private-status-format"),
                adminFormat = getPluginInstance().getConfig().getString("list-menu-section.admin-status-format");

        switch (defaultFilterIndex)
        {
            case 1:
                currentStatus = privateFormat;
                break;
            case 2:
                currentStatus = adminFormat;
                break;
            case 3:
                currentStatus = ownFormat;
                break;
            default:
                currentStatus = publicFormat;
                break;
        }

        getPaging().resetWarpPages(player);
        HashMap<Integer, List<Warp>> warpPageMap = getPaging().getWarpPages(player, "list-menu-section", Objects.requireNonNull(currentStatus));
        getPaging().getWarpPageMap().put(player.getUniqueId(), warpPageMap);
        List<Warp> pageOneWarpList = new ArrayList<>();
        if (warpPageMap != null && !warpPageMap.isEmpty() && warpPageMap.containsKey(1))
            pageOneWarpList = new ArrayList<>(warpPageMap.get(1));

        for (int i = -1; ++i < inventory.getSize(); )
        {
            if (warpSlots.contains(i) && pageOneWarpList.size() >= 1)
            {
                Warp warp = pageOneWarpList.get(0);
                inventory.setItem(i, buildWarpIcon(player, warp));
                pageOneWarpList.remove(warp);
            }
        }

        int currentPage = getPaging().getCurrentPage(player);
        boolean hasPreviousPage = getPaging().hasPreviousWarpPage(player), hasNextPage = getPaging().hasNextWarpPage(player);

        List<String> itemIds = new ArrayList<>(Objects.requireNonNull(getPluginInstance().getConfig().getConfigurationSection("list-menu-section.items")).getKeys(false));
        for (int i = -1; ++i < itemIds.size(); )
        {
            String itemId = itemIds.get(i);
            boolean usePlayerHead = getPluginInstance().getConfig().getBoolean("list-menu-section.items." + itemId + ".use-player-head"),
                    fillEmptySlots = getPluginInstance().getConfig().getBoolean("list-menu-section.items." + itemId + ".fill-empty-slots");
            if (usePlayerHead)
            {
                String displayName = Objects.requireNonNull(getPluginInstance().getConfig()
                        .getString("list-menu-section.items." + itemId + ".display-name"))
                        .replace("{current-page}", String.valueOf(currentPage))
                        .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                        .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")
                        .replace("{current-status}", Objects.requireNonNull(currentStatus));
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getConfig().getStringList("list-menu-section.items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                            .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                            .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")
                            .replace("{current-status}", currentStatus)));

                ItemStack playerHeadItem = getPlayerHead(getPluginInstance().getConfig().getString("list-menu-section.items." + itemId + ".player-head-name"),
                        displayName, newLore, getPluginInstance().getConfig().getInt("list-menu-section.items." + itemId + ".amount"));
                inventory.setItem(getPluginInstance().getConfig().getInt("list-menu-section.items." + itemId + ".slot"), playerHeadItem);
                if (fillEmptySlots) emptySlotFiller = playerHeadItem;
            } else
            {
                String displayName = Objects.requireNonNull(getPluginInstance().getConfig()
                        .getString("list-menu-section.items." + itemId + ".display-name"))
                        .replace("{current-page}", String.valueOf(currentPage))
                        .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                        .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")
                        .replace("{current-status}", Objects.requireNonNull(currentStatus));
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getConfig()
                        .getStringList("list-menu-section.items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                            .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                            .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")
                            .replace("{current-status}", currentStatus)));
                Material material = Material.getMaterial(Objects.requireNonNull(getPluginInstance().getConfig().getString("list-menu-section.items." + itemId + ".material"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"));

                ItemStack itemStack = buildItem(material, getPluginInstance().getConfig().getInt("list-menu-section.items." + itemId + ".durability"),
                        displayName, newLore, getPluginInstance().getConfig().getInt("list-menu-section.items." + itemId + ".amount"));
                inventory.setItem(getPluginInstance().getConfig().getInt("list-menu-section.items." + itemId + ".slot"), itemStack);
                if (fillEmptySlots) emptySlotFiller = itemStack;
            }
        }

        for (int i = -1; ++i < inventory.getSize(); )
        {
            if (emptySlotFiller != null && !warpSlots.contains(i))
            {
                ItemStack itemStack = inventory.getItem(i);
                if (itemStack == null || itemStack.getType() == Material.AIR) inventory.setItem(i, emptySlotFiller);
            }
        }

        return inventory;
    }

    public Inventory buildPlayerSelectionMenu(OfflinePlayer player)
    {
        List<UUID> playersSelected = getPluginInstance().getManager().getPaging().getSelectedPlayers(player);
        if (playersSelected != null)
        {
            playersSelected.clear();
            getPluginInstance().getManager().getPaging().getPlayerSelectedMap().remove(player.getUniqueId());
        }

        Inventory inventory = getPluginInstance().getServer().createInventory(null, getPluginInstance().getConfig().getInt("ps-menu-section.size"),
                colorText(getPluginInstance().getConfig().getString("ps-menu-section.title")));

        List<Integer> playerSlots = getPluginInstance().getConfig().getIntegerList("ps-menu-section.player-slots");
        ItemStack emptySlotFiller = null;


        HashMap<Integer, List<UUID>> playerSelectionMap = getPaging().getPlayerSelectionPages(player);
        getPaging().getPlayerSelectionPageMap().put(player.getUniqueId(), playerSelectionMap);
        List<UUID> pageOnePlayerList = new ArrayList<>();
        if (playerSelectionMap != null && !playerSelectionMap.isEmpty() && playerSelectionMap.containsKey(1))
            pageOnePlayerList = new ArrayList<>(playerSelectionMap.get(1));

        List<UUID> selectedPlayers = getPaging().getSelectedPlayers(player);
        for (int i = -1; ++i < inventory.getSize(); )
        {
            if (playerSlots.contains(i) && pageOnePlayerList.size() >= 1)
            {
                UUID playerUniqueId = pageOnePlayerList.get(0);
                if (playerUniqueId == null) continue;

                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(playerUniqueId);
                if (!offlinePlayer.isOnline()) continue;

                inventory.setItem(i, getPlayerSelectionHead(offlinePlayer, selectedPlayers != null && selectedPlayers.contains(playerUniqueId)));
                pageOnePlayerList.remove(playerUniqueId);
            }
        }

        int currentPage = getPaging().getCurrentPage(player);
        boolean hasPreviousPage = getPaging().hasPreviousPlayerSelectionPage(player), hasNextPage = getPaging().hasNextPlayerSelectionPage(player);
        List<String> itemIds = new ArrayList<>(Objects.requireNonNull(getPluginInstance().getConfig().getConfigurationSection("ps-menu-section.items")).getKeys(false));
        for (int i = -1; ++i < itemIds.size(); )
        {
            String itemId = itemIds.get(i);
            boolean usePlayerHead = getPluginInstance().getConfig().getBoolean("ps-menu-section.items." + itemId + ".use-player-head"),
                    fillEmptySlots = getPluginInstance().getConfig().getBoolean("ps-menu-section.items." + itemId + ".fill-empty-slots");
            if (usePlayerHead)
            {
                String displayName = Objects.requireNonNull(getPluginInstance().getConfig().getString("ps-menu-section.items." + itemId + ".display-name"))
                        .replace("{current-page}", String.valueOf(currentPage))
                        .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                        .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None");
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getConfig().getStringList("ps-menu-section.items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                            .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                            .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")));

                ItemStack playerHeadItem = getPlayerHead(getPluginInstance().getConfig().getString("ps-menu-section.items." + itemId + ".player-head-name"),
                        displayName, newLore, getPluginInstance().getConfig().getInt("ps-menu-section.items." + itemId + ".amount"));
                inventory.setItem(getPluginInstance().getConfig().getInt("ps-menu-section.items." + itemId + ".slot"), playerHeadItem);
                if (fillEmptySlots) emptySlotFiller = playerHeadItem;
            } else
            {
                String displayName = Objects.requireNonNull(getPluginInstance().getConfig().getString("ps-menu-section.items." + itemId + ".display-name"))
                        .replace("{current-page}", String.valueOf(currentPage))
                        .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                        .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None");
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getConfig()
                        .getStringList("ps-menu-section.items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                            .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                            .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")));
                Material material = Material.getMaterial(Objects.requireNonNull(getPluginInstance().getConfig().getString("ps-menu-section.items." + itemId + ".material"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"));

                ItemStack itemStack = buildItem(material, getPluginInstance().getConfig().getInt("ps-menu-section.items." + itemId + ".durability"),
                        displayName, newLore, getPluginInstance().getConfig().getInt("ps-menu-section.items." + itemId + ".amount"));
                inventory.setItem(getPluginInstance().getConfig().getInt("ps-menu-section.items." + itemId + ".slot"), itemStack);
                if (fillEmptySlots) emptySlotFiller = itemStack;
            }
        }

        for (int i = -1; ++i < inventory.getSize(); )
        {
            if (!playerSlots.contains(i) && emptySlotFiller != null)
            {
                ItemStack itemStack = inventory.getItem(i);
                if (itemStack == null || itemStack.getType() == Material.AIR) inventory.setItem(i, emptySlotFiller);
            }
        }

        return inventory;
    }

    public Inventory buildEditMenu(Warp warp)
    {
        Inventory inventory = getPluginInstance().getServer().createInventory(null, getPluginInstance().getConfig().getInt("edit-menu-section.size"),
                colorText(getPluginInstance().getConfig().getString("edit-menu-section.title") + "&" + warp.getDisplayNameColor().getChar() + warp.getWarpName()));
        ItemStack emptySlotFiller = null;

        String toggleFormat = getPluginInstance().getConfig().getString("general-section.option-toggle-format");
        int currentStatusIndex = getPluginInstance().getManager().getStatusIndex(warp.getStatus().name());
        EnumContainer.Status nextStatus = (currentStatusIndex + 1) >= EnumContainer.Status.values().length ? EnumContainer.Status.values()[0] : EnumContainer.Status.values()[currentStatusIndex + 1];

        String publicFormat = getPluginInstance().getConfig().getString("list-menu-section.public-status-format"),
                privateFormat = getPluginInstance().getConfig().getString("list-menu-section.private-status-format"),
                adminFormat = getPluginInstance().getConfig().getString("list-menu-section.admin-status-format");

        String currentStatusName, nextStatusName;

        switch (currentStatusIndex)
        {
            case 1:
                currentStatusName = privateFormat;
                break;
            case 2:
                currentStatusName = adminFormat;
                break;
            default:
                currentStatusName = publicFormat;
                break;
        }

        switch (nextStatus)
        {
            case PRIVATE:
                nextStatusName = privateFormat;
                break;
            case ADMIN:
                nextStatusName = adminFormat;
                break;
            default:
                nextStatusName = publicFormat;
                break;
        }

        List<String> itemIds = new ArrayList<>(Objects.requireNonNull(getPluginInstance().getConfig().getConfigurationSection("edit-menu-section.items")).getKeys(false));
        for (int i = -1; ++i < itemIds.size(); )
        {
            String itemId = itemIds.get(i);
            boolean usePlayerHead = getPluginInstance().getConfig().getBoolean("edit-menu-section.items." + itemId + ".use-player-head"),
                    fillEmptySlots = getPluginInstance().getConfig().getBoolean("edit-menu-section.items." + itemId + ".fill-empty-slots");
            if (usePlayerHead)
            {
                String displayName = getPluginInstance().getConfig().getString("edit-menu-section.items." + itemId + ".display-name"),
                        nextThemeLine = getPluginInstance().getManager().getNextIconTheme(warp), nextAnimationSet = getPluginInstance().getManager().getNextAnimationSet(warp);
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getConfig().getStringList("edit-menu-section.items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j)
                            .replace("{warp-name}", warp.getWarpName()).replace("{warp}", warp.getWarpName())
                            .replace("{enchant-status}", (toggleFormat != null && toggleFormat.contains(":")) ? warp.hasIconEnchantedLook()
                                    ? toggleFormat.split(":")[0] : toggleFormat.split(":")[1] : "")
                            .replace("{current-status}", Objects.requireNonNull(currentStatusName))
                            .replace("{next-status}", Objects.requireNonNull(nextStatusName))
                            .replace("{next-theme}", nextThemeLine != null && nextThemeLine.contains(":") ? nextThemeLine.split(":")[0] : "")
                            .replace("{animation-set}", nextAnimationSet != null && nextAnimationSet.contains(":") ? nextAnimationSet.split(":")[0] : "")
                            .replace("{usage-price}", String.valueOf(getPluginInstance().getConfig().getDouble("edit-menu-section.items." + itemId + ".usage-cost")))));

                ItemStack playerHeadItem = getPlayerHead(getPluginInstance().getConfig().getString("edit-menu-section.items." + itemId + ".player-head-name"),
                        displayName, newLore, getPluginInstance().getConfig().getInt("edit-menu-section.items." + itemId + ".amount"));
                inventory.setItem(getPluginInstance().getConfig().getInt("edit-menu-section.items." + itemId + ".slot"), playerHeadItem);
                if (fillEmptySlots) emptySlotFiller = playerHeadItem;
            } else
            {
                String displayName = getPluginInstance().getConfig().getString("edit-menu-section.items." + itemId + ".display-name"),
                        nextThemeLine = getPluginInstance().getManager().getNextIconTheme(warp),
                        nextAnimationSet = getPluginInstance().getManager().getNextAnimationSet(warp);
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getConfig().getStringList("edit-menu-section.items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j)
                            .replace("{warp-name}", warp.getWarpName()).replace("{warp}", warp.getWarpName())
                            .replace("{enchant-status}", (toggleFormat != null && toggleFormat.contains(":")) ? warp.hasIconEnchantedLook()
                                    ? toggleFormat.split(":")[0] : toggleFormat.split(":")[1] : "")
                            .replace("{current-status}", Objects.requireNonNull(currentStatusName))
                            .replace("{next-status}", Objects.requireNonNull(nextStatusName))
                            .replace("{next-theme}", nextThemeLine != null && nextThemeLine.contains(":") ? nextThemeLine.split(":")[0] : "")
                            .replace("{animation-set}", nextAnimationSet != null && nextAnimationSet.contains(":") ? nextAnimationSet.split(":")[0] : "")
                            .replace("{usage-price}", String.valueOf(getPluginInstance().getConfig().getDouble("edit-menu-section.items." + itemId + ".usage-cost")))));
                Material material = Material.getMaterial(Objects.requireNonNull(getPluginInstance().getConfig().getString("edit-menu-section.items." + itemId + ".material"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"));

                ItemStack itemStack = buildItem(material, getPluginInstance().getConfig().getInt("edit-menu-section.items." + itemId + ".durability"), displayName, newLore,
                        getPluginInstance().getConfig().getInt("edit-menu-section.items." + itemId + ".amount"));
                inventory.setItem(getPluginInstance().getConfig().getInt("edit-menu-section.items." + itemId + ".slot"), itemStack);
                if (fillEmptySlots) emptySlotFiller = itemStack;
            }
        }

        for (int i = -1; ++i < inventory.getSize(); )
        {
            if (emptySlotFiller != null)
            {
                ItemStack itemStack = inventory.getItem(i);
                if (itemStack == null || itemStack.getType() == Material.AIR)
                    inventory.setItem(i, emptySlotFiller);
            }
        }

        return inventory;
    }

    public Inventory buildCustomMenu(OfflinePlayer player, String menuId)
    {
        Inventory inventory = getPluginInstance().getServer().createInventory(null, getPluginInstance().getConfig().getInt("custom-menus-section." + menuId + ".size"),
                colorText(getPluginInstance().getConfig().getString("custom-menus-section." + menuId + ".title")));

        List<Integer> warpSlots = getPluginInstance().getConfig().getIntegerList("custom-menus-section." + menuId + ".warp-slots");
        String currentFilterStatus = getCurrentFilterStatus("custom-menus-section." + menuId, inventory);
        ItemStack emptySlotFiller = null;

        HashMap<Integer, List<Warp>> warpPageMap = getPaging().getWarpPages(player, "custom-menus-section." + menuId, currentFilterStatus);
        getPaging().getWarpPageMap().put(player.getUniqueId(), warpPageMap);
        List<Warp> pageOneWarpList = new ArrayList<>();
        if (warpPageMap != null && !warpPageMap.isEmpty() && warpPageMap.containsKey(1))
            pageOneWarpList = new ArrayList<>(warpPageMap.get(1));

        for (int i = -1; ++i < inventory.getSize(); )
        {
            if (warpSlots.contains(i) && pageOneWarpList.size() >= 1)
            {
                Warp warp = pageOneWarpList.get(0);
                inventory.setItem(i, buildWarpIcon(player, warp));
                pageOneWarpList.remove(warp);
            }
        }

        int currentPage = getPaging().getCurrentPage(player);
        boolean hasPreviousPage = getPaging().hasPreviousWarpPage(player), hasNextPage = getPaging().hasNextWarpPage(player);
        List<String> itemIds = new ArrayList<>(Objects.requireNonNull(getPluginInstance().getConfig().getConfigurationSection("custom-menus-section." + menuId + ".items")).getKeys(false));
        for (int i = -1; ++i < itemIds.size(); )
        {
            String itemId = itemIds.get(i);
            boolean usePlayerHead = getPluginInstance().getConfig().getBoolean("custom-menus-section." + menuId + ".items." + itemId + ".use-player-head"),
                    fillEmptySlots = getPluginInstance().getConfig().getBoolean("custom-menus-section." + menuId + ".items." + itemId + ".fill-empty-slots");
            if (usePlayerHead)
            {
                String displayName = Objects.requireNonNull(getPluginInstance().getConfig()
                        .getString("custom-menus-section." + menuId + ".items." + itemId + ".display-name"))
                        .replace("{current-page}", String.valueOf(currentPage))
                        .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                        .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")
                        .replace("{current-status}", WordUtils.capitalize(currentFilterStatus));
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getConfig()
                        .getStringList("custom-menus-section." + menuId + ".items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                            .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                            .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")
                            .replace("{current-status}", WordUtils.capitalize(currentFilterStatus))));

                ItemStack playerHeadItem = getPlayerHead(getPluginInstance().getConfig().getString("custom-menus-section." + menuId + ".items." + itemId + ".player-head-name"),
                        displayName, newLore, getPluginInstance().getConfig().getInt("custom-menus-section." + menuId + ".items." + itemId + ".amount"));
                inventory.setItem(getPluginInstance().getConfig().getInt("custom-menus-section." + menuId + ".items." + itemId + ".slot"), playerHeadItem);
                if (fillEmptySlots) emptySlotFiller = playerHeadItem;
            } else
            {
                String displayName = Objects.requireNonNull(getPluginInstance().getConfig()
                        .getString("custom-menus-section." + menuId + ".items." + itemId + ".display-name"))
                        .replace("{current-page}", String.valueOf(currentPage))
                        .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                        .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")
                        .replace("{current-status}", WordUtils.capitalize(currentFilterStatus));
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getConfig().getStringList("custom-menus-section." + menuId + ".items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                            .replace("{previous-page}", hasPreviousPage ? String.valueOf((currentPage - 1)) : "None")
                            .replace("{next-page}", hasNextPage ? String.valueOf((currentPage + 1)) : "None")
                            .replace("{current-status}", WordUtils.capitalize(currentFilterStatus))));
                Material material = Material.getMaterial(Objects.requireNonNull(getPluginInstance().getConfig().getString("custom-menus-section." + menuId + ".items." + itemId + ".material"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"));

                ItemStack itemStack = buildItem(material, getPluginInstance().getConfig().getInt("custom-menus-section." + menuId + ".items." + itemId + ".durability"),
                        displayName, newLore, getPluginInstance().getConfig().getInt("custom-menus-section." + menuId + ".items." + itemId + ".amount"));
                inventory.setItem(getPluginInstance().getConfig().getInt("custom-menus-section." + menuId + ".items." + itemId + ".slot"), itemStack);
                if (fillEmptySlots) emptySlotFiller = itemStack;
            }
        }

        for (int i = -1; ++i < inventory.getSize(); )
        {
            if (emptySlotFiller != null && !warpSlots.contains(i))
            {
                ItemStack itemStack = inventory.getItem(i);
                if (itemStack == null || itemStack.getType() == Material.AIR) inventory.setItem(i, emptySlotFiller);
            }
        }

        return inventory;
    }

    // warp stuff
    public String getNextAnimationSet(Warp warp)
    {
        List<String> animationSetList = getPluginInstance().getConfig().getStringList("special-effects-section.warp-animation-list");
        int currentIndex = -1;
        for (int i = -1; ++i < animationSetList.size(); )
        {
            String animationSetLine = animationSetList.get(i);
            if (animationSetLine.equalsIgnoreCase(warp.getAnimationSet()))
            {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex + 1 >= animationSetList.size()) return animationSetList.get(0);
        else return animationSetList.get(currentIndex + 1);
    }

    public int getStatusIndex(String status)
    {
        int index = -1;

        EnumContainer.Status[] statusList = EnumContainer.Status.values();
        for (int i = -1; ++i < statusList.length; )
        {
            if (status != null)
            {
                index += 1;
                if (status.replace(" ", "_").replace("-", "_").equalsIgnoreCase(statusList[i].name())) break;
            }
        }

        return index;
    }

    public boolean hasMetWarpLimit(OfflinePlayer player)
    {
        int warpCount = 0, warpLimit = getWarpLimit(player);
        if (warpLimit < 0) return false;

        List<Warp> warpList = new ArrayList<>(getWarpMap().values());
        for (int i = -1; ++i < warpList.size(); )
        {
            Warp warp = warpList.get(i);
            if (warp.getOwner().toString().equalsIgnoreCase(player.getUniqueId().toString()))
                warpCount += 1;
        }

        return (warpCount >= warpLimit);
    }

    public int getWarpLimit(OfflinePlayer player)
    {
        int currentFoundAmount = getPluginInstance().getConfig().getInt("general-section.default-warp-limit");

        if (player.isOnline())
        {
            Player p = player.getPlayer();
            if (p == null) return 0;
            if (p.hasPermission("hyperdrive.warplimit.*")) return -1;

            List<PermissionAttachmentInfo> lists = new ArrayList<>(p.getEffectivePermissions());
            for (int i = -1; ++i < lists.size(); )
            {
                PermissionAttachmentInfo permission = lists.get(i);
                if (permission.getPermission().toLowerCase().startsWith("hyperdrive.warplimit.") && permission.getValue())
                {
                    int tempValue = Integer.parseInt(permission.getPermission().toLowerCase().replace("hyperdrive.warplimit.", ""));
                    if (tempValue > currentFoundAmount) currentFoundAmount = tempValue;
                }
            }
        }

        return currentFoundAmount;
    }

    public boolean doesWarpExist(String warpName)
    {
        return !getWarpMap().isEmpty() && getWarpMap().containsKey(warpName.toLowerCase());
    }

    public Warp getWarp(String warpName)
    {
        if (!getWarpMap().isEmpty() && getWarpMap().containsKey(warpName.toLowerCase()))
            return getWarpMap().get(warpName.toLowerCase());
        return null;
    }

    public List<String> getPermittedWarps(OfflinePlayer player)
    {
        List<String> permittedWarpNames = new ArrayList<>(), warpNames = new ArrayList<>(getWarpMap().keySet());
        for (int i = -1; ++i < warpNames.size(); )
        {
            String warpName = warpNames.get(i);
            Warp warp = getWarp(warpName);
            if (warp != null && (warp.getOwner().toString().equals(player.getUniqueId().toString()) || warp.getAssistants().contains(player.getUniqueId())))
                if (!permittedWarpNames.contains(warpName)) permittedWarpNames.add(warpName);
        }

        return permittedWarpNames;
    }

    // group methods
    public List<UUID> getGroupMembers(OfflinePlayer player)
    {
        UUID groupLeaderId = getGroupLeader(player);
        if (groupLeaderId != null) if (!getGroupMap().isEmpty() && getGroupMap().containsKey(groupLeaderId))
            return getGroupMap().get(groupLeaderId);
        return null;
    }

    public boolean isGroupLeader(OfflinePlayer player)
    {
        return !getGroupMap().isEmpty() && getGroupMap().containsKey(player.getUniqueId());
    }

    public UUID getGroupLeader(OfflinePlayer player)
    {
        if (isGroupLeader(player)) return player.getUniqueId();

        List<UUID> leaderList = new ArrayList<>(getGroupMap().keySet());
        for (int i = -1; ++i < leaderList.size(); )
        {
            UUID leaderId = leaderList.get(i);
            List<UUID> memberList = getGroupMap().get(leaderId);
            if (memberList != null && memberList.contains(player.getUniqueId()))
                return leaderId;
        }

        return null;
    }


    // chat interaction map methods
    public void updateChatInteraction(OfflinePlayer player, String chatInteractionId, String chatInteractionValue)
    {
        if (!getChatInteractionMap().isEmpty() && getChatInteractionMap().containsKey(player.getUniqueId()))
        {
            HashMap<String, String> interactionMap = getChatInteractionMap().get(player.getUniqueId());
            if (interactionMap == null)
                interactionMap = new HashMap<>();

            interactionMap.put(chatInteractionId, chatInteractionValue);
            getChatInteractionMap().put(player.getUniqueId(), interactionMap);
        } else
        {
            HashMap<String, String> interactionMap = new HashMap<>();
            interactionMap.put(chatInteractionId, chatInteractionValue);
            getChatInteractionMap().put(player.getUniqueId(), interactionMap);
        }
    }

    public String getChatInteractionValue(OfflinePlayer player, String chatInteractionId)
    {
        if (!getChatInteractionMap().isEmpty() && getChatInteractionMap().containsKey(player.getUniqueId()))
        {
            HashMap<String, String> interactionMap = getChatInteractionMap().get(player.getUniqueId());
            if (interactionMap != null && !interactionMap.isEmpty() && interactionMap.containsKey(chatInteractionId))
                return interactionMap.get(chatInteractionId);
        }

        return null;
    }

    public void clearChatInteraction(OfflinePlayer player, String chatInteractionId)
    {
        if (!getChatInteractionMap().isEmpty() && getChatInteractionMap().containsKey(player.getUniqueId()))
        {
            HashMap<String, String> interactionMap = getChatInteractionMap().get(player.getUniqueId());
            if (interactionMap != null && !interactionMap.isEmpty())
                interactionMap.remove(chatInteractionId);
        }
    }

    public void clearChatInteractions(OfflinePlayer player)
    {
        if (!getChatInteractionMap().isEmpty())
            getChatInteractionMap().remove(player.getUniqueId());
    }

    public boolean isInOtherChatInteraction(OfflinePlayer player, String currentChatInteractionId)
    {
        if (!getChatInteractionMap().isEmpty() && getChatInteractionMap().containsKey(player.getUniqueId()))
        {
            HashMap<String, String> interactionMap = getChatInteractionMap().get(player.getUniqueId());
            if (interactionMap != null && !interactionMap.isEmpty())
            {
                List<String> keysetList = new ArrayList<>(interactionMap.keySet());
                for (int i = -1; ++i < keysetList.size(); )
                {
                    String keyId = keysetList.get(i);
                    if (keyId != null && !keyId.equalsIgnoreCase(currentChatInteractionId))
                        return true;
                }
            }
        }

        return false;
    }

    // getters and setters
    private HyperDrive getPluginInstance()
    {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance)
    {
        this.pluginInstance = pluginInstance;
    }

    private HashMap<UUID, HashMap<String, Long>> getCooldownMap()
    {
        return cooldownMap;
    }

    private void setCooldownMap(HashMap<UUID, HashMap<String, Long>> cooldownMap)
    {
        this.cooldownMap = cooldownMap;
    }

    private ParticleHandler getParticleHandler()
    {
        return particleHandler;
    }

    private void setParticleHandler(ParticleHandler particleHandler)
    {
        this.particleHandler = particleHandler;
    }

    public JSONHandler getJsonHandler()
    {
        return jsonHandler;
    }

    private void setJsonHandler(JSONHandler jsonHandler)
    {
        this.jsonHandler = jsonHandler;
    }

    private TitleHandler getTitleHandler()
    {
        return titleHandler;
    }

    private void setTitleHandler(TitleHandler titleHandler)
    {
        this.titleHandler = titleHandler;
    }

    public HashMap<String, Warp> getWarpMap()
    {
        return warpMap;
    }

    private void setWarpMap(HashMap<String, Warp> warpMap)
    {
        this.warpMap = warpMap;
    }

    private HashMap<UUID, HashMap<String, String>> getChatInteractionMap()
    {
        return chatInteractionMap;
    }

    private void setChatInteractionMap(HashMap<UUID, HashMap<String, String>> chatInteractionMap)
    {
        this.chatInteractionMap = chatInteractionMap;
    }

    public Paging getPaging()
    {
        return paging;
    }

    private void setPaging(Paging paging)
    {
        this.paging = paging;
    }

    public SimpleDateFormat getSimpleDateFormat()
    {
        return simpleDateFormat;
    }

    public void setSimpleDateFormat(SimpleDateFormat simpleDateFormat)
    {
        this.simpleDateFormat = simpleDateFormat;
    }

    private HashMap<UUID, Double> getLastTransactionMap()
    {
        return lastTransactionMap;
    }

    private void setLastTransactionMap(HashMap<UUID, Double> lastTransactionMap)
    {
        this.lastTransactionMap = lastTransactionMap;
    }

    private ActionBarHandler getActionBarHandler()
    {
        return actionBarHandler;
    }

    private void setActionBarHandler(ActionBarHandler actionBarHandler)
    {
        this.actionBarHandler = actionBarHandler;
    }

    private HashMap<UUID, List<UUID>> getGroupMap()
    {
        return groupMap;
    }

    private void setGroupMap(HashMap<UUID, List<UUID>> groupMap)
    {
        this.groupMap = groupMap;
    }
}
