/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.api;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import us.eunoians.prisma.ColorProvider;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.events.EconomyChargeEvent;
import xzot1k.plugins.hd.api.objects.SerializableLocation;
import xzot1k.plugins.hd.api.objects.Warp;
import xzot1k.plugins.hd.core.internals.Paging;
import xzot1k.plugins.hd.core.objects.InteractionModule;
import xzot1k.plugins.hd.core.packets.actionbars.ActionBarHandler;
import xzot1k.plugins.hd.core.packets.actionbars.versions.*;
import xzot1k.plugins.hd.core.packets.particles.ParticleHandler;
import xzot1k.plugins.hd.core.packets.particles.versions.*;
import xzot1k.plugins.hd.core.packets.titles.TitleHandler;
import xzot1k.plugins.hd.core.packets.titles.versions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Manager {
    private HyperDrive pluginInstance;
    private Paging paging;
    private SimpleDateFormat simpleDateFormat;

    private ParticleHandler particleHandler;
    private TitleHandler titleHandler;
    private ActionBarHandler actionBarHandler;

    private HashMap<String, Warp> warpMap;
    private HashMap<UUID, HashMap<String, Long>> cooldownMap;
    private HashMap<UUID, InteractionModule> chatInteractionMap;
    private HashMap<UUID, List<UUID>> groupMap;
    private final String[] eventPlaceholders;

    public Manager(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);
        eventPlaceholders = new String[]{"{is-owner}", "{has-access}", "{no-access}", "{can-edit}", "{is-private}", "{is-public}", "{is-admin}"};
        setSimpleDateFormat(new SimpleDateFormat(Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.date-format"))));
        setPaging(new Paging(getPluginInstance()));
        setCooldownMap(new HashMap<>());
        setWarpMap(new HashMap<>());
        setChatInteractionMap(new HashMap<>());
        setGroupMap(new HashMap<>());

        setupPackets();
    }

    // general stuff
    private void setupPackets() {
        boolean succeeded = true;
        long startTime = System.currentTimeMillis();
        switch (getPluginInstance().getServerVersion()) {
           /* case "v1_16_R1":
                setParticleHandler(new Particle_Latest());
                setTitleHandler(new Titles_Latest(getPluginInstance()));
                setActionBarHandler(new ABH_Latest());
                break;
            case "v1_15_R1":
                setParticleHandler(new Particle_Latest());
                setTitleHandler(new Titles_Latest(getPluginInstance()));
                setActionBarHandler(new ABH_Latest());
                break;
            case "v1_14_R1":
                setParticleHandler(new Particle_Latest());
                setTitleHandler(new Titles_Latest(getPluginInstance()));
                setActionBarHandler(new ABH_Latest());
                break;
            case "v1_13_R2":
                setParticleHandler(new Particle_Latest());
                setTitleHandler(new Titles_Latest(getPluginInstance()));
                setActionBarHandler(new ABH_Latest());
                break;
            case "v1_13_R1":
                setParticleHandler(new Particle_Latest());
                setTitleHandler(new Titles_Latest(getPluginInstance()));
                setActionBarHandler(new ABH_Latest());
                break;*/
            case "v1_12_R1":
                setParticleHandler(new Particle1_12R1());
                setTitleHandler(new Titles1_12R1(getPluginInstance()));
                setActionBarHandler(new ABH_Latest());
                break;
            case "v1_11_R1":
                setParticleHandler(new Particle1_11R1());
                setTitleHandler(new Titles1_11R1(getPluginInstance()));
                setActionBarHandler(new ABH_Latest());
                break;
            case "v1_10_R1":
                setParticleHandler(new Particle1_10R1());
                setTitleHandler(new Titles1_10R1(getPluginInstance()));
                setActionBarHandler(new ABH_Latest());
                break;
            case "v1_9_R2":
                setParticleHandler(new Particle1_9R2());
                setTitleHandler(new Titles1_9R2(getPluginInstance()));
                setActionBarHandler(new ABH_Latest());
                break;
            case "v1_9_R1":
                setParticleHandler(new Particle1_9R1());
                setTitleHandler(new Titles1_9R1(getPluginInstance()));
                setActionBarHandler(new ABH_1_9R1());
                break;
            case "v1_8_R3":
                setParticleHandler(new Particle1_8R3());
                setTitleHandler(new Titles1_8R3(getPluginInstance()));
                setActionBarHandler(new ABH_1_8R3());
                break;
            case "v1_8_R2":
                setParticleHandler(new Particle1_8R2());
                setTitleHandler(new Titles1_8R2(getPluginInstance()));
                setActionBarHandler(new ABH_1_8R2());
                break;
            case "v1_8_R1":
                setParticleHandler(new Particle1_8R1());
                setTitleHandler(new Titles1_8R1(getPluginInstance()));
                setActionBarHandler(new ABH_1_8R1());
                break;
            default:
                if (!getPluginInstance().getServerVersion().contains("v1_7")) {
                    setParticleHandler(new Particle_Latest());
                    setTitleHandler(new Titles_Latest(getPluginInstance()));
                    setActionBarHandler(new ABH_Latest());
                } else succeeded = false;
                break;
        }

        if (succeeded)
            getPluginInstance().log(Level.INFO, getPluginInstance().getServerVersion()
                    + " packets were successfully setup! (Took " + (System.currentTimeMillis() - startTime) + "ms)");
        else
            getPluginInstance().log(Level.WARNING,
                    "Your version is not supported by HyperDrive's packets. Expect errors when attempting to use anything packet "
                            + "related. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    /**
     * Gets a Serializable location instance from a string format.
     *
     * @param locationString The location string.
     * @return The new instance of the location.
     */
    public SerializableLocation getLocationFromString(String locationString) {
        if (!locationString.contains(":")) return null;

        String[] worldSplit = locationString.split(":"), coordSplit = worldSplit[1].split(",");
        return new SerializableLocation(worldSplit[0], Double.parseDouble(coordSplit[0]), Double.parseDouble(coordSplit[1]),
                Double.parseDouble(coordSplit[2]), (float) Double.parseDouble(coordSplit[3]), (float) Double.parseDouble(coordSplit[4]));
    }

    public ItemStack getHandItem(Player player) {
        if (!getPluginInstance().getServerVersion().startsWith("v1_8"))
            return player.getInventory().getItemInMainHand();
        else return player.getItemInHand();
    }

    /**
     * See if a string is NOT a numerical value.
     *
     * @param string The string to check.
     * @return Whether it is numerical or not.
     */
    public boolean isNotNumeric(String string) {
        final char[] chars = string.toCharArray();
        for (int i = -1; ++i < string.length(); ) {
            final char c = chars[i];
            if (!Character.isDigit(c) && c != '.' && c != '-') return true;
        }

        return false;
    }

    public List<String> wrapString(String text, int lineSize) {
        List<String> result = new ArrayList<>();

        final int longWordCount = getPluginInstance().getConfig().getInt("warp-icon-section.long-word-wrap");
        final String[] words = text.trim().split(" ");
        if (words.length > 0) {
            int wordCounter = 0;
            StringBuilder sb = new StringBuilder();
            for (int i = -1; ++i < words.length; ) {
                String word = words[i];
                if (wordCounter < lineSize) {

                    if (word.length() >= longWordCount && longWordCount > 0)
                        word.substring(0, Math.min(word.length(), longWordCount));

                    sb.append(word).append(" ");
                    wordCounter++;
                    continue;
                }

                result.add(sb.toString().trim());
                sb = new StringBuilder();
                sb.append(word).append(" ");
                wordCounter = 1;
            }

            result.add(sb.toString().trim());
        }
        return result;
    }

    /**
     * Colors the text passed.
     *
     * @param message The message to translate.
     * @return The colored text.
     */
    public String colorText(String message) {
        String messageCopy = message;
        if ((!getPluginInstance().getServerVersion().startsWith("v1_15") && !getPluginInstance().getServerVersion().startsWith("v1_14")
                && !getPluginInstance().getServerVersion().startsWith("v1_13") && !getPluginInstance().getServerVersion().startsWith("v1_12")
                && !getPluginInstance().getServerVersion().startsWith("v1_11") && !getPluginInstance().getServerVersion().startsWith("v1_10")
                && !getPluginInstance().getServerVersion().startsWith("v1_9") && !getPluginInstance().getServerVersion().startsWith("v1_8"))
                && messageCopy.contains("{#")) {
            if (getPluginInstance().getHookChecker().isPrismaInstalled())
                messageCopy = ColorProvider.translatePrisma(messageCopy);
            else {

                final Pattern hexPattern = Pattern.compile("\\{#([A-Fa-f0-9]){6}}");
                Matcher matcher = hexPattern.matcher(message);
                while (matcher.find()) {
                    final net.md_5.bungee.api.ChatColor hex = net.md_5.bungee.api.ChatColor.of(matcher.group().substring(1, matcher.group().length() - 1));
                    if (hex != null) {
                        final String pre = message.substring(0, matcher.start()), post = message.substring(matcher.end());
                        matcher = hexPattern.matcher(message = (pre + hex + post));
                    }
                }

                return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
            }
        }

        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', messageCopy);
    }

    public void sendCustomMessage(String path, Player player, String... placeholders) {
        String message = getPluginInstance().getLangConfig().getString(path);
        if (message != null && !message.equalsIgnoreCase("")) {
            message = getPluginInstance().replacePapi(player, message);

            for (String phLine : placeholders) {
                if (!phLine.contains(":")) continue;
                String[] args = phLine.split(":");
                if (args.length >= 2)
                    message = message.replace(args[0], args[1]);
            }

            if (message.toLowerCase().startsWith("{bar}")) {
                sendActionBar(player, message.substring(5));
                return;
            }

            String prefix = getPluginInstance().getLangConfig().getString("prefix");
            if (message.contains("<") && message.contains(">")) {
                message = (prefix != null && !prefix.equalsIgnoreCase("") ? prefix : "") + message;
                String jsonFormat = StringUtils.substringBetween(message, "<", ">");
                if (jsonFormat != null) {
                    String splitMessage = message.replace("<" + jsonFormat + ">", "_.SPLIT._");
                    String[] splitMessageArgs = splitMessage.split("_.SPLIT._");

                    BaseComponent originalMessage = new TextComponent(colorText(splitMessageArgs[0]));

                    if (jsonFormat.contains(",")) {
                        String[] extraSplits = jsonFormat.split(",");
                        for (int i = -1; ++i < extraSplits.length; )
                            implementJSONExtras(extraSplits[i], splitMessageArgs, originalMessage);
                    } else implementJSONExtras(jsonFormat, splitMessageArgs, originalMessage);

                    player.spigot().sendMessage(originalMessage);
                    return;
                }
            }

            player.sendMessage(colorText(prefix + message));
        }
    }

    private void implementJSONExtras(String extraLine, String[] splitMessageArgs, BaseComponent originalMessage) {
        final String[] jsonFormatArgs = extraLine.split(":");
        BaseComponent extraMessage = new TextComponent(colorText(jsonFormatArgs[0]));

        if (jsonFormatArgs.length >= 2)
            extraMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, jsonFormatArgs[1]));

        if (jsonFormatArgs.length >= 3)
            extraMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent(jsonFormatArgs[2])}));

        originalMessage.addExtra(extraMessage);

        if (splitMessageArgs.length >= 2) {
            BaseComponent extraExtraMessage = new TextComponent(colorText(splitMessageArgs[1]));
            originalMessage.addExtra(extraExtraMessage);
        }
    }

    public void sendActionBar(Player player, String message) {
        if (message != null && !message.equalsIgnoreCase(""))
            getActionBarHandler().sendActionBar(player, message);
    }

    public void sendTitle(Player player, String title, String subTitle, int fadeIn, int displayTime, int fadeOut) {
        getTitleHandler().sendTitle(player, colorText(title), colorText(subTitle), fadeIn, displayTime, fadeOut);
    }

    public void displayParticle(Location location, String particleEffect) {
        if (location == null || (particleEffect == null || particleEffect.isEmpty())) return;
        getParticleHandler().displayParticle(particleEffect, location, 0, 0, 0, 0, 1);
    }

    public List<UUID> getPlayerUUIDs() {
        List<UUID> tempList = new ArrayList<>();
        List<Player> playerList = new ArrayList<>(getPluginInstance().getServer().getOnlinePlayers());
        for (int i = -1; ++i < playerList.size(); ) {
            Player player = playerList.get(i);
            tempList.add(player.getUniqueId());
        }

        return tempList;
    }

    public String getProgressionBar(int f1, int f2, int segments) {
        StringBuilder bar = new StringBuilder();
        int fractionValue = (int) (((double) f1) / ((double) f2) * segments);

        for (int i = -1; ++i < segments; ) {
            if (fractionValue > 0) {
                bar.append("&a█");
                fractionValue -= 1;
            } else
                bar.append("&c█");
        }

        return bar.toString();
    }

    public int getBounds(World world) {
        List<String> boundsList = getPluginInstance().getConfig().getStringList("random-teleport-section.bounds-radius-list");
        for (int i = -1; ++i < boundsList.size(); ) {
            String boundsLine = boundsList.get(i);
            if (boundsLine != null && !boundsLine.equalsIgnoreCase("") && boundsLine.contains(":")) {
                String[] boundsArgs = boundsLine.split(":");
                if (boundsArgs[0].equalsIgnoreCase(world.getName())) return Integer.parseInt(boundsArgs[1]);
            }
        }

        return getPluginInstance().getConfig().getInt("random-teleport-section.default-bounds");
    }

    /**
     * Gets a chunk using paper methods and other checks.
     *
     * @param world The world to obtain the chunk from.
     * @param x     The chunk x-axis coordinate.
     * @param z     The chunk z-axis coordinate.
     * @return The found chunk after it is found.
     */
    public CompletableFuture<Chunk> getChunk(World world, int x, int z) {
        boolean useAsync = getPluginInstance().asyncChunkMethodExists();
        if (useAsync && !getPluginInstance().getServerVersion().startsWith("v1_8"))
            return world.getChunkAtAsync(x >> 4, z >> 4, true);

        CompletableFuture<Chunk> chunkFuture = new CompletableFuture<>();
        getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () -> {
            if (!useAsync) chunkFuture.complete(world.getChunkAt(x >> 4, z >> 4));
            else
                world.getChunkAtAsync(x >> 4, z >> 4, chunk1 -> chunkFuture.complete(world.getChunkAt(x >> 4, z >> 4)));
        });

        return chunkFuture;
    }

    // cross-server stuff
    public void teleportCrossServer(Player player, String serverName, SerializableLocation location) {
        if (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql") && getPluginInstance().getDatabaseConnection() == null)
            return;

        try {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteArray);
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(pluginInstance, "BungeeCord", byteArray.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
            getPluginInstance().log(Level.WARNING,
                    "There seems to have been an issue when switching the player to the '" + serverName + "' server.");
            return;
        }

        try {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteArray);
            out.writeUTF("Forward");
            out.writeUTF(serverName);

            final String data = (player.getUniqueId().toString() + ";" + location.toString());
            out.writeUTF("HyperDrive");
            out.writeUTF(data);
            player.sendPluginMessage(pluginInstance, "BungeeCord", byteArray.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
            //  return;
        }

       /* getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
            try {
                Statement statement = getPluginInstance().getDatabaseConnection().createStatement();

                ResultSet rs = statement.executeQuery("select * from transfer where player_uuid = '" + player.getUniqueId().toString() + "'");
                if (rs.next()) {
                    statement.executeUpdate("update transfer set location = '" + (location.getWorldName() + "," + location.getX() + "," + location.getY() + ","
                            + location.getZ() + "," + location.getYaw() + "," + location.getPitch()) + "', server_ip = '" + serverIP + "' where player_uuid = '"
                            + player.getUniqueId().toString() + "';");
                    rs.close();
                    statement.close();
                    getPluginInstance().log(Level.WARNING, " updated (" + player.getName() + "_" + player.getUniqueId().toString() + " / "
                            + serverName + " / World: " + location.getWorldName() + ", X: " + location.getX() + ", Y: " + location.getY() + ", Z: " + location.getZ() + ")");
                    return;
                }

                PreparedStatement preparedStatement = getPluginInstance().getDatabaseConnection().prepareStatement("insert into transfer (player_uuid, location, server_ip) values ('"
                        + player.getUniqueId().toString() + "', '" + (location.getWorldName() + "," + location.getX() + "," + location.getY() + "," + location.getZ() + ","
                        + location.getYaw() + "," + location.getPitch()) + "', '" + serverIP + "');");
                preparedStatement.executeUpdate();
                preparedStatement.close();
                getPluginInstance().log(Level.WARNING, "Cross-Server transfer updated (" + player.getName() + "_" + player.getUniqueId().toString()
                        + " / " + serverName + " / World: " + location.getWorldName() + ", X: "
                        + location.getX() + ", Y: " + location.getY() + ", Z: " + location.getZ() + ")");
            } catch (SQLException e) {
                e.printStackTrace();
                getPluginInstance().log(Level.WARNING,
                        "There seems to have been an issue communicating with the MySQL database.");
                getPluginInstance().log(Level.WARNING,
                        "Cross-Server initiation failed for the player '" + player.getName() + "_"
                                + player.getUniqueId().toString() + "'. They have been sent to " + "the '" + serverName
                                + "' server, but the location was unable to be processed.");
            }
        });*/
    }

    // cooldown stuff
    public void updateCooldown(OfflinePlayer player, String cooldownId) {
        HashMap<String, Long> cooldownMap;

        if (!getCooldownMap().isEmpty() && getCooldownMap().containsKey(player.getUniqueId())) {
            cooldownMap = getCooldownMap().get(player.getUniqueId());
            if (cooldownMap != null) {
                cooldownMap.put(cooldownId, System.currentTimeMillis());
                return;
            }
        }

        cooldownMap = new HashMap<>();
        cooldownMap.put(cooldownId, System.currentTimeMillis());
        getCooldownMap().put(player.getUniqueId(), cooldownMap);
    }

    public long getCooldownDuration(OfflinePlayer player, String cooldownId, int cooldownDuration) {
        if (!getCooldownMap().isEmpty() && getCooldownMap().containsKey(player.getUniqueId())) {
            HashMap<String, Long> playerCooldownMap = getCooldownMap().get(player.getUniqueId());
            if (playerCooldownMap != null && !playerCooldownMap.isEmpty() && playerCooldownMap.containsKey(cooldownId))
                return ((playerCooldownMap.get(cooldownId) / 1000) + cooldownDuration)
                        - (System.currentTimeMillis() / 1000);
        }

        return 0;
    }

    public void clearCooldown(OfflinePlayer player, String cooldownId) {
        if (!getCooldownMap().isEmpty() && getCooldownMap().containsKey(player.getUniqueId())) {
            HashMap<String, Long> playerCooldownMap = getCooldownMap().get(player.getUniqueId());
            if (playerCooldownMap != null) playerCooldownMap.remove(cooldownId);
        }
    }

    // inventory stuff
    @SuppressWarnings("deprecation")
    private ItemStack buildItem(Material material, int durability, String displayName, List<String> lore, int amount) {
        ItemStack itemStack = new ItemStack(material, amount);
        itemStack.setDurability((short) durability);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(displayName);
            itemMeta.setLore(lore);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    @SuppressWarnings("deprecation")
    private ItemStack getPlayerHead(String playerName, String displayName, List<String> lore, int amount) {
        boolean isNew = !(getPluginInstance().getServerVersion().startsWith("v1_12") || getPluginInstance().getServerVersion().startsWith("v1_11")
                || getPluginInstance().getServerVersion().startsWith("v1_10") || getPluginInstance().getServerVersion().startsWith("v1_9")
                || getPluginInstance().getServerVersion().startsWith("v1_8"));
        ItemStack itemStack;

        if (isNew) {
            itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial("PLAYER_HEAD")), amount);
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            if (skullMeta != null) {
                if (playerName != null && !playerName.equalsIgnoreCase("")) {
                    OfflinePlayer player = getPluginInstance().getServer().getOfflinePlayer(playerName);
                    skullMeta.setOwningPlayer(player);
                }

                skullMeta.setDisplayName(displayName);
                skullMeta.setLore(lore);
                skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                itemStack.setItemMeta(skullMeta);
            }
        } else {
            itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial("SKULL_ITEM")), amount,
                    (short) org.bukkit.SkullType.PLAYER.ordinal());
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            if (skullMeta != null) {
                if (playerName != null && !playerName.equalsIgnoreCase(""))
                    skullMeta.setOwner(playerName);
                skullMeta.setDisplayName(displayName);
                skullMeta.setLore(lore);
                skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                itemStack.setItemMeta(skullMeta);
            }
        }

        return itemStack;
    }

    @SuppressWarnings("deprecation")
    public ItemStack getPlayerSelectionHead(OfflinePlayer player, boolean isSelected) {
        boolean isNew = getPluginInstance().getServerVersion().startsWith("v1_13") || getPluginInstance().getServerVersion().startsWith("v1_14")
                || getPluginInstance().getServerVersion().startsWith("v1_15") || getPluginInstance().getServerVersion().startsWith("v1_16");
        ItemStack itemStack;

        if (isNew) {
            itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial("PLAYER_HEAD")), 1);
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(player);
                skullMeta.setDisplayName(colorText(isSelected ? getPluginInstance().getMenusConfig().getString("ps-menu-section.selected-player-head.display-name") + player.getName()
                        : getPluginInstance().getMenusConfig().getString("ps-menu-section.unselected-player-head.display-name") + player.getName()));

                List<String> lore = isSelected ? getPluginInstance().getMenusConfig().getStringList("ps-menu-section.selected-player-head.lore")
                        : getPluginInstance().getMenusConfig().getStringList("ps-menu-section.unselected-player-head.lore"), newLore = new ArrayList<>();
                for (int i = -1; ++i < lore.size(); )
                    newLore.add(getPluginInstance().getManager().colorText(lore.get(i)));

                skullMeta.setLore(newLore);
                skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                itemStack.setItemMeta(skullMeta);
            }
        } else {
            itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial("SKULL_ITEM")), 1, (short) 3);
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwner(player.getName());
                skullMeta.setDisplayName(colorText(isSelected ? getPluginInstance().getMenusConfig().getString("ps-menu-section.selected-player-head.display-name") + player.getName()
                        : getPluginInstance().getMenusConfig().getString("ps-menu-section.unselected-player-head.display-name") + player.getName()));

                List<String> lore = isSelected ? getPluginInstance().getMenusConfig().getStringList("ps-menu-section.selected-player-head.lore")
                        : getPluginInstance().getMenusConfig().getStringList("ps-menu-section.unselected-player-head.lore"), newLore = new ArrayList<>();
                for (int i = -1; ++i < lore.size(); )
                    newLore.add(getPluginInstance().getManager().colorText(lore.get(i)));

                skullMeta.setLore(newLore);
                skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                itemStack.setItemMeta(skullMeta);
            }
        }

        return itemStack;
    }

    public String getMenuId(String inventoryName) {
        ConfigurationSection configurationSection = getPluginInstance().getMenusConfig().getConfigurationSection("custom-menus-section");
        if (configurationSection != null) {
            List<String> menuIds = new ArrayList<>(configurationSection.getKeys(false));
            for (int i = -1; ++i < menuIds.size(); ) {
                String menuId = menuIds.get(i), inventoryTitle = colorText(getPluginInstance().getMenusConfig().getString("custom-menus-section." + menuId + ".title"));
                if (inventoryTitle.equalsIgnoreCase(inventoryName)) return menuId;
            }
        }

        return null;
    }

    public String getIdFromSlot(String menuPath, int slot) {
        List<String> configurationSection = new ArrayList<>(
                Objects.requireNonNull(getPluginInstance().getMenusConfig().getConfigurationSection(menuPath + ".items")).getKeys(false));
        for (int i = -1; ++i < configurationSection.size(); ) {
            String itemId = configurationSection.get(i);
            int itemSlot = getPluginInstance().getMenusConfig().getInt(menuPath + ".items." + itemId + ".slot");
            if (itemSlot == slot)
                return itemId;
        }

        return null;
    }

    public String getFilterStatusFromItem(ItemStack itemStack, String menuPath, String itemId) {
        if (itemStack != null && itemStack.hasItemMeta() && Objects.requireNonNull(itemStack.getItemMeta()).hasDisplayName()) {
            String displayName = getPluginInstance().getMenusConfig().getString(menuPath + ".items." + itemId + ".display-name");
            if ((displayName != null) && displayName.toLowerCase().contains("{current-status}")) {
                String[] displayNameArgs = displayName.replace("{current-status}", "%%/split_point/%%")
                        .split("%%/split_point/%%");
                String pulledStatus;
                switch (displayNameArgs.length) {
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

                if (pulledStatus.equals(getPluginInstance().getMenusConfig().getString("list-menu-section.public-status-format")))
                    return EnumContainer.Status.PUBLIC.name();
                else if (pulledStatus.equals(getPluginInstance().getMenusConfig().getString("list-menu-section.private-status-format")))
                    return EnumContainer.Status.PRIVATE.name();
                else if (pulledStatus.equals(getPluginInstance().getMenusConfig().getString("list-menu-section.admin-status-format")))
                    return EnumContainer.Status.ADMIN.name();
                else if (pulledStatus.equals(getPluginInstance().getMenusConfig().getString("list-menu-section.featured-status-format")))
                    return "FEATURED";
                else return ChatColor.stripColor(pulledStatus);
            }
        }

        return EnumContainer.Status.PUBLIC.name();
    }

    public String getCurrentFilterStatus(String menuPath, Inventory inventory) {
        List<String> itemIds = new ArrayList<>(Objects.requireNonNull(getPluginInstance().getMenusConfig().getConfigurationSection(menuPath + ".items")).getKeys(false));
        for (int i = -1; ++i < itemIds.size(); ) {
            String itemId = itemIds.get(i), clickAction = getPluginInstance().getMenusConfig().getString(menuPath + ".items." + itemId + ".click-action");
            if (clickAction != null && clickAction.equalsIgnoreCase("filter-switch")) {
                ItemStack itemStack = inventory.getItem(getPluginInstance().getMenusConfig().getInt(menuPath + ".items." + itemId + ".slot"));
                if (itemStack != null && itemStack.hasItemMeta() && Objects.requireNonNull(itemStack.getItemMeta()).hasDisplayName()) {
                    String displayName = getPluginInstance().getMenusConfig().getString(menuPath + ".items." + itemId + ".display-name");
                    if (displayName != null && displayName.toLowerCase().contains("{current-status}")) {
                        String[] displayNameArgs = displayName.replace("{current-status}", "%%/split_point/%%").split("%%/split_point/%%");
                        String pulledStatus;
                        switch (displayNameArgs.length) {
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

                        return ChatColor.stripColor(pulledStatus);
                    }
                }
            }
        }

        return EnumContainer.Status.PUBLIC.name();
    }

    public ItemStack buildWarpIcon(OfflinePlayer player, Warp warp) {
        String publicFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.public-status-format"),
                privateFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.private-status-format"),
                adminFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.admin-status-format"), statusName;

        switch (warp.getStatus()) {
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

        List<String> iconLoreFormat = getPluginInstance().getConfig().getStringList("warp-icon-section.list-lore-format"), newLore = new ArrayList<>(),
                wrappedDescription = getPluginInstance().getManager().wrapString(warp.getDescription(), getPluginInstance().getConfig().getInt("warp-icon-section.description-line-cap"));

        for (int i = -1; ++i < iconLoreFormat.size(); ) {
            String formatLine = iconLoreFormat.get(i), foundEventPlaceholder = null;
            if ((formatLine.contains("{usage-price}") && warp.getUsagePrice() <= 0)
                    || (formatLine.contains("{creation-date}") && warp.getStatus() == EnumContainer.Status.ADMIN))
                continue;

            if (formatLine.equalsIgnoreCase("{description}") && warp.getDescription() != null) {
                if (wrappedDescription != null && wrappedDescription.size() > 0)
                    for (int j = -1; ++j < wrappedDescription.size(); )
                        newLore.add(ChatColor.GRAY + colorText(wrappedDescription.get(j)));
                continue;
            }

            for (String placeHolder : eventPlaceholders)
                if (formatLine.toLowerCase().contains(placeHolder.toLowerCase())) {
                    foundEventPlaceholder = placeHolder;
                    formatLine = formatLine.replace(placeHolder, "");
                }

            String furtherFormattedLine;
            OfflinePlayer offlinePlayer = warp.getOwner() != null ? getPluginInstance().getServer().getOfflinePlayer(warp.getOwner()) : null;
            String invalidRetrieval = getPluginInstance().getConfig().getString("warp-icon-section.invalid-retrieval");
            if (invalidRetrieval == null) invalidRetrieval = "";

            furtherFormattedLine = formatLine.replace("{creation-date}", warp.getCreationDate() != null ? warp.getCreationDate() : "")
                    .replace("{assistant-count}", String.valueOf(warp.getAssistants().size()))
                    .replace("{usage-price}", String.valueOf(warp.getUsagePrice()))
                    .replace("{list-count}", String.valueOf(warp.getPlayerList().size()))
                    .replace("{status}", statusName != null ? statusName : "")
                    .replace("{theme}", (warp.getIconTheme() != null && warp.getIconTheme().contains(",")) ? warp.getIconTheme().split(",")[0] : "")
                    .replace("{animation-set}", warp.getAnimationSet() != null && warp.getAnimationSet().contains(":") ? warp.getAnimationSet().split(":")[0] : "")
                    .replace("{player}", player != null ? Objects.requireNonNull(player.getName()) : "")
                    .replace("{traffic}", String.valueOf(warp.getTraffic())).replace("{owner}", offlinePlayer != null ? (offlinePlayer.getName() != null ? offlinePlayer.getName() : invalidRetrieval) : invalidRetrieval)
                    .replace("{likes}", String.valueOf(warp.getLikes())).replace("{dislikes}", String.valueOf(warp.getDislikes()))
                    .replace("{like-bar}", warp.getLikeBar());

            if (foundEventPlaceholder != null && player != null) {
                switch (foundEventPlaceholder) {
                    case "{is-owner}":
                        if (warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString()))
                            newLore.add(colorText(furtherFormattedLine));
                        break;
                    case "{can-edit}":
                        if (warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString())
                                || warp.getAssistants().contains(player.getUniqueId()))
                            newLore.add(colorText(furtherFormattedLine));
                        break;
                    case "{has-access}":
                        if (warp.getStatus() == EnumContainer.Status.PUBLIC || ((warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString()))
                                || warp.getAssistants().contains(player.getUniqueId()) || (warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode()))
                                || (Objects.requireNonNull(player.getPlayer()).hasPermission("hyperdrive.warps." + warp.getWarpName()) || player.getPlayer().hasPermission("hyperdrive.warps.*")))
                            newLore.add(colorText(furtherFormattedLine));
                        break;
                    case "{no-access}":
                        if (warp.getStatus() != EnumContainer.Status.PUBLIC && (warp.getOwner() != null && !warp.getOwner().toString().equals(player.getUniqueId().toString()))
                                && !warp.getAssistants().contains(player.getUniqueId()) && (!warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode())
                                && (!Objects.requireNonNull(player.getPlayer()).hasPermission("hyperdrive.warps." + warp.getWarpName()) && !player.getPlayer().hasPermission("hyperdrive.warps.*")))
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
            } else
                newLore.add(colorText(furtherFormattedLine));
        }

        if (warp.getIconTheme() != null && warp.getIconTheme().contains(",")) {
            String[] themeArgs = warp.getIconTheme().split(",");

            if (themeArgs.length <= 3) {
                String materialName = themeArgs[0].toUpperCase().replace(" ", "_").replace("-", "_");

                int durability = themeArgs.length >= 2 ? Integer.parseInt(themeArgs[1]) : 0, amount = themeArgs.length >= 3 ? Integer.parseInt(themeArgs[2]) : 1;
                if (materialName.equalsIgnoreCase("SKULL_ITEM") || materialName.equalsIgnoreCase("PLAYER_HEAD")) {
                    OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(warp.getOwner());
                    ItemStack item = getPlayerHead(offlinePlayer.getName(), colorText(warp.getWarpName()), newLore, amount);
                    ItemMeta itemMeta = item.getItemMeta();
                    if (warp.hasIconEnchantedLook() && itemMeta != null) {
                        itemMeta.addEnchant(Enchantment.DURABILITY, 10, true);
                        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        item.setItemMeta(itemMeta);
                    }
                    return item;
                } else {
                    Material material;
                    try {
                        material = Material.getMaterial(materialName);
                    } catch (Exception ignored) {
                        material = Material.ARROW;
                    }

                    ItemStack item = buildItem(material, durability, colorText(warp.getWarpName()), newLore, amount);
                    ItemMeta itemMeta = item.getItemMeta();
                    if (warp.hasIconEnchantedLook() && itemMeta != null) {
                        itemMeta.addEnchant(Enchantment.DURABILITY, 10, true);
                        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        item.setItemMeta(itemMeta);
                    }

                    return item;
                }
            }
        }

        warp.setIconTheme("");
        ItemStack item;
        if (warp.getOwner() != null) {
            OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(warp.getOwner());
            item = getPlayerHead(offlinePlayer.getName(), warp.getWarpName(), newLore, 1);
            ItemMeta itemMeta = item.getItemMeta();
            if (warp.hasIconEnchantedLook() && itemMeta != null) {
                itemMeta.addEnchant(Enchantment.DURABILITY, 10, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(itemMeta);
            }
        } else {
            if (!(getPluginInstance().getServerVersion().startsWith("v1_12") || getPluginInstance().getServerVersion().startsWith("v1_11")
                    || getPluginInstance().getServerVersion().startsWith("v1_10") || getPluginInstance().getServerVersion().startsWith("v1_9")
                    || getPluginInstance().getServerVersion().startsWith("v1_8")))
                item = new ItemStack(Objects.requireNonNull(Material.getMaterial("PLAYER_HEAD")), 1);
            else item = new ItemStack(Objects.requireNonNull(Material.getMaterial("SKULL_ITEM")), 1, (short) 3);

            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta != null) {
                itemMeta.setDisplayName(warp.getWarpName());
                itemMeta.setLore(newLore);

                if (warp.hasIconEnchantedLook()) {
                    itemMeta.addEnchant(Enchantment.DURABILITY, 10, true);
                    itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(itemMeta);
            }
        }

        return item;
    }

    public ItemStack buildItemFromId(OfflinePlayer player, String currentFilterStatus, String menuPath, String
            itemId) {
        boolean hasPreviousPage = getPaging().hasPreviousWarpPage(player),
                hasNextPage = getPaging().hasNextWarpPage(player);
        int currentPage = getPaging().getCurrentPage(player);
        List<String> itemIds = new ArrayList<>(
                Objects.requireNonNull(getPluginInstance().getMenusConfig().getConfigurationSection(menuPath + ".items"))
                        .getKeys(false));

        String ownFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.own-status-format"),
                publicFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.public-status-format"),
                privateFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.private-status-format"),
                adminFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.admin-status-format"),
                featuredFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.featured-status-format");

        int index = 0;
        if (currentFilterStatus.replace("-", " ").replace("_", " ").equalsIgnoreCase(privateFormat)
                || currentFilterStatus.equalsIgnoreCase(EnumContainer.Status.PRIVATE.name()))
            index = 1;
        else if (currentFilterStatus.replace("-", " ").replace("_", " ").equalsIgnoreCase(adminFormat)
                || currentFilterStatus.equalsIgnoreCase(EnumContainer.Status.ADMIN.name()))
            index = 2;
        else if (currentFilterStatus.replace("-", " ").replace("_", " ").equalsIgnoreCase(ownFormat))
            index = 3;
        else if (currentFilterStatus.replace("-", " ").replace("_", " ").equalsIgnoreCase(featuredFormat))
            index = 4;

        String statusFormat;
        switch (index) {
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
                statusFormat = featuredFormat;
                break;
            default:
                statusFormat = publicFormat;
                break;
        }

        for (int i = -1; ++i < itemIds.size(); ) {
            if (itemIds.get(i).equalsIgnoreCase(itemId)) {
                boolean usePlayerHead = getPluginInstance().getMenusConfig().getBoolean(menuPath + ".items." + itemId + ".use-player-head");
                final String replacement = hasPreviousPage ? String.valueOf((currentPage - 1)) : "None",
                        replacement1 = hasNextPage ? String.valueOf((currentPage + 1)) : "None";
                String displayName = Objects.requireNonNull(getPluginInstance().getMenusConfig().getString(menuPath + ".items." + itemId + ".display-name"))
                        .replace("{current-page}", String.valueOf(currentPage)).replace("{previous-page}", replacement).replace("{next-page}", replacement1)
                        .replace("{next-status}", Objects.requireNonNull(statusFormat)).replace("{current-status}", statusFormat);
                if (usePlayerHead) {
                    List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getMenusConfig().getStringList(menuPath + ".items." + itemId + ".lore");
                    for (int j = -1; ++j < lore.size(); )
                        newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                                .replace("{previous-page}", replacement).replace("{next-page}", replacement1)
                                .replace("{next-status}", statusFormat).replace("{current-status}", statusFormat)));
                    return getPlayerHead(getPluginInstance().getMenusConfig().getString(menuPath + ".items." + itemId + ".player-head-name"),
                            colorText(displayName), newLore, getPluginInstance().getMenusConfig().getInt(menuPath + ".items." + itemId + ".amount"));
                } else {
                    List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getMenusConfig().getStringList(menuPath + ".items." + itemId + ".lore");
                    for (int j = -1; ++j < lore.size(); )
                        newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                                .replace("{previous-page}", replacement).replace("{next-page}", replacement1)
                                .replace("{next-status}", statusFormat).replace("{current-status}", statusFormat)));
                    Material material = Material.getMaterial(Objects.requireNonNull(getPluginInstance().getMenusConfig().getString(menuPath + ".items." + itemId + ".material"))
                            .toUpperCase().replace(" ", "_").replace("-", "_"));
                    return buildItem(material, getPluginInstance().getMenusConfig().getInt(menuPath + ".items." + itemId + ".durability"),
                            colorText(displayName), newLore, getPluginInstance().getMenusConfig().getInt(menuPath + ".items." + itemId + ".amount"));
                }
            }
        }

        return null;
    }

    public Inventory buildListMenu(OfflinePlayer player) {
        final Inventory inventory = getPluginInstance().getServer().createInventory(null, getPluginInstance().getMenusConfig().getInt("list-menu-section.size"),
                colorText(getPluginInstance().getMenusConfig().getString("list-menu-section.title")));

        if (player != null && player.isOnline()) {
            List<Integer> warpSlots = getPluginInstance().getMenusConfig().getIntegerList("list-menu-section.warp-slots");

            int defaultFilterIndex = getPluginInstance().getMenusConfig().getInt("list-menu-section.default-filter-index");
            String currentStatus, ownFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.own-status-format"),
                    publicFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.public-status-format"),
                    privateFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.private-status-format"),
                    adminFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.admin-status-format"),
                    featuredFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.featured-status-format");

            switch (defaultFilterIndex) {
                case 1:
                    currentStatus = privateFormat;
                    break;
                case 2:
                    currentStatus = adminFormat;
                    break;
                case 3:
                    currentStatus = ownFormat;
                    break;
                case 4:
                    currentStatus = featuredFormat;
                    break;
                default:
                    currentStatus = publicFormat;
                    break;
            }

            ItemStack emptySlotFiller = null;

            getPaging().resetWarpPages(player);
            int currentPage = getPaging().getCurrentPage(player);
            boolean hasPreviousPage = getPaging().hasPreviousWarpPage(player), hasNextPage = getPaging().hasNextWarpPage(player);

            List<String> itemIds = new ArrayList<>(Objects.requireNonNull(getPluginInstance().getMenusConfig().getConfigurationSection("list-menu-section.items")).getKeys(false));
            for (int i = -1; ++i < itemIds.size(); ) {
                String itemId = itemIds.get(i);
                if (itemId != null && !itemId.equalsIgnoreCase("")) {
                    final int slot = getPluginInstance().getMenusConfig().getInt("list-menu-section.items." + itemId + ".slot");
                    if (slot <= -1) continue;

                    boolean usePlayerHead = getPluginInstance().getMenusConfig().getBoolean("list-menu-section.items." + itemId + ".use-player-head"),
                            fillEmptySlots = getPluginInstance().getMenusConfig().getBoolean("list-menu-section.items." + itemId + ".fill-empty-slots");
                    String replacement = hasPreviousPage ? String.valueOf((currentPage - 1)) : "None", replacement1 = hasNextPage ? String.valueOf((currentPage + 1)) : "None";
                    String displayName = Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("list-menu-section.items." + itemId + ".display-name"))
                            .replace("{current-page}", String.valueOf(currentPage)).replace("{previous-page}", replacement).replace("{next-page}", replacement1)
                            .replace("{current-status}", Objects.requireNonNull(currentStatus));
                    if (usePlayerHead) {
                        List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getMenusConfig().getStringList("list-menu-section.items." + itemId + ".lore");
                        for (int j = -1; ++j < lore.size(); )
                            newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                                    .replace("{previous-page}", replacement).replace("{next-page}", replacement1)
                                    .replace("{current-status}", currentStatus)));

                        ItemStack playerHeadItem = getPlayerHead(getPluginInstance().getMenusConfig().getString("list-menu-section.items." + itemId + ".player-head-name"),
                                colorText(displayName), newLore, getPluginInstance().getMenusConfig().getInt("list-menu-section.items." + itemId + ".amount"));
                        inventory.setItem(slot, playerHeadItem);
                        if (fillEmptySlots) emptySlotFiller = playerHeadItem;
                    } else {
                        List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getMenusConfig().getStringList("list-menu-section.items." + itemId + ".lore");
                        for (int j = -1; ++j < lore.size(); )
                            newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage)).replace("{previous-page}", replacement)
                                    .replace("{next-page}", replacement1).replace("{current-status}", currentStatus)));
                        Material material = Material.getMaterial(Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("list-menu-section.items." + itemId + ".material"))
                                .toUpperCase().replace(" ", "_").replace("-", "_"));
                        ItemStack itemStack = buildItem(material, getPluginInstance().getMenusConfig().getInt("list-menu-section.items." + itemId + ".durability"),
                                colorText(displayName), newLore, getPluginInstance().getMenusConfig().getInt("list-menu-section.items." + itemId + ".amount"));
                        inventory.setItem(slot, itemStack);
                        if (fillEmptySlots)
                            emptySlotFiller = itemStack;
                    }
                }
            }

            for (int i = -1; ++i < inventory.getSize(); ) {
                if (emptySlotFiller != null && !warpSlots.contains(i)) {
                    ItemStack itemStack = inventory.getItem(i);
                    if (itemStack == null || itemStack.getType() == Material.AIR)
                        inventory.setItem(i, emptySlotFiller);
                }
            }

            getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
                HashMap<Integer, List<Warp>> warpPageMap = getPaging().getWarpPages(player, "list-menu-section", Objects.requireNonNull(currentStatus));
                getPaging().getWarpPageMap().put(player.getUniqueId(), warpPageMap);
                if (warpPageMap != null && !warpPageMap.isEmpty() && warpPageMap.containsKey(1)) {
                    List<Warp> pageOneWarpList = new ArrayList<>(warpPageMap.get(1));
                    for (int i = -1; ++i < inventory.getSize(); )
                        if (warpSlots.contains(i) && pageOneWarpList.size() >= 1) {
                            Warp warp = pageOneWarpList.get(0);

                            ItemStack item = buildWarpIcon(player, warp);
                            if (item != null) inventory.setItem(i, item);
                            pageOneWarpList.remove(warp);
                        }
                }
            });
        }

        return inventory;
    }

    public Inventory buildPlayerSelectionMenu(OfflinePlayer player) {
        List<UUID> playersSelected = getPluginInstance().getManager().getPaging().getSelectedPlayers(player);
        if (playersSelected != null) {
            playersSelected.clear();
            getPluginInstance().getManager().getPaging().getPlayerSelectedMap().remove(player.getUniqueId());
        }

        Inventory inventory = getPluginInstance().getServer().createInventory(null, getPluginInstance().getMenusConfig().getInt("ps-menu-section.size"),
                colorText(getPluginInstance().getMenusConfig().getString("ps-menu-section.title")));

        List<Integer> playerSlots = getPluginInstance().getMenusConfig().getIntegerList("ps-menu-section.player-slots");
        ItemStack emptySlotFiller = null;

        HashMap<Integer, List<UUID>> playerSelectionMap = getPaging().getPlayerSelectionPages(player);
        getPaging().getPlayerSelectionPageMap().put(player.getUniqueId(), playerSelectionMap);
        List<UUID> pageOnePlayerList = new ArrayList<>();
        if (playerSelectionMap != null && !playerSelectionMap.isEmpty() && playerSelectionMap.containsKey(1))
            pageOnePlayerList = new ArrayList<>(playerSelectionMap.get(1));

        List<UUID> selectedPlayers = getPaging().getSelectedPlayers(player);
        for (int i = -1; ++i < inventory.getSize(); ) {
            if (playerSlots.contains(i) && pageOnePlayerList.size() >= 1) {
                UUID playerUniqueId = pageOnePlayerList.get(0);
                if (playerUniqueId == null) continue;

                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(playerUniqueId);
                if (!offlinePlayer.isOnline())
                    continue;

                if (getPluginInstance().getTeleportationHandler().isTeleporting(offlinePlayer.getPlayer()))
                    continue;

                inventory.setItem(i, getPlayerSelectionHead(offlinePlayer, selectedPlayers != null && selectedPlayers.contains(playerUniqueId)));
                pageOnePlayerList.remove(playerUniqueId);
            }
        }

        int currentPage = getPaging().getCurrentPage(player);
        boolean hasPreviousPage = getPaging().hasPreviousPlayerSelectionPage(player), hasNextPage = getPaging().hasNextPlayerSelectionPage(player);
        List<String> itemIds = new ArrayList<>(Objects.requireNonNull(getPluginInstance().getMenusConfig().getConfigurationSection("ps-menu-section.items")).getKeys(false));
        for (int i = -1; ++i < itemIds.size(); ) {
            String itemId = itemIds.get(i);
            final int slot = getPluginInstance().getMenusConfig().getInt("ps-menu-section.items." + itemId + ".slot");
            if (slot <= -1) continue;

            boolean usePlayerHead = getPluginInstance().getMenusConfig().getBoolean("ps-menu-section.items." + itemId + ".use-player-head"),
                    fillEmptySlots = getPluginInstance().getMenusConfig().getBoolean("ps-menu-section.items." + itemId + ".fill-empty-slots");
            String replacement = hasPreviousPage ? String.valueOf((currentPage - 1)) : "None",
                    replacement1 = hasNextPage ? String.valueOf((currentPage + 1)) : "None";

            String displayName = Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("ps-menu-section.items." + itemId + ".display-name"))
                    .replace("{current-page}", String.valueOf(currentPage)).replace("{previous-page}", replacement)
                    .replace("{next-page}", replacement1);
            if (usePlayerHead) {
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getMenusConfig().getStringList("ps-menu-section.items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                            .replace("{previous-page}", replacement).replace("{next-page}", replacement1)));

                ItemStack playerHeadItem = getPlayerHead(getPluginInstance().getMenusConfig().getString("ps-menu-section.items." + itemId + ".player-head-name"),
                        colorText(displayName), newLore, getPluginInstance().getMenusConfig().getInt("ps-menu-section.items." + itemId + ".amount"));
                inventory.setItem(slot, playerHeadItem);
                if (fillEmptySlots) emptySlotFiller = playerHeadItem;
            } else {
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getMenusConfig().getStringList("ps-menu-section.items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage)).replace("{previous-page}", replacement)
                            .replace("{next-page}", replacement1)));
                Material material = Material.getMaterial(Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("ps-menu-section.items." + itemId + ".material"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"));

                ItemStack itemStack = buildItem(material, getPluginInstance().getMenusConfig().getInt("ps-menu-section.items." + itemId + ".durability"),
                        colorText(displayName), newLore, getPluginInstance().getMenusConfig().getInt("ps-menu-section.items." + itemId + ".amount"));
                inventory.setItem(slot, itemStack);
                if (fillEmptySlots)
                    emptySlotFiller = itemStack;
            }
        }

        for (int i = -1; ++i < inventory.getSize(); ) {
            if (!playerSlots.contains(i) && emptySlotFiller != null) {
                ItemStack itemStack = inventory.getItem(i);
                if (itemStack == null || itemStack.getType() == Material.AIR)
                    inventory.setItem(i, emptySlotFiller);
            }
        }

        return inventory;
    }

    public Inventory buildEditMenu(Player player, Warp warp) {
        final Inventory inventory = getPluginInstance().getServer().createInventory(null, getPluginInstance().getMenusConfig().getInt("edit-menu-section.size"),
                colorText(getPluginInstance().getMenusConfig().getString("edit-menu-section.title") + warp.getWarpName()));

        ItemStack emptySlotFiller = null;
        String toggleFormat = getPluginInstance().getConfig().getString("general-section.option-toggle-format");
        int currentStatusIndex = getPluginInstance().getManager().getStatusIndex(warp.getStatus().name());
        EnumContainer.Status nextStatus = (currentStatusIndex + 1) >= EnumContainer.Status.values().length
                ? EnumContainer.Status.values()[0]
                : EnumContainer.Status.values()[currentStatusIndex + 1];

        String publicFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.public-status-format"),
                privateFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.private-status-format"),
                adminFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.admin-status-format");

        String currentStatusName, nextStatusName;

        switch (currentStatusIndex) {
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

        switch (nextStatus) {
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

        if (!player.hasPermission("hyperdrive.admin.status") && nextStatus == EnumContainer.Status.ADMIN)
            nextStatusName = publicFormat;

        List<String> itemIds = new ArrayList<>(Objects.requireNonNull(getPluginInstance().getMenusConfig().getConfigurationSection("edit-menu-section.items")).getKeys(false));
        for (int i = -1; ++i < itemIds.size(); ) {
            String itemId = itemIds.get(i);
            final int slot = getPluginInstance().getMenusConfig().getInt("edit-menu-section.items." + itemId + ".slot");
            if (slot <= -1) continue;

            boolean usePlayerHead = getPluginInstance().getMenusConfig().getBoolean("edit-menu-section.items." + itemId + ".use-player-head"),
                    fillEmptySlots = getPluginInstance().getMenusConfig().getBoolean("edit-menu-section.items." + itemId + ".fill-empty-slots");
            if (usePlayerHead) {
                String displayName = getPluginInstance().getMenusConfig().getString("edit-menu-section.items." + itemId + ".display-name"),
                        nextAnimationSet = getPluginInstance().getManager().getNextAnimationSet(warp);
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getMenusConfig().getStringList("edit-menu-section.items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{warp-name}", warp.getWarpName()).replace("{warp}", warp.getWarpName())
                            .replace("{enchant-status}", (toggleFormat != null && toggleFormat.contains(":")) ? warp.hasIconEnchantedLook()
                                    ? toggleFormat.split(":")[0] : toggleFormat.split(":")[1] : "")
                            .replace("{notify-status}", (toggleFormat != null && toggleFormat.contains(":")) ? warp.canNotify()
                                    ? toggleFormat.split(":")[0] : toggleFormat.split(":")[1] : "")
                            .replace("{current-status}", Objects.requireNonNull(currentStatusName))
                            .replace("{next-status}", Objects.requireNonNull(nextStatusName))
                            .replace("{next-list-type}", warp.isWhiteListMode() ? "Blacklist" : "Whitelist")
                            .replace("{animation-set}", nextAnimationSet != null && nextAnimationSet.contains(":") ? nextAnimationSet.split(":")[0] : "")
                            .replace("{usage-price}", String.valueOf(getPluginInstance().getMenusConfig().getDouble("edit-menu-section.items." + itemId + ".usage-cost")))));

                ItemStack playerHeadItem = getPlayerHead(getPluginInstance().getMenusConfig().getString("edit-menu-section.items." + itemId + ".player-head-name"),
                        colorText(displayName), newLore, getPluginInstance().getMenusConfig().getInt("edit-menu-section.items." + itemId + ".amount"));
                inventory.setItem(slot, playerHeadItem);
                if (fillEmptySlots) emptySlotFiller = playerHeadItem;
            } else {
                String displayName = getPluginInstance().getMenusConfig().getString("edit-menu-section.items." + itemId + ".display-name"),
                        nextAnimationSet = getPluginInstance().getManager().getNextAnimationSet(warp);
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getMenusConfig().getStringList("edit-menu-section.items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{warp-name}", warp.getWarpName()).replace("{warp}", warp.getWarpName())
                            .replace("{enchant-status}", (toggleFormat != null && toggleFormat.contains(":"))
                                    ? warp.hasIconEnchantedLook() ? toggleFormat.split(":")[0] : toggleFormat.split(":")[1] : "")
                            .replace("{notify-status}", (toggleFormat != null && toggleFormat.contains(":")) ? warp.canNotify()
                                    ? toggleFormat.split(":")[0] : toggleFormat.split(":")[1] : "")
                            .replace("{current-status}", Objects.requireNonNull(currentStatusName))
                            .replace("{next-status}", Objects.requireNonNull(nextStatusName))
                            .replace("{next-list-type}", warp.isWhiteListMode() ? "Blacklist" : "Whitelist")
                            .replace("{animation-set}", nextAnimationSet != null && nextAnimationSet.contains(":") ? nextAnimationSet.split(":")[0] : "")
                            .replace("{usage-price}", String.valueOf(getPluginInstance().getMenusConfig().getDouble("edit-menu-section.items." + itemId + ".usage-cost")))));
                Material material = Material.getMaterial(Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("edit-menu-section.items." + itemId + ".material"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"));

                ItemStack itemStack = buildItem(material, getPluginInstance().getMenusConfig().getInt("edit-menu-section.items." + itemId + ".durability"),
                        colorText(displayName), newLore, getPluginInstance().getMenusConfig().getInt("edit-menu-section.items." + itemId + ".amount"));
                inventory.setItem(slot, itemStack);
                if (fillEmptySlots) emptySlotFiller = itemStack;
            }
        }

        for (int i = -1; ++i < inventory.getSize(); ) {
            if (emptySlotFiller != null) {
                ItemStack itemStack = inventory.getItem(i);
                if (itemStack == null || itemStack.getType() == Material.AIR)
                    inventory.setItem(i, emptySlotFiller);
            }
        }

        return inventory;
    }

    public Inventory buildLikeMenu(Warp warp) {
        final Inventory inventory = getPluginInstance().getServer().createInventory(null, getPluginInstance().getMenusConfig().getInt("like-menu-section.size"),
                colorText(getPluginInstance().getMenusConfig().getString("like-menu-section.title") + warp.getWarpName()));

        ItemStack emptySlotFiller = null;
        List<String> itemIds = new ArrayList<>(Objects.requireNonNull(getPluginInstance().getMenusConfig().getConfigurationSection("like-menu-section.items")).getKeys(false));
        for (int i = -1; ++i < itemIds.size(); ) {
            String itemId = itemIds.get(i);
            final int slot = getPluginInstance().getMenusConfig().getInt("like-menu-section.items." + itemId + ".slot");
            if (slot <= -1) continue;

            boolean usePlayerHead = getPluginInstance().getMenusConfig().getBoolean("like-menu-section.items." + itemId + ".use-player-head"),
                    fillEmptySlots = getPluginInstance().getMenusConfig().getBoolean("like-menu-section.items." + itemId + ".fill-empty-slots");
            String displayName = getPluginInstance().getMenusConfig().getString("like-menu-section.items." + itemId + ".display-name");
            if (usePlayerHead) {
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getMenusConfig().getStringList("like-menu-section.items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{warp-name}", warp.getWarpName()).replace("{warp}", warp.getWarpName())
                            .replace("{usage-price}", String.valueOf(getPluginInstance().getMenusConfig().getDouble("like-menu-section.items." + itemId + ".usage-cost")))));

                ItemStack playerHeadItem = getPlayerHead(getPluginInstance().getMenusConfig().getString("like-menu-section.items." + itemId + ".player-head-name"),
                        colorText(displayName), newLore, getPluginInstance().getMenusConfig().getInt("like-menu-section.items." + itemId + ".amount"));
                inventory.setItem(slot, playerHeadItem);
                if (fillEmptySlots) emptySlotFiller = playerHeadItem;
            } else {
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getMenusConfig().getStringList("like-menu-section.items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{warp-name}", warp.getWarpName()).replace("{warp}", warp.getWarpName())
                            .replace("{usage-price}", String.valueOf(getPluginInstance().getMenusConfig().getDouble("like-menu-section.items." + itemId + ".usage-cost")))));
                Material material = Material.getMaterial(Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("like-menu-section.items." + itemId + ".material"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"));

                ItemStack itemStack = buildItem(material, getPluginInstance().getMenusConfig().getInt("like-menu-section.items." + itemId + ".durability"),
                        colorText(displayName), newLore, getPluginInstance().getMenusConfig().getInt("like-menu-section.items." + itemId + ".amount"));
                inventory.setItem(slot, itemStack);
                if (fillEmptySlots) emptySlotFiller = itemStack;
            }
        }

        for (int i = -1; ++i < inventory.getSize(); ) {
            if (emptySlotFiller != null) {
                ItemStack itemStack = inventory.getItem(i);
                if (itemStack == null || itemStack.getType() == Material.AIR)
                    inventory.setItem(i, emptySlotFiller);
            }
        }

        return inventory;
    }

    public Inventory buildCustomMenu(OfflinePlayer player, String menuId) {
        final Inventory inventory = getPluginInstance().getServer().createInventory(null, getPluginInstance().getMenusConfig().getInt("custom-menus-section." + menuId + ".size"),
                colorText(getPluginInstance().getMenusConfig().getString("custom-menus-section." + menuId + ".title")));
        if (player == null || !player.isOnline()) return inventory;

        List<Integer> warpSlots = getPluginInstance().getMenusConfig().getIntegerList("custom-menus-section." + menuId + ".warp-slots");
        String currentFilterStatus = getCurrentFilterStatus("custom-menus-section." + menuId, inventory);
        ItemStack emptySlotFiller = null;

        int currentPage = getPaging().getCurrentPage(player);
        boolean hasPreviousPage = getPaging().hasPreviousWarpPage(player), hasNextPage = getPaging().hasNextWarpPage(player);
        List<String> itemIds = new ArrayList<>(Objects.requireNonNull(getPluginInstance().getMenusConfig().getConfigurationSection("custom-menus-section." + menuId + ".items")).getKeys(false));
        for (int i = -1; ++i < itemIds.size(); ) {
            String itemId = itemIds.get(i);
            final int slot = getPluginInstance().getMenusConfig().getInt("custom-menus-section." + menuId + ".items." + itemId + ".slot");
            if (slot <= -1) continue;

            boolean usePlayerHead = getPluginInstance().getMenusConfig().getBoolean("custom-menus-section." + menuId + ".items." + itemId + ".use-player-head"),
                    fillEmptySlots = getPluginInstance().getMenusConfig().getBoolean("custom-menus-section." + menuId + ".items." + itemId + ".fill-empty-slots");
            String replacement = hasPreviousPage ? String.valueOf((currentPage - 1)) : "None",
                    replacement1 = hasNextPage ? String.valueOf((currentPage + 1)) : "None";

            String displayName = Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("custom-menus-section." + menuId + ".items." + itemId + ".display-name"))
                    .replace("{current-page}", String.valueOf(currentPage)).replace("{previous-page}", replacement)
                    .replace("{next-page}", replacement1).replace("{current-status}", WordUtils.capitalize(currentFilterStatus));
            if (usePlayerHead) {
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getMenusConfig().getStringList("custom-menus-section." + menuId + ".items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                            .replace("{previous-page}", replacement).replace("{next-page}", replacement1)
                            .replace("{current-status}", WordUtils.capitalize(currentFilterStatus))));

                ItemStack playerHeadItem = getPlayerHead(getPluginInstance().getMenusConfig().getString("custom-menus-section." + menuId + ".items." + itemId + ".player-head-name"),
                        colorText(displayName), newLore, getPluginInstance().getMenusConfig().getInt("custom-menus-section." + menuId + ".items." + itemId + ".amount"));
                inventory.setItem(slot, playerHeadItem);
                if (fillEmptySlots)
                    emptySlotFiller = playerHeadItem;
            } else {
                List<String> newLore = new ArrayList<>(), lore = getPluginInstance().getMenusConfig().getStringList("custom-menus-section." + menuId + ".items." + itemId + ".lore");
                for (int j = -1; ++j < lore.size(); )
                    newLore.add(colorText(lore.get(j).replace("{current-page}", String.valueOf(currentPage))
                            .replace("{previous-page}", replacement).replace("{next-page}", replacement1)
                            .replace("{current-status}", WordUtils.capitalize(currentFilterStatus))));
                Material material = Material.getMaterial(Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("custom-menus-section." + menuId + ".items." + itemId + ".material"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"));

                ItemStack itemStack = buildItem(material, getPluginInstance().getMenusConfig().getInt("custom-menus-section." + menuId + ".items." + itemId + ".durability"),
                        colorText(displayName), newLore, getPluginInstance().getMenusConfig().getInt("custom-menus-section." + menuId + ".items." + itemId + ".amount"));
                inventory.setItem(slot, itemStack);
                if (fillEmptySlots)
                    emptySlotFiller = itemStack;
            }
        }

        for (int i = -1; ++i < inventory.getSize(); ) {
            if (emptySlotFiller != null && !warpSlots.contains(i)) {
                ItemStack itemStack = inventory.getItem(i);
                if (itemStack == null || itemStack.getType() == Material.AIR)
                    inventory.setItem(i, emptySlotFiller);
            }
        }

        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
            HashMap<Integer, List<Warp>> wpMap = getPluginInstance().getManager().getPaging().getWarpPages(player, "custom-menus-section." + menuId, currentFilterStatus);
            getPaging().getWarpPageMap().put(player.getUniqueId(), wpMap);
            int page = getPluginInstance().getManager().getPaging().getCurrentPage(player);
            List<Warp> wList = new ArrayList<>();
            if (wpMap != null && !wpMap.isEmpty() && wpMap.containsKey(page))
                wList = new ArrayList<>(wpMap.get(page));

            if (!wList.isEmpty()) {
                int increment = 0;
                for (Warp warp : wList) {
                    if ((increment + 1) < warpSlots.size()) {
                        final ItemStack warpIcon = getPluginInstance().getManager().buildWarpIcon(player, warp);
                        if (warpIcon != null) inventory.setItem(warpSlots.get(increment), warpIcon);
                        increment++;
                        continue;
                    }

                    break;
                }
            }
        });

        return inventory;
    }

    // economy stuff
    public boolean initiateEconomyCharge(Player player, double chargeAmount) {
        boolean useVault = getPluginInstance().getConfig().getBoolean("general-section.use-vault");
        if (useVault && !player.hasPermission("hyperdrive.economybypass") && chargeAmount > 0) {
            EconomyChargeEvent economyChargeEvent = new EconomyChargeEvent(player, chargeAmount);
            getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () -> getPluginInstance().getServer().getPluginManager().callEvent(economyChargeEvent));
            if (!economyChargeEvent.isCancelled()) {
                if (!getPluginInstance().getVaultHandler().getEconomy().has(player, economyChargeEvent.getAmount())) {
                    getPluginInstance().getManager().sendCustomMessage("insufficient-funds", player, "{amount}:" + chargeAmount, "{player}:" + player.getName());
                    return false;
                }

                getPluginInstance().getVaultHandler().getEconomy().withdrawPlayer(player, chargeAmount);
                getPluginInstance().getManager().sendCustomMessage("transaction-success", player, "{amount}:" + chargeAmount, "{player}:" + player.getName());
            }
        }

        return true;
    }

    // warp stuff

    /**
     * Checks if the passed player can edit the passed warp.
     *
     * @param player The player to check.
     * @param warp   The warp to check access for.
     * @return Whether the player can edit the warp.
     */
    public boolean canEditWarp(Player player, Warp warp) {
        return (player.hasPermission("hyperdrive.admin.edit") || player.hasPermission("hyperdrive.edit.*")
                || player.hasPermission("hyperdrive.edit." + net.md_5.bungee.api.ChatColor.stripColor(warp.getWarpName()))
                || warp.getOwner().toString().equals(player.getUniqueId().toString())
                || warp.getAssistants().contains(player.getUniqueId()));
    }

    /**
     * Checks if the passed player can use the passed warp.
     *
     * @param player The player to check.
     * @param warp   The warp to check access for.
     * @return Whether the place can use the warp.
     */
    public boolean canUseWarp(Player player, Warp warp) {
        return (warp.getStatus() == EnumContainer.Status.PUBLIC || (warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString()))
                || warp.getAssistants().contains(player.getUniqueId()) || (warp.isWhiteListMode() && warp.getPlayerList().contains(player.getUniqueId()))
                || (!warp.isWhiteListMode() && !warp.getPlayerList().contains(player.getUniqueId()))
                || player.hasPermission("hyperdrive.warps." + net.md_5.bungee.api.ChatColor.stripColor(warp.getWarpName()))
                || player.hasPermission("hyperdrive.warps.*"));
    }

    public String getNextAnimationSet(Warp warp) {
        List<String> animationSetList = getPluginInstance().getConfig()
                .getStringList("special-effects-section.warp-animation-list");
        int currentIndex = -1;
        for (int i = -1; ++i < animationSetList.size(); ) {
            String animationSetLine = animationSetList.get(i);
            if (animationSetLine.equalsIgnoreCase(warp.getAnimationSet())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex + 1 >= animationSetList.size())
            return animationSetList.get(0);
        else
            return animationSetList.get(currentIndex + 1);
    }

    public int getStatusIndex(String status) {
        int index = -1;

        EnumContainer.Status[] statusList = EnumContainer.Status.values();
        for (int i = -1; ++i < statusList.length; ) {
            if (status != null) {
                index += 1;
                if (status.replace(" ", "_").replace("-", "_").equalsIgnoreCase(statusList[i].name()))
                    break;
            }
        }

        return index;
    }

    public boolean hasMetWarpLimit(OfflinePlayer player) {
        int warpCount = 0, warpLimit = getWarpLimit(player);
        if (warpLimit < 0)
            return false;

        List<Warp> warpList = new ArrayList<>(getWarpMap().values());
        for (int i = -1; ++i < warpList.size(); ) {
            Warp warp = warpList.get(i);
            if (warp.getOwner() != null && warp.getOwner().toString().equalsIgnoreCase(player.getUniqueId().toString()))
                warpCount += 1;
        }

        return (warpCount >= warpLimit);
    }

    public int getWarpLimit(OfflinePlayer player) {
        int currentFoundAmount = getPluginInstance().getConfig().getInt("general-section.default-warp-limit");

        if (player.isOnline()) {
            Player p = player.getPlayer();
            if (p == null)
                return 0;
            if (p.hasPermission("hyperdrive.warplimit.*"))
                return -1;

            List<PermissionAttachmentInfo> lists = new ArrayList<>(p.getEffectivePermissions());
            for (int i = -1; ++i < lists.size(); ) {
                PermissionAttachmentInfo permission = lists.get(i);
                if (permission.getPermission().toLowerCase().startsWith("hyperdrive.warplimit.") && permission.getValue()) {
                    String foundValue = permission.getPermission().toLowerCase().replace("hyperdrive.warplimit.", "");
                    if (isNotNumeric(foundValue)) continue;

                    int tempValue = Integer.parseInt(foundValue);
                    if (tempValue > currentFoundAmount)
                        currentFoundAmount = tempValue;
                }
            }
        }

        return currentFoundAmount;
    }

    public boolean doesWarpExist(String warpName) {
        if (!getWarpMap().isEmpty())
            for (Map.Entry<String, Warp> entry : getWarpMap().entrySet())
                if (net.md_5.bungee.api.ChatColor.stripColor(entry.getKey()).equalsIgnoreCase(net.md_5.bungee.api.ChatColor.stripColor(warpName)))
                    return true;
        return false;
    }

    public Warp getWarp(String warpName) {
        if (!getWarpMap().isEmpty())
            for (Map.Entry<String, Warp> entry : getWarpMap().entrySet()) {
                if (net.md_5.bungee.api.ChatColor.stripColor(entry.getKey()).equalsIgnoreCase(net.md_5.bungee.api.ChatColor.stripColor(warpName)))
                    return entry.getValue();
            }
        return null;
    }

    public List<String> getPermittedWarps(OfflinePlayer player) {
        List<String> permittedWarpNames = new ArrayList<>();
        for (Map.Entry<String, Warp> entry : getWarpMap().entrySet()) {
            final String warpName = net.md_5.bungee.api.ChatColor.stripColor(entry.getKey());
            final Warp warp = entry.getValue();
            if (warp != null && (warp.getOwner().toString().equals(player.getUniqueId().toString())
                    || warp.getAssistants().contains(player.getUniqueId())))
                if (!permittedWarpNames.contains(warpName)) permittedWarpNames.add(warpName);
        }

        return permittedWarpNames;
    }

    public boolean isBlockedWorld(World world) {
        if (world == null) return false;
        List<String> worldNames = getPluginInstance().getConfig().getStringList("general-section.world-blacklist");
        for (int i = -1; ++i < worldNames.size(); )
            if (worldNames.get(i).equalsIgnoreCase(world.getName())) return true;
        return false;
    }

    // group methods
    public List<UUID> getGroupMembers(OfflinePlayer player) {
        UUID groupLeaderId = getGroupLeader(player);
        if (groupLeaderId != null)
            if (!getGroupMap().isEmpty() && getGroupMap().containsKey(groupLeaderId))
                return getGroupMap().get(groupLeaderId);
        return null;
    }

    public boolean isGroupLeader(OfflinePlayer player) {
        return !getGroupMap().isEmpty() && getGroupMap().containsKey(player.getUniqueId());
    }

    public UUID getGroupLeader(OfflinePlayer player) {
        if (isGroupLeader(player))
            return player.getUniqueId();

        List<UUID> leaderList = new ArrayList<>(getGroupMap().keySet());
        for (int i = -1; ++i < leaderList.size(); ) {
            UUID leaderId = leaderList.get(i);
            List<UUID> memberList = getGroupMap().get(leaderId);
            if (memberList != null && memberList.contains(player.getUniqueId()))
                return leaderId;
        }

        return null;
    }

    // chat interaction map methods
    public void updateChatInteraction(OfflinePlayer player, String interactionId, String interactionValue,
                                      double interactionPrice) {
        InteractionModule interactionModule = new InteractionModule(interactionId, interactionValue, interactionPrice);
        getChatInteractionMap().put(player.getUniqueId(), interactionModule);
    }

    public InteractionModule getChatInteraction(OfflinePlayer player) {
        if (!getChatInteractionMap().isEmpty() && getChatInteractionMap().containsKey(player.getUniqueId()))
            return getChatInteractionMap().get(player.getUniqueId());
        return null;
    }

    public void clearChatInteraction(OfflinePlayer player) {
        if (!getChatInteractionMap().isEmpty())
            getChatInteractionMap().remove(player.getUniqueId());
    }

    public boolean isNotInChatInteraction(OfflinePlayer player) {
        return getChatInteractionMap().isEmpty() || !getChatInteractionMap().containsKey(player.getUniqueId());
    }

    // getters and setters
    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    private HashMap<UUID, HashMap<String, Long>> getCooldownMap() {
        return cooldownMap;
    }

    private void setCooldownMap(HashMap<UUID, HashMap<String, Long>> cooldownMap) {
        this.cooldownMap = cooldownMap;
    }

    private ParticleHandler getParticleHandler() {
        return particleHandler;
    }

    private void setParticleHandler(ParticleHandler particleHandler) {
        this.particleHandler = particleHandler;
    }

    private TitleHandler getTitleHandler() {
        return titleHandler;
    }

    private void setTitleHandler(TitleHandler titleHandler) {
        this.titleHandler = titleHandler;
    }

    public HashMap<String, Warp> getWarpMap() {
        return warpMap;
    }

    private void setWarpMap(HashMap<String, Warp> warpMap) {
        this.warpMap = warpMap;
    }

    public Paging getPaging() {
        return paging;
    }

    private void setPaging(Paging paging) {
        this.paging = paging;
    }

    public SimpleDateFormat getSimpleDateFormat() {
        return simpleDateFormat;
    }

    public void setSimpleDateFormat(SimpleDateFormat simpleDateFormat) {
        this.simpleDateFormat = simpleDateFormat;
    }

    private ActionBarHandler getActionBarHandler() {
        return actionBarHandler;
    }

    private void setActionBarHandler(ActionBarHandler actionBarHandler) {
        this.actionBarHandler = actionBarHandler;
    }

    private HashMap<UUID, List<UUID>> getGroupMap() {
        return groupMap;
    }

    private void setGroupMap(HashMap<UUID, List<UUID>> groupMap) {
        this.groupMap = groupMap;
    }

    private HashMap<UUID, InteractionModule> getChatInteractionMap() {
        return chatInteractionMap;
    }

    private void setChatInteractionMap(HashMap<UUID, InteractionModule> chatInteractionMap) {
        this.chatInteractionMap = chatInteractionMap;
    }

}
