/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals;

import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;
import xzot1k.plugins.hd.api.events.EconomyChargeEvent;
import xzot1k.plugins.hd.api.events.MenuOpenEvent;
import xzot1k.plugins.hd.api.objects.Warp;
import xzot1k.plugins.hd.core.objects.Destination;
import xzot1k.plugins.hd.core.objects.GroupTemp;
import xzot1k.plugins.hd.core.objects.InteractionModule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class Listeners implements Listener {
    private HyperDrive pluginInstance;

    public Listeners(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player player = (Player) e.getWhoClicked();
            if (player.isSleeping()) return;

            String inventoryName;

            try {
                if (getPluginInstance().getServerVersion().startsWith("v1_14") || getPluginInstance().getServerVersion().startsWith("v1_15"))
                    inventoryName = e.getView().getTitle();
                else {
                    Method method = e.getInventory().getClass().getMethod("getName");
                    if (method == null)
                        method = e.getInventory().getClass().getMethod("getTitle");
                    inventoryName = (String) method.invoke(e.getInventory());
                }
            } catch (NoSuchMethodException | IllegalStateException | IllegalAccessException | InvocationTargetException ex) {
                return;
            }

            if (ChatColor.stripColor(inventoryName).equalsIgnoreCase(ChatColor.stripColor(getPluginInstance()
                    .getManager().colorText(getPluginInstance().getMenusConfig().getString("list-menu-section.title")))))
                runListMenuClick(player, e);
            else if (ChatColor.stripColor(inventoryName).contains(ChatColor.stripColor(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getMenusConfig().getString("edit-menu-section.title")))))
                runEditMenuClick(player, inventoryName, e);
            else if (ChatColor.stripColor(inventoryName).contains(ChatColor.stripColor(getPluginInstance().getManager()
                    .colorText(getPluginInstance().getMenusConfig().getString("like-menu-section.title")))))
                runLikeMenuClick(player, inventoryName, e);
            else if (ChatColor.stripColor(inventoryName).equalsIgnoreCase(ChatColor.stripColor(getPluginInstance()
                    .getManager().colorText(getPluginInstance().getMenusConfig().getString("ps-menu-section.title")))))
                runPlayerSelectionClick(player, e);
            else {
                String menuId = getPluginInstance().getManager().getMenuId(inventoryName);
                if (menuId != null)
                    runCustomMenuClick(player, menuId, e);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        InteractionModule interactionModule = getPluginInstance().getManager().getChatInteraction(e.getPlayer());
        if (interactionModule == null)
            return;

        Warp warp;
        ChatColor enteredColor;
        OfflinePlayer offlinePlayer;
        String enteredName = e.getMessage().replace(" ", "_"), message, enteredText, enteredValue,
                chatInteractionCancelKey = getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel");
        List<String> globalFilterStrings = getPluginInstance().getConfig().getStringList("filter-section.global-filter");
        boolean useMySQL = getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql");
        switch (interactionModule.getInteractionId().toLowerCase()) {
            case "create-warp":
                e.setCancelled(true);
                enteredName = ChatColor.stripColor(getPluginInstance().getManager().colorText(e.getMessage())).replace(" ", "_");
                if (e.getMessage().equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                enteredName = enteredName.replaceAll("[.,?:;'\"\\\\|`~!@#$%^&*()+=/<>]", "");
                for (int i = -1; ++i < globalFilterStrings.size(); ) {
                    String filterString = globalFilterStrings.get(i).replaceAll("[.,?:;'\"\\\\|`~!@#$%^&*()+=/<>]", "");
                    if (!filterString.equalsIgnoreCase(""))
                        enteredName = enteredName.replaceAll("(?i)" + filterString, "");
                }

                if (getPluginInstance().getManager().hasMetWarpLimit(e.getPlayer())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-limit-met")), e.getPlayer());
                    return;
                }

                if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(enteredName)) || (!useMySQL && getPluginInstance().getManager().doesWarpExist(enteredName))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-exists"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                warp = new Warp(enteredName, e.getPlayer(), e.getPlayer().getLocation());
                warp.register();
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-created"))
                        .replace("{warp}", enteredName), e.getPlayer());
                break;
            case "rename":
                e.setCancelled(true);
                if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                enteredName = enteredName.replaceAll("[.,?:;'\"\\\\|`~!@#$%^&*()+=/<>]", "");
                for (int i = -1; ++i < globalFilterStrings.size(); ) {
                    String filterString = globalFilterStrings.get(i).replaceAll("[.,?:;'\"\\\\|`~!@#$%^&*()+=/<>]", "");
                    if (!filterString.equalsIgnoreCase(""))
                        enteredName = enteredName.replaceAll("(?i)" + filterString, "");
                }

                String previousName = interactionModule.getInteractionValue();
                warp = getPluginInstance().getManager().getWarp(previousName);
                if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(enteredName)) || (!useMySQL && getPluginInstance().getManager().doesWarpExist(enteredName))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-exists"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.rename(enteredName);
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-renamed"))
                        .replace("{previous-name}", previousName).replace("{new-name}", enteredName), e.getPlayer());
                break;
            case "give-ownership":
                e.setCancelled(true);
                if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(enteredName);
                if (!offlinePlayer.isOnline()) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                            .replace("{player}", enteredName), e.getPlayer());
                    return;
                }

                previousName = interactionModule.getInteractionValue();
                warp = getPluginInstance().getManager().getWarp(previousName);
                if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-invalid"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.setOwner(offlinePlayer.getUniqueId());
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-renamed"))
                        .replace("{previous-name}", previousName).replace("{new-name}", enteredName), e.getPlayer());
                break;
            case "edit-description":
                e.setCancelled(true);
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (e.getMessage().equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", warp.getWarpName()), e.getPlayer());
                    return;
                }

                if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-invalid"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                if (e.getMessage().equalsIgnoreCase(getPluginInstance().getConfig().getString("warp-icon-section.description-clear-symbol"))) {
                    warp.setDescription("");
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("description-cleared"))
                            .replace("{warp}", warp.getWarpName()), e.getPlayer());
                } else {
                    String desc = ChatColor.stripColor(getPluginInstance().getManager().colorText(e.getMessage()));
                    for (int i = -1; ++i < globalFilterStrings.size(); ) {
                        String filterString = globalFilterStrings.get(i);
                        if (filterString != null && !filterString.equalsIgnoreCase(""))
                            enteredName = enteredName.replaceAll("(?i)" + filterString, "");
                    }

                    warp.setDescription(desc);
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("description-set"))
                            .replace("{warp}", warp.getWarpName()).replace("{description}", warp.getDescriptionColor()
                                    + ChatColor.stripColor(getPluginInstance().getManager().colorText(e.getMessage()))), e.getPlayer());
                }

                break;
            case "change-description-color":
                e.setCancelled(true);
                enteredText = ChatColor.stripColor(e.getMessage().replace(" ", "_"));
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (enteredText.equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", warp.getWarpName()), e.getPlayer());
                    return;
                }

                if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-invalid"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                message = Objects.requireNonNull(getPluginInstance().getLangConfig().getString("invalid-color")).replace("{colors}", getPluginInstance().getManager().getColorNames().toString());
                enteredValue = ChatColor.stripColor(getPluginInstance().getManager().colorText(e.getMessage().toUpperCase().replace(" ", "_").replace("-", "_")));
                if (getPluginInstance().getManager().isNotChatColor(enteredValue)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(message, e.getPlayer());
                    return;
                }

                enteredColor = ChatColor.valueOf(enteredValue);
                if (enteredColor == ChatColor.BOLD || enteredColor == ChatColor.MAGIC || enteredColor == ChatColor.UNDERLINE
                        || enteredColor == ChatColor.STRIKETHROUGH || enteredColor == ChatColor.ITALIC
                        || enteredColor == ChatColor.RESET) {
                    getPluginInstance().getManager().sendCustomMessage(message, e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.setDescriptionColor(enteredColor);
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("description-color-changed"))
                        .replace("{warp}", warp.getWarpName()).replace("{color}", enteredColor + WordUtils.capitalize(enteredColor.name().toLowerCase()
                                .replace("_", " ").replace("-", "_"))), e.getPlayer());
                break;
            case "change-name-color":
                e.setCancelled(true);
                message = Objects.requireNonNull(getPluginInstance().getLangConfig().getString("invalid-color")).replace("{colors}", getPluginInstance().getManager().getColorNames().toString());
                enteredText = ChatColor.stripColor(getPluginInstance().getManager().colorText(e.getMessage().toUpperCase().replace(" ", "_").replace("-", "_")));
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (enteredText.equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", warp.getWarpName()), e.getPlayer());
                    return;
                }

                if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-invalid"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                if (getPluginInstance().getManager().isNotChatColor(enteredText)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(message, e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount()))
                    return;

                enteredColor = ChatColor.valueOf(enteredText);
                if (enteredColor == ChatColor.BOLD || enteredColor == ChatColor.MAGIC || enteredColor == ChatColor.UNDERLINE || enteredColor == ChatColor.STRIKETHROUGH
                        || enteredColor == ChatColor.ITALIC || enteredColor == ChatColor.RESET) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(message, e.getPlayer());
                    return;
                }

                warp.setDisplayNameColor(enteredColor);
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("name-color-changed"))
                        .replace("{warp}", warp.getWarpName()).replace("{color}", enteredColor
                                + WordUtils.capitalize(enteredColor.name().toLowerCase().replace("_", " ").replace("-", "_"))), e.getPlayer());
                break;
            case "change-usage-price":
                e.setCancelled(true);
                enteredText = e.getMessage().replace(" ", "_").replace("$", "");
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());

                if (enteredText.equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", warp.getWarpName()), e.getPlayer());
                    return;
                }

                if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-invalid"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                if (getPluginInstance().getManager().isNotNumeric(enteredText)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("invalid-usage-price"), e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.setUsagePrice(Double.parseDouble(enteredText));
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("usage-price-set"))
                        .replace("{warp}", warp.getWarpName()).replace("{price}", enteredText), e.getPlayer());
                break;
            case "give-assistant":
                e.setCancelled(true);
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", warp.getWarpName()), e.getPlayer());
                    return;
                }

                if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-invalid"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(enteredName);
                if (!offlinePlayer.isOnline()) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                            .replace("{player}", enteredName), e.getPlayer());
                    return;
                }

                if (warp.getAssistants().contains(offlinePlayer.getUniqueId())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-already-assistant"))
                            .replace("{player}", enteredName), e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount()))
                    return;

                warp.getAssistants().add(offlinePlayer.getUniqueId());
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("give-assistant"))
                                .replace("{warp}", warp.getWarpName()).replace("{player}", Objects.requireNonNull(offlinePlayer.getName())),
                        e.getPlayer());
                break;
            case "remove-assistant":
                e.setCancelled(true);
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());

                if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", warp.getWarpName()), e.getPlayer());
                    return;
                }

                if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-invalid"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(enteredName);
                if (!offlinePlayer.isOnline()) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                            .replace("{player}", enteredName), e.getPlayer());
                    return;
                }

                if (!warp.getAssistants().contains(offlinePlayer.getUniqueId())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-not-assistant"))
                            .replace("{player}", enteredName), e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.getAssistants().remove(offlinePlayer.getUniqueId());
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("remove-assistant"))
                        .replace("{warp}", warp.getWarpName()).replace("{player}", Objects.requireNonNull(offlinePlayer.getName())), e.getPlayer());
                break;
            case "add-to-list":
                e.setCancelled(true);
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", warp.getWarpName()), e.getPlayer());
                    return;
                }

                if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-invalid"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(enteredName);
                if (!offlinePlayer.isOnline()) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                            .replace("{player}", enteredName), e.getPlayer());
                    return;
                }

                if (warp.getPlayerList().contains(offlinePlayer.getUniqueId())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-in-list"))
                            .replace("{player}", enteredName), e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.getPlayerList().add(offlinePlayer.getUniqueId());
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("add-list"))
                        .replace("{warp}", warp.getWarpName()).replace("{player}", Objects.requireNonNull(offlinePlayer.getName())), e.getPlayer());
                break;
            case "remove-from-list":
                e.setCancelled(true);
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", warp.getWarpName()), e.getPlayer());
                    return;
                }

                if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-invalid"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(enteredName);
                if (!offlinePlayer.isOnline()) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-invalid"))
                            .replace("{player}", enteredName), e.getPlayer());
                    return;
                }

                if (!warp.getPlayerList().contains(offlinePlayer.getUniqueId())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-not-listed"))
                            .replace("{player}", enteredName), e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.getPlayerList().remove(offlinePlayer.getUniqueId());
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage(Objects
                        .requireNonNull(getPluginInstance().getLangConfig().getString("remove-list"))
                        .replace("{warp}", warp.getWarpName())
                        .replace("{player}", Objects.requireNonNull(offlinePlayer.getName())), e.getPlayer());
                break;
            case "add-command":
                e.setCancelled(true);
                String enteredCommand = e.getMessage();
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());

                if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", warp.getWarpName()), e.getPlayer());
                    return;
                }

                if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                        || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-invalid"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                List<String> commandList = warp.getCommands();
                for (int i = -1; ++i < commandList.size(); ) {
                    String command = commandList.get(i);
                    if (command.equalsIgnoreCase(enteredCommand)) {
                        getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("command-already-exists"))
                                .replace("{warp}", warp.getWarpName()).replace("{command}", command), e.getPlayer());
                        return;
                    }
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.getCommands().add(enteredCommand);
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("add-command"))
                        .replace("{warp}", warp.getWarpName()).replace("{command}", enteredCommand), e.getPlayer());
                break;
            case "remove-command":
                e.setCancelled(true);
                String enteredIndex = e.getMessage();
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());

                if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("chat-interaction-cancelled"))
                            .replace("{warp}", warp.getWarpName()), e.getPlayer());
                    return;
                }

                if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-invalid"))
                            .replace("{warp}", enteredName), e.getPlayer());
                    return;
                }

                int index;
                if (getPluginInstance().getManager().isNotNumeric(enteredIndex)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("invalid-command-index"))
                            .replace("{command-count}", String.valueOf(warp.getCommands().size())), e.getPlayer());
                    return;
                }

                index = Integer.parseInt(enteredIndex);
                if (index < 1 || index > warp.getCommands().size()) {
                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("invalid-command-index"))
                            .replace("{command-count}", String.valueOf(warp.getCommands().size())), e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.getCommands().remove(index - 1);
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("remove-command"))
                        .replace("{warp}", warp.getWarpName()).replace("{index}", String.valueOf(index)), e.getPlayer());
                break;
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if ((e.getFrom().getBlockX() != Objects.requireNonNull(e.getTo()).getBlockX()) || (e.getFrom().getBlockY() != e.getTo().getBlockY())
                || (e.getFrom().getBlockZ() != e.getTo().getBlockZ()) || !Objects.requireNonNull(e.getFrom().getWorld())
                .getName().equalsIgnoreCase(Objects.requireNonNull(e.getTo().getWorld()).getName())) {
            boolean moveCancellation = getPluginInstance().getConfig()
                    .getBoolean("teleportation-section.move-cancellation");
            if (moveCancellation) {
                GroupTemp groupTemp = getPluginInstance().getTeleportationHandler()
                        .getGroupTemp(e.getPlayer().getUniqueId());
                if (groupTemp != null && !groupTemp.isCancelled()) {
                    groupTemp.setCancelled(true);

                    getPluginInstance().getManager().sendCustomMessage(
                            getPluginInstance().getLangConfig().getString("group-teleport-cancelled"),
                            e.getPlayer());
                    List<UUID> playerList = groupTemp.getAcceptedPlayers();
                    for (int i = -1; ++i < playerList.size(); ) {
                        UUID playerUniqueId = playerList.get(i);
                        if (playerUniqueId == null)
                            continue;

                        Player player = getPluginInstance().getServer().getPlayer(playerUniqueId);
                        if (player == null || !player.isOnline())
                            continue;

                        getPluginInstance().getManager().sendCustomMessage(
                                getPluginInstance().getLangConfig().getString("group-teleport-cancelled"),
                                player);
                    }

                    getPluginInstance().getTeleportationHandler().clearGroupTemp(e.getPlayer());
                    getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(e.getPlayer());
                    getPluginInstance().getTeleportationHandler().getAnimation().stopGroupActiveAnimation(groupTemp);
                    return;
                }

                GroupTemp acceptedGroupTemp = getPluginInstance().getTeleportationHandler()
                        .getAcceptedGroupTemp(e.getPlayer().getUniqueId());
                if (acceptedGroupTemp != null && !acceptedGroupTemp.isCancelled()) {
                    acceptedGroupTemp.setCancelled(true);

                    getPluginInstance().getManager().sendCustomMessage(
                            getPluginInstance().getLangConfig().getString("group-teleport-cancelled"),
                            e.getPlayer());

                    Player gl = getPluginInstance().getServer().getPlayer(
                            getPluginInstance().getTeleportationHandler().getGroupLeader(e.getPlayer().getUniqueId()));
                    if (gl != null && gl.isOnline())
                        getPluginInstance().getManager().sendCustomMessage(
                                getPluginInstance().getLangConfig().getString("group-teleport-cancelled"),
                                gl);

                    List<UUID> playerList = acceptedGroupTemp.getAcceptedPlayers();
                    for (int i = -1; ++i < playerList.size(); ) {
                        UUID playerUniqueId = playerList.get(i);
                        if (playerUniqueId == null
                                || playerUniqueId.toString().equals(e.getPlayer().getUniqueId().toString()))
                            continue;

                        Player player = getPluginInstance().getServer().getPlayer(playerUniqueId);
                        if (player == null || !player.isOnline())
                            continue;

                        getPluginInstance().getManager().sendCustomMessage(
                                getPluginInstance().getLangConfig().getString("group-teleport-cancelled"),
                                player);
                    }

                    getPluginInstance().getTeleportationHandler().clearGroupTemp(e.getPlayer());
                    getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(e.getPlayer());
                    getPluginInstance().getTeleportationHandler().getAnimation()
                            .stopGroupActiveAnimation(acceptedGroupTemp);
                    return;
                }

                if (getPluginInstance().getTeleportationHandler().isTeleporting(e.getPlayer())
                        && getPluginInstance().getTeleportationHandler().getRemainingTime(e.getPlayer()) > 0) {
                    getPluginInstance().getTeleportationHandler().getRandomTeleportingPlayers()
                            .remove(e.getPlayer().getUniqueId());
                    getPluginInstance().getTeleportationHandler().removeTeleportTemp(e.getPlayer());
                    getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(
                            getPluginInstance().getLangConfig().getString("teleportation-cancelled"),
                            e.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            boolean damageCancellation = getPluginInstance().getConfig().getBoolean("teleportation-section.damage-cancellation");
            if (damageCancellation) {
                getPluginInstance().getTeleportationCommands().getTpaSentMap().remove(player.getUniqueId());
                getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);

                GroupTemp groupTemp = getPluginInstance().getTeleportationHandler().getGroupTemp(player.getUniqueId());
                if (groupTemp != null && !groupTemp.isCancelled()) {
                    groupTemp.setCancelled(true);
                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("group-teleport-cancelled"), player);
                    List<UUID> playerList = groupTemp.getAcceptedPlayers();
                    for (int i = -1; ++i < playerList.size(); ) {
                        UUID playerUniqueId = playerList.get(i);
                        if (playerUniqueId == null)
                            continue;

                        Player p = getPluginInstance().getServer().getPlayer(playerUniqueId);
                        if (p == null || !p.isOnline())
                            continue;

                        getPluginInstance().getManager().sendCustomMessage(
                                getPluginInstance().getLangConfig().getString("group-teleport-cancelled"),
                                p);
                    }

                    getPluginInstance().getTeleportationHandler().clearGroupTemp(player);
                    getPluginInstance().getTeleportationHandler().getAnimation().stopGroupActiveAnimation(groupTemp);
                    return;
                }

                GroupTemp acceptedGroupTemp = getPluginInstance().getTeleportationHandler().getAcceptedGroupTemp(player.getUniqueId());
                if (acceptedGroupTemp != null && !acceptedGroupTemp.isCancelled()) {
                    acceptedGroupTemp.setCancelled(true);
                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("group-teleport-cancelled"), player);

                    Player gl = getPluginInstance().getServer().getPlayer(getPluginInstance().getTeleportationHandler().getGroupLeader(player.getUniqueId()));
                    if (gl != null && gl.isOnline())
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("group-teleport-cancelled"), gl);

                    List<UUID> playerList = acceptedGroupTemp.getAcceptedPlayers();
                    for (int i = -1; ++i < playerList.size(); ) {
                        UUID playerUniqueId = playerList.get(i);
                        if (playerUniqueId == null || playerUniqueId.toString().equals(player.getUniqueId().toString()))
                            continue;

                        Player p = getPluginInstance().getServer().getPlayer(playerUniqueId);
                        if (p == null || !p.isOnline())
                            continue;

                        getPluginInstance().getManager().sendCustomMessage(
                                getPluginInstance().getLangConfig().getString("group-teleport-cancelled"),
                                p);
                    }

                    getPluginInstance().getTeleportationHandler().clearGroupTemp(player);
                    getPluginInstance().getTeleportationHandler().getAnimation().stopGroupActiveAnimation(acceptedGroupTemp);
                    return;
                }

                if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
                    getPluginInstance().getTeleportationHandler().getRandomTeleportingPlayers().remove(player.getUniqueId());
                    getPluginInstance().getTeleportationHandler().removeTeleportTemp(player);
                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("teleportation-cancelled"), player);
                }
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerJoinEvent e) {
        if (getPluginInstance().getConfig().getBoolean("general-section.force-spawn"))
            if (!e.getPlayer().hasPlayedBefore() && getPluginInstance().getTeleportationCommands().getFirstJoinLocation() != null) {
                e.getPlayer().setVelocity(new Vector(0, 0, 0));
                e.getPlayer().teleport(getPluginInstance().getTeleportationCommands().getFirstJoinLocation().asBukkitLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);

                String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"),
                        animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
                if (!teleportSound.equalsIgnoreCase(""))
                    e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.valueOf(teleportSound), 1, 1);
                if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
                    String[] themeArgs = animationSet.split(":");
                    getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(e.getPlayer());
                    getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(e.getPlayer(), themeArgs[1],
                            EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                                    .replace("-", "_")), 1);
                }

                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("teleport-first-join-spawn"))
                        .replace("{player}", e.getPlayer().getName()), e.getPlayer());

                List<String> commandList = getPluginInstance().getConfig().getStringList("general-section.first-join-commands");
                for (int i = -1; ++i < commandList.size(); )
                    getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(), commandList.get(i).replace("{player}", e.getPlayer().getName()));
            } else if (!getPluginInstance().getConfig().getBoolean("general-section.force-only-first-join") && getPluginInstance().getTeleportationCommands().getSpawnLocation() != null) {
                e.getPlayer().setVelocity(new Vector(0, 0, 0));
                e.getPlayer().teleport(getPluginInstance().getTeleportationCommands().getSpawnLocation().asBukkitLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);

                String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"),
                        animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
                if (!teleportSound.equalsIgnoreCase(""))
                    e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.valueOf(teleportSound), 1, 1);
                if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
                    String[] themeArgs = animationSet.split(":");
                    getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(e.getPlayer());
                    getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(e.getPlayer(), themeArgs[1],
                            EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                                    .replace("-", "_")), 1);
                }

                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("teleport-spawn"))
                        .replace("{player}", e.getPlayer().getName()), e.getPlayer());
            }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || e.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND)
            getPluginInstance().getTeleportationCommands().updateLastLocation(e.getPlayer(), e.getFrom());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (getPluginInstance().getConfig().getBoolean("general-section.force-death-spawn") && getPluginInstance().getTeleportationCommands().getSpawnLocation() != null) {
            e.getPlayer().setVelocity(new Vector(0, 0, 0));

            Location location = getPluginInstance().getTeleportationCommands().getSpawnLocation().asBukkitLocation();
            e.setRespawnLocation(location);

            String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                    .toUpperCase().replace(" ", "_").replace("-", "_"),
                    animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
            if (!teleportSound.equalsIgnoreCase(""))
                e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.valueOf(teleportSound), 1, 1);
            if (animationSet != null && !animationSet.equalsIgnoreCase("") && animationSet.contains(":")) {
                String[] themeArgs = animationSet.split(":");
                getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(e.getPlayer());
                getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(e.getPlayer(), themeArgs[1],
                        EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                                .replace("-", "_")), 1);
            }

            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("teleport-spawn"))
                    .replace("{player}", e.getPlayer().getName()), e.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        getPluginInstance().getTeleportationCommands().updateLastLocation(e.getEntity(), e.getEntity().getLocation());
    }

    @EventHandler
    public void onSignCreate(SignChangeEvent e) {
        String initialLine = e.getLine(0);
        if (initialLine == null
                || (!initialLine.equalsIgnoreCase("[HyperDrive]") && !initialLine.equalsIgnoreCase("[HD]")))
            return;

        String secondLine = e.getLine(1);
        if (secondLine == null || (!secondLine.equalsIgnoreCase("WARP") && !secondLine.equalsIgnoreCase("RTP")
                && !secondLine.equalsIgnoreCase("GROUP WARP") && !secondLine.equalsIgnoreCase("GROUP RTP")
                && !secondLine.equalsIgnoreCase("GROUP_WARP") && !secondLine.equalsIgnoreCase("GROUP_RTP")
                && !secondLine.equalsIgnoreCase("GROUP-WARP") && !secondLine.equalsIgnoreCase("GROUP-RTP")))
            return;

        if (!e.getPlayer().hasPermission("hyperdrive.use.createsigns")) return;
        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("sign-creation"), e.getPlayer());
        e.setLine(0, getPluginInstance().getManager().colorText(getPluginInstance().getConfig().getString("general-section.sign-header-color")) + initialLine);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && (e.getClickedBlock() != null && (e.getClickedBlock().getType().name().contains("SIGN")))) {
            Sign sign = (Sign) e.getClickedBlock().getState();
            String initialLine = ChatColor.stripColor(sign.getLine(0));
            if (!initialLine.equalsIgnoreCase("[HyperDrive]") && !initialLine.equalsIgnoreCase("[HD]"))
                return;
            if (!e.getPlayer().hasPermission("hyperdrive.use.signs")) {
                getPluginInstance().getManager().sendCustomMessage(
                        getPluginInstance().getLangConfig().getString("no-permission"), e.getPlayer());
                return;
            }

            String secondLine = sign.getLine(1);
            if (!secondLine.equalsIgnoreCase("WARP") && !secondLine.equalsIgnoreCase("RTP")
                    && !secondLine.equalsIgnoreCase("GROUP WARP") && !secondLine.equalsIgnoreCase("GROUP RTP")
                    && !secondLine.equalsIgnoreCase("GROUP_WARP") && !secondLine.equalsIgnoreCase("GROUP_RTP")
                    && !secondLine.equalsIgnoreCase("GROUP-WARP") && !secondLine.equalsIgnoreCase("GROUP-RTP")) {
                getPluginInstance().getManager().sendCustomMessage(
                        getPluginInstance().getLangConfig().getString("sign-action-invalid"),
                        e.getPlayer());
                return;
            }

            String warpName;
            Warp warp;
            switch (secondLine.toLowerCase().replace("_", " ").replace("-", " ")) {
                case "warp":

                    warpName = sign.getLine(2);
                    if (!getPluginInstance().getManager().doesWarpExist(warpName)) {
                        getPluginInstance().getManager().sendCustomMessage(
                                getPluginInstance().getLangConfig().getString("sign-warp-invalid"), player);
                        return;
                    }

                    warp = getPluginInstance().getManager().getWarp(warpName);
                    if (warp.getStatus() == EnumContainer.Status.PUBLIC || (player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())
                            || warp.getAssistants().contains(player.getUniqueId()) || player.hasPermission("hyperdrive.warps." + warpName) || player.hasPermission("hyperdrive.warps.*")
                            || (!warp.getPlayerList().isEmpty() && ((warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode()) || (!warp.getPlayerList().contains(player.getUniqueId()) && !warp.isWhiteListMode()))))) {
                        player.closeInventory();
                        int duration = getPluginInstance().getConfig().getInt("teleportation-section.warp-delay-duration"),
                                cooldown = getPluginInstance().getConfig().getInt("teleportation-section.cooldown-duration");

                        long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp", cooldown);
                        if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
                            getPluginInstance().getManager().sendCustomMessage(Objects
                                    .requireNonNull(
                                            getPluginInstance().getLangConfig().getString("warp-cooldown"))
                                    .replace("{duration}", String.valueOf(currentCooldown)), player);
                            return;
                        }

                        if (getPluginInstance().getConfig().getBoolean("general-section.use-vault")
                                && !player.hasPermission("hyperdrive.economybypass")
                                && !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())
                                && !warp.getAssistants().contains(player.getUniqueId())
                                && (!warp.getPlayerList().isEmpty() && ((warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode()) || (!warp.getPlayerList().contains(player.getUniqueId()) && !warp.isWhiteListMode())))) {
                            EconomyResponse economyResponse = getPluginInstance().getVaultHandler().getEconomy().withdrawPlayer(player,
                                    warp.getUsagePrice());
                            if (!economyResponse.transactionSuccess()) {
                                getPluginInstance().getManager()
                                        .sendCustomMessage(Objects
                                                .requireNonNull(getPluginInstance().getLangConfig().getString("insufficient-funds"))
                                                .replace("{amount}", String.valueOf(warp.getUsagePrice())), player);
                                return;
                            } else
                                getPluginInstance().getManager()
                                        .sendCustomMessage(Objects
                                                .requireNonNull(getPluginInstance().getLangConfig().getString("transaction-success"))
                                                .replace("{amount}", String.valueOf(warp.getUsagePrice())), player);
                        }

                        if (warp.getAnimationSet().contains(":")) {
                            String[] themeArgs = warp.getAnimationSet().split(":");
                            String delayTheme = themeArgs[1];
                            if (delayTheme.contains("/")) {
                                String[] delayThemeArgs = delayTheme.split("/");
                                getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
                                getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, delayThemeArgs[1], EnumContainer.Animation.valueOf(
                                        delayThemeArgs[0].toUpperCase().replace(" ", "_").replace("-", "_")), duration);
                            }
                        }

                        String title = getPluginInstance().getConfig().getString("teleportation-section.start-title"),
                                subTitle = getPluginInstance().getConfig().getString("teleportation-section.start-sub-title");
                        if (title != null && subTitle != null)
                            getPluginInstance().getManager().sendTitle(player, title.replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()),
                                    subTitle.replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()), 0, 5, 0);

                        getPluginInstance().getManager().sendActionBar(player, Objects.requireNonNull(getPluginInstance().getConfig().getString("teleportation-section.start-bar-message"))
                                .replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()));

                        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("teleportation-start"))
                                        .replace("{warp}", warp.getWarpName()).replace("{duration}", String.valueOf(duration)),
                                player);

                        getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "warp", warp.getWarpName(), duration);
                    } else
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
                    return;

                case "group warp":

                    warpName = sign.getLine(2);
                    if (!getPluginInstance().getManager().doesWarpExist(warpName)) {
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("sign-warp-invalid"), player);
                        return;
                    }

                    warp = getPluginInstance().getManager().getWarp(warpName);
                    if (warp.getStatus() == EnumContainer.Status.PUBLIC || ((player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())
                            || warp.getAssistants().contains(player.getUniqueId()) || player.hasPermission("hyperdrive.warps." + warpName)
                            || player.hasPermission("hyperdrive.warps.*") || (!warp.getPlayerList().isEmpty() && ((warp.getPlayerList().contains(player.getUniqueId())
                            && warp.isWhiteListMode()) || (!warp.getPlayerList().contains(player.getUniqueId()) && !warp.isWhiteListMode()))))
                            && player.hasPermission("hyperdrive.groups.use"))) {
                        player.closeInventory();

                        int cooldown = getPluginInstance().getConfig().getInt("teleportation-section.cooldown-duration");
                        long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp", cooldown);
                        if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
                            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-cooldown"))
                                    .replace("{duration}", String.valueOf(currentCooldown)), player);
                            return;
                        }

                        if (getPluginInstance().getConfig().getBoolean("general-section.use-vault") && !player.hasPermission("hyperdrive.economybypass")
                                && !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString()) && !warp.getAssistants().contains(player.getUniqueId())
                                && (!warp.getPlayerList().isEmpty() && ((warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode())
                                || (!warp.getPlayerList().contains(player.getUniqueId()) && !warp.isWhiteListMode())))) {
                            EconomyResponse economyResponse = getPluginInstance().getVaultHandler().getEconomy().withdrawPlayer(player,
                                    warp.getUsagePrice());
                            if (!economyResponse.transactionSuccess()) {
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("insufficient-funds"))
                                        .replace("{amount}", String.valueOf(warp.getUsagePrice())), player);
                                return;
                            } else
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("transaction-success"))
                                        .replace("{amount}", String.valueOf(warp.getUsagePrice())), player);
                        }

                        Destination destination = new Destination(warp.getWarpLocation());
                        destination.setWarp(warp);
                        getPluginInstance().getTeleportationHandler().updateDestination(player, destination);
                        List<UUID> playerList = getPluginInstance().getManager().getPlayerUUIDs();
                        playerList.remove(player.getUniqueId());
                        if (playerList.size() <= 0) {
                            getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-players-found"), player);
                            return;
                        }

                        Inventory inventory = getPluginInstance().getManager().buildPlayerSelectionMenu(player);

                        MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.PLAYER_SELECTION, inventory, player.getPlayer());
                        getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                        if (menuOpenEvent.isCancelled()) return;

                        player.openInventory(inventory);
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("group-selection-start"), player);
                    } else
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
                    return;

                case "rtp":

                    if (!player.hasPermission("hyperdrive.use.rtp")) {
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
                        return;
                    }

                    int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance().getConfig().getInt("teleportation-section.warp-delay-duration") : 0;
                    World world = getPluginInstance().getServer().getWorld(sign.getLine(2));
                    getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "rtp", world != null ? world.getName() : player.getWorld().getName(), duration);
                    break;

                case "group rtp":

                    if (!player.hasPermission("hyperdrive.use.rtpgroup")) {
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
                        return;
                    }

                    List<UUID> onlinePlayers = getPluginInstance().getManager().getPlayerUUIDs();
                    onlinePlayers.remove(player.getUniqueId());
                    if (onlinePlayers.size() <= 0) {
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-players-found"), player);
                        return;
                    }

                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("random-teleport-start"), player);
                    getPluginInstance().getTeleportationHandler().getDestinationMap().remove(player.getUniqueId());
                    getPluginInstance().getTeleportationHandler().updateDestinationWithRandomLocation(player, player.getLocation(), player.getWorld());
                    player.openInventory(getPluginInstance().getManager().buildPlayerSelectionMenu(player));
                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("player-selection-group"), player);
                    break;

                default:
                    break;
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        List<String> commandStrings = getPluginInstance().getConfig().getStringList("general-section.custom-alias-commands");
        for (int i = -1; ++i < commandStrings.size(); ) {
            String commandString = commandStrings.get(i);
            if (!commandString.contains(":")) continue;

            String[] args = commandString.split(":");
            if (e.getMessage().equalsIgnoreCase(args[0]) && args[2].equalsIgnoreCase("player"))
                e.setMessage(args[1].replace("{player}", e.getPlayer().getName()));
            else if (e.getMessage().equalsIgnoreCase(args[0]) && args[2].equalsIgnoreCase("console")) {
                e.setCancelled(true);
                getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(), args[1].replace("{player}", e.getPlayer().getName()));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        getPluginInstance().getManager().getPaging().getWarpPageMap().remove(e.getPlayer().getUniqueId());
        getPluginInstance().getManager().getPaging().getPlayerSelectedMap().remove(e.getPlayer().getUniqueId());
        getPluginInstance().getManager().getPaging().getCurrentPageMap().remove(e.getPlayer().getUniqueId());
        getPluginInstance().getTeleportationCommands().getToggledPlayers().remove(e.getPlayer().getUniqueId());
        getPluginInstance().getTeleportationCommands().getLastLocationMap().remove(e.getPlayer().getUniqueId());
        getPluginInstance().getTeleportationCommands().getTpaHereSentPlayers().remove(e.getPlayer().getUniqueId());
    }

    // methods
    private void runListMenuClick(Player player, InventoryClickEvent e) {
        if (e.getCurrentItem() != null && Objects.requireNonNull(e.getClickedInventory()).getType() != InventoryType.PLAYER) {
            e.setCancelled(true);
            if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick() == ClickType.CREATIVE) return;

            List<Integer> warpSlots = getPluginInstance().getMenusConfig().getIntegerList("list-menu-section.warp-slots");
            if (warpSlots.contains(e.getSlot()) && e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()) {
                ClickType clickType = e.getClick();
                String warpName = ChatColor
                        .stripColor(Objects.requireNonNull(e.getCurrentItem().getItemMeta()).getDisplayName());
                Warp warp = getPluginInstance().getManager().getWarp(warpName);

                if (warp != null) {

                    String soundName = getPluginInstance().getConfig().getString("warp-icon-section.click-sound");
                    if (soundName != null && !soundName.equalsIgnoreCase(""))
                        player.getWorld().playSound(player.getLocation(),
                                Sound.valueOf(soundName.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);

                    switch (clickType) {
                        case LEFT:

                            if (warp.getStatus() == EnumContainer.Status.PUBLIC || (player.hasPermission("hyperdrive.warps." + warpName) || player.hasPermission("hyperdrive.warps.*"))
                                    || warp.getOwner().toString().equals(player.getUniqueId().toString()) || warp.getAssistants().contains(player.getUniqueId())
                                    || (!warp.getPlayerList().isEmpty() && ((warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode())
                                    || (!warp.getPlayerList().contains(player.getUniqueId()) && !warp.isWhiteListMode())))) {
                                player.closeInventory();

                                if (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")) {
                                    String serverIP = (getPluginInstance().getServer().getIp().equalsIgnoreCase("") || getPluginInstance().getServer().getIp().equalsIgnoreCase("0.0.0.0"))
                                            ? getPluginInstance().getConfig().getString("mysql-connection.default-ip") + ":" + getPluginInstance().getServer().getPort()
                                            : (getPluginInstance().getServer().getIp().replace("localhost", "127.0.0.1") + ":" + getPluginInstance().getServer().getPort());

                                    if (!warp.getServerIPAddress().equalsIgnoreCase(serverIP)) {
                                        String server = getPluginInstance().getBungeeListener().getServerName(warp.getServerIPAddress());
                                        if (server == null) {
                                            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("ip-ping-fail"))
                                                    .replace("{warp}", warp.getWarpName()).replace("{ip}", warp.getServerIPAddress()), player);
                                            return;
                                        }
                                    }
                                }

                                int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance().getConfig().getInt("teleportation-section.warp-delay-duration") : 0,
                                        cooldown = getPluginInstance().getConfig().getInt("teleportation-section.cooldown-duration");

                                long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp", cooldown);
                                if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
                                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-cooldown"))
                                            .replace("{duration}", String.valueOf(currentCooldown)), player);
                                    return;
                                }

                                if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
                                    player.closeInventory();
                                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("already-teleporting"), player);
                                    return;
                                }

                                if (getPluginInstance().getConfig().getBoolean("general-section.use-vault") && !player.hasPermission("hyperdrive.economybypass")
                                        && !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString()) && !warp.getAssistants().contains(player.getUniqueId())
                                        && (!warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode())) {
                                    EconomyChargeEvent economyChargeEvent = new EconomyChargeEvent(player, warp.getUsagePrice());
                                    getPluginInstance().getServer().getPluginManager().callEvent(economyChargeEvent);
                                    if (!economyChargeEvent.isCancelled()) {
                                        if (!getPluginInstance().getVaultHandler().getEconomy().has(player, economyChargeEvent.getAmount())) {
                                            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("insufficient-funds"))
                                                    .replace("{amount}", String.valueOf(economyChargeEvent.getAmount())), player);
                                            return;
                                        }

                                        getPluginInstance().getVaultHandler().getEconomy().withdrawPlayer(player, warp.getUsagePrice());
                                        if (warp.getOwner() != null) {
                                            OfflinePlayer owner = getPluginInstance().getServer().getOfflinePlayer(warp.getOwner());
                                            getPluginInstance().getVaultHandler().getEconomy().depositPlayer(owner, warp.getUsagePrice());
                                        }

                                        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("transaction-success"))
                                                .replace("{amount}", String.valueOf(economyChargeEvent.getAmount())), player);
                                    }
                                }

                                if (warp.getAnimationSet().contains(":")) {
                                    String[] themeArgs = warp.getAnimationSet().split(":");
                                    String delayTheme = themeArgs[1];
                                    if (delayTheme.contains("/")) {
                                        String[] delayThemeArgs = delayTheme.split("/");
                                        getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
                                        getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, delayThemeArgs[1], EnumContainer.Animation.valueOf(delayThemeArgs[0]
                                                .toUpperCase().replace(" ", "_").replace("-", "_")), duration);
                                    }
                                }

                                String title = getPluginInstance().getConfig().getString("teleportation-section.start-title"),
                                        subTitle = getPluginInstance().getConfig().getString("teleportation-section.start-sub-title");
                                if (title != null && subTitle != null)
                                    getPluginInstance().getManager().sendTitle(player, title.replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()),
                                            subTitle.replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()), 0, 5, 0);

                                getPluginInstance().getManager().sendActionBar(player, Objects.requireNonNull(getPluginInstance().getConfig().getString("teleportation-section.start-bar-message"))
                                        .replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()));

                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("teleportation-start"))
                                        .replace("{warp}", warp.getWarpName()).replace("{duration}", String.valueOf(duration)), player);

                                getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "warp", warp.getWarpName(), duration);
                                return;
                            }

                            break;

                        case RIGHT:

                            if ((warp.getStatus() == EnumContainer.Status.PUBLIC || player.hasPermission("hyperdrive.warps." + warpName) || player.hasPermission("hyperdrive.warps.*")
                                    || warp.getOwner().toString().equals(player.getUniqueId().toString()) || warp.getAssistants().contains(player.getUniqueId())
                                    || (!warp.getPlayerList().isEmpty() && ((warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode()) || (!warp.getPlayerList().contains(player.getUniqueId()) && !warp.isWhiteListMode()))))
                                    && player.hasPermission("hyperdrive.groups.use")) {
                                player.closeInventory();

                                if (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")) {
                                    String serverIP = (getPluginInstance().getServer().getIp().equalsIgnoreCase("") || getPluginInstance().getServer().getIp().equalsIgnoreCase("0.0.0.0"))
                                            ? getPluginInstance().getConfig().getString("mysql-connection.default-ip") + ":" + getPluginInstance().getServer().getPort()
                                            : (getPluginInstance().getServer().getIp().replace("localhost", "127.0.0.1") + ":" + getPluginInstance().getServer().getPort());

                                    if (!warp.getServerIPAddress().equalsIgnoreCase(serverIP)) {
                                        String server = getPluginInstance().getBungeeListener().getServerName(warp.getServerIPAddress());
                                        if (server == null) {
                                            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("ip-ping-fail"))
                                                    .replace("{warp}", warp.getWarpName()).replace("{ip}", warp.getServerIPAddress()), player);
                                            return;
                                        }
                                    }
                                }

                                final int cooldown = getPluginInstance().getConfig().getInt("teleportation-section.cooldown-duration");
                                long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp", cooldown);
                                if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
                                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-cooldown"))
                                            .replace("{duration}", String.valueOf(currentCooldown)), player);
                                    return;
                                }

                                if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
                                    player.closeInventory();
                                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("already-teleporting"), player);
                                    return;
                                }

                                if (getPluginInstance().getConfig().getBoolean("general-section.use-vault") && !player.hasPermission("hyperdrive.economybypass")
                                        && !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString()) && !warp.getAssistants().contains(player.getUniqueId())
                                        && (!warp.getPlayerList().isEmpty() && !warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode())) {
                                    EconomyChargeEvent economyChargeEvent = new EconomyChargeEvent(player, warp.getUsagePrice());
                                    getPluginInstance().getServer().getPluginManager().callEvent(economyChargeEvent);
                                    if (!economyChargeEvent.isCancelled()) {
                                        if (!getPluginInstance().getVaultHandler().getEconomy().has(player, economyChargeEvent.getAmount())) {
                                            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("insufficient-funds"))
                                                    .replace("{amount}", String.valueOf(economyChargeEvent.getAmount())), player);
                                            return;
                                        }

                                        getPluginInstance().getVaultHandler().getEconomy().withdrawPlayer(player, economyChargeEvent.getAmount());
                                        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("transaction-success"))
                                                .replace("{amount}", String.valueOf(economyChargeEvent.getAmount())), player);
                                    }
                                }

                                Destination destination = new Destination(warp.getWarpLocation());
                                destination.setWarp(warp);
                                getPluginInstance().getTeleportationHandler().updateDestination(player, destination);
                                List<UUID> playerList = getPluginInstance().getManager().getPlayerUUIDs();
                                playerList.remove(player.getUniqueId());
                                if (playerList.size() <= 0) {
                                    getPluginInstance().getManager().sendCustomMessage(
                                            getPluginInstance().getLangConfig().getString("no-players-found"),
                                            player);
                                    return;
                                }

                                Inventory inventory = getPluginInstance().getManager().buildPlayerSelectionMenu(player);

                                MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(),
                                        EnumContainer.MenuType.PLAYER_SELECTION, inventory, player.getPlayer());
                                getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                                if (menuOpenEvent.isCancelled())
                                    return;

                                player.openInventory(inventory);
                                getPluginInstance().getManager().sendCustomMessage(
                                        getPluginInstance().getLangConfig().getString("group-selection-start"),
                                        player);
                                return;
                            }

                            break;

                        case SHIFT_LEFT:

                            if (player.hasPermission("hyperdrive.like")) {
                                if ((warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString())) && !warp.getAssistants().contains(player.getUniqueId())) {
                                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("like-own")), player);
                                    return;
                                }

                                if (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")) {
                                    String serverIP = (getPluginInstance().getServer().getIp().equalsIgnoreCase("") || getPluginInstance().getServer().getIp().equalsIgnoreCase("0.0.0.0"))
                                            ? getPluginInstance().getConfig().getString("mysql-connection.default-ip") + ":" + getPluginInstance().getServer().getPort()
                                            : (getPluginInstance().getServer().getIp().replace("localhost", "127.0.0.1") + ":" + getPluginInstance().getServer().getPort());

                                    if (!warp.getServerIPAddress().equalsIgnoreCase(serverIP)) {
                                        getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("like-incorrect-server"))
                                                .replace("{warp}", warp.getWarpName()), player);
                                        return;
                                    }
                                }

                                Inventory inventory = getPluginInstance().getManager().buildLikeMenu(warp);
                                MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.LIKE, inventory, player.getPlayer());
                                getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                                if (menuOpenEvent.isCancelled()) return;

                                player.openInventory(inventory);
                                return;
                            }

                            break;
                        case SHIFT_RIGHT:

                            if (player.hasPermission("hyperdrive.admin.edit") || player.hasPermission("hyperdrive.edit.*") || player.hasPermission("hyperdrive.edit." + warpName)
                                    || player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString()) || warp.getAssistants().contains(player.getUniqueId())) {
                                Inventory inventory = getPluginInstance().getManager().buildEditMenu(player, warp);

                                MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.EDIT, inventory, player.getPlayer());
                                getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                                if (menuOpenEvent.isCancelled()) return;

                                player.openInventory(inventory);
                                return;
                            }

                            break;

                        default:
                            break;
                    }
                }

                return;
            }

            String itemId = getPluginInstance().getManager().getIdFromSlot("list-menu-section", e.getSlot());
            if (itemId != null) {
                if (getPluginInstance().getMenusConfig().getBoolean("list-menu-section.items." + itemId + ".click-sound")) {
                    String soundName = getPluginInstance().getMenusConfig().getString("list-menu-section.items." + itemId + ".sound-name");
                    if (soundName != null && !soundName.equalsIgnoreCase(""))
                        player.playSound(player.getLocation(),
                                Sound.valueOf(soundName.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);
                }

                ConfigurationSection cs = getPluginInstance().getMenusConfig().getConfigurationSection("list-menu-section.items." + itemId);
                if (cs != null && cs.getKeys(false).contains("permission")) {
                    String permission = getPluginInstance().getMenusConfig().getString("list-menu-section.items." + itemId + ".permission");
                    if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
                        return;
                    }
                }

                String ownFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.own-status-format"), publicFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.public-status-format"),
                        privateFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.private-status-format"), adminFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.admin-status-format"),
                        featuredFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.featured-status-format"), clickAction = getPluginInstance().getMenusConfig().getString("list-menu-section.items." + itemId + ".click-action");
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("list-menu-section.items." + itemId + ".click-message"))
                        .replace("{player}", player.getName()).replace("{item-id}", itemId), player);

                if (clickAction != null) {
                    String action = clickAction, value = "";
                    if (clickAction.contains(":")) {
                        String[] actionArgs = clickAction.toLowerCase().split(":");
                        action = actionArgs[0].replace(" ", "-").replace("_", "-");
                        value = actionArgs[1].replace("{player}", player.getName());
                    }

                    final double itemUsageCost = getPluginInstance().getMenusConfig().getDouble("list-menu-section.items." + itemId + ".usage-cost");
                    HashMap<Integer, List<Warp>> warpPageMap;
                    int currentPage;
                    List<Warp> pageWarpList;
                    switch (action) {
                        case "dispatch-command-console":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(), value);
                            }

                            break;
                        case "dispatch-command-player":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                getPluginInstance().getServer().dispatchCommand(player, value);
                            }

                            break;
                        case "create-warp":
                            player.closeInventory();

                            if (getPluginInstance().getManager().isBlockedWorld(player.getWorld())) {
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("blocked-world"), player);
                                return;
                            }

                            if (!getPluginInstance().getHookChecker().isLocationHookSafe(player, player.getLocation())) {
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("not-hook-safe"), player);
                                break;
                            }

                            if (getPluginInstance().getManager().hasMetWarpLimit(player)) {
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("warp-limit-met"), player);
                                return;
                            }

                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "create-warp", "", itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("create-warp-interaction"))
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"))).replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);
                            break;
                        case "refresh":
                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) return;

                            getPluginInstance().getManager().getPaging().resetWarpPages(player);
                            String currentStatus = getPluginInstance().getManager().getCurrentFilterStatus("list-menu-section", e.getInventory());
                            warpPageMap = getPluginInstance().getManager().getPaging().getWarpPages(player, "list-menu-section", currentStatus);
                            getPluginInstance().getManager().getPaging().getWarpPageMap().put(player.getUniqueId(), warpPageMap);
                            currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
                            pageWarpList = new ArrayList<>();
                            if (warpPageMap != null && !warpPageMap.isEmpty() && warpPageMap.containsKey(currentPage))
                                pageWarpList = new ArrayList<>(warpPageMap.get(currentPage));

                            if (!pageWarpList.isEmpty())
                                for (int i = -1; ++i < warpSlots.size(); ) {
                                    int warpSlot = warpSlots.get(i);
                                    e.getInventory().setItem(warpSlot, null);

                                    if (pageWarpList.size() >= 1) {
                                        Warp warp = pageWarpList.get(0);
                                        e.getInventory().setItem(warpSlots.get(i),
                                                getPluginInstance().getManager().buildWarpIcon(player, warp));
                                        pageWarpList.remove(warp);
                                    }
                                }
                            else
                                getPluginInstance().getManager().sendCustomMessage(
                                        getPluginInstance().getLangConfig().getString("refresh-fail"), player);

                            break;
                        case "next-page":
                            if (getPluginInstance().getManager().getPaging().hasNextWarpPage(player) && getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) {
                                warpPageMap = getPluginInstance().getManager().getPaging().getWarpPages(player, "list-menu-section",
                                        getPluginInstance().getManager().getCurrentFilterStatus("list-menu-section", e.getInventory()));
                                getPluginInstance().getManager().getPaging().getWarpPageMap().put(player.getUniqueId(), warpPageMap);
                                currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
                                pageWarpList = new ArrayList<>();
                                if (warpPageMap != null && !warpPageMap.isEmpty() && warpPageMap.containsKey(currentPage + 1))
                                    pageWarpList = new ArrayList<>(warpPageMap.get(currentPage + 1));

                                if (!pageWarpList.isEmpty()) {
                                    getPluginInstance().getManager().getPaging().updateCurrentWarpPage(player, true);
                                    for (int i = -1; ++i < warpSlots.size(); ) {
                                        e.getInventory().setItem(warpSlots.get(i), null);
                                        if (pageWarpList.size() >= 1) {
                                            Warp warp = pageWarpList.get(0);
                                            e.getInventory().setItem(warpSlots.get(i),
                                                    getPluginInstance().getManager().buildWarpIcon(player, warp));
                                            pageWarpList.remove(warp);
                                        }
                                    }
                                }
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-next-page"), player);
                            break;
                        case "previous-page":
                            if (getPluginInstance().getManager().getPaging().hasPreviousWarpPage(player) && getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) {
                                warpPageMap = getPluginInstance().getManager().getPaging().getCurrentWarpPages(player) == null ? getPluginInstance().getManager().getPaging().getWarpPages(player,
                                        "list-menu-section", getPluginInstance().getManager().getCurrentFilterStatus("list-menu-section", e.getInventory()))
                                        : getPluginInstance().getManager().getPaging().getCurrentWarpPages(player);
                                getPluginInstance().getManager().getPaging().getWarpPageMap().put(player.getUniqueId(), warpPageMap);

                                currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
                                pageWarpList = new ArrayList<>();
                                if (warpPageMap != null && !warpPageMap.isEmpty()
                                        && warpPageMap.containsKey(currentPage - 1))
                                    pageWarpList = new ArrayList<>(warpPageMap.get(currentPage - 1));

                                if (!pageWarpList.isEmpty()) {
                                    getPluginInstance().getManager().getPaging().updateCurrentWarpPage(player, false);
                                    for (int i = -1; ++i < warpSlots.size(); ) {
                                        e.getInventory().setItem(warpSlots.get(i), null);
                                        if (pageWarpList.size() >= 1) {
                                            Warp warp = pageWarpList.get(0);
                                            e.getInventory().setItem(warpSlots.get(i),
                                                    getPluginInstance().getManager().buildWarpIcon(player, warp));
                                            pageWarpList.remove(warp);
                                        }
                                    }
                                }
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-previous-page"), player);
                            break;
                        case "filter-switch":
                            String statusFromItem = getPluginInstance().getManager().getFilterStatusFromItem(e.getCurrentItem(), "list-menu-section", itemId);
                            if (statusFromItem != null && getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) {
                                int index = -1;
                                boolean isOwnFormat = statusFromItem.equalsIgnoreCase(ownFormat), isPublicFormat = statusFromItem.equalsIgnoreCase(publicFormat), isPrivateFormat = statusFromItem.equalsIgnoreCase(privateFormat),
                                        isAdminFormat = statusFromItem.equalsIgnoreCase(adminFormat), isFeaturedFormat = statusFromItem.equalsIgnoreCase(adminFormat);

                                if (isPublicFormat || statusFromItem.equalsIgnoreCase(EnumContainer.Status.PUBLIC.name()))
                                    index = 0;
                                else if (isPrivateFormat
                                        || statusFromItem.equalsIgnoreCase(EnumContainer.Status.PRIVATE.name()))
                                    index = 1;
                                else if (isAdminFormat
                                        || statusFromItem.equalsIgnoreCase(EnumContainer.Status.ADMIN.name()))
                                    index = 2;
                                else if (isOwnFormat)
                                    index = 3;
                                else if (isFeaturedFormat)
                                    index = 4;

                                int nextIndex = index + 1;
                                String nextStatus;

                                if (nextIndex == 1)
                                    nextStatus = EnumContainer.Status.PRIVATE.name();
                                else if (nextIndex == 2)
                                    nextStatus = EnumContainer.Status.ADMIN.name();
                                else if (nextIndex == 3)
                                    nextStatus = ownFormat;
                                else if (nextIndex == 4)
                                    nextStatus = featuredFormat;
                                else
                                    nextStatus = EnumContainer.Status.PUBLIC.name();

                                ItemStack filterItem = getPluginInstance().getManager().buildItemFromId(player,
                                        Objects.requireNonNull(nextStatus), "list-menu-section", itemId);
                                e.getInventory().setItem(e.getSlot(), filterItem);

                                for (int i = -1; ++i < warpSlots.size(); ) {
                                    int warpSlot = warpSlots.get(i);
                                    e.getInventory().setItem(warpSlot, null);
                                }

                                getPluginInstance().getManager().getPaging().resetWarpPages(player);
                                warpPageMap = getPluginInstance().getManager().getPaging().getWarpPages(player, "list-menu-section", nextStatus);
                                getPluginInstance().getManager().getPaging().getWarpPageMap().put(player.getUniqueId(), warpPageMap);
                                currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
                                pageWarpList = new ArrayList<>();
                                if (warpPageMap != null && !warpPageMap.isEmpty() && warpPageMap.containsKey(currentPage))
                                    pageWarpList = new ArrayList<>(warpPageMap.get(currentPage));

                                if (!pageWarpList.isEmpty())
                                    for (int i = -1; ++i < warpSlots.size(); ) {
                                        if (pageWarpList.size() >= 1) {
                                            Warp warp = pageWarpList.get(0);
                                            e.getInventory().setItem(warpSlots.get(i), getPluginInstance().getManager().buildWarpIcon(player, warp));
                                            pageWarpList.remove(warp);
                                        }
                                    }
                            }

                            break;
                        case "open-custom-menu":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;

                                Inventory inventory = getPluginInstance().getManager().buildCustomMenu(player, value);
                                MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.CUSTOM, inventory, player.getPlayer());
                                menuOpenEvent.setCustomMenuId(value);
                                getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                                if (menuOpenEvent.isCancelled()) return;

                                player.openInventory(inventory);
                            }

                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    private void runEditMenuClick(Player player, String inventoryName, InventoryClickEvent e) {
        if (e.getCurrentItem() != null && Objects.requireNonNull(e.getClickedInventory()).getType() != InventoryType.PLAYER) {
            e.setCancelled(true);
            if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick() == ClickType.CREATIVE) return;

            String itemId = getPluginInstance().getManager().getIdFromSlot("edit-menu-section", e.getSlot());
            if (itemId != null) {
                if (getPluginInstance().getMenusConfig().getBoolean("edit-menu-section.items." + itemId + ".click-sound")) {
                    String soundName = getPluginInstance().getMenusConfig().getString("edit-menu-section.items." + itemId + ".sound-name");
                    if (soundName != null && !soundName.equalsIgnoreCase(""))
                        player.playSound(player.getLocation(),
                                Sound.valueOf(soundName.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);
                }

                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("edit-menu-section.items." + itemId + ".click-message"))
                        .replace("{player}", player.getName()).replace("{item-id}", itemId), player);

                String clickAction = getPluginInstance().getMenusConfig().getString("edit-menu-section.items." + itemId + ".click-action"),
                        toggleFormat = getPluginInstance().getConfig().getString("general-section.option-toggle-format"),
                        warpName = Objects.requireNonNull(ChatColor.stripColor(inventoryName)).replace(ChatColor.stripColor(getPluginInstance().getManager().colorText(
                                getPluginInstance().getMenusConfig().getString("edit-menu-section.title"))), "");
                Warp warp = getPluginInstance().getManager().getWarp(warpName);

                ConfigurationSection cs = getPluginInstance().getMenusConfig().getConfigurationSection("edit-menu-section.items." + itemId);
                if (cs != null && cs.getKeys(false).contains("permission")) {
                    String permission = getPluginInstance().getMenusConfig().getString("edit-menu-section.items." + itemId + ".permission");
                    if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
                        return;
                    }
                }

                if (clickAction != null) {

                    double itemUsageCost = getPluginInstance().getMenusConfig().getDouble("edit-menu-section.items." + itemId + ".usage-cost");

                    String value = "";
                    if (clickAction.contains(":")) {
                        String[] actionArgs = clickAction.toLowerCase().split(":");
                        clickAction = actionArgs[0].replace(" ", "-").replace("_", "-");
                        value = actionArgs[1].replace("{player}", player.getName());
                    }

                    String publicFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.public-status-format"),
                            privateFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.private-status-format"),
                            adminFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.admin-status-format");
                    boolean useMySQL = getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql");

                    switch (clickAction) {
                        case "dispatch-command-console":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(), value);
                            }

                            break;

                        case "dispatch-command-player":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                getPluginInstance().getServer().dispatchCommand(player, value);
                            }
                            break;

                        case "open-custom-menu":

                            if (!value.equalsIgnoreCase("")) {
                                if (value.equalsIgnoreCase("List Menu") || value.equalsIgnoreCase("List-Menu")) {
                                    player.closeInventory();
                                    if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                        return;

                                    Inventory inventory = getPluginInstance().getManager().buildListMenu(player);
                                    MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.LIST, inventory, player.getPlayer());
                                    getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                                    if (menuOpenEvent.isCancelled())
                                        return;

                                    player.openInventory(inventory);
                                    return;
                                } else {
                                    player.closeInventory();
                                    if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                        return;

                                    Inventory inventory = getPluginInstance().getManager().buildCustomMenu(player, value);
                                    MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.CUSTOM, inventory, player.getPlayer());
                                    menuOpenEvent.setCustomMenuId(value);
                                    getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                                    if (menuOpenEvent.isCancelled())
                                        return;

                                    player.openInventory(inventory);
                                    return;
                                }
                            }

                            break;
                        case "rename":

                            player.closeInventory();
                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "rename", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("rename-warp-interaction"))
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                        .replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);

                            break;

                        case "delete":

                            player.closeInventory();
                            if (((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName())))) {
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;

                                warp.unRegister();
                                warp.deleteSaved(true);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-deleted"))
                                        .replace("{warp}", warp.getWarpName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-no-longer-exists"))
                                        .replace("{warp}", warp.getWarpName()), player);

                            break;

                        case "relocate":
                            player.closeInventory();
                            if (getPluginInstance().getManager().isBlockedWorld(player.getWorld())) {
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("blocked-world"), player);
                                return;
                            }

                            if (((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName())))) {
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;

                                warp.setWarpLocation(player.getLocation());
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-relocated"))
                                        .replace("{warp}", warp.getWarpName()).replace("{x}", String.valueOf((int) warp.getWarpLocation().getX()))
                                        .replace("{y}", String.valueOf((int) warp.getWarpLocation().getY())).replace("{z}", String.valueOf((int) warp.getWarpLocation().getZ()))
                                        .replace("{world}", warp.getWarpLocation().getWorldName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-invalid"))
                                        .replace("{warp}", warp.getWarpName()), player);

                            player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                            break;

                        case "change-status":

                            player.closeInventory();
                            if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;

                                EnumContainer.Status[] statusList = EnumContainer.Status.values();
                                EnumContainer.Status nextStatus = EnumContainer.Status.PUBLIC, previousStatus = warp.getStatus();
                                for (int i = -1; ++i < statusList.length; ) {
                                    EnumContainer.Status status = statusList[i];
                                    if (status == previousStatus) {
                                        int nextIndex = (i + 1);
                                        nextStatus = statusList[nextIndex >= statusList.length ? 0 : nextIndex];
                                        if (!player.hasPermission("hyperdrive.admin.status") && nextStatus == EnumContainer.Status.ADMIN)
                                            nextStatus = statusList[0];
                                        break;
                                    }
                                }

                                warp.setStatus(nextStatus);
                                String nextStatusName, previousStatusName;

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

                                switch (previousStatus) {
                                    case PRIVATE:
                                        previousStatusName = privateFormat;
                                        break;
                                    case ADMIN:
                                        previousStatusName = adminFormat;
                                        break;
                                    default:
                                        previousStatusName = publicFormat;
                                        break;
                                }

                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-status-changed"))
                                        .replace("{warp}", warp.getWarpName())
                                        .replace("{next-status}", Objects.requireNonNull(nextStatusName))
                                        .replace("{previous-status}", Objects.requireNonNull(previousStatusName)), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-no-longer-exists"))
                                        .replace("{warp}", warp.getWarpName()), player);

                            player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                            break;

                        case "give-ownership":

                            player.closeInventory();
                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "give-ownership", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("give-ownership-interaction"))
                                        .replace("{warp}", warp != null ? warp.getWarpName() : "")
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                        .replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);

                            break;

                        case "edit-description":

                            player.closeInventory();
                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "edit-description", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("edit-description-interaction"))
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                        .replace("{clear}", Objects.requireNonNull(getPluginInstance().getConfig().getString("warp-icon-section.description-clear-symbol")))
                                        .replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);

                            break;

                        case "change-name-color":

                            player.closeInventory();
                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "change-name-color", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("change-name-color-interaction"))
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                        .replace("{colors}", getPluginInstance().getManager().getColorNames().toString())
                                        .replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);

                            break;

                        case "change-description-color":

                            player.closeInventory();
                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "change-description-color", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("change-description-color-interaction"))
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                        .replace("{colors}", getPluginInstance().getManager().getColorNames().toString())
                                        .replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);

                            break;

                        case "change-usage-price":

                            player.closeInventory();
                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "change-usage-price", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("change-usage-price-interaction"))
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                        .replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);

                            break;

                        case "give-assistant":

                            player.closeInventory();
                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "give-assistant", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("give-assistant-interaction"))
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                        .replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);

                            break;

                        case "remove-assistant":

                            player.closeInventory();
                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "remove-assistant", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("remove-assistant-interaction"))
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                        .replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);

                            break;

                        case "add-to-list":

                            player.closeInventory();
                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "add-to-list", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("add-list-interaction"))
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                        .replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);

                            break;

                        case "remove-from-list":

                            player.closeInventory();
                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "remove-from-list", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("remove-list-interaction"))
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                        .replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);

                            break;

                        case "toggle-white-list":

                            player.closeInventory();
                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) return;

                            warp.setWhiteListMode(!warp.isWhiteListMode());
                            getPluginInstance().getManager().sendCustomMessage(warp.isWhiteListMode() ? Objects.requireNonNull(getPluginInstance().getLangConfig().getString("white-list"))
                                    : Objects.requireNonNull(getPluginInstance().getLangConfig().getString("black-list"))
                                    .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                    .replace("{player}", player.getName()), player);

                            player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                            break;

                        case "add-command":

                            player.closeInventory();
                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "add-command", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("add-command-interaction"))
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                        .replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);

                            break;

                        case "remove-command":

                            player.closeInventory();
                            if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                getPluginInstance().getManager().updateChatInteraction(player, "remove-command", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("remove-command-interaction"))
                                        .replace("{cancel}", Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel")))
                                        .replace("{player}", player.getName()), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("interaction-already-active"), player);

                            break;

                        case "toggle-enchant-look":

                            player.closeInventory();
                            if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;

                                warp.setIconEnchantedLook(!warp.hasIconEnchantedLook());
                                if (toggleFormat != null && toggleFormat.contains(":")) {
                                    String[] toggleFormatArgs = toggleFormat.split(":");
                                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("enchanted-look-toggle"))
                                            .replace("{warp}", warp.getWarpName()).replace("{status}", warp.hasIconEnchantedLook() ? toggleFormatArgs[0] : toggleFormatArgs[1]), player);
                                } else
                                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("enchanted-look-toggle"))
                                            .replace("{warp}", warp.getWarpName()).replace("{status}", warp.hasIconEnchantedLook() ? "&aEnabled" : "&cDisabled"), player);
                            } else {
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-no-longer-exists"))
                                        .replace("{warp}", warp.getWarpName()), player);
                            }

                            player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                            break;

                        case "change-icon":

                            player.closeInventory();
                            if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                                ItemStack handItem = getPluginInstance().getManager().getHandItem(player);

                                if (handItem == null || handItem.getType() == Material.AIR) {
                                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("invalid-item"))
                                            .replace("{warp}", warp.getWarpName()), player);
                                    return;
                                }

                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                warp.setIconTheme(handItem.getType().name() + "," + handItem.getDurability() + "," + handItem.getAmount());
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("icon-changed"))
                                        .replace("{warp}", warp.getWarpName()), player);
                            }

                            player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                            break;

                        case "change-animation-set":

                            player.closeInventory();
                            if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;

                                String nextAnimationSet = getPluginInstance().getManager().getNextAnimationSet(warp);
                                if (nextAnimationSet != null) {
                                    warp.setAnimationSet(nextAnimationSet);
                                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(
                                            getPluginInstance().getLangConfig().getString("animation-set-changed"))
                                            .replace("{warp}", warp.getWarpName())
                                            .replace("{animation-set}", nextAnimationSet.split(":")[0]), player);
                                } else
                                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("animation-set-invalid"), player);
                            } else {
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("warp-no-longer-exists"))
                                        .replace("{warp}", warp.getWarpName()), player);
                            }

                            player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                            break;

                        default:
                            break;
                    }
                }
            }
        }
    }

    private void runLikeMenuClick(Player player, String inventoryName, InventoryClickEvent e) {
        if (e.getCurrentItem() != null && Objects.requireNonNull(e.getClickedInventory()).getType() != InventoryType.PLAYER) {
            e.setCancelled(true);
            if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick() == ClickType.CREATIVE) return;

            String itemId = getPluginInstance().getManager().getIdFromSlot("like-menu-section", e.getSlot());
            if (itemId != null) {
                if (getPluginInstance().getConfig().getBoolean("like-menu-section.items." + itemId + ".click-sound")) {
                    String soundName = getPluginInstance().getMenusConfig().getString("like-menu-section.items." + itemId + ".sound-name");
                    if (soundName != null && !soundName.equalsIgnoreCase(""))
                        player.playSound(player.getLocation(),
                                Sound.valueOf(soundName.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);
                }

                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("like-menu-section.items." + itemId + ".click-message"))
                        .replace("{player}", player.getName()).replace("{item-id}", itemId), player);

                String clickAction = getPluginInstance().getMenusConfig().getString("like-menu-section.items." + itemId + ".click-action"),
                        warpName = Objects.requireNonNull(ChatColor.stripColor(inventoryName)).replace(ChatColor.stripColor(getPluginInstance().getManager().colorText(
                                getPluginInstance().getMenusConfig().getString("like-menu-section.title"))), "");
                Warp warp = getPluginInstance().getManager().getWarp(warpName);

                ConfigurationSection cs = getPluginInstance().getMenusConfig().getConfigurationSection("like-menu-section.items." + itemId);
                if (cs != null && cs.getKeys(false).contains("permission")) {
                    String permission = getPluginInstance().getMenusConfig().getString("like-menu-section.items." + itemId + ".permission");
                    if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
                        getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-permission"), player);
                        return;
                    }
                }

                if (clickAction != null) {
                    double itemUsageCost = getPluginInstance().getMenusConfig().getDouble("like-menu-section.items." + itemId + ".usage-cost");
                    String value = "";
                    if (clickAction.contains(":")) {
                        String[] actionArgs = clickAction.toLowerCase().split(":");
                        clickAction = actionArgs[0].replace(" ", "-").replace("_", "-");
                        value = actionArgs[1].replace("{player}", player.getName());
                    }

                    switch (clickAction) {
                        case "dispatch-command-console":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(), value);
                            }
                            break;
                        case "dispatch-command-player":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                getPluginInstance().getServer().dispatchCommand(player, value);
                            }
                            break;
                        case "open-custom-menu":
                            if (!value.equalsIgnoreCase(""))
                                if (value.equalsIgnoreCase("List Menu") || value.equalsIgnoreCase("List-Menu")) {
                                    player.closeInventory();
                                    if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                        return;

                                    Inventory inventory = getPluginInstance().getManager().buildListMenu(player);
                                    MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.LIST, inventory, player.getPlayer());
                                    getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                                    if (menuOpenEvent.isCancelled())
                                        return;

                                    player.openInventory(inventory);
                                    return;
                                } else {
                                    player.closeInventory();
                                    if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                        return;

                                    Inventory inventory = getPluginInstance().getManager().buildCustomMenu(player, value);
                                    MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.CUSTOM, inventory, player.getPlayer());
                                    menuOpenEvent.setCustomMenuId(value);
                                    getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                                    if (menuOpenEvent.isCancelled()) return;

                                    player.openInventory(inventory);
                                    return;
                                }
                            break;
                        case "like":
                            player.closeInventory();
                            if (!player.hasPermission("hyperdrive.likebypass") && warp.getVoters().contains(player.getUniqueId())) {
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("already-liked")), player);
                                return;
                            }

                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) return;
                            warp.setLikes(warp.getLikes() + 1);
                            warp.getVoters().add(player.getUniqueId());
                            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("liked-message"))
                                    .replace("{warp}", warp.getWarpName()), player);
                            break;
                        case "dislike":
                            player.closeInventory();
                            if (!player.hasPermission("hyperdrive.likebypass") && warp.getVoters().contains(player.getUniqueId())) {
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("already-liked")), player);
                                return;
                            }

                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) return;
                            warp.setDislikes(warp.getDislikes() + 1);
                            warp.getVoters().add(player.getUniqueId());
                            getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("disliked-message"))
                                    .replace("{warp}", warp.getWarpName()), player);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    private void runPlayerSelectionClick(Player player, InventoryClickEvent e) {
        if (e.getCurrentItem() != null && Objects.requireNonNull(e.getClickedInventory()).getType() != InventoryType.PLAYER) {
            e.setCancelled(true);
            if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick() == ClickType.CREATIVE)
                return;

            List<Integer> playerSlots = getPluginInstance().getMenusConfig().getIntegerList("ps-menu-section.player-slots");
            if (playerSlots.contains(e.getSlot()) && e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()) {
                String soundName = getPluginInstance().getMenusConfig().getString("ps-menu-section.player-click-sound");
                if (soundName != null && !soundName.equalsIgnoreCase(""))
                    player.getWorld().playSound(player.getLocation(),
                            Sound.valueOf(soundName.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);

                String playerName = ChatColor
                        .stripColor(Objects.requireNonNull(e.getCurrentItem().getItemMeta()).getDisplayName());
                if (playerName.equalsIgnoreCase(""))
                    return;

                @SuppressWarnings("deprecation")
                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(playerName);
                if (!offlinePlayer.isOnline())
                    return;

                List<UUID> selectedPlayers = getPluginInstance().getManager().getPaging().getSelectedPlayers(player);
                boolean isSelected = selectedPlayers != null && selectedPlayers.contains(offlinePlayer.getUniqueId());

                e.getClickedInventory().setItem(e.getSlot(),
                        getPluginInstance().getManager().getPlayerSelectionHead(offlinePlayer, !isSelected));
                getPluginInstance().getManager().getPaging().updateSelectedPlayers(player, offlinePlayer.getUniqueId(),
                        isSelected);
                return;
            }

            String itemId = getPluginInstance().getManager().getIdFromSlot("ps-menu-section", e.getSlot());
            if (itemId != null) {
                if (getPluginInstance().getMenusConfig().getBoolean("ps-menu-section.items." + itemId + ".click-sound")) {
                    String soundName = getPluginInstance().getMenusConfig().getString("ps-menu-section.items." + itemId + ".sound-name");
                    if (soundName != null && !soundName.equalsIgnoreCase(""))
                        player.playSound(player.getLocation(),
                                Sound.valueOf(soundName.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);
                }

                ConfigurationSection cs = getPluginInstance().getMenusConfig().getConfigurationSection("ps-menu-section.items." + itemId);
                if (cs != null && cs.getKeys(false).contains("permission")) {
                    String permission = getPluginInstance().getMenusConfig().getString("ps-menu-section.items." + itemId + ".permission");
                    if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
                        getPluginInstance().getManager().sendCustomMessage(
                                getPluginInstance().getLangConfig().getString("no-permission"), player);
                        return;
                    }
                }

                double itemUsageCost = getPluginInstance().getMenusConfig().getDouble("ps-menu-section.items." + itemId + ".usage-cost");
                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("ps-menu-section.items." + itemId + ".click-message"))
                        .replace("{player}", player.getName()).replace("{item-id}", itemId), player);

                String clickAction = getPluginInstance().getMenusConfig().getString("ps-menu-section.items." + itemId + ".click-action");
                if (clickAction != null) {
                    String action = clickAction, value = "";
                    if (clickAction.contains(":")) {
                        String[] actionArgs = clickAction.toLowerCase().split(":");
                        action = actionArgs[0].replace(" ", "-").replace("_", "-");
                        value = actionArgs[1].replace("{player}", player.getName());
                    }

                    HashMap<Integer, List<UUID>> playerSelectionPageMap;
                    int currentPage;
                    List<UUID> playerSelectionPageList;
                    switch (action) {
                        case "dispatch-command-console":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(), value);
                            }

                            break;
                        case "dispatch-command-player":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                getPluginInstance().getServer().dispatchCommand(player, value);
                            }

                            break;
                        case "confirm":

                            player.closeInventory();
                            final List<UUID> selectedPlayers = new ArrayList<>(getPluginInstance().getManager().getPaging().getSelectedPlayers(player));
                            if (selectedPlayers.size() >= 1) {
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                for (int i = -1; ++i < selectedPlayers.size(); ) {
                                    UUID selectedPlayerId = selectedPlayers.get(i);
                                    if (selectedPlayerId == null) continue;

                                    OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(selectedPlayerId);
                                    if (!offlinePlayer.isOnline()) continue;

                                    if (getPluginInstance().getTeleportationHandler().isTeleporting(offlinePlayer.getPlayer()))
                                        continue;

                                    getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("player-selected-teleport"))
                                            .replace("{player}", player.getName()), offlinePlayer.getPlayer());
                                }

                                getPluginInstance().getTeleportationHandler().createGroupTemp(player, selectedPlayers, getPluginInstance().getTeleportationHandler().getDestination(player));
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("group-teleport-sent"), player);
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("player-selection-fail"), player);

                            getPluginInstance().getManager().getPaging().getPlayerSelectedMap().remove(player.getUniqueId());
                            break;
                        case "refresh":
                            getPluginInstance().getManager().getPaging().resetPlayerSelectionPages(player);
                            playerSelectionPageMap = getPluginInstance().getManager().getPaging().getPlayerSelectionPages(player);
                            getPluginInstance().getManager().getPaging().getPlayerSelectionPageMap().put(player.getUniqueId(), playerSelectionPageMap);
                            currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
                            playerSelectionPageList = new ArrayList<>();
                            if (playerSelectionPageMap != null && !playerSelectionPageMap.isEmpty() && playerSelectionPageMap.containsKey(currentPage))
                                playerSelectionPageList = new ArrayList<>(playerSelectionPageMap.get(currentPage));

                            if (!playerSelectionPageList.isEmpty()) {
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                for (int i = -1; ++i < playerSlots.size(); ) {
                                    int playerSlot = playerSlots.get(i);
                                    e.getInventory().setItem(playerSlot, null);

                                    if (playerSelectionPageList.size() >= 1) {
                                        UUID playerUniqueId = playerSelectionPageList.get(0);
                                        OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(playerUniqueId);
                                        if (!offlinePlayer.isOnline())
                                            continue;

                                        List<UUID> sps = getPluginInstance().getManager().getPaging().getSelectedPlayers(player);
                                        e.getInventory().setItem(playerSlots.get(i), getPluginInstance().getManager().getPlayerSelectionHead(player,
                                                sps != null && sps.contains(offlinePlayer.getUniqueId())));
                                        playerSelectionPageList.remove(playerUniqueId);
                                    }
                                }
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("refresh-fail"), player);

                            break;
                        case "next-page":
                            if (getPluginInstance().getManager().getPaging().hasNextPlayerSelectionPage(player)) {
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                playerSelectionPageMap = getPluginInstance().getManager().getPaging().getPlayerSelectionPages(player);
                                getPluginInstance().getManager().getPaging().getPlayerSelectionPageMap().put(player.getUniqueId(), playerSelectionPageMap);
                                currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
                                playerSelectionPageList = new ArrayList<>();
                                if (playerSelectionPageMap != null && !playerSelectionPageMap.isEmpty() && playerSelectionPageMap.containsKey(currentPage + 1))
                                    playerSelectionPageList = new ArrayList<>(playerSelectionPageMap.get(currentPage + 1));

                                if (!playerSelectionPageList.isEmpty()) {
                                    getPluginInstance().getManager().getPaging().updateCurrentPlayerSelectionPage(player, true);
                                    for (int i = -1; ++i < playerSlots.size(); ) {
                                        e.getInventory().setItem(playerSlots.get(i), null);
                                        if (playerSelectionPageList.size() >= 1) {
                                            UUID playerUniqueId = playerSelectionPageList.get(0);
                                            OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(playerUniqueId);
                                            if (!offlinePlayer.isOnline())
                                                continue;

                                            List<UUID> sps = getPluginInstance().getManager().getPaging().getSelectedPlayers(player);
                                            e.getInventory().setItem(playerSlots.get(i), getPluginInstance().getManager().getPlayerSelectionHead(player, sps != null && sps.contains(offlinePlayer.getUniqueId())));
                                            playerSelectionPageList.remove(playerUniqueId);
                                        }
                                    }
                                }
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-next-page"), player);

                            break;
                        case "previous-page":
                            if (getPluginInstance().getManager().getPaging().hasPreviousWarpPage(player)) {
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                playerSelectionPageMap = getPluginInstance().getManager().getPaging().getCurrentPlayerSelectionPages(player) == null ? getPluginInstance().getManager().getPaging().getPlayerSelectionPages(player)
                                        : getPluginInstance().getManager().getPaging().getCurrentPlayerSelectionPages(player);
                                getPluginInstance().getManager().getPaging().getPlayerSelectionPageMap().put(player.getUniqueId(), playerSelectionPageMap);

                                currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
                                playerSelectionPageList = new ArrayList<>();
                                if (playerSelectionPageMap != null && !playerSelectionPageMap.isEmpty() && playerSelectionPageMap.containsKey(currentPage - 1))
                                    playerSelectionPageList = new ArrayList<>(playerSelectionPageMap.get(currentPage - 1));

                                if (!playerSelectionPageList.isEmpty()) {
                                    getPluginInstance().getManager().getPaging().updateCurrentPlayerSelectionPage(player, false);
                                    for (int i = -1; ++i < playerSlots.size(); ) {
                                        e.getInventory().setItem(playerSlots.get(i), null);
                                        if (playerSelectionPageList.size() >= 1) {
                                            UUID playerUniqueId = playerSelectionPageList.get(0);
                                            OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(playerUniqueId);
                                            if (!offlinePlayer.isOnline()) continue;

                                            List<UUID> sps = getPluginInstance().getManager().getPaging().getSelectedPlayers(player);
                                            e.getInventory().setItem(playerSlots.get(i), getPluginInstance().getManager().getPlayerSelectionHead(player, sps != null && sps.contains(offlinePlayer.getUniqueId())));
                                            playerSelectionPageList.remove(playerUniqueId);
                                        }
                                    }
                                }
                            } else
                                getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("no-previous-page"), player);

                            break;
                        case "open-custom-menu":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;

                                Inventory inventory = getPluginInstance().getManager().buildCustomMenu(player, value);
                                MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.CUSTOM, inventory, player.getPlayer());
                                menuOpenEvent.setCustomMenuId(value);
                                getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                                if (menuOpenEvent.isCancelled())
                                    return;

                                player.openInventory(inventory);
                            }

                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    private void runCustomMenuClick(Player player, String menuId, InventoryClickEvent e) {
        if (e.getCurrentItem() != null && Objects.requireNonNull(e.getClickedInventory()).getType() != InventoryType.PLAYER) {
            e.setCancelled(true);
            if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick() == ClickType.CREATIVE)
                return;

            String itemId = getPluginInstance().getManager().getIdFromSlot("custom-menu-section." + menuId, e.getSlot());
            if (itemId != null) {
                if (getPluginInstance().getMenusConfig().getBoolean("custom-menu-section." + menuId + "." + itemId + ".click-sound")) {
                    String soundName = getPluginInstance().getMenusConfig().getString("custom-menu-section." + menuId + ".items." + itemId + ".sound-name");
                    if (soundName != null && !soundName.equalsIgnoreCase(""))
                        player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);
                }

                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getMenusConfig().getString("custom-menu-section." + menuId + ".items." + itemId + ".click-message"))
                        .replace("{player}", player.getName()).replace("{item-id}", itemId), player);

                ConfigurationSection cs = getPluginInstance().getMenusConfig().getConfigurationSection("custom-menu-section." + menuId + ".items." + itemId);
                if (cs != null && cs.getKeys(false).contains("permission")) {
                    String permission = getPluginInstance().getMenusConfig().getString("custom-menu-section." + menuId + ".items." + itemId + ".permission");
                    if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
                        getPluginInstance().getManager().sendCustomMessage(
                                getPluginInstance().getLangConfig().getString("no-permission"), player);
                        return;
                    }
                }

                double itemUsageCost = getPluginInstance().getMenusConfig().getDouble("custom-menu-section." + menuId + ".items." + itemId + ".usage-cost");
                String clickAction = getPluginInstance().getMenusConfig().getString("custom-menu-section." + menuId + ".items." + itemId + ".click-action");
                if (clickAction != null) {
                    String action = clickAction, value = "";
                    if (clickAction.contains(":")) {
                        String[] actionArgs = clickAction.toLowerCase().split(":");
                        action = actionArgs[0].replace(" ", "-").replace("_", "-");
                        value = actionArgs[1].replace("{player}", player.getName());
                    }

                    switch (action) {
                        case "dispatch-command-console":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(), value);
                            }
                            break;
                        case "dispatch-command-player":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;
                                getPluginInstance().getServer().dispatchCommand(player, value);
                            }
                            break;
                        case "open-custom-menu":
                            if (!value.equalsIgnoreCase("")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;

                                Inventory inventory = getPluginInstance().getManager().buildCustomMenu(player, value);
                                MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.CUSTOM, inventory, player.getPlayer());
                                menuOpenEvent.setCustomMenuId(value);
                                getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                                if (menuOpenEvent.isCancelled()) return;

                                player.openInventory(inventory);
                            }

                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    // getters & setters
    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}
