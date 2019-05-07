package xzot1k.plugins.hd.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import xzot1k.plugins.hd.HyperDrive;

public class WarpEvent extends Event implements Cancellable
{
    private HyperDrive pluginInstance;
    private HandlerList handlers;
    private boolean cancelled;
    private Location location;
    private Player player;

    public WarpEvent(HyperDrive pluginInstance, Location location, Player player)
    {
        setPluginInstance(pluginInstance);
        setHandlers(new HandlerList());
        setCancelled(false);
        setLocation(location);
        setPlayer(player);
    }

    public HandlerList getHandlers()
    {
        return handlers;
    }

    private HyperDrive getPluginInstance()
    {
        return pluginInstance;
    }

    private void setPluginInstance(HyperDrive pluginInstance)
    {
        this.pluginInstance = pluginInstance;
    }

    private void setHandlers(HandlerList handlers)
    {
        this.handlers = handlers;
    }

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }

    public Location getLocation()
    {
        return location;
    }

    public void setLocation(Location location)
    {
        this.location = location;
    }

    public Player getPlayer()
    {
        return player;
    }

    private void setPlayer(Player player)
    {
        this.player = player;
    }
}
