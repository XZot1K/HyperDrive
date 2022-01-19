/*
 * Copyright (c) 2020. All rights reserved.
 */

package xzot1k.plugins.hd.core.internals.hooks;

import net.milkbowl.vault.economy.Economy;
import xzot1k.plugins.hd.HyperDrive;

import java.util.Objects;

public class VaultHandler {

    private HyperDrive pluginInstance;
    private Economy economy;

    public VaultHandler(HyperDrive pluginInstance) {
        setPluginInstance(pluginInstance);
        setEconomy(Objects.requireNonNull(getPluginInstance().getServer().getServicesManager().getRegistration(Economy.class)).getProvider());
    }

    public Economy getEconomy() {
        return economy;
    }

    private void setEconomy(Economy economy) {
        this.economy = economy;
    }

    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}
