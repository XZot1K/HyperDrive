package xzot1k.plugins.hd.api;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import xzot1k.plugins.hd.HyperDrive;
import xzot1k.plugins.hd.api.events.HookCheckEvent;
import xzot1k.plugins.hd.api.events.RandomTeleportEvent;
import xzot1k.plugins.hd.api.events.WarpEvent;
import xzot1k.plugins.hd.api.objects.Warp;
import xzot1k.plugins.hd.core.internals.Animation;
import xzot1k.plugins.hd.core.internals.hooks.worldguard.WG_6;
import xzot1k.plugins.hd.core.internals.hooks.worldguard.WG_7;
import xzot1k.plugins.hd.core.objects.Destination;
import xzot1k.plugins.hd.core.objects.GroupTemp;
import xzot1k.plugins.hd.core.objects.TeleportTemp;

import java.util.*;

public class TeleportationHandler implements Runnable {
	private HyperDrive pluginInstance;
	private Animation animation;
	private HashMap<UUID, GroupTemp> groupTempMap;
	private HashMap<UUID, TeleportTemp> teleportTempMap;
	private HashMap<UUID, Destination> destinationMap;
	private List<UUID> randomTeleportingPlayers;

	public TeleportationHandler(HyperDrive pluginInstance) {
		setPluginInstance(pluginInstance);
		setAnimation(new Animation(pluginInstance));
		setGroupTempMap(new HashMap<>());
		setTeleportTempMap(new HashMap<>());
		setDestinationMap(new HashMap<>());
		setRandomTeleportingPlayers(new ArrayList<>());
	}

