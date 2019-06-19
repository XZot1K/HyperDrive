package xzot1k.plugins.hd.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import xzot1k.plugins.hd.HyperDrive;

public class HookCheckEvent extends Event {
    private HyperDrive pluginInstance;
    private static HandlerList handlers;
    private Location location;
    private boolean safeLocation;
    private Player player;

    public HookCheckEvent(HyperDrive pluginInstance, Location location, Player player, boolean safeLocation) {
        setPluginInstance(pluginInstance);
        handlers = new HandlerList();
        setLocation(location);
        setPlayer(player);
        setSafeLocation(safeLocation);
    }

    private HyperDrive getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public Location getLocation() {
        return location;
    }

    private void setLocation(Location location) {
        this.location = location;
    }

    public Player getPlayer() {
        return player;
    }

    private void setPlayer(Player player) {
        this.player = player;
    }

    public boolean isSafeLocation() {
        return safeLocation;
    }

    public void setSafeLocation(boolean safeLocation) {
        this.safeLocation = safeLocation;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
