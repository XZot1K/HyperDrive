package xzot1k.plugins.hd;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import xzot1k.plugins.hd.api.EnumContainer;
import xzot1k.plugins.hd.api.Manager;
import xzot1k.plugins.hd.api.TeleportationHandler;
import xzot1k.plugins.hd.api.objects.SerializableLocation;
import xzot1k.plugins.hd.api.objects.Warp;
import xzot1k.plugins.hd.core.internals.BungeeListener;
import xzot1k.plugins.hd.core.internals.Listeners;
import xzot1k.plugins.hd.core.internals.Metrics;
import xzot1k.plugins.hd.core.internals.cmds.MainCommands;
import xzot1k.plugins.hd.core.internals.cmds.TeleportationCommands;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class HyperDrive extends JavaPlugin {
	private static HyperDrive pluginInstance;
	private TeleportationHandler teleportationHandler;
	private MainCommands mainCommands;
	private TeleportationCommands teleportationCommands;
	private Manager manager;
	private BungeeListener bungeeListener;

	private Economy vaultEconomy;
	private Connection connection;
	private List<String> databaseWarps;

	private String serverVersion;
	private int teleportationHandlerTaskId, autoSaveTaskId, crossServerTaskId;

	@Override
	public void onEnable() {
		long startTime = System.currentTimeMillis();
		setPluginInstance(this);
		setServerVersion(
				getPluginInstance().getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3]);
		saveDefaultVersionConfig();
		updateConfig();

		if (getConfig().getBoolean("general-section.use-vault") && !setupVaultEconomy()) {
			log(Level.INFO, "The plugin was disabled due to Vault or an economy plugin for it being invalid. "
					+ "(Took " + (System.currentTimeMillis() - startTime) + "ms)");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		File warpsFile = new File(getDataFolder(), "/warps.yml"),
				backupFile = new File(getDataFolder(), "/warps-backup.yml");
		if (warpsFile.exists()) {
			try {
				copy(warpsFile, backupFile);
			} catch (IOException e) {
				e.printStackTrace();
				log(Level.WARNING, "Unable to backup the warp file as it may not exist. (Took "
						+ (System.currentTimeMillis() - startTime) + "ms)");
			}
		}

		setDatabaseWarps(new ArrayList<>());
		if (getConfig().getBoolean("mysql-connection.use-mysql")) {
			long databaseStartTime = System.currentTimeMillis();
			attemptMySQLConnection();
			if (getConnection() != null)
				log(Level.INFO, "The MySQL database connection was formed. (Took "
						+ (System.currentTimeMillis() - databaseStartTime) + "ms)");
			else {
				log(Level.WARNING, "The MySQL database connection failed. (Took "
						+ (System.currentTimeMillis() - databaseStartTime) + "ms)");
				getPluginInstance().getServer().getPluginManager().disablePlugin(this);
				return;
			}

			if (getConfig().getBoolean("mysql-connection.run-flat-file-converter"))
				runFlatFileConverter();
			else if (getConfig().getBoolean("mysql-connection.run-mysql-converter"))
				runMySQLConverter();
		}

		getServer().getMessenger().registerOutgoingPluginChannel(getPluginInstance(), "BungeeCord");
		setBungeeListener(new BungeeListener(getPluginInstance()));
		getServer().getMessenger().registerIncomingPluginChannel(getPluginInstance(), "BungeeCord",
				getBungeeListener());
		setManager(new Manager(this));

		setMainCommands(new MainCommands(this));
		String[] commandNames = { "warps", "hyperdrive" };
		for (String cmd : commandNames) {
			PluginCommand command = getCommand(cmd);
			if (command != null)
				command.setExecutor(getMainCommands());
		}

		setTeleportationCommands(new TeleportationCommands(this));
		String[] teleportCommandNames = { "teleport", "teleporthere", "teleportoverride", "teleportoverridehere",
				"teleportposition", "teleportask", "teleportaccept", "teleportdeny", "teleporttoggle", "back",
				"teleportaskhere", "crossserver" };
		for (String cmd : teleportCommandNames) {
			PluginCommand command = getCommand(cmd);
			if (command != null)
				command.setExecutor(getTeleportationCommands());
		}

		getServer().getPluginManager().registerEvents(new Listeners(getPluginInstance()), getPluginInstance());
		loadWarps(getConnection() != null);
		startWarpConverter();

		startTasks();
		new Metrics(getPluginInstance());
		log(Level.INFO,
				"The plugin was enabled successfully. (Took " + (System.currentTimeMillis() - startTime) + "ms)");

		if (isOutdated())
			log(Level.INFO, "Your version of HyperDrive (" + getDescription().getVersion() + ") doesn't match the "
					+ "version found on the main page (" + getLatestVersion() + "). There may be a update for you!");
		else
			log(Level.INFO,
					"You current HyperDrive version, " + getDescription().getVersion() + ", seems to be up to date!");
	}

	@Override
	public void onDisable() {
		saveWarps(getConnection() != null);
		if (getConnection() != null)
			try {
				getConnection().close();
				log(Level.WARNING, "The MySQL connection has been completely closed.");
			} catch (SQLException e) {
				e.printStackTrace();
				log(Level.WARNING, "The MySQL connection was unable to be closed.");
			}
	}

	// update checker methods
	private boolean isOutdated() {
		try {
			HttpURLConnection c = (HttpURLConnection) new URL(
					"https://api.spigotmc.org/legacy/update.php?resource=17184").openConnection();
			c.setRequestMethod("GET");
			String oldVersion = getDescription().getVersion(),
					newVersion = new BufferedReader(new InputStreamReader(c.getInputStream())).readLine();
			if (!newVersion.equalsIgnoreCase(oldVersion))
				return true;
		} catch (Exception ignored) {
		}
		return false;
	}

	private String getLatestVersion() {
		try {
			HttpURLConnection c = (HttpURLConnection) new URL(
					"https://api.spigotmc.org/legacy/update.php?resource=17184").openConnection();
			c.setRequestMethod("GET");
			return new BufferedReader(new InputStreamReader(c.getInputStream())).readLine();
		} catch (Exception ex) {
			return getDescription().getVersion();
		}
	}

	// configuration methods
	private void saveDefaultVersionConfig() {
		if (new File(getDataFolder(), "config.yml").exists())
			return;

		if (getServerVersion().startsWith("v1_14")) {
			saveResource("config_(1.14).yml", false);
			File file = new File(getDataFolder(), "config_(1.14).yml");
			file.renameTo(new File(getDataFolder(), "config.yml"));
		} else if (getServerVersion().startsWith("v1_13")) {
			saveResource("config_(1.13).yml", false);
			File file = new File(getDataFolder(), "config_(1.13).yml");
			file.renameTo(new File(getDataFolder(), "config.yml"));
		} else if (getServerVersion().startsWith("v1_9") || getServerVersion().startsWith("v1_10")
				|| getServerVersion().startsWith("v1_11") || getServerVersion().startsWith("v1_12")) {
			saveResource("config_(1.9-1.12).yml", false);
			File file = new File(getDataFolder(), "config_(1.9-1.12).yml");
			file.renameTo(new File(getDataFolder(), "config.yml"));
		} else {
			saveResource("config_(1.8).yml", false);
			File file = new File(getDataFolder(), "config_(1.8).yml");
			file.renameTo(new File(getDataFolder(), "config.yml"));
		}

		log(Level.INFO, getServerVersion() + " has been detected. Configuration created!");
	}

	private void updateConfig() {
		int updateCount = 0;
		File latestConfigFile;

		if (getServerVersion().startsWith("v1_14")) {
			saveResource("config_(1.14).yml", true);
			latestConfigFile = new File(getDataFolder(), "config_(1.14).yml");
		} else if (getServerVersion().startsWith("v1_13")) {
			saveResource("config_(1.13).yml", true);
			latestConfigFile = new File(getDataFolder(), "config_(1.13).yml");
		} else if (getServerVersion().startsWith("v1_9") || getServerVersion().startsWith("v1_10")
				|| getServerVersion().startsWith("v1_11") || getServerVersion().startsWith("v1_12")) {
			saveResource("config_(1.9-1.12).yml", true);
			latestConfigFile = new File(getDataFolder(), "config_(1.9-1.12).yml");
		} else {
			saveResource("config_(1.8).yml", true);
			latestConfigFile = new File(getDataFolder(), "config_(1.8).yml");
		}

		FileConfiguration updatedYaml = YamlConfiguration.loadConfiguration(latestConfigFile);
		List<String> currentKeys = new ArrayList<>(
				Objects.requireNonNull(getConfig().getConfigurationSection("")).getKeys(true)),
				updatedKeys = new ArrayList<>(
						Objects.requireNonNull(updatedYaml.getConfigurationSection("")).getKeys(true));
		for (int i = -1; ++i < updatedKeys.size();) {
			String updatedKey = updatedKeys.get(i);
			if (!currentKeys.contains(updatedKey) && !updatedKey.contains(".items.")
					&& !updatedKey.contains("custom-menus-section.")) {
				getConfig().set(updatedKey, updatedYaml.get(updatedKey));
				updateCount += 1;
				log(Level.INFO, "Updated the '" + updatedKey + "' key within the configuration since it wasn't found.");
			}
		}

		for (int i = -1; ++i < currentKeys.size();) {
			String currentKey = currentKeys.get(i);
			if (!updatedKeys.contains(currentKey)) {
				getConfig().set(currentKey, null);
				updateCount += 1;
				log(Level.INFO, "Removed the '" + currentKey + "' key within the configuration since it was invalid.");
			}
		}

		if (updateCount > 0) {
			saveConfig();
			log(Level.INFO, "The configuration has been updated using the " + latestConfigFile.getName() + " file.");
			log(Level.WARNING,
					"Please go check out the configuration and customize these newly generated options to your liking. "
							+ "Messages and similar values may not appear the same as they did in the default configuration "
							+ "(P.S. Configuration comments have more than likely been removed to ensure proper syntax).");
		} else
			log(Level.INFO, "Everything inside the configuration seems to be up to date.");
		latestConfigFile.delete();
	}

	// core methods
	private void attemptMySQLConnection() {
		if (getConnection() == null)
			try {
				Class.forName("com.mysql.jdbc.Driver");
				setConnection(DriverManager.getConnection(
						"jdbc:mysql://" + getConfig().getString("mysql-connection.host") + ":"
								+ getConfig().getString("mysql-connection.port") + "/"
								+ getConfig().getString("mysql-connection.database-name"),
						getConfig().getString("mysql-connection.username"),
						getConfig().getString("mysql-connection.password")));

				Statement statement = getConnection().createStatement();
				statement.executeUpdate(
						"create table if not exists warps (name varchar(100),location varchar(255),status varchar(100),creation_date varchar(100),"
								+ "icon_theme varchar(100),animation_set varchar(100),description_color varchar(100),name_color varchar(100),description varchar(255),commands varchar(255),"
								+ "owner varchar(100),white_list varchar(255),assistants varchar(255), traffic int,usage_price double,enchanted_look int,server_ip varchar(255),primary key (name))");
				statement.executeUpdate(
						"create table if not exists transfer (player_uuid varchar(100),location varchar(255), server_ip varchar(255),primary key (player_uuid))");
				statement.executeUpdate("truncate transfer");
				statement.close();
			} catch (ClassNotFoundException | SQLException e) {
				e.printStackTrace();
				log(Level.WARNING, "There was an issue involving the MySQL connection.");
			}

	}

	private void runFlatFileConverter() {
		int convertedWarpCount = 0, failedToConvertWarps = 0;
		long startTime = System.currentTimeMillis();

		File file = new File(getDataFolder(), "/warps.yml");
		if (file.exists()) {
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
			List<String> configurationLines = new ArrayList<>(
					Objects.requireNonNull(yaml.getConfigurationSection("")).getKeys(false));
			for (int i = -1; ++i < configurationLines.size();) {
				try {
					String warpName = configurationLines.get(i);
					UUID uuid = UUID.fromString(Objects.requireNonNull(yaml.getString(warpName + ".owner")));

					SerializableLocation serializableLocation = new SerializableLocation(
							yaml.getString(warpName + ".location.world"), yaml.getDouble(warpName + ".location.x"),
							yaml.getDouble(warpName + ".location.y"), yaml.getDouble(warpName + ".location.z"),
							(float) yaml.getDouble(warpName + ".location.yaw"),
							(float) yaml.getDouble(warpName + ".location.pitch"));

					Warp warp = new Warp(warpName, getPluginInstance().getServer().getOfflinePlayer(uuid),
							serializableLocation);

					try {
						List<UUID> assistantList = new ArrayList<>(), whiteListPlayers = new ArrayList<>();
						List<String> assistants = yaml.getStringList(warpName + ".assistants"),
								whiteList = yaml.getStringList(warpName + ".whitelist");
						for (int j = -1; ++j < assistants.size();) {
							UUID uniqueId = UUID.fromString(assistants.get(j));
							assistantList.add(uniqueId);
						}

						for (int j = -1; ++j < whiteList.size();) {
							UUID uniqueId = UUID.fromString(whiteList.get(j));
							whiteListPlayers.add(uniqueId);
						}

						String statusString = yaml.getString(warpName + ".status");
						if (statusString == null)
							statusString = EnumContainer.Status.PUBLIC.name();

						EnumContainer.Status status = EnumContainer.Status
								.valueOf(statusString.toUpperCase().replace(" ", "_").replace("-", "_"));
						warp.setStatus(status);
						warp.setAssistants(assistantList);
						warp.setWhiteList(whiteListPlayers);
						warp.setCreationDate(yaml.getString(warpName + ".creation-date"));
						warp.setCommands(yaml.getStringList(warpName + ".commands"));
						warp.setAnimationSet(yaml.getString(warpName + ".animation-set"));

						warp.setIconTheme(yaml.getString(warpName + ".icon.theme"));
						warp.setDescriptionColor(
								ChatColor.valueOf(yaml.getString(warpName + ".icon.description-color")));
						warp.setDisplayNameColor(ChatColor.valueOf(yaml.getString(warpName + ".icon.name-color")));
						warp.setDescription(yaml.getStringList(warpName + ".icon.description"));
						warp.setIconEnchantedLook(yaml.getBoolean(warpName + ".icon.use-enchanted-look"));
						warp.setUsagePrice(yaml.getDouble(warpName + ".icon.prices.usage"));
						warp.setTraffic(yaml.getInt(warpName + ".traffic"));
						warp.setServerIPAddress(
								yaml.getString(warpName + ".server-ip").replace("localhost", "127.0.0.1"));
					} catch (Exception e) {
						e.printStackTrace();
						log(Level.INFO, "There was an issue loading the warp " + warp.getWarpName()
								+ "'s data aside it's location.");
					}

					saveWarp(warp, true);
					convertedWarpCount++;
				} catch (Exception ignored) {
					failedToConvertWarps++;
				}
			}
		}

		getConfig().set("mysql-connection.run-flat-file-converter", false);
		saveConfig();
		reloadConfig();
		log(Level.INFO,
				convertedWarpCount + " " + ((convertedWarpCount == 1) ? "warp was" : "warps were")
						+ " converted to MySQL and " + failedToConvertWarps + " "
						+ ((failedToConvertWarps == 1) ? "warp" : "warps") + " failed to convert to MySQL. (Took "
						+ (System.currentTimeMillis() - startTime) + "ms)");
	}

	private void runMySQLConverter() {
		int convertedWarpCount = 0, failedToConvert = 0;
		long startTime = System.currentTimeMillis();

		try {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("select * from warps");
			while (resultSet.next()) {
				String warpName = resultSet.getString(1),
						ipAddress = resultSet.getString(17).replace("localhost", "127.0.0.1"),
						locationString = resultSet.getString(2);
				if (locationString.contains(",")) {
					String[] locationStringArgs = locationString.split(",");

					SerializableLocation serializableLocation = new SerializableLocation(locationStringArgs[0],
							Double.parseDouble(locationStringArgs[1]), Double.parseDouble(locationStringArgs[2]),
							Double.parseDouble(locationStringArgs[3]), Float.parseFloat(locationStringArgs[4]),
							Float.parseFloat(locationStringArgs[5]));

					Warp warp = new Warp(warpName, serializableLocation);

					try {
						String statusString = resultSet.getString(3);
						if (statusString == null)
							statusString = EnumContainer.Status.PUBLIC.name();

						EnumContainer.Status status = EnumContainer.Status
								.valueOf(statusString.toUpperCase().replace(" ", "_").replace("-", "_"));
						warp.setStatus(status);
						warp.setCreationDate(resultSet.getString(4));
						warp.setIconTheme(resultSet.getString(5));
						warp.setAnimationSet(resultSet.getString(6));

						String descriptionColor = resultSet.getString(7);
						if (descriptionColor != null && !descriptionColor.equalsIgnoreCase(""))
							warp.setDescriptionColor(ChatColor
									.valueOf(descriptionColor.toUpperCase().replace(" ", "_").replace("-", "_")));
						String nameColor = resultSet.getString(8);
						if (nameColor != null && !nameColor.equalsIgnoreCase(""))
							warp.setDisplayNameColor(
									ChatColor.valueOf(nameColor.toUpperCase().replace(" ", "_").replace("-", "_")));

						String descriptionString = resultSet.getString(9);
						if (descriptionString.contains(",")) {
							List<String> description = new ArrayList<>();
							String[] descriptionStringArgs = descriptionString.split(",");
							for (int i = -1; ++i < descriptionStringArgs.length;)
								description.add(descriptionStringArgs[i]);
							warp.setDescription(description);
						}

						String commandsString = resultSet.getString(10);
						if (commandsString.contains(",")) {
							List<String> commands = new ArrayList<>();
							String[] commandsStringArgs = commandsString.split(",");
							for (int i = -1; ++i < commandsStringArgs.length;)
								commands.add(commandsStringArgs[i]);
							warp.setCommands(commands);
						}

						warp.setOwner(UUID.fromString(resultSet.getString(11)));

						String whitelistString = resultSet.getString(12);
						if (whitelistString.contains(",")) {
							List<UUID> whitelist = new ArrayList<>();
							String[] whitelistStringArgs = whitelistString.split(",");
							for (int i = -1; ++i < whitelistStringArgs.length;)
								whitelist.add(UUID.fromString(whitelistStringArgs[i]));
							warp.setWhiteList(whitelist);
						}

						String assistantsString = resultSet.getString(13);
						if (assistantsString.contains(",")) {
							List<UUID> assistants = new ArrayList<>();
							String[] assistantsStringArgs = assistantsString.split(",");
							for (int i = -1; ++i < assistantsStringArgs.length;)
								assistants.add(UUID.fromString(assistantsStringArgs[i]));
							warp.setAssistants(assistants);
						}

						warp.setTraffic(resultSet.getInt(14));
						warp.setUsagePrice(resultSet.getDouble(15));
						warp.setIconEnchantedLook(resultSet.getInt(16) >= 1);
						warp.setServerIPAddress(ipAddress);
					} catch (Exception e) {
						e.printStackTrace();
						log(Level.INFO, "There was an issue loading the warp " + warp.getWarpName()
								+ "'s data aside it's location.");
					}

					saveWarp(warp, false);
					convertedWarpCount++;
				}
			}

			resultSet.close();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
			failedToConvert++;
		}

		getConfig().set("mysql-connection.run-mysql-converter", false);
		saveConfig();
		reloadConfig();
		log(Level.INFO, convertedWarpCount + " " + ((convertedWarpCount == 1) ? "warp was" : "warps were")
				+ " converted to flat file and " + failedToConvert + " " + ((failedToConvert == 1) ? "warp" : "warps")
				+ " failed to convert to flat file. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
	}

	private void startWarpConverter() {
		int convertedWarpCount = 0;
		long startTime = System.currentTimeMillis();

		File warpDirectory = new File(getDataFolder(), "warps");
		if (warpDirectory.exists()) {
			File[] files = warpDirectory.listFiles();
			if (files != null && files.length > 0) {
				for (int i = -1; ++i < files.length;) {
					File file = files[i];
					if (file != null && file.getName().toLowerCase().endsWith(".yml")) {
						FileConfiguration ymlFile = YamlConfiguration.loadConfiguration(file);

						try {
							String warpName = ymlFile.getString("Name");
							if (getManager().getWarp(warpName) != null)
								continue;

							double x = ymlFile.getDouble("Location.X"), y = ymlFile.getDouble("Location.Y"),
									z = ymlFile.getDouble("Location.Z");
							float yaw = (float) ymlFile.getDouble("Location.Yaw"),
									pitch = (float) ymlFile.getDouble("Location.Pitch");

							SerializableLocation serializableLocation = new SerializableLocation(
									ymlFile.getString("Location.World"), x, y, z, yaw, pitch);
							Warp warp = new Warp(warpName,
									getServer().getOfflinePlayer(
											UUID.fromString(Objects.requireNonNull(ymlFile.getString("Main Owner")))),
									serializableLocation);

							String statusName = ymlFile.getString("Status");
							if (statusName != null)
								warp.setStatus(statusName.equalsIgnoreCase("SHOP") ? EnumContainer.Status.PUBLIC
										: statusName.equalsIgnoreCase("SERVER") ? EnumContainer.Status.ADMIN
												: EnumContainer.Status.valueOf(
														statusName.toUpperCase().replace(" ", "_").replace("-", "_")));
							warp.setCommands(ymlFile.getStringList("Commands"));
							warp.setDescription(ymlFile.getStringList("Description"));
							warp.setUsagePrice(ymlFile.getDouble("Usage Price"));
							warp.setIconEnchantedLook(ymlFile.getBoolean("Enchanted Look"));
							warp.setCreationDate(ymlFile.getString("Creation Date"));

							ChatColor descriptionColor = ChatColor.getByChar(
									Objects.requireNonNull(ymlFile.getString("Description Color")).replace("&", ""));
							if (descriptionColor != null)
								warp.setDescriptionColor(descriptionColor);

							ChatColor displayNameColor = ChatColor.getByChar(
									Objects.requireNonNull(ymlFile.getString("Name Color")).replace("&", ""));
							if (displayNameColor != null)
								warp.setDisplayNameColor(displayNameColor);

							List<String> commandList = new ArrayList<>();
							commandList.add(ymlFile.getString("Command"));
							warp.setCommands(commandList);

							List<String> ownerList = ymlFile.getStringList("Owners");
							for (int j = -1; ++j < ownerList.size();) {
								UUID uniqueId = UUID.fromString(ownerList.get(j));
								warp.getAssistants().add(uniqueId);
							}

							warp.register();
							convertedWarpCount += 1;
							log(Level.INFO, "The warp " + warp.getWarpName() + " has been successfully converted (Took "
									+ (System.currentTimeMillis() - startTime) + "ms)");
						} catch (Exception ignored) {
						}
					}
				}
			}
		}

		if (convertedWarpCount > 0) {
			log(Level.INFO,
					"A total of " + convertedWarpCount + " old warp(s) were found and converted into new warp(s). "
							+ "(Took " + (System.currentTimeMillis() - startTime) + "ms)");
			log(Level.INFO,
					"All old warp files are never deleted from the original location; therefore, nothing has been lost!");
		} else
			log(Level.INFO, "No old warps were found. Skipping the new warp conversion process... (Took "
					+ (System.currentTimeMillis() - startTime) + "ms)");

		if (getConfig().getBoolean("general-section.essentials-converter")) {
			convertedWarpCount = 0;
			File essentialsDirectory = new File(
					getDataFolder().getAbsolutePath().replace(getDescription().getName(), "Essentials"));
			File[] essentialsFileList = essentialsDirectory.listFiles();
			if (essentialsFileList != null)
				for (int i = -1; ++i < essentialsFileList.length;) {
					File file = essentialsFileList[i];
					if (file.isDirectory() && file.getName().equalsIgnoreCase("warps")) {
						File[] essentialsWarpsList = file.listFiles();
						if (essentialsWarpsList != null && essentialsWarpsList.length <= 0) {
							log(Level.INFO, "There are no more warps that need to be converted.");
							return;
						}

						if (essentialsWarpsList != null)
							for (int j = -1; ++j < essentialsWarpsList.length;) {
								File warpFile = essentialsWarpsList[j];
								if (!warpFile.isDirectory() && warpFile.getName().toLowerCase().endsWith(".yml")) {
									FileConfiguration ymlFile = YamlConfiguration.loadConfiguration(warpFile);

									try {
										String warpName = ymlFile.getString("name");
										if (getManager().getWarp(warpName) != null)
											continue;

										double x = ymlFile.getDouble("x"), y = ymlFile.getDouble("y"),
												z = ymlFile.getDouble("z");
										float yaw = (float) ymlFile.getDouble("yaw"),
												pitch = (float) ymlFile.getDouble("pitch");
										SerializableLocation serializableLocation = new SerializableLocation(
												ymlFile.getString("Location.World"), x, y, z, yaw, pitch);

										Warp warp = new Warp(warpName, serializableLocation);
										warp.register();
										convertedWarpCount += 1;
									} catch (Exception ignored) {
									}
								}
							}
					}
				}

			if (convertedWarpCount > 0) {
				log(Level.INFO,
						"A total of " + convertedWarpCount
								+ " Essentials warp(s) were found and converted into HyperDrive warp(s). " + "(Took "
								+ (System.currentTimeMillis() - startTime) + "ms)");
				log(Level.INFO,
						"All Essentials warp files are never deleted from the original location; therefore, nothing has been lost!");
			} else
				log(Level.INFO,
						"No Essentials warps were found. Skipping the HyperDrive warp conversion process... (Took "
								+ (System.currentTimeMillis() - startTime) + "ms)");
		}
	}

	public void stopTasks() {
		getServer().getScheduler().cancelTask(getTeleportationHandlerTaskId());

		if (getConnection() != null)
			getServer().getScheduler().cancelTask(getCrossServerTaskId());
		if (getConfig().getBoolean("auto-save"))
			getServer().getScheduler().cancelTask(getAutoSaveTaskId());
	}

	public void startTasks() {
		long startTime = System.currentTimeMillis();

		boolean useMySQL = getConfig().getBoolean("mysql-connection.use-mysql");
		TeleportationHandler teleportationHandler = new TeleportationHandler(this);
		int thID = getServer().getScheduler().scheduleSyncRepeatingTask(this, teleportationHandler, 0, 20);
		setTeleportationHandlerTaskId(thID);
		setTeleportationHandler(teleportationHandler);

		int interval = getConfig().getInt("general-section.auto-save-interval");
		BukkitTask autoSaveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> saveWarps(useMySQL),
				20 * interval, 20 * interval);
		setAutoSaveTaskId(autoSaveTask.getTaskId());

		if (useMySQL) {
			HashMap<UUID, Location> locationMap = new HashMap<>();
			int crossServerTeleportTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
				Player firstPlayer = getBungeeListener().getFirstPlayer();
				if (firstPlayer != null)
					getBungeeListener().requestServers(firstPlayer);
				getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
					getDatabaseWarps().clear();
					try {
						Statement statement = getConnection().createStatement();
						ResultSet resultSet = statement.executeQuery("select * from warps");
						while (resultSet.next()) {
							String warpName = resultSet.getString(1).toLowerCase();
							if (!getDatabaseWarps().contains(warpName))
								getDatabaseWarps().add(warpName);
						}

						resultSet.close();
						statement.close();

						for (Warp warp : getManager().getWarpMap().values())
							if (!getDatabaseWarps().contains(warp.getWarpName().toLowerCase()))
								warp.unRegister();

						for (String warpName : getDatabaseWarps())
							if (!getManager().getWarpMap().containsKey(warpName))
								loadWarp(warpName, (getConnection() != null));

					} catch (SQLException e) {
						e.printStackTrace();
					}

					try {
						Statement statement = getConnection().createStatement();
						ResultSet resultSet = statement.executeQuery("select * from transfer");
						while (resultSet.next()) {
							UUID playerUniqueId = UUID.fromString(resultSet.getString(1));
							if (!locationMap.containsKey(playerUniqueId)) {
								String locationString = resultSet.getString(2), server_ip = resultSet.getString(3);
								if (server_ip == null || server_ip.equalsIgnoreCase("")) {
									if (locationString.contains(",")) {
										String[] locationStringArgs = locationString.split(",");
										Location location = new Location(
												getPluginInstance().getServer().getWorld(locationStringArgs[0]),
												Double.parseDouble(locationStringArgs[1]),
												Double.parseDouble(locationStringArgs[2]),
												Double.parseDouble(locationStringArgs[3]),
												Float.parseFloat(locationStringArgs[4]),
												Float.parseFloat(locationStringArgs[5]));
										locationMap.put(playerUniqueId, location);
									}
								} else {
									if (!server_ip.replace("localhost", "127.0.0.1")
											.equalsIgnoreCase((getServer().getIp() + ":" + getServer().getPort())
													.replace("localhost", "127.0.0.1")))
										continue;

									if (locationString.contains(",")) {
										String[] locationStringArgs = locationString.split(",");
										Location location = new Location(
												getPluginInstance().getServer().getWorld(locationStringArgs[0]),
												Double.parseDouble(locationStringArgs[1]),
												Double.parseDouble(locationStringArgs[2]),
												Double.parseDouble(locationStringArgs[3]),
												Float.parseFloat(locationStringArgs[4]),
												Float.parseFloat(locationStringArgs[5]));
										locationMap.put(playerUniqueId, location);
									}
								}
							}
						}

						resultSet.close();
						statement.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				});

				for (UUID playerUniqueId : locationMap.keySet()) {
					getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
						try {
							PreparedStatement preparedStatement = getConnection().prepareStatement(
									"delete from transfer where player_uuid = '" + playerUniqueId.toString() + "'");
							preparedStatement.executeUpdate();
							preparedStatement.close();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					});

					OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(playerUniqueId);
					if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
						Location location = locationMap.get(playerUniqueId);
						locationMap.remove(playerUniqueId);
						getTeleportationHandler().teleportPlayer(offlinePlayer.getPlayer(), location);
						getManager().sendCustomMessage(getConfig().getString("language-section.cross-teleported")
								.replace("{world}", location.getWorld().getName())
								.replace("{x}", String.valueOf(location.getBlockX()))
								.replace("{y}", String.valueOf(location.getBlockY()))
								.replace("{z}", String.valueOf(location.getBlockZ())), offlinePlayer.getPlayer());
					}
				}
			}, 0, 20);
			setCrossServerTaskId(crossServerTeleportTaskId);
		}

		log(Level.INFO, "All tasks were successfully setup and started. (Took "
				+ (System.currentTimeMillis() - startTime) + "ms)");
	}

	private void saveWarps(boolean useMySQL) {
		long startTime = System.currentTimeMillis();
		int savedWarps = 0, failedToSaveWarps = 0;

		File file = new File(getDataFolder(), "/warps.yml");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		for (Warp warp : getManager().getWarpMap().values()) {
			try {
				warp.setWarpName(
						warp.getWarpName().replace("§", "").replaceAll("[.,?:;\'\"\\\\|`~!@#$%^&*()+=/<>]", ""));

				if (!useMySQL || getConnection() == null) {
					yaml.set(warp.getWarpName() + ".location.world", warp.getWarpLocation().getWorldName());
					yaml.set(warp.getWarpName() + ".location.x", warp.getWarpLocation().getX());
					yaml.set(warp.getWarpName() + ".location.y", warp.getWarpLocation().getY());
					yaml.set(warp.getWarpName() + ".location.z", warp.getWarpLocation().getZ());
					yaml.set(warp.getWarpName() + ".location.yaw", warp.getWarpLocation().getYaw());
					yaml.set(warp.getWarpName() + ".location.pitch", warp.getWarpLocation().getPitch());

					try {
						List<String> whiteList = new ArrayList<>();
						for (int j = -1; ++j < warp.getWhiteList().size();) {
							UUID uuid = warp.getWhiteList().get(j);
							whiteList.add(uuid.toString());
						}

						List<String> assistants = new ArrayList<>();
						for (int j = -1; ++j < warp.getAssistants().size();) {
							UUID uuid = warp.getAssistants().get(j);
							assistants.add(uuid.toString());
						}

						yaml.set(warp.getWarpName() + ".traffic", warp.getTraffic());
						yaml.set(warp.getWarpName() + ".status", warp.getStatus().toString());
						yaml.set(warp.getWarpName() + ".creation-date", warp.getCreationDate());
						yaml.set(warp.getWarpName() + ".server-ip",
								warp.getServerIPAddress().replace("localhost", "127.0.0.1"));
						yaml.set(warp.getWarpName() + ".owner", warp.getOwner().toString());
						yaml.set(warp.getWarpName() + ".assistants", assistants);
						yaml.set(warp.getWarpName() + ".whitelist", whiteList);
						yaml.set(warp.getWarpName() + ".commands", warp.getCommands());
						yaml.set(warp.getWarpName() + ".animation-set", warp.getAnimationSet());

						yaml.set(warp.getWarpName() + ".icon.theme", warp.getIconTheme());
						yaml.set(warp.getWarpName() + ".icon.description-color", warp.getDescriptionColor().name());
						yaml.set(warp.getWarpName() + ".icon.name-color", warp.getDisplayNameColor().name());
						yaml.set(warp.getWarpName() + ".icon.description", warp.getDescription());
						yaml.set(warp.getWarpName() + ".icon.use-enchanted-look", warp.hasIconEnchantedLook());
						yaml.set(warp.getWarpName() + ".icon.prices.usage", warp.getUsagePrice());
					} catch (Exception e) {
						e.printStackTrace();
						log(Level.INFO, "There was an issue saving the warp " + warp.getWarpName()
								+ "'s data aside it's location.");
					}

					savedWarps += 1;
					return;
				}

				StringBuilder description = new StringBuilder(), commands = new StringBuilder(),
						whitelist = new StringBuilder(), assistants = new StringBuilder();
				for (int j = -1; ++j < warp.getDescription().size();)
					description.append(warp.getDescription().get(j)).append(",");
				for (int j = -1; ++j < warp.getCommands().size();)
					commands.append(warp.getCommands().get(j)).append(",");
				for (int j = -1; ++j < warp.getWhiteList().size();)
					whitelist.append(warp.getWhiteList().get(j).toString()).append(",");
				for (int j = -1; ++j < warp.getAssistants().size();)
					assistants.append(warp.getAssistants().get(j).toString()).append(",");

				Statement statement = getConnection().createStatement();
				ResultSet rs = statement.executeQuery("select * from warps where name='" + warp.getWarpName() + "'");
				if (rs.next()) {
					statement.executeUpdate("update warps set location = '"
							+ (warp.getWarpLocation().getWorldName() + "," + warp.getWarpLocation().getX() + ","
									+ warp.getWarpLocation().getY() + "," + warp.getWarpLocation().getZ() + ","
									+ warp.getWarpLocation().getYaw() + "," + warp.getWarpLocation().getPitch())
							+ "', status = '" + warp.getStatus().name() + "', creation_date = '"
							+ warp.getCreationDate() + "', icon_theme = '" + warp.getIconTheme()
							+ "', animation_set = '" + warp.getAnimationSet() + "'," + " name_color = '"
							+ warp.getDisplayNameColor().name() + "', description = '" + description.toString()
							+ "', commands = '" + commands.toString() + "'," + " owner = '" + warp.getOwner().toString()
							+ "', white_list = '" + whitelist.toString() + "', assistants = '" + assistants.toString()
							+ "'," + " usage_price = '" + warp.getUsagePrice() + "', enchanted_look = '"
							+ (warp.hasIconEnchantedLook() ? 1 : 0) + "', server_ip = '" + warp.getServerIPAddress()
							+ "' where name = '" + warp.getWarpName() + "';");

					savedWarps += 1;
					rs.close();
					statement.close();
					continue;
				}

				PreparedStatement preparedStatement = connection
						.prepareStatement("insert into warps (name, location, status, creation_date, icon_theme,"
								+ "animation_set, description_color, name_color, description, commands, owner, white_list, assistants, traffic, usage_price, enchanted_look, server_ip) "
								+ "values ('" + warp.getWarpName() + "', '"
								+ (warp.getWarpLocation().getWorldName() + "," + warp.getWarpLocation().getX() + ","
										+ warp.getWarpLocation().getY() + "," + warp.getWarpLocation().getZ() + ","
										+ warp.getWarpLocation().getYaw() + "," + warp.getWarpLocation().getPitch())
								+ "', '" + warp.getStatus().name() + "', '" + warp.getCreationDate() + "', '"
								+ warp.getIconTheme() + "', '" + warp.getAnimationSet() + "', '"
								+ warp.getDescriptionColor().name() + "', '" + warp.getDisplayNameColor().name()
								+ "', ?, ?, '" + warp.getOwner().toString() + "', ?, ?, " + warp.getTraffic() + ", "
								+ warp.getUsagePrice() + ", " + warp.hasIconEnchantedLook() + ", '"
								+ warp.getServerIPAddress().replace("localhost", "127.0.0.1") + "');");

				preparedStatement.setString(1, description.toString());
				preparedStatement.setString(2, commands.toString());
				preparedStatement.setString(3, whitelist.toString());
				preparedStatement.setString(4, assistants.toString());

				savedWarps += 1;
				preparedStatement.executeUpdate();
				preparedStatement.close();
			} catch (SQLException e) {
				failedToSaveWarps += 1;
				e.printStackTrace();
			}
		}

		try {
			yaml.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		log(Level.INFO,
				savedWarps + " " + ((savedWarps == 1) ? "warp was" : "warps were") + " saved and " + failedToSaveWarps
						+ " " + ((failedToSaveWarps == 1) ? "warp" : "warps") + " failed to save. (Took "
						+ (System.currentTimeMillis() - startTime) + "ms)");
	}

	private void loadWarps(boolean useMySQL) {
		long startTime = System.currentTimeMillis();
		int loadedWarps = 0, failedToLoadWarps = 0;
		if (!useMySQL || getConnection() == null) {
			File file = new File(getDataFolder(), "/warps.yml");
			if (file.exists()) {
				YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
				List<String> configurationLines = new ArrayList<>(
						Objects.requireNonNull(yaml.getConfigurationSection("")).getKeys(false));
				for (int i = -1; ++i < configurationLines.size();) {
					try {
						String warpName = configurationLines.get(i).replace("§", "")
								.replaceAll("[.,?:;\'\"\\\\|`~!@#$%^&*()+=/<>]", "");
						UUID uuid = UUID.fromString(Objects.requireNonNull(yaml.getString(warpName + ".owner")));

						SerializableLocation serializableLocation = new SerializableLocation(
								yaml.getString(warpName + ".location.world"), yaml.getDouble(warpName + ".location.x"),
								yaml.getDouble(warpName + ".location.y"), yaml.getDouble(warpName + ".location.z"),
								(float) yaml.getDouble(warpName + ".location.yaw"),
								(float) yaml.getDouble(warpName + ".location.pitch"));

						Warp warp = new Warp(warpName, getPluginInstance().getServer().getOfflinePlayer(uuid),
								serializableLocation);
						warp.register();
						loadedWarps += 1;

						try {
							List<UUID> assistantList = new ArrayList<>(), whiteListPlayers = new ArrayList<>();
							List<String> assistants = yaml.getStringList(warpName + ".assistants"),
									whiteList = yaml.getStringList(warpName + ".whitelist");
							for (int j = -1; ++j < assistants.size();) {
								UUID uniqueId = UUID.fromString(assistants.get(j));
								assistantList.add(uniqueId);
							}

							for (int j = -1; ++j < whiteList.size();) {
								UUID uniqueId = UUID.fromString(whiteList.get(j));
								whiteListPlayers.add(uniqueId);
							}

							String statusString = yaml.getString(warpName + ".status");
							if (statusString == null)
								statusString = EnumContainer.Status.PUBLIC.name();

							EnumContainer.Status status = EnumContainer.Status
									.valueOf(statusString.toUpperCase().replace(" ", "_").replace("-", "_"));
							warp.setStatus(status);
							warp.setAssistants(assistantList);
							warp.setWhiteList(whiteListPlayers);
							warp.setCreationDate(yaml.getString(warpName + ".creation-date"));
							warp.setCommands(yaml.getStringList(warpName + ".commands"));
							warp.setAnimationSet(yaml.getString(warpName + ".animation-set"));

							warp.setIconTheme(yaml.getString(warpName + ".icon.theme"));
							warp.setDescriptionColor(
									ChatColor.valueOf(yaml.getString(warpName + ".icon.description-color")));
							warp.setDisplayNameColor(ChatColor.valueOf(yaml.getString(warpName + ".icon.name-color")));
							warp.setDescription(yaml.getStringList(warpName + ".icon.description"));
							warp.setIconEnchantedLook(yaml.getBoolean(warpName + ".icon.use-enchanted-look"));
							warp.setUsagePrice(yaml.getDouble(warpName + ".icon.prices.usage"));
							warp.setTraffic(yaml.getInt(warpName + ".traffic"));
							warp.setServerIPAddress(
									yaml.getString(warpName + ".server-ip").replace("localhost", "127.0.0.1"));
						} catch (Exception e) {
							e.printStackTrace();
							log(Level.INFO, "There was an issue loading the warp " + warp.getWarpName()
									+ "'s data aside it's location.");
						}
					} catch (Exception ignored) {
						failedToLoadWarps += 1;
					}
				}
			}

			log(Level.INFO,
					loadedWarps + " " + ((loadedWarps == 1) ? "warp was" : "warps were") + " loaded and "
							+ failedToLoadWarps + " " + ((failedToLoadWarps == 1) ? "warp" : "warps")
							+ " failed to load. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
			return;
		}

		try {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("select * from warps");
			while (resultSet.next()) {
				String warpName = resultSet.getString(1),
						ipAddress = resultSet.getString(17).replace("localhost", "127.0.0.1"),
						locationString = resultSet.getString(2);
				if (locationString.contains(",")) {
					String[] locationStringArgs = locationString.split(",");

					SerializableLocation serializableLocation = new SerializableLocation(locationStringArgs[0],
							Double.parseDouble(locationStringArgs[1]), Double.parseDouble(locationStringArgs[2]),
							Double.parseDouble(locationStringArgs[3]), Float.parseFloat(locationStringArgs[4]),
							Float.parseFloat(locationStringArgs[5]));

					Warp warp = new Warp(warpName, serializableLocation);
					warp.register();
					loadedWarps += 1;

					try {
						String statusString = resultSet.getString(3);
						if (statusString == null)
							statusString = EnumContainer.Status.PUBLIC.name();

						EnumContainer.Status status = EnumContainer.Status
								.valueOf(statusString.toUpperCase().replace(" ", "_").replace("-", "_"));
						warp.setStatus(status);
						warp.setCreationDate(resultSet.getString(4));
						warp.setIconTheme(resultSet.getString(5));
						warp.setAnimationSet(resultSet.getString(6));

						String descriptionColor = resultSet.getString(7);
						if (descriptionColor != null && !descriptionColor.equalsIgnoreCase(""))
							warp.setDescriptionColor(ChatColor
									.valueOf(descriptionColor.toUpperCase().replace(" ", "_").replace("-", "_")));
						String nameColor = resultSet.getString(8);
						if (nameColor != null && !nameColor.equalsIgnoreCase(""))
							warp.setDisplayNameColor(
									ChatColor.valueOf(nameColor.toUpperCase().replace(" ", "_").replace("-", "_")));

						String descriptionString = resultSet.getString(9);
						if (descriptionString.contains(",")) {
							List<String> description = new ArrayList<>();
							String[] descriptionStringArgs = descriptionString.split(",");
							for (int i = -1; ++i < descriptionStringArgs.length;)
								description.add(descriptionStringArgs[i]);
							warp.setDescription(description);
						}

						String commandsString = resultSet.getString(10);
						if (commandsString.contains(",")) {
							List<String> commands = new ArrayList<>();
							String[] commandsStringArgs = commandsString.split(",");
							for (int i = -1; ++i < commandsStringArgs.length;)
								commands.add(commandsStringArgs[i]);
							warp.setCommands(commands);
						}

						warp.setOwner(UUID.fromString(resultSet.getString(11)));

						String whitelistString = resultSet.getString(12);
						if (whitelistString.contains(",")) {
							List<UUID> whitelist = new ArrayList<>();
							String[] whitelistStringArgs = whitelistString.split(",");
							for (int i = -1; ++i < whitelistStringArgs.length;)
								whitelist.add(UUID.fromString(whitelistStringArgs[i]));
							warp.setWhiteList(whitelist);
						}

						String assistantsString = resultSet.getString(13);
						if (assistantsString.contains(",")) {
							List<UUID> assistants = new ArrayList<>();
							String[] assistantsStringArgs = assistantsString.split(",");
							for (int i = -1; ++i < assistantsStringArgs.length;)
								assistants.add(UUID.fromString(assistantsStringArgs[i]));
							warp.setAssistants(assistants);
						}

						warp.setTraffic(resultSet.getInt(14));
						warp.setUsagePrice(resultSet.getDouble(15));
						warp.setIconEnchantedLook(resultSet.getInt(16) >= 1);
						warp.setServerIPAddress(ipAddress);
					} catch (Exception e) {
						e.printStackTrace();
						log(Level.INFO, "There was an issue loading the warp " + warp.getWarpName()
								+ "'s data aside it's location.");
					}
				}
			}

			resultSet.close();
			statement.close();
		} catch (SQLException e) {
			failedToLoadWarps += 1;
		}

		log(Level.INFO,
				loadedWarps + " " + ((loadedWarps == 1) ? "warp was" : "warps were") + " loaded and "
						+ failedToLoadWarps + " " + ((failedToLoadWarps == 1) ? "warp" : "warps")
						+ " failed to load. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
	}

	public void saveWarp(Warp warp, boolean useMySQL) {
		if (!useMySQL || getConnection() == null) {
			File file = new File(getDataFolder(), "/warps.yml");
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

			try {
				yaml.set(warp.getWarpName() + ".location.world", warp.getWarpLocation().getWorldName());
				yaml.set(warp.getWarpName() + ".location.x", warp.getWarpLocation().getX());
				yaml.set(warp.getWarpName() + ".location.y", warp.getWarpLocation().getY());
				yaml.set(warp.getWarpName() + ".location.z", warp.getWarpLocation().getZ());
				yaml.set(warp.getWarpName() + ".location.yaw", warp.getWarpLocation().getYaw());
				yaml.set(warp.getWarpName() + ".location.pitch", warp.getWarpLocation().getPitch());

				try {
					List<String> whiteList = new ArrayList<>();
					for (int j = -1; ++j < warp.getWhiteList().size();) {
						UUID uuid = warp.getWhiteList().get(j);
						whiteList.add(uuid.toString());
					}

					List<String> assistants = new ArrayList<>();
					for (int j = -1; ++j < warp.getAssistants().size();) {
						UUID uuid = warp.getAssistants().get(j);
						assistants.add(uuid.toString());
					}

					yaml.set(warp.getWarpName() + ".traffic", warp.getTraffic());
					yaml.set(warp.getWarpName() + ".status", warp.getStatus().toString());
					yaml.set(warp.getWarpName() + ".creation-date", warp.getCreationDate());
					yaml.set(warp.getWarpName() + ".server-ip",
							warp.getServerIPAddress().replace("localhost", "127.0.0.1"));
					yaml.set(warp.getWarpName() + ".owner", warp.getOwner().toString());
					yaml.set(warp.getWarpName() + ".assistants", assistants);
					yaml.set(warp.getWarpName() + ".whitelist", whiteList);
					yaml.set(warp.getWarpName() + ".commands", warp.getCommands());
					yaml.set(warp.getWarpName() + ".animation-set", warp.getAnimationSet());

					yaml.set(warp.getWarpName() + ".icon.theme", warp.getIconTheme());
					yaml.set(warp.getWarpName() + ".icon.description-color", warp.getDescriptionColor().name());
					yaml.set(warp.getWarpName() + ".icon.name-color", warp.getDisplayNameColor().name());
					yaml.set(warp.getWarpName() + ".icon.description", warp.getDescription());
					yaml.set(warp.getWarpName() + ".icon.use-enchanted-look", warp.hasIconEnchantedLook());
					yaml.set(warp.getWarpName() + ".icon.prices.usage", warp.getUsagePrice());
				} catch (Exception e) {
					e.printStackTrace();
					log(Level.INFO, "There was an issue saving the warp " + warp.getWarpName()
							+ "'s data aside it's location.");
				}

				yaml.save(file);
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}

		StringBuilder description = new StringBuilder(), commands = new StringBuilder(),
				whitelist = new StringBuilder(), assistants = new StringBuilder();
		for (int j = -1; ++j < warp.getDescription().size();)
			description.append(warp.getDescription().get(j)).append(",");
		for (int j = -1; ++j < warp.getCommands().size();)
			commands.append(warp.getCommands().get(j)).append(",");
		for (int j = -1; ++j < warp.getWhiteList().size();)
			whitelist.append(warp.getWhiteList().get(j).toString()).append(",");
		for (int j = -1; ++j < warp.getAssistants().size();)
			assistants.append(warp.getAssistants().get(j).toString()).append(",");

		try {
			Statement statement = getConnection().createStatement();
			ResultSet rs = statement.executeQuery("select * from warps where name='" + warp.getWarpName() + "'");
			if (rs.next()) {
				statement.executeUpdate("update warps set location = '"
						+ (warp.getWarpLocation().getWorldName() + "," + warp.getWarpLocation().getX() + ","
								+ warp.getWarpLocation().getY() + "," + warp.getWarpLocation().getZ() + ","
								+ warp.getWarpLocation().getYaw() + "," + warp.getWarpLocation().getPitch())
						+ "', status = '" + warp.getStatus().name() + "', creation_date = '" + warp.getCreationDate()
						+ "', icon_theme = '" + warp.getIconTheme() + "', animation_set = '" + warp.getAnimationSet()
						+ "'," + " name_color = '" + warp.getDisplayNameColor().name() + "', description = '"
						+ description.toString() + "', commands = '" + commands.toString() + "'," + " owner = '"
						+ warp.getOwner().toString() + "', white_list = '" + whitelist.toString() + "', assistants = '"
						+ assistants.toString() + "'," + " usage_price = '" + warp.getUsagePrice()
						+ "', enchanted_look = '" + (warp.hasIconEnchantedLook() ? 1 : 0) + "', server_ip = '"
						+ warp.getServerIPAddress() + "' where name = '" + warp.getWarpName() + "';");

				rs.close();
				statement.close();
				return;
			}

			PreparedStatement preparedStatement = connection
					.prepareStatement("insert into warps (name, location, status, creation_date, icon_theme,"
							+ "animation_set, description_color, name_color, description, commands, owner, white_list, assistants, traffic, usage_price, enchanted_look, server_ip) "
							+ "values ('" + warp.getWarpName() + "', '"
							+ (warp.getWarpLocation().getWorldName() + "," + warp.getWarpLocation().getX() + ","
									+ warp.getWarpLocation().getY() + "," + warp.getWarpLocation().getZ() + ","
									+ warp.getWarpLocation().getYaw() + "," + warp.getWarpLocation().getPitch())
							+ "', '" + warp.getStatus().name() + "', '" + warp.getCreationDate() + "', '"
							+ warp.getIconTheme() + "', '" + warp.getAnimationSet() + "', '"
							+ warp.getDescriptionColor().name() + "', '" + warp.getDisplayNameColor().name()
							+ "', ?, ?, '" + warp.getOwner().toString() + "', ?, ?, " + warp.getTraffic() + ", "
							+ warp.getUsagePrice() + ", " + warp.hasIconEnchantedLook() + ", '"
							+ warp.getServerIPAddress().replace("localhost", "127.0.0.1") + "');");

			preparedStatement.setString(1, description.toString());
			preparedStatement.setString(2, commands.toString());
			preparedStatement.setString(3, whitelist.toString());
			preparedStatement.setString(4, assistants.toString());

			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void loadWarp(String warpName, boolean useMySQL) {
		if (!useMySQL || getConnection() == null) {
			File file = new File(getDataFolder(), "/warps.yml");
			if (file.exists()) {
				YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
				UUID uuid = UUID.fromString(Objects.requireNonNull(yaml.getString(warpName + ".owner")));

				SerializableLocation serializableLocation = new SerializableLocation(
						yaml.getString(warpName + ".location.world"), yaml.getDouble(warpName + ".location.x"),
						yaml.getDouble(warpName + ".location.y"), yaml.getDouble(warpName + ".location.z"),
						(float) yaml.getDouble(warpName + ".location.yaw"),
						(float) yaml.getDouble(warpName + ".location.pitch"));

				Warp warp = new Warp(warpName, getPluginInstance().getServer().getOfflinePlayer(uuid),
						serializableLocation);
				warp.register();

				try {
					List<UUID> assistantList = new ArrayList<>(), whiteListPlayers = new ArrayList<>();
					List<String> assistants = yaml.getStringList(warpName + ".assistants"),
							whiteList = yaml.getStringList(warpName + ".whitelist");
					for (int j = -1; ++j < assistants.size();) {
						UUID uniqueId = UUID.fromString(assistants.get(j));
						assistantList.add(uniqueId);
					}

					for (int j = -1; ++j < whiteList.size();) {
						UUID uniqueId = UUID.fromString(whiteList.get(j));
						whiteListPlayers.add(uniqueId);
					}

					String statusString = yaml.getString(warpName + ".status");
					if (statusString == null)
						statusString = EnumContainer.Status.PUBLIC.name();

					EnumContainer.Status status = EnumContainer.Status
							.valueOf(statusString.toUpperCase().replace(" ", "_").replace("-", "_"));
					warp.setStatus(status);
					warp.setAssistants(assistantList);
					warp.setWhiteList(whiteListPlayers);
					warp.setCreationDate(yaml.getString(warpName + ".creation-date"));
					warp.setCommands(yaml.getStringList(warpName + ".commands"));
					warp.setAnimationSet(yaml.getString(warpName + ".animation-set"));

					warp.setIconTheme(yaml.getString(warpName + ".icon.theme"));
					warp.setDescriptionColor(ChatColor.valueOf(yaml.getString(warpName + ".icon.description-color")));
					warp.setDisplayNameColor(ChatColor.valueOf(yaml.getString(warpName + ".icon.name-color")));
					warp.setDescription(yaml.getStringList(warpName + ".icon.description"));
					warp.setIconEnchantedLook(yaml.getBoolean(warpName + ".icon.use-enchanted-look"));
					warp.setUsagePrice(yaml.getDouble(warpName + ".icon.prices.usage"));
					warp.setTraffic(yaml.getInt(warpName + ".traffic"));
					warp.setServerIPAddress(yaml.getString(warpName + ".server-ip").replace("localhost", "127.0.0.1"));
				} catch (Exception e) {
					e.printStackTrace();
					log(Level.INFO, "There was an issue loading the warp " + warp.getWarpName()
							+ "'s data aside it's location.");
				}
				return;
			}
		}

		try {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("select * from warps");
			while (resultSet.next()) {
				String ipAddress = resultSet.getString(17).replace("localhost", "127.0.0.1"),
						locationString = resultSet.getString(2);
				if (locationString.contains(",")) {
					String[] locationStringArgs = locationString.split(",");

					SerializableLocation serializableLocation = new SerializableLocation(locationStringArgs[0],
							Double.parseDouble(locationStringArgs[1]), Double.parseDouble(locationStringArgs[2]),
							Double.parseDouble(locationStringArgs[3]), Float.parseFloat(locationStringArgs[4]),
							Float.parseFloat(locationStringArgs[5]));

					Warp warp = new Warp(warpName, serializableLocation);
					warp.register();

					try {
						String statusString = resultSet.getString(3);
						if (statusString == null)
							statusString = EnumContainer.Status.PUBLIC.name();

						EnumContainer.Status status = EnumContainer.Status
								.valueOf(statusString.toUpperCase().replace(" ", "_").replace("-", "_"));
						warp.setStatus(status);
						warp.setCreationDate(resultSet.getString(4));
						warp.setIconTheme(resultSet.getString(5));
						warp.setAnimationSet(resultSet.getString(6));

						String descriptionColor = resultSet.getString(7);
						if (descriptionColor != null && !descriptionColor.equalsIgnoreCase(""))
							warp.setDescriptionColor(ChatColor
									.valueOf(descriptionColor.toUpperCase().replace(" ", "_").replace("-", "_")));
						String nameColor = resultSet.getString(8);
						if (nameColor != null && !nameColor.equalsIgnoreCase(""))
							warp.setDisplayNameColor(
									ChatColor.valueOf(nameColor.toUpperCase().replace(" ", "_").replace("-", "_")));

						String descriptionString = resultSet.getString(9);
						if (descriptionString.contains(",")) {
							List<String> description = new ArrayList<>();
							String[] descriptionStringArgs = descriptionString.split(",");
							for (int i = -1; ++i < descriptionStringArgs.length;)
								description.add(descriptionStringArgs[i]);
							warp.setDescription(description);
						}

						String commandsString = resultSet.getString(10);
						if (commandsString.contains(",")) {
							List<String> commands = new ArrayList<>();
							String[] commandsStringArgs = commandsString.split(",");
							for (int i = -1; ++i < commandsStringArgs.length;)
								commands.add(commandsStringArgs[i]);
							warp.setCommands(commands);
						}

						warp.setOwner(UUID.fromString(resultSet.getString(11)));

						String whitelistString = resultSet.getString(12);
						if (whitelistString.contains(",")) {
							List<UUID> whitelist = new ArrayList<>();
							String[] whitelistStringArgs = whitelistString.split(",");
							for (int i = -1; ++i < whitelistStringArgs.length;)
								whitelist.add(UUID.fromString(whitelistStringArgs[i]));
							warp.setWhiteList(whitelist);
						}

						String assistantsString = resultSet.getString(13);
						if (assistantsString.contains(",")) {
							List<UUID> assistants = new ArrayList<>();
							String[] assistantsStringArgs = assistantsString.split(",");
							for (int i = -1; ++i < assistantsStringArgs.length;)
								assistants.add(UUID.fromString(assistantsStringArgs[i]));
							warp.setAssistants(assistants);
						}

						warp.setTraffic(resultSet.getInt(14));
						warp.setUsagePrice(resultSet.getDouble(15));
						warp.setIconEnchantedLook(resultSet.getInt(16) >= 1);
						warp.setServerIPAddress(ipAddress);
					} catch (Exception e) {
						e.printStackTrace();
						log(Level.INFO, "There was an issue loading the warp " + warp.getWarpName()
								+ "'s data aside it's location.");
					}
				}
			}

			resultSet.close();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean doesWarpExistInDatabase(String warpName) {
		return getDatabaseWarps().contains(warpName.toLowerCase());
	}

	private static void copy(File source, File destination) throws IOException {
		try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(destination)) {
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = is.read(buf)) > 0) {
				os.write(buf, 0, bytesRead);
			}
		}
	}

	public void reloadWarps() {
		saveWarps(getConnection() != null);
		loadWarps(getConnection() != null);
	}

	public void log(Level level, String message) {
		getPluginInstance().getServer().getLogger().log(level,
				"[" + getPluginInstance().getDescription().getName() + "] " + message);
	}

	private boolean setupVaultEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null)
			return false;
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null)
			return false;

		setVaultEconomy(rsp.getProvider());
		return getVaultEconomy() != null;
	}

	// getters and setters
	public static HyperDrive getPluginInstance() {
		return pluginInstance;
	}

	private static void setPluginInstance(HyperDrive pluginInstance) {
		HyperDrive.pluginInstance = pluginInstance;
	}

	public Manager getManager() {
		return manager;
	}

	private void setManager(Manager manager) {
		this.manager = manager;
	}

	public TeleportationHandler getTeleportationHandler() {
		return teleportationHandler;
	}

	private void setTeleportationHandler(TeleportationHandler teleportationHandler) {
		this.teleportationHandler = teleportationHandler;
	}

	public Economy getVaultEconomy() {
		return vaultEconomy;
	}

	private void setVaultEconomy(Economy vaultEconomy) {
		this.vaultEconomy = vaultEconomy;
	}

	public MainCommands getMainCommands() {
		return mainCommands;
	}

	private void setMainCommands(MainCommands mainCommands) {
		this.mainCommands = mainCommands;
	}

	public TeleportationCommands getTeleportationCommands() {
		return teleportationCommands;
	}

	private void setTeleportationCommands(TeleportationCommands teleportationCommands) {
		this.teleportationCommands = teleportationCommands;
	}

	public Connection getConnection() {
		return connection;
	}

	private void setConnection(Connection connection) {
		this.connection = connection;
	}

	private int getTeleportationHandlerTaskId() {
		return teleportationHandlerTaskId;
	}

	private void setTeleportationHandlerTaskId(int teleportationHandlerTaskId) {
		this.teleportationHandlerTaskId = teleportationHandlerTaskId;
	}

	private int getAutoSaveTaskId() {
		return autoSaveTaskId;
	}

	private void setAutoSaveTaskId(int autoSaveTaskId) {
		this.autoSaveTaskId = autoSaveTaskId;
	}

	private int getCrossServerTaskId() {
		return crossServerTaskId;
	}

	private void setCrossServerTaskId(int crossServerTaskId) {
		this.crossServerTaskId = crossServerTaskId;
	}

	public String getServerVersion() {
		return serverVersion;
	}

	private void setServerVersion(String serverVersion) {
		this.serverVersion = serverVersion;
	}

	public BungeeListener getBungeeListener() {
		return bungeeListener;
	}

	private void setBungeeListener(BungeeListener bungeeListener) {
		this.bungeeListener = bungeeListener;
	}

	private List<String> getDatabaseWarps() {
		return databaseWarps;
	}

	private void setDatabaseWarps(List<String> databaseWarps) {
		this.databaseWarps = databaseWarps;
	}
}