	@Override
	public void run() {
		String teleportSound = Objects
				.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
				.toUpperCase().replace(" ", "_").replace("-", "_"),
				teleportTitle = getPluginInstance().getConfig().getString("teleportation-section.teleport-title"),
				teleportSubTitle = getPluginInstance().getConfig()
						.getString("teleportation-section.teleport-sub-title"),
				delayTitle = getPluginInstance().getConfig().getString("teleportation-section.delay-title"),
				delaySubTitle = getPluginInstance().getConfig().getString("teleportation-section.delay-sub-title");
		List<UUID> uuidList = new ArrayList<>(getTeleportTempMap().keySet());
		for (int i = -1; ++i < uuidList.size();) {
			UUID uuid = uuidList.get(i);
			Player player = getPluginInstance().getServer().getPlayer(uuid);
			if (player != null && player.isOnline()) {
				TeleportTemp teleportTemp = getTeleportTempMap().get(uuid);
				if (teleportTemp != null) {
					Warp warp = null;
					if (teleportTemp.getTeleportValue() != null)
						warp = getPluginInstance().getManager().getWarp(teleportTemp.getTeleportValue());

					if (teleportTemp.getSeconds() > 0) {
						teleportTemp.setSeconds(teleportTemp.getSeconds() - 1);
						if (warp != null && teleportTemp.getSeconds() <= 5 && teleportTemp.getSeconds() > 0) {
							if (teleportTemp.getTeleportTypeId().equalsIgnoreCase("warp")) {
								if (delayTitle != null && delaySubTitle != null)
									getPluginInstance().getManager().sendTitle(player,
											delayTitle.replace("{warp}", warp.getWarpName()).replace("{duration}",
													String.valueOf(teleportTemp.getSeconds())),
											delaySubTitle.replace("{warp}", warp.getWarpName()).replace("{duration}",
													String.valueOf(teleportTemp.getSeconds())),
											0, 5, 0);

								int delayDuration = getPluginInstance().getConfig()
										.getInt("teleportation-section.warp-delay-duration");
								getPluginInstance().getManager().sendActionBar(player,
										Objects.requireNonNull(getPluginInstance().getConfig()
												.getString("teleportation-section.delay-bar-message"))
												.replace("{progress}",
														getPluginInstance().getManager().getProgressionBar(
																teleportTemp.getSeconds(), delayDuration, 10))
												.replace("{duration}", String.valueOf(delayDuration))
												.replace("{duration-left}", String.valueOf(teleportTemp.getSeconds()))
												.replace("{warp}", warp.getWarpName()));

								getPluginInstance().getManager()
										.sendCustomMessage(Objects
												.requireNonNull(getPluginInstance().getConfig()
														.getString("language-section.teleportation-delay"))
												.replace("{warp}", warp.getWarpName()).replace("{duration}",
														String.valueOf(teleportTemp.getSeconds())),
												player);
							}
						}
					} else {
						if (teleportTemp.getTeleportTypeId().equalsIgnoreCase("warp")) {
							if (teleportTemp.getTeleportValue() != null) {
								if (warp != null) {
									Location warpLocation = warp.getWarpLocation().asBukkitLocation();

									if (warpLocation == null || warpLocation.getWorld() == null) {
										getPluginInstance().getManager().sendActionBar(player,
												Objects.requireNonNull(getPluginInstance().getConfig()
														.getString("language-section.teleport-fail-message"))
														.replace("{warp}", warp.getWarpName()));
										getTeleportTempMap().remove(uuid);
										return;
									}

									WarpEvent warpEvent = new WarpEvent(warpLocation, player);
									getPluginInstance().getServer().getPluginManager().callEvent(warpEvent);
									if (warpEvent.isCancelled()) {
										getAnimation().stopActiveAnimation(player);
										getTeleportTempMap().remove(uuid);
										return;
									}

									if (!warp.getOwner().toString().equalsIgnoreCase(player.getUniqueId().toString())
											&& !warp.getAssistants().contains(player.getUniqueId()))
										warp.setTraffic(warp.getTraffic() + 1);

									boolean useMySQL = getPluginInstance().getConfig()
											.getBoolean("mysql-connection.use-mysql");
									if (useMySQL && getPluginInstance().getConnection() != null) {
										String warpIP = warp.getServerIPAddress().replace("localhost", "127.0.0.1"),
												serverIP = (getPluginInstance().getServer().getIp().equalsIgnoreCase("")
														|| getPluginInstance().getServer().getIp().equalsIgnoreCase(
																"0.0.0.0")) ? getPluginInstance().getConfig()
																		.getString("mysql-connection.default-ip") + ":"
																		+ getPluginInstance().getServer().getPort()
																		: (getPluginInstance().getServer().getIp()
																				.replace("localhost", "127.0.0.1") + ":"
																				+ getPluginInstance().getServer()
																						.getPort());

										if (!warpIP.equalsIgnoreCase(serverIP)) {
											String server = getPluginInstance().getBungeeListener()
													.getServerName(warp.getServerIPAddress());
											if (server != null) {
												getPluginInstance().getManager().teleportCrossServer(player,
														warp.getServerIPAddress(), server,
														warp.getWarpLocation().asBukkitLocation());
												getPluginInstance().getManager().updateCooldown(player, "warp");
												return;
											}
										}

										teleportPlayer(player, warpLocation);
										getPluginInstance().getManager().updateCooldown(player, "warp");
									} else {
										teleportPlayer(player, warpLocation);
										getPluginInstance().getManager().updateCooldown(player, "warp");
									}

									// Warp teleport animation
									if (warp.getAnimationSet().contains(":")) {
										String[] themeArgs = warp.getAnimationSet().split(":");
										String teleportTheme = themeArgs[2];
										if (teleportTheme.contains("/")) {
											String[] teleportThemeArgs = teleportTheme.split("/");
											getAnimation().stopActiveAnimation(player);
											getPluginInstance().getTeleportationHandler().getAnimation()
													.playAnimation(player, teleportThemeArgs[1],
															EnumContainer.Animation.valueOf(teleportThemeArgs[0]
																	.toUpperCase().replace(" ", "_").replace("-", "_")),
															1);
										}
									}

									if (!teleportSound.equalsIgnoreCase(""))
										player.getWorld().playSound(player.getLocation(), Sound.valueOf(teleportSound),
												1, 1);

									if (teleportTitle != null && teleportSubTitle != null)
										getPluginInstance().getManager().sendTitle(player,
												teleportTitle.replace("{warp}", warp.getWarpName()),
												teleportSubTitle.replace("{warp}", warp.getWarpName()), 0, 5, 0);

									getPluginInstance().getManager().sendActionBar(player,
											Objects.requireNonNull(getPluginInstance().getConfig()
													.getString("teleportation-section.teleport-bar-message"))
													.replace("{warp}", warp.getWarpName()));

									getPluginInstance().getManager()
											.sendCustomMessage(Objects
													.requireNonNull(getPluginInstance().getConfig()
															.getString("language-section.teleportation-engaged"))
													.replace("{warp}", warp.getWarpName()).replace("{duration}",
															String.valueOf(teleportTemp.getSeconds())),
													player);
								}
							}

							getTeleportTempMap().remove(uuid);
						}
					}

					continue;
				}
			}

			getTeleportTempMap().remove(uuid);
		}
	}

