package xzot1k.plugins.hd.core.internals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.milkbowl.vault.economy.EconomyResponse;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.EnumContainer;
import xzot1k.plugins.hd.api.events.EconomyChargeEvent;
import xzot1k.plugins.hd.api.events.MenuOpenEvent;
import xzot1k.plugins.hd.api.objects.Warp;
import xzot1k.plugins.hd.core.objects.Destination;
import xzot1k.plugins.hd.core.objects.GroupTemp;
import xzot1k.plugins.hd.core.objects.InteractionModule;

public class Listeners implements Listener {
	private HyperDrive pluginInstance;

	public Listeners(HyperDrive pluginInstance) {
		setPluginInstance(pluginInstance);
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			Player player = (Player) e.getWhoClicked();
			String inventoryName;

			try {
				if (getPluginInstance().getServerVersion().startsWith("v1_14"))
					inventoryName = e.getView().getTitle();
				else {
					Method method = e.getInventory().getClass().getMethod("getName");
					if (method == null)
						method = e.getInventory().getClass().getMethod("getTitle");
					inventoryName = (String) method.invoke(e.getInventory());
				}
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
				return;
			}

			if (ChatColor.stripColor(inventoryName).equalsIgnoreCase(ChatColor.stripColor(getPluginInstance()
					.getManager().colorText(getPluginInstance().getConfig().getString("list-menu-section.title")))))
				runListMenuClick(player, e);
			else if (ChatColor.stripColor(inventoryName).contains(ChatColor.stripColor(getPluginInstance().getManager()
					.colorText(getPluginInstance().getConfig().getString("edit-menu-section.title")))))
				runEditMenuClick(player, inventoryName, e);
			else if (ChatColor.stripColor(inventoryName).equalsIgnoreCase(ChatColor.stripColor(getPluginInstance()
					.getManager().colorText(getPluginInstance().getConfig().getString("ps-menu-section.title")))))
				runPlayerSelectionClick(player, e);
			else {
				String menuId = getPluginInstance().getManager().getMenuId(inventoryName);
				if (menuId != null)
					runCustomMenuClick(player, menuId, e);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent e) {
		InteractionModule interactionModule = getPluginInstance().getManager().getChatInteraction(e.getPlayer());
		if (interactionModule == null)
			return;

		Warp warp;
		ChatColor enteredColor;
		OfflinePlayer offlinePlayer;
		String enteredName = e.getMessage().replace(" ", "_"), message, enteredText, enteredValue,
				chatInteractionCancelKey = getPluginInstance().getConfig()
						.getString("general-section.chat-interaction-cancel");
		List<String> globalFilterStrings = getPluginInstance().getConfig()
				.getStringList("filter-section.global-filter");
		boolean useMySQL = getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql"),
				useVault = getPluginInstance().getConfig().getBoolean("general-section.use-vault");
		switch (interactionModule.getInteractionId().toLowerCase()) {
		case "create-warp":
			e.setCancelled(true);
			enteredName = ChatColor.stripColor(getPluginInstance().getManager().colorText(e.getMessage())).replace(" ",
					"_");
			if (e.getMessage().equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			enteredName = enteredName.replaceAll("[.,?:;\'\"\\\\|`~!@#$%^&*()+=/<>]", "");
			for (int i = -1; ++i < globalFilterStrings.size();) {
				String filterString = globalFilterStrings.get(i).replaceAll("[.,?:;\'\"\\\\|`~!@#$%^&*()+=/<>]", "");
				if (filterString != null && !filterString.equalsIgnoreCase(""))
					enteredName = enteredName.replaceAll("(?i)" + filterString, "");
			}

			if (getPluginInstance().getManager().hasMetWarpLimit(e.getPlayer())) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager().sendCustomMessage(
						Objects.requireNonNull(
								getPluginInstance().getConfig().getString("language-section.warp-limit-met")),
						e.getPlayer());
				return;
			}

			if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(enteredName))
					|| (!useMySQL && getPluginInstance().getManager().doesWarpExist(enteredName))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-exists"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(e.getPlayer(),
						itemUsageCost);
				if (!economyResponse.transactionSuccess()) {
					getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
					return;
				} else
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.transaction-success"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
			}

