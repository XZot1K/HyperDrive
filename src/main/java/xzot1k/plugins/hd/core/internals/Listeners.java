/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.util.Vector;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;
import xzot1k.plugins.hd.api.RandomTeleportation;
import xzot1k.plugins.hd.api.events.MenuOpenEvent;
import xzot1k.plugins.hd.api.events.RandomTeleportEvent;
import xzot1k.plugins.hd.api.events.WarpEvent;
import xzot1k.plugins.hd.api.objects.Warp;
import xzot1k.plugins.hd.core.internals.hooks.HookChecker;
import xzot1k.plugins.hd.core.objects.Destination;
import xzot1k.plugins.hd.core.objects.GroupTemp;
import xzot1k.plugins.hd.core.objects.InteractionModule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

public class Listeners implements Listener {
    private HyperDrive pluginInstance;

    public Listeners(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        String inventoryName;
        try {
            if (!(getPluginInstance().getServerVersion().startsWith("v1_13") || getPluginInstance().getServerVersion().startsWith("v1_12")
                    || getPluginInstance().getServerVersion().startsWith("v1_11") || getPluginInstance().getServerVersion().startsWith("v1_10")
                    || getPluginInstance().getServerVersion().startsWith("v1_9") || getPluginInstance().getServerVersion().startsWith("v1_8")))
                inventoryName = e.getView().getTitle();
            else {
                Method method = e.getInventory().getClass().getMethod("getName");
                inventoryName = (String) method.invoke(e.getInventory());
            }
        } catch (NoSuchMethodException | IllegalStateException | IllegalAccessException | InvocationTargetException ex) {
            return;
        }

        if (inventoryName.equalsIgnoreCase(getPluginInstance().getManager()
                .colorText(getPluginInstance().getMenusConfig().getString("list-menu-section.title")))) {
            e.setCancelled(true);
            runListMenuClick(player, e);
        } else if (inventoryName.contains(getPluginInstance().getManager()
                .colorText(getPluginInstance().getMenusConfig().getString("edit-menu-section.title")))) {
            e.setCancelled(true);
            runEditMenuClick(player, inventoryName, e);
        } else if (inventoryName.contains(getPluginInstance().getManager()
                .colorText(getPluginInstance().getMenusConfig().getString("like-menu-section.title")))) {
            e.setCancelled(true);
            runLikeMenuClick(player, inventoryName, e);
        } else if (inventoryName.equalsIgnoreCase(getPluginInstance()
                .getManager().colorText(getPluginInstance().getMenusConfig().getString("ps-menu-section.title")))) {
            e.setCancelled(true);
            runPlayerSelectionClick(player, e);
        } else {
            String menuId = getPluginInstance().getManager().getMenuId(inventoryName);
            if (menuId != null) {
                e.setCancelled(true);
                runCustomMenuClick(player, menuId, e);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        InteractionModule interactionModule = getPluginInstance().getManager().getChatInteraction(e.getPlayer());
        if (interactionModule == null) return;
        e.setCancelled(true);

        Warp warp;
        String textEntry = getPluginInstance().getManager().colorText(e.getMessage().replace("'", "").replace("\"", "")),
                chatInteractionCancelKey = getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel");

        if (e.getMessage().equalsIgnoreCase(chatInteractionCancelKey)) {
            getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
            getPluginInstance().getManager().sendCustomMessage("chat-interaction-cancelled", e.getPlayer());
            return;
        }

        List<String> globalFilterStrings = getPluginInstance().getConfig().getStringList("filter-section.global-filter");
        for (int i = -1; ++i < globalFilterStrings.size(); ) {
            final String replacement = globalFilterStrings.get(i);
            if (replacement.equalsIgnoreCase("#") || replacement.equalsIgnoreCase("{") || replacement.equalsIgnoreCase("}"))
                continue;
            textEntry = textEntry.replaceAll("(?i)" + globalFilterStrings.get(i), "");
        }

        final String strippedName = net.md_5.bungee.api.ChatColor.stripColor(textEntry);
        boolean useMySQL = (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")
                && getPluginInstance().getConfig().getBoolean("mysql-connection.cross-server"));
        switch (interactionModule.getInteractionId().toLowerCase()) {
            case "search": {
                Inventory inventory = getPluginInstance().getManager().buildListMenu(e.getPlayer(), EnumContainer.Filter.SEARCH, strippedName);
                getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () -> e.getPlayer().openInventory(inventory));
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                break;
            }
            case "create-warp": {
                if (getPluginInstance().getManager().isBlockedWorld(e.getPlayer().getWorld())) {
                    getPluginInstance().getManager().sendCustomMessage("blocked-world", e.getPlayer());
                    return;
                }

                if (getPluginInstance().getHookChecker().isNotSafe(e.getPlayer(), e.getPlayer().getLocation(), HookChecker.CheckType.CREATION)) {
                    getPluginInstance().getManager().sendCustomMessage("not-hook-safe", e.getPlayer());
                    break;
                }

                if (getPluginInstance().getManager().hasMetWarpLimit(e.getPlayer())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("warp-limit-met", e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().isSafeDistance(e.getPlayer().getLocation())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("not-safe-distance", e.getPlayer());
                    return;
                }

                if (strippedName.isEmpty()) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("invalid-warp-name", e.getPlayer());
                    return;
                }

                if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(textEntry)) || (!useMySQL && getPluginInstance().getManager().doesWarpExist(textEntry))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("warp-exists", e.getPlayer(), "{warp}:" + textEntry);
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                warp = new Warp(net.md_5.bungee.api.ChatColor.WHITE + textEntry, e.getPlayer(), e.getPlayer().getLocation());
                warp.save(true);
                warp.register();
                getPluginInstance().getManager().sendCustomMessage("warp-created", e.getPlayer(), "{warp}:" + warp.getWarpName());
                break;
            }
            case "rename": {
                String previousName = interactionModule.getInteractionValue();
                warp = getPluginInstance().getManager().getWarp(previousName);

                if (strippedName.isEmpty()) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("invalid-warp-name", e.getPlayer());
                    return;
                }

                if (!net.md_5.bungee.api.ChatColor.stripColor(textEntry).equalsIgnoreCase(net.md_5.bungee.api.ChatColor.stripColor(warp.getWarpName())))
                    if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(textEntry)) || (!useMySQL && getPluginInstance().getManager().doesWarpExist(textEntry))) {
                        getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                        getPluginInstance().getManager().sendCustomMessage("warp-exists", e.getPlayer(), "{warp}:" + textEntry);
                        return;
                    }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                final String finalTextEntry = textEntry;
                getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () -> {
                    e.getPlayer().closeInventory();
                    warp.rename(finalTextEntry);
                });

                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage("warp-renamed", e.getPlayer(), "{previous-name}:" + previousName, "{new-name}:" + finalTextEntry);
                break;
            }
            case "give-ownership": {
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (warp == null || (useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                        || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("warp-invalid", e.getPlayer(), "{warp}:" + interactionModule.getInteractionValue());
                    return;
                }

                if (textEntry.equalsIgnoreCase(e.getPlayer().getName()) && warp.getOwner().toString().equals(e.getPlayer().getUniqueId().toString())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("ownership-self", e.getPlayer(), "{warp}:" + warp.getWarpName());
                    return;
                }

                final Player enteredPlayer = getPluginInstance().getServer().getPlayer(textEntry);
                if (enteredPlayer == null || !enteredPlayer.isOnline()) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("player-invalid", e.getPlayer(), "{player}:" + textEntry);
                    return;
                }

                if (!enteredPlayer.hasPermission("hyperdrive.use.create") || !enteredPlayer.hasPermission("hyperdrive.use.edit")) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("give-no-permission", e.getPlayer(), "{player}:" + textEntry);
                    return;
                }

                if (getPluginInstance().getManager().wouldMeetWarpLimit(enteredPlayer, getPluginInstance().getManager().getOwnedWarps(enteredPlayer).size())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("warp-limit-met-other", e.getPlayer(), "{player}:" + enteredPlayer.getName());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.setOwner(enteredPlayer.getUniqueId());
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage("ownership-given", e.getPlayer(),
                        "{warp}:" + warp.getWarpName(), "{player}:" + enteredPlayer.getName());

                getPluginInstance().getManager().sendCustomMessage("ownership-obtained", enteredPlayer,
                        "{warp}:" + warp.getWarpName(), "{player}:" + enteredPlayer.getName());
                break;
            }
            case "edit-description": {
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (warp == null || (useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("warp-invalid", e.getPlayer(), "warp:" + interactionModule.getInteractionValue());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                if (e.getMessage().equalsIgnoreCase(getPluginInstance().getConfig().getString("warp-icon-section.description-clear-symbol"))) {
                    warp.setDescription("");
                    getPluginInstance().getManager().sendCustomMessage("description-cleared", e.getPlayer(), "{warp}:" + warp.getWarpName());
                } else {
                    warp.setDescription(textEntry);
                    getPluginInstance().getManager().sendCustomMessage("description-set", e.getPlayer(), "{warp}:" + warp.getWarpName(),
                            "{description}:" + net.md_5.bungee.api.ChatColor.stripColor(textEntry));
                }

                break;
            }
            case "change-usage-price": {
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (warp == null || (useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                        || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("warp-invalid", e.getPlayer(), "{warp}:" + interactionModule.getInteractionValue());
                    return;
                }

                if (getPluginInstance().getManager().isNotNumeric(textEntry.replace("$", ""))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getLangConfig().getString("invalid-usage-price"), e.getPlayer());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.setUsagePrice(Double.parseDouble(textEntry.replace("$", "")));
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage("usage-price-set", e.getPlayer(), "{warp}:" + warp.getWarpName(), "{price}:" + warp.getUsagePrice());
                break;
            }
            case "give-assistant": {
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (warp == null || (useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("warp-invalid", e.getPlayer(), "{warp}:" + interactionModule.getInteractionValue());
                    return;
                }

                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(textEntry);
                if (!offlinePlayer.isOnline()) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("player-invalid", e.getPlayer(), "{player}:" + textEntry);
                    return;
                }

                if (warp.getAssistants().contains(offlinePlayer.getUniqueId())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("player-already-assistant", e.getPlayer(), "{player}:" + textEntry);
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount()))
                    return;

                warp.getAssistants().add(offlinePlayer.getUniqueId());
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage("give-assistant", e.getPlayer(), "{warp}:" + warp.getWarpName(), "{player}:" + offlinePlayer.getName());
                break;
            }
            case "remove-assistant": {
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (warp == null || (useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("warp-invalid", e.getPlayer(), "{warp}:" + interactionModule.getInteractionValue());
                    return;
                }

                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(textEntry);
                if (!offlinePlayer.isOnline()) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("player-invalid", e.getPlayer(), "{player}:" + textEntry);
                    return;
                }

                if (!warp.getAssistants().contains(offlinePlayer.getUniqueId())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("player-not-assistant", e.getPlayer(), "{player}:" + textEntry);
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.getAssistants().remove(offlinePlayer.getUniqueId());
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());

                getPluginInstance().getManager().sendCustomMessage("remove-assistant", e.getPlayer(), ("{warp}:" + warp.getWarpName()), ("{player}:" + offlinePlayer.getName()));
                break;
            }
            case "add-to-list": {
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (warp == null || (useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("warp-invalid", e.getPlayer(), "{warp}:" + interactionModule.getInteractionValue());
                    return;
                }

                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(textEntry);
                if (!offlinePlayer.isOnline()) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("player-invalid", e.getPlayer(), "{player}:" + textEntry);
                    return;
                }

                if (warp.getPlayerList().contains(offlinePlayer.getUniqueId())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("player-in-list", e.getPlayer(), "{player}:" + textEntry);
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.getPlayerList().add(offlinePlayer.getUniqueId());
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage("add-list", e.getPlayer(), "{warp}:" + warp.getWarpName(), "{player}:" + offlinePlayer.getName());
                break;
            }
            case "remove-from-list": {
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (warp == null || (useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("warp-invalid", e.getPlayer(), "{warp}:" + interactionModule.getInteractionValue());
                    return;
                }

                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(textEntry);
                if (!offlinePlayer.isOnline()) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("player-invalid", e.getPlayer(), "{player}:" + textEntry);
                    return;
                }

                if (!warp.getPlayerList().contains(offlinePlayer.getUniqueId())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("player-not-listed", e.getPlayer(), "{player}:" + textEntry);
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.getPlayerList().remove(offlinePlayer.getUniqueId());
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage("remove-list", e.getPlayer(), "{warp}:" + warp.getWarpName(), "{player}:" + offlinePlayer.getName());
                break;
            }
            case "add-command": {
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (warp == null || (useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("warp-invalid", e.getPlayer(), "{warp}:" + interactionModule.getInteractionValue());
                    return;
                }

                List<String> commandList = warp.getCommands();
                for (int i = -1; ++i < commandList.size(); ) {
                    String command = commandList.get(i);
                    if (command.equalsIgnoreCase(textEntry)) {
                        getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                        getPluginInstance().getManager().sendCustomMessage("command-already-exists", e.getPlayer(), "{warp}:" + warp.getWarpName(), "{command}:" + command);
                        return;
                    }
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.getCommands().add(textEntry);
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage("add-command", e.getPlayer(), "{warp}:" + warp.getWarpName(), "{command}:" + textEntry);
                break;
            }
            case "remove-command": {
                String enteredIndex = e.getMessage();
                warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
                if (warp == null || (useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName())) || (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("warp-invalid", e.getPlayer(), "{warp}:" + interactionModule.getInteractionValue());
                    return;
                }

                int index;
                if (getPluginInstance().getManager().isNotNumeric(enteredIndex)) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("invalid-command-index", e.getPlayer(), "{command-count}:" + warp.getCommands().size());
                    return;
                }

                index = Integer.parseInt(enteredIndex);
                if (index < 1 || index > warp.getCommands().size()) {
                    getPluginInstance().getManager().sendCustomMessage("invalid-command-index", e.getPlayer(), "{command-count}:" + warp.getCommands().size());
                    return;
                }

                if (!getPluginInstance().getManager().initiateEconomyCharge(e.getPlayer(), interactionModule.getPassedChargeAmount())) {
                    getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                    return;
                }

                warp.getCommands().remove(index - 1);
                getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
                getPluginInstance().getManager().sendCustomMessage("remove-command", e.getPlayer(), "{warp}:" + warp.getWarpName(), "{index}:" + index);
                break;
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if ((e.getFrom().getBlockX() != Objects.requireNonNull(e.getTo()).getBlockX()) || (e.getFrom().getBlockZ() != e.getTo().getBlockZ())
                || !Objects.requireNonNull(e.getFrom().getWorld()).getName().equalsIgnoreCase(Objects.requireNonNull(e.getTo().getWorld()).getName())) {
            boolean moveCancellation = getPluginInstance().getConfig()
                    .getBoolean("teleportation-section.move-cancellation");
            if (moveCancellation) {
                GroupTemp groupTemp = getPluginInstance().getTeleportationHandler()
                        .getGroupTemp(e.getPlayer().getUniqueId());
                if (groupTemp != null && !groupTemp.isCancelled()) {
                    groupTemp.setCancelled(true);

                    getPluginInstance().getManager().sendCustomMessage("group-teleport-cancelled", e.getPlayer());
                    List<UUID> playerList = groupTemp.getAcceptedPlayers();
                    for (int i = -1; ++i < playerList.size(); ) {
                        UUID playerUniqueId = playerList.get(i);
                        if (playerUniqueId == null)
                            continue;

                        Player player = getPluginInstance().getServer().getPlayer(playerUniqueId);
                        if (player == null || !player.isOnline())
                            continue;

                        getPluginInstance().getManager().sendCustomMessage("group-teleport-cancelled", player);
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

                    getPluginInstance().getManager().sendCustomMessage("group-teleport-cancelled", e.getPlayer());

                    Player gl = getPluginInstance().getServer().getPlayer(getPluginInstance().getTeleportationHandler().getGroupLeader(e.getPlayer().getUniqueId()));
                    if (gl != null && gl.isOnline())
                        getPluginInstance().getManager().sendCustomMessage("group-teleport-cancelled", gl);

                    List<UUID> playerList = acceptedGroupTemp.getAcceptedPlayers();
                    for (int i = -1; ++i < playerList.size(); ) {
                        UUID playerUniqueId = playerList.get(i);
                        if (playerUniqueId == null || playerUniqueId.toString().equals(e.getPlayer().getUniqueId().toString()))
                            continue;

                        Player player = getPluginInstance().getServer().getPlayer(playerUniqueId);
                        if (player == null || !player.isOnline())
                            continue;

                        getPluginInstance().getManager().sendCustomMessage("group-teleport-cancelled", player);
                    }

                    getPluginInstance().getTeleportationHandler().clearGroupTemp(e.getPlayer());
                    getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(e.getPlayer());
                    getPluginInstance().getTeleportationHandler().getAnimation().stopGroupActiveAnimation(acceptedGroupTemp);
                    return;
                }

                if (getPluginInstance().getTeleportationHandler().isTeleporting(e.getPlayer()) && getPluginInstance().getTeleportationHandler().getRemainingTime(e.getPlayer()) > 0) {
                    RandomTeleportation.clearRTPInstance(e.getPlayer().getUniqueId());
                    getPluginInstance().getTeleportationHandler().removeTeleportTemp(e.getPlayer());
                    getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(e.getPlayer());
                    getPluginInstance().getManager().sendCustomMessage("teleportation-cancelled", e.getPlayer());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            final Player player = (Player) e.getEntity();

            int invulnerabilityDuration = getPluginInstance().getConfig().getInt("teleportation-section.invulnerability-duration");
            if (invulnerabilityDuration > 0) {
                long timeRemaining = getPluginInstance().getManager().getCooldownDuration(player, "invulnerability", invulnerabilityDuration);
                if (timeRemaining > 0) {
                    e.setCancelled(true);
                    return;
                }
            }

            boolean damageCancellation = getPluginInstance().getConfig().getBoolean("teleportation-section.damage-cancellation");
            if (damageCancellation && e.getCause() != EntityDamageEvent.DamageCause.SUFFOCATION && e.getCause() != EntityDamageEvent.DamageCause.VOID
                    && e.getCause() != EntityDamageEvent.DamageCause.LAVA && e.getCause() != EntityDamageEvent.DamageCause.DROWNING) {
                getPluginInstance().getTeleportationCommands().getTpaSentMap().remove(player.getUniqueId());
                getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);

                GroupTemp groupTemp = getPluginInstance().getTeleportationHandler().getGroupTemp(player.getUniqueId());
                if (groupTemp != null && !groupTemp.isCancelled()) {
                    groupTemp.setCancelled(true);
                    getPluginInstance().getManager().sendCustomMessage("group-teleport-cancelled", player);
                    List<UUID> playerList = groupTemp.getAcceptedPlayers();
                    for (int i = -1; ++i < playerList.size(); ) {
                        UUID playerUniqueId = playerList.get(i);
                        if (playerUniqueId == null) continue;

                        Player p = getPluginInstance().getServer().getPlayer(playerUniqueId);
                        if (p == null || !p.isOnline()) continue;

                        getPluginInstance().getManager().sendCustomMessage("group-teleport-cancelled", p);
                    }

                    getPluginInstance().getTeleportationHandler().clearGroupTemp(player);
                    getPluginInstance().getTeleportationHandler().getAnimation().stopGroupActiveAnimation(groupTemp);
                    return;
                }

                GroupTemp acceptedGroupTemp = getPluginInstance().getTeleportationHandler().getAcceptedGroupTemp(player.getUniqueId());
                if (acceptedGroupTemp != null && !acceptedGroupTemp.isCancelled()) {
                    acceptedGroupTemp.setCancelled(true);
                    getPluginInstance().getManager().sendCustomMessage("group-teleport-cancelled", player);

                    Player gl = getPluginInstance().getServer().getPlayer(getPluginInstance().getTeleportationHandler().getGroupLeader(player.getUniqueId()));
                    if (gl != null && gl.isOnline())
                        getPluginInstance().getManager().sendCustomMessage("group-teleport-cancelled", gl);

                    List<UUID> playerList = acceptedGroupTemp.getAcceptedPlayers();
                    for (int i = -1; ++i < playerList.size(); ) {
                        UUID playerUniqueId = playerList.get(i);
                        if (playerUniqueId == null || playerUniqueId.toString().equals(player.getUniqueId().toString()))
                            continue;

                        Player p = getPluginInstance().getServer().getPlayer(playerUniqueId);
                        if (p == null || !p.isOnline())
                            continue;

                        getPluginInstance().getManager().sendCustomMessage("group-teleport-cancelled", p);
                    }

                    getPluginInstance().getTeleportationHandler().clearGroupTemp(player);
                    getPluginInstance().getTeleportationHandler().getAnimation().stopGroupActiveAnimation(acceptedGroupTemp);
                    return;
                }

                if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
                    RandomTeleportation.clearRTPInstance(player.getUniqueId());
                    getPluginInstance().getTeleportationHandler().removeTeleportTemp(player);
                    getPluginInstance().getManager().sendCustomMessage("teleportation-cancelled", player);
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (getPluginInstance().getConfig().getBoolean("general-section.force-spawn"))
            if (!e.getPlayer().hasPlayedBefore() && getPluginInstance().getTeleportationCommands().getFirstJoinLocation() != null) {
                e.getPlayer().setVelocity(new Vector(0, 0, 0));
                e.getPlayer().teleport(getPluginInstance().getTeleportationCommands().getFirstJoinLocation().asBukkitLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);

                String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"),
                        animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
                if (!teleportSound.equalsIgnoreCase(""))
                    e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.valueOf(teleportSound), 1, 1);

                boolean isVanished = getPluginInstance().getManager().isVanished(e.getPlayer());
                if (!isVanished && animationSet != null && animationSet.contains(":")) {
                    String[] themeArgs = animationSet.split(":");
                    getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(e.getPlayer());
                    getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(e.getPlayer(), themeArgs[1],
                            EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                                    .replace("-", "_")), 1);
                }

                List<String> commandList = getPluginInstance().getConfig().getStringList("general-section.first-join-commands");
                for (int i = -1; ++i < commandList.size(); )
                    getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(), commandList.get(i).replace("{player}",
                            e.getPlayer().getName()));
                getPluginInstance().getManager().sendCustomMessage("teleport-first-join-spawn", e.getPlayer(), "{player}:" + e.getPlayer().getName());
            } else if (!getPluginInstance().getConfig().getBoolean("general-section.force-only-first-join")
                    && getPluginInstance().getTeleportationCommands().getSpawnLocation() != null) {
                e.getPlayer().setVelocity(new Vector(0, 0, 0));
                e.getPlayer().teleport(getPluginInstance().getTeleportationCommands().getSpawnLocation().asBukkitLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);

                String teleportSound = Objects.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"),
                        animationSet = getPluginInstance().getConfig().getString("special-effects-section.standalone-teleport-animation");
                if (!teleportSound.equalsIgnoreCase(""))
                    e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.valueOf(teleportSound), 1, 1);
                if (animationSet != null && animationSet.contains(":")) {
                    String[] themeArgs = animationSet.split(":");
                    getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(e.getPlayer());
                    getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(e.getPlayer(), themeArgs[1],
                            EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                                    .replace("-", "_")), 1);
                }

                getPluginInstance().getManager().sendCustomMessage("teleport-spawn", e.getPlayer(), "{player}:" + e.getPlayer().getName());
            }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN)
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

            boolean isVanished = getPluginInstance().getManager().isVanished(e.getPlayer());
            if (!isVanished && animationSet != null && animationSet.contains(":")) {
                String[] themeArgs = animationSet.split(":");
                getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(e.getPlayer());
                getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(e.getPlayer(), themeArgs[1],
                        EnumContainer.Animation.valueOf(themeArgs[0].toUpperCase().replace(" ", "_")
                                .replace("-", "_")), 1);
            }

            getPluginInstance().getManager().sendCustomMessage("teleport-spawn", e.getPlayer(), "{player}:" + e.getPlayer().getName());
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
        getPluginInstance().getManager().sendCustomMessage("sign-creation", e.getPlayer());
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
                getPluginInstance().getManager().sendCustomMessage("no-permission", e.getPlayer());
                return;
            }

            String secondLine = sign.getLine(1);
            if (!secondLine.equalsIgnoreCase("WARP") && !secondLine.equalsIgnoreCase("RTP")
                    && !secondLine.equalsIgnoreCase("GROUP WARP") && !secondLine.equalsIgnoreCase("GROUP RTP")
                    && !secondLine.equalsIgnoreCase("GROUP_WARP") && !secondLine.equalsIgnoreCase("GROUP_RTP")
                    && !secondLine.equalsIgnoreCase("GROUP-WARP") && !secondLine.equalsIgnoreCase("GROUP-RTP")) {
                getPluginInstance().getManager().sendCustomMessage("sign-action-invalid", e.getPlayer());
                return;
            }

            String warpName;
            Warp warp;
            switch (secondLine.toLowerCase().replace("_", " ").replace("-", " ")) {
                case "warp":

                    warpName = sign.getLine(2);
                    if (!getPluginInstance().getManager().doesWarpExist(warpName)) {
                        getPluginInstance().getManager().sendCustomMessage("sign-warp-invalid", player);
                        return;
                    }

                    warp = getPluginInstance().getManager().getWarp(warpName);
                    if (warp.getStatus() == EnumContainer.Status.PUBLIC || (player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())
                            || warp.getAssistants().contains(player.getUniqueId()) || player.hasPermission("hyperdrive.warps." + warpName) || player.hasPermission("hyperdrive" +
                            ".warps.*")
                            || (!warp.getPlayerList().isEmpty() && ((warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode()) || (!warp.getPlayerList().contains(player.getUniqueId()) && !warp.isWhiteListMode()))))) {
                        player.closeInventory();
                        int duration = getPluginInstance().getConfig().getInt("teleportation-section.warp-delay-duration"),
                                cooldown = getPluginInstance().getConfig().getInt("teleportation-section.cooldown-duration");

                        long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp", cooldown);
                        if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
                            getPluginInstance().getManager().sendCustomMessage("warp-cooldown", player, "{duration}:" + currentCooldown);
                            return;
                        }

                        if (getPluginInstance().getConfig().getBoolean("general-section.use-vault") && !player.hasPermission("hyperdrive.economybypass")
                                && !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString()) && !warp.getAssistants().contains(player.getUniqueId())
                                && (!warp.getPlayerList().isEmpty() && ((warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode())
                                || (!warp.getPlayerList().contains(player.getUniqueId()) && !warp.isWhiteListMode())))) {
                            EconomyResponse economyResponse = getPluginInstance().getVaultHandler().getEconomy().withdrawPlayer(player, warp.getUsagePrice());
                            if (!economyResponse.transactionSuccess()) {
                                getPluginInstance().getManager().sendCustomMessage("insufficient-funds", player, "{amount}:" + warp.getUsagePrice());
                                return;
                            } else
                                getPluginInstance().getManager().sendCustomMessage("transaction-success", player, "{amount}:" + warp.getUsagePrice());
                        }

                        boolean isVanished = getPluginInstance().getManager().isVanished(player);
                        if (!isVanished && warp.getAnimationSet().contains(":")) {
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

                        String actionMessage = getPluginInstance().getConfig().getString("teleportation-section.start-bar-message");
                        if (actionMessage != null && !actionMessage.isEmpty())
                            getPluginInstance().getManager().sendActionBar(player, actionMessage
                                    .replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()));

                        getPluginInstance().getManager().sendCustomMessage("teleportation-start", player, "{warp}:" + warp.getWarpName(), "{duration}:" + duration);
                        getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "warp", warp.getWarpName(), duration);
                    } else
                        getPluginInstance().getManager().sendCustomMessage("no-permission", player);
                    return;

                case "group warp":

                    warpName = sign.getLine(2);
                    if (!getPluginInstance().getManager().doesWarpExist(warpName)) {
                        getPluginInstance().getManager().sendCustomMessage("sign-warp-invalid", player);
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
                            getPluginInstance().getManager().sendCustomMessage("warp-cooldown", player, "{duration}:" + currentCooldown);
                            return;
                        }

                        if (getPluginInstance().getConfig().getBoolean("general-section.use-vault") && !player.hasPermission("hyperdrive.economybypass")
                                && !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString()) && !warp.getAssistants().contains(player.getUniqueId())
                                && (!warp.getPlayerList().isEmpty() && ((warp.getPlayerList().contains(player.getUniqueId()) && warp.isWhiteListMode())
                                || (!warp.getPlayerList().contains(player.getUniqueId()) && !warp.isWhiteListMode())))) {
                            EconomyResponse economyResponse = getPluginInstance().getVaultHandler().getEconomy().withdrawPlayer(player,
                                    warp.getUsagePrice());
                            if (!economyResponse.transactionSuccess()) {
                                getPluginInstance().getManager().sendCustomMessage("insufficient-funds", player, "{amount}:" + warp.getUsagePrice());
                                return;
                            } else
                                getPluginInstance().getManager().sendCustomMessage("transaction-success", player, "{amount}:" + warp.getUsagePrice());
                        }

                        Destination destination = new Destination(warp.getWarpLocation());
                        destination.setWarp(warp);
                        getPluginInstance().getTeleportationHandler().updateDestination(player, destination);
                        List<UUID> playerList = getPluginInstance().getManager().getPlayerUUIDs();
                        playerList.remove(player.getUniqueId());
                        if (playerList.size() <= 0) {
                            getPluginInstance().getManager().sendCustomMessage("no-players-found", player);
                            return;
                        }

                        Inventory inventory = getPluginInstance().getManager().buildPlayerSelectionMenu(player);

                        MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.PLAYER_SELECTION, inventory, player.getPlayer());
                        getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                        if (menuOpenEvent.isCancelled()) return;

                        player.openInventory(inventory);
                        getPluginInstance().getManager().sendCustomMessage("group-selection-start", player);
                    } else
                        getPluginInstance().getManager().sendCustomMessage("no-permission", player);
                    return;

                case "rtp":

                    if (!player.hasPermission("hyperdrive.use.rtp")) {
                        getPluginInstance().getManager().sendCustomMessage("no-permission", player);
                        return;
                    }

                    int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance().getConfig().getInt("teleportation-section.warp-delay-duration") : 0;
                    World world = getPluginInstance().getServer().getWorld(sign.getLine(2));
                    getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "rtp", world != null ? world.getName() : player.getWorld().getName(), duration);
                    break;

                case "group rtp":

                    if (!player.hasPermission("hyperdrive.use.rtpgroup")) {
                        getPluginInstance().getManager().sendCustomMessage("no-permission", player);
                        return;
                    }

                    List<UUID> onlinePlayers = getPluginInstance().getManager().getPlayerUUIDs();
                    onlinePlayers.remove(player.getUniqueId());
                    if (onlinePlayers.size() <= 0) {
                        getPluginInstance().getManager().sendCustomMessage("no-players-found", player);
                        return;
                    }

                    getPluginInstance().getManager().sendCustomMessage("random-teleport-start", player);
                    getPluginInstance().getTeleportationHandler().getDestinationMap().remove(player.getUniqueId());
                    getPluginInstance().getTeleportationHandler().updateDestinationWithRandomLocation(player, player.getWorld());
                    player.openInventory(getPluginInstance().getManager().buildPlayerSelectionMenu(player));
                    getPluginInstance().getManager().sendCustomMessage("player-selection-group", player);
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
        getPluginInstance().getTeleportationCommands().getTpaHereSentPlayers().remove(e.getPlayer().getUniqueId());
        getPluginInstance().getMainCommands().clearConfirmation(e.getPlayer());
    }

    @EventHandler
    public void onRandomTeleport(RandomTeleportEvent e) {
        checkEssentials(e.getPlayer());
    }

    @EventHandler
    public void onWarp(WarpEvent e) {
        checkEssentials(e.getPlayer());

        List<String> globalCommands = getPluginInstance().getConfig().getStringList("general-section.global-warp-commands");
        for (String commandLine : globalCommands) {
            if (commandLine.toLowerCase().endsWith(":player"))
                getPluginInstance().getServer().dispatchCommand(e.getPlayer(), commandLine.replace("{player}", e.getPlayer().getName())
                        .replaceAll("(?i):PLAYER", "").replace("(?i):CONSOLE", ""));
            else getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(),
                    commandLine.replace("{player}", e.getPlayer().getName()).replaceAll("(?i):PLAYER", "")
                            .replace("(?i):CONSOLE", ""));
        }
    }

    // methods
    private boolean cancelClick(InventoryClickEvent e, Player player) {
        if (e.getClickedInventory() != null && e.getClickedInventory().getType() == InventoryType.PLAYER || e.getClickedInventory() instanceof PlayerInventory) {
            e.setCancelled(true);
            e.setResult(Event.Result.DENY);
            return true;
        }

        if (e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || e.getAction() == InventoryAction.HOTBAR_SWAP
                || e.getAction() == InventoryAction.CLONE_STACK || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || player.getGameMode() == GameMode.CREATIVE) {
            e.setCancelled(true);
            e.setResult(Event.Result.DENY);
        }

        if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick() == ClickType.CREATIVE) {
            e.setCancelled(true);
            e.setResult(Event.Result.DENY);
            return true;
        } else {
            e.setCancelled(true);
            e.setResult(Event.Result.DENY);
        }

        return false;
    }

    private void runListMenuClick(Player player, InventoryClickEvent e) {
        if (e.getCurrentItem() == null || cancelClick(e, player)) {
            e.setResult(Event.Result.DENY);
            e.setCancelled(true);
            return;
        }

        final ConfigurationSection cs = getPluginInstance().getMenusConfig().getConfigurationSection("list-menu-section");
        if (cs == null) return;

        List<Integer> warpSlots = cs.getIntegerList("warp-slots");
        if (warpSlots.contains(e.getSlot()) && e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()
                && Objects.requireNonNull(e.getCurrentItem().getItemMeta()).hasDisplayName()) {

            final ClickType clickType = e.getClick();
            if ((clickType == ClickType.RIGHT && !player.hasPermission("hyperdrive.groups.use"))
                    || (clickType == ClickType.SHIFT_LEFT && !player.hasPermission("hyperdrive.like"))) return;

            final String warpName = Objects.requireNonNull(e.getCurrentItem()).getItemMeta().getDisplayName();
            Warp warp = getPluginInstance().getManager().getWarp(warpName);
            if (warp != null) {

                if (!getPluginInstance().getManager().canUseWarp(player, warp)) return;

                String soundName = getPluginInstance().getConfig().getString("warp-icon-section.click-sound");
                if (soundName != null && !soundName.equalsIgnoreCase(""))
                    player.getWorld().playSound(player.getLocation(),
                            Sound.valueOf(soundName.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);

                switch (clickType) {
                    case LEFT: {
                        player.closeInventory();
                        int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance().getConfig().getInt("teleportation-section.warp-delay-duration") : 0,
                                cooldown = getPluginInstance().getConfig().getInt("teleportation-section.cooldown-duration");

                        long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp", cooldown);
                        if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
                            getPluginInstance().getManager().sendCustomMessage("warp-cooldown", player, "{duration}:" + currentCooldown);
                            return;
                        }

                        if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
                            player.closeInventory();
                            getPluginInstance().getManager().sendCustomMessage("already-teleporting", player);
                            return;
                        }

                        if (getPluginInstance().getHookChecker().isNotSafe(player, warp.getWarpLocation().asBukkitLocation(), HookChecker.CheckType.WARP)) {
                            getPluginInstance().getManager().sendCustomMessage("not-hook-safe", player, ("{warp}:" + warp.getWarpName()));
                            return;
                        }

                        if (getPluginInstance().getConfig().getBoolean("general-section.use-vault") && !player.hasPermission("hyperdrive.economybypass")
                                && !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString()) && !warp.getAssistants().contains(player.getUniqueId())
                                && !getPluginInstance().getVaultHandler().getEconomy().has(player, warp.getUsagePrice())) {
                            getPluginInstance().getManager().sendCustomMessage("insufficient-funds", player, "{amount}:" + warp.getUsagePrice());
                            return;
                        }

                        boolean isVanished = getPluginInstance().getManager().isVanished(player);
                        if (!isVanished && warp.getAnimationSet().contains(":")) {
                            String[] themeArgs = warp.getAnimationSet().split(":");
                            String delayTheme = themeArgs[1];
                            if (delayTheme.contains("/")) {
                                String[] delayThemeArgs = delayTheme.split("/");
                                getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
                                getPluginInstance().getTeleportationHandler().getAnimation().playAnimation(player, delayThemeArgs[1],
                                        EnumContainer.Animation.valueOf(delayThemeArgs[0]
                                                .toUpperCase().replace(" ", "_").replace("-", "_")), duration);
                            }
                        }

                        String title = getPluginInstance().getConfig().getString("teleportation-section.start-title"),
                                subTitle = getPluginInstance().getConfig().getString("teleportation-section.start-sub-title");
                        if (title != null && subTitle != null)
                            getPluginInstance().getManager().sendTitle(player, title.replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()),
                                    subTitle.replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()), 0, 5, 0);

                        String actionMessage = getPluginInstance().getConfig().getString("teleportation-section.start-bar-message");
                        if (actionMessage != null && !actionMessage.isEmpty())
                            getPluginInstance().getManager().sendActionBar(player, actionMessage.replace("{duration}", String.valueOf(duration)).replace("{warp}",
                                    warp.getWarpName()));
                        getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "warp", warp.getWarpName(), duration);
                        getPluginInstance().getManager().sendCustomMessage("teleportation-start", player, "{warp}:" + warp.getWarpName(), "{duration}:" + duration);
                        break;
                    }

                    case RIGHT: {
                        player.closeInventory();
                        final int cooldown = getPluginInstance().getConfig().getInt("teleportation-section.cooldown-duration");
                        long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp", cooldown);
                        if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
                            getPluginInstance().getManager().sendCustomMessage("warp-cooldown", player, "{duration}:" + currentCooldown);
                            return;
                        }

                        if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
                            player.closeInventory();
                            getPluginInstance().getManager().sendCustomMessage("already-teleporting", player);
                            return;
                        }

                        if (getPluginInstance().getHookChecker().isNotSafe(player, warp.getWarpLocation().asBukkitLocation(), HookChecker.CheckType.WARP)) {
                            getPluginInstance().getManager().sendCustomMessage("not-hook-safe", player, ("{warp}:" + warp.getWarpName()));
                            return;
                        }

                        if (getPluginInstance().getConfig().getBoolean("general-section.use-vault") && !player.hasPermission("hyperdrive.economybypass")
                                && !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString()) && !warp.getAssistants().contains(player.getUniqueId())
                                && !getPluginInstance().getVaultHandler().getEconomy().has(player, warp.getUsagePrice())) {
                            getPluginInstance().getManager().sendCustomMessage("insufficient-funds", player, "{amount}:" + warp.getUsagePrice());
                            return;
                        }

                        Destination destination = new Destination(warp.getWarpLocation());
                        destination.setWarp(warp);
                        getPluginInstance().getTeleportationHandler().updateDestination(player, destination);
                        List<UUID> playerList = getPluginInstance().getManager().getPlayerUUIDs();
                        playerList.remove(player.getUniqueId());
                        if (playerList.size() <= 0) {
                            getPluginInstance().getManager().sendCustomMessage("no-players-found", player);
                            return;
                        }

                        Inventory inventory = getPluginInstance().getManager().buildPlayerSelectionMenu(player);

                        MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(),
                                EnumContainer.MenuType.PLAYER_SELECTION, inventory, player.getPlayer());
                        getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                        if (menuOpenEvent.isCancelled())
                            return;

                        player.openInventory(inventory);
                        getPluginInstance().getManager().sendCustomMessage("group-selection-start", player);
                        break;
                    }

                    case SHIFT_LEFT: {
                        if (player.hasPermission("hyperdrive.like")) {
                            if ((warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString()))
                                    && !warp.getAssistants().contains(player.getUniqueId())) {
                                getPluginInstance().getManager().sendCustomMessage(Objects.requireNonNull(getPluginInstance().getLangConfig().getString("like-own")), player);
                                return;
                            }

                            if (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")
                                    && getPluginInstance().getDatabaseConnection() != null && getPluginInstance().getBungeeListener() != null) {
                                String serverIP = (warp.getServerIPAddress().contains(".") ? getPluginInstance().getBungeeListener().getServerAddressMap().get(
                                        getPluginInstance().getBungeeListener().getMyServer()) : getPluginInstance().getBungeeListener().getMyServer());

                                if (!warp.getServerIPAddress().equalsIgnoreCase(serverIP)) {
                                    getPluginInstance().getManager().sendCustomMessage("like-incorrect-server", player, "{warp}:" + warp.getWarpName());
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
                    }

                    case SHIFT_RIGHT: {
                        if (player.hasPermission("hyperdrive.use.edit")
                                && ((warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString()))
                                || warp.getAssistants().contains(player.getUniqueId()))) {
                            Inventory inventory = getPluginInstance().getManager().buildEditMenu(player, warp);

                            MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(), EnumContainer.MenuType.EDIT, inventory, player.getPlayer());
                            getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
                            if (menuOpenEvent.isCancelled()) return;

                            player.openInventory(inventory);
                        }
                        break;
                    }

                    default:
                        break;
                }
            }

            return;
        }

        final String itemId = getPluginInstance().getManager().getIdFromSlot(cs, e.getSlot());
        if (itemId != null) {
            final ConfigurationSection itemSection = cs.getConfigurationSection("items." + itemId);
            if (itemSection == null) return;

            if (itemSection.getBoolean("click-sound")) {
                final String soundName = itemSection.getString("sound-name");
                if (soundName != null && !soundName.equalsIgnoreCase(""))
                    player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()
                            .replace(" ", "_").replace("-", "_")), 1, 1);
            }

            if (itemSection.getKeys(false).contains("permission")) {
                final String permission = itemSection.getString("permission");
                if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
                    getPluginInstance().getManager().sendCustomMessage("no-permission", player);
                    return;
                }
            }

            final String clickAction = itemSection.getString("click-action"),
                    message = itemSection.getString("click-message");

            if (message != null && !message.isEmpty())
                getPluginInstance().getManager().sendCustomMessage(message,
                        player, "{player}:" + player.getName(), "{item-id}:" + itemId);

            if (clickAction != null) {
                String action = clickAction, value = "";
                if (clickAction.contains(":")) {
                    String[] actionArgs = clickAction.toLowerCase().split(":");
                    action = actionArgs[0].replace(" ", "-").replace("_", "-");
                    value = actionArgs[1].replace("{player}", player.getName());
                }

                double itemUsageCost;
                if (getPluginInstance().getConfig().getBoolean("general-section.use-permission-based-cost")) {
                    String priceExpression = "0";
                    for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
                        if (perm.getPermission().startsWith("hyperdrive." + itemId + ".")) {
                            priceExpression = perm.getPermission().substring(perm.getPermission().lastIndexOf(".") + 1);
                        }
                    }
                    if (!priceExpression.equals("0")) {
                        if (action.equals("create-warp"))
                            priceExpression = priceExpression.replace("n", String.valueOf(getPluginInstance().getManager().getWarpCount(player)));
                        Expression exp = new Expression(priceExpression);
                        BigDecimal result = null;
                        try {
                            result = exp.evaluate().getNumberValue();
                        } catch (EvaluationException | ParseException ex) {ex.printStackTrace();}
                        itemUsageCost = (result == null ? 0 : result.doubleValue());
                    } else itemUsageCost = itemSection.getDouble("usage-cost");
                } else itemUsageCost = itemSection.getDouble("usage-cost");

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
                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) return;
                            getPluginInstance().getServer().dispatchCommand(player, value);
                        }

                        break;
                    case "create-warp":
                        player.closeInventory();

                        if (getPluginInstance().getManager().isBlockedWorld(player.getWorld())) {
                            getPluginInstance().getManager().sendCustomMessage("blocked-world", player);
                            return;
                        }

                        if (getPluginInstance().getHookChecker().isNotSafe(player, player.getLocation(), HookChecker.CheckType.CREATION)) {
                            getPluginInstance().getManager().sendCustomMessage("not-hook-safe", player);
                            break;
                        }

                        if (getPluginInstance().getManager().hasMetWarpLimit(player)) {
                            getPluginInstance().getManager().sendCustomMessage("warp-limit-met", player);
                            return;
                        }

                        if (!getPluginInstance().getManager().isSafeDistance(player.getLocation())) {
                            getPluginInstance().getManager().clearChatInteraction(player);
                            getPluginInstance().getManager().sendCustomMessage("not-safe-distance", player);
                            return;
                        }

                        if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                            getPluginInstance().getManager().updateChatInteraction(player, "create-warp", "", itemUsageCost);
                            getPluginInstance().getManager().sendCustomMessage("create-warp-interaction", player, "{player}:" + player.getName(),
                                    "{cancel}:" + getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"));
                        } else
                            getPluginInstance().getManager().sendCustomMessage("interaction-already-active", player);
                        break;
                    case "refresh":
                        if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) return;

                        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
                            getPluginInstance().getManager().getPaging().resetWarpPages(player);
                            EnumContainer.Filter filter = getPluginInstance().getManager().getCurrentFilter("list-menu-section", e.getInventory());

                            String filterValue = null;
                            ItemStack filterItem = e.getInventory().getItem(getPluginInstance().getMenusConfig().getInt("list-menu-section.items.filter-switch.slot"));
                            if (filterItem != null && filterItem.getItemMeta() != null)
                                filterValue = ChatColor.stripColor(filterItem.getItemMeta().getDisplayName()).replace(EnumContainer.Filter.SEARCH.getFormat(), "");

                            Map<Integer, List<Warp>> wpMap = getPluginInstance().getManager().getPaging().getWarpPages(player, "list-menu-section", filter, filterValue);
                            getPluginInstance().getManager().getPaging().getWarpPageMap().put(player.getUniqueId(), wpMap);
                            int page = getPluginInstance().getManager().getPaging().getCurrentPage(player);
                            List<Warp> pageList = new ArrayList<>();
                            if (wpMap != null && !wpMap.isEmpty() && wpMap.containsKey(page))
                                pageList = new ArrayList<>(wpMap.get(page));

                            if (!pageList.isEmpty())
                                for (int i = -1; ++i < warpSlots.size(); ) {
                                    e.getInventory().setItem(warpSlots.get(i), null);
                                    if (!pageList.isEmpty()) {
                                        Warp warp = pageList.get(0);
                                        ItemStack warpIcon = getPluginInstance().getManager().buildWarpIcon(player, warp);
                                        if (warpIcon != null)
                                            e.getInventory().setItem(warpSlots.get(i), warpIcon);
                                        pageList.remove(warp);
                                    }
                                }
                            else getPluginInstance().getManager().sendCustomMessage("refresh-fail", player);
                        });

                        break;
                    case "next-page":
                        if (getPluginInstance().getManager().getPaging().hasNextWarpPage(player)
                                && getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                            nextPage(player, e.getInventory(), warpSlots, true);
                        else getPluginInstance().getManager().sendCustomMessage("no-next-page", player);
                        break;
                    case "previous-page":
                        if (getPluginInstance().getManager().getPaging().hasPreviousWarpPage(player)
                                && getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                            previousPage(player, e.getInventory(), warpSlots, true);
                        else getPluginInstance().getManager().sendCustomMessage("no-previous-page", player);
                        break;
                    case "filter-switch":
                        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
                            String statusFromItem = getPluginInstance().getManager().getFilterStatusFromItem(e.getCurrentItem(), "list-menu-section", itemId);
                            if (statusFromItem != null && getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) {

                                EnumContainer.Filter next = EnumContainer.Filter.getByName(statusFromItem);
                                if (next == null) next = EnumContainer.Filter.PUBLIC;

                                next = next.getNext();
                                if (getPluginInstance().getManager().isDisabled(next)) next = next.getNext();

                                if (next == EnumContainer.Filter.SEARCH) {
                                    getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () -> {
                                        player.closeInventory();

                                        if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                                            getPluginInstance().getManager().updateChatInteraction(player, "search", null, itemUsageCost);
                                            getPluginInstance().getManager().sendCustomMessage("search-interaction", player,
                                                    "{cancel}:" + getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"),
                                                    "{player}:" + player.getName());
                                        } else
                                            getPluginInstance().getManager().sendCustomMessage("interaction-already-active", player);
                                    });
                                    return;
                                }


                                ItemStack filterItem = getPluginInstance().getManager().buildItemFromId(player,
                                        Objects.requireNonNull(next.getFormat()), "list-menu-section", itemId);
                                e.getInventory().setItem(e.getSlot(), filterItem);

                                for (int i = -1; ++i < warpSlots.size(); ) {
                                    int warpSlot = warpSlots.get(i);
                                    e.getInventory().setItem(warpSlot, null);
                                }

                                getPluginInstance().getManager().getPaging().resetWarpPages(player);
                                Map<Integer, List<Warp>> wpMap = getPluginInstance().getManager().getPaging().getWarpPages(player, "list-menu-section", next, null);
                                getPluginInstance().getManager().getPaging().getWarpPageMap().put(player.getUniqueId(), wpMap);
                                int page = getPluginInstance().getManager().getPaging().getCurrentPage(player);
                                List<Warp> wList = new ArrayList<>();
                                if (wpMap != null && !wpMap.isEmpty() && wpMap.containsKey(page))
                                    wList = new ArrayList<>(wpMap.get(page));

                                if (!wList.isEmpty())
                                    for (int i = -1; ++i < warpSlots.size(); ) {
                                        e.getInventory().setItem(warpSlots.get(i), null);
                                        if (!wList.isEmpty()) {
                                            Warp warp = wList.get(0);
                                            ItemStack warpIcon = getPluginInstance().getManager().buildWarpIcon(player, warp);
                                            if (warpIcon != null)
                                                e.getInventory().setItem(warpSlots.get(i), warpIcon);
                                            wList.remove(warp);
                                        }
                                    }
                            }
                        });

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

    private void runEditMenuClick(Player player, String inventoryName, InventoryClickEvent e) {
        if (e.getCurrentItem() == null || cancelClick(e, player)) return;

        final ConfigurationSection cs = getPluginInstance().getMenusConfig().getConfigurationSection("edit-menu-section");
        if (cs == null) return;

        String itemId = getPluginInstance().getManager().getIdFromSlot(cs, e.getSlot());
        if (itemId != null) {
            final ConfigurationSection itemSection = cs.getConfigurationSection("items." + itemId);
            if (itemSection == null) return;

            if (itemSection.getBoolean("click-sound")) {
                final String soundName = itemSection.getString("sound-name");
                if (soundName != null && !soundName.equalsIgnoreCase(""))
                    player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()
                            .replace(" ", "_").replace("-", "_")), 1, 1);
            }

            final String message = itemSection.getString("click-message");
            if (message != null && !message.isEmpty())
                getPluginInstance().getManager().sendCustomMessage(message,
                        player, "{player}:" + player.getName(), "{item-id}:" + itemId);

            String clickAction = itemSection.getString("click-action"),
                    toggleFormat = getPluginInstance().getConfig().getString("general-section.option-toggle-format"),
                    warpName = inventoryName.replace(getPluginInstance().getManager().colorText(cs.getString("title")), "");
            Warp warp = getPluginInstance().getManager().getWarp(warpName);

            if (itemSection.getKeys(false).contains("permission")) {
                final String permission = itemSection.getString("permission");
                if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
                    getPluginInstance().getManager().sendCustomMessage("no-permission", player);
                    return;
                }
            }

            if (clickAction != null) {

                final double itemUsageCost = itemSection.getDouble("usage-cost");

                String value = "";
                if (clickAction.contains(":")) {
                    String[] actionArgs = clickAction.toLowerCase().split(":");
                    clickAction = actionArgs[0].replace(" ", "-").replace("_", "-");
                    value = actionArgs[1].replace("{player}", player.getName());
                }

                final String publicFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.public-status-format"),
                        privateFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.private-status-format"),
                        adminFormat = getPluginInstance().getMenusConfig().getString("list-menu-section.admin-status-format");
                boolean useMySQL = (getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql")
                        && getPluginInstance().getConfig().getBoolean("mysql-connection.cross-server"));

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

                                Inventory inventory = getPluginInstance().getManager().buildListMenu(player, null, null);
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
                            getPluginInstance().getManager().sendCustomMessage("rename-warp-interaction", player, "{cancel}:" + getPluginInstance().getConfig().getString(
                                            "general-section" +
                                                    ".chat-interaction-cancel"),
                                    "{player}:" + player.getName());
                        } else
                            getPluginInstance().getManager().sendCustomMessage("interaction-already-active", player);

                        break;

                    case "delete":

                        player.closeInventory();
                        if (player.hasPermission("hyperdrive.use.delete") && (warp.getOwner() != null && warp.getOwner().toString().equals(player.getUniqueId().toString()))) {
                            if (((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                                    || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName())))) {
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;

                                warp.unRegister();
                                warp.deleteSaved(true);
                                getPluginInstance().getManager().sendCustomMessage("warp-deleted", player, "{warp}:" + warp.getWarpName());
                            } else
                                getPluginInstance().getManager().sendCustomMessage("warp-no-longer-exists", player, "{warp}:" + warp.getWarpName());

                        } else getPluginInstance().getManager().sendCustomMessage("delete-not-owner", player, "{warp}:" + warp.getWarpName());

                        break;

                    case "relocate": {
                        player.closeInventory();
                        if (getPluginInstance().getManager().isBlockedWorld(player.getWorld())) {
                            getPluginInstance().getManager().sendCustomMessage("blocked-world", player);
                            return;
                        }

                        if (getPluginInstance().getHookChecker().isNotSafe(player, player.getLocation(), HookChecker.CheckType.CREATION)) {
                            getPluginInstance().getManager().sendCustomMessage("not-hook-safe", player);
                            break;
                        }

                        if (!getPluginInstance().getManager().isSafeDistance(player.getLocation())) {
                            getPluginInstance().getManager().clearChatInteraction(player);
                            getPluginInstance().getManager().sendCustomMessage("not-safe-distance", player);
                            return;
                        }

                        if (((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                                || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName())))) {
                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                return;

                            warp.setWarpLocation(player.getLocation());
                            getPluginInstance().getManager().sendCustomMessage("warp-relocated", player, "{warp}:" + warp.getWarpName(), "{x}:" + warp.getWarpLocation().getX(),
                                    "{y}:" + warp.getWarpLocation().getY(), "{z}:" + warp.getWarpLocation().getZ(), "{world}:" + warp.getWarpLocation().getWorldName());
                        } else
                            getPluginInstance().getManager().sendCustomMessage("warp-invalid", player, "{warp}:" + warp.getWarpName());

                        player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                        break;
                    }
                    case "change-status":

                        player.closeInventory();
                        if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                                || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                return;

                            final EnumContainer.Status[] statusList = EnumContainer.Status.values();
                            EnumContainer.Status nextStatus, previousStatus = warp.getStatus();

                            final int currentIndex = previousStatus.getIndex();
                            int nextIndex = (currentIndex + 1);
                            nextStatus = statusList[nextIndex >= statusList.length ? 0 : nextIndex];

                            if (!player.hasPermission("hyperdrive.admin.status") && nextStatus == EnumContainer.Status.ADMIN)
                                nextStatus = statusList[0];
                            else if (getPluginInstance().getManager().hasAnyStatusPermission(player)) {
                                while (getPluginInstance().getManager().isDisabled(nextStatus)
                                        || !getPluginInstance().getManager().canUseStatus(player, nextStatus.name())) {
                                    nextStatus = statusList[(nextIndex >= statusList.length) ? 0 : nextIndex];
                                    nextIndex++;
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

                            getPluginInstance().getManager().sendCustomMessage("warp-status-changed", player, "{warp}:" + warp.getWarpName(),
                                    "{next-status}:" + nextStatusName, "{previous-status}:" + previousStatusName);
                        } else
                            getPluginInstance().getManager().sendCustomMessage("warp-no-longer-exists", player, "{warp}:" + warp.getWarpName());

                        player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                        break;

                    case "give-ownership":

                        player.closeInventory();
                        if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                            getPluginInstance().getManager().updateChatInteraction(player, "give-ownership", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                            getPluginInstance().getManager().sendCustomMessage("give-ownership-interaction", player, "{warp}:" + (warp != null ? warp.getWarpName() : ""),
                                    "{cancel}:" + getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"), "{player}:" + player.getName());
                        } else
                            getPluginInstance().getManager().sendCustomMessage("interaction-already-active", player);

                        break;

                    case "edit-description":

                        player.closeInventory();
                        if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                            getPluginInstance().getManager().updateChatInteraction(player, "edit-description", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                            getPluginInstance().getManager().sendCustomMessage("edit-description-interaction", player,
                                    "{cancel}:" + getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"),
                                    "{clear}:" + getPluginInstance().getConfig().getString("warp-icon-section.description-clear-symbol"), "{player}:" + player.getName());
                        } else
                            getPluginInstance().getManager().sendCustomMessage("interaction-already-active", player);

                        break;

                    case "change-usage-price":

                        player.closeInventory();
                        if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                            getPluginInstance().getManager().updateChatInteraction(player, "change-usage-price", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                            getPluginInstance().getManager().sendCustomMessage("change-usage-price-interaction", player,
                                    "{cancel}:" + getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"),
                                    "{player}:" + player.getName());
                        } else
                            getPluginInstance().getManager().sendCustomMessage("interaction-already-active", player);

                        break;

                    case "give-assistant":

                        player.closeInventory();
                        if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                            getPluginInstance().getManager().updateChatInteraction(player, "give-assistant", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                            getPluginInstance().getManager().sendCustomMessage("give-assistant-interaction", player,
                                    "{cancel}:" + getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"),
                                    "{player}:" + player.getName());
                        } else
                            getPluginInstance().getManager().sendCustomMessage("interaction-already-active", player);

                        break;

                    case "remove-assistant":

                        player.closeInventory();
                        if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                            getPluginInstance().getManager().updateChatInteraction(player, "remove-assistant", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                            getPluginInstance().getManager().sendCustomMessage("remove-assistant-interaction", player,
                                    "{cancel}:" + getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"),
                                    "{player}:" + player.getName());
                        } else
                            getPluginInstance().getManager().sendCustomMessage("interaction-already-active", player);

                        break;

                    case "add-to-list":

                        player.closeInventory();
                        if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                            getPluginInstance().getManager().updateChatInteraction(player, "add-to-list", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                            getPluginInstance().getManager().sendCustomMessage("add-list-interaction", player,
                                    "{cancel}:" + getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"),
                                    "{player}:" + player.getName());
                        } else
                            getPluginInstance().getManager().sendCustomMessage("interaction-already-active", player);

                        break;

                    case "remove-from-list":

                        player.closeInventory();
                        if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                            getPluginInstance().getManager().updateChatInteraction(player, "remove-from-list", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                            getPluginInstance().getManager().sendCustomMessage("remove-list-interaction", player,
                                    "{cancel}:" + getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"),
                                    "{player}:" + player.getName());
                        } else
                            getPluginInstance().getManager().sendCustomMessage("interaction-already-active", player);

                        break;

                    case "toggle-white-list":

                        player.closeInventory();
                        if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) return;

                        warp.setWhiteListMode(!warp.isWhiteListMode());
                        getPluginInstance().getManager().sendCustomMessage(warp.isWhiteListMode() ? "white-list" : "black-list", player,
                                "{cancel}:" + getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"),
                                "{player}:" + player.getName());

                        player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                        break;

                    case "add-command":

                        player.closeInventory();
                        if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                            getPluginInstance().getManager().updateChatInteraction(player, "add-command", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                            getPluginInstance().getManager().sendCustomMessage("add-command-interaction", player,
                                    "{cancel}:" + getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"),
                                    "{player}:" + player.getName());
                        } else
                            getPluginInstance().getManager().sendCustomMessage("interaction-already-active", player);

                        break;

                    case "remove-command":

                        player.closeInventory();
                        if (getPluginInstance().getManager().isNotInChatInteraction(player)) {
                            getPluginInstance().getManager().updateChatInteraction(player, "remove-command", warp != null ? warp.getWarpName() : warpName, itemUsageCost);
                            getPluginInstance().getManager().sendCustomMessage("remove-command-interaction", player,
                                    "{cancel}:" + getPluginInstance().getConfig().getString("general-section.chat-interaction-cancel"),
                                    "{player}:" + player.getName());
                        } else
                            getPluginInstance().getManager().sendCustomMessage("interaction-already-active", player);

                        break;

                    case "toggle-enchant-look":

                        player.closeInventory();
                        if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                                || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                return;

                            warp.setIconEnchantedLook(!warp.hasIconEnchantedLook());
                            if (toggleFormat != null && toggleFormat.contains(":")) {
                                String[] toggleFormatArgs = toggleFormat.split(":");
                                getPluginInstance().getManager().sendCustomMessage("enchanted-look-toggle", player,
                                        "{warp}:" + warp.getWarpName(), "{status}:"
                                                + (warp.hasIconEnchantedLook() ? "&a" + toggleFormatArgs[0] : "&c" + toggleFormatArgs[1]));
                            } else
                                getPluginInstance().getManager().sendCustomMessage("enchanted-look-toggle", player,
                                        "{warp}:" + warp.getWarpName(), "{status}:" + (warp.hasIconEnchantedLook() ? "&aEnabled" : "&cDisabled"));
                        } else {
                            getPluginInstance().getManager().sendCustomMessage("warp-no-longer-exists", player, "{warp}:" + warp.getWarpName());
                        }

                        player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                        break;

                    case "notify":
                        player.closeInventory();
                        if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                                || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                return;

                            warp.setNotify(!warp.canNotify());
                            if (toggleFormat != null && toggleFormat.contains(":")) {
                                String[] toggleFormatArgs = toggleFormat.split(":");
                                getPluginInstance().getManager().sendCustomMessage("notify-toggle", player, "{warp}:" + warp.getWarpName(),
                                        "{status}:" + (warp.canNotify() ? "&a" + toggleFormatArgs[0] : "&c" + toggleFormatArgs[1]));
                            } else
                                getPluginInstance().getManager().sendCustomMessage("notify-toggle", player, "{warp}:" + warp.getWarpName(),
                                        "{status}:" + (warp.canNotify() ? "&aEnabled" : "&cDisabled"));
                        } else
                            getPluginInstance().getManager().sendCustomMessage("warp-no-longer-exists", player, "{warp}:" + warp.getWarpName());

                        player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                        break;

                    case "change-icon":

                        player.closeInventory();
                        if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                                || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                            ItemStack handItem = getPluginInstance().getManager().getHandItem(player);

                            if (handItem == null || handItem.getType() == Material.AIR) {
                                getPluginInstance().getManager().sendCustomMessage("invalid-item", player, "{warp}:" + warp.getWarpName());
                                return;
                            }

                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                return;
                            warp.setIconTheme(handItem.getType().name() + "," + handItem.getDurability() + "," + handItem.getAmount());
                            getPluginInstance().getManager().sendCustomMessage("icon-changed", player, "{warp}:" + warp.getWarpName());
                        }

                        player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                        break;

                    case "change-animation-set":

                        player.closeInventory();
                        if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
                                || (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                return;

                            String nextAnimationSet = getPluginInstance().getManager().getNextAnimationSet(warp);
                            if (nextAnimationSet != null) {
                                warp.setAnimationSet(nextAnimationSet);
                                getPluginInstance().getManager().sendCustomMessage("animation-set-changed", player, "{warp}:" + warp.getWarpName(),
                                        "{animation-set}:" + nextAnimationSet.split(":")[0]);
                            } else
                                getPluginInstance().getManager().sendCustomMessage("animation-set-invalid", player, "{warp}:" + warp.getWarpName());
                        } else {
                            getPluginInstance().getManager().sendCustomMessage("warp-no-longer-exists", player, "{warp}:" + warp.getWarpName());
                        }

                        player.openInventory(getPluginInstance().getManager().buildEditMenu(player, warp));
                        break;

                    default:
                        break;
                }
            }
        }
    }

    private void runLikeMenuClick(Player player, String inventoryName, InventoryClickEvent e) {
        if (e.getCurrentItem() == null || cancelClick(e, player)) {
            e.setResult(Event.Result.DENY);
            e.setCancelled(true);
            return;
        }

        if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick() == ClickType.CREATIVE || e.getClickedInventory() == null
                || e.getClickedInventory().getType() == InventoryType.PLAYER)
            return;

        final ConfigurationSection cs = getPluginInstance().getMenusConfig().getConfigurationSection("like-menu-section");
        if (cs == null) return;

        final String itemId = getPluginInstance().getManager().getIdFromSlot(cs, e.getSlot());
        if (itemId != null) {
            final ConfigurationSection itemSection = cs.getConfigurationSection("items." + itemId);
            if (itemSection == null) return;

            if (itemSection.getBoolean("click-sound")) {
                String soundName = itemSection.getString("sound-name");
                if (soundName != null && !soundName.equalsIgnoreCase(""))
                    player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()
                            .replace(" ", "_").replace("-", "_")), 1, 1);
            }

            final String message = itemSection.getString("click-message");
            if (message != null && !message.isEmpty())
                getPluginInstance().getManager().sendCustomMessage(message,
                        player, "{player}:" + player.getName(), "{item-id}:" + itemId);

            String clickAction = itemSection.getString("click-action"),
                    warpName = inventoryName.replace(getPluginInstance().getManager().colorText(cs.getString("title")), "");
            final Warp warp = getPluginInstance().getManager().getWarp(warpName);

            if (itemSection.getKeys(false).contains("permission")) {
                String permission = itemSection.getString("permission");
                if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
                    getPluginInstance().getManager().sendCustomMessage("no-permission", player);
                    return;
                }
            }

            if (clickAction != null) {
                final double itemUsageCost = itemSection.getDouble("usage-cost");
                String value = "";
                if (clickAction.contains(":")) {
                    String[] actionArgs = clickAction.toLowerCase().split(":");
                    clickAction = actionArgs[0].replace(" ", "-").replace("_", "-");
                    value = actionArgs[1].replace("{player}", player.getName());
                }

                switch (clickAction) {
                    case "dispatch-command-console": {
                        if (!value.equalsIgnoreCase("")) {
                            player.closeInventory();
                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                return;
                            getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(), value);
                        }
                        break;
                    }
                    case "dispatch-command-player": {
                        if (!value.equalsIgnoreCase("")) {
                            player.closeInventory();
                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                return;
                            getPluginInstance().getServer().dispatchCommand(player, value);
                        }
                        break;
                    }
                    case "open-custom-menu": {
                        if (!value.equalsIgnoreCase(""))
                            if (value.equalsIgnoreCase("List Menu") || value.equalsIgnoreCase("List-Menu")) {
                                player.closeInventory();
                                if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                    return;

                                Inventory inventory = getPluginInstance().getManager().buildListMenu(player, null, null);
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
                    }
                    case "like": {
                        player.closeInventory();

                        final boolean hasBypass = player.hasPermission("hyperdrive.likebypass");
                        EnumContainer.VoteType voteType = warp.getVoters().getOrDefault(player.getUniqueId(), null);

                        if (voteType != null) {
                            if (!hasBypass && voteType == EnumContainer.VoteType.LIKE) {
                                warp.setLikes(warp.getLikes() - 1);
                                getPluginInstance().getManager().sendCustomMessage("redacted-like", player, ("{warp}:" + warp.getWarpName()));
                                return;
                            }

                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) return;

                            if (!hasBypass) {
                                if (voteType == EnumContainer.VoteType.DISLIKE) warp.setDislikes(Math.max(0, (warp.getDislikes() - 1)));
                                warp.getVoters().put(player.getUniqueId(), EnumContainer.VoteType.LIKE);
                            }

                            warp.setLikes(warp.getLikes() + 1);
                            getPluginInstance().getManager().sendCustomMessage("liked-message", player, ("{warp}:" + warp.getWarpName()));
                            return;
                        }

                        if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) return;
                        warp.setLikes(warp.getLikes() + 1);
                        if (!hasBypass) warp.getVoters().put(player.getUniqueId(), EnumContainer.VoteType.LIKE);
                        getPluginInstance().getManager().sendCustomMessage("liked-message", player, "{warp}:" + warp.getWarpName());
                        break;
                    }
                    case "dislike": {
                        player.closeInventory();

                        final boolean hasBypass = player.hasPermission("hyperdrive.likebypass");
                        EnumContainer.VoteType voteType = warp.getVoters().getOrDefault(player.getUniqueId(), null);

                        if (voteType != null) {
                            if (!hasBypass && voteType == EnumContainer.VoteType.DISLIKE) {
                                warp.setDislikes(warp.getDislikes() - 1);
                                getPluginInstance().getManager().sendCustomMessage("redacted-dislike", player, ("{warp}:" + warp.getWarpName()));
                                return;
                            }

                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) return;

                            if (!hasBypass) {
                                if (voteType == EnumContainer.VoteType.LIKE) warp.setLikes(Math.max(0, (warp.getLikes() - 1)));
                                warp.getVoters().put(player.getUniqueId(), EnumContainer.VoteType.DISLIKE);
                            }

                            warp.setDislikes(warp.getDislikes() + 1);
                            getPluginInstance().getManager().sendCustomMessage("disliked-message", player, "{warp}:" + warp.getWarpName());
                            return;
                        }

                        if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost)) return;
                        warp.setDislikes(warp.getDislikes() + 1);
                        if (!hasBypass) warp.getVoters().put(player.getUniqueId(), EnumContainer.VoteType.DISLIKE);
                        getPluginInstance().getManager().sendCustomMessage("disliked-message", player, "{warp}:" + warp.getWarpName());
                        break;
                    }
                    default: {break;}
                }
            }
        }
    }

    private void runPlayerSelectionClick(Player player, InventoryClickEvent e) {
        if (e.getCurrentItem() == null || cancelClick(e, player) || e.getClickedInventory() == null) {
            e.setResult(Event.Result.DENY);
            e.setCancelled(true);
            return;
        }

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

        final ConfigurationSection cs = getPluginInstance().getMenusConfig().getConfigurationSection("ps-menu-section");
        if (cs == null) return;

        String itemId = getPluginInstance().getManager().getIdFromSlot(cs, e.getSlot());
        if (itemId != null) {
            final ConfigurationSection itemSection = cs.getConfigurationSection("items." + itemId);
            if (itemSection == null) return;

            if (itemSection.getBoolean("click-sound")) {
                final String soundName = itemSection.getString("sound-name");
                if (soundName != null && !soundName.equalsIgnoreCase(""))
                    player.playSound(player.getLocation(),
                            Sound.valueOf(soundName.toUpperCase().replace(" ", "_")
                                    .replace("-", "_")), 1, 1);
            }

            if (cs.getKeys(false).contains("permission")) {
                String permission = itemSection.getString("permission");
                if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
                    getPluginInstance().getManager().sendCustomMessage("no-permission", player);
                    return;
                }
            }

            final double itemUsageCost = itemSection.getDouble("usage-cost");
            getPluginInstance().getManager().sendCustomMessage("click-message",
                    player, "{player}:" + player.getName(), "{item-id}:" + itemId);

            final String clickAction = itemSection.getString("click-action");
            if (clickAction != null) {
                String action = clickAction, value = "";
                if (clickAction.contains(":")) {
                    String[] actionArgs = clickAction.toLowerCase().split(":");
                    action = actionArgs[0].replace(" ", "-").replace("_", "-");
                    value = actionArgs[1].replace("{player}", player.getName());
                }

                Map<Integer, List<UUID>> playerSelectionPageMap;
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
                                if (offlinePlayer.getPlayer() == null || !offlinePlayer.isOnline()) continue;

                                if (getPluginInstance().getTeleportationHandler().isTeleporting(offlinePlayer.getPlayer()))
                                    continue;

                                getPluginInstance().getManager().sendCustomMessage("player-selected-teleport", offlinePlayer.getPlayer(),
                                        "{player}:" + player.getName());
                            }

                            getPluginInstance().getTeleportationHandler().createGroupTemp(player, selectedPlayers,
                                    getPluginInstance().getTeleportationHandler().getDestination(player));
                            getPluginInstance().getManager().sendCustomMessage("group-teleport-sent", player);
                        } else
                            getPluginInstance().getManager().sendCustomMessage("player-selection-fail", player);

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
                            getPluginInstance().getManager().sendCustomMessage("refresh-fail", player);

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
                                        e.getInventory().setItem(playerSlots.get(i), getPluginInstance().getManager().getPlayerSelectionHead(player,
                                                sps != null && sps.contains(offlinePlayer.getUniqueId())));
                                        playerSelectionPageList.remove(playerUniqueId);
                                    }
                                }
                            }
                        } else
                            getPluginInstance().getManager().sendCustomMessage("no-next-page", player);

                        break;
                    case "previous-page":
                        if (getPluginInstance().getManager().getPaging().hasPreviousWarpPage(player)) {
                            if (!getPluginInstance().getManager().initiateEconomyCharge(player, itemUsageCost))
                                return;
                            playerSelectionPageMap = getPluginInstance().getManager().getPaging().getCurrentPlayerSelectionPages(player) == null ?
                                    getPluginInstance().getManager().getPaging().getPlayerSelectionPages(player)
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
                                        e.getInventory().setItem(playerSlots.get(i), getPluginInstance().getManager().getPlayerSelectionHead(player,
                                                sps != null && sps.contains(offlinePlayer.getUniqueId())));
                                        playerSelectionPageList.remove(playerUniqueId);
                                    }
                                }
                            }
                        } else
                            getPluginInstance().getManager().sendCustomMessage("no-previous-page", player);

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

    private void runCustomMenuClick(Player player, String menuId, InventoryClickEvent e) {
        if (e.getCurrentItem() == null || cancelClick(e, player)) {
            e.setResult(Event.Result.DENY);
            e.setCancelled(true);
            return;
        }

        if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick() == ClickType.CREATIVE || e.getClickedInventory() == null
                || e.getClickedInventory().getType() == InventoryType.PLAYER)
            return;

        final ConfigurationSection cs = getPluginInstance().getMenusConfig().getConfigurationSection(("custom-menus-section." + menuId));
        if (cs == null) return;

        final String itemId = getPluginInstance().getManager().getIdFromSlot(cs, e.getSlot());
        if (itemId != null) {
            final ConfigurationSection itemSection = cs.getConfigurationSection("items." + itemId);
            if (itemSection == null) return;

            if (itemSection.getBoolean("click-sound")) {
                final String soundName = itemSection.getString("sound-name");
                if (soundName != null && !soundName.equalsIgnoreCase(""))
                    player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()
                            .replace(" ", "_").replace("-", "_")), 1, 1);
            }

            final String message = itemSection.getString("click-message");
            if (message != null && !message.isEmpty())
                getPluginInstance().getManager().sendCustomMessage(message,
                        player, "{player}:" + player.getName(), "{item-id}:" + itemId);

            if (itemSection.getKeys(false).contains("permission")) {
                final String permission = itemSection.getString("permission");
                if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
                    getPluginInstance().getManager().sendCustomMessage("no-permission", player);
                    return;
                }
            }

            final double itemUsageCost = itemSection.getDouble("usage-cost");
            final String clickAction = itemSection.getString("click-action");
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

    // helper methods
    private void checkEssentials(Player player) {
        if (getPluginInstance().getHookChecker().getEssentialsPlugin() != null) {
            com.earth2me.essentials.User user = ((com.earth2me.essentials.Essentials) getPluginInstance().getHookChecker().getEssentialsPlugin()).getUser(player);
            if (user != null) user.setLastLocation();
        }
    }

    private void nextPage(Player player, Inventory inventory, List<Integer> warpSlots, boolean isListMenu) {
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {

            String filterValue = null;
            if (isListMenu) {
                ItemStack filterItem = inventory.getItem(getPluginInstance().getMenusConfig().getInt("list-menu-section.items.filter-switch.slot"));
                if (filterItem != null && filterItem.getItemMeta() != null)
                    filterValue = ChatColor.stripColor(filterItem.getItemMeta().getDisplayName()).replace(EnumContainer.Filter.SEARCH.getFormat(), "");
            }

            Map<Integer, List<Warp>> wpMap = getPluginInstance().getManager().getPaging().getWarpPages(player, "list-menu-section",
                    getPluginInstance().getManager().getCurrentFilter("list-menu-section", inventory), filterValue);
            getPluginInstance().getManager().getPaging().getWarpPageMap().put(player.getUniqueId(), wpMap);
            int page = getPluginInstance().getManager().getPaging().getCurrentPage(player);
            List<Warp> pageList = new ArrayList<>();
            if (wpMap != null && !wpMap.isEmpty() && wpMap.containsKey(page + 1))
                pageList = new ArrayList<>(wpMap.get(page + 1));

            if (!pageList.isEmpty()) {
                getPluginInstance().getManager().getPaging().updateCurrentWarpPage(player, true);
                for (int i = -1; ++i < warpSlots.size(); ) {
                    inventory.setItem(warpSlots.get(i), null);
                    if (pageList.size() >= 1) {
                        Warp warp = pageList.get(0);
                        ItemStack warpIcon = getPluginInstance().getManager().buildWarpIcon(player, warp);
                        if (warpIcon != null)
                            inventory.setItem(warpSlots.get(i), warpIcon);
                        pageList.remove(warp);
                    }
                }
            }
        });
    }

    private void previousPage(Player player, Inventory inventory, List<Integer> warpSlots, boolean isListMenu) {
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {

            String filterValue = null;
            if (isListMenu) {
                ItemStack filterItem = inventory.getItem(getPluginInstance().getMenusConfig().getInt("list-menu-section.items.filter-switch.slot"));
                if (filterItem != null && filterItem.getItemMeta() != null)
                    filterValue = ChatColor.stripColor(filterItem.getItemMeta().getDisplayName()).replace(EnumContainer.Filter.SEARCH.getFormat(), "");
            }

            Map<Integer, List<Warp>> wpMap = getPluginInstance().getManager().getPaging().getWarpPages(player, "list-menu-section",
                    getPluginInstance().getManager().getCurrentFilter("list-menu-section", inventory), filterValue);
            getPluginInstance().getManager().getPaging().getWarpPageMap().put(player.getUniqueId(), wpMap);
            int page = getPluginInstance().getManager().getPaging().getCurrentPage(player);
            List<Warp> pageList = new ArrayList<>();
            if (wpMap != null && !wpMap.isEmpty() && wpMap.containsKey(page - 1))
                pageList = new ArrayList<>(wpMap.get(page - 1));

            if (!pageList.isEmpty()) {
                getPluginInstance().getManager().getPaging().updateCurrentWarpPage(player, false);
                for (int i = -1; ++i < warpSlots.size(); ) {
                    inventory.setItem(warpSlots.get(i), null);
                    if (pageList.size() >= 1) {
                        Warp warp = pageList.get(0);
                        ItemStack warpIcon = getPluginInstance().getManager().buildWarpIcon(player, warp);
                        if (warpIcon != null)
                            inventory.setItem(warpSlots.get(i), warpIcon);
                        pageList.remove(warp);
                    }
                }
            }
        });
    }

    // getters & setters
    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}