	// teleportation temp stuff
	public void updateTeleportTemp(Player player, String teleportTypeId, String teleportValue, int seconds) {
		getTeleportTempMap().put(player.getUniqueId(),
				new TeleportTemp(getPluginInstance(), teleportTypeId, teleportValue, seconds));
	}

	public boolean isTeleporting(Player player) {
		return (!getTeleportTempMap().isEmpty() && getTeleportTempMap().containsKey(player.getUniqueId()))
				|| getRandomTeleportingPlayers().contains(player.getUniqueId());
	}

	public int getRemainingTime(Player player) {
		if (!getTeleportTempMap().isEmpty() && getTeleportTempMap().containsKey(player.getUniqueId())) {
			TeleportTemp teleportTemp = getTeleportTempMap().get(player.getUniqueId());
			if (teleportTemp != null)
				return teleportTemp.getSeconds();
		}

		return 0;
	}

	public void removeTeleportTemp(Player player) {
		getTeleportTempMap().remove(player.getUniqueId());
	}

	/**
	 * Warps the player to the specified warp
	 *
	 * @param player    The player to warp
	 * @param warp      The warp to teleport the player to
	 * @param warpDelay The delay before teleportation takes place (Seconds)
	 */
	public void warpPlayer(Player player, Warp warp, int warpDelay) {
		updateTeleportTemp(player, "warp", warp.getWarpName(), warpDelay);
	}

	// teleport methods
	@SuppressWarnings("deprecation")
	public void teleportPlayer(Player player, Location location) {
		boolean teleportVehicle = getPluginInstance().getConfig().getBoolean("teleportation-section.teleport-vehicles");
		if (player.getVehicle() != null && teleportVehicle) {
			Entity entity = player.getVehicle();
			if (getPluginInstance().getServerVersion().startsWith("v1_11")
					|| getPluginInstance().getServerVersion().startsWith("v1_12")
					|| getPluginInstance().getServerVersion().startsWith("v1_13")
					|| getPluginInstance().getServerVersion().startsWith("v1_14"))
				entity.removePassenger(player);
			else
				entity.setPassenger(null);
			if (entity.getPassengers().contains(player))
				entity.eject();

			player.teleport(location);
			new BukkitRunnable() {
				@Override
				public void run() {
					entity.teleport(player.getLocation());
					entity.addPassenger(player);
				}
			}.runTaskLater(getPluginInstance(), 1);
		} else
			player.teleport(location);
	}

