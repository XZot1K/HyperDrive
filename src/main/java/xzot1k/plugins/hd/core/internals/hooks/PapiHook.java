/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.hd.HyperDrive;

public class PapiHook extends PlaceholderExpansion {

    private HyperDrive pluginInstance;

    public PapiHook(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);

    }

    public String replace(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public String replace(OfflinePlayer player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @NotNull
    @Override
    public String getAuthor() {
        return getPluginInstance().getDescription().getAuthors().toString();
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "HyperDrive";
    }

    @NotNull
    @Override
    public String getVersion() {
        return getPluginInstance().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        switch (identifier.toLowerCase()) {
            case "limit":
                return String.valueOf(getPluginInstance().getManager().getWarpLimit(player));
            case "count":
                return String.valueOf(getPluginInstance().getManager().getPermittedWarps(player).size());
            default:
                return null;
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
