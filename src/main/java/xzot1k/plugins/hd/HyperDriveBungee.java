package xzot1k.plugins.hd;

import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class HyperDriveBungee extends Plugin implements Listener {

    @Override
    public void onEnable() {

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().registerChannel("Return");

    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {

    }



}