	public void randomlyTeleportPlayer(Player player, World world) {
		if (!player.hasPermission("hyperdrive.rtpbypass")) {
			long cooldownDurationLeft = getPluginInstance().getManager().getCooldownDuration(player, "rtp",
					getPluginInstance().getConfig().getInt("random-teleport-section.cooldown"));
			if (cooldownDurationLeft > 0) {
				getPluginInstance().getManager().sendCustomMessage(Objects
						.requireNonNull(
								getPluginInstance().getConfig().getString("language-section.random-teleport-cooldown"))
						.replace("{duration}", String.valueOf(cooldownDurationLeft)), player);
				return;
			}
		}

		getPluginInstance().getManager().sendCustomMessage(
				getPluginInstance().getConfig().getString("language-section.random-teleport-start"), player);

		getRandomTeleportingPlayers().add(player.getUniqueId());
		Location basedLocation = world != null ? world.getSpawnLocation() : player.getLocation();

		List<String> forcedLocationList = getPluginInstance().getConfig()
				.getStringList("random-teleport-section.forced-location-list");
		for (int i = -1; ++i < forcedLocationList.size();) {
			String line = forcedLocationList.get(i);
			if (line.contains(":")) {
				String[] lineArgs = line.split(":");
				if ((lineArgs.length >= 2 && lineArgs[1].contains(","))) {
					String[] coordinateArgs = lineArgs[1].split(",");
					if (coordinateArgs.length >= 3) {
						if (getPluginInstance().getManager().isNumeric(coordinateArgs[0])
								&& getPluginInstance().getManager().isNumeric(coordinateArgs[1])
								&& getPluginInstance().getManager().isNumeric(coordinateArgs[2])) {
							if (basedLocation.getWorld() != null
									&& basedLocation.getWorld().getName().equalsIgnoreCase(lineArgs[0])) {
								basedLocation = new Location(getPluginInstance().getServer().getWorld(lineArgs[0]),
										Double.parseDouble(coordinateArgs[0]), Double.parseDouble(coordinateArgs[1]),
										Double.parseDouble(coordinateArgs[2]), player.getLocation().getYaw(),
										player.getLocation().getPitch());
								break;
							}
						}
					}
				}
			}
		}

		Location finalBasedLocation = basedLocation;
		new BukkitRunnable() {
			Random random = new Random();
			List<String> forbiddenMaterialList = getPluginInstance().getConfig()
					.getStringList("random-teleport-section.forbidden-materials");
			double boundsRadius = getPluginInstance().getConfig().getDouble("random-teleport-section.bounds-radius");
			int tries = 0, maxTries = getPluginInstance().getConfig().getInt("random-teleport-section.max-tries"), x, z,
					smartLimit = 0;
			boolean foundSafeLocation = false,
					canLoadChunks = getPluginInstance().getConfig()
							.getBoolean("random-teleport-section.can-load-chunks"),
					canGenerateChunks = getPluginInstance().getConfig()
							.getBoolean("random-teleport-section.can-generate-chunks");
			String teleportSound = Objects
					.requireNonNull(getPluginInstance().getConfig().getString("general-section.global-sounds.teleport"))
					.toUpperCase().replace(" ", "_").replace("-", "_");

			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				if (!getRandomTeleportingPlayers().contains(player.getUniqueId())) {
					cancel();
					return;
				}

				if (maxTries > -1 ? (!foundSafeLocation && tries <= maxTries) : !foundSafeLocation) {
					tries += 1;
					int smartBounds = (int) (boundsRadius - smartLimit),
							xAddition = random.nextInt(((smartBounds - (-smartBounds)) + 1)) + (-smartBounds),
							zAddition = random.nextInt(((smartBounds - (-smartBounds)) + 1)) + (-smartBounds);

					x = (int) (finalBasedLocation.getX() + xAddition);
					z = (int) (finalBasedLocation.getZ() + zAddition);

					if (finalBasedLocation.getWorld() == null)
						return;

					if (smartLimit < boundsRadius)
						smartLimit += (boundsRadius * 0.005);

					if (getPluginInstance().getServerVersion().startsWith("v1_13")
							|| getPluginInstance().getServerVersion().startsWith("v1_14")) {
						if (!canLoadChunks && !finalBasedLocation.getWorld().isChunkLoaded(x >> 4, z >> 4))
							return;
					} else {
						if (!canLoadChunks
								&& !finalBasedLocation.getWorld().isChunkLoaded(finalBasedLocation.getChunk()))
							return;
					}

					if (getPluginInstance().getServerVersion().startsWith("v1_13")
							|| getPluginInstance().getServerVersion().startsWith("v1_14"))
						if (!canGenerateChunks && !finalBasedLocation.getWorld().isChunkGenerated(x >> 4, z >> 4))
							return;

					int safeY = getSafeY(finalBasedLocation.getWorld(), x, z);
					if (!(safeY > 0 && safeY <= finalBasedLocation.getWorld().getMaxHeight()))
						return;

					Block foundBlock = Objects.requireNonNull(finalBasedLocation.getWorld()).getBlockAt(x, safeY, z);

					if (!isLocationHookSafe(player, foundBlock.getLocation()))
						return;

					boolean isForbidden = false;
					for (int i = -1; ++i < forbiddenMaterialList.size();) {
						String materialLine = forbiddenMaterialList.get(i);
						if (materialLine != null && materialLine.contains(":")) {
							String[] materialArgs = materialLine.split(":");
							Material material = Material
									.getMaterial(materialArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"));
							int durability = 0;

							if (getPluginInstance().getManager().isNumeric(materialArgs[1]))
								durability = Integer.parseInt(materialArgs[1]);

							if (material != null && foundBlock.getType() == material
									&& (durability == -1 || foundBlock.getData() == durability)) {
								isForbidden = true;
								break;
							}
						}
					}

					if (isForbidden && foundBlock.getType() == Material.AIR) {
						Block blockUnder = foundBlock.getRelative(BlockFace.DOWN);
						boolean underIsForbidden = false;
						for (int i = -1; ++i < forbiddenMaterialList.size();) {
							String materialLine = forbiddenMaterialList.get(i);
							if (materialLine != null && materialLine.contains(":")) {
								String[] materialArgs = materialLine.split(":");
								Material material = Material
										.getMaterial(materialArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"));
								int durability = 0;

								if (getPluginInstance().getManager().isNumeric(materialArgs[1]))
									durability = Integer.parseInt(materialArgs[1]);

								if (material != null && blockUnder.getType() == material
										&& (durability == -1 || blockUnder.getData() == durability)) {
									underIsForbidden = true;
									break;
								}
							}
						}

						if (!underIsForbidden)
							isForbidden = false;
					}

					if (!isForbidden) {
						Location newLocation = new Location(finalBasedLocation.getWorld(), x, safeY, z,
								finalBasedLocation.getYaw(), finalBasedLocation.getPitch());

						RandomTeleportEvent randomTeleportEvent = new RandomTeleportEvent(newLocation, player);
						getPluginInstance().getServer().getPluginManager().callEvent(randomTeleportEvent);
						if (randomTeleportEvent.isCancelled()) {
							getAnimation().stopActiveAnimation(player);
							getRandomTeleportingPlayers().remove(player.getUniqueId());
							cancel();
							return;
						}

						teleportPlayer(player, newLocation.add(0.5, 0, 0.5));

						String animationLine = getPluginInstance().getConfig()
								.getString("special-effects-section.random-teleport-animation");
						if (animationLine != null && animationLine.contains(":")) {
							String[] animationArgs = animationLine.split(":");
							if (animationArgs.length >= 2) {
								EnumContainer.Animation animation = EnumContainer.Animation
										.valueOf(animationArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"));
								getAnimation().playAnimation(player,
										animationArgs[1].toUpperCase().replace(" ", "_").replace("-", "_"), animation,
										1);
							}
						}

						if (!teleportSound.equalsIgnoreCase(""))
							Objects.requireNonNull(newLocation.getWorld()).playSound(newLocation,
									Sound.valueOf(teleportSound), 1, 1);

						getPluginInstance().getManager().sendCustomMessage(Objects
								.requireNonNull(
										getPluginInstance().getConfig().getString("language-section.random-teleported"))
								.replace("{tries}", String.valueOf(tries - 1))
								.replace("{x}", String.valueOf(newLocation.getBlockX()))
								.replace("{y}", String.valueOf(newLocation.getBlockY()))
								.replace("{z}", String.valueOf(newLocation.getBlockZ()))
								.replace("{world}", Objects.requireNonNull(newLocation.getWorld()).getName()), player);

						getRandomTeleportingPlayers().remove(player.getUniqueId());
						getPluginInstance().getManager().updateCooldown(player, "rtp");
						foundSafeLocation = true;
						cancel();
					}

				} else {
					if (!foundSafeLocation) {
						getPluginInstance().getManager()
								.sendCustomMessage(Objects
										.requireNonNull(getPluginInstance().getConfig()
												.getString("language-section.random-teleport-fail"))
										.replace("{tries}", String.valueOf(tries - 1)), player);
					}

					getRandomTeleportingPlayers().remove(player.getUniqueId());
					cancel();
				}
			}
		}.runTaskTimer(getPluginInstance(), 0, 0);
	}

	public void updateDestinationWithRandomLocation(Player player, Location baseLocation, World world) {
		Location basedLocation = world != null ? world.getSpawnLocation() : baseLocation;

		List<String> forcedLocationList = getPluginInstance().getConfig()
				.getStringList("random-teleport-section.forced-location-list");
		for (int i = -1; ++i < forcedLocationList.size();) {
			String line = forcedLocationList.get(i);
			if (line.contains(":")) {
				String[] lineArgs = line.split(":");
				if ((lineArgs.length >= 2 && lineArgs[1].contains(","))) {
					String[] coordinateArgs = lineArgs[1].split(",");
					if (coordinateArgs.length >= 3) {
						if (getPluginInstance().getManager().isNumeric(coordinateArgs[0])
								&& getPluginInstance().getManager().isNumeric(coordinateArgs[1])
								&& getPluginInstance().getManager().isNumeric(coordinateArgs[2])) {
							if (basedLocation.getWorld() != null
									&& basedLocation.getWorld().getName().equalsIgnoreCase(lineArgs[0])) {
								basedLocation = new Location(getPluginInstance().getServer().getWorld(lineArgs[0]),
										Double.parseDouble(coordinateArgs[0]), Double.parseDouble(coordinateArgs[1]),
										Double.parseDouble(coordinateArgs[2]), player.getLocation().getYaw(),
										player.getLocation().getPitch());
								break;
							}
						}
					}
				}
			}
		}

		Location finalBasedLocation = basedLocation.clone();
		new BukkitRunnable() {
			Random random = new Random();
			List<String> forbiddenMaterialList = getPluginInstance().getConfig()
					.getStringList("random-teleport-section.forbidden-materials");
			double boundsRadius = getPluginInstance().getConfig().getDouble("random-teleport-section.bounds-radius");
			int tries = 0, maxTries = getPluginInstance().getConfig().getInt("random-teleport-section.max-tries"), x, z,
					smartLimit = 0;
			boolean foundSafeLocation = false,
					canLoadChunks = getPluginInstance().getConfig()
							.getBoolean("random-teleport-section.can-load-chunks"),
					canGenerateChunks = getPluginInstance().getConfig()
							.getBoolean("random-teleport-section.can-generated-chunks");

			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				if (maxTries > -1 ? (!foundSafeLocation && tries <= maxTries) : !foundSafeLocation) {
					tries += 1;
					int smartBounds = (int) (boundsRadius - smartLimit),
							xAddition = random.nextInt(((smartBounds - (-smartBounds)) + 1)) + (-smartBounds),
							zAddition = random.nextInt(((smartBounds - (-smartBounds)) + 1)) + (-smartBounds);

					x = (int) (finalBasedLocation.getX() + xAddition);
					z = (int) (finalBasedLocation.getZ() + zAddition);

					if (finalBasedLocation.getWorld() == null)
						return;

					if (smartLimit < boundsRadius)
						smartLimit += (boundsRadius * 0.005);

					if (getPluginInstance().getServerVersion().startsWith("v1_13")
							|| getPluginInstance().getServerVersion().startsWith("v1_14")) {
						if (!canLoadChunks && !finalBasedLocation.getWorld().isChunkLoaded(x >> 4, z >> 4))
							return;
					} else {
						if (!canLoadChunks
								&& !finalBasedLocation.getWorld().isChunkLoaded(finalBasedLocation.getChunk()))
							return;
					}

					if (getPluginInstance().getServerVersion().startsWith("v1_13")
							|| getPluginInstance().getServerVersion().startsWith("v1_14"))
						if (!canGenerateChunks && !finalBasedLocation.getWorld().isChunkGenerated(x >> 4, z >> 4))
							return;

					int safeY = getSafeY(finalBasedLocation.getWorld(), x, z);
					if (!(safeY > 0 && safeY <= finalBasedLocation.getWorld().getMaxHeight()))
						return;

					Block foundBlock = Objects.requireNonNull(finalBasedLocation.getWorld()).getBlockAt(x, safeY, z);

					if (!isLocationHookSafe(player, foundBlock.getLocation()))
						return;

					boolean isForbidden = false;
					for (int i = -1; ++i < forbiddenMaterialList.size();) {
						String materialLine = forbiddenMaterialList.get(i);
						if (materialLine != null && materialLine.contains(":")) {
							String[] materialArgs = materialLine.split(":");
							Material material = Material
									.getMaterial(materialArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"));
							int durability = 0;

							if (getPluginInstance().getManager().isNumeric(materialArgs[1]))
								durability = Integer.parseInt(materialArgs[1]);

							if (material != null && foundBlock.getType() == material
									&& (durability == -1 || foundBlock.getData() == durability)) {
								isForbidden = true;
								break;
							}
						}
					}

					if (isForbidden && foundBlock.getType() == Material.AIR) {
						Block blockUnder = foundBlock.getRelative(BlockFace.DOWN);
						boolean underIsForbidden = false;
						for (int i = -1; ++i < forbiddenMaterialList.size();) {
							String materialLine = forbiddenMaterialList.get(i);
							if (materialLine != null && materialLine.contains(":")) {
								String[] materialArgs = materialLine.split(":");
								Material material = Material
										.getMaterial(materialArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"));
								int durability = 0;

								if (getPluginInstance().getManager().isNumeric(materialArgs[1]))
									durability = Integer.parseInt(materialArgs[1]);

								if (material != null && blockUnder.getType() == material
										&& (durability == -1 || blockUnder.getData() == durability)) {
									underIsForbidden = true;
									break;
								}
							}
						}

						if (!underIsForbidden)
							isForbidden = false;
					}

					if (!isForbidden) {
						Location newLocation = new Location(finalBasedLocation.getWorld(), x, safeY, z,
								finalBasedLocation.getYaw(), finalBasedLocation.getPitch());
						updateDestination(player, new Destination(getPluginInstance(), newLocation.add(0.5, 0, 0.5)));
						foundSafeLocation = true;
						cancel();
					}

				} else {
					if (!foundSafeLocation) {
						getPluginInstance().getManager()
								.sendCustomMessage(Objects
										.requireNonNull(getPluginInstance().getConfig()
												.getString("language-section.random-teleport-fail"))
										.replace("{tries}", String.valueOf(tries - 1)), player);
					}

					cancel();
				}
			}
		}.runTaskTimer(getPluginInstance(), 0, 0);

	}

	private int getSafeY(World world, double x, double z) {
		if (world.getEnvironment() == World.Environment.NORMAL)
			return world.getHighestBlockYAt((int) x, (int) z);

		for (int i = 1; ++i < (world.getMaxHeight() / 2);) {
			Block block = world.getBlockAt((int) x, i, (int) z);
			if (block.getType() != Material.AIR && block.getType() != Material.BEDROCK
					&& block.getRelative(BlockFace.UP).getType() == Material.AIR
					&& block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType() == Material.AIR)
				return i + 1;
		}

		return -1;
	}

	public boolean isLocationHookSafe(Player player, Location location) {
		if (player.hasPermission("hyperdrive.admin.bypass"))
			return true;
		boolean isSafeLocation = true;

		Plugin worldGuardPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("WorldGuard");
		if (worldGuardPlugin != null && getPluginInstance().getConfig().getBoolean("hooks-section.use-worldguard")) {
			if (worldGuardPlugin.getDescription().getVersion().toLowerCase().startsWith("6")) {
				if (!WG_6.passedWorldGuardHook(location))
					isSafeLocation = false;
			} else if (worldGuardPlugin.getDescription().getVersion().toLowerCase().startsWith("7")) {
				if (!WG_7.passedWorldGuardHook(location))
					isSafeLocation = false;
			}
		}

		Plugin factionsPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("Factions");
		if (factionsPlugin != null && getPluginInstance().getConfig().getBoolean("hooks-section.use-factions")) {
			String[] authorsList = { "Cayorion", "Madus", "Ulumulu1510", "MarkehMe", "Brettflan" };

			if (factionsPlugin.getDescription().getAuthors().toString().equals(Arrays.toString(authorsList))) {
				com.massivecraft.factions.entity.Faction factionAtLocation = BoardColl.get()
						.getFactionAt(PS.valueOf(location));
				MPlayer mPlayer = MPlayer.get(player);
				if (!factionAtLocation.getId().equalsIgnoreCase(FactionColl.get().getNone().getId())
						&& !factionAtLocation.getId().equalsIgnoreCase(mPlayer.getFaction().getId()))
					isSafeLocation = false;
			} else {
				FLocation fLocation = new FLocation(location);
				com.massivecraft.factions.Faction factionAtLocation = Board.getInstance().getFactionAt(fLocation);
				FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
				if (!factionAtLocation.isWilderness() && !fPlayer.getFaction().getComparisonTag()
						.equalsIgnoreCase(factionAtLocation.getComparisonTag()))
					isSafeLocation = false;
			}
		}

		Plugin askyblockPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("ASkyBlock");
		if (askyblockPlugin != null && getPluginInstance().getConfig().getBoolean("hooks-section.use-askyblock")) {
			Island island = ASkyBlockAPI.getInstance().getIslandAt(location);
			if (island != null && !island.getOwner().toString().equals(player.getUniqueId().toString())
					&& !island.getMembers().contains(player.getUniqueId()))
				isSafeLocation = false;
		}

		Plugin griefPreventionPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("GriefPrevention");
		if (griefPreventionPlugin != null
				&& getPluginInstance().getConfig().getBoolean("hooks-section.use-grief-prevention")) {
			Claim claimAtLocation = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
			if (claimAtLocation != null)
				isSafeLocation = false;
		}

		Plugin townyPlugin = getPluginInstance().getServer().getPluginManager().getPlugin("Towny");
		if (townyPlugin != null && getPluginInstance().getConfig().getBoolean("hooks-section.use-towny")) {
			try {
				Town town = WorldCoord.parseWorldCoord(location).getTownBlock().getTown();
				if (town != null)
					isSafeLocation = false;
			} catch (Exception ignored) {
			}
		}

		Plugin residence = getPluginInstance().getServer().getPluginManager().getPlugin("Residence");
		if (residence != null && getPluginInstance().getConfig().getBoolean("hooks-section.use-residence")) {
			ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(location);
			if (res != null)
				isSafeLocation = false;
		}

		HookCheckEvent hookCheckEvent = new HookCheckEvent(location, player, isSafeLocation);
		getPluginInstance().getServer().getPluginManager().callEvent(hookCheckEvent);
		isSafeLocation = hookCheckEvent.isSafeLocation();

		return isSafeLocation;
	}

	// group stuff

	/**
	 * Returns the GroupTemp the passed player id has accepted.
	 *
	 * @param playerUniqueId The passed player id.
	 * @return The found GroupTemp the player accepted.
	 */
	public GroupTemp getAcceptedGroupTemp(UUID playerUniqueId) {
		List<UUID> groupLeaderList = new ArrayList<>(getGroupTempMap().keySet());
		for (int i = -1; ++i < groupLeaderList.size();) {
			UUID groupLeaderId = groupLeaderList.get(i);
			if (groupLeaderId != null) {
				GroupTemp groupTemp = getGroupTempMap().get(groupLeaderId);
				if (groupTemp.getAcceptedPlayers().contains(playerUniqueId))
					return groupTemp;
			}
		}

		return null;
	}

	/**
	 * Returns the leader of the group the player accepted.
	 *
	 * @param playerUniqueId The passed player id.
	 * @return The id of the group leader.
	 */
	public UUID getGroupLeader(UUID playerUniqueId) {
		List<UUID> groupLeaderList = new ArrayList<>(getGroupTempMap().keySet());
		for (int i = -1; ++i < groupLeaderList.size();) {
			UUID groupLeaderId = groupLeaderList.get(i);
			if (groupLeaderId != null) {
				GroupTemp groupTemp = getGroupTempMap().get(groupLeaderId);
				if (groupTemp.getAcceptedPlayers().contains(playerUniqueId))
					return groupLeaderId;
			}
		}

		return null;
	}

	/**
	 * Returns the GroupTemp of the passed group leader id.
	 *
	 * @param playerUniqueId The passed group leader id.
	 * @return The found GroupTemp the group leader.
	 */
	public GroupTemp getGroupTemp(UUID playerUniqueId) {
		if (!getGroupTempMap().isEmpty() && getGroupTempMap().containsKey(playerUniqueId))
			return getGroupTempMap().get(playerUniqueId);
		return null;
	}

	public void clearGroupTemp(OfflinePlayer player) {
		getGroupTempMap().remove(player.getUniqueId());
	}

	public List<UUID> getGroupRequests(Player player) {
		List<UUID> uuidList = new ArrayList<>(), groupTempList = new ArrayList<>(getGroupTempMap().keySet());
		for (int i = -1; ++i < groupTempList.size();) {
			UUID requesterId = groupTempList.get(i);
			if (requesterId == null)
				continue;

			GroupTemp groupTemp = getGroupTemp(requesterId);
			if (groupTemp == null)
				continue;

			if (groupTemp.getSelectedPlayers().contains(player.getUniqueId()))
				uuidList.add(requesterId);
		}

		return uuidList;
	}

	public void createGroupTemp(Player player, Destination destination) {
		GroupTemp groupTemp = new GroupTemp(getPluginInstance(), player, destination);
		getGroupTempMap().put(player.getUniqueId(), groupTemp);
	}

	public Destination getDestination(Player player) {
		if (!getDestinationMap().isEmpty() && getDestinationMap().containsKey(player.getUniqueId()))
			return getDestinationMap().get(player.getUniqueId());
		return null;
	}

	public void updateDestination(Player player, Destination destination) {
		getDestinationMap().put(player.getUniqueId(), destination);
	}

	// getters and setters
	public HyperDrive getPluginInstance() {
		return pluginInstance;
	}

	private void setPluginInstance(HyperDrive pluginInstance) {
		this.pluginInstance = pluginInstance;
	}

	private HashMap<UUID, TeleportTemp> getTeleportTempMap() {
		return teleportTempMap;
	}

	private void setTeleportTempMap(HashMap<UUID, TeleportTemp> teleportTempMap) {
		this.teleportTempMap = teleportTempMap;
	}

	public Animation getAnimation() {
		return animation;
	}

	private void setAnimation(Animation animation) {
		this.animation = animation;
	}

	public List<UUID> getRandomTeleportingPlayers() {
		return randomTeleportingPlayers;
	}

	private void setRandomTeleportingPlayers(List<UUID> randomTeleportingPlayers) {
		this.randomTeleportingPlayers = randomTeleportingPlayers;
	}

	private HashMap<UUID, GroupTemp> getGroupTempMap() {
		return groupTempMap;
	}

	private void setGroupTempMap(HashMap<UUID, GroupTemp> groupTempMap) {
		this.groupTempMap = groupTempMap;
	}

	public HashMap<UUID, Destination> getDestinationMap() {
		return destinationMap;
	}

	private void setDestinationMap(HashMap<UUID, Destination> destinationMap) {
		this.destinationMap = destinationMap;
	}
}