			warp = new Warp(enteredName, e.getPlayer(), e.getPlayer().getLocation());
			warp.register();
			getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
			getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
					() -> getPluginInstance().saveWarp(warp, useMySQL));
			getPluginInstance().getManager()
					.sendCustomMessage(Objects
							.requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-created"))
							.replace("{warp}", enteredName), e.getPlayer());
			break;
		case "rename":
			e.setCancelled(true);
			if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			enteredName = enteredName.replaceAll("[.,?:;\'\"\\\\|`~!@#$%^&*()+=/<>]", "");
			for (int i = -1; ++i < globalFilterStrings.size();) {
				String filterString = globalFilterStrings.get(i).replaceAll("[.,?:;\'\"\\\\|`~!@#$%^&*()+=/<>]", "");
				if (filterString != null && !filterString.equalsIgnoreCase(""))
					enteredName = enteredName.replaceAll("(?i)" + filterString, "");
			}

			String previousName = interactionModule.getInteractionValue();
			warp = getPluginInstance().getManager().getWarp(previousName);
			if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(enteredName))
					|| (!useMySQL && getPluginInstance().getManager().doesWarpExist(enteredName))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-exists"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(e.getPlayer(),
						itemUsageCost);
				if (!economyResponse.transactionSuccess()) {
					getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
					return;
				} else
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.transaction-success"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
			}

			warp.rename(enteredName);
			getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
			getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
					() -> getPluginInstance().saveWarp(warp, useMySQL));
			getPluginInstance().getManager().sendCustomMessage(
					Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-renamed"))
							.replace("{previous-name}", previousName).replace("{new-name}", enteredName),
					e.getPlayer());
			break;
		case "give-ownership":
			e.setCancelled(true);
			if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(enteredName);
			if (!offlinePlayer.isOnline()) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.player-invalid"))
								.replace("{player}", enteredName), e.getPlayer());
				return;
			}

			previousName = interactionModule.getInteractionValue();
			warp = getPluginInstance().getManager().getWarp(previousName);
			if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(enteredName))
					|| (!useMySQL && !getPluginInstance().getManager().doesWarpExist(enteredName))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-invalid"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(e.getPlayer(),
						itemUsageCost);
				if (!economyResponse.transactionSuccess()) {
					getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
					return;
				} else
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.transaction-success"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
			}

			warp.setOwner(offlinePlayer.getUniqueId());
			getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
			getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
					() -> getPluginInstance().saveWarp(warp, useMySQL));
			getPluginInstance().getManager().sendCustomMessage(
					Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.warp-renamed"))
							.replace("{previous-name}", previousName).replace("{new-name}", enteredName),
					e.getPlayer());
			break;
		case "edit-description":
			e.setCancelled(true);
			warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
			if (e.getMessage().equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", warp.getWarpName()), e.getPlayer());
				return;
			}

			if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
					|| (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-invalid"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyChargeEvent economyChargeEvent = new EconomyChargeEvent(e.getPlayer(), itemUsageCost);
				getPluginInstance().getServer().getPluginManager().callEvent(economyChargeEvent);
				if (!economyChargeEvent.isCancelled()) {
					EconomyResponse economyResponse = getPluginInstance().getVaultEconomy()
							.withdrawPlayer(e.getPlayer(), itemUsageCost);
					if (!economyResponse.transactionSuccess()) {
						getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
						getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
						getPluginInstance().getManager()
								.sendCustomMessage(Objects
										.requireNonNull(getPluginInstance().getConfig()
												.getString("language-section.insufficient-funds"))
										.replace("{amount}", String.valueOf(itemUsageCost))
										.replace("{player}", e.getPlayer().getName()), e.getPlayer());
						return;
					} else
						getPluginInstance().getManager()
								.sendCustomMessage(Objects
										.requireNonNull(getPluginInstance().getConfig()
												.getString("language-section.transaction-success"))
										.replace("{amount}", String.valueOf(itemUsageCost))
										.replace("{player}", e.getPlayer().getName()), e.getPlayer());
				}
			}

			if (e.getMessage().equalsIgnoreCase(
					getPluginInstance().getConfig().getString("warp-icon-section.description-clear-symbol"))) {
				warp.getDescription().clear();
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager().sendCustomMessage(Objects
						.requireNonNull(
								getPluginInstance().getConfig().getString("language-section.description-cleared"))
						.replace("{warp}", warp.getWarpName()), e.getPlayer());
			} else {
				String desc = ChatColor.stripColor(getPluginInstance().getManager().colorText(e.getMessage()));
				for (int i = -1; ++i < globalFilterStrings.size();) {
					String filterString = globalFilterStrings.get(i);
					if (filterString != null && !filterString.equalsIgnoreCase(""))
						enteredName = enteredName.replaceAll("(?i)" + filterString, "");
				}

				warp.setDescription(getPluginInstance().getManager().wrapString(desc,
						getPluginInstance().getConfig().getInt("warp-icon-section.description-line-cap")));
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
						() -> getPluginInstance().saveWarp(warp, useMySQL));
				getPluginInstance().getManager().sendCustomMessage(Objects
						.requireNonNull(getPluginInstance().getConfig().getString("language-section.description-set"))
						.replace("{warp}", warp.getWarpName()).replace("{description}",
								warp.getDescriptionColor() + ChatColor
										.stripColor(getPluginInstance().getManager().colorText(e.getMessage()))),
						e.getPlayer());
			}

			break;
		case "change-description-color":
			e.setCancelled(true);
			enteredText = ChatColor.stripColor(e.getMessage().replace(" ", "_"));
			warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
			if (enteredText.equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", warp.getWarpName()), e.getPlayer());
				return;
			}

			if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
					|| (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-invalid"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			message = Objects
					.requireNonNull(getPluginInstance().getConfig().getString("language-section.invalid-color"))
					.replace("{colors}", getPluginInstance().getManager().getColorNames().toString());
			enteredValue = ChatColor.stripColor(getPluginInstance().getManager()
					.colorText(e.getMessage().toUpperCase().replace(" ", "_").replace("-", "_")));
			if (!getPluginInstance().getManager().isChatColor(enteredValue)) {
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

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(e.getPlayer(),
						itemUsageCost);
				if (!economyResponse.transactionSuccess()) {
					getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
					return;
				} else
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.transaction-success"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
			}

			warp.setDescriptionColor(enteredColor);
			getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
			getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
					() -> getPluginInstance().saveWarp(warp, useMySQL));
			getPluginInstance().getManager().sendCustomMessage(
					Objects.requireNonNull(
							getPluginInstance().getConfig().getString("language-section.description-color-changed"))
							.replace("{warp}", warp.getWarpName()).replace("{color}",
									enteredColor + WordUtils.capitalize(
											enteredColor.name().toLowerCase().replace("_", " ").replace("-", "_"))),
					e.getPlayer());
			break;
		case "change-name-color":
			e.setCancelled(true);
			message = Objects
					.requireNonNull(getPluginInstance().getConfig().getString("language-section.invalid-color"))
					.replace("{colors}", getPluginInstance().getManager().getColorNames().toString());
			enteredText = ChatColor.stripColor(getPluginInstance().getManager()
					.colorText(e.getMessage().toUpperCase().replace(" ", "_").replace("-", "_")));
			warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
			if (enteredText.equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", warp.getWarpName()), e.getPlayer());
				return;
			}

			if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
					|| (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-invalid"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			if (!getPluginInstance().getManager().isChatColor(enteredText)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager().sendCustomMessage(message, e.getPlayer());
				return;
			}

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(e.getPlayer(),
						itemUsageCost);
				if (!economyResponse.transactionSuccess()) {
					getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
					return;
				} else
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.transaction-success"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
			}

			enteredColor = ChatColor.valueOf(enteredText);
			if (enteredColor == ChatColor.BOLD || enteredColor == ChatColor.MAGIC || enteredColor == ChatColor.UNDERLINE
					|| enteredColor == ChatColor.STRIKETHROUGH || enteredColor == ChatColor.ITALIC
					|| enteredColor == ChatColor.RESET) {
				getPluginInstance().getManager().sendCustomMessage(message, e.getPlayer());
				return;
			}

			warp.setDisplayNameColor(enteredColor);
			getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
			getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
					() -> getPluginInstance().saveWarp(warp, useMySQL));
			getPluginInstance().getManager().sendCustomMessage(Objects
					.requireNonNull(getPluginInstance().getConfig().getString("language-section.name-color-changed"))
					.replace("{warp}", warp.getWarpName()).replace("{color}",
							enteredColor + WordUtils
									.capitalize(enteredColor.name().toLowerCase().replace("_", " ").replace("-", "_"))),
					e.getPlayer());
			break;
		case "change-usage-price":
			e.setCancelled(true);
			enteredText = e.getMessage().replace(" ", "_");
			warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());

			if (enteredText.equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", warp.getWarpName()), e.getPlayer());
				return;
			}

			if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
					|| (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-invalid"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			if (getPluginInstance().getManager().isNumeric(enteredText)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager().sendCustomMessage(
						getPluginInstance().getConfig().getString("language-section.invalid-usage-price"),
						e.getPlayer());
				return;
			}

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(e.getPlayer(),
						itemUsageCost);
				if (!economyResponse.transactionSuccess()) {
					getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
					return;
				} else
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.transaction-success"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
			}

			warp.setUsagePrice(Double.parseDouble(enteredText));
			getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
			getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
					() -> getPluginInstance().saveWarp(warp, useMySQL));
			getPluginInstance().getManager().sendCustomMessage(Objects
					.requireNonNull(getPluginInstance().getConfig().getString("language-section.usage-price-set"))
					.replace("{warp}", warp.getWarpName()).replace("{price}", enteredText), e.getPlayer());
			break;
		case "give-assistant":
			e.setCancelled(true);
			warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
			if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", warp.getWarpName()), e.getPlayer());
				return;
			}

			if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
					|| (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-invalid"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(enteredName);
			if (!offlinePlayer.isOnline()) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.player-invalid"))
								.replace("{player}", enteredName), e.getPlayer());
				return;
			}

			if (warp.getAssistants().contains(offlinePlayer.getUniqueId())) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager().sendCustomMessage(Objects
						.requireNonNull(
								getPluginInstance().getConfig().getString("language-section.player-already-assistant"))
						.replace("{player}", enteredName), e.getPlayer());
				return;
			}

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(e.getPlayer(),
						itemUsageCost);
				if (!economyResponse.transactionSuccess()) {
					getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
					return;
				} else
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.transaction-success"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
			}

			warp.getAssistants().add(offlinePlayer.getUniqueId());
			getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
			getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
					() -> getPluginInstance().saveWarp(warp, useMySQL));
			getPluginInstance().getManager().sendCustomMessage(
					Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.give-assistant"))
							.replace("{warp}", warp.getWarpName())
							.replace("{player}", Objects.requireNonNull(offlinePlayer.getName())),
					e.getPlayer());
			break;
		case "remove-assistant":
			e.setCancelled(true);
			warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());

			if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", warp.getWarpName()), e.getPlayer());
				return;
			}

			if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
					|| (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-invalid"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(enteredName);
			if (!offlinePlayer.isOnline()) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.player-invalid"))
								.replace("{player}", enteredName), e.getPlayer());
				return;
			}

			if (!warp.getAssistants().contains(offlinePlayer.getUniqueId())) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager().sendCustomMessage(Objects
						.requireNonNull(
								getPluginInstance().getConfig().getString("language-section.player-not-assistant"))
						.replace("{player}", enteredName), e.getPlayer());
				return;
			}

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(e.getPlayer(),
						itemUsageCost);
				if (!economyResponse.transactionSuccess()) {
					getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
					return;
				} else
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.transaction-success"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
			}

			warp.getAssistants().remove(offlinePlayer.getUniqueId());
			getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
			getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
					() -> getPluginInstance().saveWarp(warp, useMySQL));
			getPluginInstance().getManager().sendCustomMessage(Objects
					.requireNonNull(getPluginInstance().getConfig().getString("language-section.remove-assistant"))
					.replace("{warp}", warp.getWarpName())
					.replace("{player}", Objects.requireNonNull(offlinePlayer.getName())), e.getPlayer());
			break;
		case "add-to-whitelist":
			e.setCancelled(true);
			warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
			if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", warp.getWarpName()), e.getPlayer());
				return;
			}

			if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
					|| (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-invalid"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(enteredName);
			if (!offlinePlayer.isOnline()) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.player-invalid"))
								.replace("{player}", enteredName), e.getPlayer());
				return;
			}

			if (warp.getWhiteList().contains(offlinePlayer.getUniqueId())) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager().sendCustomMessage(Objects
						.requireNonNull(
								getPluginInstance().getConfig().getString("language-section.player-whitelisted"))
						.replace("{player}", enteredName), e.getPlayer());
				return;
			}

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(e.getPlayer(),
						itemUsageCost);
				if (!economyResponse.transactionSuccess()) {
					getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
					return;
				} else
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.transaction-success"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
			}

			warp.getWhiteList().add(offlinePlayer.getUniqueId());
			getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
			getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
					() -> getPluginInstance().saveWarp(warp, useMySQL));
			getPluginInstance().getManager()
					.sendCustomMessage(Objects
							.requireNonNull(getPluginInstance().getConfig().getString("language-section.add-whitelist"))
							.replace("{warp}", warp.getWarpName())
							.replace("{player}", Objects.requireNonNull(offlinePlayer.getName())), e.getPlayer());
			break;
		case "remove-from-whitelist":
			e.setCancelled(true);
			warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());
			if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", warp.getWarpName()), e.getPlayer());
				return;
			}

			if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
					|| (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-invalid"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(enteredName);
			if (!offlinePlayer.isOnline()) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.player-invalid"))
								.replace("{player}", enteredName), e.getPlayer());
				return;
			}

			if (!warp.getWhiteList().contains(offlinePlayer.getUniqueId())) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager().sendCustomMessage(Objects
						.requireNonNull(
								getPluginInstance().getConfig().getString("language-section.player-not-whitelisted"))
						.replace("{player}", enteredName), e.getPlayer());
				return;
			}

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(e.getPlayer(),
						itemUsageCost);
				if (!economyResponse.transactionSuccess()) {
					getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
					return;
				} else
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.transaction-success"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
			}

			warp.getWhiteList().remove(offlinePlayer.getUniqueId());
			getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
			getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
					() -> getPluginInstance().saveWarp(warp, useMySQL));
			getPluginInstance().getManager().sendCustomMessage(Objects
					.requireNonNull(getPluginInstance().getConfig().getString("language-section.remove-whitelist"))
					.replace("{warp}", warp.getWarpName())
					.replace("{player}", Objects.requireNonNull(offlinePlayer.getName())), e.getPlayer());
			break;
		case "add-command":
			e.setCancelled(true);
			String enteredCommand = e.getMessage();
			warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());

			if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", warp.getWarpName()), e.getPlayer());
				return;
			}

			if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
					|| (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-invalid"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			List<String> commandList = warp.getCommands();
			for (int i = -1; ++i < commandList.size();) {
				String command = commandList.get(i);
				if (command.equalsIgnoreCase(enteredCommand)) {
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(getPluginInstance().getConfig()
									.getString("language-section.command-already-exists"))
							.replace("{warp}", warp.getWarpName()).replace("{command}", command), e.getPlayer());
					return;
				}
			}

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(e.getPlayer(),
						itemUsageCost);
				if (!economyResponse.transactionSuccess()) {
					getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
					return;
				} else
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.transaction-success"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
			}

			warp.getCommands().add(enteredCommand);
			getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
			getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
					() -> getPluginInstance().saveWarp(warp, useMySQL));
			getPluginInstance().getManager()
					.sendCustomMessage(Objects
							.requireNonNull(getPluginInstance().getConfig().getString("language-section.add-command"))
							.replace("{warp}", warp.getWarpName()).replace("{command}", enteredCommand), e.getPlayer());
			break;
		case "remove-command":
			e.setCancelled(true);
			String enteredIndex = e.getMessage();
			warp = getPluginInstance().getManager().getWarp(interactionModule.getInteractionValue());

			if (enteredName.equalsIgnoreCase(chatInteractionCancelKey)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("language-section.chat-interaction-cancelled"))
								.replace("{warp}", warp.getWarpName()), e.getPlayer());
				return;
			}

			if ((useMySQL && !getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
					|| (!useMySQL && !getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-invalid"))
								.replace("{warp}", enteredName), e.getPlayer());
				return;
			}

			int index;
			if (!getPluginInstance().getManager().isNumeric(enteredIndex)) {
				getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
				getPluginInstance().getManager()
						.sendCustomMessage(Objects.requireNonNull(
								getPluginInstance().getConfig().getString("language-section.invalid-command-index"))
								.replace("{command-count}", String.valueOf(warp.getCommands().size())), e.getPlayer());
				return;
			}

			index = Integer.parseInt(enteredIndex);
			if (index < 1 || index > warp.getCommands().size()) {
				getPluginInstance().getManager()
						.sendCustomMessage(Objects.requireNonNull(
								getPluginInstance().getConfig().getString("language-section.invalid-command-index"))
								.replace("{command-count}", String.valueOf(warp.getCommands().size())), e.getPlayer());
				return;
			}

			if (useVault && interactionModule.getPassedChargeAmount() > 0) {
				double itemUsageCost = interactionModule.getPassedChargeAmount();
				EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(e.getPlayer(),
						itemUsageCost);
				if (!economyResponse.transactionSuccess()) {
					getPluginInstance().getManager().updateLastTransactionAmount(e.getPlayer(), itemUsageCost);
					getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.insufficient-funds"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
					return;
				} else
					getPluginInstance().getManager().sendCustomMessage(Objects
							.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.transaction-success"))
							.replace("{amount}", String.valueOf(itemUsageCost))
							.replace("{player}", e.getPlayer().getName()), e.getPlayer());
			}

			warp.getCommands().remove(index - 1);
			getPluginInstance().getManager().clearChatInteraction(e.getPlayer());
			getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
					() -> getPluginInstance().saveWarp(warp, useMySQL));
			getPluginInstance().getManager().sendCustomMessage(
					Objects.requireNonNull(getPluginInstance().getConfig().getString("language-section.remove-command"))
							.replace("{warp}", warp.getWarpName()).replace("{index}", String.valueOf(index)),
					e.getPlayer());
			break;
		}
	}

	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		if ((e.getFrom().getBlockX() != Objects.requireNonNull(e.getTo()).getBlockX())
				|| (e.getFrom().getBlockY() != e.getTo().getBlockY())
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
							getPluginInstance().getConfig().getString("language-section.group-teleport-cancelled"),
							e.getPlayer());
					List<UUID> playerList = groupTemp.getAcceptedPlayers();
					for (int i = -1; ++i < playerList.size();) {
						UUID playerUniqueId = playerList.get(i);
						if (playerUniqueId == null)
							continue;

						Player player = getPluginInstance().getServer().getPlayer(playerUniqueId);
						if (player == null || !player.isOnline())
							continue;

						getPluginInstance().getManager().sendCustomMessage(
								getPluginInstance().getConfig().getString("language-section.group-teleport-cancelled"),
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
							getPluginInstance().getConfig().getString("language-section.group-teleport-cancelled"),
							e.getPlayer());

					Player gl = getPluginInstance().getServer().getPlayer(
							getPluginInstance().getTeleportationHandler().getGroupLeader(e.getPlayer().getUniqueId()));
					if (gl != null && gl.isOnline())
						getPluginInstance().getManager().sendCustomMessage(
								getPluginInstance().getConfig().getString("language-section.group-teleport-cancelled"),
								gl);

					List<UUID> playerList = acceptedGroupTemp.getAcceptedPlayers();
					for (int i = -1; ++i < playerList.size();) {
						UUID playerUniqueId = playerList.get(i);
						if (playerUniqueId == null
								|| playerUniqueId.toString().equals(e.getPlayer().getUniqueId().toString()))
							continue;

						Player player = getPluginInstance().getServer().getPlayer(playerUniqueId);
						if (player == null || !player.isOnline())
							continue;

						getPluginInstance().getManager().sendCustomMessage(
								getPluginInstance().getConfig().getString("language-section.group-teleport-cancelled"),
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
							getPluginInstance().getConfig().getString("language-section.teleportation-cancelled"),
							e.getPlayer());
				}
			}
		}
	}

	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			boolean damageCancellation = getPluginInstance().getConfig()
					.getBoolean("teleportation-section.damage-cancellation");
			if (damageCancellation) {
				GroupTemp groupTemp = getPluginInstance().getTeleportationHandler().getGroupTemp(player.getUniqueId());
				if (groupTemp != null && !groupTemp.isCancelled()) {
					groupTemp.setCancelled(true);

					getPluginInstance().getManager().sendCustomMessage(
							getPluginInstance().getConfig().getString("language-section.group-teleport-cancelled"),
							player);
					List<UUID> playerList = groupTemp.getAcceptedPlayers();
					for (int i = -1; ++i < playerList.size();) {
						UUID playerUniqueId = playerList.get(i);
						if (playerUniqueId == null)
							continue;

						Player p = getPluginInstance().getServer().getPlayer(playerUniqueId);
						if (p == null || !p.isOnline())
							continue;

						getPluginInstance().getManager().sendCustomMessage(
								getPluginInstance().getConfig().getString("language-section.group-teleport-cancelled"),
								p);
					}

					getPluginInstance().getTeleportationHandler().clearGroupTemp(player);
					getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
					getPluginInstance().getTeleportationHandler().getAnimation().stopGroupActiveAnimation(groupTemp);
					return;
				}

				GroupTemp acceptedGroupTemp = getPluginInstance().getTeleportationHandler()
						.getAcceptedGroupTemp(player.getUniqueId());
				if (acceptedGroupTemp != null && !acceptedGroupTemp.isCancelled()) {
					acceptedGroupTemp.setCancelled(true);

					getPluginInstance().getManager().sendCustomMessage(
							getPluginInstance().getConfig().getString("language-section.group-teleport-cancelled"),
							player);

					Player gl = getPluginInstance().getServer().getPlayer(
							getPluginInstance().getTeleportationHandler().getGroupLeader(player.getUniqueId()));
					if (gl != null && gl.isOnline())
						getPluginInstance().getManager().sendCustomMessage(
								getPluginInstance().getConfig().getString("language-section.group-teleport-cancelled"),
								gl);

					List<UUID> playerList = acceptedGroupTemp.getAcceptedPlayers();
					for (int i = -1; ++i < playerList.size();) {
						UUID playerUniqueId = playerList.get(i);
						if (playerUniqueId == null || playerUniqueId.toString().equals(player.getUniqueId().toString()))
							continue;

						Player p = getPluginInstance().getServer().getPlayer(playerUniqueId);
						if (p == null || !p.isOnline())
							continue;

						getPluginInstance().getManager().sendCustomMessage(
								getPluginInstance().getConfig().getString("language-section.group-teleport-cancelled"),
								p);
					}

					getPluginInstance().getTeleportationHandler().clearGroupTemp(player);
					getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
					getPluginInstance().getTeleportationHandler().getAnimation()
							.stopGroupActiveAnimation(acceptedGroupTemp);
					return;
				}

				if (getPluginInstance().getTeleportationHandler().isTeleporting(player)) {
					getPluginInstance().getTeleportationHandler().getRandomTeleportingPlayers()
							.remove(player.getUniqueId());
					getPluginInstance().getTeleportationHandler().removeTeleportTemp(player);
					getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
					getPluginInstance().getManager().sendCustomMessage(
							getPluginInstance().getConfig().getString("language-section.teleportation-cancelled"),
							player);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent e) {
		getPluginInstance().getTeleportationCommands().updateLastLocation(e.getPlayer(), e.getFrom());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
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

		if (!e.getPlayer().hasPermission("hyperdrive.use.signs"))
			return;

		getPluginInstance().getManager().sendCustomMessage(
				getPluginInstance().getConfig().getString("language-section.sign-creation"), e.getPlayer());
		e.setLine(0, getPluginInstance().getManager().colorText(
				getPluginInstance().getConfig().getString("general-section.sign-header-color")) + initialLine);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK
				&& (e.getClickedBlock() != null && (e.getClickedBlock().getType().name().contains("SIGN")))) {
			Sign sign = (Sign) e.getClickedBlock().getState();
			String initialLine = ChatColor.stripColor(sign.getLine(0));
			if (!initialLine.equalsIgnoreCase("[HyperDrive]") && !initialLine.equalsIgnoreCase("[HD]"))
				return;
			if (!e.getPlayer().hasPermission("hyperdrive.use.signs")) {
				getPluginInstance().getManager().sendCustomMessage(
						getPluginInstance().getConfig().getString("language-section.no-permission"), e.getPlayer());
				return;
			}

			String secondLine = sign.getLine(1);
			if (!secondLine.equalsIgnoreCase("WARP") && !secondLine.equalsIgnoreCase("RTP")
					&& !secondLine.equalsIgnoreCase("GROUP WARP") && !secondLine.equalsIgnoreCase("GROUP RTP")
					&& !secondLine.equalsIgnoreCase("GROUP_WARP") && !secondLine.equalsIgnoreCase("GROUP_RTP")
					&& !secondLine.equalsIgnoreCase("GROUP-WARP") && !secondLine.equalsIgnoreCase("GROUP-RTP")) {
				getPluginInstance().getManager().sendCustomMessage(
						getPluginInstance().getConfig().getString("language-section.sign-action-invalid"),
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
							getPluginInstance().getConfig().getString("language-section.sign-warp-invalid"), player);
					return;
				}

				warp = getPluginInstance().getManager().getWarp(warpName);
				if (warp.getStatus() == EnumContainer.Status.PUBLIC
						|| (player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())
								|| warp.getAssistants().contains(player.getUniqueId())
								|| player.hasPermission("hyperdrive.warps." + warpName)
								|| player.hasPermission("hyperdrive.warps.*")
								|| (warp.getWhiteList().contains(player.getUniqueId())))) {
					player.closeInventory();
					int duration = getPluginInstance().getConfig().getInt("teleportation-section.warp-delay-duration"),
							cooldown = getPluginInstance().getConfig()
									.getInt("teleportation-section.cooldown-duration");

					long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp",
							cooldown);
					if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
						getPluginInstance().getManager().sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-cooldown"))
								.replace("{duration}", String.valueOf(currentCooldown)), player);
						return;
					}

					if (getPluginInstance().getConfig().getBoolean("general-section.use-vault")
							&& !player.hasPermission("hyperdrive.economybypass")
							&& !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())
							&& !warp.getAssistants().contains(player.getUniqueId())
							&& (warp.getWhiteList().contains(player.getUniqueId()))) {
						EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(player,
								warp.getUsagePrice());
						if (!economyResponse.transactionSuccess()) {
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.insufficient-funds"))
											.replace("{amount}", String.valueOf(warp.getUsagePrice())), player);
							return;
						} else
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.transaction-success"))
											.replace("{amount}", String.valueOf(warp.getUsagePrice())), player);
					}

					if (warp.getAnimationSet().contains(":")) {
						String[] themeArgs = warp.getAnimationSet().split(":");
						String delayTheme = themeArgs[1];
						if (delayTheme.contains("/")) {
							String[] delayThemeArgs = delayTheme.split("/");
							getPluginInstance().getTeleportationHandler().getAnimation().stopActiveAnimation(player);
							getPluginInstance().getTeleportationHandler().getAnimation()
									.playAnimation(player, delayThemeArgs[1], EnumContainer.Animation.valueOf(
											delayThemeArgs[0].toUpperCase().replace(" ", "_").replace("-", "_")),
											duration);
						}
					}

					String title = getPluginInstance().getConfig().getString("teleportation-section.start-title"),
							subTitle = getPluginInstance().getConfig()
									.getString("teleportation-section.start-sub-title");
					if (title != null && subTitle != null)
						getPluginInstance().getManager().sendTitle(player,
								title.replace("{duration}", String.valueOf(duration)).replace("{warp}",
										warp.getWarpName()),
								subTitle.replace("{duration}", String.valueOf(duration)).replace("{warp}",
										warp.getWarpName()),
								0, 5, 0);

					getPluginInstance().getManager().sendActionBar(player, Objects
							.requireNonNull(getPluginInstance().getConfig()
									.getString("teleportation-section.start-bar-message"))
							.replace("{duration}", String.valueOf(duration)).replace("{warp}", warp.getWarpName()));

					getPluginInstance().getManager().sendCustomMessage(
							Objects.requireNonNull(
									getPluginInstance().getConfig().getString("language-section.teleportation-start"))
									.replace("{warp}", warp.getWarpName())
									.replace("{duration}", String.valueOf(duration)),
							player);

					getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "warp", warp.getWarpName(),
							duration);
					return;
				} else {
					getPluginInstance().getManager().sendCustomMessage(
							getPluginInstance().getConfig().getString("language-section.no-permission"), player);
					return;
				}

			case "group warp":

				warpName = sign.getLine(2);
				if (!getPluginInstance().getManager().doesWarpExist(warpName)) {
					getPluginInstance().getManager().sendCustomMessage(
							getPluginInstance().getConfig().getString("language-section.sign-warp-invalid"), player);
					return;
				}

				warp = getPluginInstance().getManager().getWarp(warpName);
				if (warp.getStatus() == EnumContainer.Status.PUBLIC
						|| ((player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())
								|| warp.getAssistants().contains(player.getUniqueId())
								|| player.hasPermission("hyperdrive.warps." + warpName)
								|| player.hasPermission("hyperdrive.warps.*")
								|| (warp.getWhiteList().contains(player.getUniqueId())))
								&& player.hasPermission("hyperdrive.groups.use"))) {
					player.closeInventory();

					int cooldown = getPluginInstance().getConfig().getInt("teleportation-section.cooldown-duration");
					long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp",
							cooldown);
					if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
						getPluginInstance().getManager().sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.warp-cooldown"))
								.replace("{duration}", String.valueOf(currentCooldown)), player);
						return;
					}

					if (getPluginInstance().getConfig().getBoolean("general-section.use-vault")
							&& !player.hasPermission("hyperdrive.economybypass")
							&& !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())
							&& !warp.getAssistants().contains(player.getUniqueId())
							&& (warp.getWhiteList().contains(player.getUniqueId()))) {
						EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(player,
								warp.getUsagePrice());
						if (!economyResponse.transactionSuccess()) {
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.insufficient-funds"))
											.replace("{amount}", String.valueOf(warp.getUsagePrice())), player);
							return;
						} else
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.transaction-success"))
											.replace("{amount}", String.valueOf(warp.getUsagePrice())), player);
					}

					Destination destination = new Destination(warp.getWarpLocation());
					destination.setWarp(warp);
					getPluginInstance().getTeleportationHandler().updateDestination(player, destination);
					List<UUID> playerList = getPluginInstance().getManager().getPlayerUUIDs();
					playerList.remove(player.getUniqueId());
					if (playerList.size() <= 0) {
						getPluginInstance().getManager().sendCustomMessage(
								getPluginInstance().getConfig().getString("language-section.no-players-found"), player);
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
							getPluginInstance().getConfig().getString("language-section.group-selection-start"),
							player);
					return;
				} else {
					getPluginInstance().getManager().sendCustomMessage(
							getPluginInstance().getConfig().getString("language-section.no-permission"), player);
					return;
				}

			case "rtp":

				if (!player.hasPermission("hyperdrive.use.rtp")) {
					getPluginInstance().getManager().sendCustomMessage(
							getPluginInstance().getConfig().getString("language-section.no-permission"), player);
					return;
				}

				World world = getPluginInstance().getServer().getWorld(sign.getLine(2));
				getPluginInstance().getTeleportationHandler().randomlyTeleportPlayer(player,
						world != null ? world : player.getLocation().getWorld());
				break;

			case "group rtp":

				if (!player.hasPermission("hyperdrive.use.rtpgroup")) {
					getPluginInstance().getManager().sendCustomMessage(
							getPluginInstance().getConfig().getString("language-section.no-permission"), player);
					return;
				}

				List<UUID> onlinePlayers = getPluginInstance().getManager().getPlayerUUIDs();
				onlinePlayers.remove(player.getUniqueId());
				if (onlinePlayers.size() <= 0) {
					getPluginInstance().getManager().sendCustomMessage(
							getPluginInstance().getConfig().getString("language-section.no-players-found"), player);
					return;
				}

				getPluginInstance().getManager().sendCustomMessage(
						getPluginInstance().getConfig().getString("language-section.random-teleport-start"), player);
				getPluginInstance().getTeleportationHandler().getDestinationMap().remove(player.getUniqueId());
				getPluginInstance().getTeleportationHandler().updateDestinationWithRandomLocation(player,
						player.getLocation(), player.getWorld());
				player.openInventory(getPluginInstance().getManager().buildPlayerSelectionMenu(player));
				getPluginInstance().getManager().sendCustomMessage(
						getPluginInstance().getConfig().getString("language-section.player-selection-group"), player);
				break;

			default:
				break;
			}
		}
	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {
		List<String> commandStrings = getPluginInstance().getConfig()
				.getStringList("general-section.custom-alias-commands");
		for (int i = -1; ++i < commandStrings.size();) {
			String commandString = commandStrings.get(i);
			if (!commandString.contains(":"))
				continue;

			String[] args = commandString.split(":");
			if (e.getMessage().equalsIgnoreCase(args[0]) && args[2].equalsIgnoreCase("player"))
				e.setMessage(args[1].replace("{player}", e.getPlayer().getName()));
			else if (e.getMessage().equalsIgnoreCase(args[0]) && args[2].equalsIgnoreCase("console")) {
				e.setCancelled(true);
				getPluginInstance().getServer().dispatchCommand(getPluginInstance().getServer().getConsoleSender(),
						args[1].replace("{player}", e.getPlayer().getName()));
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
		if (e.getCurrentItem() != null
				&& Objects.requireNonNull(e.getClickedInventory()).getType() != InventoryType.PLAYER) {
			e.setCancelled(true);
			if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick() == ClickType.CREATIVE)
				return;

			List<Integer> warpSlots = getPluginInstance().getConfig().getIntegerList("list-menu-section.warp-slots");
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

						if (warp.getStatus() == EnumContainer.Status.PUBLIC
								|| player.hasPermission("hyperdrive.warps." + warpName)
								|| player.hasPermission("hyperdrive.warps.*")
								|| warp.getOwner().toString().equals(player.getUniqueId().toString())
								|| warp.getAssistants().contains(player.getUniqueId())
								|| warp.getWhiteList().contains(player.getUniqueId())) {
							player.closeInventory();

							if ((getPluginInstance().getConnection() != null
									&& getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql"))) {
								String warpIP = warp.getServerIPAddress().replace("localhost", "127.0.0.1"),
										serverIP = (getPluginInstance().getServer().getIp().equalsIgnoreCase("")
												|| getPluginInstance().getServer().getIp().equalsIgnoreCase("0.0.0.0"))
														? getPluginInstance().getConfig()
																.getString("mysql-connection.default-ip") + ":"
																+ getPluginInstance().getServer().getPort()
														: (getPluginInstance().getServer().getIp().replace("localhost",
																"127.0.0.1") + ":"
																+ getPluginInstance().getServer().getPort());

								if (!warpIP.equalsIgnoreCase(serverIP)) {
									String server = getPluginInstance().getBungeeListener()
											.getServerName(warp.getServerIPAddress());
									if (server == null) {
										getPluginInstance().getManager()
												.sendCustomMessage(Objects
														.requireNonNull(getPluginInstance().getConfig()
																.getString("language-section.ip-ping-fail"))
														.replace("{warp}", warp.getWarpName())
														.replace("{ip}", warp.getServerIPAddress()), player);
										return;
									}
								}
							}

							int duration = !player.hasPermission("hyperdrive.tpdelaybypass") ? getPluginInstance()
									.getConfig().getInt("teleportation-section.warp-delay-duration") : 0,
									cooldown = getPluginInstance().getConfig()
											.getInt("teleportation-section.cooldown-duration");

							long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp",
									cooldown);
							if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
								getPluginInstance().getManager()
										.sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig()
												.getString("language-section.warp-cooldown"))
												.replace("{duration}", String.valueOf(currentCooldown)), player);
								return;
							}

							if (getPluginInstance().getConfig().getBoolean("general-section.use-vault")
									&& !player.hasPermission("hyperdrive.economybypass")
									&& !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())
									&& !warp.getAssistants().contains(player.getUniqueId())
									&& (warp.getWhiteList().contains(player.getUniqueId()))) {
								EconomyChargeEvent economyChargeEvent = new EconomyChargeEvent(player,
										warp.getUsagePrice());
								getPluginInstance().getServer().getPluginManager().callEvent(economyChargeEvent);
								if (!economyChargeEvent.isCancelled()) {
									EconomyResponse economyResponse = getPluginInstance().getVaultEconomy()
											.withdrawPlayer(player, warp.getUsagePrice());
									if (!economyResponse.transactionSuccess()) {
										getPluginInstance().getManager()
												.sendCustomMessage(Objects
														.requireNonNull(getPluginInstance().getConfig()
																.getString("language-section.insufficient-funds"))
														.replace("{amount}", String.valueOf(warp.getUsagePrice())),
														player);
										return;
									} else
										getPluginInstance().getManager()
												.sendCustomMessage(Objects
														.requireNonNull(getPluginInstance().getConfig()
																.getString("language-section.transaction-success"))
														.replace("{amount}", String.valueOf(warp.getUsagePrice())),
														player);
								}
							}

							if (warp.getAnimationSet().contains(":")) {
								String[] themeArgs = warp.getAnimationSet().split(":");
								String delayTheme = themeArgs[1];
								if (delayTheme.contains("/")) {
									String[] delayThemeArgs = delayTheme.split("/");
									getPluginInstance().getTeleportationHandler().getAnimation()
											.stopActiveAnimation(player);
									getPluginInstance()
											.getTeleportationHandler().getAnimation().playAnimation(player,
													delayThemeArgs[1], EnumContainer.Animation.valueOf(delayThemeArgs[0]
															.toUpperCase().replace(" ", "_").replace("-", "_")),
													duration);
								}
							}

							String title = getPluginInstance().getConfig()
									.getString("teleportation-section.start-title"),
									subTitle = getPluginInstance().getConfig()
											.getString("teleportation-section.start-sub-title");
							if (title != null && subTitle != null)
								getPluginInstance().getManager().sendTitle(player,
										title.replace("{duration}", String.valueOf(duration)).replace("{warp}",
												warp.getWarpName()),
										subTitle.replace("{duration}", String.valueOf(duration)).replace("{warp}",
												warp.getWarpName()),
										0, 5, 0);

							getPluginInstance().getManager().sendActionBar(player,
									Objects.requireNonNull(getPluginInstance().getConfig()
											.getString("teleportation-section.start-bar-message"))
											.replace("{duration}", String.valueOf(duration))
											.replace("{warp}", warp.getWarpName()));

							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.teleportation-start"))
											.replace("{warp}", warp.getWarpName())
											.replace("{duration}", String.valueOf(duration)), player);

							getPluginInstance().getTeleportationHandler().updateTeleportTemp(player, "warp",
									warp.getWarpName(), duration);
							return;
						}

						break;

					case RIGHT:

						if ((warp.getStatus() == EnumContainer.Status.PUBLIC
								|| player.hasPermission("hyperdrive.warps." + warpName)
								|| player.hasPermission("hyperdrive.warps.*")
								|| warp.getOwner().toString().equals(player.getUniqueId().toString())
								|| warp.getAssistants().contains(player.getUniqueId())
								|| warp.getWhiteList().contains(player.getUniqueId()))
								&& player.hasPermission("hyperdrive.groups.use")) {
							player.closeInventory();

							if ((getPluginInstance().getConnection() != null
									&& getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql"))) {
								String warpIP = warp.getServerIPAddress().replace("localhost", "127.0.0.1"),
										serverIP = (getPluginInstance().getServer().getIp().equalsIgnoreCase("")
												|| getPluginInstance().getServer().getIp().equalsIgnoreCase("0.0.0.0"))
														? getPluginInstance().getConfig()
																.getString("mysql-connection.default-ip") + ":"
																+ getPluginInstance().getServer().getPort()
														: (getPluginInstance().getServer().getIp().replace("localhost",
																"127.0.0.1") + ":"
																+ getPluginInstance().getServer().getPort());

								if (!warpIP.equalsIgnoreCase(serverIP)) {
									String server = getPluginInstance().getBungeeListener()
											.getServerName(warp.getServerIPAddress());
									if (server == null) {
										getPluginInstance().getManager()
												.sendCustomMessage(Objects
														.requireNonNull(getPluginInstance().getConfig()
																.getString("language-section.ip-ping-fail"))
														.replace("{warp}", warp.getWarpName())
														.replace("{ip}", warp.getServerIPAddress()), player);
										return;
									}
								}
							}

							int cooldown = getPluginInstance().getConfig()
									.getInt("teleportation-section.cooldown-duration");
							long currentCooldown = getPluginInstance().getManager().getCooldownDuration(player, "warp",
									cooldown);
							if (currentCooldown > 0 && !player.hasPermission("hyperdrive.tpcooldown")) {
								getPluginInstance().getManager()
										.sendCustomMessage(Objects.requireNonNull(getPluginInstance().getConfig()
												.getString("language-section.warp-cooldown"))
												.replace("{duration}", String.valueOf(currentCooldown)), player);
								return;
							}

							if (getPluginInstance().getConfig().getBoolean("general-section.use-vault")
									&& !player.hasPermission("hyperdrive.economybypass")
									&& !player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())
									&& !warp.getAssistants().contains(player.getUniqueId())
									&& (warp.getWhiteList().contains(player.getUniqueId()))) {
								EconomyChargeEvent economyChargeEvent = new EconomyChargeEvent(player,
										warp.getUsagePrice());
								getPluginInstance().getServer().getPluginManager().callEvent(economyChargeEvent);
								if (!economyChargeEvent.isCancelled()) {
									EconomyResponse economyResponse = getPluginInstance().getVaultEconomy()
											.withdrawPlayer(player, warp.getUsagePrice());
									if (!economyResponse.transactionSuccess()) {
										getPluginInstance().getManager()
												.sendCustomMessage(Objects
														.requireNonNull(getPluginInstance().getConfig()
																.getString("language-section.insufficient-funds"))
														.replace("{amount}", String.valueOf(warp.getUsagePrice())),
														player);
										return;
									} else
										getPluginInstance().getManager()
												.sendCustomMessage(Objects
														.requireNonNull(getPluginInstance().getConfig()
																.getString("language-section.transaction-success"))
														.replace("{amount}", String.valueOf(warp.getUsagePrice())),
														player);
								}
							}

							Destination destination = new Destination(warp.getWarpLocation());
							destination.setWarp(warp);
							getPluginInstance().getTeleportationHandler().updateDestination(player, destination);
							List<UUID> playerList = getPluginInstance().getManager().getPlayerUUIDs();
							playerList.remove(player.getUniqueId());
							if (playerList.size() <= 0) {
								getPluginInstance().getManager().sendCustomMessage(
										getPluginInstance().getConfig().getString("language-section.no-players-found"),
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
									getPluginInstance().getConfig().getString("language-section.group-selection-start"),
									player);
							return;
						}

						break;

					case SHIFT_LEFT:

					case SHIFT_RIGHT:

						if (player.hasPermission("hyperdrive.admin.edit") || player.hasPermission("hyperdrive.edit.*")
								|| player.hasPermission("hyperdrive.edit." + warpName)
								|| player.getUniqueId().toString().equalsIgnoreCase(warp.getOwner().toString())
								|| warp.getAssistants().contains(player.getUniqueId())) {
							Inventory inventory = getPluginInstance().getManager().buildEditMenu(warp);

							MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(),
									EnumContainer.MenuType.EDIT, inventory, player.getPlayer());
							getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
							if (menuOpenEvent.isCancelled())
								return;

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
				if (getPluginInstance().getConfig().getBoolean("list-menu-section.items." + itemId + ".click-sound")) {
					String soundName = getPluginInstance().getConfig()
							.getString("list-menu-section.items." + itemId + ".sound-name");
					if (soundName != null && !soundName.equalsIgnoreCase(""))
						player.playSound(player.getLocation(),
								Sound.valueOf(soundName.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);
				}

				ConfigurationSection cs = getPluginInstance().getConfig()
						.getConfigurationSection("list-menu-section.items." + itemId);
				if (cs != null && cs.getKeys(false).contains("permission")) {
					String permission = getPluginInstance().getConfig()
							.getString("list-menu-section.items." + itemId + ".permission");
					if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
						getPluginInstance().getManager().sendCustomMessage(
								getPluginInstance().getConfig().getString("language-section.no-permission"), player);
						return;
					}
				}

				String ownFormat = getPluginInstance().getConfig().getString("list-menu-section.own-status-format"),
						publicFormat = getPluginInstance().getConfig()
								.getString("list-menu-section.public-status-format"),
						privateFormat = getPluginInstance().getConfig()
								.getString("list-menu-section.private-status-format"),
						adminFormat = getPluginInstance().getConfig()
								.getString("list-menu-section.admin-status-format"),
						featuredFormat = getPluginInstance().getConfig()
								.getString("list-menu-section.featured-status-format"),
						clickAction = getPluginInstance().getConfig()
								.getString("list-menu-section.items." + itemId + ".click-action");
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("list-menu-section.items." + itemId + ".click-message"))
								.replace("{player}", player.getName()).replace("{item-id}", itemId), player);

				if (clickAction != null) {
					String action = clickAction, value = "";
					if (clickAction.contains(":")) {
						String[] actionArgs = clickAction.toLowerCase().split(":");
						action = actionArgs[0].replace(" ", "-").replace("_", "-");
						value = actionArgs[1].replace("{player}", player.getName());
					}

					boolean useVault = getPluginInstance().getConfig().getBoolean("general-section.use-vault"),
							charged = false;
					double itemUsageCost = getPluginInstance().getConfig()
							.getDouble("list-menu-section.items." + itemId + ".usage-cost");
					if (useVault && itemUsageCost > 0
							&& !getPluginInstance().getManager().isChatInteractionId(action)) {
						EconomyChargeEvent economyChargeEvent = new EconomyChargeEvent(player, itemUsageCost);
						getPluginInstance().getServer().getPluginManager().callEvent(economyChargeEvent);
						if (!economyChargeEvent.isCancelled()) {
							EconomyResponse economyResponse = getPluginInstance().getVaultEconomy()
									.withdrawPlayer(player, itemUsageCost);
							if (!economyResponse.transactionSuccess()) {
								getPluginInstance().getManager().updateLastTransactionAmount(player, itemUsageCost);
								charged = true;
								getPluginInstance().getManager()
										.sendCustomMessage(Objects
												.requireNonNull(getPluginInstance().getConfig()
														.getString("language-section.insufficient-funds"))
												.replace("{amount}", String.valueOf(itemUsageCost))
												.replace("{player}", player.getName()), player);
								return;
							} else
								getPluginInstance().getManager()
										.sendCustomMessage(Objects
												.requireNonNull(getPluginInstance().getConfig()
														.getString("language-section.transaction-success"))
												.replace("{amount}", String.valueOf(itemUsageCost))
												.replace("{player}", player.getName()), player);
						}
					}

					HashMap<Integer, List<Warp>> warpPageMap;
					int currentPage;
					List<Warp> pageWarpList;
					switch (action) {
					case "dispatch-command-console":
						if (!value.equalsIgnoreCase("")) {
							player.closeInventory();
							getPluginInstance().getServer()
									.dispatchCommand(getPluginInstance().getServer().getConsoleSender(), value);
						}

						break;
					case "dispatch-command-player":
						if (!value.equalsIgnoreCase("")) {
							player.closeInventory();
							getPluginInstance().getServer().dispatchCommand(player, value);
						}

						break;
					case "create-warp":
						player.closeInventory();

						if (!getPluginInstance().getTeleportationHandler().isLocationHookSafe(player,
								player.getLocation())) {
							getPluginInstance().getManager().sendCustomMessage(
									getPluginInstance().getConfig().getString("language-section.not-hook-safe"),
									player);
							break;
						}

						if (getPluginInstance().getManager().hasMetWarpLimit(player)) {
							getPluginInstance().getManager().sendCustomMessage(
									getPluginInstance().getConfig().getString("language-section.warp-limit-met"),
									player);
							return;
						}

						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "create-warp", "",
									!charged ? itemUsageCost : 0);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.create-warp-interaction"))
											.replace("{cancel}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("general-section.chat-interaction-cancel")))
											.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);
						break;
					case "refresh":
						getPluginInstance().getManager().getPaging().resetWarpPages(player);
						String currentStatus = getPluginInstance().getManager()
								.getCurrentFilterStatus("list-menu-section", e.getInventory());
						warpPageMap = getPluginInstance().getManager().getPaging().getWarpPages(player,
								"list-menu-section", currentStatus);
						getPluginInstance().getManager().getPaging().getWarpPageMap().put(player.getUniqueId(),
								warpPageMap);
						currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
						pageWarpList = new ArrayList<>();
						if (warpPageMap != null && !warpPageMap.isEmpty() && warpPageMap.containsKey(currentPage))
							pageWarpList = new ArrayList<>(warpPageMap.get(currentPage));

						if (!pageWarpList.isEmpty())
							for (int i = -1; ++i < warpSlots.size();) {
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
									getPluginInstance().getConfig().getString("language-section.refresh-fail"), player);

						break;
					case "next-page":
						if (getPluginInstance().getManager().getPaging().hasNextWarpPage(player)) {
							warpPageMap = getPluginInstance().getManager().getPaging().getWarpPages(player,
									"list-menu-section", getPluginInstance().getManager()
											.getCurrentFilterStatus("list-menu-section", e.getInventory()));
							getPluginInstance().getManager().getPaging().getWarpPageMap().put(player.getUniqueId(),
									warpPageMap);
							currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
							pageWarpList = new ArrayList<>();
							if (warpPageMap != null && !warpPageMap.isEmpty()
									&& warpPageMap.containsKey(currentPage + 1))
								pageWarpList = new ArrayList<>(warpPageMap.get(currentPage + 1));

							if (!pageWarpList.isEmpty()) {
								getPluginInstance().getManager().getPaging().updateCurrentWarpPage(player, true);
								for (int i = -1; ++i < warpSlots.size();) {
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
							getPluginInstance().getManager().sendCustomMessage(
									getPluginInstance().getConfig().getString("language-section.no-next-page"), player);

						break;
					case "previous-page":
						if (getPluginInstance().getManager().getPaging().hasPreviousWarpPage(player)) {
							warpPageMap = getPluginInstance().getManager().getPaging()
									.getCurrentWarpPages(player) == null
											? getPluginInstance().getManager().getPaging().getWarpPages(player,
													"list-menu-section",
													getPluginInstance().getManager().getCurrentFilterStatus(
															"list-menu-section", e.getInventory()))
											: getPluginInstance().getManager().getPaging().getCurrentWarpPages(player);
							getPluginInstance().getManager().getPaging().getWarpPageMap().put(player.getUniqueId(),
									warpPageMap);

							currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
							pageWarpList = new ArrayList<>();
							if (warpPageMap != null && !warpPageMap.isEmpty()
									&& warpPageMap.containsKey(currentPage - 1))
								pageWarpList = new ArrayList<>(warpPageMap.get(currentPage - 1));

							if (!pageWarpList.isEmpty()) {
								getPluginInstance().getManager().getPaging().updateCurrentWarpPage(player, false);
								for (int i = -1; ++i < warpSlots.size();) {
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
							getPluginInstance().getManager().sendCustomMessage(
									getPluginInstance().getConfig().getString("language-section.no-previous-page"),
									player);

						break;
					case "filter-switch":
						String statusFromItem = getPluginInstance().getManager()
								.getFilterStatusFromItem(e.getCurrentItem(), "list-menu-section", itemId);
						if (statusFromItem != null) {
							int index = -1;
							boolean isOwnFormat = statusFromItem.equalsIgnoreCase(ownFormat),
									isPublicFormat = statusFromItem.equalsIgnoreCase(publicFormat),
									isPrivateFormat = statusFromItem.equalsIgnoreCase(privateFormat),
									isAdminFormat = statusFromItem.equalsIgnoreCase(adminFormat),
									isFeaturedFormat = statusFromItem.equalsIgnoreCase(adminFormat);

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

							for (int i = -1; ++i < warpSlots.size();) {
								int warpSlot = warpSlots.get(i);
								e.getInventory().setItem(warpSlot, null);
							}

							getPluginInstance().getManager().getPaging().resetWarpPages(player);
							warpPageMap = getPluginInstance().getManager().getPaging().getWarpPages(player,
									"list-menu-section", nextStatus);

							getPluginInstance().getManager().getPaging().getWarpPageMap().put(player.getUniqueId(),
									warpPageMap);
							currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
							pageWarpList = new ArrayList<>();
							if (warpPageMap != null && !warpPageMap.isEmpty() && warpPageMap.containsKey(currentPage))
								pageWarpList = new ArrayList<>(warpPageMap.get(currentPage));

							if (!pageWarpList.isEmpty())
								for (int i = -1; ++i < warpSlots.size();) {
									if (pageWarpList.size() >= 1) {
										Warp warp = pageWarpList.get(0);
										e.getInventory().setItem(warpSlots.get(i),
												getPluginInstance().getManager().buildWarpIcon(player, warp));
										pageWarpList.remove(warp);
									}
								}
						}

						break;
					case "open-custom-menu":
						if (!value.equalsIgnoreCase("")) {
							player.closeInventory();

							Inventory inventory = getPluginInstance().getManager().buildCustomMenu(player, value);

							MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(),
									EnumContainer.MenuType.CUSTOM, inventory, player.getPlayer());
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

	private void runEditMenuClick(Player player, String inventoryName, InventoryClickEvent e) {
		if (e.getCurrentItem() != null
				&& Objects.requireNonNull(e.getClickedInventory()).getType() != InventoryType.PLAYER) {
			e.setCancelled(true);
			if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick() == ClickType.CREATIVE)
				return;

			String itemId = getPluginInstance().getManager().getIdFromSlot("edit-menu-section", e.getSlot());
			if (itemId != null) {
				if (getPluginInstance().getConfig().getBoolean("edit-menu-section.items." + itemId + ".click-sound")) {
					String soundName = getPluginInstance().getConfig()
							.getString("edit-menu-section.items." + itemId + ".sound-name");
					if (soundName != null && !soundName.equalsIgnoreCase(""))
						player.playSound(player.getLocation(),
								Sound.valueOf(soundName.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);
				}

				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("edit-menu-section.items." + itemId + ".click-message"))
								.replace("{player}", player.getName()).replace("{item-id}", itemId), player);

				String clickAction = getPluginInstance().getConfig()
						.getString("edit-menu-section.items." + itemId + ".click-action"),
						toggleFormat = getPluginInstance().getConfig()
								.getString("general-section.option-toggle-format"),
						warpName = Objects.requireNonNull(ChatColor.stripColor(inventoryName))
								.replace(
										ChatColor.stripColor(getPluginInstance().getManager().colorText(
												getPluginInstance().getConfig().getString("edit-menu-section.title"))),
										"");
				Warp warp = getPluginInstance().getManager().getWarp(warpName);

				ConfigurationSection cs = getPluginInstance().getConfig()
						.getConfigurationSection("edit-menu-section.items." + itemId);
				if (cs != null && cs.getKeys(false).contains("permission")) {
					String permission = getPluginInstance().getConfig()
							.getString("edit-menu-section.items." + itemId + ".permission");
					if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
						getPluginInstance().getManager().sendCustomMessage(
								getPluginInstance().getConfig().getString("language-section.no-permission"), player);
						return;
					}
				}

				if (clickAction != null) {

					boolean useVault = getPluginInstance().getConfig().getBoolean("general-section.use-vault"),
							charged = false;
					double itemUsageCost = getPluginInstance().getConfig()
							.getDouble("edit-menu-section.items." + itemId + ".usage-cost");
					if (useVault && itemUsageCost > 0
							&& !getPluginInstance().getManager().isChatInteractionId(clickAction)) {
						EconomyChargeEvent economyChargeEvent = new EconomyChargeEvent(player, itemUsageCost);
						getPluginInstance().getServer().getPluginManager().callEvent(economyChargeEvent);
						if (!economyChargeEvent.isCancelled()) {
							EconomyResponse economyResponse = getPluginInstance().getVaultEconomy()
									.withdrawPlayer(player, itemUsageCost);
							if (!economyResponse.transactionSuccess()) {
								getPluginInstance().getManager().updateLastTransactionAmount(player, itemUsageCost);
								charged = true;
								getPluginInstance().getManager()
										.sendCustomMessage(Objects
												.requireNonNull(getPluginInstance().getConfig()
														.getString("language-section.insufficient-funds"))
												.replace("{amount}", String.valueOf(itemUsageCost))
												.replace("{player}", player.getName()), player);
								return;
							} else
								getPluginInstance().getManager()
										.sendCustomMessage(Objects
												.requireNonNull(getPluginInstance().getConfig()
														.getString("language-section.transaction-success"))
												.replace("{amount}", String.valueOf(itemUsageCost))
												.replace("{player}", player.getName()), player);
						}
					}

					if (clickAction.toLowerCase().startsWith("open-menu") && clickAction.contains(":")) {
						String[] actionArgs = clickAction.split(":");
						if (actionArgs[1].equalsIgnoreCase("List Menu")
								|| actionArgs[1].equalsIgnoreCase("List-Menu")) {
							player.closeInventory();
							player.openInventory(getPluginInstance().getManager().buildListMenu(player));
							return;
						} else {
							player.closeInventory();
							player.openInventory(
									getPluginInstance().getManager().buildCustomMenu(player, actionArgs[1]));
							return;
						}
					}

					String value = "";
					if (clickAction.contains(":")) {
						String[] actionArgs = clickAction.toLowerCase().split(":");
						clickAction = actionArgs[0].replace(" ", "-").replace("_", "-");
						value = actionArgs[1].replace("{player}", player.getName());
					}

					String publicFormat = getPluginInstance().getConfig()
							.getString("list-menu-section.public-status-format"),
							privateFormat = getPluginInstance().getConfig()
									.getString("list-menu-section.private-status-format"),
							adminFormat = getPluginInstance().getConfig()
									.getString("list-menu-section.admin-status-format");
					boolean useMySQL = getPluginInstance().getConfig().getBoolean("mysql-connection.use-mysql");

					switch (clickAction) {
					case "dispatch-command-console":

						if (!value.equalsIgnoreCase(""))
							getPluginInstance().getServer()
									.dispatchCommand(getPluginInstance().getServer().getConsoleSender(), value);
						break;

					case "dispatch-command-player":

						if (!value.equalsIgnoreCase(""))
							getPluginInstance().getServer().dispatchCommand(player, value);
						break;

					case "open-custom-menu":

						if (!value.equalsIgnoreCase("")) {
							if (value.equalsIgnoreCase("List Menu") || value.equalsIgnoreCase("List-Menu")) {
								player.closeInventory();

								Inventory inventory = getPluginInstance().getManager().buildListMenu(player);

								MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(),
										EnumContainer.MenuType.LIST, inventory, player.getPlayer());
								getPluginInstance().getServer().getPluginManager().callEvent(menuOpenEvent);
								if (menuOpenEvent.isCancelled())
									return;

								player.openInventory(inventory);
								return;
							} else {
								player.closeInventory();
								Inventory inventory = getPluginInstance().getManager().buildCustomMenu(player, value);

								MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(),
										EnumContainer.MenuType.CUSTOM, inventory, player.getPlayer());
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
						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "rename",
									warp != null ? warp.getWarpName() : warpName, !charged ? itemUsageCost : 0);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.rename-warp-interaction"))
											.replace("{cancel}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("general-section.chat-interaction-cancel")))
											.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);

						break;

					case "delete":

						player.closeInventory();
						if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
								|| (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
							warp.unRegister();
							warp.deleteSaved(true);
							getPluginInstance().getManager().sendCustomMessage(Objects
									.requireNonNull(
											getPluginInstance().getConfig().getString("language-section.warp-deleted"))
									.replace("{warp}", warp.getWarpName()), player);
						}

						break;

					case "relocate":

						player.closeInventory();
						if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
								|| (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
							warp.setWarpLocation(player.getLocation());
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.warp-relocated"))
											.replace("{warp}", warp.getWarpName())
											.replace("{x}", String.valueOf((int) warp.getWarpLocation().getX()))
											.replace("{y}", String.valueOf((int) warp.getWarpLocation().getY()))
											.replace("{z}", String.valueOf((int) warp.getWarpLocation().getZ()))
											.replace("{world}", warp.getWarpLocation().getWorldName()), player);
							getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
									() -> getPluginInstance().saveWarp(warp, useMySQL));
						} else {
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.warp-no-longer-exists"))
											.replace("{warp}", warp.getWarpName()), player);
						}

						break;

					case "change-status":

						player.closeInventory();

						if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
								|| (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
							EnumContainer.Status[] statusList = EnumContainer.Status.values();
							EnumContainer.Status nextStatus = EnumContainer.Status.PUBLIC,
									previousStatus = warp.getStatus();
							for (int i = -1; ++i < statusList.length;) {
								EnumContainer.Status status = statusList[i];
								if (status == previousStatus) {
									int nextIndex = (i + 1);
									nextStatus = statusList[nextIndex >= statusList.length ? 0 : nextIndex];
									if (!player.hasPermission("hyperdrive.admin.status")
											&& nextStatus == EnumContainer.Status.ADMIN)
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

							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.warp-status-changed"))
											.replace("{warp}", warp.getWarpName())
											.replace("{next-status}", Objects.requireNonNull(nextStatusName))
											.replace("{previous-status}", Objects.requireNonNull(previousStatusName)),
											player);
							getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
									() -> getPluginInstance().saveWarp(warp, useMySQL));
						} else {
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.warp-no-longer-exists"))
											.replace("{warp}", warp.getWarpName()), player);
						}

						player.openInventory(getPluginInstance().getManager().buildEditMenu(warp));
						break;

					case "give-ownership":

						player.closeInventory();
						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "give-ownership",
									warp != null ? warp.getWarpName() : warpName, !charged ? itemUsageCost : 0);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.give-ownership-interaction"))
											.replace("{cancel}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("general-section.chat-interaction-cancel")))
											.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);

						break;

					case "edit-description":

						player.closeInventory();
						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "edit-description",
									warp != null ? warp.getWarpName() : warpName, !charged ? itemUsageCost : 0);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.edit-description-interaction"))
											.replace("{cancel}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("general-section.chat-interaction-cancel")))
											.replace("{clear}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("warp-icon-section.description-clear-symbol")))
											.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);

						break;

					case "change-name-color":

						player.closeInventory();
						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "change-name-color",
									warp != null ? warp.getWarpName() : warpName, !charged ? itemUsageCost : 0);
							getPluginInstance().getManager().sendCustomMessage(Objects
									.requireNonNull(getPluginInstance().getConfig()
											.getString("language-section.change-name-color-interaction"))
									.replace("{cancel}",
											Objects.requireNonNull(getPluginInstance().getConfig()
													.getString("general-section.chat-interaction-cancel")))
									.replace("{colors}", getPluginInstance().getManager().getColorNames().toString())
									.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);

						break;

					case "change-description-color":

						player.closeInventory();
						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "change-description-color",
									warp != null ? warp.getWarpName() : warpName, !charged ? itemUsageCost : 0);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.change-description-color-interaction"))
											.replace("{cancel}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("general-section.chat-interaction-cancel")))
											.replace("{colors}",
													getPluginInstance().getManager().getColorNames().toString())
											.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);

						break;

					case "change-usage-price":

						player.closeInventory();
						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "change-usage-price",
									warp != null ? warp.getWarpName() : warpName, !charged ? itemUsageCost : 0);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.change-usage-price-interaction"))
											.replace("{cancel}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("general-section.chat-interaction-cancel")))
											.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);

						break;

					case "give-assistant":

						player.closeInventory();
						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "give-assistant",
									warp != null ? warp.getWarpName() : warpName, !charged ? itemUsageCost : 0);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.give-assistant-interaction"))
											.replace("{cancel}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("general-section.chat-interaction-cancel")))
											.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);

						break;

					case "remove-assistant":

						player.closeInventory();
						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "remove-assistant",
									warp != null ? warp.getWarpName() : warpName, !charged ? itemUsageCost : 0);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.remove-assistant-interaction"))
											.replace("{cancel}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("general-section.chat-interaction-cancel")))
											.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);

						break;

					case "add-to-whitelist":

						player.closeInventory();
						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "add-to-whitelist",
									warp != null ? warp.getWarpName() : warpName, !charged ? itemUsageCost : 0);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.add-whitelist-interaction"))
											.replace("{cancel}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("general-section.chat-interaction-cancel")))
											.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);

						break;

					case "remove-from-whitelist":

						player.closeInventory();
						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "remove-from-whitelist",
									warp != null ? warp.getWarpName() : warpName, !charged ? itemUsageCost : 0);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.remove-whitelist-interaction"))
											.replace("{cancel}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("general-section.chat-interaction-cancel")))
											.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);

						break;

					case "add-command":

						player.closeInventory();
						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "add-command",
									warp != null ? warp.getWarpName() : warpName, !charged ? itemUsageCost : 0);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.add-command-interaction"))
											.replace("{cancel}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("general-section.chat-interaction-cancel")))
											.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);

						break;

					case "remove-command":

						player.closeInventory();
						if (!getPluginInstance().getManager().isInChatInteraction(player)) {
							getPluginInstance().getManager().updateChatInteraction(player, "remove-command",
									warp != null ? warp.getWarpName() : warpName, !charged ? itemUsageCost : 0);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.remove-command-interaction"))
											.replace("{cancel}",
													Objects.requireNonNull(getPluginInstance().getConfig()
															.getString("general-section.chat-interaction-cancel")))
											.replace("{player}", player.getName()), player);
						} else
							getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
									.getString("language-section.interaction-already-active"), player);

						break;

					case "toggle-enchant-look":

						player.closeInventory();
						if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
								|| (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
							warp.setIconEnchantedLook(!warp.hasIconEnchantedLook());

							if (toggleFormat != null && toggleFormat.contains(":")) {
								String[] toggleFormatArgs = toggleFormat.split(":");
								getPluginInstance().getManager()
										.sendCustomMessage(Objects
												.requireNonNull(getPluginInstance().getConfig()
														.getString("language-section.enchanted-look-toggle"))
												.replace("{warp}", warp.getWarpName())
												.replace("{status}", warp.hasIconEnchantedLook() ? toggleFormatArgs[0]
														: toggleFormatArgs[1]),
												player);
							} else
								getPluginInstance().getManager()
										.sendCustomMessage(Objects
												.requireNonNull(getPluginInstance().getConfig()
														.getString("language-section.enchanted-look-toggle"))
												.replace("{warp}", warp.getWarpName()).replace("{status}",
														warp.hasIconEnchantedLook() ? "&aEnabled" : "&cDisabled"),
												player);
							getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
									() -> getPluginInstance().saveWarp(warp, useMySQL));
						} else {
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.warp-no-longer-exists"))
											.replace("{warp}", warp.getWarpName()), player);
						}

						player.openInventory(getPluginInstance().getManager().buildEditMenu(warp));
						break;

					case "change-icon-theme":

						player.closeInventory();
						if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
								|| (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
							String nextIconTheme = getPluginInstance().getManager().getNextIconTheme(warp);
							if (nextIconTheme != null && nextIconTheme.contains(":")) {
								warp.setIconTheme(nextIconTheme);
								getPluginInstance().getManager()
										.sendCustomMessage(Objects
												.requireNonNull(getPluginInstance().getConfig()
														.getString("language-section.theme-changed"))
												.replace("{warp}", warp.getWarpName())
												.replace("{theme}", nextIconTheme.split(":")[0]), player);
							} else
								getPluginInstance().getManager().sendCustomMessage(
										getPluginInstance().getConfig().getString("language-section.theme-invalid"),
										player);
							getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
									() -> getPluginInstance().saveWarp(warp, useMySQL));
						}

						player.openInventory(getPluginInstance().getManager().buildEditMenu(warp));
						break;

					case "change-animation-set":

						player.closeInventory();
						if ((useMySQL && getPluginInstance().doesWarpExistInDatabase(warp.getWarpName()))
								|| (!useMySQL && getPluginInstance().getManager().doesWarpExist(warp.getWarpName()))) {
							String nextAnimationSet = getPluginInstance().getManager().getNextAnimationSet(warp);
							if (nextAnimationSet != null) {
								warp.setAnimationSet(nextAnimationSet);
								getPluginInstance().getManager()
										.sendCustomMessage(Objects
												.requireNonNull(getPluginInstance().getConfig()
														.getString("language-section.animation-set-changed"))
												.replace("{warp}", warp.getWarpName())
												.replace("{animation-set}", nextAnimationSet.split(":")[0]), player);
							} else
								getPluginInstance().getManager().sendCustomMessage(getPluginInstance().getConfig()
										.getString("language-section.animation-set-invalid"), player);
							getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(),
									() -> getPluginInstance().saveWarp(warp, useMySQL));
						} else {
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.warp-no-longer-exists"))
											.replace("{warp}", warp.getWarpName()), player);
						}

						player.openInventory(getPluginInstance().getManager().buildEditMenu(warp));
						break;

					default:
						break;
					}
				}
			}
		}
	}

	private void runPlayerSelectionClick(Player player, InventoryClickEvent e) {
		if (e.getCurrentItem() != null
				&& Objects.requireNonNull(e.getClickedInventory()).getType() != InventoryType.PLAYER) {
			e.setCancelled(true);
			if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick() == ClickType.CREATIVE)
				return;

			List<Integer> playerSlots = getPluginInstance().getConfig().getIntegerList("ps-menu-section.player-slots");
			if (playerSlots.contains(e.getSlot()) && e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()) {
				String soundName = getPluginInstance().getConfig().getString("ps-menu-section.player-click-sound");
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
				if (getPluginInstance().getConfig().getBoolean("ps-menu-section.items." + itemId + ".click-sound")) {
					String soundName = getPluginInstance().getConfig()
							.getString("ps-menu-section.items." + itemId + ".sound-name");
					if (soundName != null && !soundName.equalsIgnoreCase(""))
						player.playSound(player.getLocation(),
								Sound.valueOf(soundName.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);
				}

				ConfigurationSection cs = getPluginInstance().getConfig()
						.getConfigurationSection("ps-menu-section.items." + itemId);
				if (cs != null && cs.getKeys(false).contains("permission")) {
					String permission = getPluginInstance().getConfig()
							.getString("ps-menu-section.items." + itemId + ".permission");
					if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
						getPluginInstance().getManager().sendCustomMessage(
								getPluginInstance().getConfig().getString("language-section.no-permission"), player);
						return;
					}
				}

				boolean useVault = getPluginInstance().getConfig().getBoolean("general-section.use-vault");
				double itemUsageCost = getPluginInstance().getConfig()
						.getDouble("ps-menu-section.items." + itemId + ".usage-cost");

				if (useVault && itemUsageCost > 0) {
					EconomyChargeEvent economyChargeEvent = new EconomyChargeEvent(player, itemUsageCost);
					getPluginInstance().getServer().getPluginManager().callEvent(economyChargeEvent);
					if (!economyChargeEvent.isCancelled()) {
						EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(player,
								itemUsageCost);
						if (!economyResponse.transactionSuccess()) {
							getPluginInstance().getManager().updateLastTransactionAmount(player, itemUsageCost);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.insufficient-funds"))
											.replace("{amount}", String.valueOf(itemUsageCost))
											.replace("{player}", player.getName()), player);
							return;
						} else
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.transaction-success"))
											.replace("{amount}", String.valueOf(itemUsageCost))
											.replace("{player}", player.getName()), player);
					}
				}
				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig()
										.getString("ps-menu-section.items." + itemId + ".click-message"))
								.replace("{player}", player.getName()).replace("{item-id}", itemId), player);

				String clickAction = getPluginInstance().getConfig()
						.getString("ps-menu-section.items." + itemId + ".click-action");
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
							getPluginInstance().getServer()
									.dispatchCommand(getPluginInstance().getServer().getConsoleSender(), value);
						}

						break;
					case "dispatch-command-player":
						if (!value.equalsIgnoreCase("")) {
							player.closeInventory();
							getPluginInstance().getServer().dispatchCommand(player, value);
						}

						break;
					case "confirm":

						player.closeInventory();
						List<UUID> selectedPlayers = getPluginInstance().getManager().getPaging()
								.getSelectedPlayers(player);
						if (selectedPlayers != null && selectedPlayers.size() >= 1) {
							for (int i = -1; ++i < selectedPlayers.size();) {
								UUID selectedPlayerId = selectedPlayers.get(i);
								if (selectedPlayerId == null)
									continue;

								OfflinePlayer offlinePlayer = getPluginInstance().getServer()
										.getOfflinePlayer(selectedPlayerId);
								if (!offlinePlayer.isOnline())
									continue;

								getPluginInstance().getManager()
										.sendCustomMessage(Objects
												.requireNonNull(getPluginInstance().getConfig()
														.getString("language-section.player-selected-teleport"))
												.replace("{player}", player.getName()), offlinePlayer.getPlayer());
							}

							getPluginInstance().getTeleportationHandler().createGroupTemp(player,
									getPluginInstance().getTeleportationHandler().getDestination(player));
							getPluginInstance().getManager().sendCustomMessage(
									getPluginInstance().getConfig().getString("language-section.group-teleport-sent"),
									player);
						} else
							getPluginInstance().getManager().sendCustomMessage(
									getPluginInstance().getConfig().getString("language-section.player-selection-fail"),
									player);

						break;
					case "refresh":
						getPluginInstance().getManager().getPaging().resetPlayerSelectionPages(player);
						playerSelectionPageMap = getPluginInstance().getManager().getPaging()
								.getPlayerSelectionPages(player);
						getPluginInstance().getManager().getPaging().getPlayerSelectionPageMap()
								.put(player.getUniqueId(), playerSelectionPageMap);
						currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
						playerSelectionPageList = new ArrayList<>();
						if (playerSelectionPageMap != null && !playerSelectionPageMap.isEmpty()
								&& playerSelectionPageMap.containsKey(currentPage))
							playerSelectionPageList = new ArrayList<>(playerSelectionPageMap.get(currentPage));

						if (!playerSelectionPageList.isEmpty())
							for (int i = -1; ++i < playerSlots.size();) {
								int playerSlot = playerSlots.get(i);
								e.getInventory().setItem(playerSlot, null);

								if (playerSelectionPageList.size() >= 1) {
									UUID playerUniqueId = playerSelectionPageList.get(0);
									OfflinePlayer offlinePlayer = getPluginInstance().getServer()
											.getOfflinePlayer(playerUniqueId);
									if (!offlinePlayer.isOnline())
										continue;

									List<UUID> sps = getPluginInstance().getManager().getPaging()
											.getSelectedPlayers(player);
									e.getInventory().setItem(playerSlots.get(i),
											getPluginInstance().getManager().getPlayerSelectionHead(player,
													sps != null && sps.contains(offlinePlayer.getUniqueId())));
									playerSelectionPageList.remove(playerUniqueId);
								}
							}
						else
							getPluginInstance().getManager().sendCustomMessage(
									getPluginInstance().getConfig().getString("language-section.refresh-fail"), player);

						break;
					case "next-page":
						if (getPluginInstance().getManager().getPaging().hasNextPlayerSelectionPage(player)) {
							playerSelectionPageMap = getPluginInstance().getManager().getPaging()
									.getPlayerSelectionPages(player);
							getPluginInstance().getManager().getPaging().getPlayerSelectionPageMap()
									.put(player.getUniqueId(), playerSelectionPageMap);
							currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
							playerSelectionPageList = new ArrayList<>();
							if (playerSelectionPageMap != null && !playerSelectionPageMap.isEmpty()
									&& playerSelectionPageMap.containsKey(currentPage + 1))
								playerSelectionPageList = new ArrayList<>(playerSelectionPageMap.get(currentPage + 1));

							if (!playerSelectionPageList.isEmpty()) {
								getPluginInstance().getManager().getPaging().updateCurrentPlayerSelectionPage(player,
										true);
								for (int i = -1; ++i < playerSlots.size();) {
									e.getInventory().setItem(playerSlots.get(i), null);
									if (playerSelectionPageList.size() >= 1) {
										UUID playerUniqueId = playerSelectionPageList.get(0);
										OfflinePlayer offlinePlayer = getPluginInstance().getServer()
												.getOfflinePlayer(playerUniqueId);
										if (!offlinePlayer.isOnline())
											continue;

										List<UUID> sps = getPluginInstance().getManager().getPaging()
												.getSelectedPlayers(player);
										e.getInventory().setItem(playerSlots.get(i),
												getPluginInstance().getManager().getPlayerSelectionHead(player,
														sps != null && sps.contains(offlinePlayer.getUniqueId())));
										playerSelectionPageList.remove(playerUniqueId);
									}
								}
							}
						} else
							getPluginInstance().getManager().sendCustomMessage(
									getPluginInstance().getConfig().getString("language-section.no-next-page"), player);

						break;
					case "previous-page":
						if (getPluginInstance().getManager().getPaging().hasPreviousWarpPage(player)) {
							playerSelectionPageMap = getPluginInstance().getManager().getPaging()
									.getCurrentPlayerSelectionPages(player) == null
											? getPluginInstance().getManager().getPaging()
													.getPlayerSelectionPages(player)
											: getPluginInstance().getManager().getPaging()
													.getCurrentPlayerSelectionPages(player);
							getPluginInstance().getManager().getPaging().getPlayerSelectionPageMap()
									.put(player.getUniqueId(), playerSelectionPageMap);

							currentPage = getPluginInstance().getManager().getPaging().getCurrentPage(player);
							playerSelectionPageList = new ArrayList<>();
							if (playerSelectionPageMap != null && !playerSelectionPageMap.isEmpty()
									&& playerSelectionPageMap.containsKey(currentPage - 1))
								playerSelectionPageList = new ArrayList<>(playerSelectionPageMap.get(currentPage - 1));

							if (!playerSelectionPageList.isEmpty()) {
								getPluginInstance().getManager().getPaging().updateCurrentPlayerSelectionPage(player,
										false);
								for (int i = -1; ++i < playerSlots.size();) {
									e.getInventory().setItem(playerSlots.get(i), null);
									if (playerSelectionPageList.size() >= 1) {
										UUID playerUniqueId = playerSelectionPageList.get(0);
										OfflinePlayer offlinePlayer = getPluginInstance().getServer()
												.getOfflinePlayer(playerUniqueId);
										if (!offlinePlayer.isOnline())
											continue;

										List<UUID> sps = getPluginInstance().getManager().getPaging()
												.getSelectedPlayers(player);
										e.getInventory().setItem(playerSlots.get(i),
												getPluginInstance().getManager().getPlayerSelectionHead(player,
														sps != null && sps.contains(offlinePlayer.getUniqueId())));
										playerSelectionPageList.remove(playerUniqueId);
									}
								}
							}
						} else
							getPluginInstance().getManager().sendCustomMessage(
									getPluginInstance().getConfig().getString("language-section.no-previous-page"),
									player);

						break;
					case "open-custom-menu":
						if (!value.equalsIgnoreCase("")) {
							player.closeInventory();
							Inventory inventory = getPluginInstance().getManager().buildCustomMenu(player, value);

							MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(),
									EnumContainer.MenuType.CUSTOM, inventory, player.getPlayer());
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
		if (e.getCurrentItem() != null
				&& Objects.requireNonNull(e.getClickedInventory()).getType() != InventoryType.PLAYER) {
			e.setCancelled(true);
			if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick() == ClickType.CREATIVE)
				return;

			String itemId = getPluginInstance().getManager().getIdFromSlot("custom-menu-section." + menuId,
					e.getSlot());
			if (itemId != null) {
				if (getPluginInstance().getConfig()
						.getBoolean("custom-menu-section." + menuId + "." + itemId + ".click-sound")) {
					String soundName = getPluginInstance().getConfig()
							.getString("custom-menu-section." + menuId + ".items." + itemId + ".sound-name");
					if (soundName != null && !soundName.equalsIgnoreCase(""))
						player.playSound(player.getLocation(),
								Sound.valueOf(soundName.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);
				}

				getPluginInstance().getManager()
						.sendCustomMessage(Objects
								.requireNonNull(getPluginInstance().getConfig().getString(
										"custom-menu-section." + menuId + ".items." + itemId + ".click-message"))
								.replace("{player}", player.getName()).replace("{item-id}", itemId), player);

				ConfigurationSection cs = getPluginInstance().getConfig()
						.getConfigurationSection("custom-menu-section." + menuId + ".items." + itemId);
				if (cs != null && cs.getKeys(false).contains("permission")) {
					String permission = getPluginInstance().getConfig()
							.getString("custom-menu-section." + menuId + ".items." + itemId + ".permission");
					if (permission != null && !permission.equalsIgnoreCase("") && !player.hasPermission(permission)) {
						getPluginInstance().getManager().sendCustomMessage(
								getPluginInstance().getConfig().getString("language-section.no-permission"), player);
						return;
					}
				}

				boolean useVault = getPluginInstance().getConfig().getBoolean("general-section.use-vault");
				double itemUsageCost = getPluginInstance().getConfig()
						.getDouble("custom-menu-section." + menuId + ".items." + itemId + ".usage-cost");
				if (useVault && itemUsageCost > 0) {
					EconomyChargeEvent economyChargeEvent = new EconomyChargeEvent(player, itemUsageCost);
					getPluginInstance().getServer().getPluginManager().callEvent(economyChargeEvent);
					if (!economyChargeEvent.isCancelled()) {
						EconomyResponse economyResponse = getPluginInstance().getVaultEconomy().withdrawPlayer(player,
								itemUsageCost);
						if (!economyResponse.transactionSuccess()) {
							getPluginInstance().getManager().updateLastTransactionAmount(player, itemUsageCost);
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.insufficient-funds"))
											.replace("{amount}", String.valueOf(itemUsageCost))
											.replace("{player}", player.getName()), player);
							return;
						} else
							getPluginInstance().getManager()
									.sendCustomMessage(Objects
											.requireNonNull(getPluginInstance().getConfig()
													.getString("language-section.transaction-success"))
											.replace("{amount}", String.valueOf(itemUsageCost))
											.replace("{player}", player.getName()), player);
					}
				}

				String clickAction = getPluginInstance().getConfig()
						.getString("custom-menu-section." + menuId + ".items." + itemId + ".click-action");
				if (clickAction != null) {
					String action = clickAction, value = "";
					if (clickAction.contains(":")) {
						String[] actionArgs = clickAction.toLowerCase().split(":");
						action = actionArgs[0].replace(" ", "-").replace("_", "-");
						value = actionArgs[1].replace("{player}", player.getName());
					}

					switch (action) {
					case "dispatch-command-console":
						if (!value.equalsIgnoreCase(""))
							getPluginInstance().getServer()
									.dispatchCommand(getPluginInstance().getServer().getConsoleSender(), value);
						break;
					case "dispatch-command-player":
						if (!value.equalsIgnoreCase(""))
							getPluginInstance().getServer().dispatchCommand(player, value);
						break;
					case "open-custom-menu":
						if (!value.equalsIgnoreCase("")) {
							player.closeInventory();
							Inventory inventory = getPluginInstance().getManager().buildCustomMenu(player, value);

							MenuOpenEvent menuOpenEvent = new MenuOpenEvent(getPluginInstance(),
									EnumContainer.MenuType.CUSTOM, inventory, player.getPlayer());
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

	// getters & setters
	private HyperDrive getPluginInstance() {
		return pluginInstance;
	}

	private void setPluginInstance(HyperDrive pluginInstance) {
		this.pluginInstance = pluginInstance;
	}
}
